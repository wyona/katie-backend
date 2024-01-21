package com.wyona.katie.handlers;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.search.documents.indexes.models.SearchField;
import com.azure.search.documents.indexes.models.SearchFieldDataType;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.wyona.katie.models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * https://learn.microsoft.com/en-us/azure/search/
 * https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/search/azure-search-documents/src/samples
 */
@Slf4j
@Component
public class AzureAISearchImpl implements QuestionAnswerHandler {

    @Value("${azure.ai.search.endpoint}")
    private String ENDPOINT;

    @Value("${azure.ai.search.admin.key}")
    private String ADMIN_KEY;

    @Value("${azure.ai.search.query.key}")
    private String QUERY_KEY;

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        String indexName = domain.getAzureAISearchIndexName();
        log.info("Azure AI Search implementation: Delete index '" + indexName + "' ...");
        SearchIndexClient searchIndexClient = new SearchIndexClientBuilder().endpoint(ENDPOINT).credential(new AzureKeyCredential(ADMIN_KEY)).buildClient();
        searchIndexClient.deleteIndex(indexName);

    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        String indexName = "katie" + new Date().getTime();
        //String indexName = "katie" + domain.getId(); // INFO: No dashes permitted by Azure AI Search
        log.info("Azure AI Search implementation: Create index '" + indexName + "' ...");

        // INFO: https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/search/azure-search-documents/src/samples/java/com/azure/search/documents/indexes/CreateIndexExample.java
        List<SearchField> searchFields = Arrays.asList(
                new SearchField("id", SearchFieldDataType.STRING).setKey(true),
                new SearchField("text", SearchFieldDataType.STRING).setSearchable(true)
        );
        SearchIndex searchIndex = new SearchIndex(indexName, searchFields);
        SearchIndexClient searchIndexClient = new SearchIndexClientBuilder().endpoint(ENDPOINT).credential(new AzureKeyCredential(ADMIN_KEY)).buildClient();
        SearchIndex indexFromService = searchIndexClient.createIndex(searchIndex);

        return indexFromService.getName();
        //return indexName;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.info("Index QnA: " + qna.getQuestion() + " | " + qna.getUuid());
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Delete/train is just a workaround, implement retrain by itself");
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public void train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation to index more than one QnA at the same time!");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        // TODO
        return false;
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        log.info("TODO: Consider entities individually and not just the question as a whole!");
        return getAnswers(question.getSentence(), question.getClassifications(), context, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.info("Get answer(s) from Katie implementation for question '" + question + "' ...");

        List<Hit> answers = new ArrayList<Hit>();

        return answers.toArray(new Hit[0]);
    }
}
