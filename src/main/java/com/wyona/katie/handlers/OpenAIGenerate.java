package com.wyona.katie.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.CompletionAssistant;
import com.wyona.katie.models.CompletionResponse;
import com.wyona.katie.models.CompletionTool;
import com.wyona.katie.models.PromptMessage;
import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.assistant.Assistant;
import io.github.sashirestela.openai.domain.file.FileRequest;
import io.github.sashirestela.openai.domain.file.FileResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.*;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.*;

/**
 *
 */
@Slf4j
@Component
public class OpenAIGenerate implements GenerateProvider {

    @Value("${openai.host}")
    private String openAIHost;

    private static final String ASSISTANT_TOOL_FILE_SEARCH = "file_search";

    /**
     * @see GenerateProvider#getAssistant(String, String, String, List, String, String)
     */
    public CompletionAssistant getAssistant(String id, String name, String instructions, List<CompletionTool> tools, String model, String apiToken) throws Exception {
        if (id == null || (id != null && !assistantExists(id, apiToken))) {
            if (id != null) {
                log.warn("No assistant exists with Id '" + id + "'!");
            }
            return createAssistant(new CompletionAssistant(id, name, instructions), tools, model, apiToken);
        }

        log.info("Assistant " + id + "'' already exists.");
        return new CompletionAssistant(id, name, instructions);
    }

    /**
     * @see GenerateProvider#getCompletion(List, CompletionAssistant, String, Double, String)
     */
    public CompletionResponse getCompletion(List<PromptMessage> promptMessages, CompletionAssistant assistant, String openAIModel, Double temperature, String openAIKey) throws Exception {
        if (assistant != null) {
            return assistantThread(promptMessages, assistant.getId(), temperature, openAIKey);
        } else {
            return new CompletionResponse(chatCompletion(promptMessages, openAIModel, temperature, openAIKey));
        }
    }

    /**
     * Get list of previously uploaded files
     * @return list of files, which got previously uploaded to OpenAI
     */
    private FileResponse[] getFiles(String openAIKey) throws Exception {
        SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(openAIKey).build();
        //openAI.files().getList(FileRequest.PurposeType.ASSISTANTS).join();
        //var fileResponses = openAI.files().getList(null).join();
        List<FileResponse> fileResponses = openAI.files().getList(null).join();
        return fileResponses.toArray(new FileResponse[0]);

        /*
        HttpHeaders headers = getHttpHeaders(openAIKey);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        String getFilesUrl = openAIHost + "/v1/files";
        log.info("Get files " + getFilesUrl);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.exchange(getFilesUrl, HttpMethod.GET, request, JsonNode.class);
        JsonNode responseBodyNode = response.getBody();
        log.info("JSON Response: " + responseBodyNode);

        List<String> files = new ArrayList<>();
        JsonNode dataNode = responseBodyNode.get("data");
        if (dataNode.isArray()) {
            for (int i = 0; i < dataNode.size(); i++) {
                String fileName = dataNode.get(i).get("filename").asText();
                String fileId = dataNode.get(i).get("id").asText();

                // TOOD: Add fileId
                files.add(fileName);
            }
        } else {
            log.warn("No files yet!");
        }

        return files.toArray(new String[0]);
         */
    }

    /**
     * Delete file from OpenAI
     * @param fileId OpenAI File Id, e.g. "file-RCViAw1sYooq4djLVq3eo9"
     */
    private void deleteFile(String fileId, String openAIKey) throws Exception {
        log.info("File '" + fileId + "' will be deleted from OpenAI ...");
        SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(openAIKey).build();
        openAI.files().delete(fileId).join();
    }

