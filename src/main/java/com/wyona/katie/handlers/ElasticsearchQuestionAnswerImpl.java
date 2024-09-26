package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.StringEntity;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;

import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

//import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
@Slf4j
@Component
public class ElasticsearchQuestionAnswerImpl implements QuestionAnswerHandler {

    @Value("${elasticsearch.hostname}")
    private String elasticsearchHostname;

    @Value("${elasticsearch.scheme}")
    private String elasticsearchScheme;

    @Value("${elasticsearch.port}")
    private String elasticsearchPort;

    @Value("${elasticsearch.basic.auth.username}")
    private String elasticsearchBasicAuthUsername;

    @Value("${elasticsearch.basic.auth.password}")
    private String elasticsearchBasicAuthPassword;

    @Value("${elasticsearch.score.threshold}")
    private float scoreThreshold;

    private static final String INDEX_NAME_PREFIX = "askkatie_";

    private static final String DATE_KATIE_FIELD = "date_katie";
    private static final String TYPE = "question_answer";
    private static String ELASTICSEARCH_DATE_PATTERN = "yyyyMMdd'T'HHmmss.SSSZ";

    private static final String CONTEXT_ID_FIELD = "context_id";
    private static final String QUESTION_FIELD = "question";
    private static final String ANSWER_ID_FIELD = "answer";

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) { 
        log.info("Elasticsearch implementation of deleting tenant ...");
        deleteIndex(domain);
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        log.info("Elasticsearch implementation of creating tenant ...");
        return createIndex();
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context context, boolean indexAlternativeQuestions) {
        log.info("Train Elasticsearch implementation ...");

        try {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"" + CONTEXT_ID_FIELD + "\":\"" + context.getId() + "\"");
            sb.append(",");
            sb.append("\"" + QUESTION_FIELD + "\":\"" + qna.getQuestion() + "\"");
            sb.append(",");
            sb.append("\"" + ANSWER_ID_FIELD + "\":\"" + Answer.AK_UUID_COLON + qna.getUuid() + "\"");
            sb.append(",");
            DateFormat df = new SimpleDateFormat(ELASTICSEARCH_DATE_PATTERN);
            Date date = new Date();
            sb.append("\"" + DATE_KATIE_FIELD + "\":\"" + df.format(date) + "\"");
            sb.append("}");

            HttpEntity entity = new StringEntity(sb.toString(), "application/json", "utf-8");
            RestClient restClient = getRestClient();
            Response response = restClient.performRequest("POST", "/" + context.getElasticsearchIndex() + "/" + TYPE, java.util.Collections.<String, String>emptyMap(), entity);
            if (response.getStatusLine().getStatusCode() == 200) {
                log.info("Question/answer entity added successfully to index");
            } else {
                log.warn("Response code '" + response.getStatusLine().getStatusCode() + "'");
            }
            restClient.close();
        } catch(ResponseException e) {
            log.error(e.getMessage(), e);
            Response errorResponse = e.getResponse();
            log.warn("Response status: " + errorResponse.getStatusLine() + ", Status code: " + errorResponse.getStatusLine().getStatusCode());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
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
        log.info("Delete question with UUID '' from Elasticsearch ...");

        try {
            String akUuid = Answer.AK_UUID_COLON + uuid;
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"query\":{");
            sb.append("\"match\":{");
            sb.append("\"" + ANSWER_ID_FIELD + "\":\"" + akUuid + "\"");
            sb.append("}");
            sb.append("}");
            sb.append("}");

            HttpEntity entity = new StringEntity(sb.toString(), "application/json", "utf-8");
            RestClient restClient = getRestClient();
            Response response = restClient.performRequest("POST", "/" + domain.getElasticsearchIndex() + "/_delete_by_query", java.util.Collections.<String, String>emptyMap(), entity);
            if (response.getStatusLine().getStatusCode() == 200) {
                log.info("Question deleted successfully.");
            } else {
                log.warn("Response code '" + response.getStatusLine().getStatusCode() + "'");
            }
            restClient.close();
            return true;
        } catch(ResponseException e) {
            log.error(e.getMessage(), e);
            Response errorResponse = e.getResponse();
            log.warn("Response status: " + errorResponse.getStatusLine() + ", Status code: " + errorResponse.getStatusLine().getStatusCode());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        log.info("TODO: Consider using entities!");
        return getAnswers(question.getSentence(), question.getClassifications(), context, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context context, int limit) {
        List<Hit> answers = new ArrayList<Hit>();

        log.info("Get answer from Elasticsearch implementation ...");

        try {
            // INFO: See https://www.yulup.com/en/projects/2924e29b-c744-4dbb-9e3f-e4b46831779c/bdd-scenarios/bdd-004.html, curl -XGET http://localhost:9200/askkatie/_search?q=context_id:'ROOT'

            int size = 5; // INFO: Number of returned hits
            StringBuilder query = new StringBuilder("{");
            query.append("\"size\":" + size + ",");
            query.append("\"query\":{");
            query.append("\"bool\":{");
            query.append("\"must\":[");
            //query.append("{\"term\":{\"" + CONTEXT_ID_FIELD + "\":\"" + context.getId() + "\"}}");
            query.append("{\"match\":{\"" + CONTEXT_ID_FIELD + "\":\"" + context.getId() + "\"}}");
            query.append(",");
            query.append("{\"match\":{\"" + QUESTION_FIELD + "\":\"" + question + "\"}}");
            // TODO: Add more matchers
            query.append("]"); // INFO: End must
            query.append("}"); // INFO: End bool
            // TODO: Add sort
            query.append("}"); // INFO: End query
            query.append("}");

            log.info("Elasticsearch query: " + query);
            HttpEntity entity = new StringEntity(query.toString(), "application/json", "utf-8");
            RestClient restClient = getRestClient();
            Response response = restClient.performRequest("GET", "/" + context.getElasticsearchIndex() + "/_search", java.util.Collections.singletonMap("pretty", "true"), entity);
            if (response.getStatusLine().getStatusCode() == 200) {
                java.io.InputStream in = response.getEntity().getContent();
                ObjectMapper jsonPojoMapper = new ObjectMapper();
                java.util.Map<String, Object> data = jsonPojoMapper.readValue(in, java.util.Map.class);
                log.info("Data: " + data); // INFO: {took=2, timed_out=false, _shards={total=5, successful=5, skipped=0, failed=0}, hits={total=1, max_score=0.2876821, hits=[{_index=askkatie, _type=question_answer, _id=fguU-nQB485E39MqKvVN, _score=0.2876821, _source={context_id=ROOT, question=What time is it?, answer=ak-uuid:3012c004-6966-4567-81bc-98e448f2c710, date_katie=20201005T230408.242+0200}}]}}

                java.util.HashMap hitsOverview = (java.util.HashMap)data.get("hits");
                String total = hitsOverview.get("total").toString();
                log.warn("DEBUG: Total number of answers (Context Id: " + context.getId() + "): " + total);

                java.util.List<java.util.Map<String, Object>> hits = (java.util.ArrayList)hitsOverview.get("hits");
                for (java.util.Map<String, Object> hit: hits) {
                    log.info("Hit: " + hit); // INFO: {_index=askkatie, _type=question_answer, _id=fguU-nQB485E39MqKvVN, _score=0.2876821, _source={context_id=ROOT, question=What time is it?, answer=ak-uuid:3012c004-6966-4567-81bc-98e448f2c710, date_katie=20201005T230408.242+0200}}

                    java.util.HashMap<String, Object> source = (java.util.HashMap)hit.get("_source");
                    String _question = source.get(QUESTION_FIELD).toString();
                    String _answer = source.get(ANSWER_ID_FIELD).toString();
                    log.info("Question/Answer: " + _question + " " + _answer);

                    Date dateAnswered = null;
                    Date dateAnswerModified = null;
                    Date dateOriginalQuestionSubmitted = null;

                    double _score = Double.parseDouble(hit.get("_score").toString()); // INFO: For example _score=0.5753642, whereas see for example https://www.compose.com/articles/how-scoring-works-in-elasticsearch/

                    if (_score >= scoreThreshold) {
                        answers.add(new Hit(new Answer(question, _answer, null,null, classifications,null, null, dateAnswered, dateAnswerModified, null, context.getId(), null, _question, dateOriginalQuestionSubmitted, true, null, true, null), _score));
                    } else {
                        log.info("Do not add answer, because score '" + _score + "' of hit is below configured threshold '" + scoreThreshold + "'.");
                    }
                }

                in.close();
            } else {
                log.warn("Response code '" + response.getStatusLine().getStatusCode() + "'");
            }
            restClient.close();
        } catch(ResponseException e) {
            log.error(e.getMessage(), e);
            Response errorResponse = e.getResponse();
            log.warn("Response status: " + errorResponse.getStatusLine() + ", Status code: " + errorResponse.getStatusLine().getStatusCode());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return answers.toArray(new Hit[0]);
    }

    /**
     * Get elasticsearch host, e.g. 'http://elasticsearch.ukatie.com:80'
     */
    private HttpHost getHttpHost() {
        return new HttpHost(elasticsearchHostname, Integer.parseInt(elasticsearchPort), elasticsearchScheme);
    }

    /**
     * Delete Elasticsearch index associated with Katie domain Id
     * @param domain Domain within Katie, e.g. "5bd57b92-da98-422f-8ad6-6670b9c69184"
     */
    private void deleteIndex(Context domain) {
        String indexName = domain.getElasticsearchIndex();
        RestClient restClient = getRestClient();
        try {
            log.info("Delete index '" + indexName + "' ...");
            // INFO: curl -XDELETE http://localhost:9200/askkatie_5bd57b92-da98-422f-8ad6-6670b9c69184/
            restClient.performRequest("DELETE", "/" + indexName + "/");
        } catch(ResponseException re) {
            Response errorResponse = re.getResponse();
            log.warn("Response status: " + errorResponse.getStatusLine() + ", Status code: " + errorResponse.getStatusLine().getStatusCode());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            restClient.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @return elasticsearch index name, e.g. "askkatie_1bb13fed-683a-4e37-b531-45b5c9a4324f"
     */
    private String getIndexName() {
        return INDEX_NAME_PREFIX + UUID.randomUUID().toString();
    }

    /**
     * Check whether index exists and in case it does not exist, then create an index with an appropriate mapping
     * @return index name, e.g. "askkatie_5bd57b92-da98-422f-8ad6-6670b9c69184"
     */
    private String createIndex() {
        String indexName = getIndexName();

        if (indexExists(indexName)) {
            log.error("Index '" + indexName + "' already exists! Therefore abort index creation.");
            return null;
        }

        RestClient restClient = getRestClient();
        try {
            // INFO: Delete index in order to test index creation: curl -XDELETE http://localhost:9200/askkatie/
            // INFO: https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html
            String body = "{ \"mappings\": { \"" + TYPE + "\": { \"properties\": { \"" + DATE_KATIE_FIELD + "\": { \"type\": \"date\", \"format\":\"" + ELASTICSEARCH_DATE_PATTERN + "\" },\"" + QUESTION_FIELD + "\":{\"type\":\"text\"},\"" + CONTEXT_ID_FIELD + "\":{\"type\":\"keyword\"},\"" + ANSWER_ID_FIELD + "\":{\"type\":\"keyword\"} } } } }";
            log.info("Create index '" + indexName + "' with mappings '" + body + "' ...");
            HttpEntity entity = new StringEntity(body, "application/json", "utf-8");
            Response response = restClient.performRequest("PUT", "/" + indexName + "/", java.util.Collections.<String, String>emptyMap(), entity);
            return indexName;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            restClient.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Check whether index exists
     * @param indexName Index name, e.g. "askkatie_5bd57b92-da98-422f-8ad6-6670b9c69184"
     * @return true when index exists and false otherwise
     */
    private boolean indexExists(String indexName) {
        boolean indexExists = false;
        RestClient restClient = getRestClient();
        try {
            log.info("Check whether index '" + indexName + "' exists ...");
            // INFO: curl -XGET http://localhost:9200/askkatie_5bd57b92-da98-422f-8ad6-6670b9c69184/
            restClient.performRequest("GET", "/" + indexName + "/");
            indexExists = true;
        } catch(ResponseException re) {
            Response errorResponse = re.getResponse();
            log.debug("Response status: " + errorResponse.getStatusLine() + ", Status code: " + errorResponse.getStatusLine().getStatusCode());

            if (errorResponse.getStatusLine().getStatusCode() == 404) {
                log.debug("Index '" + indexName + "' does not exist yet.");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            restClient.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return indexExists;
    }

    /**
     * Build RestClient
     */
    private RestClient getRestClient() {
        RestClientBuilder builder = RestClient.builder(getHttpHost());

        // INFO: Add Basic Auth credentials
        if (elasticsearchBasicAuthUsername != null && elasticsearchBasicAuthUsername.length() > 0) {
            org.apache.http.client.CredentialsProvider credentialsProvider = new org.apache.http.impl.client.BasicCredentialsProvider();
            credentialsProvider.setCredentials(org.apache.http.auth.AuthScope.ANY, new org.apache.http.auth.UsernamePasswordCredentials(elasticsearchBasicAuthUsername, elasticsearchBasicAuthPassword));

            builder.setHttpClientConfigCallback(new HttpClientConfigCallback() {
                @Override
                public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                    return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
            });
        }

        return builder.build();
    }
}
