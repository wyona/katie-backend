package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.*;

import com.wyona.katie.models.Error;
import com.wyona.katie.services.*;
import io.swagger.annotations.ApiParam;

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

import io.swagger.annotations.ApiOperation;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

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
    private AIService aiService;
    
    @Autowired
    private BenchmarkService bmService;

    @Value("${datasets.data_path}")
    private String datasetsDataPath;

    @Value("${benchmarks.data_path}")
    private String benchmarksDataPath;

    /**
     * REST interface to ask questions which have an answer inside domain / knowledge base
     */
    @RequestMapping(value = "/accuracy-true-positives", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Ask questions which have an answer inside domain / knowledge base")
    public ResponseEntity<?> getAccuracyTruePositives(
            @ApiParam(name = "domainId", value = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
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
    @ApiOperation(value="Ask questions which do not have an answer inside domain / knowledge base")
    public ResponseEntity<?> getAccuracyTrueNegatives(
            @ApiParam(name = "domainId", value = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
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
    @ApiOperation(value="Get Precision (Number of retrieved correct resp. relevant answers divided by total number of retrieved answers) and Recall (Number of retrieved correct resp. relevant answers divided by total number of relevant answers)")
    public ResponseEntity<?> getPrecisionAndRecall(
            @ApiParam(name = "domainId", value = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
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

            BenchmarkPrecision report = qaService.getAccuracyAndPrecisionAndRecallBenchmark(domain.getId(), questionsAndRelevantUuids.toArray(new BenchmarkQuestion[0]), -1, null);

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
    @ApiOperation(value="Get confidence score whether two sentences are similar")
    public ResponseEntity<?> getSentencesSimilarity(
            @ApiParam(name = "domainId", value = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @ApiParam(name = "sentence-one", value = "Sentence One, e.g. 'How old are you'",required = true)
            @RequestParam(value = "sentence-one", required = true) String sentenceOne,
            @ApiParam(name = "sentence-two", value = "Sentence Two, e.g. 'What is your age'",required = true)
            @RequestParam(value = "sentence-two", required = true) String sentenceTwo,
            @ApiParam(name = "embeddings-impl", value = "Embeddings implementation / provider",required = true)
            @RequestParam(value = "embeddings-impl", required = true) EmbeddingsImpl embeddingsImpl,
            @ApiParam(name = "get-embeddings", value = "When set to true, then embeddings will be returned as well (false by default)",required = false)
            @RequestParam(value = "get-embeddings", required = false) Boolean getEmbeddings,
            HttpServletRequest request) {

        if (!contextService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        // TODO: Billing per domain for embeddings, e.g. Cohere, OpenAPI, Aleph Alpha, ...

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
    @ApiOperation(value="Get confidence score whether two words are similar")
    public ResponseEntity<?> getWordsSimilarity(
            @ApiParam(name = "domainId", value = "Domain Id, for example 'a30b9bfe-0ffb-41eb-a2e2-34b238427a74', which represents a single realm containing its own set of questions/answers.",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            @ApiParam(name = "word-one", value = "Word One, e.g. 'old'",required = true)
            @RequestParam(value = "word-one", required = true) String wordOne,
            @ApiParam(name = "word-two", value = "Word Two, e.g. 'age'",required = true)
            @RequestParam(value = "word-two", required = true) String wordTwo,
            @ApiParam(name = "embeddings-impl", value = "Embeddings implementation / provider",required = true)
            @RequestParam(value = "embeddings-impl", required = true) WordEmbeddingImpl embeddingsImpl,
            @ApiParam(name = "get-embeddings", value = "When set to true, then embeddings will be returned as well (false by default)", required = false)
            @RequestParam(value = "get-embeddings", required = false) Boolean getEmbeddings,
            HttpServletRequest request) {

        if (!contextService.isMemberOrAdmin(domainId)) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        // TODO: Billing per domain for embeddings, e.g. Cohere, OpenAPI, Aleph Alpha, ...

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
     * REST interface to perform a full benchmark and create evaluation graphs
     * 
     */
    @RequestMapping(value = "/run-benchmark", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Run an extensive benchmark for every currently active search system returning accuracy, precision, recall and average performance time")
    public ResponseEntity<?> performBenchmark(
            // TODO: Add description for file, e.g. "Dataset" and that if not provided a default set is being used
            @RequestPart(name = "file", required = false) MultipartFile file, // INFO: When no dataset is provided, then a default set is used
            @ApiParam(name = "search-implementations", value = "Comma separated list of search implementations to be benchmarked: LUCENE_DEFAULT, SENTENCE_BERT, LUCENE_VECTOR_SEARCH, WEAVIATE, ELASTICSEARCH",required = true)
            @RequestParam(value = "search-implementations", required = true) String searchImplementations,
            @ApiParam(name = "email", value = "E-Mail to get notification when benchmark is completed",required = false)
            @RequestParam(value = "email", required = false) String email,
            @ApiParam(name = "index-alternative-questions", value = "Also index alternative questions when set to true",required = true)
            @RequestParam(value = "index-alternative-questions", required = true) Boolean indexAlternativeQuestions,
            @ApiParam(name = "re-rank-answers", value = "Re-rank answers when set to true (false by default)",required = false)
            @RequestParam(value = "re-rank-answers", required = false) Boolean reRankAnswers,
            @ApiParam(name = "throttle-time", value = "Throttle time in milliseconds",required = false)
            @RequestParam(value = "throttle-time", required = false) Integer customThrottleTimeInMilis,
            @ApiParam(name = "delete-domain", value = "When set to true, then delete domain which was created to run benchmark (true by default)", required = false)
            @RequestParam(value = "delete-domain", required = false) Boolean deleteDomain,
            HttpServletRequest request) {

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
            User user = authService.getUser(false, false);
            bmService.runBenchmark(bmDataset, searchImplementations, indexAlternativeQuestions, reRankAnswers, throttleTimeInMillis, deleteDomain, email, user, processId);

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
            // INFO: Use default dataset if none was given
            String datasetPathName = "weaviate-size7-v1.json";
            //String datasetPathName = "weaviate-size121-v2.json"; //TODO: maybe put this into config

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
    @ApiOperation(value="Get a list of all saved benchmarks with their id and date")
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
                File benchmarkD = new File(benchmarksDataPath, dir);
                File resultF = new File(benchmarkD, bmService.DATASET_INFO_FILE);

                benchmarkResultHistory.add(objectMapper.readValue(resultF, BenchmarkInfo.class));
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
    @ApiOperation(value="Get a report (as JSON) of a particular benchmark, containing execution date, dataset info")
    public ResponseEntity<?> getBenchmarkReportAsJson(
            @ApiParam(name = "id", value = "Benchmark Id, for example '230214_112023', which represents the results of all benchmarks performed at a specific time", required = true)
            @PathVariable(value = "id", required = true) String benchmarkId,
            HttpServletRequest request) {

        try {
            // for reading json files as objects
            ObjectMapper objectMapper = getObjectMapper();

            // get info file and graph files from desired benchmark directory
            File benchmarkD = new File(benchmarksDataPath, benchmarkId);
            File resultF = new File(benchmarkD, bmService.DATASET_INFO_FILE);
            BenchmarkInfo bmInfo = objectMapper.readValue(resultF, BenchmarkInfo.class);

            BenchmarkResult[] implementationResults = bmService.getBenchmarkResults(benchmarkId);

            String referenceBenchmarkId = "231005_210106"; // TODO: Make configurable
            //String referenceBenchmarkId = "230302_164322"; // TODO: Make configurable
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
    @ApiOperation(value="Get a report (as PDF) of a particular benchmark, containing execution date, dataset info and result graphs")
    public ResponseEntity<?> getBenchmarkReportAsPDF(
            @ApiParam(name = "id", value = "Benchmark Id, for example '230214_112023', which represents the results of all benchmarks performed at a specific time", required = true)
            @PathVariable(value = "id", required = true) String benchmarkId,
            HttpServletRequest request) {

        try {
            // for reading json files as objects
            ObjectMapper objectMapper = getObjectMapper();
            
            // get info file and graph files from desired benchmark directory
            File benchmarkD = new File(benchmarksDataPath, benchmarkId);
            File resultF = new File(benchmarkD, bmService.DATASET_INFO_FILE);
            BenchmarkInfo bmInfo = objectMapper.readValue(resultF, BenchmarkInfo.class);
                    
                    
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
            File barPlot = new File(benchmarkD, "accuracy_precision_recall_bar.png");
            PDImageXObject pdImageBar = PDImageXObject.createFromFile(barPlot.getPath(),doc);
            contentStream.drawImage(pdImageBar, 50, 320, 500, 300);
            File scatterPlot = new File(benchmarkD, "accuracy_vs_time_scatter.png");
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