    /**
     * Upload file to OpenAI
     * @param file File, e.g. "/Users/michaelwechner/Desktop/Auftragsrecht.pdf"
     * @return file Id, e.g. "file-SLGUENEL9ZAPaCSZscBTcm"
     */
    private String uploadFile(File file, String openAIKey) throws Exception {
        if (!file.isFile()) {
            throw new FileNotFoundException("No such file '" + file.getAbsolutePath() + "'!");
        }
        log.info("File '" + file.getAbsolutePath() + "' will be uploaded to OpenAI ...");
        SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(openAIKey).build();
        FileRequest fileRequest = FileRequest.builder()
                .file(Paths.get(file.getAbsolutePath()))
                .purpose(FileRequest.PurposeType.ASSISTANTS)
                .build();
        FileResponse fileResponse = openAI.files().create(fileRequest).join();
        log.info("File '" + file.getAbsolutePath() + "' successfully uploaded to OpenAI.");

        return fileResponse.getId();

        /*
        HttpHeaders headers = getHttpHeaders(openAIKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        byte[] fileAsByteArray = IOUtils.toByteArray(new FileInputStream(file));

        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
        ContentDisposition contentDisposition = ContentDisposition
                .builder("form-data").build();
        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        //HttpEntity<byte[]> fileEntity = new HttpEntity<>(fileAsByteArray, fileMap);
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(fileAsByteArray);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", "assistants");
        //body.add("file", fileEntity);
        body.add("file", fileAsByteArray);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        String uploadFileUrl = openAIHost + "/v1/files";
        log.info("Upload file " + uploadFileUrl);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.exchange(uploadFileUrl, HttpMethod.POST, request, JsonNode.class);
        JsonNode responseBodyNode = response.getBody();
        log.info("JSON Response: " + responseBodyNode);
        String fileId = responseBodyNode.get("id").asText();
        return fileId;
         */
    }

