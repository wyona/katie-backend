package com.wyona.katie.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.handlers.*;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.highlight.TokenSources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Component
public class AIService {

    @Autowired
    private TaxonomyServiceLuceneImpl taxonomyService;

    @Autowired
    private LuceneQuestionAnswerImpl luceneImpl;
    @Autowired
    private KnowledgeGraphQuestionAnswerImpl knowledgeGraphImpl;
    @Autowired
    private ElasticsearchQuestionAnswerImpl elasticsearchImpl;
    @Autowired
    private SentenceBERTQuestionAnswerImpl sbertImpl;
    @Autowired
    private LuceneVectorSearchQuestionAnswerImpl luceneVectorSearchImpl;
    @Autowired
    private LuceneSparseVectorEmbeddingsRetrievalImpl luceneSparseVectorEmbeddingsRetrievalImpl;
    @Autowired
    private WeaviateQuestionAnswerImpl weaviateImpl;
    @Autowired
    private MilvusRetrievalImpl milvusImpl;
    @Autowired
    private QueryServiceQuestionAnswerImpl queryServiceImpl;
    @Autowired
    private KatieQuestionAnswerImpl katieImpl;
    @Autowired
    private AzureAISearchImpl azureAISearchImpl;
    @Autowired
    private LLMQuestionAnswerImpl llmQuestionAnswerImpl;
    @Autowired
    private MCPQuestionAnswerImpl mcpQuestionAnswerImpl;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    //@Autowired
    //private CohereReRank cohereReRank;
    //@Autowired
    //private SentenceBERTreRank sentenceBERTreRank;

    @Autowired
    private XMLService xmlService;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private LuceneVectorSearchHumanFeedbackImpl luceneVectorSearchHumanFeedbackImpl;

    @Autowired
    private CohereGenerate cohereGenerateImpl;

    @Autowired
    private OpenAIEmbeddings openAIImpl;
    @Autowired
    private OpenAIAzureEmbeddings openAIAzureImpl;
    @Autowired
    private CohereEmbeddings cohereEmbeddingsImpl;
    @Autowired
    private AlephAlphaEmbeddings alephAlphaEmbeddingsImpl;
    @Autowired
    //private NumentaEmbeddingsWithPreTokenization numentaEmbeddings; // INFO: With pre tokenization
    private NumentaEmbeddingsWithoutPreTokenization numentaEmbeddings;
    @Autowired
    private GoogleEmbeddings googleEmbeddings;

    @Autowired
    private NamedEntityRecognitionService nerService;

    @Value("${openai.model}")
    private String openAIModel;

    @Value("${cohere.model}")
    private String cohereModel;

    @Value("${aleph-alpha.model}")
    private String alephAlphaModel;

    @Value("${numenta.model}")
    private String numentaModel;

    @Value("${google.model}")
    private String googleModel;

