package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.learningcoach.ConversationStarter;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.LearningCoachService;
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

    @Autowired
    private LearningCoachService learningCoachService;

    /**
     * REST interface to get conversation starters
     */
    @RequestMapping(value = "/conversation-starters", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary="Get conversation starters")
    public ResponseEntity<?> getConversationStarters(
        HttpServletRequest request) {

        try {
            ConversationStarter[] conversationStarters = learningCoachService.getConversationStarters();

            ObjectMapper mapper = new ObjectMapper();
            //ObjectNode body = mapper.createObjectNode();
            ArrayNode starters = mapper.createArrayNode();
            //body.put("conversation-starters", starters);

            for (ConversationStarter cs : conversationStarters) {
                ObjectNode starter0 = mapper.createObjectNode();
                starters.add(starter0);
                starter0.put("id", cs.getSuggestion().getId());
                starter0.put("suggestion", cs.getSuggestion().getContent());
            }

            //return new ResponseEntity<>(body.toString(), HttpStatus.OK);
            return new ResponseEntity<>(starters.toString(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }
}
