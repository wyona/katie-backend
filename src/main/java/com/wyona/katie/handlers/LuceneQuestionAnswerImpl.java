package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.queryparser.classic.QueryParser;

import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

/**
 * Lucene based question answer implementation
 */
@Slf4j
@Component
public class LuceneQuestionAnswerImpl implements QuestionAnswerHandler {

    private static final String CONTENTS_FIELD = "question"; // INFO: The field "question" contains beside question also alternative questions and answer
    private static final String PATH_FIELD = "qna_uuid";
    private static final String FEATURE_FIELD = "feature";

    /**
     * Get Lucene version
     * @return lucene version, e.g. "9.8.0"
     */
    public String getVersion() {
        return "" + Version.LATEST;
    }

    /**
     *
     */
    private Analyzer initAnalyzer() {
        return new StandardAnalyzer();
    }

    /**
     * Get directory path containing Lucene index
     * @param domain Domain associated with Lucene index
     * @return absolute directory path containing Lucene index
     */
    private Path getIndexDirPath(Context domain) {
        File indexDir = new File(domain.getContextDirectory(), "lucene-index");
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        return Paths.get(indexDir.getAbsolutePath());
    }

    /**
     *
     */
    private Directory getIndexDirectory(Context domain) throws Exception {
        Path indexPath = getIndexDirPath(domain);
        return FSDirectory.open(indexPath);
    }

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("Lucene implementation: Deleting tenant ...");
        File indexDir = new File(domain.getContextDirectory(), "lucene-index");
        if (indexDir.isDirectory()) {
            log.info("Delete directory recursively: "+ indexDir.getAbsolutePath());
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
    public String createTenant(Context domain) {
        try {
            Analyzer analyzer = initAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(getIndexDirectory(domain), iwc);
            // writer.forceMerge(1);
            writer.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return null; 
    }

    /**
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context domain, boolean indexAlternativeQuestions) {
        log.info("Index QnA: " + qna.getQuestion() + " | " + qna.getUuid());

        String akUuid = Answer.AK_UUID_COLON + qna.getUuid();

        Analyzer analyzer = initAnalyzer();
        String indexedContent = null;
        try {
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(getIndexDirectory(domain), iwc);

            indexedContent = indexQnA(writer, qna.getQuestion(), qna.getAlternativeQuestions(), indexAlternativeQuestions, qna.getAnswer(), akUuid, qna.getAnswerClientSideEncryptionAlgorithm() != null);
            log.info("Indexed content: " + indexedContent);

            // writer.forceMerge(1);
            writer.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            log.info("TODO: Vectorize keywords / terms with word2vec or GloVe or fastText and add to vector index ...");
            if (false) {
                log.info("Get terms of document '" + akUuid + "' ...");
                TokenStream stream = TokenSources.getTokenStream(null, null, indexedContent, analyzer, -1);
                stream.reset(); // INFO: See https://lucene.apache.org/core/9_8_0/core/org/apache/lucene/analysis/TokenStream.html
                while (stream.incrementToken()) {
                    log.info("Token: " + stream.getAttribute(CharTermAttribute.class));
                }
                stream.end();
                stream.close();
            }

            if (false) {
                IndexReader reader = DirectoryReader.open(getIndexDirectory(domain));
                log.info("Get all terms of index '" + getIndexDirectory(domain) + "' ...");
                List<LeafReaderContext> list = reader.leaves();
                for (LeafReaderContext lrc : list) {
                    Terms terms = lrc.reader().terms(CONTENTS_FIELD);
                    if (terms != null) {
                        TermsEnum termsEnum = terms.iterator();
                        BytesRef term = null;
                        while ((term = termsEnum.next()) != null) {
                            log.info("Term: " + term.utf8ToString());
                        }
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
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
     * Add content of QnA to search index or update content of QnA inside search index in case QnA with this uuid was indexed before
     * @param question Question
     * @param alternativeQuestions Alternative questions
     * @param indexAlternativeQuestions When true, then also index alternative questions
     * @param akUuid UUID associated with question, e.g. "ak-uuid:0e3eb6c4-4543-4d73-996b-452583013938"
     * @param answer Answer to question
     * @param answerEncrypted True when answer is encrypted and false when answer not encrypted
     * @return content which got indexed
     */
    private String indexQnA(IndexWriter writer, String question, String[] alternativeQuestions, boolean indexAlternativeQuestions, String answer, String akUuid, boolean answerEncrypted) throws Exception {
        log.info("Index QnA with question '" + question + "' and with uuid '" + akUuid + "' ...");

        Document doc = buildDocument(question, alternativeQuestions, indexAlternativeQuestions, answer, akUuid, answerEncrypted);

        if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
            log.info("Add document with Id '" + akUuid + "' to Lucene index ...");
            writer.addDocument(doc);
        } else {
            log.info("Update document inside Lucene index with Id '" + akUuid + "' ...");
            writer.updateDocument(new Term(PATH_FIELD, akUuid), doc);
        }

        return doc.get(CONTENTS_FIELD).toString();
    }

    /**
     * Build Lucene document
     */
    private Document buildDocument(String question, String[] alternativeQuestions, boolean indexAlternativeQuestions, String answer, String akUuid, boolean answerEncrypted) {
        Document doc = new Document();

        Field pathField = new StringField(PATH_FIELD, akUuid, Field.Store.YES);
        doc.add(pathField);

        doc.add(new LongPoint("modified", new Date().getTime()));

        // INFO: Content to be indexed
        StringBuilder content = new StringBuilder();

        // INFO: Always index question
        if (question != null) {
            content.append(question);
        } else {
            log.info("QnA '" + akUuid + "' has no question yet associated with.");
        }

        // TODO: Consider increasing "multiplier" when not concatenating anymore!

        // TODO: Consider separate field or even separate lucene document for alternative questions
        if (alternativeQuestions != null && alternativeQuestions.length > 0) {
            if (indexAlternativeQuestions) {
                for (String aQuestion : alternativeQuestions) {
                    content.append(" " + aQuestion);
                }
            } else {
                if (alternativeQuestions.length > 0) {
                    log.info("QnA '" + akUuid + "' has " + alternativeQuestions.length + " alternative question(s), but do not index them.");
                }
            }
        }

        // TODO: Consider separate field for answer
        if (answerEncrypted) {
            log.info("Answer of QnA '" + akUuid + "' is encrypted, therefore do not index answer.");
        } else {
            // INFO: Index question and answer together
            content.append(" " + answer);
        }

        doc.add(new TextField(CONTENTS_FIELD, content.toString(), Field.Store.NO));

        if (question != null) {
            List<String> features = getFeatures(question);
            for (String feature : features) {
                // TODO: Replace hard-coded weight
                doc.add(new FeatureField(FEATURE_FIELD, feature, 0.4F));
            }
        } else {
            log.warn("No question available, therefore no features can be extracted.");
        }

        return doc;
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.warn("TODO: Finish implementation to index more than one QnA at the same time!");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        String akUuid = Answer.AK_UUID_COLON + uuid;
        Field pathField = new StringField(PATH_FIELD, akUuid, Field.Store.YES);

        try {
            IndexReader reader = DirectoryReader.open(getIndexDirectory(domain));
            int numberOfDocsBeforeDeleting = reader.numDocs();
            log.info("Number of documents: " + numberOfDocsBeforeDeleting);
            //log.info("Number of deleted documents: " + reader.numDeletedDocs());
            reader.close();

            log.info("Delete document with path '" + akUuid + "' from index of domain '" + domain.getId() + "' ...");
            IndexWriterConfig iwc = new IndexWriterConfig();
            //iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(getIndexDirectory(domain), iwc);
            Term term = new Term(PATH_FIELD, akUuid);
            writer.deleteDocuments(term);
            // writer.forceMerge(1);
            writer.close();

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
    public Hit[] getAnswers(Sentence question, Context context, int limit) {
        log.info("TODO: Consider entities individually and not just the question as a whole!");
        return getAnswers(question.getSentence(), question.getClassifications(), context, limit);
    }

    /**
     * @see QuestionAnswerHandler#getAnswers(String, List, Context, int)
     */
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        log.info("Get answer using Lucene BM25 (Lucene version: " + getVersion() + ") implementation for question '" + question + "' ...");

        List<Hit> answers = new ArrayList<Hit>();

        try {
            IndexReader reader = DirectoryReader.open(getIndexDirectory(domain));
            StoredFields storedFields = reader.storedFields();
            IndexSearcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = initAnalyzer();

            Query query = buildQuery(question, analyzer);

            int top_n_hits = 100; // INFO: Top 100 hits for query
            if (limit > 0) {
                // INFO: The same QnA UUID can be indexed at least three times: question, alternative question, answer
                // We currently concatenate question, alternative questions and answer, therefore the multiplier is 1. Also consider that there could be an arbitrary number of alternative questions, so we might want to set the multiplier even greater than 3 if we do not concatenate anymore
                int multiplier = 1;
                top_n_hits = multiplier * limit;
                log.info("External limit set to " + limit + ", therefore get top " + top_n_hits + " hits (multiplier is " + multiplier + ") ...");
            } else {
                log.info("No external limit set, therefore get top " + top_n_hits + " hits ...");
            }

            log.info("Get results for query '" + query + "' and return top " + top_n_hits + " hits ...");
            TopDocs results = searcher.search(query, top_n_hits);

            // INFO: Init Hightlighter to highlight keywords within returned answer
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(scorer);

            if (results.totalHits.value() > 0) {
                log.info("Total hits found: " + results.totalHits.value());
                log.info("Total score docs: " + results.scoreDocs.length);
                for (ScoreDoc hit : results.scoreDocs) {
                    log.info("Lucene hit: " + hit.toString());
                    //log.info("Lucene Score: " + hit.score);
                    //Document doc = searcher.doc(hit.doc);
                    Document doc = storedFields.document(hit.doc);

                    String akUuid = doc.get(PATH_FIELD);
                    String orgQuestion = null;
                    if (akUuid != null) {
                        log.info("AskKatie UUID: " + akUuid);
                        Date dateAnswered = null;
                        Date dateAnswerModified = null;
                        Date dateOriginalQuestionSubmitted = null;
                        String uuid = Answer.removePrefix(akUuid);
                        String akUuidInsteadActualAnswer = akUuid; // INFO: Id (e.g. "ak-uuid:00928749-f34e-4288-ac12-428087652b95") will be replaced by actual answer at a later stage by QuestionAnsweringService#getFromUUID(Answer, Context)
                        answers.add(new Hit(new Answer(question, akUuidInsteadActualAnswer, null,null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, orgQuestion, dateOriginalQuestionSubmitted, true, null, true, null), hit.score));

                        if (false) { // INFO: Only activate when actual answer gets resolved first (see QuestionAnsweringService#getFromUUID(Answer, Context))
                            // INFO: Query: "Wann kam Vanya auf die Welt?", Answer: "Vanya wurde geboren im Jahr 2001", Fragment: "<B>Vanya</B> wurde geboren im Jahr 2001"
                            String[] frags = highlighter.getBestFragments(analyzer, CONTENTS_FIELD, "Vanya wurde geboren im Jahr 2001", 10);
                            for (String frag : frags) {
                                log.info("Highlighted text fragment: " + frag);
                            }
                        }
                    } else {
                        log.warn("No path");
                    }
                }
            } else {
                log.info("No hits found.");
            }

            reader.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return answers.toArray(new Hit[0]);
    }

    /**
     *
     */
    private Query buildQuery(String question, Analyzer analyzer) throws Exception {
        QueryParser parser = new QueryParser(CONTENTS_FIELD, analyzer);
        String questionEscaped = parser.escape(question); // INFO: Escape slash, question mark, etc. https://stackoverflow.com/questions/17798300/lucene-queryparser-with-in-query-criteria
        //log.info("Escaped question: " + questionEscaped);
        Query questionQuery = parser.parse(questionEscaped);

        List<String> features = getFeatures(questionEscaped);
        if (features.size() > 0) {
            BooleanQuery.Builder bqb = new BooleanQuery.Builder();
            bqb.add(questionQuery, BooleanClause.Occur.SHOULD);
            for (String feature : features) {
                // TODO: Replace hard-coded weight
                bqb.add(new BooleanClause(FeatureField.newLinearQuery(FEATURE_FIELD, feature, 0.3F), BooleanClause.Occur.SHOULD));
                //bqb.add(new TermQuery(new Term(CONTENTS_FIELD, feature)), BooleanClause.Occur.SHOULD);
            }
            BooleanQuery termExpansionQuery = bqb.build();
            log.info("Term expansion query: " + termExpansionQuery);
            return termExpansionQuery;
        } else {
            log.info("Regular query (no term expansion using additional features): " + questionQuery);
            return questionQuery;
        }
    }

    /**
     * Term expansion / Splade (https://www.pinecone.io/learn/splade/)
     * @param text Text, e.g. "Vanya was born in the summer of 2001"
     * @return list of features, e.g. "Brucker", "July"
     */
    private List<String> getFeatures(String text) {
        String _text = text.toLowerCase();

        List<String> features = new ArrayList<String>();
        if (true) { // TODO
            return features;
        }

        // TODO: Do term expansion using a language model like for example BERT
        if (_text.contains("ivan")) {
            features.add("brucker");
        }
        if (_text.contains("michael")) {
            features.add("hannes");
        }
        if (_text.contains("tiefenbrunnen")) {
            features.add("seefeld");
        }
        if (_text.contains("vanya")) {
            features.add("brucker");
        }
        return features;
    }
}