    /**
     * Get answers for a particular question
     * @param question Submitted question, for example 'What is the highest mountain of the world?'
     * @param domain Domain / Knowledge base associated with question
     * @param limit Limit returned hits
     * @return found answers
     */
    protected Hit[] findAnswers(Sentence question, Context domain, int limit) throws Exception {

        // TODO: Do not search for with and without entities, but replace in the index the entities by the keys, e.g. "How old is Michael?", then add to the index "How old is ak-entity:person_name" and search for "How old is ak-entity:person_name"

        // TODO: Consider moving search without and with entities to search implementations

        Hit[] answersReQuestionWithoutEntities = new Hit[0];
        if (question.getAllEntities().length > 0) {
            // INFO: Find answer without named entities, for example question "How old is Vanya Brucker?" is without person entity "How old is?"
            String questionWithoutEntities = question.getSentenceWithoutEntities();
            log.info("Get answers for question without entities: '" + questionWithoutEntities + "'");
            answersReQuestionWithoutEntities = getAnswers(questionWithoutEntities, question.getClassifications(), domain, limit);
            if (answersReQuestionWithoutEntities != null) {
                log.info(answersReQuestionWithoutEntities.length + " answers found for question '" + questionWithoutEntities + "' without entities.");
            } else {
                log.info("No answers found for question '" + questionWithoutEntities + "' without entities.");
            }
        } else {
            log.info("Question '" + question.getSentence() + "' does not contain entities (NER implementation: '" + nerService.getNerImplementation(domain) + "').");
        }

        // INFO: Question "Katerina?" will become "?", which does not make sense, therefore we also get answers for question with entities.
        log.info("Get answers for question with entities: '" + question.getSentence() + "'");
        Hit[] answersForQuestionWithEntities = getAnswers(question, domain, limit);
        if (answersForQuestionWithEntities != null) {
            log.info(answersForQuestionWithEntities.length + " answers found for question with entities.");
        } else {
            log.info("No answers found for question with entities.");
        }

        // TODO: Generate fake / synthetic answer and search for fake / synthetic answer ...
        // WARNING: Performance!!!
        //String fakeAnswer = getFakeAnswer(question, domain);

        Hit[] mergedAnswers = null;
        if (answersReQuestionWithoutEntities != null && answersReQuestionWithoutEntities.length > 0) {
            log.info("Merge answers for question with and without entities...");
            mergedAnswers = mergeAnswers(answersForQuestionWithEntities, answersReQuestionWithoutEntities);
        } else {
            log.info("There are no answers for question without entities, therefore return only answers for question with entities.");
            mergedAnswers = mergeAnswers(answersForQuestionWithEntities, null);
        }

        /* INFO: Moved to QuestionAnsweringService
        if (domain.getReRankAnswers()) {
            if (mergedAnswers.length > 0) {
                mergedAnswers = reRankAnswers(question, domain, mergedAnswers, limit);
            } else {
                log.info("No answers available, therefore nothing to re-rank.");
            }
        }

         */

        /* INFO: Moved to QuestionAnsweringService
        if (domain.getConsiderHumanFeedback()) {
            mergedAnswers = correctByConsideringPreviousHumanFeedback(question, domain, mergedAnswers);
        }

         */

        return mergedAnswers;
    }

    /**
     * Re-rank answers
     * @param question Asked question
     * @param domain Domain associated with question
     * @param answers Found answers
     * @param limit Limit of returned hits, whereas no limit when set to -1
     * @return re-ranked answers
     */
    /*
    private Hit[] reRankAnswers(Sentence question, Context domain, Hit[] answers, int limit) throws Exception {
        log.info("Re-rank " + answers.length + " answers ...");

        List<String> _answers = new ArrayList<String>();
        for (Hit answer: answers) {
            _answers.add(getTextAnswer(answer.getAnswer().getAnswer(), domain));
        }

        ReRankProvider reRankImpl = null;
        if (domain.getReRankImpl().equals(ReRankImpl.SBERT)) {
            reRankImpl = sentenceBERTreRank;
        } else if (domain.getReRankImpl().equals(ReRankImpl.COHERE)) {
            reRankImpl = cohereReRank;
        } else {
            log.error("Re-rank implementation not set!");
            return answers;
        }

        Integer[] reRankedIndex = reRankImpl.getReRankedAnswers(question.getSentence(), _answers.toArray(new String[0]), limit);

        List<Hit> reRankedAnswers = new ArrayList<Hit>();
        for (int i:reRankedIndex) {
            reRankedAnswers.add(answers[i]);
        }

        return reRankedAnswers.toArray(new Hit[0]);
    }

     */

    /**
     * @param answer Either UUID, e.g. "ak-uuid:b0bc5269-ce62-4bd6-848d-23bc1cbd74d9" or actual answer, e.g. "Bern is the capital of Switzerland"
     */
    /*
    private String getTextAnswer(String answer, Context domain) throws Exception {
        if (answer.startsWith(Answer.AK_UUID_COLON)) {
            String uuid = Answer.removePrefix(answer);
            log.info("Get answer with UUID '" + uuid + "' ...");
            Answer _answer = xmlService.parseQuestionAnswer(null, false, domain, uuid, null);

            // INFO: Concatenate question and answer to provide more context
            //answer = _answer.getAnswer();
            answer = _answer.getOriginalquestion() + " " + _answer.getAnswer();
        } else {
            log.info("Answer from AI service already contains actual answer.");
        }

        return Utils.stripHTML(answer).trim();
    }

     */

