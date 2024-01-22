package com.wyona.katie.handlers;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.search.documents.indexes.models.IndexDocumentsBatch;
import com.azure.search.documents.indexes.models.SearchField;
import com.azure.search.documents.indexes.models.SearchFieldDataType;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.models.SearchResult;
import com.wyona.katie.models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * https://learn.microsoft.com/en-us/azure/search/
 * https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/search/azure-search-documents/src/samples
 */
@Slf4j
@Component
public class AzureAISearchImpl implements QuestionAnswerHandler {

    private static final String ID = "id";
    private static final String TEXT = "text";

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        String indexName = domain.getAzureAISearchIndexName();
        log.info("Azure AI Search implementation: Delete index '" + indexName + "' ...");
        SearchIndexClient searchIndexClient = getSearchIndexClient(domain);
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
                new SearchField(ID, SearchFieldDataType.STRING).setKey(true),
                new SearchField(TEXT, SearchFieldDataType.STRING).setSearchable(true)
        );
        SearchIndex searchIndex = new SearchIndex(indexName, searchFields);
        SearchIndexClient searchIndexClient = getSearchIndexClient(domain);
        SearchIndex indexFromService = searchIndexClient.createIndex(searchIndex);

        return indexFromService.getName();
        //return indexName;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        // INFO: https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/search/azure-search-documents#adding-documents-to-your-index
        log.info("Index QnA: " + qna.getQuestion() + " | " + qna.getUuid());

        IndexDocumentsBatch<AzureAISearchDoc> batch = new IndexDocumentsBatch<>();
        batch.addUploadActions(Collections.singletonList(new AzureAISearchDoc().setId(qna.getUuid()).setText(qna.getQuestion() + " " + qna.getAnswer())));
        SearchClient searchClient = getSearchClient(domain);
        searchClient.indexDocuments(batch);
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
        log.info("Delete QnA '" + uuid + "' ...");
        SearchClient searchClient = getSearchClient(domain);

        //IndexDocumentsBatch<AzureAISearchDoc> batch = new IndexDocumentsBatch<>();
        //batch.addDeleteActions(Collections.singletonList(new AzureAISearchDoc().setId(uuid)));
        //searchClient.indexDocuments(batch);

        Iterable<AzureAISearchDoc> docs = Collections.singletonList(new AzureAISearchDoc().setId(uuid));
        searchClient.deleteDocuments(docs);
        return true;
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

        // INFO: https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/search/azure-search-documents#querying
        SearchClient searchClient = getSearchClient(domain);
        for (SearchResult result : searchClient.search(question)) {
            SearchDocument doc = result.getDocument(SearchDocument.class);
            String id = (String) doc.get(ID);
            String text = (String) doc.get(TEXT);
            double score = 0; // TODO

            log.info("Katie Id of found document: " + id);

            String orgQuestion = null;
            Date dateAnswered = null;
            Date dateAnswerModified = null;
            Date dateOriginalQuestionSubmitted = null;

            String _answer = Answer.AK_UUID_COLON + id;
            ContentType answerContentType = null;

            String uuid = id;
            Answer answer = new Answer(question, _answer, answerContentType,null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, orgQuestion, dateOriginalQuestionSubmitted, true, null, true, null);
            answers.add(new Hit(answer, score));
        }

        return answers.toArray(new Hit[0]);
    }

    /**
     *
     */
    private SearchClient getSearchClient(Context domain) {
        return new SearchClientBuilder().endpoint(domain.getAzureAISearchEndpoint()).credential(new AzureKeyCredential(domain.getAzureAISearchAdminKey())).indexName(domain.getAzureAISearchIndexName()).buildClient();
    }

    /**
     *
     */
    private SearchIndexClient getSearchIndexClient(Context domain) {
        return new SearchIndexClientBuilder().endpoint(domain.getAzureAISearchEndpoint()).credential(new AzureKeyCredential(domain.getAzureAISearchAdminKey())).buildClient();
    }
}
