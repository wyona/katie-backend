package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.services.ContextService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller providing learning coach interfaces
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/learning-coach")
public class LearningCoachController {

    @Autowired
    private ContextService domainService;

    /**
     * REST interface to get conversation starters
     */
    @RequestMapping(value = "/conversation-starters", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary="Get conversation starters")
    public ResponseEntity<?> getConversationStarters(
        @ApiParam(name = "domainId", value = "Katie domain Id",required = true)
        @RequestParam(value = "domainId", required = true) String domainId,
        HttpServletRequest request) {

        if (!domainService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();
            ArrayNode starters = mapper.createArrayNode();
            body.put("conversation-starters", starters);

            ObjectNode starter0 = mapper.createObjectNode();
            starters.add(starter0);
            starter0.put("id", 0);
            starter0.put("suggestion", "Erkläre mir wie man eine analoge Uhr liest!");

            ObjectNode starter1 = mapper.createObjectNode();
            starters.add(starter1);
            starter1.put("id", 1);
            starter1.put("suggestion", "Lass uns spielerisch zusammen lernen, wie man eine analoge Uhr liest!");

            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }
}