    /**
     * Correct / adjust found answers by considering previous human feedback
     * @param question Asked question
     * @param domain Domain associated with question
     * @param answers Found answers
     * @return annotated hits / answers based on previous human feedback
     */
    /*
    private Hit[] correctByConsideringPreviousHumanFeedback(Sentence question, Context domain, Hit[] answers) {
        Rating[] ratings = getHumanFeedback(question.getSentence(), domain);

        List<Hit> filteredAnswers = new ArrayList<Hit>();
        for (Hit hit: answers) {
            for (Rating rating: ratings) {
                if (hit.getAnswer().getUuid().equals(rating.getUuid())) {
                    log.info("Question '" + question + "' was asked before and the accuracy of found answer with UUID '" + rating.getUuid() + "' was rated as '" + rating.getRating() + "'");
                    hit.setRating(rating.getRating());
                }
            }
            if (hit.getRating() == 0) {
                log.info("Ignore answer '" + hit.getAnswer().getUuid() + "' and all other answers.");
                return filteredAnswers.toArray(new Hit[0]);
            } else {
                filteredAnswers.add(hit);
            }
        }

        return filteredAnswers.toArray(new Hit[0]);
    }

     */

    /**
     * @param question Asked question
     * @param domain Domain associated with question
     */
    /*
    private Rating[] getHumanFeedback(String question, Context domain) {
        HumanFeedbackHandler humanFeedbackImpl = getHumanFeedbackImpl();
        try {
            return humanFeedbackImpl.getHumanFeedback(question, domain);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new Rating[0];
        }
    }

     */