    /**
     * Generate answer using OpenAI Chat Completion API https://platform.openai.com/docs/api-reference/chat
     */
    private String chatCompletion(List<PromptMessage> promptMessages, String openAIModel, Double temperature, String openAIKey) throws Exception {
        if (openAIKey != null) {
            log.info("Complete prompt using OpenAI chat completion (API key: " + openAIKey.substring(0, 7) + "******) ...");
        } else {
            throw new Exception("No OpenAI API key configured!");
        }

        String completedText = null;

        try {
            // INFO: See https://platform.openai.com/docs/api-reference/chat/create
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
            requestBodyNode.put("model", openAIModel);
            //requestBodyNode.put("model", "gpt-4o-2024-08-06");
            ArrayNode messages = mapper.createArrayNode();
            requestBodyNode.put("messages", messages);

            for (PromptMessage msg : promptMessages) {
                ObjectNode messageNode = mapper.createObjectNode();
                messageNode.put("role", msg.getRole().toString());
                messageNode.put("content", msg.getContent());
                messages.add(messageNode);
            }

            String responseFormat = null;
            if (responseFormat != null && responseFormat.equals("json")) {
                // See https://platform.openai.com/docs/guides/structured-outputs
                ObjectNode responseFormatNode = mapper.createObjectNode();
                requestBodyNode.put("response_format", responseFormatNode);

                responseFormatNode.put("type", "json_schema");
                ObjectNode jsonSchemaNode = mapper.createObjectNode();
                responseFormatNode.put("json_schema", jsonSchemaNode);
                jsonSchemaNode.put("name", "TODO");

                ObjectNode schemaNode = mapper.createObjectNode();
                jsonSchemaNode.put("schema", schemaNode);
                schemaNode.put("type", "object");
                ObjectNode propertiesNode = mapper.createObjectNode();
                schemaNode.put("properties", propertiesNode);

                ObjectNode propertyNode = mapper.createObjectNode();
                propertiesNode.put("selected-answer", propertyNode);
                propertyNode.put("type", "string");
            }

            HttpHeaders headers = getHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String requestUrl = openAIHost + "/v1/chat/completions";
            log.info("Get chat completion: " + requestUrl + " (Body: " + requestBodyNode + ")");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);

            JsonNode choicesNode = responseBodyNode.get("choices");
            if (choicesNode.isArray()) {
                completedText = choicesNode.get(0).get("message").get("content").asText();
            } else {
                log.warn("No choices!");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

        return completedText;
    }

    /***
     * Generate answer using OpenAI Assistant Thread API https://platform.openai.com/docs/api-reference/threads
     */
    private CompletionResponse assistantThread(List<PromptMessage> promptMessages, String assistantId, Double temperature, String openAIKey) throws Exception {
        log.info("Complete prompt using OpenAI assistant thread (API key: " + openAIKey.substring(0, 7) + "******) ...");

        String threadId = createThread(promptMessages, openAIKey);
        return runThread(assistantId, threadId, openAIKey);
    }

    /**
     * Check whether assistant already exists
     * @param id Assistant Id, e.g. "asst_79S9rWytfx7oNqyIr2rrJGBB"
     * @return true when assistant already exists and false otherwise
     */
    private boolean assistantExists(String id, String openAIKey) {
        SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(openAIKey).build();
        List<Assistant> assistants = openAI.assistants().getList().join();
        for (Assistant assistant : assistants) {
            if (assistant.getId().equals(id)) {
                log.info("Assistant with Id '" + id + "' exists.");
                return true;
            }
        }
        return false;

        /*
        HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
        HttpEntity<String> request = new HttpEntity<String>(headers);
        String getRunUrl = openAIHost + "/v1/assistants?order=desc&limit=20";
        log.info("Get list of assistants " + getRunUrl);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.exchange(getRunUrl, HttpMethod.GET, request, JsonNode.class);
        JsonNode responseBodyNode = response.getBody();
        log.info("JSON Response: " + responseBodyNode);
        JsonNode dataNode = responseBodyNode.get("data");
        if (dataNode.isArray()) {
            for (int i = 0; i < dataNode.size(); i++) {
                JsonNode assistantNode = dataNode.get(i);
                String assistantId = assistantNode.get("id").asText();
                if (assistantId.equals(id)) {
                    log.info("Assistant with Id '" + id + "' exists.");
                    return true;
                }
            }
        }

        return false;
         */
    }

    /**
     * Create new assistant
     * @param assistant Assistant name and instructions
     * @return assistant including its Id, e.g. "asst_79S9rWytfx7oNqyIr2rrJGBB"
     */
    private CompletionAssistant createAssistant(CompletionAssistant assistant, List<CompletionTool> tools, String openAIModel, String openAIKey) throws Exception {
        log.info("Create assistant (API key: " + openAIKey.substring(0, 7) + "******) ...");

        try {
            // INFO: See https://platform.openai.com/docs/api-reference/threads
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
            requestBodyNode.put("model", openAIModel);
            //requestBodyNode.put("model", "gpt-4o-2024-08-06");
            requestBodyNode.put("instructions", assistant.getInstructions());
            requestBodyNode.put("name", assistant.getName());

            if (true) {
                ArrayNode toolsNode = mapper.createArrayNode();
                requestBodyNode.put("tools", toolsNode);

                ObjectNode fileSearchTool = mapper.createObjectNode();
                fileSearchTool.put("type", ASSISTANT_TOOL_FILE_SEARCH);
                toolsNode.add(fileSearchTool);

                if (tools != null && tools.size() > 0) {
                    for (CompletionTool tool: tools) {
                        // TODO: Add tool dynamically
                        ObjectNode customTool = mapper.createObjectNode();
                        customTool.put("type", "function");
                        ObjectNode functionNode = mapper.createObjectNode();
                        functionNode.put("name", "get_file_path_of_relevant_document");
                        functionNode.put("description", "Get file path of the relevant document");
                        functionNode.put("strict", true);
                        ObjectNode parametersNode = mapper.createObjectNode();
                        parametersNode.put("type", "object");
                        ObjectNode propertiesNode = mapper.createObjectNode();
                        ObjectNode filePathNode = mapper.createObjectNode();
                        filePathNode.put("type", "string");
                        filePathNode.put("description", "The file path of the relevant document");
                        propertiesNode.put(tool.getFunctionArgument(), filePathNode);
                        parametersNode.put("properties", propertiesNode);
                        ArrayNode arrayOfPropertiesNode = mapper.createArrayNode();
                        arrayOfPropertiesNode.add(tool.getFunctionArgument());
                        parametersNode.put("required", arrayOfPropertiesNode);
                        parametersNode.put("additionalProperties", false);
                        functionNode.put("parameters", parametersNode);
                        customTool.put("function", functionNode);

                        toolsNode.add(customTool);
                    }
                }
            }

            HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String requestUrl = openAIHost + "/v1/assistants";
            log.info("Create assistant: " + requestUrl + " (Body: " + requestBodyNode + ")");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);

            String assistantId = responseBodyNode.get("id").asText();
            if (assistantId != null) {
                assistant.setId(assistantId);
                return assistant;
            } else {
                log.error("No assistant Id!");
                return null;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create new thread
     * @return thread Id
     */
    private String createThread(List<PromptMessage> promptMessages, String openAIKey) throws Exception {
        log.info("Create thread (API key: " + openAIKey.substring(0, 7) + "******) ...");

        try {
            // INFO: See https://platform.openai.com/docs/api-reference/threads
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
            ArrayNode messages = mapper.createArrayNode();
            requestBodyNode.put("messages", messages);

            for (PromptMessage msg : promptMessages) {
                ObjectNode messageNode = mapper.createObjectNode();
                messageNode.put("role", msg.getRole().toString());
                messageNode.put("content", msg.getContent());

                // Add attachment(s), see for example https://platform.openai.com/docs/assistants/tools/file-search
                File[] attachments = msg.getAttachments();
                if (attachments != null && attachments.length > 0) {
                    ArrayNode attachmentsNode = mapper.createArrayNode();
                    messageNode.put("attachments", attachmentsNode);

                    deleteOutdatedFilesFromOpenAI(openAIKey);

                    FileResponse[] previouslyUploadedFiles = getFiles(openAIKey);
                    log.info("Number of uploaded files: " + previouslyUploadedFiles.length);
                    for (FileResponse previouslyUploadedFile : previouslyUploadedFiles) {
                        log.info("Previously uploaded file: " + previouslyUploadedFile.getFilename() + " (" + new Date(previouslyUploadedFile.getCreatedAt() * 1000) + ")");
                    }

                    for (File attachment : attachments) {
                        String fileId = getFileId(attachment, previouslyUploadedFiles, openAIKey);
                        if (fileId != null) {
                            ObjectNode attachmentNode = mapper.createObjectNode();
                            attachmentNode.put("file_id", fileId);

                            ArrayNode toolsNode = mapper.createArrayNode();
                            ObjectNode typeNode = mapper.createObjectNode();
                            typeNode.put("type", ASSISTANT_TOOL_FILE_SEARCH);
                            toolsNode.add(typeNode);
                            attachmentNode.put("tools", toolsNode);

                            attachmentsNode.add(attachmentNode);
                        } else {
                            log.error("No file Id available for attachment '" + attachment.getAbsolutePath() + "'!");
                        }
                    }
                } else {
                    log.info("No attachments provided.");
                }

                messages.add(messageNode);
            }

            HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String createThreadUrl = openAIHost + "/v1/threads";
            log.info("Create thread: " + createThreadUrl + " (Body: " + requestBodyNode + ")");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(createThreadUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);

            String threadId = responseBodyNode.get("id").asText();
            if (threadId == null) {
                log.error("No thread Id!");
                return null;
            }

            /*
            ObjectNode requestBodyNode2 = mapper.createObjectNode();
            requestBodyNode2.put("role", "user");
            requestBodyNode2.put("content", "How can I upload a file");

            HttpEntity<String> request2 = new HttpEntity<String>(requestBodyNode2.toString(), headers);

            String createMessageUrl = openAIHost + "/v1/threads/" +threadId + "/messages";
            log.info("Create message " + createMessageUrl + " (Body: " + requestBodyNode2 + ")");
            RestTemplate restTemplate2 = new RestTemplate();
            ResponseEntity<JsonNode> response2 = restTemplate2.exchange(createMessageUrl, HttpMethod.POST, request2, JsonNode.class);
            JsonNode responseBodyNode2 = response2.getBody();
            log.info("JSON Response: " + responseBodyNode2);
             */

            return threadId;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete all files from OpenAI which are not up to date
     */
    private void deleteOutdatedFilesFromOpenAI(String openAIKey) throws Exception {
        FileResponse[] previouslyUploadedFiles = getFiles(openAIKey);
        log.info("Number of uploaded files: " + previouslyUploadedFiles.length);
        for (FileResponse previouslyUploadedFile : previouslyUploadedFiles) {
            Date lastModifiedRemote = new Date(previouslyUploadedFile.getCreatedAt() * 1000);
            log.info("Previously uploaded file: " + previouslyUploadedFile.getFilename() + " (" + lastModifiedRemote + ")");
            if (new File(previouslyUploadedFile.getFilename()).isFile()) {
                Date lastModifiedLocal = new Date(new File(previouslyUploadedFile.getFilename()).lastModified());
                log.info("Last modified of local file: " + lastModifiedLocal);
                try {
                    if (lastModifiedLocal.getTime() > lastModifiedRemote.getTime()) {
                        log.info("Local file (" + lastModifiedLocal + ") is more recent than remote file (" + lastModifiedRemote + ").");
                        deleteFile(previouslyUploadedFile.getId(), openAIKey);
                    } else {
                        log.info("Remote file '" + previouslyUploadedFile.getId() + "' will not be deleted from OpenAI, because it is up to date.");
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.warn("No such file '" + previouslyUploadedFile.getFilename() + "'!");
                // TODO: Consider to delete all files from OpenAI, which do not exist locally
            }
        }
    }

    /**
     * Get file Id of file which was previously uploaded or will be uploaded now
     * @param file File (previously uploaded or will be uploaded) for which Id is being retrieved
     * @param alreadyUploadedFiles Already uploaded files
     * @return file Id, e.g. "file-SLGUENEL9ZAPaCSZscBTcm" or "file-D72TcUvBpxs4aKa5BUssCA"
     */
    private String getFileId(File file, FileResponse[] alreadyUploadedFiles, String openAIKey) throws Exception {
        String filePathNormalized = Normalizer.normalize(file.getAbsolutePath(), Normalizer.Form.NFKD);
        for (FileResponse alreadyUploadedFile: alreadyUploadedFiles) {
            log.debug("Previously uploaded file: " + alreadyUploadedFile.getFilename());
            // Files like for example "Auftragsrecht inkl. gemischte Verträge (Beendigung OR 404) in a nutshell.pdf" with special characters like for example "ä" can use different characters / sequences to represent special characters
            // Therefore use Normalizer to use the same form and being able to compare
            // See for example https://stackoverflow.com/questions/64574044/comparing-strings-with-equivalent-but-different-unicode-code-points-in-java-kotl
            String alreadyUploadedFilePathNormalized = Normalizer.normalize(alreadyUploadedFile.getFilename(), Normalizer.Form.NFKD);
            if (filePathNormalized.equals(alreadyUploadedFilePathNormalized)) {
                log.info("File was previously uploaded: " + file.getAbsolutePath() + " (Created: " + new Date(alreadyUploadedFile.getCreatedAt() * 1000) + ")");
                // TODO: Check last modified and if local file is more recent, then upload again and overwrite remote file
                return alreadyUploadedFile.getId();
            }
        }

        return uploadFile(file, openAIKey);
    }

    /**
     * Get file
     * @param fileId OpenAI file Id
     */
    private File getFile(String fileId, String openAIKey) throws Exception {
        FileResponse[] fileResponses = getFiles(openAIKey);
        for (FileResponse fileResponse: fileResponses) {
            if (fileResponse.getId().equals(fileId)) {
                return new File(fileResponse.getFilename());
            }
        }
        log.warn("No uploaded file with file Id '" + fileId + "'!");
        return null;
    }

    /**
     * @return response message
     */
    private CompletionResponse runThread(String assistantId, String threadId, String openAIKey) throws Exception {
        log.info("Run thread (API key: " + openAIKey.substring(0, 7) + "******) ...");

        try {
            ObjectMapper mapper = new ObjectMapper();

            ObjectNode requestBodyNode2 = mapper.createObjectNode();
            requestBodyNode2.put("assistant_id", assistantId);

            HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode2.toString(), headers);

            String runThreadUrl = openAIHost + "/v1/threads/" +threadId + "/runs";
            log.info("Run thread " + runThreadUrl + " (Body: " + requestBodyNode2 + ")");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(runThreadUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);
            String runId = responseBodyNode.get("id").asText();

            String status = responseBodyNode.get("status").asText();
            int timeoutCounter = 0;
            while (!(status.equals("completed") || status.equals("requires_action"))) {
                timeoutCounter++;
                if (timeoutCounter > 30) { // TODO: Make timeout max configurable
                    return new CompletionResponse("Timeout!");
                }
                try {
                    log.info("Sleep for 1 second ...");
                    Thread.sleep(1000);
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
                responseBodyNode = getRunResponseBodyNode(threadId, runId, openAIKey);
                status = responseBodyNode.get("status").asText();
                log.info("Thread status: " + status);
            }

            log.info("Response generation completed.");

            if (status.equals("completed")) {
                return new CompletionResponse(getResponseMessage(threadId, openAIKey));
            } else if (status.equals("requires_action")) {
                CompletionResponse completionResponse = new CompletionResponse("Tool call completed");
                Map<String, String> arguments = getArguments(responseBodyNode);
                for (Map.Entry<String, String> entry : arguments.entrySet()) {
                    completionResponse.addFunctionArgument(entry.getKey(), entry.getValue());
                }
                return completionResponse;
            } else {
                log.warn("No such status '" + status + "' implemented!");
                return new CompletionResponse(getResponseMessage(threadId, openAIKey));
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get arguments from tool call response
     * @param node Json node containing tool call response
     * @return hash map of argument key / value pairs, e.g. {"file_path":"/Users/michaelwechner/Desktop/Auftragsrecht.pdf"}
     */
    private HashMap<String, String> getArguments(JsonNode node) {
        String arguments = node.get("required_action").get("submit_tool_outputs").get("tool_calls").get(0).get("function").get("arguments").asText();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode argumentsNode = mapper.readTree(arguments);
            log.info("Arguments: " + argumentsNode);
            Iterator<String> iterator = argumentsNode.fieldNames();
            HashMap<String, String> map = new HashMap<>();
            while (iterator.hasNext()) {
                String key = iterator.next();
                map.put(key, argumentsNode.get(key).asText());
            }
            return map;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @return LLM response message
     */
    private String getResponseMessage(String threadId, String openAIKey) throws Exception {
        log.info("Get response message (API key: " + openAIKey.substring(0, 7) + "******) ...");

        try {
            HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(headers);
            String getMessagesUrl = openAIHost + "/v1/threads/" +threadId + "/messages";
            log.info("Get messages " + getMessagesUrl);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(getMessagesUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);

            JsonNode dataNode = responseBodyNode.get("data");
            if (dataNode.isArray()) {
                JsonNode textNode = dataNode.get(0).get("content").get(0).get("text");

                String completedText = textNode.get("value").asText();

                // INFO: Previously uploaded files might get cited
                if (textNode.has("annotations")) {
                    JsonNode annotationsNode = textNode.get("annotations");
                    if (annotationsNode.isArray() && annotationsNode.size() > 0) {
                        for (int i = 0; i < annotationsNode.size(); i++) {
                            JsonNode annotationNode = annotationsNode.get(i);

                            // File citations are created by the file_search tool and define references to a specific file that was uploaded and used by the Assistant to generate the response
                            if (annotationNode.get("type").asText().equals("file_citation")) {
                                String fileId = annotationNode.get("file_citation").get("file_id").asText();
                                log.info("File citation: " + fileId);
                                File file = getFile(fileId, openAIKey);
                                log.info("File: " + file.getAbsolutePath());
                            } else {
                                log.info("Annotation type: " + annotationNode.get("type").asText());
                            }
                        }
                    }
                } else {
                    log.info("No annotations available.");
                }

                // TODO: Return file citations together with completed text, such that frontend can link text with citations
                return completedText;
            } else {
                log.error("No data!");
                return "No response available!";
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     *
     */
    private JsonNode getRunResponseBodyNode(String threadId, String runId, String openAIKey) throws Exception {
        HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
        HttpEntity<String> request = new HttpEntity<String>(headers);
        String getRunUrl = openAIHost + "/v1/threads/" + threadId + "/runs/" + runId;
        log.info("Get run " + getRunUrl);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.exchange(getRunUrl, HttpMethod.GET, request, JsonNode.class);
        JsonNode responseBodyNode = response.getBody();
        log.info("JSON Response: " + responseBodyNode);
        return responseBodyNode;
    }

    /**
     *
     */
    private HttpHeaders getAssistantHttpHeaders(String openAIKey) {
        HttpHeaders headers = getHttpHeaders(openAIKey);
        headers.set("OpenAI-Beta", "assistants=v2");
        return headers;
    }

    /**
     * Get default headers
     */
    private HttpHeaders getHttpHeaders(String openAIKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + openAIKey);
        return headers;
    }
}
