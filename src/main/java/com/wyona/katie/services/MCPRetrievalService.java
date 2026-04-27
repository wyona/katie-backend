package com.wyona.katie.services;

import com.wyona.katie.answers.OpenERZ;
import com.wyona.katie.models.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MCPRetrievalService {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private XMLService xmlService;
    //@Autowired
    //private QuestionAnsweringService qaService;
    @Autowired
    private AuthenticationService authenticationService;

    @Tool(
            name = "katie_text_search",
            description = "Find relevant content by natural language query"
    )
    public List<String> findRelevantContent(
            @ToolParam(description = "The question to search for", required = true) String question
            //@ToolParam(description = "The Katie knowledge base Id", required = false) String domainId
    ) throws Exception {
        String domainId = getDomainId();
        String userId = authenticationService.getUserId();
        // TODO: Check whether user is member of domain

        log.info("Finding relevant content for question '" + question + "' of user '" + userId + "' inside domain '" + domainId + "' ...");

        Context domain = xmlService.parseContextConfig(domainId);

        try {
            List<String> results = new ArrayList<>();

            results.add("Dummy answer");
            /*
            List<String> classifications = new ArrayList<String>();
            String messageId = null; // TODO
            String channelRequestId = null; // TODO
            boolean includeFeedbackLinks = false;
            ContentType answerContentType = null;
            String remoteAddress = null; // getRemoteAddress(request);
            java.util.List<ResponseAnswer> responseAnswers = qaService.getAnswers(question, null, false, classifications, messageId, domain, new Date(), remoteAddress, ChannelType.UNDEFINED, channelRequestId, 10, 0, true, answerContentType, includeFeedbackLinks, false, false);
            for (ResponseAnswer answer : responseAnswers) {
                //log.info("Answer: " + answer.getAnswer());
                results.add(answer.getAnswer());
            }
            */

            return results;
        } catch (Exception e) {
            log.warn("Getting answers for domain '" + domain.getName() + "' failed!");
            log.error(e.getMessage(), e);
        }

        return List.of(
                "Katherina was born October 18, 1896",
                "Michael was born February 16, 1969",
                "Result for question: " + question
        );
    }

    /**
     * Get the dates for the city of Zurich’s paper and cardboard collection.
     * @param zipCode Swiss ZIP code within the city of Zurich (e.g. 8044, 8003, 8032).
     * @return A list of upcoming paper and cardboard collection dates
     */
    @Tool(
            name = "katie_paper_cardboard_collection_city_of_zurich",
            description = "Get the dates for the city of Zurich’s paper and cardboard collection."
    )
    public List<String> getPapierKartonSammlungDatesCityOfZurich(
            @ToolParam(description = "Swiss ZIP code within the city of Zurich (e.g. 8044, 8003, 8032).", required = true) Integer zipCode,
            @ToolParam(description = "Type of waste, either 'cardboard' or 'paper'", required = true) String wasteType
    ) {
        log.info("Get the paper and cardboard collection dates for ZIP code " + zipCode + " in the city of Zurich.");
        OpenERZ openERZ = new OpenERZ();
        List<Date> dates = openERZ.getDates(zipCode.toString(), wasteType);
        List<String> datesAsString = new ArrayList<>();
        for (Date date : dates) {
            datesAsString.add(date.toString());
        }

        return datesAsString;
    }

    /**
     *
     */
    private String getDomainId() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof BearerTokenAuthenticationToken) {
            BearerTokenAuthenticationToken bearerToken = (BearerTokenAuthenticationToken) authentication;
            //log.debug("Bearer token: " + bearerToken.getToken());
            if (jwtService.isJWTValid(bearerToken.getToken(), null)) {
                log.info("Bearer Token is valid");
                throw new Exception("Bearer Token is valid, but otherwise not implemented");
            } else {
                log.warn("Bearer Token is not valid!");
                throw new Exception("Bearer token invalid!");
            }
        } else if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            String tokenValue = jwtToken.getToken().getTokenValue();
            //log.debug("JWT token: " + tokenValue);
            if (jwtService.isJWTValid(tokenValue, null)) {
                log.info("JWT Token is valid. Expire date: " + new Date(jwtService.getJWTExpirationTime(tokenValue)));
                String domainId = jwtService.getJWTClaimValue(tokenValue, "domain-id");
                if (domainId == null) {
                    throw new Exception("Access token does not contain a domain Id (domain-id)!");
                }
                String username = jwtService.getJWTSubject(tokenValue);
                authenticationService.login(username, null);
                return domainId;
            } else {
                log.warn("JWT Token is not valid!");
                throw new Exception("Access token invalid!");
            }
        } else {
            //Object principal = authentication.getPrincipal();
            //log.info("Authentication principal : " + principal);
            log.warn("Authentication " + authentication + " not implemented!");
            throw new Exception("Authentication " + authentication + " not implemented!");
        }
    }
}

