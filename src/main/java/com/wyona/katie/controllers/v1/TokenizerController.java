package com.wyona.katie.controllers.v1;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.wyona.katie.handlers.NumentaEmbeddingsWithPreTokenization;
import com.wyona.katie.models.Error;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ContextService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
 * Controller to tokenize texts
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/tokenizer")
public class TokenizerController {

    @Autowired
    private ContextService domainService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private NumentaEmbeddingsWithPreTokenization numentaEmbeddingsWithPreTokenization;

    /**
     * REST interface to get bert-base-cased tokenization
     */
    @RequestMapping(value = "/bert-base-cased", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get bert-base-cased tokenization of text")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> getBertBaseCasedTokenization(
        @ApiParam(name = "domainId", value = "Domain, for example 'wyona', which represents a single realm containing its own summary.",required = true)
        @RequestParam(value = "domainId", required = true) String domainId,
        @ApiParam(name = "text", value = "Text to be tokenized", required = true)
        @RequestParam(value = "text", required = true) String text,
        HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (!domainService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        try {
            // INFO: https://platform.openai.com/docs/guides/embeddings/second-generation-models
            // https://medium.com/@basics.machinelearning/tokenization-in-openai-api-lets-explore-tiktoken-library-d02d3ce94b0a
            //String tokenizerName = "cl100k_base";

            // INFO: https://huggingface.co/bert-base-cased/blob/main/tokenizer.json
            String tokenizerName = "bert-base-cased";
            HuggingFaceTokenizer tokenizer = numentaEmbeddingsWithPreTokenization.getTokenizer(tokenizerName);
            Encoding encoding = tokenizer.encode(text);
            String[] tokens = encoding.getTokens();
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode body = mapper.createObjectNode();
            body.put("tokenizer", tokenizerName);
            body.put("text", text);
            body.put("number-of-tokens", tokens.length);
            ArrayNode arrayNode = mapper.createArrayNode();
            for (String token : tokens) {
                arrayNode.add(token);
            }
            body.put("tokens", arrayNode);
            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }
}
