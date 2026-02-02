package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.*;

import com.wyona.katie.models.Error;
import com.wyona.katie.services.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

/**
 * Benchmark / Quality Assurance controller
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/benchmark")
public class BenchmarkController {

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private AIService aiService;
    
    @Autowired
    private BenchmarkService bmService;

    @Value("${datasets.data_path}")
    private String datasetsDataPath;

    @Value("${benchmarks.data_path}")
    private String benchmarksDataPath;

    @Value("${lucene.vector.search.embedding.impl}")
    private EmbeddingsImpl embeddingsDefaultImpl;

    /**
     * REST interface to ask questions which have an answer inside domain / knowledge base
     */
    @RequestMapping(value = "/accuracy-true-positives", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Ask questions which have an answer inside domain / knowledge base")
    public ResponseEntity<?> getAccuracyTruePositives(
            @Parameter(name = "domainId", description = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            HttpServletRequest request) {

        if (!contextService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        try {
            log.info("Test questions which have an answer inside domain / knowledge base ...");
            TestReport report = qaService.getAccuracyTruePositives(domainId);

            return new ResponseEntity<>(report, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to ask questions which do not have an answer inside domain / knowledge base
     */
    @RequestMapping(value = "/accuracy-true-negatives", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Ask questions which do not have an answer inside domain / knowledge base")
    public ResponseEntity<?> getAccuracyTrueNegatives(
            @Parameter(name = "domainId", description = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @RequestBody String[] questions,
            HttpServletRequest request) {

        if (!contextService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        try {
            log.info("Test questions which do not have an answer inside domain / knowledge base ...");
            TestReport report = qaService.getAccuracyTrueNegatives(domainId, questions);

            return new ResponseEntity<>(report, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get Precision (Number retrieved of correct resp. relevant answers divided by total number of retrieved answers) and Recall (Number of retrieved correct resp. relevant answers divided by total number of relevant answers)
     * https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)#Precision
     */
    @RequestMapping(value = "/precision-recall", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Get Precision (Number of retrieved correct resp. relevant answers divided by total number of retrieved answers) and Recall (Number of retrieved correct resp. relevant answers divided by total number of relevant answers)")
    public ResponseEntity<?> getPrecisionAndRecall(
            @Parameter(name = "domainId", description = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            //@RequestBody String[] questionsAndRelevantUuids, // TODO: Add expected UUIDs of relevant answers
            HttpServletRequest request) {

        if (!contextService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        try {
            Context domain = contextService.getDomain(domainId);

            Answer[] qnas = contextService.getTrainedQnAs(domain, -1, -1);
            List<BenchmarkQuestion> questionsAndRelevantUuids = new ArrayList<BenchmarkQuestion>();
            for (Answer qna : qnas) {
                if (qna.getOriginalquestion() != null) {
                    BenchmarkQuestion originalQuestion = new BenchmarkQuestion();
                    originalQuestion.setQuestion(qna.getOriginalquestion());
                    originalQuestion.addRelevantUuid(qna.getUuid());
                    if (qna.getClassifications() != null) {
                        // TODO: Add classifications
                    }
                    questionsAndRelevantUuids.add(originalQuestion);
                } else {
                    log.info("QnA '" + qna.getUuid() + "' has no original question.");
                }

                if (qna.getAlternativequestions() != null) {
                    for (String aq : qna.getAlternativequestions()) {
                        BenchmarkQuestion altQuestion = new BenchmarkQuestion();
                        altQuestion.setQuestion(aq);
                        altQuestion.addRelevantUuid(qna.getUuid());
                        if (qna.getClassifications() != null) {
                            // TODO: Add classifications
                        }
                        questionsAndRelevantUuids.add(altQuestion);
                    }
                } else {
                    log.info("QnA '" + qna.getUuid() + "' has no alternative questions.");
                }
            }

            BenchmarkPrecision report = qaService.getAccuracyAndPrecisionAndRecallBenchmark(domain.getId(), questionsAndRelevantUuids.toArray(new BenchmarkQuestion[0]), 2, -1, null);

            return new ResponseEntity<>(report, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get confidence score whether two sentences are similar
     * https://txt.cohere.ai/what-is-similarity-between-sentences/
     */
    @RequestMapping(value = "/similarity-sentences", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Get confidence score whether two sentences are similar", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> getSentencesSimilarity(
            @Parameter(name = "domainId", description = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @Parameter(name = "sentence-one", description = "Sentence One, e.g. 'How old are you'",required = true)
            @RequestParam(value = "sentence-one", required = true) String sentenceOne,
            @Parameter(name = "sentence-two", description = "Sentence Two, e.g. 'What is your age'",required = true)
            @RequestParam(value = "sentence-two", required = true) String sentenceTwo,
            @Parameter(name = "embeddings-impl", description = "Embeddings implementation / provider", required = true)
            @RequestParam(value = "embeddings-impl", required = true) EmbeddingsImpl embeddingsImpl,
            @Parameter(name = "get-embeddings", description = "When set to true, then embeddings will be returned as well", required = false, schema = @Schema(defaultValue = "false"))
            @RequestParam(value = "get-embeddings", required = false) Boolean getEmbeddings,
            HttpServletRequest request) {

        if (!contextService.isAuthorized(domainId, request, "/similarity-sentences", JwtService.GET_SENTENCE_SIMILARITY)) {
            log.warn("Not authorized to get similarity of sentences!");
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        // TODO: Billing per domain for embeddings, e.g. Cohere, OpenAI, Aleph Alpha, ...

        try {
            Context domain = contextService.getContext(domainId);

            boolean _getEmbeddings = false;
            if (getEmbeddings != null) {
                _getEmbeddings = getEmbeddings;
            }

            Distances distances = aiService.getDistancesBetweenSentences(sentenceOne, sentenceTwo, embeddingsImpl, getApiToken(embeddingsImpl, domain), _getEmbeddings);

            return new ResponseEntity<>(distances, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get confidence score whether two words are similar
     */
    @RequestMapping(value = "/similarity-words", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Get confidence score whether two words are similar")
    public ResponseEntity<?> getWordsSimilarity(
            @Parameter(name = "domainId", description = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @Parameter(name = "word-one", description = "Word One, e.g. 'old'",required = true)
            @RequestParam(value = "word-one", required = true) String wordOne,
            @Parameter(name = "word-two", description = "Word Two, e.g. 'age'",required = true)
            @RequestParam(value = "word-two", required = true) String wordTwo,
            @Parameter(name = "embeddings-impl", description = "Embeddings implementation / provider",required = true)
            @RequestParam(value = "embeddings-impl", required = true) WordEmbeddingImpl embeddingsImpl,
            @Parameter(name = "get-embeddings", description = "When set to true, then embeddings will be returned as well (false by default)", required = false)
            @RequestParam(value = "get-embeddings", required = false) Boolean getEmbeddings,
            HttpServletRequest request) {

        if (!contextService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        // TODO: Billing per domain for embeddings, e.g. Cohere, OpenAI, Aleph Alpha, ...

        try {
            Context domain = contextService.getContext(domainId);

            boolean _getEmbeddings = false;
            if (getEmbeddings != null) {
                _getEmbeddings = getEmbeddings;
            }

            DistancesWords distances = aiService.getDistancesBetweenWords(wordOne, wordTwo, embeddingsImpl, null, _getEmbeddings);

            return new ResponseEntity<>(distances, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     *
     */
    private String getApiToken(EmbeddingsImpl embeddingsImpl, Context domain) {
        if (embeddingsImpl.equals(domain.getEmbeddingsImpl())) {
            return domain.getEmbeddingsApiToken();
        } else {
            log.warn("Use embeddings implementation API token provided by Katie ...");
            return contextService.getApiToken(embeddingsImpl);
        }
    }

    /**
     * REST interface to run a full benchmark and create evaluation graphs
     * 
     */
    @RequestMapping(value = "/run-benchmark", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Run an extensive benchmark for every currently active search system returning accuracy, precision, recall and average performance time", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> performBenchmark(
            @Parameter(name = "email", description = "E-Mail to get notification when benchmark is completed",required = false)
            @RequestParam(value = "email", required = false) String email,
            @Parameter(name = "index-alternative-questions", description = "Also index alternative questions when set to true", required = true, schema = @Schema(defaultValue = "false"))
            @RequestParam(value = "index-alternative-questions", required = true) Boolean indexAlternativeQuestions,
            @Parameter(name = "re-rank-answers", description = "Re-rank answers when set to true", required = false, schema = @Schema(defaultValue = "false"))
            @RequestParam(value = "re-rank-answers", required = false) Boolean reRankAnswers,
            @Parameter(name = "throttle-time", description = "Throttle time in milliseconds",required = false)
            @RequestParam(value = "throttle-time", required = false) Integer customThrottleTimeInMilis,
            @Parameter(name = "delete-domain", description = "When set to true, then delete domain which was created to run benchmark", required = false , schema = @Schema(defaultValue = "false"))
            @RequestParam(value = "delete-domain", required = false) Boolean deleteDomain,
            // TODO: Add description for file, e.g. "Test Dataset" and if not provided, then a default test dataset is being used
            @RequestPart(name = "file", required = false) MultipartFile file, // INFO: When no dataset is provided, then a default set is used
            @Parameter(name = "search-implementations", description = "Comma separated list of search implementations to be benchmarked: LUCENE_DEFAULT, SENTENCE_BERT, LUCENE_VECTOR_SEARCH, WEAVIATE, ELASTICSEARCH",required = true)
            @RequestParam(value = "search-implementations", required = true) String searchImplementations,
            //@Parameter(name = "benchmark_config", value = "Benchmark configuration", required = true)
            //@RequestPart(name = "benchmark_config", required = true) BenchmarkConfiguration benchmarkConfiguration,
            @RequestPart(name = "benchmark_config", required = false) MultipartFile benchmark_config,
            HttpServletRequest request) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            if (!contextService.isAdmin() && !contextService.hasRole(Role.BENCHMARK)) {
                log.error("Access denied, because user has neither role " + Role.ADMIN + " nor role " + Role.BENCHMARK + "!");
                return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }

            int throttleTimeInMillis = -1; // INFO: No throttling
            if (customThrottleTimeInMilis != null && customThrottleTimeInMilis > 0) {
                throttleTimeInMillis = customThrottleTimeInMilis;
            }

            if (reRankAnswers == null) {
                reRankAnswers = false;
            }

            if (deleteDomain == null) {
                deleteDomain = true;
            }

            ObjectMapper objectMapper = getObjectMapper();

            InputStream in = getDatasetAsInputStream(file);
            CustomBenchmarkDataset bmDataset = objectMapper.readValue(in, CustomBenchmarkDataset.class);
            in.close();

            String processId = UUID.randomUUID().toString();
            User user = iamService.getUser(false, false);

            // INFO: List of search implementations to be benchmarked
            List<RetrievalConfiguration> systemsToBenchmark = new ArrayList<>();

            String[] searchImpls = searchImplementations.split(",");
            for (String searchImpl : searchImpls) {
                RetrievalConfiguration retrievalConfig = new RetrievalConfiguration();
                retrievalConfig.setRetrievalImpl(DetectDuplicatedQuestionImpl.valueOf(searchImpl.trim()));

                if (retrievalConfig.getRetrievalImpl() == DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH) {
                    retrievalConfig.setEmbeddingImpl(embeddingsDefaultImpl);
                    retrievalConfig.setEmbeddingEndpoint(null); // INFO: The default implementations have their endpoints configured already
                    retrievalConfig.setEmbeddingAPIToken(contextService.getApiToken(embeddingsDefaultImpl));
                    retrievalConfig.setEmbeddingValueType(EmbeddingValueType.float32);
                } else if (retrievalConfig.getRetrievalImpl() == DetectDuplicatedQuestionImpl.LUCENE_SPARSE_VECTOR_EMBEDDINGS_RETRIEVAL) {
                    retrievalConfig.setEmbeddingImpl(embeddingsDefaultImpl);
                    retrievalConfig.setEmbeddingEndpoint(null); // INFO: The default implementations have their endpoints configured already
                    retrievalConfig.setEmbeddingAPIToken(contextService.getApiToken(embeddingsDefaultImpl));
                } else {
                    retrievalConfig.setEmbeddingImpl(null);
                    retrievalConfig.setEmbeddingEndpoint(null);
                    retrievalConfig.setEmbeddingAPIToken(null);
                    retrievalConfig.setEmbeddingValueType(EmbeddingValueType.float32);
                }

                systemsToBenchmark.add(retrievalConfig);
            }

            /*
            RetrievalConfiguration rConfig = new RetrievalConfiguration();
            rConfig.setRetrievalImpl(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH);
            rConfig.setEmbeddingImpl(EmbeddingsImpl.OPENAI_COMPATIBLE);
            rConfig.setEmbeddingEndpoint("http://localhost:3000/v1/embeddings");
            rConfig.setEmbeddingAPIToken("YOUR_API_TOKEN");
            rConfig.setEmbeddingValueType(EmbeddingValueType.float32);
            systemsToBenchmark.add(rConfig);
             */

            if (benchmark_config != null) {
                log.info("Read optional benchmark configuration ...");
                InputStream inputBenchmarkConfig = new BufferedInputStream(benchmark_config.getInputStream());
                BenchmarkConfiguration benchmarkConfig = objectMapper.readValue(inputBenchmarkConfig, BenchmarkConfiguration.class);
                inputBenchmarkConfig.close();

                for (RetrievalConfiguration retrievalConfig : benchmarkConfig.getRetrievalConfigs()) {
                    log.info("Additional retrieval system to benchmark: " + retrievalConfig.getRetrievalImpl());
                    systemsToBenchmark.add(retrievalConfig);
                }
            }

            bmService.runBenchmark(bmDataset, systemsToBenchmark, indexAlternativeQuestions, reRankAnswers, throttleTimeInMillis, deleteDomain, email, user, processId);

            return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to run a MTEB evaluation (https://embeddings-benchmark.github.io/mteb/)
     *
     */
    @RequestMapping(value = "/mteb-evaluation", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Run a MTEB evaluation", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> runMtebEvaluation(
            @Parameter(name = "dataset-path", description = "Dataset path, e.g. 'Wyona/LIMIT-very-small' (https://huggingface.co/datasets/Wyona/LIMIT-very-small) or 'orionweller/LIMIT-small' (https://huggingface.co/datasets/orionweller/LIMIT-small)", required = true)
            @RequestParam(value = "dataset-path", required = true) String datasetPath,
            HttpServletRequest request) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            if (!contextService.isAdmin() && !contextService.hasRole(Role.BENCHMARK)) {
                log.error("Access denied, because user has neither role " + Role.ADMIN + " nor role " + Role.BENCHMARK + "!");
                return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }

            int throttleTimeInMillis = -1; // INFO: No throttling

            String processId = UUID.randomUUID().toString();
            User user = iamService.getUser(false, false);

            // TODO: Make retrieval implementation configurable
            RetrievalConfiguration rConfig = new RetrievalConfiguration();

            //rConfig.setRetrievalImpl(DetectDuplicatedQuestionImpl.LUCENE_DEFAULT);

            //rConfig.setRetrievalImpl(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH);
            //rConfig.setEmbeddingImpl(EmbeddingsImpl.SBERT);

            rConfig.setRetrievalImpl(DetectDuplicatedQuestionImpl.LUCENE_SPARSE_VECTOR_EMBEDDINGS_RETRIEVAL);
            rConfig.setEmbeddingImpl(EmbeddingsImpl.SBERT);

            bmService.runMtebEvaluation(throttleTimeInMillis, datasetPath, rConfig, user, processId);

            return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to run a classification benchmark
     *
     */
    @RequestMapping(value = "/run-classification-benchmark", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Run a classification benchmark", security = { @SecurityRequirement(name = "bearerAuth") })
    public ResponseEntity<?> performClassificationBenchmark(
            @Parameter(name = "domain-id", description = "Domain Id containing preference dataset", required = true)
            @RequestParam(value = "domain-id", required = true) String domainId,
            @Parameter(name = "email", description = "E-Mail to get notification when benchmark is completed", required = false)
            @RequestParam(value = "email", required = false) String email,
            @Parameter(name = "throttle-time", description = "Throttle time in milliseconds",required = false)
            @RequestParam(value = "throttle-time", required = false) Integer customThrottleTimeInMilis,
            HttpServletRequest request) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            if (!contextService.isAdmin() && !contextService.hasRole(Role.BENCHMARK)) {
                log.error("Access denied, because user has neither role " + Role.ADMIN + " nor role " + Role.BENCHMARK + "!");
                return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }

            int throttleTimeInMillis = -1; // INFO: No throttling
            if (customThrottleTimeInMilis != null && customThrottleTimeInMilis > 0) {
                throttleTimeInMillis = customThrottleTimeInMilis;
            }

            String processId = UUID.randomUUID().toString();
            User user = iamService.getUser(false, false);
            bmService.runClassificationBenchmark(domainId, throttleTimeInMillis, email, user, processId);

            return new ResponseEntity<>("{\"process-id\":\"" + processId + "\"}", HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get dataset as input stream, either from provided file or from a default dataset file
     * @param file File containing dataset
     * @return dataset as input stream
     */
    private InputStream getDatasetAsInputStream(MultipartFile file) throws Exception {
        if (file != null) {
            InputStream inputStream = new BufferedInputStream(file.getInputStream());
            return inputStream;
        } else {
            // INFO: Use default dataset if none was given.
            // TODO: Make configurable
            //String datasetPathName = "weaviate-size2-v1.json";
            //String datasetPathName = "weaviate-size7-v1.json";
            String datasetPathName = "weaviate-size121-v2.json";

            File datasetFile = new File(datasetsDataPath, "questions-and-answers/" + datasetPathName);
            log.info("Use default dataset: " + datasetFile.getAbsolutePath());
            return new FileInputStream(datasetFile);

            //String fileName = String.join("", "benchmark_data/", datasetPathName);
            //log.info("Use default dataset from classpath: " + fileName);
            //ClassLoader classLoader = getClass().getClassLoader();
            //InputStream inputStream = classLoader.getResourceAsStream(fileName);
            //return inputStream;
        }
    }

    /**
     *
     */
    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper;
    }

    /**
     * REST interface to get a list of all saved benchmarks with their id and date
     * 
     */
    @RequestMapping(value = "/list", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Get a list of all saved benchmarks with their id and date")
    public ResponseEntity<?> getBenchmarkList(
            HttpServletRequest request) {

        try {
            // for reading json files as objects
            ObjectMapper objectMapper = getObjectMapper();
            
            
            // get a list of all saved benchmark directories
            List<String> bechnmarkHistoryDirectories = bmService.getBenchmarkDirectories();
            
            // get benchmark info out of every directory. date/dataset_name/dataset_size
            
            LinkedList<BenchmarkInfo> benchmarkResultHistory = new LinkedList<>();
            for (String dir : bechnmarkHistoryDirectories) {
                benchmarkResultHistory.add(bmService.getBenchmarkInfo(dir));
            }

            return new ResponseEntity<>(benchmarkResultHistory, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get a report of a particular benchmark as JSON
     */
    @RequestMapping(value = "/report/{id}/json", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Get a report (as JSON) of a particular benchmark, containing execution date, dataset info")
    public ResponseEntity<?> getBenchmarkReportAsJson(
            @Parameter(name = "id", description = "Benchmark Id, for example '230214_112023', which represents the results of all benchmarks performed at a specific time", required = true)
            @PathVariable(value = "id", required = true) String benchmarkId,
            HttpServletRequest request) {

        try {
            BenchmarkInfo bmInfo = bmService.getBenchmarkInfo(benchmarkId);

            BenchmarkResult[] implementationResults = bmService.getBenchmarkResults(benchmarkId);

            // TODO: Make reference benchmark configurable or add to dataset
            String referenceBenchmarkId = "231005_210106";
            //String referenceBenchmarkId = "230302_164322";
            implementationResults = bmService.compareWithReferenceBenchmark(implementationResults, referenceBenchmarkId);

            BenchmarkResponse responseObject = new BenchmarkResponse(bmInfo, implementationResults);

            return new ResponseEntity<>(responseObject, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * REST interface to get a report of a particular benchmark as PDF
     */
    @RequestMapping(value = "/report/{id}/pdf", method = RequestMethod.GET, produces = "application/pdf")
    @Operation(summary = "Get a report (as PDF) of a particular benchmark, containing execution date, dataset info and result graphs")
    public ResponseEntity<?> getBenchmarkReportAsPDF(
            @Parameter(name = "id", description = "Benchmark Id, for example '230214_112023', which represents the results of all benchmarks performed at a specific time", required = true)
            @PathVariable(value = "id", required = true) String benchmarkId,
            HttpServletRequest request) {

        try {
            BenchmarkInfo bmInfo = bmService.getBenchmarkInfo(benchmarkId);
                    
            // create report with data and graphs
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage();
            doc.addPage(page);
            
            PDFont font = PDType1Font.HELVETICA;
            
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            
            // write all the text info
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(50, 730);
            contentStream.showText("Katie Benchmark Report");
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(50, 715);
            contentStream.showText("ID: " + bmInfo.getId());
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(50, 640);
            contentStream.showText("Date: " + bmInfo.getDateTime().toString());
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText("Dataset Name: " + bmInfo.getDatasetName());
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(50, 685);
            contentStream.showText("Dataset Version: " + bmInfo.getDatasetVersion());
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(50, 670);
            contentStream.showText("Dataset Size: " + String.valueOf(bmInfo.getDatasetSize()));
            contentStream.endText();
            
            contentStream.beginText();
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(50, 655);
            contentStream.showText("Number of test runs: " + String.valueOf(bmInfo.getNumberOfRuns()));
            contentStream.endText();
            
            // draw the graphs
            File benchmarkDir = new File(benchmarksDataPath, benchmarkId);
            File barPlot = new File(benchmarkDir, "accuracy_precision_recall_bar.png");
            PDImageXObject pdImageBar = PDImageXObject.createFromFile(barPlot.getPath(),doc);
            contentStream.drawImage(pdImageBar, 50, 320, 500, 300);
            File scatterPlot = new File(benchmarkDir, "accuracy_vs_time_scatter.png");
            PDImageXObject pdImageScatter = PDImageXObject.createFromFile(scatterPlot.getPath(),doc);
            contentStream.drawImage(pdImageScatter, 50, 10, 500, 300);
            
            contentStream.close();
            
            // save to outputstream
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            doc.save(os);
            byte[] pdfAsBytes = os.toByteArray();
            
            //set new headers for pdf
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/pdf"));
            headers.add("content-disposition",  "inline;filename=" + "Katie_Benchmark_Report_"+ bmInfo.getId() + ".pdf");
            headers.setContentDispositionFormData(bmInfo.getId() + ".pdf", bmInfo.getId() + ".pdf");


            return new ResponseEntity<>(pdfAsBytes, headers, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }
}
