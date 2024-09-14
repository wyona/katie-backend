package com.wyona.katie.handlers;

import com.wyona.katie.services.DataRepositoryService;
import com.wyona.katie.services.EmbeddingsService;
import com.wyona.katie.services.LuceneCodecFactory;
import com.wyona.katie.models.*;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;

/**
 * Index and retrieve questions and answers using Lucene Vector Search
 */
@Slf4j
@Component
public class LuceneVectorSearchQuestionAnswerImpl implements QuestionAnswerHandler {

    @Autowired
    private EmbeddingsService embeddingsService;

    @Autowired
    private LuceneCodecFactory luceneCodecFactory;

    @Autowired
    private DataRepositoryService dataRepoService;

    private static final String PATH_FIELD = "qna_uuid";
    private static final String VECTOR_FIELD = "vector";
    private static final String CLASSIFICATION_FIELD = "classification";

    /**
     * Get file system directory path containing Lucene vector index
     * @param domain Domain associated with Lucene index
     * @return absolute directory path containing Lucene vector index
     */
    private String getVectorIndexDir(Context domain) {
        File indexDir = new File(domain.getContextDirectory(), "lucene-vector-index");
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        return indexDir.getAbsolutePath();
    }

    /**
     *
     */
    private Directory getIndexDirectory(Context domain) throws Exception {
        String indexPath = getVectorIndexDir(domain);
        return FSDirectory.open(Paths.get(indexPath));
    }

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("Lucene-Vector-Search implementation of deleting tenant ...");
        File indexDir = new File(domain.getContextDirectory(), "lucene-vector-index");
        if (indexDir.isDirectory()) {
            log.info("Delete directory recursively: " + indexDir.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(indexDir);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) throws Exception {
        IndexWriterConfig iwc = new IndexWriterConfig();
        //iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        iwc.setCodec(luceneCodecFactory.getCodec(domain.getEmbeddingValueType()));

        IndexWriter writer = null;
        try {
            writer = new IndexWriter(getIndexDirectory(domain), iwc);
            // writer.forceMerge(1);
            writer.close();
        } catch (Exception e){
            closeIndexWriter(writer);
            throw e;
        }

        return null; 
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) throws Exception {
        log.info("Index QnA '" + qna.getUuid() + "' as multiple vectors (question, alternative questions, answer) ...");

        String akUuid = Answer.AK_UUID_COLON + qna.getUuid();

        IndexWriterConfig iwc = new IndexWriterConfig();
        //iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
        iwc.setCodec(luceneCodecFactory.getCodec(domain.getEmbeddingValueType()));

        // INFO: https://www.elastic.co/de/blog/what-is-an-apache-lucene-codec
        log.info("Lucene Codec: " + iwc.getCodec());

        IndexWriter writer = null;
        try {
            writer = new IndexWriter(getIndexDirectory(domain), iwc);

            // INFO: Index question
            if (qna.getQuestion() != null) {
                log.info("Index question ...");
                Vector vector = indexTextAsVector(writer, qna.getQuestion(), qna.getClassifications(), akUuid, domain);
                saveEmbedding(vector, qna.getUuid(), qna.getQuestion(), domain,"question");
            } else {
                log.info("QnA '" + qna.getUuid() + "' has no question yet associated with.");
            }

            // INFO: Index alternative questions
            log.debug("Number of alternative questions: " + qna.getAlternativeQuestions().length);
            if (indexAlternativeQuestions) {
                int counter = 0;
                for (String aQuestion : qna.getAlternativeQuestions()) {
                    counter++;
                    log.info("Index alternative question '" + aQuestion + "' ...");
                    Vector vector = indexTextAsVector(writer, aQuestion, qna.getClassifications(), akUuid, domain);
                    saveEmbedding(vector, qna.getUuid(), aQuestion, domain, "alternative_q_" + counter);
                }
            } else {
                if (qna.getAlternativeQuestions().length > 0) {
                    log.info("QnA '" + qna.getUuid() + "' has " + qna.getAlternativeQuestions().length + " alternative question(s), but do not index them.");
                }
            }

            // INFO: Index answer
            if (qna.getAnswerClientSideEncryptionAlgorithm() == null) {
                log.info("Index answer ...");
                Vector vector = indexTextAsVector(writer, qna.getAnswer(), qna.getClassifications(), akUuid, domain);
                saveEmbedding(vector, qna.getUuid(), qna.getAnswer(), domain, "answer");
            } else {
                log.info("Answer of QnA '" + qna.getUuid() + "' is encrypted, therefore do not index answer.");
            }

            // TODO: Also consider indexing question and answer together

            // writer.forceMerge(1);
            writer.close();
        } catch (Exception e) {
            log.error("Training of QnA '" + qna.getUuid() + "' of domain '" + domain.getId() + "' failed because of the following error: " + e.getMessage());
            //log.error("Training of QnA '" + qna.getUuid() + "' of domain '" + domain.getId() + "' failed because of the following error: " + e.getMessage(), e);
            closeIndexWriter(writer);
            throw e;
        }
    }

    /**
     * Save embedding vector persistently
     * @param vector Embedding vector
     * @param uuid UUID of QnA
     * @param text Embedded text
     * @param fieldName Field associated with text, e.g. "question" or "answer"
     */
    private void saveEmbedding(Vector vector, String uuid, String text, Context domain, String fieldName) {
        if (true) { // TODO: Make configurable
            log.info("Do not save embedding");
            return;
        }
        log.info("Save embedding ...");

        File embeddingDir = domain.getQnAEmbeddingsPath(uuid);
        if (!embeddingDir.isDirectory()) {
            embeddingDir.mkdirs();
        }

        File file = new File(embeddingDir, fieldName + ".json");
        dataRepoService.saveEmbedding(vector, text, file);
    }

    /**
     * Index text as vector
     * @param writer Lucene Index Writer
     * @param text Text, e.g. question "Was mache ich, wenn mich jemand bedroht?"
     * @param classifications Classifications, e.g. "num", "date", "count", "hum", "instruction", "code", "config"
     * @param akUuid Answer.AK_UUID_COLON + uuid
     * @param domain Katie Domain associated with QnA
     * @return embedding vector
     */
    private Vector indexTextAsVector(IndexWriter writer, String text, List<String> classifications, String akUuid, Context domain) throws Exception {
        Document doc = new Document();

        Field pathField = new StringField(PATH_FIELD, akUuid, Field.Store.YES);
        doc.add(pathField);

        // TODO: Check input sequence length and log warning when text is too long:
        // https://www.sbert.net/examples/applications/computing-embeddings/README.html#input-sequence-length
        // https://docs.cohere.ai/docs/embeddings#how-embeddings-are-obtained
        Vector vector = null;
        try {
            vector = embeddingsService.getEmbedding(text, domain, EmbeddingType.SEARCH_DOCUMENT, domain.getEmbeddingValueType());
        } catch (Exception e) {
            log.error("Get embedding failed for text '" + text + "', therefore do not add embedding to Lucene vector index of domain '" + domain.getId() + "'.");
            throw e;
        }

        log.info("Add vector with " + vector.getDimension() + " dimensions to Lucene index ...");

        // TODO: Lucene 9.8.0 does not support anymore overriding the vector length with a custom field type
        // See "workaround" desribed by Uwe Schindler https://lists.apache.org/thread/kckbdqj4g1g9k2tl19x1y1ocndpzd0td
        Field vectorField = null;
        if (domain.getEmbeddingValueType() == EmbeddingValueType.int8) {
            FieldType vectorFieldType = new CustomVectorFieldType(vector.getDimension(), domain.getVectorSimilarityMetric(), VectorEncoding.BYTE);
            vectorField = new KnnByteVectorField(VECTOR_FIELD, ((ByteVector)vector).getValues(), vectorFieldType);
        } else {
            FieldType vectorFieldType = new CustomVectorFieldType(vector.getDimension(), domain.getVectorSimilarityMetric(), VectorEncoding.FLOAT32);
            vectorField = new KnnFloatVectorField(VECTOR_FIELD, ((FloatVector)vector).getValues(), vectorFieldType);
        }
        doc.add(vectorField);

        if (classifications != null && classifications.size() > 0) {
            // TODO: Consider using FacetField instead of StringField or KeywordField: https://lists.apache.org/thread/ygp9x7nws6vkod9bdlhcgsj1gnhq7x8n
            /*
            //DirectoryTaxonomyWriter taxonomyWriter = null;
            //taxonomyWriter.close();
            // https://github.com/apache/lucene/blob/main/lucene/demo/src/java/org/apache/lucene/demo/facet/SimpleFacetsExample.java
            // https://www.tabnine.com/code/java/classes/org.apache.lucene.facet.FacetField
            // https://norconex.com/facets-with-lucene/
            for (String classification : classifications) {
                doc.add(new SortedSetDocValuesFacetField(CLASSIFICATION_FIELD, classification));
                //Field f = new org.apache.lucene.facet.FacetField(CLASSIFICATION_FIELD, classification);
            }
            FacetsConfig facetsConfig = new FacetsConfig();
            writer.addDocument(facetsConfig.build(doc));
             */
            for (String classification : classifications) {
                log.info("Classification: " + classification);

                //Field classificationField = new FacetField(CLASSIFICATION_FIELD, classification);

                // WARN: Backwards compatibility! Requires reindexing from scratch because "cannot change field "classification" from doc values type=NONE to inconsistent doc values type=SORTED_SET"
                // INFO: See Adrien Grand's feedback re KeywordField: https://lists.apache.org/thread/ygp9x7nws6vkod9bdlhcgsj1gnhq7x8n
                //Field classificationField = new KeywordField(CLASSIFICATION_FIELD, classification, Field.Store.YES);

                Field classificationField = new StringField(CLASSIFICATION_FIELD, classification, Field.Store.YES);
                doc.add(classificationField);
            }
            writer.addDocument(doc);
        } else {
            log.info("No classification provided.");
            writer.addDocument(doc);
        }

        return vector;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public void train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) throws Exception {
        log.warn("TODO: Improve performance of batch indexing!");
        int counter = 0;
        for (QnA qna: qnas) {
            try {
                train(qna, domain, indexAlternativeQuestions);
                counter++;
            } catch(Exception e) {
                Exception ee = new Exception(counter + " QnAs of " + qnas.length + " trained, but training of QnA '" + qna.getUuid() + "' failed, because of the following error: " + e.getMessage());
                ee.setStackTrace(e.getStackTrace());
                throw ee;
            }
        }
    }

    /**
     * @see QuestionAnswerHandler#retrain(QnA, Context, boolean)
     */
    public void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) throws Exception {
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
        String akUuid = Answer.AK_UUID_COLON + uuid;

        try {
            IndexReader reader = DirectoryReader.open(getIndexDirectory(domain));
            int numberOfDocsBeforeDeleting = reader.numDocs();
            log.info("Number of documents: " + numberOfDocsBeforeDeleting);
            //log.info("Number of deleted documents: " + reader.numDeletedDocs());
            reader.close();

            log.info("Delete document with path '" + akUuid + "' from index of domain '" + domain.getId() + "' ...");
            IndexWriterConfig iwc = new IndexWriterConfig();
            //iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            iwc.setCodec(luceneCodecFactory.getCodec(domain.getEmbeddingValueType()));

            IndexWriter writer = null;
            try {
                writer = new IndexWriter(getIndexDirectory(domain), iwc);
                Term term = new Term(PATH_FIELD, akUuid);
                writer.deleteDocuments(term);
                // writer.forceMerge(1);
                writer.close();
            } catch (Exception e){
                closeIndexWriter(writer);
                throw e;
            }

            reader = DirectoryReader.open(getIndexDirectory(domain));
            int numberOfDocsAfterDeleting = reader.numDocs();
            log.info("Number of documents: " + numberOfDocsAfterDeleting);
            log.info("Number of deleted documents: " + (numberOfDocsBeforeDeleting - numberOfDocsAfterDeleting));
            // TODO: Not sure whether the method numDeletedDocs() makes sense here
            //log.info("Number of deleted documents: " + reader.numDeletedDocs());
            reader.close();

            return true;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(Sentence, Context, int)
     */
    public Hit[] getAnswers(Sentence question, Context context, int limit) throws Exception {
        log.info("TODO: Consider using entities!");
        return getAnswers(question.getSentence(), question.getClassifications(), context, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) throws Exception {
        log.info("Get answer using Lucene Vector Search (Lucene version: " + Version.LATEST + ") implementation for question '" + question + "' ...");

        try {
            // TODO: Make BooleanClause.Occur configurable (see https://lucene.apache.org/core/9_7_0/core/org/apache/lucene/search/BooleanClause.Occur.html)
            return getAnswersFromVectorIndex(question, classifications, BooleanClause.Occur.MUST, domain, limit);
            //return getAnswersFromVectorIndex(question, classifications, BooleanClause.Occur.SHOULD, domain, limit);
        } catch (Exception e) {
            //log.error(e.getMessage(), e);
            //return null;
            throw e;
        }
    }

    /**
     * Get answers from vector index
     * @param question Questiom, resp. search query
     * @param classifications Classification, e.g. "num", "date", "count", "hum", "instruction"
     * @param occur See https://lucene.apache.org/core/9_7_0/core/org/apache/lucene/search/BooleanClause.Occur.html (SHOULD corresponds with OR and MUST corresponds with AND)
     */
    private Hit[] getAnswersFromVectorIndex(String question, List<String> classifications, BooleanClause.Occur occur, Context domain, int limit) throws Exception {
        List<Hit> answers = new ArrayList<Hit>();

        log.info("Get embedding for question ...");
        Vector queryVector = null;
        try {
            queryVector = embeddingsService.getEmbedding(question, domain, EmbeddingType.SEARCH_QUERY, domain.getEmbeddingValueType());
        } catch (Exception e) {
            log.error("Get embedding failed, therefore do not search for answers.");
            //log.error(e.getMessage(), e);
            //return answers.toArray(new Hit[0]);
            throw e;
        }

        // WARN: https://lists.apache.org/thread/cjxr03p74onb6fc2rb0hgjogtj74fncx
        //int k = 30; // INFO: The default number of documents to find
        int k = 100;
        //if (limit > 0) {
        if (false) {
            // INFO: The same QnA UUID can be indexed at least three times: question, alternative question, answer
            // Whereas there could be an arbitrary number of alternative questions, so we might want to set the multiplier even greater than 3
            int multiplier = 3;
            k = multiplier * limit;
            log.info("External limit set to " + limit + ", therefore get " + k + " (" + multiplier + " times " + limit + ") nearest neighbours ...");
        } else {
            log.info("No external limit set, therefore get " + k + " nearest neighbours ...");
        }

        try {
            IndexReader indexReader = DirectoryReader.open(getIndexDirectory(domain));
            StoredFields storedFields = indexReader.storedFields();
            IndexSearcher searcher = new IndexSearcher(indexReader);

            Query query = null;
            if (classifications != null && classifications.size() > 0) {
                /*
                Collection<BytesRef> terms = new ArrayList<BytesRef>();
                for (int i = 0; i < classifications.size(); i++) {
                    log.info("Filter by classification: " + classifications.get(i));
                    terms.add(new BytesRef(classifications.get(i)));
                }
                Query filter = new TermInSetQuery(CLASSIFICATION_FIELD, terms);
                 */
                BooleanQuery.Builder bqb = new BooleanQuery.Builder();
                for (String classification : classifications) {
                    // INFO: Classifications are saved as lower case, whereas see ContextService#addClassification()
                    if (!classification.trim().isEmpty()) {
                        bqb.add(new TermQuery(new Term(CLASSIFICATION_FIELD, classification.toLowerCase())), occur);
                    }
                }
                Query filter = new ConstantScoreQuery(bqb.build());

                log.info("Filter applied before the vector search: " + filter);
                if (domain.getEmbeddingValueType() == EmbeddingValueType.int8) {
                    query = new KnnByteVectorQuery(VECTOR_FIELD, ((ByteVector)queryVector).getValues(), k, filter);
                } else {
                    query = new KnnFloatVectorQuery(VECTOR_FIELD, ((FloatVector)queryVector).getValues(), k, filter);
                }
            } else {
                log.info("No classification provided, therefore no pre-filtering applied.");
                if (domain.getEmbeddingValueType() == EmbeddingValueType.int8) {
                    query = new KnnByteVectorQuery(VECTOR_FIELD, ((ByteVector)queryVector).getValues(), k);
                } else {
                    query = new KnnFloatVectorQuery(VECTOR_FIELD, ((FloatVector)queryVector).getValues(), k);
                }
            }

            TopDocs topDocs = searcher.search(query, k);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = storedFields.document(scoreDoc.doc);
                String path = doc.get(PATH_FIELD);
                log.info("Vector found with UUID '" + path + "' and confidence score (" + domain.getVectorSimilarityMetric() + ") '" + scoreDoc.score + "'.");
                String _question = null; // doc.get(CONTENTS_FIELD);
                Date dateAnswered = null;
                Date dateAnswerModified = null;
                Date dateOriginalQuestionSubmitted = null;
                String uuid = Answer.removePrefix(path);

                double SIMILARITY_THRESHOLD = 0.67; // TODO
                //if (scoreDoc.score > SIMILARITY_THRESHOLD) {
                if (true) {
                    answers.add(new Hit(new Answer(question, path, null, null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, _question, dateOriginalQuestionSubmitted, true, null, true, null), scoreDoc.score));
                } else {
                    log.info("Confidence score " + scoreDoc.score + " (" +  domain.getVectorSimilarityMetric() + ") is below similarity threshold " + SIMILARITY_THRESHOLD + "!");
                }
            }
            indexReader.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return answers.toArray(new Hit[0]);
    }

    /**
     *
     */
    private void closeIndexWriter(IndexWriter writer) {
        if (writer != null) {
            log.error("Something went wrong, but always make sure to close index writer ...");
            try {
                writer.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}

/**
 * Vector field type which overrides MAX_DIMENSIONS check. See https://markmail.org/message/kf4nzoqyhwacb7ri
 */
@Slf4j
class CustomVectorFieldType extends FieldType {

    private int dimension;
    private VectorSimilarityFunction vectorSimilarityFunction;
    private VectorEncoding vectorEncoding;

    /**
     *
     */
    public CustomVectorFieldType(int dimension, VectorSimilarityFunction vectorSimilarityFunction, VectorEncoding vectorEncoding) {
        this.dimension = dimension;
        this.vectorSimilarityFunction = vectorSimilarityFunction;
        this.vectorEncoding = vectorEncoding;
    }

    @Override
    public int vectorDimension() {
        log.info("Vector dimension: " + dimension);
        return dimension;
    }

    @Override
    public VectorEncoding vectorEncoding() {
        return vectorEncoding;
    }

    @Override
    public VectorSimilarityFunction vectorSimilarityFunction() {
        return vectorSimilarityFunction;
    }
}
