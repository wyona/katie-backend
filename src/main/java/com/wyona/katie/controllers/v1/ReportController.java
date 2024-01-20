package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.wyona.katie.models.Context;
import com.wyona.katie.services.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller to generate and send reports
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/report")
public class ReportController {

    @Autowired
    private ContextService contextService;

    @Autowired
    public ReportController(ContextService contextService) {
        this.contextService = contextService;
    }

    /**
     * REST interface to generate / send report
     */
    @RequestMapping(value = "/current", method = RequestMethod.GET, produces = "application/json")
    //@RequestMapping(value = "/current", method = RequestMethod.GET, produces = "text/html;charset=UTF-8")
    @ApiOperation(value="Generate report for a particular domain")
    public ResponseEntity<?> getCurrentSummary(
        @ApiParam(name = "domainId", value = "Domain, for example 'wyona', which represents a single realm containing its own summary.",required = true)
        @RequestParam(value = "domainId", required = true) String domainId,
        @ApiParam(name = "lastNumberOfDays", value = "Last number of days for which insights", required = true)
        @RequestParam(value = "lastNumberOfDays", required = true) Integer lastNumberOfDays,
        HttpServletRequest request) {

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "DOMAIN_SERVICE_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            int numberOfReportsSent = contextService.generateReport(domain, lastNumberOfDays.intValue());
            String body = "{\"reports-sent\":" + numberOfReportsSent+ "}";
            return new ResponseEntity<>(body, HttpStatus.OK);

            /*
            String html = "<html><body>" + numberOfReportsSent + " reports sent :-)</body></html>";

            return ResponseEntity.ok()
                //.headers(headers)
                //.contentLength(html.length())
                //.contentType(org.springframework.http.MediaType.parseMediaType("text/html;charset=UTF-8")) // See "produces" above
                .body(html);

             */
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "TEMPLATE_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }
}
