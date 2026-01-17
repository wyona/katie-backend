package com.wyona.katie.services;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wyona.katie.handlers.LuceneQuestionAnswerImpl;
import com.wyona.katie.handlers.SentenceBERTQuestionAnswerImpl;
import com.wyona.katie.handlers.WeaviateQuestionAnswerImpl;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wyona.katie.models.*;
import freemarker.template.Template;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * TODO
 */
@Slf4j
@Component
public class BenchmarkService {

    public static final String DATASET_INFO_FILE = "info.json";
    public static final String BENCHMARK_RESULTS = "results.json";

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private SentenceBERTQuestionAnswerImpl sbertImpl;
    @Autowired
    private WeaviateQuestionAnswerImpl weaviateImpl;
    @Autowired
    private LuceneQuestionAnswerImpl luceneImpl;

    @Autowired
    private AIService aiService;

    @Autowired
    private ClassificationService classificationService;

    @Value("${benchmarks.data_path}")
    private String benchmarksDataPath;

    @Value("${datasets.data_path}")
    private String datasetsDataPath;

    @Value("${mail.subject.tag}")
    private String mailSubjectTag;

    /**
     * Run MTEB evaluation
     */
    @Async
    public void runMtebEvaluation(int throttleTimeInMillis, String task, User user, String processId) {
        backgroundProcessService.startProcess(processId, "Run MTEB evaluation for task '" + task + "' ...", user.getId());

        File corpusFile = null;
        File queriesFile = null;
        File qrelsFile = null;
        if (task == null) {
            corpusFile = new File(datasetsDataPath, "mteb/limit-small/corpus.jsonl");
            queriesFile = new File(datasetsDataPath, "mteb/limit-small/queries.jsonl");
            qrelsFile = new File(datasetsDataPath, "mteb/limit-small/qrels.jsonl");
        } else {
            // TODO: Get corpus, queries and qrels referenced by task
            corpusFile = new File(datasetsDataPath, "mteb/limit-small/corpus.jsonl");
            queriesFile = new File(datasetsDataPath, "mteb/limit-small/queries.jsonl");
            qrelsFile = new File(datasetsDataPath, "mteb/limit-small/qrels.jsonl");
        }

        if (corpusFile.isFile()) {
            try {
                LocalDateTime currentDateTime = LocalDateTime.now();
                String benchmarkId = getBenchmarkId(currentDateTime);
                String domainName = "MTEB Evaluation " + benchmarkId;
                backgroundProcessService.updateProcessStatus(processId, "Create MTEB evaluation domain '" + domainName + "' ...");
                Context domain = contextService.createDomain(false, domainName, domainName, false, user);

                // TODO: Make retrieval implementation configurable
                RetrievalConfiguration rConfig = new RetrievalConfiguration();
                //rConfig.setRetrievalImpl(DetectDuplicatedQuestionImpl.LUCENE_DEFAULT);
                rConfig.setRetrievalImpl(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH);
                rConfig.setEmbeddingImpl(EmbeddingsImpl.SBERT);

                HashMap corpusAnswerIds = indexCorpus(domain, rConfig, processId, corpusFile, throttleTimeInMillis);

                List<BenchmarkQuestion> benchmarkQuestions = getBenchmarkQuestions(queriesFile, qrelsFile, corpusAnswerIds);

                int recall_top_k = 2;
                BenchmarkResult result = queryCorpus(domain.getId(), processId, benchmarkQuestions, recall_top_k, throttleTimeInMillis);
                backgroundProcessService.updateProcessStatus(processId, "recall@" + recall_top_k+ ": " + result.getRecall());

                // INFO: Delete the created benchmark domain
                if (false) { // TODO: Make deleting domain configurable
                    backgroundProcessService.updateProcessStatus(processId, "Delete domain '" + domain.getId() + "' ...");
                    contextService.deleteDomain(domain.getId(), user.getId());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        backgroundProcessService.stopProcess(processId, null);
    }

    /**
     * @param recall_top_k Recall top-k value, e.g. "2" means, check how many query relevant document(s) appear in the top-2 retrieved results (divided by the total number of query relevant document(s) in the corpus)
     */
    private BenchmarkResult queryCorpus(String domainId, String processId, List<BenchmarkQuestion> benchmarkQuestions, int recall_top_k, int throttleTimeInMillis) throws Exception {
        backgroundProcessService.updateProcessStatus(processId, "Query corpus ...");

        double accuracy = -1;
        String[] failedQuestions = null;
        int totalNumQuestions = -1;
        double precision = -1;
        double recall = -1;

        // INFO: Benchmark accuracy, precision and recall
        LocalDateTime benchmarkStartTime = LocalDateTime.now();
        long startTime = System.currentTimeMillis();
        try {
            BenchmarkPrecision precisionAndRecall = qaService.getAccuracyAndPrecisionAndRecallBenchmark(domainId, benchmarkQuestions.toArray(new BenchmarkQuestion[0]), recall_top_k, throttleTimeInMillis, processId);
            accuracy = precisionAndRecall.getAccuracy();
            failedQuestions = precisionAndRecall.getFailedQuestions();
            totalNumQuestions = precisionAndRecall.getTotalNumQuestions();
            precision = precisionAndRecall.getPrecision();
            recall = precisionAndRecall.getRecall();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        long timeInMilliseconds = System.currentTimeMillis() - startTime;
        log.info("Number of milliseconds to run benchmark: " + timeInMilliseconds);

        double timeToRunBenchnarkInSeconds = timeInMilliseconds / 1000.0;
        log.info("Number of seconds to run benchmark: " + timeToRunBenchnarkInSeconds);

        Context domain = contextService.getContext(domainId);
        DetectDuplicatedQuestionImpl retrievalImpl = domain.getDetectDuplicatedQuestionImpl();
        int timeToIndex = 0; // TODO
        return new BenchmarkResult(retrievalImpl, getSearchImplementationVersion(retrievalImpl, domain), getSearchImplementationMeta(domain), accuracy, totalNumQuestions, failedQuestions, precision, recall, timeToIndex, timeToRunBenchnarkInSeconds, benchmarkStartTime);
    }

    /**
     *
     */
    private List<BenchmarkQuestion> getBenchmarkQuestions(File queriesFile, File qrelsFile, HashMap corpusAnswerIds) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        java.io.BufferedReader readerQrels = Files.newBufferedReader(Path.of(qrelsFile.getAbsolutePath()));
        String line;
        List<StringTuple> qrels = new ArrayList<>();
        while ((line = readerQrels.readLine()) != null) {
            java.util.Map<String, Object> obj = mapper.readValue(line, java.util.Map.class);
            String queryId = (String) obj.get("query-id");
            String corpusId = (String) obj.get("corpus-id");
            log.info("Query Id: " + queryId + " | Corpus Id: " + corpusId);
            String answerId = getAnswerId(corpusId, corpusAnswerIds);
            if (answerId != null) {
                qrels.add(new StringTuple(queryId, answerId));
            } else {
                log.error("No answer Id for corpus Id '" + corpusId + "'!");
                throw new Exception("No answer Id for corpus Id '" + corpusId + "'!");
            }
        }
        readerQrels.close();

        java.io.BufferedReader readerQueries = Files.newBufferedReader(Path.of(queriesFile.getAbsolutePath()));
        int numberOfQueries = 0;
        //String line;
        List<BenchmarkQuestion> benchmarkQuestions = new ArrayList<>();
        while ((line = readerQueries.readLine()) != null) {
            numberOfQueries++;
            /*
            if (numberOfQueries > 2) {
                log.error("TODO: Use all queries!");
                break;
            }
            */
            java.util.Map<String, Object> obj = mapper.readValue(line, java.util.Map.class);
            String id = (String) obj.get("_id");
            String query = (String) obj.get("text");
            log.info("Id: " + id + " | Query: " + query);
            BenchmarkQuestion question = new BenchmarkQuestion();
            question.setQuestion(query);
            String[] relevantUids = getRelevantUids(id, qrels);
            for (String uuid : relevantUids) {
                question.addRelevantUuid(uuid);
            }
            benchmarkQuestions.add(question);
        }
        readerQueries.close();
        return benchmarkQuestions;
    }

    /**
     *
     */
    private String getAnswerId(String corpusId, HashMap<String, String> corpusAnsserIds) {
        return corpusAnsserIds.get(corpusId);
    }

    /**
     * @param queryId Query Id, e.g. "query_989"
     */
    private String[] getRelevantUids(String queryId, List<StringTuple> qrels) {
        List<String> relevantUids = new ArrayList<>();
        for (StringTuple tuple : qrels) {
            if (tuple.first().equals(queryId)) {
                relevantUids.add(tuple.second());
            }
        }
        return relevantUids.toArray(new String[0]);
    }

    /**
     * Index corpus
     */
    private HashMap indexCorpus(Context domain, RetrievalConfiguration rConfig, String processId, File corpusFile, int throttleTimeInMillis) throws Exception {
        backgroundProcessService.updateProcessStatus(processId, "Indexing corpus '" + corpusFile.getAbsolutePath() + "'...");
        ObjectMapper mapper = new ObjectMapper();
        java.io.BufferedReader reader = Files.newBufferedReader(Path.of(corpusFile.getAbsolutePath()));
        int numberOfLines = 0;
        String line;
        HashMap corpusAnswerIds = new HashMap<>();
        while ((line = reader.readLine()) != null) {
            numberOfLines++;
            java.util.Map<String, Object> obj = mapper.readValue(line, java.util.Map.class);
            String id = (String) obj.get("_id");
            String text = (String) obj.get("text");
            log.info("Id: " + id + " | Text: " + text);

            Answer answer = new Answer(null, text, ContentType.TEXT_PLAIN, null, null, null, null, null, null, null, domain.getId(), null, null, null, false, null, false, null);
            answer = contextService.addQuestionAnswer(answer, domain);
            log.info("Answer Id: " + answer.getUuid());
            corpusAnswerIds.put(id, answer.getUuid());
        }
        reader.close();

        double timeToIndex = -1;
        timeToIndex = new Date().getTime();

        contextService.reindex(domain.getId(), rConfig.getRetrievalImpl(), null, null, rConfig.getEmbeddingImpl(), rConfig.getEmbeddingModel(), rConfig.getEmbeddingValueType(), rConfig.getEmbeddingEndpoint(), rConfig.getEmbeddingAPIToken(), false, true, processId, throttleTimeInMillis);
        timeToIndex = (new Date().getTime() - timeToIndex) / 1000.0;

        backgroundProcessService.updateProcessStatus(processId, numberOfLines + " text snippets indexed.");

        return corpusAnswerIds;
    }

    /**
     * @param searchImplementations List of search / retrieval implementations
     * @param indexAlternativeQuestions When true, then index alternative questions, when false, then do not index alternative questions
     * @param reRankAnswers When true, then re-rank results
     */
    @Async
    public void runBenchmark(CustomBenchmarkDataset bmDataset, List<RetrievalConfiguration> searchImplementations, Boolean indexAlternativeQuestions, Boolean reRankAnswers, int throttleTimeInMillis, Boolean deleteDomain, String email, User user, String processId) {
        backgroundProcessService.startProcess(processId, "Run benchmark ...", user.getId());
        try {
            String datasetName = bmDataset.getDatasetName();
            String datasetVersion = bmDataset.getDatasetVersion();

            LocalDateTime currentDateTime = LocalDateTime.now();
            String benchmarkId = getBenchmarkId(currentDateTime);

            // INFO: Create new domain
            String domainName = "Benchmark " + benchmarkId;
            backgroundProcessService.updateProcessStatus(processId, "Create benchmark domain '" + domainName + "' ...");
            Context domain = contextService.createDomain(false, domainName, domainName, false, user);
            if (reRankAnswers) {
                domain = contextService.enableReRankAnswers(domain.getId());
                // INFO: By default SBERT re-ranker will be used
            }

            List<BenchmarkQuestion> benchmarkQuestions = new ArrayList<BenchmarkQuestion>();

            // INFO: Import QnAs into domain
            List<BenchmarkQna> qnas = bmDataset.getQnas();
            backgroundProcessService.updateProcessStatus(processId, "Import " + qnas.size() + " QnAs ...");
            int counter = 0;
            final int BATCH_SIZE = 100;
            for (BenchmarkQna qna : qnas) {
                // Import Benchmark QnA into Domain
                Answer answer = new Answer(null, qna.getAnswer(), null, null, null, null, null, null, null, null, domain.getId(), null, qna.getQuestion(), null, false, null, false, null);
                for (String aQuestion : qna.getAlternativeQuestions()) {
                    answer.addAlternativeQuestion(aQuestion);
                }
                for (String classification: qna.getClassifications()) {
                    answer.addClassification(classification);
                }
                if (qna.getUrl() != null) {
                    answer.setUrl(qna.getUrl());
                }
                answer = contextService.addQuestionAnswer(answer, domain);
                qna.setUuid(answer.getUuid());

                // INFO: Add benchmark question
                if (answer.getOriginalquestion() != null) {
                    BenchmarkQuestion bq = new BenchmarkQuestion();
                    bq.setQuestion(answer.getOriginalquestion());
                    bq.addRelevantUuid(answer.getUuid());
                    if (qna.getClassifications() != null) {
                        // TODO: Add classifications
                    }
                    benchmarkQuestions.add(bq);
                }
                if (answer.getAlternativequestions() != null) {
                    for (String altQuestion: answer.getAlternativequestions()) {
                        BenchmarkQuestion bq = new BenchmarkQuestion();
                        bq.setQuestion(altQuestion);
                        bq.addRelevantUuid(answer.getUuid());
                        if (qna.getClassifications() != null) {
                            // TODO: Add classifications
                        }
                        benchmarkQuestions.add(bq);
                    }
                }
                if (qna.getTestQuestions() != null) {
                    for (BenchmarkQuestion testQuestion : qna.getTestQuestions()) {
                        testQuestion.addRelevantUuid(answer.getUuid());
                        benchmarkQuestions.add(testQuestion);
                    }
                }

                counter++;
                if (counter % BATCH_SIZE == 0) {
                    backgroundProcessService.updateProcessStatus(processId, counter + " QnAs imported, " + (qnas.size() - counter) + " QnAs remaining ...");
                }
            }

            // INFO: Create new folder in volume/benchmarks named <date>_<time> to save all new results in
            File benchmarkDir = new File(benchmarksDataPath, benchmarkId);

            // INFO: Check if the directory can be created
            if (benchmarkDir.mkdirs() == true) {
                log.info("Benchmark directory has been created successfully");
            } else {
                log.error("New benchmark directory cannot be created!");
                backgroundProcessService.updateProcessStatus(processId, "New benchmark directory cannot be created!", BackgroundProcessStatusType.ERROR);
            }

            // INFO: Create info.json file in benchmark directory
            StringBuilder envMeta = new StringBuilder("OS: " + System.getProperty("os.name"));
            BenchmarkInfo benchmarkInfo = new BenchmarkInfo(currentDateTime, benchmarkId, datasetName, qnas.size(), 1, datasetVersion, envMeta.toString(), indexAlternativeQuestions, reRankAnswers, domain.getReRankImpl());
            File infoFile = new File(benchmarkDir, DATASET_INFO_FILE);
            ObjectMapper objectMapper = getObjectMapper();
            objectMapper.writeValue(infoFile, benchmarkInfo);

            // INFO: Re-index the created domain and perform benchmarks for each system. save results in list
            List<BenchmarkResult> implementationResults = new LinkedList<>();
            for (RetrievalConfiguration searchImplementation : searchImplementations) {
                implementationResults.add(reindexAndBenchmark(searchImplementation, domain.getId(), indexAlternativeQuestions, benchmarkQuestions.toArray(new BenchmarkQuestion[0]), throttleTimeInMillis, processId));
            }

            // INFO: Save list of json file with accuracy, precision, recall, time, date
            File resultFile = new File(benchmarkDir, BENCHMARK_RESULTS);
            objectMapper.writeValue(resultFile, implementationResults);

            // INFO: Create benchmark result graphs and save them in the created benchmark directory
            backgroundProcessService.updateProcessStatus(processId, "Create Benchmark graphs ...");
            createBenchmarkGraphs(implementationResults, benchmarkDir, objectMapper);

            // INFO: Delete the created benchmark domain
            if (deleteDomain) {
                backgroundProcessService.updateProcessStatus(processId, "Delete domain '" + domain.getId() + "' ...");
                contextService.deleteDomain(domain.getId(), user.getId());
            }

            if (email != null) {
                String subject = mailSubjectTag + " Benchmark completed (" + benchmarkId + ")";
                mailerService.send(email, domain.getMailSenderEmail(), subject, getCompletedBenchmarkEmailBody(domain.getHost(), benchmarkInfo, "en"), true);
            }

            backgroundProcessService.updateProcessStatus(processId, "Benchmark finished: " + benchmarkId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }
        backgroundProcessService.stopProcess(processId, null);
    }

    /**
     * Run classification benchmark
     */
    @Async
    public void runClassificationBenchmark(String domainId, int throttleTimeInMillis, String email, User user, String processId) {
        backgroundProcessService.startProcess(processId, "Run classification benchmark ...", user.getId());

        try {
            Context domain = contextService.getContext(domainId);

            LocalDateTime currentDateTime = LocalDateTime.now();
            String benchmarkId = getBenchmarkId(currentDateTime);

            HumanPreferenceLabel[] preferences = contextService.getRatingsOfPredictedLabels(domainId, true, true);
            int total = preferences.length;

            if (total == 0) {
                backgroundProcessService.updateProcessStatus(processId, "No human preference dataset entries available!", BackgroundProcessStatusType.WARN);
            }

            int successful = 0;
            List<HumanPreferenceLabel> failedPredictions = new ArrayList<>();

            for (HumanPreferenceLabel preference : preferences) {
                if (throttleTimeInMillis > 0) {
                    try {
                        log.info("Sleep for " + throttleTimeInMillis + " milliseconds ...");
                        Thread.sleep(throttleTimeInMillis);
                    } catch(Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                HitLabel[] labels = classificationService.predictLabels(domain, preference.getText(), 3);
                if (labels != null && labels.length > 0) {
                    backgroundProcessService.updateProcessStatus(processId, "Classify text '" + preference.getText() + "' and compare prediction with preference ...");
                    if (preference.getChosenLabel() != null && labels[0].getLabel().getId().equals(preference.getChosenLabel().getId())) {
                        backgroundProcessService.updateProcessStatus(processId, "Prediction and chosen preference match");
                        successful++;
                    } else {
                        backgroundProcessService.updateProcessStatus(processId, "Prediction and chosen preference do not match", BackgroundProcessStatusType.WARN);
                        failedPredictions.add(preference);
                    }
                } else {
                    backgroundProcessService.updateProcessStatus(processId, "No labels predicted for text '" + preference.getText() + "'", BackgroundProcessStatusType.WARN);
                    failedPredictions.add(preference);
                }
            }

            double accuracy = Double.valueOf(successful) / Double.valueOf(total);
            String benchmarkResult = "Accuracy: " + accuracy + " (Total: " + total + ", Successful: " + successful + ", Failed: " + failedPredictions.size() + ")";
            backgroundProcessService.updateProcessStatus(processId, benchmarkResult);

            if (email != null) {
                String subject = mailSubjectTag + " Classification benchmark completed (" + benchmarkId + ")";

                if (total > 0) {
                    mailerService.send(email, domain.getMailSenderEmail(), subject, getCompletedClassificationBenchmarkEmailBody(domain, benchmarkResult, failedPredictions), true);
                } else {
                    mailerService.send(email, domain.getMailSenderEmail(), subject, "<div>WARNING: No human preference dataset entries for domain '" + domainId + "' available!</div>", true);
                }

                backgroundProcessService.updateProcessStatus(processId, "E-Mail notification sent to " + email);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }

        backgroundProcessService.stopProcess(processId, domainId);
    }

    /**
     * Get email body re completed classification benchmark
     */
    private String getCompletedClassificationBenchmarkEmailBody(Context domain, String benchmarkResult, List<HumanPreferenceLabel> failedPredictions) {
        String domainLink = domain.getHost() + "/#/domain/" + domain.getId();
        StringBuilder body = new StringBuilder();
        body.append("<div>Classification benchmark completed for domain '" + domain.getName() + "' (" + domainLink + "):</div>");
        body.append("<br/><br/>");
        body.append("<div>" + benchmarkResult + "</div>");
        body.append("<br/>");
        body.append("<h3>Failed predictions</h3>");
        body.append("<ol>");
        for (HumanPreferenceLabel failedPrediction : failedPredictions) {
            body.append("<li>Text: " + failedPrediction.getText() + " (");
            if (failedPrediction.getRejectedLabel() != null) {
                body.append("Wrong label: " + failedPrediction.getRejectedLabel().getTerm());
            } else {
                body.append("Wrong label has been deleted in the meantime");
            }
            body.append(" | Correct label: ");
            if (failedPrediction.getChosenLabel() != null) {
                body.append(failedPrediction.getChosenLabel().getTerm());
            } else {
                body.append("No correct label provided by human feedback");
            }
            body.append(")</li>");
        }
        body.append("</ol>");

        return body.toString();
    }

    /**
     * Get email body containing information about completed benchmark
     */
    private String getCompletedBenchmarkEmailBody(String hostname, BenchmarkInfo benchmarkInfo, String userLanguage) throws Exception {
        BenchmarkResult[] benchmarkResults = getBenchmarkResults(benchmarkInfo.getId());
        // TODO: Make reference benchmark configurable or add to dataset
        String referenceBenchmarkId = "231005_210106";
        //String referenceBenchmarkId = "230302_164322";
        benchmarkResults = compareWithReferenceBenchmark(benchmarkResults, referenceBenchmarkId);

        BenchmarkInfo referenceBenchmarkInfo = getBenchmarkInfo(referenceBenchmarkId);

        TemplateArguments tmplArgs = new TemplateArguments(null, hostname);

        String benchmarkRawDataLink = hostname + "/api/v1/benchmark/report/" + benchmarkInfo.getId() + "/json";
        String referenceBenchmarkRawDataLink = hostname + "/api/v1/benchmark/report/" + referenceBenchmarkId + "/json";
        tmplArgs.add("raw_data_link", benchmarkRawDataLink);
        tmplArgs.add("raw_reference_data_link", referenceBenchmarkRawDataLink);
        tmplArgs.add("results", benchmarkResults);
        tmplArgs.add("info", benchmarkInfo);
        tmplArgs.add("reference_info", referenceBenchmarkInfo);

        StringWriter writer = new StringWriter();
        Template emailTemplate = mailerService.getTemplate("benchmark_completed_", Language.valueOf(userLanguage), null);
        emailTemplate.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * Compare benchmark results with reference benchmark to check whether there are improvements or degradations
     * @param implementationResults Benchmark results of the various search implementations
     * @param referenceBenchmarkId Id of reference benchmark, e.g. "231005_210106"
     */
    public BenchmarkResult[] compareWithReferenceBenchmark(BenchmarkResult[] implementationResults, String referenceBenchmarkId) throws Exception {
        log.info("Compare results with reference benchmark '" + referenceBenchmarkId + "' ...");
        BenchmarkResult[] referenceImplementationResults = getBenchmarkResults(referenceBenchmarkId);

        for (BenchmarkResult result : implementationResults) {
            boolean referenceImplementationExists = false;
            for (BenchmarkResult referenceResult : referenceImplementationResults) {
                if (result.getSystemName().equals(referenceResult.getSystemName())) {
                    referenceImplementationExists = true;
                    log.info("Compare results for implementation: " + result.getSystemName());

                    double accuracyDeviation = getPercentDeviation(result.getAccuracy(), referenceResult.getAccuracy());
                    result.setAccuracyDeviationInPercentage(accuracyDeviation);
                    log.info("Accuracy deviation in percentage: " + accuracyDeviation);

                    double precisionDeviation = getPercentDeviation(result.getPrecision(), referenceResult.getPrecision());
                    result.setPrecisionDeviationInPercentage(precisionDeviation);
                    log.info("Precision deviation in percentage: " + precisionDeviation);

                    double recallDeviation = getPercentDeviation(result.getRecall(), referenceResult.getRecall());
                    result.setRecallDeviationInPercentage(recallDeviation);
                    log.info("Recall deviation in percentage: " + recallDeviation);

                    double indexingTimeDeviation = getPercentDeviation(result.getIndexingTimeInSeconds(), referenceResult.getIndexingTimeInSeconds());
                    result.setIndexingTimeDeviationInPercentage(indexingTimeDeviation);
                    log.info("Indexing time deviation in percentage: " + indexingTimeDeviation);
                    result.setIndexingTimePerformanceFactor(referenceResult.getIndexingTimeInSeconds() / result.getIndexingTimeInSeconds());

                    double inferenceTimeDeviation = getPercentDeviation(result.getInferenceTimeInSeconds(), referenceResult.getInferenceTimeInSeconds());
                    result.setInferenceTimeDeviationInPercentage(inferenceTimeDeviation);
                    log.info("Average inference time deviation in percentage: " + inferenceTimeDeviation);
                    result.setInferenceTimePerformanceFactor(referenceResult.getInferenceTimeInSeconds() / result.getInferenceTimeInSeconds());
                }
            }

            if (!referenceImplementationExists) {
                log.warn("No reference implementation '" + result.getSystemName() + "' exists, therefore no comparison possible.");
            }
        }

        return implementationResults;
    }

    /**
     * Get deviation in percentage
     */
    private double getPercentDeviation(double observed, double reference) {
        return ((observed - reference) / reference) * 100.0;
    }

    /**
     * Get benchmark results
     * @param benchmarkId Benchmark Id, e.g. "230302_164322"
     * @return list of benchmark results
     */
    public BenchmarkResult[] getBenchmarkResults(String benchmarkId) throws Exception {
        File benchmarkD = new File(benchmarksDataPath, benchmarkId);
        File resultsF = new File(benchmarkD, BENCHMARK_RESULTS);
        ObjectMapper objectMapper = getObjectMapper();
        BenchmarkResult[] implementationResults = objectMapper.readValue(resultsF, BenchmarkResult[].class);

        return implementationResults;
    }

    /**
     * Get benchmark info
     * @param benchmarkId Benchmark Id, e.g. "230302_164322"
     * @return benchmark info
     */
    public BenchmarkInfo getBenchmarkInfo(String benchmarkId) throws Exception {
        File benchmarkD = new File(benchmarksDataPath, benchmarkId);
        File infoF = new File(benchmarkD, DATASET_INFO_FILE);
        ObjectMapper objectMapper = getObjectMapper();
        BenchmarkInfo info = objectMapper.readValue(infoF, BenchmarkInfo.class);
        return info;
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
     *
     */
    private String getBenchmarkId(LocalDateTime currentDateTime) {
        final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyMMdd_HHmmss");
        String benchmarkId = currentDateTime.format(customFormatter);
        return benchmarkId;
    }

    /**
     * get a list of all saved benchmark directories
     */
    public List<String> getBenchmarkDirectories() {
        List<String> bechnmarkHistoryDirectories = new LinkedList<String>();
        try (Stream<Path> stream = Files.list(Paths.get(benchmarksDataPath))) {
            bechnmarkHistoryDirectories = stream
                    .filter(dir -> Files.isDirectory(dir))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return bechnmarkHistoryDirectories;
    }

    /**
     * Re-index domain with a particular search implementation and run benchmark
     * @param rConfig Retrieval / Search configuration
     * @param indexAlternativeQuestions When true, then alternative questions will be indexed as well
     * @return benchmark results for a particular search implementation
     */
    public BenchmarkResult reindexAndBenchmark(RetrievalConfiguration rConfig, String domainId, boolean indexAlternativeQuestions, BenchmarkQuestion[] benchmarkQuestions, int throttleTimeInMillis, String processId) {
        String msg = "Re-index and run benchmark using search implementation '" + rConfig.getRetrievalImpl() + "' ...";
        log.info(msg);
        backgroundProcessService.updateProcessStatus(processId, msg);

        LocalDateTime benchmarkStartTime = LocalDateTime.now();
        double timeToIndex = -1;

        if (indexAlternativeQuestions) {
            msg = "Re-index QnAs including alternative questions ...";
            log.info(msg);
            backgroundProcessService.updateProcessStatus(processId, msg);
        } else {
            msg = "Re-index QnAs, but without re-indexing alternative questions ...";
            log.info(msg);
            backgroundProcessService.updateProcessStatus(processId, msg);
        }

        try {
            timeToIndex = new Date().getTime();
            contextService.reindex(domainId, rConfig.getRetrievalImpl(), null, null, rConfig.getEmbeddingImpl(), rConfig.getEmbeddingModel(), rConfig.getEmbeddingValueType(), rConfig.getEmbeddingEndpoint(), rConfig.getEmbeddingAPIToken(), indexAlternativeQuestions, true, processId, throttleTimeInMillis);
            timeToIndex = (new Date().getTime() - timeToIndex) / 1000.0;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        msg = "Run benchmark for '" + rConfig.getRetrievalImpl() + "' ...";
        log.info(msg);
        backgroundProcessService.updateProcessStatus(processId, msg);

        double accuracy = -1;
        String[] failedQuestions = null;
        int totalNumQuestions = -1;
        double precision = -1;
        double recall = -1;

        // INFO: Benchmark accuracy, precision and recall
        long startTime = System.currentTimeMillis();
        try {
            BenchmarkPrecision precisionAndRecall = qaService.getAccuracyAndPrecisionAndRecallBenchmark(domainId, benchmarkQuestions, 2, throttleTimeInMillis, processId);
            accuracy = precisionAndRecall.getAccuracy();
            failedQuestions = precisionAndRecall.getFailedQuestions();
            totalNumQuestions = precisionAndRecall.getTotalNumQuestions();
            precision = precisionAndRecall.getPrecision();
            recall = precisionAndRecall.getRecall();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        long timeInMilliseconds = System.currentTimeMillis() - startTime;
        log.info("Number of milliseconds to run benchmark: " + timeInMilliseconds);

        double timeToRunBenchnarkInSeconds = timeInMilliseconds / 1000.0;
        log.info("Number of seconds to run benchmark: " + timeToRunBenchnarkInSeconds);

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }
        return new BenchmarkResult(rConfig.getRetrievalImpl(), getSearchImplementationVersion(rConfig.getRetrievalImpl(), domain), getSearchImplementationMeta(domain), accuracy, totalNumQuestions, failedQuestions, precision, recall, timeToIndex, timeToRunBenchnarkInSeconds, benchmarkStartTime);
    }

    /**
     * Get version number of search implementation
     * @return version number, e.g. "9.12.0"
     */
    private String getSearchImplementationVersion(DetectDuplicatedQuestionImpl impl, Context domain) {
        String version = null;
        if (impl.equals(DetectDuplicatedQuestionImpl.LUCENE_DEFAULT) || impl.equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            version = luceneImpl.getVersion();
        } else if (impl.equals(DetectDuplicatedQuestionImpl.SENTENCE_BERT)) {
            version = sbertImpl.getVersionAndModel().get(sbertImpl.VERSION);
        } else if (impl.equals(DetectDuplicatedQuestionImpl.WEAVIATE)) {
            version = weaviateImpl.getVersion(domain);
        } else {
            log.warn("No version number available for search implementation '" + impl + "'.");
        }
        return version;
    }

    /**
     *
     */
    private String getSearchImplementationMeta(Context domain) {
        StringBuilder meta = new StringBuilder("Search Impl: " + domain.getDetectDuplicatedQuestionImpl());
        if (domain.getDetectDuplicatedQuestionImpl().equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH)) {
            meta.append(", Embeddings Impl: " + domain.getEmbeddingsImpl());
            meta.append(", Embeddings Model: " + aiService.getEmbeddingModel(domain.getEmbeddingsImpl()));
            if (domain.getEmbeddingsImpl().equals(EmbeddingsImpl.SBERT)) {
                meta.append(", Embedding URL: " + sbertImpl.getHttpHost());
            } else {
                meta.append(", Embedding URL: " + domain.getEmbeddingsEndpoint());
            }
            meta.append(", Similarity metric: " + domain.getVectorSimilarityMetric());
        } else if (domain.getDetectDuplicatedQuestionImpl().equals(DetectDuplicatedQuestionImpl.SENTENCE_BERT)) {
            meta.append(", Embeddings Model: " + sbertImpl.getVersionAndModel().get(sbertImpl.MODEL));
            meta.append(", Query URL: " + sbertImpl.getHttpHost());
            meta.append(", Distance threshold: " + domain.getSentenceBERTDistanceThreshold());
        } else if (domain.getDetectDuplicatedQuestionImpl().equals(DetectDuplicatedQuestionImpl.WEAVIATE)) {
            meta.append(", Query URL: " + domain.getWeaviateQueryUrl());
            meta.append(", Certainty threshold: " + domain.getWeaviateCertaintyThreshold());
        }
        return meta.toString();
    }

    /**
     * TODO
     * @param listOfResults TODO
     * @param benchmarkDir TODO
     * @param objectMapper TODO
     */
    public void createBenchmarkGraphs(List<BenchmarkResult> listOfResults, File benchmarkDir, ObjectMapper objectMapper) throws JsonParseException, JsonMappingException, IOException {
        // create graphs from results

        DefaultCategoryDataset dataset_bar = new DefaultCategoryDataset();
        XYSeriesCollection dataset_scatter = new XYSeriesCollection();

        XYSeries series;
        for (BenchmarkResult result : listOfResults) {
            dataset_bar.addValue(result.getAccuracy(), result.getSystemName(), "Accuracy");
            dataset_bar.addValue(result.getPrecision(), result.getSystemName(), "Precision");
            dataset_bar.addValue(result.getRecall(), result.getSystemName(), "Recall");

            String seriesKey = result.getSystemName().toString();
            if (dataset_scatter.indexOf(seriesKey) >= 0) {
                log.warn("Series with key '" + seriesKey + "' already exists, therefore append timestamp to have unique series key.");
                seriesKey = seriesKey + "_" + new Date().getTime();
            }
            series = new XYSeries(seriesKey);
            series.add(result.getInferenceTimeInSeconds(), result.getAccuracy());
            dataset_scatter.addSeries(series);
        }

        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Accuracy, Precision, Recall");
        JFreeChart barChart = ChartFactory.createBarChart(strBuilder.toString(), "", "in %", dataset_bar, PlotOrientation.VERTICAL, true, true, false);
        strBuilder = new StringBuilder();
        strBuilder.append("Accuracy vs. Time ");
        JFreeChart scatterplot = ChartFactory.createScatterPlot(strBuilder.toString(), "Time in seconds", "Accuracy in %", dataset_scatter);

        File accuracyChartFile = new File(benchmarkDir, "accuracy_precision_recall_bar.png");
        ChartUtils.saveChartAsPNG(accuracyChartFile, barChart, 900, 600);


        File accuracyVsTimeScatterFile = new File(benchmarkDir, "accuracy_vs_time_scatter.png");
        ChartUtils.saveChartAsPNG(accuracyVsTimeScatterFile, scatterplot, 900, 600);

        /*
        // scan the benchmark folder and load all json files into list
        List<String> bechnmarkHistoryDirectories = this.getBenchmarkDirectories();

        LinkedList<List<BenchmarkResult>> benchmarkResultHistory = new LinkedList<>();
        for (String dir : bechnmarkHistoryDirectories) {
            File benchmarkD = new File(benchmarksDataPath, dir);
            File resultF = new File(benchmarkD, "results.json");

            benchmarkResultHistory.add(objectMapper.readValue(resultF, new TypeReference<List<BenchmarkResult>>(){}));
        }

        DefaultCategoryDataset dataset_history = new DefaultCategoryDataset();
        for (List<BenchmarkResult> res : benchmarkResultHistory) {
            for (BenchmarkResult bm_res: res) {
                dataset_history.addValue(bm_res.getAccuracy(), bm_res.getSystemName(), bm_res.getDateTime());
            }
        }

        // TODO:
        // -create and save benchmark history line chart
        // -Visuals of charts:
        // label bars and markers with numbers
        // change color schema
        // include line chart in pdf in api route /benchmark-report

         */
    }
}

record StringTuple(String first, String second) {}