    /**
     * Index human feedback, such that it can be reused to answer questions in the future
     * @param question Question asked by user
     * @param answerUuid UUID of QnA which was used as answer to the question of user
     * @param domain Domain associated with QnA
     * @param rating Rating of user re answer (0: completely wrong, 10: completely correct)
     * @param user User which asked question. If user anonymous, then null
     */
    protected void indexHumanFeedback(String question, String answerUuid, Context domain, int rating, User user) {
        if (user != null) {
            log.info("The answer with the UUID '" + answerUuid + "' (Domain: " + domain.getId() + ") to the question '" + question + "' was rated by the user '" + user.getUsername() + "' as '" + rating + "'.");
        } else {
            log.info("The answer with the UUID '" + answerUuid + "' (Domain: " + domain.getId() + ") to the question '" + question + "' was rated by an anonymous user as '" + rating + "'.");
        }
        HumanFeedbackHandler humanFeedbackImpl = getHumanFeedbackImpl();
        try {
            humanFeedbackImpl.indexHumanFeedback(question, answerUuid, domain, rating, user);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @param question Question, e.g. "Are there exceptions to the certificate requirement?"
     * @return fake / synthentic answer generated for example by a LLM, e.g. "Yes, there are exceptions for book stores."
     */
    protected String getFakeAnswer(Sentence question, Context domain) {
        // TODO: User domain specific sample QnAs
        String fakeAnswer = cohereGenerateImpl.getFakeAnswer(question.getSentence(), "xlarge");
        log.info("Fake answer: " + fakeAnswer);
        return fakeAnswer;
    }

    /**
     * Get distances (cosine similarity, cosine distance, dot product) between two sentences
     * @param sentenceOne First sentence, e.g. "How old are you?"
     * @param sentenceTwo Second sentence, e.g. "What is your age?" or "How many people live in Rio de Janeiro?"
     * @param embeddingsImpl Embedding service
     * @param apiToken API token of embedding service
     * @param getEmbeddings When set to true, then embeddings will be returned as well
     */
    public Distances getDistancesBetweenSentences(String sentenceOne, String sentenceTwo, EmbeddingsImpl embeddingsImpl, String apiToken, boolean getEmbeddings) throws Exception {

        String model = getEmbeddingModel(embeddingsImpl, true);

        Vector embeddingOne = getSentenceEmbedding(sentenceOne, embeddingsImpl, model, apiToken);
        Vector embeddingTwo = getSentenceEmbedding(sentenceTwo, embeddingsImpl, model, apiToken);

        // INFO: Very simple test
        /*
        float[] embeddingOne = new float[2];
        embeddingOne[0] = 1;
        embeddingOne[1] = 0;
        float[] embeddingTwo = new float[2];
        embeddingTwo[0] = -1;
        embeddingTwo[1] = 1;

         */

        float cosineSimilarity = getCosineSimilarity(((FloatVector)embeddingOne).getValues(), ((FloatVector)embeddingTwo).getValues());
        float cosineDistance = getCosineDistance(cosineSimilarity);
        float dotProduct = UtilsService.getDotProduct(((FloatVector)embeddingOne).getValues(), ((FloatVector)embeddingTwo).getValues()); // TODO: Consider re-using dot product from cosine similarity calculation

        Distances distances = new Distances(cosineSimilarity, cosineDistance, embeddingsImpl, model, embeddingOne.getDimension(), sentenceOne, sentenceTwo, dotProduct);
        if (getEmbeddings) {
            distances.setEmbeddingOne(((FloatVector)embeddingOne).getValues());
            distances.setEmbeddingTwo(((FloatVector)embeddingTwo).getValues());
        }

        return distances;
    }

    /**
     * Get distances (cosine similarity, cosine distance, dot product) between two words
     * @param wordOne First word, e.g. "old"
     * @param wordTwo Second word, e.g. "age" or "person"
     * @param getEmbeddings When set to true, then embeddings will be returned as well
     */
    public DistancesWords getDistancesBetweenWords(String wordOne, String wordTwo, WordEmbeddingImpl embeddingsImpl, String apiToken, boolean getEmbeddings) throws Exception {

        // INFO: https://fasttext.cc/docs/en/crawl-vectors.html
        String model = "cc.en.300.bin"; // TODO: Get model
        //String model = "cc.de.300.bin"; // TODO: Get model

        float[] embeddingOne = getWordEmbedding(wordOne, embeddingsImpl, model, apiToken);
        float[] embeddingTwo = getWordEmbedding(wordTwo, embeddingsImpl, model, apiToken);

        // INFO: Very simple test
        /*
        float[] embeddingOne = new float[2];
        embeddingOne[0] = 1;
        embeddingOne[1] = 0;
        float[] embeddingTwo = new float[2];
        embeddingTwo[0] = -1;
        embeddingTwo[1] = 1;

         */

        float cosineSimilarity = getCosineSimilarity(embeddingOne, embeddingTwo);
        float cosineDistance = getCosineDistance(cosineSimilarity);
        float dotProduct = UtilsService.getDotProduct(embeddingOne, embeddingTwo); // TODO: Consider re-using dot product from cosine similarity calculation

        DistancesWords distances = new DistancesWords(cosineSimilarity, cosineDistance, embeddingsImpl, model, embeddingOne.length, wordOne, wordTwo, dotProduct);
        if (getEmbeddings) {
            distances.setEmbeddingOne(embeddingOne);
            distances.setEmbeddingTwo(embeddingTwo);
        }

        return distances;
    }

    /**
     * Get cosine similarity of two sentences
     * https://en.wikipedia.org/wiki/Cosine_similarity
     * https://de.wikipedia.org/wiki/Kosinus-%C3%84hnlichkeit
     * @return cosine similarity, whereas 1 means exactly same direction , -1 means exactly opposite direction, 0, means orthogonal
     */
    private float getCosineSimilarity(float[] embeddingOne, float[] embeddingTwo) throws Exception {

        return UtilsService.getDotProduct(embeddingOne, embeddingTwo) / (UtilsService.getVectorLength(embeddingOne) * UtilsService.getVectorLength(embeddingTwo));
    }

    /**
     * Also see cosine distance at https://en.wikipedia.org/wiki/Cosine_similarity
     * https://docs.scipy.org/doc/scipy/reference/generated/scipy.spatial.distance.cdist.html
     */
    private float getCosineDistance(float cosineSimilarity) throws Exception {
        return 1 - cosineSimilarity;
    }

    /**
     * @param dense When true, then return dense model, otherwise return sparse model
     * @return embeddings model, e.g. "all-mpnet-base-v2"
     */
    protected String getEmbeddingModel(EmbeddingsImpl embeddingsImpl, boolean dense) {
        String model = null;

        if (embeddingsImpl.equals(EmbeddingsImpl.SBERT)) {
            if (dense) {
                model = sbertImpl.getVersionAndModel().get(sbertImpl.DENSE_MODEL);
            } else {
                model = sbertImpl.getVersionAndModel().get(sbertImpl.SPARSE_MODEL);
            }
        } else if (embeddingsImpl.equals(EmbeddingsImpl.OPENAI) || embeddingsImpl.equals(EmbeddingsImpl.OPENAI_AZURE)) {
            model = openAIModel; // TODO: Make model configurable
        } else if (embeddingsImpl.equals(EmbeddingsImpl.COHERE)) {
            model = cohereModel; // TODO: Make model configurable
        } else if (embeddingsImpl.equals(EmbeddingsImpl.ALEPH_ALPHA)) {
            model = alephAlphaModel; // TODO: Make model configurable
        } else if (embeddingsImpl.equals(EmbeddingsImpl.NUMENTA)) {
            model = numentaModel; // TODO: Make model configurable
        } else if (embeddingsImpl.equals(EmbeddingsImpl.GOOGLE)) {
            model = googleModel; // TODO: Make model configurable
        } else {
            log.error("No such embedding implementation '" + embeddingsImpl + "' supported!");
        }

        return model;
    }

    /**
     * Get embedding for a sentence
     * @param model Model of embeddings implementation
     */
    private Vector getSentenceEmbedding(String sentence, EmbeddingsImpl embeddingsImpl, String model, String apiToken) throws Exception {
        EmbeddingsProvider embeddingsProvider = null;

        if (embeddingsImpl.equals(EmbeddingsImpl.SBERT)) {
            embeddingsProvider = sbertImpl;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.OPENAI)) {
            embeddingsProvider = openAIImpl;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.OPENAI_AZURE)) {
            embeddingsProvider = openAIAzureImpl;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.COHERE)) {
            embeddingsProvider = cohereEmbeddingsImpl;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.ALEPH_ALPHA)) {
            embeddingsProvider = alephAlphaEmbeddingsImpl;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.NUMENTA)) {
            embeddingsProvider = numentaEmbeddings;
        } else if (embeddingsImpl.equals(EmbeddingsImpl.GOOGLE)) {
            embeddingsProvider = googleEmbeddings;
        } else {
            log.error("No such embedding implementation '" + embeddingsImpl + "' supported!");
        }

        // TODO: Also allow int8
        return embeddingsProvider.getEmbedding(sentence, model, EmbeddingType.SEARCH_DOCUMENT, EmbeddingValueType.float32, apiToken);
    }

    /**
     * Get embedding for a word
     * @param model Model of embeddings implementation, e.g. "cc.de.300.bin"
     */
    private float[] getWordEmbedding(String word, WordEmbeddingImpl embeddingsImpl, String model, String apiToken) throws Exception {
        // TODO: Get embedding from fastText REST API

        String embeddingFilePath = "fasttext-embeddings/" + model + "/" + word + ".json";

        InputStream in = null;
        try {
            in = new ClassPathResource(embeddingFilePath).getInputStream();
            float[] embedding = getWordEmbedding(in);
            in.close();
            return embedding;
        } catch (FileNotFoundException e) {
            log.warn("No embedding for word '" + word + "'.");
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     *
     */
    private float[] getWordEmbedding(InputStream in) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(in);
        float[] embedding = new float[rootNode.size()];
        for (int i = 0; i < rootNode.size(); i++) {
            embedding[i] = Float.parseFloat(rootNode.get(i).asText());
        }
        return embedding;
    }

    /**
     * Merge answers, whereas remove duplicated answers
     * @param answersForQuestionWithEntities Answers which were found for question with entities, e.g. "How many people live in Rio de Janeiro?"
     * @param answersReQuestionWithoutEntities Answers which were found for question without entities, e.g. "How many people live in?"
     */
    private Hit[] mergeAnswers(Hit[] answersForQuestionWithEntities, Hit[] answersReQuestionWithoutEntities) {
        List<Hit> allAnswers = new ArrayList<Hit>();

        // TBD: Why are the answers for question with entities more relevant?!
        log.info("We consider answers for question with entities more relevant, therefore we list them first.");

        if (answersForQuestionWithEntities != null) {
            for (int i = 0; i < answersForQuestionWithEntities.length; i++) {
                if (!isDuplicated(answersForQuestionWithEntities[i].getAnswer(), allAnswers.toArray(new Hit[0]))) {
                    log.info("Add answer '" + answersForQuestionWithEntities[i].getAnswer().getUuid() + "' for question with entities.");
                    allAnswers.add(answersForQuestionWithEntities[i]);
                } else {
                    log.info("Do not add answer '" + answersForQuestionWithEntities[i].getAnswer().getUuid() + "', because was already added.");
                }
            }
        }

        if (answersReQuestionWithoutEntities != null) {
            for (int i = 0; i < answersReQuestionWithoutEntities.length; i++) {
                if (!isDuplicated(answersReQuestionWithoutEntities[i].getAnswer(), allAnswers.toArray(new Hit[0]))) {
                    log.info("Add answer '" + answersReQuestionWithoutEntities[i].getAnswer().getUuid() + "' for question without entities.");
                    allAnswers.add(answersReQuestionWithoutEntities[i]);
                } else {
                    log.info("Do not add answer '" + answersReQuestionWithoutEntities[i].getAnswer().getUuid() + "', because was already added.");
                }
            }
        }

        return allAnswers.toArray(new Hit[0]);
    }

    /**
     * Check whether a particular answer is already contained by an existing list of answers
     * @return true when answer is duplicated and false otherwise
     */
    private boolean isDuplicated(Answer answer, Hit[] answers) {
        //log.info("Check whether answer '" + answer.getAnswer() + "' / '" + answer.getUuid() + "' is already contained by existing list of answers ...");
        for (int i = 0; i < answers.length; i++) {
            //log.info("Compare with: " + answers[i].getAnswer().getAnswer() + " / " + answers[i].getAnswer().getUuid());
            if (answer.getUuid() != null && answers[i].getAnswer().getUuid() != null) {
                if (answer.getUuid().equals(answers[i].getAnswer().getUuid())) {
                    return true;
                }
            } else {
                if (answer.getAnswer().equals(answers[i].getAnswer().getAnswer())) {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * Get answers for a question using a specific implementation
     * @param question Question
     * @param classifications Classifications, e.g. "num", "hum"
     * @param domain Domain where actual implementation is configured
     */
    private Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) throws Exception {
        if (question != null && question.length() == 0) {
            log.info("Question is empty, probably because original questions only consists of named entities.");
            return null;
        }

        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(domain.getDetectDuplicatedQuestionImpl());
        return answerQuestionImpl.getAnswers(question, classifications, domain, limit);
    }

    /**
     * Get answers to question
     * @param question Question including entities and including classifications
     */
    private Hit[] getAnswers(Sentence question, Context domain, int limit) throws Exception {
        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(domain.getDetectDuplicatedQuestionImpl());
        log.info("Get answers for question '" + question.getSentence() + "' using implementation '" + domain.getDetectDuplicatedQuestionImpl() + "' ...");
        return answerQuestionImpl.getAnswers(question, domain, limit);
    }

    /**
     * Get implementation to handle human feedback
     */
    private HumanFeedbackHandler getHumanFeedbackImpl() {
        return luceneVectorSearchHumanFeedbackImpl;
    }

    /**
     * Get retrieval implementation to index content and find similar content
     * @param impl Retrieval implementation
     */
    private QuestionAnswerHandler getAnswerQuestionImpl(DetectDuplicatedQuestionImpl impl) {
        log.info("Load retrieval implementation '" + impl + "' ...");
        if (impl.equals(DetectDuplicatedQuestionImpl.ELASTICSEARCH)) {
            return elasticsearchImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.SENTENCE_BERT)) {
            return sbertImpl;
        } else if(impl.equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            return luceneVectorSearchImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.LUCENE_SPARSE_VECTOR_EMBEDDINGS_RETRIEVAL)) {
            return luceneSparseVectorEmbeddingsRetrievalImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.KNOWLEDGE_GRAPH)) {
            return knowledgeGraphImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.WEAVIATE)) {
            return weaviateImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.MILVUS)) {
            return milvusImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.QUERY_SERVICE)) {
            return queryServiceImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.KATIE)) {
            return katieImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.AZURE_AI_SEARCH)) {
            return azureAISearchImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.LLM)) {
            return llmQuestionAnswerImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.MCP)) {
            return mcpQuestionAnswerImpl;
        } else if (impl.equals(DetectDuplicatedQuestionImpl.LUCENE_DEFAULT)) {
            return luceneImpl;
        } else {
            log.warn("No such search implementation '" + impl + "', therefore use '" + luceneImpl.getClass().getName() + "' ...");
            return luceneImpl;
        }
    }

    /**
     * Delete particular QnA
     * @param uuid UUID of QnA, e.g. "93d29be2-0618-4397-98ad-80836ec80a09"
     * @param domain Domain associated with QnAs
     * @return true when QnA was deleted successfully and false otherwise
     */
    public boolean delete(String uuid, Context domain) throws Exception {
        log.info("Delete QnA with UUID '" + uuid + "' ...");

        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(domain.getDetectDuplicatedQuestionImpl());
        return answerQuestionImpl.delete(uuid, domain);
    }

    /**
     * Delete all QnAs from index
     * @param domain Domain associated with QnAs
     */
    public void clean(Context domain) {
        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(domain.getDetectDuplicatedQuestionImpl());
        answerQuestionImpl.deleteTenant(domain);
        try {
            String indexNameOrUrl = answerQuestionImpl.createTenant(domain);
            // TODO: Save indexNameOrUrl, see ContextService#reindex(...)
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Retrain "AI" with a particular QnA
     * @param qna Question and Answer
     * @param domain Domain
     */
    protected void retrain(QnA qna, Context domain, boolean indexAlternativeQuestions) throws Exception {
        log.info("Retrain QnA with UUID '" + qna.getUuid() + "' ...");

        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(domain.getDetectDuplicatedQuestionImpl());
        // TODO: Is replaceTwoOrMoreSpacesBySingleSpace() necessary, because stripHTML also allows to remove multiple spaces
        qna.setAnswer(replaceTwoOrMoreSpacesBySingleSpace(Utils.stripHTML(qna.getAnswer(), false, false)));
        answerQuestionImpl.retrain(qna, domain, indexAlternativeQuestions);

        extractKeywordsAndAddToAutocompletionIndex(qna, domain);
        updateTaxonomyIndex(qna, domain);
    }

    /**
     * Train "AI" with a particular QnA
     * @param qna Question and Answer
     * @param domain Domain
     */
    protected void train(QnA qna, Context domain, boolean indexAlternativeQuestions) throws Exception {
        log.info("Train QnA '" + qna.getQuestion() + "' (UUID: " + qna.getUuid() + ")");

        // INFO: Pre-process content
        qna.setQuestion(utilsService.preProcessQuestion(qna.getQuestion()));
        qna.setAnswer(replaceTwoOrMoreSpacesBySingleSpace(Utils.stripHTML(qna.getAnswer(), false, false)));

        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(domain.getDetectDuplicatedQuestionImpl());
        answerQuestionImpl.train(qna, domain, indexAlternativeQuestions);

        extractKeywordsAndAddToAutocompletionIndex(qna, domain);
        updateTaxonomyIndex(qna, domain);
    }

    /**
     *
     */
    private String replaceTwoOrMoreSpacesBySingleSpace(String text) {
        // INFO: See https://stackoverflow.com/questions/2932392/java-how-to-replace-2-or-more-spaces-with-single-space-in-string-and-delete-lead
        //return text.replaceAll(" +", " ");
        return text.replaceAll("  +", " ");
    }

    /**
     * Train "AI" with several QnAs
     * @param qnas Batch of Questions and Answers
     * @param domain Domain
     * @param indexAlternativeQuestions When set to true, then alternative questions are also indexed
     * @param processId Background process Id
     * @return trained QnAs
     */
    protected QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions, String processId) throws Exception {
        log.info("Train " + qnas.length + " QnAs ...");

        for (int i = 0; i < qnas.length; i++) {
            qnas[i].setAnswer(replaceTwoOrMoreSpacesBySingleSpace(Utils.stripHTML(qnas[i].getAnswer(), false, false)));
        }

        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(domain.getDetectDuplicatedQuestionImpl());

        QnA[] trainedQnAs = answerQuestionImpl.train(qnas, domain, indexAlternativeQuestions);

        for (QnA qna: trainedQnAs) {
            extractKeywordsAndAddToAutocompletionIndex(qna, domain);
            updateTaxonomyIndex(qna, domain);
        }

        return trainedQnAs;
    }

    /**
     *
     */
    private void extractKeywordsAndAddToAutocompletionIndex(QnA qna, Context domain) throws Exception {
        log.info("Extract keywords from content of QnA in order to use for autocomplete / auto-suggest ...");
        //java.util.List<CardKeyword> keywords = KeywordsExtractor.getKeywordsList(getContent(qna), Language.de);
        //domainService.addAutocompletionEntry(domain.getId(), "TODO");

        String text = qna.getQuestion() + " " + qna.getAnswer(); // TODO: What about alternative questions?
        TokenStream stream = TokenSources.getTokenStream(null, null, text, new StandardAnalyzer(), -1);
        stream.reset(); // INFO: See https://lucene.apache.org/core/9_8_0/core/org/apache/lucene/analysis/TokenStream.html
        while (stream.incrementToken()) {
            //log.info("Token: " + stream.getAttribute(CharTermAttribute.class));
        }
        stream.end();
        stream.close();

        log.info("TODO: Update autocomplete / auto-suggest index ...");
    }

    /**
     * Add QnA classifications as taxonomy entries
     */
    private void updateTaxonomyIndex(QnA qna, Context domain) throws Exception {
        log.info("Update taxonomy index ...");
        if (qna.getClassifications() != null) {
            taxonomyService.addEntries(domain, qna.getClassifications().toArray(new String[0]));
        } else {
            log.info("QnA '" + qna.getUuid() + "' has no classifications.");
        }

    }

    /**
     * Create tenant
     * @param domain Domain using AI Service
     * @param retrievalImplementation Retrieval implementation
     * @return base URL or UUID of tenant, e.g. "https://deeppavlov.wyona.com" or "ace1ced2-3dda-40e3-8fb9-cdbf22694051"
     */
    protected String createTenant(Context domain, DetectDuplicatedQuestionImpl retrievalImplementation) throws Exception {
        log.info("Create AI Service tenant (Implementation: " + retrievalImplementation + ") for domain '" + domain.getId() + "' ...");

        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(retrievalImplementation);
        String baseURL = answerQuestionImpl.createTenant(domain);
        log.info("Base URL / UUID of AI Service tenant: " + baseURL);
        return baseURL;
    }

    /**
     * Delete tenant
     * @param domain Domain within Katie
     */
    public void deleteTenant(Context domain) {
        log.info("Delete AI Service tenant ...");

        QuestionAnswerHandler answerQuestionImpl = getAnswerQuestionImpl(domain.getDetectDuplicatedQuestionImpl());
        answerQuestionImpl.deleteTenant(domain);
    }
}
