package com.wyona.katie.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.springframework.util.MultiValueMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * TODO: Finish implementation, whereas see task https://pm.wyona.com/issues/2723 re using SPARQL using wikidata.org
 */
@Slf4j
@Component
public class KnowledgeGraphQuestionAnswerImpl implements QuestionAnswerHandler {

    // TODO: Make Ontology configurable
    private static final String queryURL = "https://query.wikidata.org/sparql";

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("TODO: Knowledge Graph implementation of deleting tenant ...");
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        log.info("TODO: Knowledge Graph implementation of creating tenant ...");
        return queryURL;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context context, boolean indexAlternativeQuestions) {
        log.info("TODO: Train Knowledge Graph implementation ...");
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation!");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Delete/train is just a workaround, implement retrain by itself");
        if (delete(qna.getUuid(), domain)) {
            train(qna, domain, indexAlternativeQuestions);
        } else {
            log.warn("QnA with UUID '" + qna.getUuid() + "' was not deleted and therefore was not retrained!");
        }
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        log.warn("TODO: Implement delete()!");
        return false;
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        log.info("Get answers from knowledge graph ...");

        String answer = getAnswerFromWikidata(question);

        List<Hit> answers = new ArrayList<Hit>();
        if (answer != null) {
            Date dateAnswered = null;
            Date dateAnswerModified = null;
            Date dateOriginalQuestionSubmitted = null;
            String originalQuestion = "TODO?";
            double score = -1; // TODO: Get score
            answers.add(new Hit(new Answer(question.getSentence(), answer, null, null, question.getClassifications(), null, null, dateAnswered, dateAnswerModified, null, context.getId(), null, originalQuestion, dateOriginalQuestionSubmitted, true, null, true, null), score));
        }

        return answers.toArray(new Hit[0]);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context context, int limit) {
        log.info("TODO: Get answers from Knowledge Graph implementation, also considering classifiations ...");

        log.info("Configured knowledge graph query URL: " + context.getKnowledgeGraphQueryUrl());

        List<Hit> answers = new ArrayList<Hit>();

        return answers.toArray(new Hit[0]);
    }

    /**
     *
     */
    private String getAnswerFromWikidata(Sentence question) {
        // INFO: See https://www.wikidata.org/wiki/Wikidata:SPARQL_query_service/en or https://www.mediawiki.org/wiki/Wikidata_Query_Service/User_Manual#SPARQL_endpoint
        log.info("Wikidata SPARQL query URL: " + queryURL);

        Entity[] personEntities = question.getEntities(Entity.AK_PERSON);
        if (personEntities.length == 1) {
            String personName = personEntities[0].getValue();
            log.info("Search for person '" + personName + "' inside wikidata ...");
            String query = getDateOfBirthFullTextSearchQuery(personName);
            //String query = getDateOfBirthExactMatchQuery(personName);

            log.info("Query: " + query);

            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                //headers.set("Accept", "application/sparql-results+json");

                MultiValueMap<String, String> map= new org.springframework.util.LinkedMultiValueMap<String, String>();
                //String urlEncodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                //map.add("query", urlEncodedQuery);
                map.add("query", query); // INFO: RestTemplate automatically URL encodes parameters
                map.add("format", "json");
                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

                String requestUrl = queryURL;
                log.info("Request URL: " + requestUrl);
                ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
                JsonNode bodyNode = response.getBody();
                log.info("Response JSON: " + bodyNode);

                JsonNode bindingsNode = bodyNode.get("results").get("bindings");
                if (bindingsNode.isArray()) {
                    if (bindingsNode.size() > 1) {
                        log.warn("There are " + bindingsNode.size() + " humans matching the name '" + personName + "'!");
                    }
                    // TODO: If more than 1 human found, then TODO
                    for (JsonNode itemNode : bindingsNode) {
                        if (itemNode.has("dateOfBirth")) {
                            String dateOfBirth = itemNode.get("dateOfBirth").get("value").asText();
                            String answer = personName + " was born " + dateOfBirth;
                            log.info("Answer: " + answer);
                            return answer;
                        }
                    }
                }

                log.warn("No date of birth found for " + personName);
                return null;
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }

        log.warn("Question '" + question + "' does not contain a person entity.");
        return null;
    }

    /**
     * @param personName Name of person, e.g. "Michael Wechner"
     */
    private String getDateOfBirthExactMatchQuery(String personName) {
        String query = "SELECT ?item ?itemLabel ?dateOfBirth ?occupationLabel WHERE {?item wdt:P31 wd:Q5.?item ?label \"" + personName + "\"@en .OPTIONAL{?item wdt:P569 ?dateOfBirth .}OPTIONAL{?item wdt:P106 ?occupation .}SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }}";
        return query;
    }

    /**
     * @param personName Name of person, e.g. "Michael Wechner"
     */
    private String getDateOfBirthFullTextSearchQuery(String personName) {
        // INFO: Full text search https://www.mediawiki.org/wiki/Wikidata_Query_Service/User_Manual/MWAPI https://stackoverflow.com/questions/39773812/how-to-query-for-people-using-wikidata-and-sparql

        StringBuilder query = new StringBuilder("");
        query.append("SELECT DISTINCT ?item ?itemLabel ?dateOfBirth");

        query.append("\nWHERE {");
        query.append("\nhint:Query hint:optimizer \"None\".");

        query.append("\nSERVICE wikibase:mwapi {");
        query.append("\nbd:serviceParam wikibase:api \"Search\";");
        query.append("\nwikibase:endpoint \"www.wikidata.org\";");
        query.append("\nmwapi:srsearch \"" + personName + " haswbstatement:P31=Q5\".");
        query.append("\n?item wikibase:apiOutputItem mwapi:title .");
        query.append("\n}");

        query.append("\nOPTIONAL {?item wdt:P569 ?dateOfBirth . }");
        query.append("\nSERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],en\". }");

        query.append("\n}");

        return query.toString();
    }
}
