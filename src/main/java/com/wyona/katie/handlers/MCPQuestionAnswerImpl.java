package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * MCP (Model Context Protocol) based search implementation
 * 1) The MCP client retrieves a list of tools from the configured MCP servers
 * 2) The MCP client sends the user query together with the tools to the LLM
 * 3) The LLM decides which tools to use and the argument values
 * 4) The MCP client is querying the MCP servers based on the selected tools
 * 5) The LLM generates an answer based on the retrieved context from the MCP servers
 *
 * https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
 * https://github.com/spring-projects/spring-ai-examples/tree/main/model-context-protocol/web-search
 * https://modelcontextprotocol.io/sdk/java/mcp-client
 */
@Slf4j
@Component
public class MCPQuestionAnswerImpl implements QuestionAnswerHandler {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private SyncMcpToolCallbackProvider toolCallbackProvider;

    /**
     * @see QuestionAnswerHandler#deleteTenant(Context)
     */
    public void deleteTenant(Context domain) {
        log.info("TODO: MCP implementation of deleting tenant ...");
        // TODO
    }

    /**
     * @see QuestionAnswerHandler#createTenant(Context)
     */
    public String createTenant(Context domain) {
        log.info("TODO: MCP implementation of creating tenant ...");
        // TODO
        return null;
    }

    /**
     * @see QuestionAnswerHandler#delete(String, Context)
     */
    public boolean delete(String uuid, Context domain) {
        log.info("TODO: Delete Q&A with UUID '" + uuid + "' of domain '" + domain.getId() + "' from MCP implementation ...");
        // TODO
        return false;
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
     * @see QuestionAnswerHandler#train(QnA, Context, boolean)
     */
    public void train(QnA qna, Context context, boolean indexAlternativeQuestions) {
        log.info("TODO: Index QnA '" + qna.getUuid() + "' with MCP implementation ...");
        // TODO
    }

    /**
     * @see QuestionAnswerHandler#train(QnA[], Context, boolean)
     */
    public QnA[] train(QnA[] qnas, Context domain, boolean indexAlternativeQuestions) {
        log.info("TODO: Implement batch training.");
        for (QnA qna: qnas) {
            train(qna, domain, indexAlternativeQuestions);
        }

        // TODO: Only return QnAs which got trained successfully
        return qnas;
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
    public Hit[] getAnswers(String question, List<String> classifications, Context domain, int limit) {
        List<Hit> hits = new ArrayList<Hit>();

        log.info("Get answer from MCP implementation ...");

        String _answer = getAnswer(question);

        String uuid = null;
        ContentType answerContentType = ContentType.TEXT_PLAIN;
        String orgQuestion = null;
        Date dateAnswered = null;
        Date dateAnswerModified = null;
        Date dateOriginalQuestionSubmitted = null;
        Answer answer = new Answer(question, _answer, answerContentType,null, classifications, null, null, dateAnswered, dateAnswerModified, null, domain.getId(), uuid, orgQuestion, dateOriginalQuestionSubmitted, true, null, true, null);

        double score = -1; // TODO: Get score
        hits.add(new Hit(answer, score));

        return hits.toArray(new Hit[0]);
    }

    /**
     *
     */
    private String getAnswer(String question) {
        // INFO: MCP servers configuration: src/main/resources/mcp-servers-config.json
        List<ToolCallback> tools = Arrays.stream(toolCallbackProvider.getToolCallbacks()).toList();
        log.info("Available tool callbacks: " + tools.size());
        List<ToolCallback> selectedTools = new ArrayList<>();
        for (ToolCallback toolCallback : tools) {
            //log.info("Tool Callback: " + toolCallback.getToolDefinition().name() + ": " + toolCallback.getToolDefinition().description());
            if (toolCallback.getToolDefinition().name().equals("milvus_vector_search") || toolCallback.getToolDefinition().name().equals("milvus_list_collections")) {
                selectedTools.add(toolCallback);
            }
        }
        for (ToolCallback toolCallback : selectedTools) {
            log.info("Selected Tool Callback: " + toolCallback.getToolDefinition().name() + ": " + toolCallback.getToolDefinition().description());
        }

        if (true) {
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .user(question)
                    .toolCallbacks(selectedTools)
                    //.toolCallbacks(tools)
                    .call()
                    .chatResponse()
                    .getResult()
                    .getOutput()
                    .getText();
        } else {
            return "Mock answer";
        }
    }
}
