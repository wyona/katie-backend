package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.Error;
import com.wyona.katie.services.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.wyona.katie.models.ServerConfiguration;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.Context;
import com.wyona.katie.services.ContextService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller to get and set FAQ
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1") 
public class FAQController {

    @Autowired
    private ContextService domainService;

    /**
     * REST interface to get FAQ
     */
    @RequestMapping(value = "/faq", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get Frequently Asked Questions")
    @Deprecated
    public ResponseEntity<?> getFAQ(
        @ApiParam(name = "language", value = "Language of FAQ, e.g. 'de' or 'en'",required = true)
        @RequestParam(value = "language", required = true) String language,
        @ApiParam(name = "context", value = "Context, for example 'wyona', which represents a single realm containing its own set of FAQ, etc.",required = false)
        @RequestParam(value = "context", required = false) String context,
        HttpServletRequest request) {

        Context ctx = null;
        try {
            ctx = domainService.getContext(context);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        // TODO: Use FAQ faq = xmlService.getFAQ(ctx, language, uuidOnly);

        java.io.File file = ctx.getFAQJsonDataPath(language);
        log.info("FAQ dataset path: " + file.getAbsolutePath());

        if (!file.exists()) {
            String msg = "No FAQs for language '" + language + "' and context '" + ctx.getId() + "'!";
            log.error(msg);
            return new ResponseEntity<>(new Error(msg, "LANGUAGE_NOT_SUPPORTED"), HttpStatus.NOT_FOUND);
        }

        try {
            org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(new java.io.FileInputStream(file));
            return ResponseEntity.ok()
                //.headers(headers)
                .contentLength(file.length())
                //.contentType(org.springframework.http.MediaType.parseMediaType("application/json"))
                .body(resource);
        } catch(java.io.FileNotFoundException e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "FILE_NOT_FOUND"), HttpStatus.NOT_FOUND);
        }
    }
}
