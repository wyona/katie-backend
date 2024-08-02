package com.wyona.katie.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.ai.models.TextEmbedding;
import com.wyona.katie.models.*;
import com.wyona.katie.models.discord.DiscordDomainMapping;
import com.wyona.katie.models.discord.DiscordEvent;
import com.wyona.katie.models.insights.Event;
import com.wyona.katie.models.matrix.MatrixConversationValues;
import com.wyona.katie.models.msteams.MSTeamsConversationValues;
import com.wyona.katie.models.msteams.MSTeamsDomainMapping;
import com.wyona.katie.models.slack.ConnectStatus;
import com.wyona.katie.models.slack.SlackDomainMapping;
import com.wyona.katie.models.slack.SlackEvent;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Component
public class DataRepositoryService {

    @Autowired
    private IAMService iamService;

    @Autowired
    private XMLService xmlService;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private UsersXMLFileService usersXMLFileService;

    private static final int MAX_LIMIT = 100; // TODO: Make max limit configurable

    @Value("${driver.class.name}")
    private String driverClassName;

    @Value("${db.url}")
    private String dbURL;

    @Value("${db.username}")
    private String dbUsername;

    @Value("${db.password}")
    private String dbPassword;

    @Value("${new.context.mail.body.host}")
    private String defaultHostname;

    private static final String TABLE_DISCORD_KATIE_DOMAIN = "DISCORD_KATIE_DOMAIN";
    private static final String DISCORD_GUILD_ID = "DISCORD_GUILD_ID";
    private static final String DISCORD_CHANNEL_ID = "DISCORD_CHANNEL_ID";
    private static final String DISCORD_DOMAIN_ID = "KATIE_DOMAIN_ID";
    private static final String DISCORD_MSG_ID = "DISCORD_MSG_ID";

    private static final String TABLE_SLACK_TEAM_KATIE_DOMAIN = "SLACK_TEAM_KATIE_DOMAIN";
    private static final String TABLE_SLACK_TEAM_TOKEN_USERID = "SLACK_TEAM_TOKEN_USERID";
    private static final String SLACK_TEAM_ID = "SLACK_TEAM_ID";
    private static final String SLACK_CHANNEL_ID = "SLACK_CHANNEL_ID";
    private static final String SLACK_BEARER_TOKEN = "BEARER_TOKEN";
    private static final String SLACK_MSG_TS = "SLACK_MSG_TS";

    private static final String TABLE_MS_TEAM_KATIE_DOMAIN = "MS_TEAM_KATIE_DOMAIN";

    private static final String TABLE_MATRIX_KATIE_DOMAIN = "MATRIX_KATIE_DOMAIN";

    private static final String TABLE_QUESTION = "QUESTION";
    private static final String TABLE_RESUBMITTED_QUESTION = "RESUBMITTED_QUESTION";

    private static final String TABLE_CHANNEL_SLACK = "CHANNEL_SLACK";
    private static final String TABLE_CHANNEL_DISCORD = "CHANNEL_DISCORD";
    private static final String TABLE_CHANNEL_MS_TEAMS = "CHANNEL_MS_TEAMS";
    private static final String TABLE_CHANNEL_WEBHOOK = "CHANNEL_WEBHOOK";

    private static final String TABLE_ANALYTICS = "ANALYTICS";

    private static final String TABLE_DOMAIN_TAG_NAME = "DOMAIN_TAG_NAME";

    private static final String MS_TEAM_ID = "MS_TEAM_ID";
    private static final String MS_TEAM_DOMAIN_ID = "KATIE_DOMAIN_ID";

    private static final String QUESTIONER_USER_ID = "QUESTIONER_USER_ID";
    private static final String QUESTIONER_LANGUAGE = "QUESTIONER_LANGUAGE";
    private static final String CHANNEL_TYPE = "CHANNEL_TYPE";
    private static final String CHANNEL_REQUEST_ID = "CHANNEL_REQUEST_UUID";
    private static final String EMAIL = "EMAIL";
    private static final String FCM_TOKEN = "FCM_TOKEN";

    private static final String TIMESTAMP_CREATED = "TIMESTAMP_CREATED";
    private static final String KATIE_DOMAIN_ID = "KATIE_DOMAIN_ID";
    private static final String ANSWER_LINK_TYPE = "ANSWER_LINK_TYPE";
    private static final String OWNERSHIP = "OWNERSHIP";

    private static final String QUESTION_QUESTION = "QUESTION";
    private static final String QUESTION_USER_NAME = "USER_NAME";
    private static final String QUESTION_DOMAIN_ID = "DOMAIN_ID";
    private static final String QUESTION_ANSWER_UUID = "ANSWER_UUID";
    private static final String QUESTION_ANSWER_SCORE = "SCORE";
    private static final String QUESTION_ANSWER_SCORE_THRESHOLD = "SCORE_THRESHOLD";
    private static final String QUESTION_MODERATION_STATUS = "MODERATION_STATUS";
    private static final String QUESTION_CHANNEL_TYPE = "CHANNEL_TYPE";
    private static final String QUESTION_CHANNEL_REQUEST_ID = "CHANNEL_REQUEST_UUID";
    private static final String QUESTION_OFFSET_RESULTS = "OFFSET_RESULTS";
    private static final String QUESTION_CLIENT_MESSAGE_ID = "CLIENT_MESSAGE_ID";
    private static final String QUESTION_CLASSIFICATIONS = "CLASSIFICATIONS";
    private static final String CLASSIFICATION_SEPARATOR = "::";

    private static final String TIMESTAMP = "TIMESTAMP";
    private static final String RESPONDENT = "RESPONDENT";
    private static final String STATUS = "STATUS";

    private static final String ROW_COUNT = "rowcount";

    private static final String EMBEDDING_TEXT_FIELD = "text";
    private static final String EMBEDDING_VECTOR_FIELD = "embedding";

    private static final String PREDICTED_LABELS_FIELD = "predicted-labels";

    /**
     * Write embedding into a file
     * @param vector Embedding vector
     * @param text Text which got vectorized
     * @param file File where embedding and text will be saved
     */
    public void saveEmbedding(Vector vector, String text, File file) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put(EMBEDDING_TEXT_FIELD, text);

        if (vector instanceof FloatVector) {
            rootNode.put("length", ((FloatVector)vector).getLength());

            ArrayNode embeddingNode = objectMapper.createArrayNode();
            rootNode.put(EMBEDDING_VECTOR_FIELD, embeddingNode);
            for (float value : ((FloatVector)vector).getValues()) {
                embeddingNode.add(value);
            }
        } else {
            log.warn("TODO: Save byte vector");
        }

        try {
            objectMapper.writeValue(file, rootNode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @param file File containing embedding
     */
    public TextEmbedding readEmbedding(File file) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(file);

        String text = rootNode.get(EMBEDDING_TEXT_FIELD).asText();

        JsonNode embeddingNode = rootNode.get(EMBEDDING_VECTOR_FIELD);
        float[] vector = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = embeddingNode.get(i).floatValue();
        }

        TextEmbedding textEmbedding = new TextEmbedding(text, vector);

        return textEmbedding;
    }

    /**
     * Add access/bearer token
     */
    private void addSlackBearerToken(String teamId, String bearerToken, String userId) throws Exception {
        log.info("Add access/bearer token of Slack team '" + teamId + "' ...");
        String sql = "INSERT INTO " + TABLE_SLACK_TEAM_TOKEN_USERID + " VALUES ('" + teamId + "', '" + bearerToken + "', '" + userId + "')";
        insert(sql);
    }

    /**
     * Update access/bearer token
     * @param botUserId User Id of Katie bot, which is generated per Slack team/workspace
     */
    public void updateSlackBearerToken(String teamId, String bearerToken, String botUserId) throws Exception {
        log.info("Update access/bearer token of Slack team '" + teamId + "' and bot user Id '" + botUserId + "' ...");

        deleteSlackBearerToken(teamId);
        addSlackBearerToken(teamId, bearerToken, botUserId);

        /*
        String sql = "Update " + TABLE_SLACK_TEAM_TOKEN_USERID + " set " + SLACK_BEARER_TOKEN + "='" + bearerToken + "', USER_ID='" + userId + "'  where SLACK_TEAM_ID = '" + teamId + "'";
        updateTableRecord(sql);
         */
    }

    /**
     * Delete access/bearer token
     */
    private void deleteSlackBearerToken(String teamId) throws Exception {
        log.info("Delete access/bearer token of Slack team '" + teamId + "' ...");

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        String sql = "Delete from " + TABLE_SLACK_TEAM_TOKEN_USERID + " where " + SLACK_TEAM_ID + "='" + teamId + "'";
        stmt.executeUpdate(sql);
        stmt.close();

        conn.close();
    }

    /**
     * @param echoData  data to be sent to configured webhook, see for example https://github.com/wyona/katie-4-faq/blob/main/clients/katie4faq-nodejs-proxy/src/proxy.ts used by Veeting
     */
    public String addWebhookEchoData(String echoData, String domainId) throws Exception {
        log.info("Add Webhook echo data ...");

        String uuid = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO " + TABLE_CHANNEL_WEBHOOK + " VALUES ('" + uuid + "', '" + domainId + "', '" + echoData + "')";
        insert(sql);

        return uuid;
    }

    /**
     * @param uuid UUID
     * @return data to be sent to configured webhook, see for example https://github.com/wyona/katie-4-faq/blob/main/clients/katie4faq-nodejs-proxy/src/proxy.ts used by Veeting
     */
    public String getWebhookEchoData(String uuid) {
        log.info("Get Webhook echo data ...");

        String domainId = null;
        String webhookEchoData = null;

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select * from " + TABLE_CHANNEL_WEBHOOK + " where UUID = '" + uuid + "'";
            log.info("Try to get conversation values: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainId = rs.getString("DOMAIN_ID");
                webhookEchoData = rs.getString("ECHO_DATA");
            }
            rs.close();
            stmt.close();
            conn.close();

            return webhookEchoData;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param values Matrix converstion values (UUID of QnA, domain Id, Matrix user ID, Matrix room ID)
     */
    public void addMatrixConversationValues(MatrixConversationValues values) throws Exception {
        log.info("Add Matrix conversation values ...");
        String sql = "INSERT INTO " + "CHANNEL_MATRIX" + " VALUES ('" + values.getUuid() + "', '" + values.getDomainId() + "', '" + values.getUserId() + "' , '" + values.getRoomId() + "', '" + values.getEventId() + "')";
        insert(sql);
    }

    /**
     * @oaram uuid UUID of QnA
     */
    public MatrixConversationValues getMatrixConversationValues(String uuid) {
        log.info("Get Matrix conversation values ...");

        String domainId = null;
        String userId = null;
        String roomId = null;
        String eventId = null;

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select * from " + "CHANNEL_MATRIX" + " where " + CHANNEL_REQUEST_ID + " = '" + uuid + "'";
            log.info("Try to get conversation values: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainId = rs.getString("DOMAIN_ID");
                userId = rs.getString("MATRIX_USER_ID");
                roomId = rs.getString("MATRIX_ROOM_ID");
                eventId = rs.getString("MATRIX_EVENT_ID");
            }
            rs.close();
            stmt.close();
            conn.close();

            MatrixConversationValues values = new MatrixConversationValues(userId, roomId, eventId);
            values.setUuid(uuid);
            values.setDomainId(domainId);
            return values;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     *
     */
    public void addEmailConversationValues(String uuid, String domainId, String emailUUID) throws Exception {
        log.info("Add email conversation values ...");
        String sql = "INSERT INTO " + "CHANNEL_EMAIL" + " VALUES ('" + uuid + "', '" + domainId + "', '" + emailUUID + "')";
        insert(sql);
    }

    /**
     * @oaram uuid UUID
     * @rerturn UUID associated with email message, e.g. TODDO
     */
    public String getEmailConversationValues(String uuid) {
        log.info("Get E-Mail conversation values ...");

        String emailUUID = null;
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select * from " + "CHANNEL_EMAIL" + " where " + CHANNEL_REQUEST_ID + " = '" + uuid + "'";
            log.info("Try to get conversation values: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                emailUUID = rs.getString("EMAIL_FROM_UUID");
            }
            rs.close();
            stmt.close();
            conn.close();

            return emailUUID;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @oaram uuid Channel request UUID, e.g. "2d4d85bb-6f31-4537-bbc5-1abf81b5563c"
     * @param domainId Katie domain Id, e.g. "ROOT" or "3139c14f-ae63-4fc4-abe2-adceb67988af"
     * @param discordGuildId Discord guild Id, e.g. "996391257549053952"
     * @param discordChannelId Discord channel Id, e.g. "996391611275694131"
     * @param discordMsgId Discord message Id, e.g. "1009007240541384785"
     */
    public void addDiscordConversationValues(String uuid, String domainId, String discordGuildId, String discordChannelId, String discordMsgId) throws Exception {
        log.info("Add Discord conversation values ...");
        String sql = "INSERT INTO " + TABLE_CHANNEL_DISCORD + " VALUES ('" + uuid + "', '" + domainId + "', '" + discordGuildId + "', '" + discordChannelId + "', '" + discordMsgId + "', null)";
        insert(sql);
    }

    /**
     * @param discordMsgId Discord message Id, e.g. "1009007240541384785"
     * @param discordThreadChannelId Discord thread channel Id, e.g. "1008700272882765874"
     */
    public void linkDiscordThreadChannel(String discordMsgId, String discordThreadChannelId) throws Exception {
        log.info("Connect Discord thread channel with original message ...");

        String sql = "Update " + TABLE_CHANNEL_DISCORD + " set " + "DISCORD_THREAD_CHANNEL_ID" + "='" + discordThreadChannelId + "' where " + "DISCORD_MSG_ID" + "='" + discordMsgId + "'";

        updateTableRecord(sql);
    }

    /**
     * Get Discord event for a particular Discord message Id
     * @param msgId Discord message Id, e.g. "1009007240541384785"
     * @return Discord event containing channel request Id and channel Id (e.g. "996391611275694131")
     */
    public DiscordEvent getDiscordConversationValues(String msgId) {
        log.info("Get Discord conversation values ...");

        DiscordEvent event = new DiscordEvent();

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select * from " + TABLE_CHANNEL_DISCORD + " where " + "DISCORD_MSG_ID" + " = '" + msgId + "'";
            log.info("Try to get conversation values: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                event.setChannelRequestId(rs.getString(CHANNEL_REQUEST_ID));
                event.setChannelId(rs.getString(DISCORD_CHANNEL_ID));
                event.setMsgId(rs.getString(DISCORD_MSG_ID));
                // TODO: Also set other values!
            }
            rs.close();
            stmt.close();
            conn.close();

            return event;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get Discord event for a particular channel request Id
     * @param requestId Channel request Id
     * @return Discord event containing channel Id (e.g. "996391611275694131")
     */
    public DiscordEvent getDiscordConversationValuesForChannelRequestId(String requestId) {
        log.info("Get Discord conversation values for channel request Id '" + requestId + "' ...");

        DiscordEvent event = new DiscordEvent();

        String sql = "Select * from " + TABLE_CHANNEL_DISCORD + " where " + CHANNEL_REQUEST_ID + " = '" + requestId + "'";
        log.info("Try to get Disord conversation values: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                event.setChannelRequestId(rs.getString(CHANNEL_REQUEST_ID));
                event.setGuildId(rs.getString(DISCORD_GUILD_ID));
                event.setChannelId(rs.getString(DISCORD_CHANNEL_ID));
                event.setMsgId(rs.getString(DISCORD_MSG_ID));
                // TODO: Also set other values!
            }
            rs.close();
            stmt.close();
            conn.close();

            return event;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param threadChannelId Discord thread channel Id
     * @return Discord event containing channel Id (e.g. "C02AGB0BLQ4") and timestamp of parent message
     */
    public DiscordEvent getDiscordConversationValuesForThreadChannelId(String threadChannelId) {
        log.info("Get Discord conversation values ...");

        DiscordEvent event = new DiscordEvent();

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select * from " + TABLE_CHANNEL_DISCORD + " where " + "DISCORD_THREAD_CHANNEL_ID" + " = '" + threadChannelId + "'";
            log.info("Try to get conversation values: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                event.setChannelRequestId(rs.getString(CHANNEL_REQUEST_ID));
                event.setDomainId(rs.getString("DOMAIN_ID"));
                event.setChannelId(rs.getString(DISCORD_CHANNEL_ID));
                event.setMsgId(rs.getString(DISCORD_MSG_ID));
                // TODO: Also set other values DISCORD_GUILD_ID, DISCORD_THREAD_CHANNEL_ID!
            }
            rs.close();
            stmt.close();
            conn.close();

            return event;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @oaram uuid Channel request UUID, e.g. "2d4d85bb-6f31-4537-bbc5-1abf81b5563c"
     * @param msgTs Timestamp of parent message, e.g. "1642975755.001100" (https://api.slack.com/messaging/retrieving#finding_threads, https://api.slack.com/messaging/sending#threading)
     */
    public void addSlackConversationValues(String uuid, String domainId, String slackTeamId, String slackChannelId, String msgTs) throws Exception {
        log.info("Add Slack conversation values ...");
        String sql = "INSERT INTO " + TABLE_CHANNEL_SLACK + " VALUES (" + addQuotes(uuid) + ", " + addQuotes(domainId) + ", " + addQuotes(slackChannelId) + ", " + addQuotes(msgTs) + ", " + addQuotes(slackTeamId) + ")";
        insert(sql);
    }

    /**
     * @oaram uuid Channel request UUID, e.g. "2d4d85bb-6f31-4537-bbc5-1abf81b5563c"
     * @return slack event containing channel Id (e.g. "C02AGB0BLQ4") and timestamp of parent message
     */
    public SlackEvent getSlackConversationValues(String uuid) {
        log.info("Get Slack conversation values ...");

        SlackEvent event = new SlackEvent();

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select * from " + TABLE_CHANNEL_SLACK + " where " + CHANNEL_REQUEST_ID + " = '" + uuid + "'";
            log.info("Try to get conversation values: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                event.setChannel(rs.getString(SLACK_CHANNEL_ID));
                event.setTeam(rs.getString(SLACK_TEAM_ID));
                event.setTs(rs.getString(SLACK_MSG_TS));
            }
            rs.close();
            stmt.close();
            conn.close();

            return event;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get channel request Id
     * @param msgTs Message timestamp, e.g. '1659692977.563019'
     * @return channel request Id
     */
    public String getSlackChannelRequestId(String msgTs) {
        log.info("Get Slack channel request id ...");

        String channelRequestId = null;

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select * from " + TABLE_CHANNEL_SLACK + " where " + SLACK_MSG_TS + " = '" + msgTs + "'";
            log.info("Try to get Slack channel request Id: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                channelRequestId = rs.getString(CHANNEL_REQUEST_ID);
            }
            rs.close();
            stmt.close();
            conn.close();

            return channelRequestId;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     *
     */
    public void addMSTeamsConversationValues(MSTeamsConversationValues values) throws Exception {
        log.info("Add MS Teams conversation values ...");
        String userName = null;
        String sql = "INSERT INTO " + TABLE_CHANNEL_MS_TEAMS + " VALUES ('" + values.getUuid() + "', '" + values.getDomainId() + "', '" + values.getServiceUrl() + "' , '" + values.getConversationId() + "', null, '" + values.getMessageId() + "', '" + values.getKatieBotId() + "', null, '" + values.getMsTeamsUserId() + "', " + addQuotes(userName) + ", " + addQuotes(values.getTeamId())+ ", " + addQuotes(values.getChannelId()) + ")";
        insert(sql);
    }

    /**
     * @oaram uuid Channel request Id
     */
    public MSTeamsConversationValues getMSTeamsConversationValues(String uuid) {
        log.info("Get MS Teams conversation values ...");

        String teamId = null;
        String channelId = null;
        String domainId = null;
        String serviceUrl = null;
        String conversationId = null;
        String messageId = null;
        String katieBotId = null;
        String msTeamsUserId = null;
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            // TODO: Rename UUID_RESUBMITTED_QUESTION to CHANNEL_REQUEST_ID
            String sql = "Select * from " + TABLE_CHANNEL_MS_TEAMS + " where UUID_RESUBMITTED_QUESTION = '" + uuid + "'";
            log.info("Try to get conversation values: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                teamId = rs.getString("MS_TEAMS_TEAM_ID");
                channelId = rs.getString("MS_TEAMS_CHANNEL_ID");
                domainId = rs.getString("DOMAIN_ID");
                serviceUrl = rs.getString("SERVICE_URL");
                conversationId = rs.getString("CONVERSATION_ID");
                messageId = rs.getString("MESSAGE_ID");
                katieBotId = rs.getString("KATIE_BOT_ID");
                msTeamsUserId = rs.getString("MS_TEAMS_USER_ID");
            }
            rs.close();
            stmt.close();
            conn.close();

            MSTeamsConversationValues values = new MSTeamsConversationValues(uuid, teamId, channelId, serviceUrl, conversationId, messageId, katieBotId, msTeamsUserId);
            values.setDomainId(domainId);
            return values;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param roomId Matrix room ID
     */
    public void addDomainIdMatrixMapping(String domainId, String roomId) throws Exception {
        log.info("Add domain Id / Matrix room Id mapping ...");
        String sql = "INSERT INTO " + TABLE_MATRIX_KATIE_DOMAIN + " VALUES ('" + roomId + "' , '" + domainId + "', '" + new Date().getTime() + "')";
        insert(sql);
    }

    /**
     * Remove all Matrix mappings for a particular domain Id
     * @param domainId Domain Id
     */
    public void removeDomainIdMatrixMapping(String domainId) throws Exception {
        log.info("Remove domain Id / Matrix Id mapping ...");

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        String sql = "Delete from " + TABLE_MATRIX_KATIE_DOMAIN + " where KATIE_DOMAIN_ID='" + domainId + "'";
        stmt.executeUpdate(sql);
        stmt.close();

        conn.close();
    }

    /**
     * Connect Katie domain with MS Team Id
     * @param teamId Team Id, e.g. "19:f3bff9f28cf54705ad994cdf1e87ab62@thread.tacv2"
     */
    public void addDomainIdMSTeamsMapping(String domainId, String teamId) throws Exception {
        log.info("Add domain Id / MS team Id mapping ...");
        String sql = "INSERT INTO " + TABLE_MS_TEAM_KATIE_DOMAIN + " VALUES ('" + teamId + "' , '" + domainId + "', '" + new Date().getTime() + "')";
        insert(sql);
    }

    /**
     * Remove MS Teams mapping for a particular domain id and team id and channel id
     * @param  domainId Katie domain Id
     * @param teamId MS Teams Id
     * @param channelId MS Teams channel Id
     */
    public void removeDomainIdMSTeamsMapping(String domainId, String teamId, String channelId) throws Exception {
        String sql = "Delete from " + TABLE_MS_TEAM_KATIE_DOMAIN + " where " + MS_TEAM_ID + "='" + teamId + "' and " + MS_TEAM_DOMAIN_ID + "='" + domainId + "'";

        // TODO: Also consider channelId

        log.info("Remove Katie domain / MS Teams mapping ...");

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
        conn.close();
    }

    /**
     * Insert record
     * @param sql SQL INSERT statement
     */
    private void insert(String sql) throws Exception {
        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
        conn.close();
    }

    /**
     * Add mapping between Katie domain and Discord guild / channel
     * @param domainId Domain Id, e.g. 'wyona'
     * @param guildId Guild Id, e.g. '996391257549053952'
     * @param channelId Channel Id, e.g. '996391611275694131'
     * @param status Status whether connection needs approval or has been approved or was discarded
     * @param token Token to match for approval
     */
    public void addDomainIdDiscordMapping(String domainId, String guildId, String channelId, ConnectStatus status, String token) throws Exception {
        String sql = "INSERT INTO " + TABLE_DISCORD_KATIE_DOMAIN + " VALUES ('" + guildId + "', " + addQuotes(channelId) + ", '" + domainId + "', '" + new Date().getTime() + "', '" + status + "', " + addQuotes(token) + ")";
        insert(sql);
    }

    /**
     * @param domainId Domain Id, e.g. 'wyona'
     * @param teamId Team Id, e.g. 'T01848J69AP'
     * @param channelId Channel Id, e.g. 'C01BG53KWLA'
     * @param status Status whether connection needs approval or has been approved or was discarded
     * @param token Token to match for approval
     */
    public void addDomainIdSlackTeamMapping(String domainId, String teamId, String channelId, ConnectStatus status, String token) throws Exception {
        String sql = "INSERT INTO " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " VALUES ('" + teamId + "' , '" + domainId + "', '" + new Date().getTime() + "', " + addQuotes(channelId) + ", '" + status + "', " + addQuotes(token) + ")";
        insert(sql);
    }

    /**
     * Remove mapping for a particular team id and channel id
     * @param teamId Slack team Id
     * @param channelId Slack channel Id
     */
    public void removeDomainIdSlackTeamChannelMapping(String teamId, String channelId) throws Exception {
        log.info("Remove Katie domain / Slack mapping ...");

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        String sql = "Delete from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where " + SLACK_TEAM_ID + "='" + teamId + "' and " + SLACK_CHANNEL_ID + "='" + channelId + "'";
        stmt.executeUpdate(sql);
        stmt.close();

        conn.close();
    }

    /**
     * Reroute team/channel to another domain
     * @param teamId Slack team Id
     * @param channelId Slack channel Id
     * @param domainId Katie domain Id to which team/channel will be mapped
     */
    public void rerouteDomainIdSlackTeamChannelMapping(String teamId, String channelId, String domainId) throws Exception {
        log.info("Reroute team/channel to another domain ...");

        String sql = "Update " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " set " + KATIE_DOMAIN_ID + "='" + domainId + "' where " + SLACK_TEAM_ID + "='" + teamId + "' and " + SLACK_CHANNEL_ID + "='" + channelId + "'";
        log.info("Try to update domain Id: " + sql);

        updateTableRecord(sql);
    }

    /**
     * Remove all mappings for a particular domain Id
     * @param domainId Domain Id
     */
    public void removeDomainIdSlackTeamMapping(String domainId) throws Exception {
        log.info("Remove domain Id / team Id mapping ...");

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        String sql = "Delete from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where KATIE_DOMAIN_ID='" + domainId + "'";
        stmt.executeUpdate(sql);
        stmt.close();

        conn.close();
    }

    /**
     * @param username TODO
     */
    public void addRememberMeToken(String username, String token, String expiryDate) throws Exception {
        log.info("Add remember me token ...");
        // TODO: Use userId instead username
        String sql = "INSERT INTO REMEMBERME VALUES ('" + username + "' , '" + token + "', '" + expiryDate + "')";
        log.info("Add remember me token to database: " + sql);
        insert(sql);
    }

    /**
     * Get expiry date of remember me token
     * @param token Remember me token
     * @return expiry date if token exists and null otherwise
     */
    public String getExpiryDate(String username, String token) {
        String expiryDate= null;
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select EXPIRY_DATE from REMEMBERME where TOKEN = '" + token + "'";
            log.info("Try to get expiry date from database: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                expiryDate = rs.getString("EXPIRY_DATE");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return expiryDate;
    }

    /**
     * Log question
     *
     * @param question Question asked by user
     * @param messageId Message Id sent by client together with question
     * @param remoteAddress Remote address of user
     * @param dateSubmitted Date question was submittedd
     * @param domain Domain where question was asked
     *
     * @param username Username, whereas is null when user is not signed in
     * @param answerUUID UUID of answer / QnA found by Katie
     * @param answer Actual answer sent back to client / user (only necessary, when answer is not based on a QnA)
     * @param score Score of answer, e.g. 0.7549
     * @param scoreThreshold Score threshold, e.g. 0.73 when configured and null otherwise
     * @param permissionStatus Permission status, e.g. answer is public or user does not have sufficient permissions
     *
     * @param channelType Channel Type
     * @param channelRequestId Channel request Id, e.g. "3139c14f-ae63-4fc4-abe2-adceb67988af"
     *
     * @param offset Results offset, -1 when no offset provided and otherwise >= 0
     *
     * @return uuid of log entry
     */
    public String logQuestion(String question, List<String> classifications, String messageId, String remoteAddress, Date dateSubmitted, Context domain, String username, String answerUUID, String answer, double score, Double scoreThreshold, PermissionStatus permissionStatus, String moderationStatus, ChannelType channelType, String channelRequestId, int offset) throws Exception {
        log.info("Log question (length: " + question.length() + ") ...");

        question = sanitizeQuestion(question, domain.getId());
        String _classifications = sanitizeClassifications(classifications);

        String uuid = java.util.UUID.randomUUID().toString();

        //log.debug("Permission status: " + permissionStatus);
        //log.debug("Message Id of client: " + messageId);

        double _scoreThreshold = -1;
        if (scoreThreshold != null) {
            _scoreThreshold = scoreThreshold;
        }

        insertQuestionAsked(uuid, question, _classifications, messageId, remoteAddress, dateSubmitted, domain.getId(), username, answerUUID, score, _scoreThreshold, permissionStatus, moderationStatus, channelType, channelRequestId, offset);
        saveQuestionAsked(uuid, question, _classifications, messageId, remoteAddress, dateSubmitted, domain, username, answerUUID, answer, score, _scoreThreshold, permissionStatus, moderationStatus, channelType, channelRequestId, offset);

        return uuid;
    }

    /**
     * @return uuid of log entry
     */
    public String logPredictedLabels(Context domain, String text, String clientMessageId, HitLabel[] labels, ClassificationImpl classificationImpl) {
        // TODO: Sanitize text
        String uuid = java.util.UUID.randomUUID().toString();
        savePredictedClassifications(uuid, domain, text, clientMessageId, labels, classificationImpl);
        return uuid;
    }

    /**
     * Save question asked, including answer, etc.
     * @param uuid UUID of question
     * @param question Asked question
     * @param answerUUID UUID of answer / QnA
     * @param answer Actual answer (in particular when answer is not based on a QnA)
     */
    private void saveQuestionAsked(String uuid, String question, String _classifications, String messageId, String remoteAddress, Date dateSubmitted, Context domain, String username, String answerUUID, String answer, double score, Double _scoreThreshold, PermissionStatus permissionStatus, String moderationStatus, ChannelType channelType, String channelRequestId, int offset) {
        if (!domain.getAskedQuestionsDirectory().isDirectory()) {
            domain.getAskedQuestionsDirectory().mkdir();
        }

        // TODO: See AskedQuestion.java
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("uuid", uuid);
        rootNode.put("domainId", domain.getId());
        rootNode.put("question", question);
        rootNode.put("qnaUuid", answerUUID);
        rootNode.put("answer", answer);

        try {
            File askedQuestionFile = getAskedQuestionFile(uuid, domain);
            mapper.writeValue(askedQuestionFile, rootNode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     *
     */
    private void savePredictedClassifications(String uuid, Context domain, String text, String clientMessageId, HitLabel[] labels, ClassificationImpl classificationImpl) {
        if (!domain.getPredictedLabelsDirectory().isDirectory()) {
            domain.getPredictedLabelsDirectory().mkdir();
        }

        // TODO: Use class HumanPreferenceLabel
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("uuid", uuid);
        rootNode.put("domainId", domain.getId());
        rootNode.put(HumanPreferenceMeta.CLIENT_MESSAGE_ID, clientMessageId);
        rootNode.put(HumanPreferenceLabel.TEXT_FIELD, text);
        rootNode.put("classification-implementation", classificationImpl.toString());

        ArrayNode labelsNode = mapper.createArrayNode();
        for (HitLabel label : labels) {
            ObjectNode labelNode = mapper.createObjectNode();
            labelNode.put(HumanPreferenceLabel.LABEL_NAME_FIELD, label.getLabel().getTerm());
            labelNode.put(HumanPreferenceLabel.LABEL_KATIE_ID_FIELD, label.getLabel().getKatieId());
            labelNode.put("score", label.getScore());
            labelsNode.add(labelNode);
        }
        rootNode.put(PREDICTED_LABELS_FIELD, labelsNode);

        try {
            File predictedLabelsFile = getPredictedLabelsLogFile(uuid, domain);
            mapper.writeValue(predictedLabelsFile, rootNode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Get top predicted classification from log file
     * @param uuid Request UUID
     */
    public Classification getTopPredictedClassification(String uuid, Context domain) throws Exception {
        File logFile = getPredictedLabelsLogFile(uuid, domain);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(logFile);
        JsonNode predictedLabels = rootNode.get(PREDICTED_LABELS_FIELD);
        if (predictedLabels.isArray() && predictedLabels.size() > 0) {
            JsonNode topLabel = predictedLabels.get(0);
            String foreignClassId = null; // TODO
            Classification classification = new Classification(topLabel.get(HumanPreferenceLabel.LABEL_NAME_FIELD).asText(), foreignClassId, topLabel.get(HumanPreferenceLabel.LABEL_KATIE_ID_FIELD).asText());
            return classification;
        } else {
            log.error("No predicted labels logged!");
            return null;
        }
    }

    /**
     * Get text for which labels were predicted
     */
    public String getClassifiedText(String uuid, Context domain) throws Exception {
        File logFile = getPredictedLabelsLogFile(uuid, domain);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(logFile);
        return rootNode.get(HumanPreferenceLabel.TEXT_FIELD).asText();
    }

    /**
     * Get client message id from logged request
     */
    public String getClientMessageId(String uuid, Context domain) throws Exception {
        File logFile = getPredictedLabelsLogFile(uuid, domain);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(logFile);
        return rootNode.get(HumanPreferenceMeta.CLIENT_MESSAGE_ID).asText();
    }

    /**
     * Add question asked to database
     */
    private void insertQuestionAsked(String uuid, String question, String _classifications, String messageId, String remoteAddress, Date dateSubmitted, String domainId, String username, String answerUUID, double score, Double _scoreThreshold, PermissionStatus permissionStatus, String moderationStatus, ChannelType channelType, String channelRequestId, int offset) throws Exception {

        String sql = "INSERT INTO " + TABLE_QUESTION + " VALUES ('" + uuid + "' , '" + domainId + "', '" + question + "', '" + remoteAddress + "', '" + dateSubmitted.getTime() + "', " + addQuotes(username) + ", " + addQuotes(answerUUID) + ", " + addQuotes(permissionStatus.toString()) + ", " + addQuotes(moderationStatus) + ", '" + channelType + "', " + addQuotes(channelRequestId)+ ", " + offset + ", " + addQuotes(messageId) + ", " + addQuotes(_classifications) + ", " + score + ", " + _scoreThreshold + ")";
        log.info("Add question to database: " + sql);

/* INFO: org.h2.jdbc.JdbcSQLException: This method is not allowed for a prepared statement; use a regular statement instead. [90130-197]
        String sqlP = "INSERT INTO " + TABLE_QUESTION + " VALUES (?,?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sqlP);
        ps.setString(1, uuid);
        ps.setString(2, contextId);
        ps.setString(3, question);
        ps.setString(4, remoteAddress);
        ps.setString(5, "" + dateSubmitted.getTime());
        ps.executeUpdate(sqlP);
        ps.close();
*/

        insert(sql);
    }

    /**
     * Sanitize question
     * @param submittedQestion Submitted question
     * @param domainId Donain Id for which question was submitted
     * @return sanitized question
     */
    private String sanitizeQuestion(String submittedQestion, String domainId) {
        String question = escapeSingleQuotes(submittedQestion);

        // INFO: Tables RESUBMITTED_QUESTION (varchar(2000)) and QUESTION (varchar(2000)), also see http://www.h2database.com/html/datatypes.html
        int MAX_DB_LENGTH = 2000; // INFO: See V26_1__resize_question_field.sql

        if (question.length() <= MAX_DB_LENGTH) {
            return question;
        } else {
            log.warn("Question is too long (> " + MAX_DB_LENGTH + ") and therefore gets shortened ...");
            question = question.substring(0, MAX_DB_LENGTH);
            log.debug("New length: " + question.length());
            mailerService.notifyAdministrator("WARNING: Submitted question exceeds maximum number of characters", getExceedsMessageBody(MAX_DB_LENGTH, domainId, submittedQestion, question), null, true);
            return question;
        }
    }

    /**
     * @param classifications List of classifications
     */
    private String sanitizeClassifications(List<String> classifications) {
        int MAX_DB_LENGTH = 100; // INFO: See V28_1__alter_question_table.sql

        if (classifications != null && classifications.size() > 0) {
            StringBuilder allClassifications = new StringBuilder();
            for (int i = 1; i < classifications.size(); i++) {
                allClassifications.append(classifications.get(i));
                if (i < classifications.size() - 1) {
                    allClassifications.append(",");
                }
            }

            StringBuilder sb = new StringBuilder();

            if (classifications.get(0).length() <= MAX_DB_LENGTH) {
                sb.append(classifications.get(0));
            } else {
                log.warn("Length of classification '" + classifications.get(0) + "' exceeds max Database field length " + MAX_DB_LENGTH);
                return null;
            }

            for (int i = 1; i < classifications.size(); i++) {
                if (sb.toString().length() + 1 + classifications.get(i).length() <= MAX_DB_LENGTH) {
                    sb.append(CLASSIFICATION_SEPARATOR + classifications.get(i));
                } else {
                    log.warn("Length of complete classifications concatenation '" + allClassifications + "' exceeds max Database field length " + MAX_DB_LENGTH + ", therefore ignore remaining classifications.");
                    break;
                }
            }

            return sb.toString();
        } else {
            return null;
        }
    }

    /**
     *
     */
    private String getExceedsMessageBody(int MAX_DB_LENGTH, String domainId, String submittedQestion, String shortenedQuestion) {
        StringBuilder body = new StringBuilder();
        body.append("<p>The following question exceeds the maximum number of " + MAX_DB_LENGTH + " characters (Domain Id: <a href=\"" + defaultHostname + "/#/asked-questions/" + domainId + "\">" + domainId + "</a>):</p>");
        body.append("<br/><p>" + submittedQestion + "</p>");
        body.append("<br/><br/><p>The submitted question was shortened to:</p>");
        body.append("<br/><p>" + shortenedQuestion + "</p>");

        return body.toString();
    }

    /**
     * Add quotes when provided string is not null
     * @return string with quotes when string is not null and return null when string is null
     */
    private String addQuotes(String s) {
        if (s != null && s.trim().length() > 0) {
            return "'" + s + "'";
        } else {
            return null;
        }
    }

    /**
     * Escape single quote
     * @param s Text containing single quote, e.g. "Michael's biycle"
     * @return escaped text, e.g. "Michael''s biycle"
     */
    private String escapeSingleQuotes(String s) {
        return s.replace("'","''");
    }

    /**
     * Save resubmitted question
     *
     * @param question Resubmitted question
     * @param questionerUserId Katie user Id of person asking question in case user has a Katie account
     * @param questionerLanguage Language of user asking question
     * @param channelType Channel type, e.g. EMAIL, FCM_TOKEN, SLACK, MS_TEAMS, WEBHOOK
     * @param channelRequestUUID Channel request UUID, e.g. af90cc6c-35e0-442a-ad91-05d2f2c32931
     * @param email Email of user submitted question
     * @param fcmToken FCM token associated with mobile device of user
     * @param answerLinkType Answer link type, for example 'deeplink'
     * @param remoteAddress Remote IP address of user which resubmitted question
     * @param domainId Domain Id
     *
     * @return UUID of resubmitted question
     */
    protected String saveResubmittedQuestion(String question, String questionerUserId, Language questionerLanguage, ChannelType channelType, String channelRequestUUID, String email, String fcmToken, String answerLinkType, String remoteAddress, String domainId) throws Exception {
        log.info("Log resubmitted question ...");

        String uuid = java.util.UUID.randomUUID().toString();
        String status = StatusResubmittedQuestion.STATUS_PENDING; // INFO: Initial status
        String answerClientSideEncrypted = null;

        ResubmittedQuestion rq = new ResubmittedQuestion(uuid, question, questionerUserId, questionerLanguage, channelType, channelRequestUUID, email, fcmToken, answerLinkType, status, remoteAddress, new Date(), null, answerClientSideEncrypted, null, null, domainId);
        Ownership ownership = rq.getOwnership();

        String contextIdValue = null;
        if (domainId != null) {
            contextIdValue = "'" + domainId + "'";
        } else {
            contextIdValue = "'" + Context.ROOT_NAME + "'";
        }
        String questionerUserIdValue = null;
        if (questionerUserId != null) {
            questionerUserIdValue = "'" + questionerUserId + "'";
        }
        String questionerLanguageValue = null;
        if (questionerLanguage != null) {
            questionerLanguageValue = "'" + questionerLanguage + "'";
            log.info("Save questioner language " + questionerLanguageValue + "!");
        }
        String emailValue = null;
        if (email != null) {
            emailValue = "'" + email + "'";
        }
        String fcmTokenValue = null;
        if (fcmToken != null) {
            fcmTokenValue = "'" + fcmToken + "'";
        }

        String slackChannelIdValue = null;

        String answerLinkTypeValue = null;
        if (answerLinkType != null) {
            answerLinkTypeValue = "'" + answerLinkType + "'";
        }

        question = sanitizeQuestion(question, domainId);
        String sql = "INSERT INTO " + TABLE_RESUBMITTED_QUESTION + " VALUES ('" + uuid + "' , " + contextIdValue + ", '" + question + "', " + emailValue + ", " + fcmTokenValue + ", " + slackChannelIdValue + ", " + answerLinkTypeValue + " , '" + status + "', '" + remoteAddress + "', '" + new Date().getTime() + "', " + ownership + ", null, " + questionerUserIdValue + ", '" + channelType + "', '" + channelRequestUUID + "', " + questionerLanguageValue + ")";
        log.info("Add resubmitted question to database: " + sql);
        insert(sql);

        return uuid;
    }

    /**
     * Get resubmitted question from database
     * @param uuid UUID of question, e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027'
     * @param inclRespondentData If true, then load data of user which responded to question
     */
    public ResubmittedQuestion getResubmittedQuestion(String uuid, boolean inclRespondentData) throws Exception {
        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        String sql = "Select * from " + TABLE_RESUBMITTED_QUESTION + " where UUID = '" + uuid + "'";
        log.info("Try to get resubmitted question from database: " + sql);
        ResubmittedQuestion resubmittedQuestion = null;
        ResultSet rs = stmt.executeQuery(sql);
        int counter = 0;
        while (rs.next()) {
            counter++;
            if (counter > 1) {
                log.warn("There are more than on resubmitted questions with UUID '" + uuid + "'!");
                break;
            }
            String question = rs.getString(QUESTION_QUESTION);
            String questionerUserId = rs.getString(QUESTIONER_USER_ID);

            String questionerLang = rs.getString(QUESTIONER_LANGUAGE);
            Language questionerLanguage = null;
            if (questionerLang != null) {
                questionerLanguage = Language.valueOf(questionerLang);
            }

            ChannelType channelType = ChannelType.valueOf(rs.getString(CHANNEL_TYPE));

            String channelRequestId = rs.getString(CHANNEL_REQUEST_ID);
            String email = rs.getString(EMAIL);
            if (email != null) channelType = ChannelType.EMAIL; // INFO: Because of backwards compatibility

            String fcmToken = rs.getString(FCM_TOKEN);
            if (fcmToken != null) channelType = ChannelType.FCM_TOKEN; // INFO: Because of backwards compatibility

            String answerLinkType = rs.getString(ANSWER_LINK_TYPE);
            String questionStatus = rs.getString(STATUS);
            String remoteAddress = rs.getString("REMOTE_ADDRESS");

            log.warn("Answer does not exist as table field, but stored as part of XML, therefore answer will be set to null.");
            //xmlService.parseQuestionAnswer();
            String answer = null;
            String answerClientSideEncryptedAlgorithm = null; // INFO: Does not exist as table field, but stored as part of XML

            String contextId = rs.getString(QUESTION_DOMAIN_ID);

            Ownership ownership = parseOwnership(rs.getString(OWNERSHIP));

            String respondentId = rs.getString(RESPONDENT);
            log.debug("Question '" + question + "' and email '" + email + "'.");
            long epochTimestampResubmitted = new Long(rs.getString("TIMESTAMP_RESUBMITTED")).longValue();
            resubmittedQuestion = new ResubmittedQuestion(uuid, question, questionerUserId, questionerLanguage, channelType, channelRequestId, email, fcmToken, answerLinkType, questionStatus, remoteAddress, new Date(epochTimestampResubmitted), answer, answerClientSideEncryptedAlgorithm, ownership, respondentId, contextId);

            if (resubmittedQuestion.getRespondentUserId() != null && inclRespondentData) {
                log.info("Resolve respondent with id '" + resubmittedQuestion.getRespondentUserId() + "' ...");
                User respondent = usersXMLFileService.getIAMUserById(resubmittedQuestion.getRespondentUserId(), false, false);
                resubmittedQuestion.setRespondent(respondent);
            }
        }
        rs.close();
        stmt.close();
        conn.close();

        return resubmittedQuestion;
    }

    /**
     *
     */
    private Ownership parseOwnership(String sOwnership) {
        Ownership ownership = null;
        if (sOwnership != null) {
            if (sOwnership.equals("iam:public")) {
                log.info("Migrate ownership value 'iam:public' ...");
                ownership = ownership.iam_public;
            } else if (sOwnership.equals("iam:context")) {
                log.info("Migrate ownership value 'iam:context' ...");
                ownership = ownership.iam_context;
            } else if (sOwnership.equals("iam:source")) {
                log.info("Migrate ownership value 'iam:source' ...");
                ownership = ownership.iam_source;
            } else {
                ownership = Ownership.valueOf(sOwnership);
            }
        }
        return ownership;
    }

    /**
     * Delete resubmitted question from database
     * @param uuid UUID of question, e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027'
     */
    public void deleteResubmittedQuestion(String uuid) throws Exception {
        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        String sql = "Delete from " + TABLE_RESUBMITTED_QUESTION + " where UUID = '" + uuid + "'";
        log.info("Try to delete resubmitted question from database: " + sql);
        ResubmittedQuestion resubmittedQuestion = null;
        stmt.executeUpdate(sql);
        stmt.close();
        conn.close();
    }

    /**
     * Update question of resubmitted question
     * @param question Question text
     * @param respondent User updating question
     */
    public void updateQuestionOfResubmittedQuestion(String uuid, String question, User respondent) throws Exception {
        StringBuilder sql = new StringBuilder("Update " + TABLE_RESUBMITTED_QUESTION + " set " + RESPONDENT + "='" + respondent.getId() + "', " + QUESTION_QUESTION + "='" + question + "'");
        sql.append(" where UUID = '" + uuid + "'");

        log.info("Try to update resubmitted question inside database: " + sql);

        updateTableRecord(sql.toString());
    }

    /**
     * Update resubmitted question as answered inside database
     * @param ownership Either user, domain or public
     * @param respondent User answering question
     */
    public void updateResubmittedQuestionAsAnswered(String uuid, Ownership ownership, User respondent) throws Exception {
        StringBuilder sql = new StringBuilder("Update " + TABLE_RESUBMITTED_QUESTION + " set " + RESPONDENT + "='" + respondent.getId() + "'");
        if (ownership != null) {
            sql.append(", " + OWNERSHIP + "='" + ownership + "'");
        }
        sql.append(", " + STATUS + "='" + StatusResubmittedQuestion.STATUS_ANSWERED + "' where UUID = '" + uuid + "'");

        log.info("Try to update resubmitted question inside database: " + sql);

        updateTableRecord(sql.toString());
    }

    /**
     * Update status of resubmitted question inside database
     * @param uuid UUID of resubmitted question
     */
    public void updateStatusOfResubmittedQuestion(String uuid, String status) throws Exception {
        String sql = "Update " + TABLE_RESUBMITTED_QUESTION + " set " + STATUS + "='" + status + "'  where UUID = '" + uuid + "'";
        log.info("Try to update status of resubmitted question inside database: " + sql);
        updateTableRecord(sql);
    }

    /**
     * Add user information
     * @param email Email address of user
     * @param domainId Domain Id for which user asked / submitted a question
     */
    /*
    public void addUserInfo(String email, String domainId) throws Exception {
        log.info("Save email '" + email + "' ...");
        StringBuilder sql = new StringBuilder("INSERT INTO " + "USER" + " VALUES ('" + email + "' , '" + domainId + "', '" + new Date().getTime() + "')");

        updateTableRecord(sql.toString());
    }

     */

    /**
     * Get all user information for a particular domain
     */
    public String[] getUserInfo(String domainId) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_ANALYTICS + " WHERE DOMAIN_ID='" + domainId + "' AND EMAIL is not null");

        List<String> emails = new ArrayList<String>();

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql.toString());
        while (rs.next()) {
            emails.add(rs.getString("EMAIL"));
        }
        rs.close();
        stmt.close();
        conn.close();

        return emails.toArray(new String[0]);
    }

    /**
     * Get resubmitted questions from database
     * @param status Status of question, e.g. 'answer-pending'
     * @param contextId Context Id of question, e.g. 'wyona'
     * @param limit Limit number of returned entries
     * @param offset From where to start returning entries
     */
    public ResubmittedQuestion[] getResubmittedQuestions(String status, String contextId, int limit, int offset) throws Exception {
        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        StringBuilder sql = new StringBuilder("Select * from " + TABLE_RESUBMITTED_QUESTION);

        if (status != null || contextId != null) {
            sql.append(" where ");

            if (status != null) {
                sql.append(STATUS + " = '" + status + "'");
            }
            if (contextId != null && status != null) {
                sql.append(" and " + QUESTION_DOMAIN_ID + " = '" + contextId + "'");
            }
            if (contextId != null && status == null) {
                sql.append(QUESTION_DOMAIN_ID + " = '" + contextId + "'");
            }
        }

        sql.append(" order by TIMESTAMP_RESUBMITTED desc");

        if (0 < limit && limit <= MAX_LIMIT) {
            sql.append(" limit " + limit);
        } else {
            log.warn("Max limit '" + MAX_LIMIT + "' used, because provided limit '" + limit + "' out of range.");
            sql.append(" limit " + MAX_LIMIT);
        }
        if (offset > 0) {
            sql.append(" offset " + offset);
        } else {
            sql.append(" offset 0");
        }
        
        log.info("Try to get resubmitted questions from database: " + sql);
        ResultSet rs = stmt.executeQuery(sql.toString());
        List<ResubmittedQuestion> questions = new ArrayList<ResubmittedQuestion>();
        while (rs.next()) {
            String uuid = rs.getString("UUID");
            String question = rs.getString("QUESTION");
            String questionerUserId = rs.getString(QUESTIONER_USER_ID);

            String questionerLang = rs.getString(QUESTIONER_LANGUAGE);
            Language questionerLanguage = null;
            if (questionerLang != null) {
                questionerLanguage = Language.valueOf(questionerLang);
            }

            ChannelType channelType = ChannelType.valueOf(rs.getString(CHANNEL_TYPE));
            String channelRequestId = rs.getString(CHANNEL_REQUEST_ID);

            String email = rs.getString(EMAIL);
            if (email != null) channelType = ChannelType.EMAIL; // INFO: Because of backwards compatibility

            String fcmToken = rs.getString(FCM_TOKEN);
            if (fcmToken != null) channelType = ChannelType.FCM_TOKEN; // INFO: Because of backwards compatibility

            String answerLinkType = rs.getString(ANSWER_LINK_TYPE);
            String questionStatus = rs.getString(STATUS);
            String remoteAddress = rs.getString("REMOTE_ADDRESS");
            String answer = null;
            String answerClientSideEncryptedAlgorithm = null;
            String questionContextId = rs.getString(QUESTION_DOMAIN_ID);

            Ownership ownership = parseOwnership(rs.getString(OWNERSHIP));

            String respondent = rs.getString(RESPONDENT);
            log.info("Question '" + question + "' and email '" + email + "'.");
            long epochTimestampResubmitted = new Long(rs.getString("TIMESTAMP_RESUBMITTED")).longValue();
            questions.add(new ResubmittedQuestion(uuid, question, questionerUserId, questionerLanguage, channelType, channelRequestId, email, fcmToken, answerLinkType, questionStatus, remoteAddress, new Date(epochTimestampResubmitted), answer, answerClientSideEncryptedAlgorithm, ownership, respondent, questionContextId));
        }
        rs.close();
        stmt.close();
        conn.close();

        return questions.toArray(new ResubmittedQuestion[0]);
    }

    /**
     * Get number of questions which have been submitted during the last 24 hours for a particular domain
     * @param domainId Domain Id
     * @param channelType Channel type, e.g. "SLACK" or "DISCORD"
     * @return number of questions which have been submitted during the last 24 hours
     */
    public int getQuestionCountDuringLast24Hours(String domainId, ChannelType channelType) throws Exception {
        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();

        Long current = new Date().getTime();
        Date twentyFourHoursAgo = new Date(current - (24 * 60 * 60 * 1000));
        //log.debug("Epoch time 24 hours ago: " + twentyFourHoursAgo.getTime());

        String sql = "Select " + TIMESTAMP + " from " + TABLE_QUESTION + " where " + QUESTION_DOMAIN_ID + " = '" + domainId + "' and " + QUESTION_CHANNEL_TYPE + " = '" + channelType + "' and " + TIMESTAMP + " > " + twentyFourHoursAgo.getTime();
        log.info("Try to get questions from database: " + sql);

        ResultSet rs = stmt.executeQuery(sql);
        List<Date> timestamps = new ArrayList<Date>();
        while (rs.next()) {
            long epochTimestamp = new Long(rs.getString(TIMESTAMP)).longValue();
            //log.debug("Timestamp of question: " + epochTimestamp);
            timestamps.add(new Date(epochTimestamp));
        }
        rs.close();
        stmt.close();
        conn.close();

        return timestamps.size();
    }

    /**
     * Update modetation status re suggested answer to asked question
     * @param qid Question Id, c
     * @param moderationStatus Moderation status, e.g. "APPROVED"
     */
    public void updateModerationStatus(String qid, String moderationStatus) throws Exception {
        log.info("Update moderation status of question '" + qid + "' ...");

        String sql = "Update " + TABLE_QUESTION + " set " + QUESTION_MODERATION_STATUS + "='" + moderationStatus + "'  where UUID = '" + qid + "'";
        updateTableRecord(sql);
    }

    /**
     * Update UUID of suggested answer to asked question
     * @param qid Question Id, e.g. "ded394f8-4a63-42a7-9180-20d8f4875662"
     * @param answerUUID UUID of answer / QnA
     */
    public void updateSuggestedAnswerUUID(String qid, String answerUUID) throws Exception {
        log.info("Update answer UUID of question '" + qid + "' ...");

        String sql = "Update " + TABLE_QUESTION + " set " + QUESTION_ANSWER_UUID + "='" + answerUUID + "'  where UUID = '" + qid + "'";
        updateTableRecord(sql);
    }

    /**
     * Update table record
     */
    private void updateTableRecord(String sql) throws Exception {
        log.info("Update table record: " + sql);

        Class.forName(driverClassName);

        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
        conn.close();
    }

    /**
     * Get particular asked question by UUID from database
     * @param qid Question Id, e.g. "ded394f8-4a63-42a7-9180-20d8f4875662"
     */
    protected AskedQuestion getAskedQuestionByUUID(String qid) throws Exception {
        StringBuilder sql = new StringBuilder("Select * from " + TABLE_QUESTION + " where UUID='" + qid + "'");
        log.info("Try to get question from database: " + sql);

        AskedQuestion askedQuestion = getAskedQuestionFromDB(sql.toString());

        if (askedQuestion == null) {
            String msg = "No question with UUID '" + qid + "'!";
            log.error(msg);
            throw new Exception(msg);
        }

        return askedQuestion;
    }

    /**
     * Get a particular asked question from database by channel request Id
     * @param id Channel request Id
     */
    public AskedQuestion getQuestionByChannelRequestId(String id) throws Exception {
        StringBuilder sql = new StringBuilder("Select * from " + TABLE_QUESTION + " where " + QUESTION_CHANNEL_REQUEST_ID + "='" + id + "'");
        log.info("Try to get question from database: " + sql);

        AskedQuestion askedQuestion = getAskedQuestionFromDB(sql.toString());

        if (askedQuestion == null) {
            throw new Exception("No question with Channel Request Id '" + id + "'!");
        }

        return askedQuestion;
    }

    /**
     * Get a particular asked question from database by message Id
     * @param id Message Id
     */
    public AskedQuestion getQuestionByMessageId(String id) throws Exception {
        StringBuilder sql = new StringBuilder("Select * from " + TABLE_QUESTION + " where " + QUESTION_CLIENT_MESSAGE_ID + "='" + id + "'");
        log.info("Try to get question from database: " + sql);

        AskedQuestion askedQuestion = getAskedQuestionFromDB(sql.toString());

        if (askedQuestion == null) {
            throw new Exception("No question with Client Message Id '" + id + "'!");
        }

        return askedQuestion;
    }

    /**
     * @param uuid UUID of asked question
     * @param domain Domain where question was asked
     */
    protected AskedQuestion getAskedQuestionFromFS(String uuid, Context domain) {
        File aqFile = getAskedQuestionFile(uuid, domain);
        ObjectMapper mapper = new ObjectMapper();
        try {
            AskedQuestion aq = mapper.readValue(aqFile, AskedQuestion.class);
            return aq;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     *
     */
    protected File getAskedQuestionFile(String uuid, Context domain) {
        return new File(domain.getAskedQuestionsDirectory(), uuid + ".json");
    }

    /**
     * Log file containing text and predicted labels
     */
    protected File getPredictedLabelsLogFile(String uuid, Context domain) {
        return new File(domain.getPredictedLabelsDirectory(), uuid + ".json");
    }

    /**
     *
     */
    protected File getRatingFile(String uuid, Context domain) {
        return new File(domain.getRatingsDirectory(), uuid + ".json");
    }

    /**
     * Get file containing rating of predicted classification
     */
    protected File getRatingOfPredictedClassificationsFile(String uuid, Context domain) {
        return new File(domain.getRatingsOfPredictedLabelsDirectory(), uuid + ".json");
    }

    /**
     * Get a specific asked question
     */
    private AskedQuestion getAskedQuestionFromDB(String sql) throws Exception {
        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery(sql);
        AskedQuestion askedQuestion = null;
        while (rs.next()) {
            String uuid = rs.getString("UUID");
            String qDomainId = rs.getString(QUESTION_DOMAIN_ID);
            String question = rs.getString("QUESTION");
            String remoteAddress = rs.getString("REMOTE_ADDRESS");
            log.info("Question '" + question + "'.");
            long epochTimestamp = Long.parseLong(rs.getString(TIMESTAMP));
            String username = rs.getString(QUESTION_USER_NAME);
            String qnaUUID = rs.getString(QUESTION_ANSWER_UUID);
            double score = rs.getDouble(QUESTION_ANSWER_SCORE);
            Double scoreThreshold = rs.getDouble(QUESTION_ANSWER_SCORE_THRESHOLD);
            String permissionStatus = rs.getString("PERMISSION_STATUS");
            String moderationStatus = rs.getString(QUESTION_MODERATION_STATUS);

            ChannelType channelType = ChannelType.valueOf(rs.getString(QUESTION_CHANNEL_TYPE));
            String channelRequestId = rs.getString(QUESTION_CHANNEL_REQUEST_ID);
            String clientMessageId = rs.getString(QUESTION_CLIENT_MESSAGE_ID);

            // INFO: Re Backwards compatibility: When offset was not recorded yet, then the DB value is null and getInt() returns 0
            int offset_results = rs.getInt(QUESTION_OFFSET_RESULTS);

            List<String> classifications = getClassifications(rs.getString(QUESTION_CLASSIFICATIONS));

            askedQuestion = new AskedQuestion(uuid, qDomainId, question, classifications, remoteAddress, new Date(epochTimestamp), username, qnaUUID, score, scoreThreshold, permissionStatus, moderationStatus, channelType, channelRequestId, offset_results);
            if (clientMessageId != null) {
                askedQuestion.setClientMessageId(clientMessageId);
            }
        }
        rs.close();
        stmt.close();
        conn.close();

        return askedQuestion;
    }

    /**
     * @param classficationsCSV Classifications as comma separated values
     */
    private List<String> getClassifications(String classficationsCSV) {
        List<String> classifications = new ArrayList<String>();
        if (classficationsCSV != null) {
            String[] classificationValues = classficationsCSV.split(CLASSIFICATION_SEPARATOR);
            for (String value : classificationValues) {
                classifications.add(value);
            }
        }
        return classifications;
    }

    /**
     * Log analytics event
     * @param channelType Channel type, e.g. EMAIL, SLACK, ...
     * @param channelId Channel Id, e.g. Slack or MS Teams channel Id
     * @param agent TODO
     * @param email Email of user
     */
    public String logAnalyticsEvent(String domainId, String evenType, String language, ChannelType channelType, String channelId, String agent, String remoteAddress, String email) throws Exception {
        Date timestamp = new Date();
        log.info("Log analytics event '" + evenType + "' for domain '" + domainId + "' ...");

        String uuid = java.util.UUID.randomUUID().toString();

        StringBuilder sql = new StringBuilder("INSERT INTO " + TABLE_ANALYTICS + " VALUES ('" + uuid + "' , '" + domainId + "', '" + evenType + "', '" + new Date().getTime() + "'");
        if (language != null) {
            sql.append(", '" + language + "'");
        } else {
            sql.append(", null");
        }
        sql.append(", '" + agent + "', '" + remoteAddress + "'");
        if (channelType != null) {
            sql.append(", '" + channelType + "'");
        } else {
            sql.append(", null");
        }
        if (channelId != null) {
            sql.append(", '" + channelId + "'");
        } else {
            sql.append(", null");
        }
        if (email != null && !email.isEmpty()) {
            sql.append(", '" + email + "'");
        } else {
            sql.append(", null");
        }
        sql.append(")");

        log.info("Add analytics event to database: " + sql);
        insert(sql.toString());

        return uuid;
    }

    /**
     * Get analytics events
     * @param eventType Event type, e.g. "get_faq" or "msg_rec" or "q_sent2expert"
     */
    public Event[] getAnalysticsEvents(String eventType, String domainId, Date start, Date end) throws Exception {
        java.util.List<Event> events = new ArrayList<Event>();

        StringBuilder sql = new StringBuilder("SELECT * FROM " + TABLE_ANALYTICS + " WHERE EVENT_TYPE='" + eventType + "' AND DOMAIN_ID='" + domainId + "'");
        sql.append(" and " + TIMESTAMP + " < " + end.getTime());
        if (start.getTime() > 0) {
            sql.append(" and " + TIMESTAMP + " > " + start.getTime());
        }

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql.toString());
        while (rs.next()) {
            long epochTimestamp = Long.parseLong(rs.getString(TIMESTAMP));
            events.add(new Event(eventType, new Date(epochTimestamp)));
        }
        stmt.close();
        conn.close();

        return events.toArray(new Event[0]);
    }

    /**
     * Get total number of events
     * @param eventType Event type, e.g. "get_faq" or "msg_rec" or "q_sent2expert"
     */
    public int getNumberOfEvents(String domainId, String eventType, String language, Date start, Date end) throws Exception {

        StringBuilder sql = new StringBuilder("Select count(*) as " + ROW_COUNT + " from " + TABLE_ANALYTICS + " where " + "DOMAIN_ID" + " = '" + domainId + "' and EVENT_TYPE = '" + eventType + "'");
        if (language != null) {
            sql.append(" and LANGUAGE = '" + language + "'");
        }
        sql.append(" and " + TIMESTAMP + " < " + end.getTime());
        if (start.getTime() > 0) {
            sql.append(" and " + TIMESTAMP + " > " + start.getTime());
        }

        log.info("Try to count number of events: " + sql);
        int numberOfEvents = getRowCount(ROW_COUNT, sql.toString());

        return numberOfEvents;
    }

    /**
     * Total number of asked questions in the specified time period
     */
    public int getNumberOfQuestions(String domainId, Date start, Date end) throws Exception {
        StringBuilder sql = new StringBuilder("Select count(*) as " + ROW_COUNT + " from " + TABLE_QUESTION + " where " + QUESTION_DOMAIN_ID + " = '" + domainId + "' and (" + QUESTION_OFFSET_RESULTS + " = " + DefaultValues.NO_OFFSET_PROVIDED + " or " + QUESTION_OFFSET_RESULTS + " = 0 or " + QUESTION_OFFSET_RESULTS + " is null)");
        sql.append(" and " + TIMESTAMP + " < " + end.getTime());
        if (start.getTime() > 0) {
            sql.append(" and " + TIMESTAMP + " > " + start.getTime());
        }

        log.debug("Try to count number of questions: " + sql);
        int numberOfQuestions = getRowCount(ROW_COUNT, sql.toString());

        return numberOfQuestions;
    }

    /**
     * Total number of next best answer in the specified time period
     */
    public int getNumberOfNextBestAnswer(String domainId, Date start, Date end) throws Exception {
        // INFO: If OFFSET_RESULTS > 0, then it means that user clicked on next best answer
        StringBuilder sql = new StringBuilder("Select count(*) as " + ROW_COUNT + " from " + TABLE_QUESTION + " where " + QUESTION_DOMAIN_ID + " = '" + domainId + "' and " + QUESTION_OFFSET_RESULTS + " > 0");
        sql.append(" and " + TIMESTAMP + " < " + end.getTime());
        if (start.getTime() > 0) {
            sql.append(" and " + TIMESTAMP + " > " + start.getTime());
        }

        log.info("Try to count number of next best answer: " + sql);
        int numberOfQuestions = getRowCount(ROW_COUNT, sql.toString());

        return numberOfQuestions;
    }

    /**
     * Total number of answered questions in the specified time period
     */
    public int getNumberOfAnsweredQuestions(String domainId, Date start, Date end) throws Exception {
        StringBuilder sql = new StringBuilder("Select count(*) as " + ROW_COUNT + " from " + TABLE_QUESTION + " where " + QUESTION_DOMAIN_ID + " = '" + domainId + "' and (" + QUESTION_OFFSET_RESULTS + " = " + DefaultValues.NO_OFFSET_PROVIDED + " or " + QUESTION_OFFSET_RESULTS + " = 0 or " + QUESTION_OFFSET_RESULTS + " is null) and " + QUESTION_ANSWER_UUID + " is not null");
        sql.append(" and " + TIMESTAMP + " < " + end.getTime());
        if (start.getTime() > 0) {
            sql.append(" and " + TIMESTAMP + " > " + start.getTime());
        }

        log.debug("Try to count number of answered questions: " + sql);
        int numberOfAnsweredQuestions = getRowCount(ROW_COUNT, sql.toString());

        return numberOfAnsweredQuestions;
    }

    /**
     * @param rowCountParamName Row count parameter name, e.g. "rowcount"
     * @param sqlQuery SQL query, e.g. "Select count(*) as rowcount from TABLE where ..."
     */
    private int getRowCount(String rowCountParamName, String sqlQuery) throws Exception {
        int rowCount = 0;

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();

        log.debug("Try to get row count: " + sqlQuery);

        ResultSet rs = stmt.executeQuery(sqlQuery.toString());
        rs.next();
        rowCount = rs.getInt(rowCountParamName);
        rs.close();
        stmt.close();
        conn.close();

        return rowCount;
    }

    /**
     * Get all questions asked from database for a specific domain
     * @param domainId Domain Id, e.g. "wyona"
     * @param limit Limit number of returned entries
     * @param offset From where to start returning entries
     * @param unanswered When set to true, then only return unanswered questions
     * @return all asked questions for a specific domain
     */
    public AskedQuestion[] getQuestions(String domainId, int limit, int offset, Boolean unanswered) throws Exception {
        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        StringBuilder sql = new StringBuilder("Select * from " + TABLE_QUESTION);
        if (domainId != null) {
            sql.append(" where " + QUESTION_DOMAIN_ID + " = '" + domainId + "'");
        }
        if (unanswered != null && unanswered.booleanValue()) {
            log.info("Only return unanswered questions ...");
            sql.append(" and " + QUESTION_ANSWER_UUID + " is null");
        }
        sql.append(" order by TIMESTAMP desc");
        if (0 < limit && limit <= MAX_LIMIT) {
            sql.append(" limit " + limit);
        } else {
            log.warn("Max limit '" + MAX_LIMIT + "' used, because provided limit '" + limit + "' out of range.");
            sql.append(" limit " + MAX_LIMIT);
        }
        if (offset > 0) {
            sql.append(" offset " + offset);
        } else {
            sql.append(" offset 0");
        }
        log.info("Try to get questions from database: " + sql);

        ResultSet rs = stmt.executeQuery(sql.toString());
        List<AskedQuestion> questions = new ArrayList<AskedQuestion>();
        while (rs.next()) {
            String uuid = rs.getString("UUID");
            String qDomainId = rs.getString(QUESTION_DOMAIN_ID);
            String question = rs.getString("QUESTION");
            String remoteAddress = rs.getString("REMOTE_ADDRESS");
            log.debug("Question '" + question + "'.");
            long epochTimestamp = Long.parseLong(rs.getString(TIMESTAMP));
            String username = rs.getString(QUESTION_USER_NAME);
            String qnaUUID = rs.getString(QUESTION_ANSWER_UUID);
            double score = rs.getDouble(QUESTION_ANSWER_SCORE);
            Double scoreThreshold = rs.getDouble(QUESTION_ANSWER_SCORE_THRESHOLD);
            String permissionStatus = rs.getString("PERMISSION_STATUS");
            String moderationStatus = rs.getString(QUESTION_MODERATION_STATUS);

            ChannelType channelType = ChannelType.valueOf(rs.getString(QUESTION_CHANNEL_TYPE));
            String channelRequestId = rs.getString(QUESTION_CHANNEL_REQUEST_ID);

            // INFO: Re Backwards compatibility: When offset was not recorded yet, then the DB value is null and getInt() returns 0
            int offset_results = rs.getInt(QUESTION_OFFSET_RESULTS);

            List<String> classifications = getClassifications(rs.getString(QUESTION_CLASSIFICATIONS));

            AskedQuestion askedQuestion = new AskedQuestion(uuid, qDomainId, question, classifications, remoteAddress, new Date(epochTimestamp), username, qnaUUID, score, scoreThreshold, permissionStatus, moderationStatus, channelType, channelRequestId, offset_results);

            String messageId = rs.getString(QUESTION_CLIENT_MESSAGE_ID);
            askedQuestion.setClientMessageId(messageId);

            if (channelType.equals(ChannelType.DISCORD)) {
                DiscordEvent discordEvent = getDiscordConversationValuesForChannelRequestId(channelRequestId);
                askedQuestion.setDiscordGuildId(discordEvent.getGuildId());
                askedQuestion.setDiscordChannelId(discordEvent.getChannelId());
            }
            if (channelType.equals(ChannelType.SLACK)) {
                SlackEvent slackEvent = getSlackConversationValues(channelRequestId);
                askedQuestion.setSlackTeamId(slackEvent.getTeam());
                askedQuestion.setSlackChannelId(slackEvent.getChannel());
            }
            if (channelType.equals(ChannelType.MS_TEAMS)) {
                log.info("Get and set MS Teams conversation values ...");
                MSTeamsConversationValues msTeamsConversationValues = getMSTeamsConversationValues(channelRequestId);
                askedQuestion.setMsTeamsId(msTeamsConversationValues.getTeamId());
                askedQuestion.setMsTeamsChannelId(msTeamsConversationValues.getChannelId());
            }

            questions.add(askedQuestion);
        }
        rs.close();
        stmt.close();
        conn.close();

        return questions.toArray(new AskedQuestion[0]);
    }

    /**
     * Delete all questions asked by a particular user within a particular domain
     */
    public void deleteQuestionsAsked(String userId, String domainId) throws Exception  {
        log.info("Delete all questions asked by user '" + userId + "' within domain '" + domainId + "' ...");

        String sql = "Delete from " + TABLE_QUESTION + " where " + QUESTION_USER_NAME + "='" + userId + "' and " + QUESTION_DOMAIN_ID + "='" + domainId + "'";

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();

        conn.close();
    }

    /**
     * Get user Id of Katie, when somebody addresses Katie directly, e.g. "@katie how is the weather today?" and therefore will be at the beginning of a message, e.g. "<@U018UN3S54G> how is the weather today?"
     * @param teamId Team Id, e.g. 'T01848J69AP'
     * @return user Id of Katie, e.g. "U018UN3S54G"
     */
    public String getKatieUserId(String teamId) {
        String userId = null;

        String sql = "Select USER_ID from " + TABLE_SLACK_TEAM_TOKEN_USERID + " where " + SLACK_TEAM_ID + " = '" + teamId + "'";
        log.info("Try to get Slack Katie user Id from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                userId = rs.getString("USER_ID");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return userId;
    }

    /**
     * Get domain linked with a particular Discord guild / channel
     * @param guildId Team Id, e.g. '996391257549053952'
     * @param channelId Channel Id, e.g. '996391611275694131'
     * @return domain Id, e.g. 'wyona'
     */
    public String getDomainIdForDiscordGuildChannel(String guildId, String channelId) {
        String domainId = null;

        String sql = "Select " + KATIE_DOMAIN_ID + " from " + TABLE_DISCORD_KATIE_DOMAIN + " where " + DISCORD_GUILD_ID + " = '" + guildId + "' and " + DISCORD_CHANNEL_ID + "='" + channelId + "'";
        log.info("Try to get domain Id from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainId = rs.getString(KATIE_DOMAIN_ID);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (domainId == null) {
            log.warn("No domain linked with Discord guild Id '" + guildId + "' and channel Id '" + channelId + "'!");
        }

        return domainId;
    }

    /**
     * Get domain Ids linked with a particular Discord guild
     * @param guildId Team Id, e.g. '996391257549053952'
     * @return domain Ids, e.g. 'wyona' and '3139c14f-ae63-4fc4-abe2-adceb67988af'
     */
    public String[] getDomainIdForDiscordGuild(String guildId) {
        String sql = "Select " + KATIE_DOMAIN_ID + " from " + TABLE_DISCORD_KATIE_DOMAIN + " where " + DISCORD_GUILD_ID + " = '" + guildId + "'";
        log.info("Try to get domain Id from database: " + sql);

        List<String> domainIds = new ArrayList<String>();
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainIds.add(rs.getString(KATIE_DOMAIN_ID));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (domainIds.size() == 0) {
            log.warn("No domain linked with Discord guild Id '" + guildId + "'!");
        }

        return domainIds.toArray(new String[0]);
    }

    /**
     * Get Discord guild/channel linked with a particular Katie domain
     * @param domainId Katie domain Id
     * @return array of Discord guilds/channels
     */
    public DiscordDomainMapping[] getDiscordDomainMappingForDomain(String domainId) {
        List<DiscordDomainMapping> mappings = new ArrayList<DiscordDomainMapping>();

        String sql = "Select * from " + TABLE_DISCORD_KATIE_DOMAIN + " where " + KATIE_DOMAIN_ID + " = '" + domainId + "'";
        log.info("Try to get Discord domain mappings from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String guildId = rs.getString(DISCORD_GUILD_ID);
                String channelId = rs.getString(DISCORD_CHANNEL_ID);
                //String domainId = rs.getString(DISCORD_DOMAIN_ID);
                log.info("Discord channel Id: " + channelId);

                DiscordDomainMapping mapping = new DiscordDomainMapping();
                mapping.setGuildId(guildId);
                mapping.setChannelId(channelId);
                mapping.setDomainId(domainId);

                mappings.add(mapping);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (mappings.size() == 0) {
            log.warn("Katie domain '" + domainId + "' not linked with Discord guild/channel!");
        }

        return mappings.toArray(new DiscordDomainMapping[0]);
    }

    /**
     * Get all domains connected with Discord channels
     * @return array of Discord guilds/channels
     */
    public DiscordDomainMapping[] getAllDomainsConnectedWithDiscordChannels() {
        List<DiscordDomainMapping> mappings = new ArrayList<DiscordDomainMapping>();

        String sql = "Select * from " + TABLE_DISCORD_KATIE_DOMAIN;
        log.info("Try to get Discord domain mappings from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String guildId = rs.getString(DISCORD_GUILD_ID);
                String channelId = rs.getString(DISCORD_CHANNEL_ID);
                String domainId = rs.getString(DISCORD_DOMAIN_ID);
                log.info("Discord channel Id: " + channelId);

                DiscordDomainMapping mapping = new DiscordDomainMapping();
                mapping.setGuildId(guildId);
                mapping.setChannelId(channelId);
                mapping.setDomainId(domainId);

                mappings.add(mapping);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (mappings.size() == 0) {
            log.warn("No Katie domain linked with Discord guild/channel!");
        }

        return mappings.toArray(new DiscordDomainMapping[0]);
    }

    /**
     * Remove mapping for a particular Discord guild id and channel id
     * @param guildId Discord guild Id
     * @param channelId Discord channel Id
     */
    public void removeDomainIdDiscordGuildChannelMapping(String guildId, String channelId) throws Exception {
        log.info("Remove Katie domain / Discord mapping ...");

        Class.forName(driverClassName);
        Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

        Statement stmt = conn.createStatement();
        String sql = "Delete from " + TABLE_DISCORD_KATIE_DOMAIN + " where " + DISCORD_GUILD_ID + "='" + guildId + "' and " + DISCORD_CHANNEL_ID + "='" + channelId + "'";
        stmt.executeUpdate(sql);
        stmt.close();

        conn.close();
    }

    /**
     * Get domain linked with a particular Slack team / channel
     * @param teamId Team Id, e.g. 'T01848J69AP'
     * @param channelId Channel Id, e.g. 'C01BG53KWLA'
     * @return domain Id, e.g. 'wyona'
     */
    public String getDomainIdForSlackTeamChannel(String teamId, String channelId) {
        // TODO: Use getDomainMappingForSlackTeamChannel(teamId, channelId) instead

        String domainId = null;

        String sql = "Select " + KATIE_DOMAIN_ID + " from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where " + SLACK_TEAM_ID + " = '" + teamId + "' and " + SLACK_CHANNEL_ID + "='" + channelId + "'";
        log.info("Try to get domain Id from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainId = rs.getString(KATIE_DOMAIN_ID);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (domainId == null) {
            log.warn("No domain linked with Slack team Id '" + teamId + "' and channel Id '" + channelId + "'. Try to migrate ...");

            // INFO: Migration code
            domainId = migrateSetSlackChannelId(teamId, channelId);

            if (domainId != null) {
                log.info("Table entry linking team Id / channel Id '" + teamId + "' / '" + channelId + "' with domain Id '" + domainId + "' has been migrated successfully.");
            } else {
                log.warn("No domain linked with Slack team Id '" + teamId + "', but where channel Id is null, therefore migration not possible!");

                SlackDomainMapping[] mappings = getDomainMappingsForSlackTeam(teamId);
                if (mappings.length > 0) {
                    String logMsg = mappings.length + " mappings exist for team Id '" + teamId + "': ";
                    for (SlackDomainMapping mapping: mappings) {
                        logMsg = logMsg + mapping.getDomainId() + " ";
                    }
                    log.info(logMsg);
                } else {
                    log.error("No domain Id linked with team Id '" + teamId + "' yet!");
                }
            }
        }

        return domainId;
    }

    /**
     * Update connection status of a particular Katie / Slack mapping
     * @param status Connection status
     */
    public void updateSlackConnectMappingStatus(String teamId, String channelId, ConnectStatus status) {
        try {
            String sql = "Update " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " set " + "STATUS" + "='" + status + "'  where " + SLACK_TEAM_ID + " = '" + teamId + "' and " + SLACK_CHANNEL_ID + "='" + channelId + "'";
            log.info("Update status: " + sql);

            updateTableRecord(sql);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Get all domain Slack mappings
     * @return array of domain Slack mappings
     */
    public SlackDomainMapping[] getDomainMappingsForSlack() {
        List<SlackDomainMapping> mappings = new ArrayList<SlackDomainMapping>();

        String sql = "Select * from " + TABLE_SLACK_TEAM_KATIE_DOMAIN;
        //String sql = "Select * from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where " + SLACK_CHANNEL_ID + " is null";
        log.info("Try to get Slack domain mappings from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String teamId = rs.getString(SLACK_TEAM_ID);
                String channelId = rs.getString(SLACK_CHANNEL_ID);
                String domainId = rs.getString(KATIE_DOMAIN_ID);
                long created = rs.getLong(TIMESTAMP_CREATED);
                ConnectStatus status = ConnectStatus.valueOf(rs.getString("STATUS"));

                // INFO: Do NOT return approval token
                mappings.add(new SlackDomainMapping(teamId, channelId, domainId, new Date(created), status, null));

            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return mappings.toArray(new SlackDomainMapping[0]);
    }

    /**
     * Get Slack domain mapping linked with a particular Katie domain and Slack channel
     * @param domainId Katie domain Id
     * @param channelId Slack channel Id
     * @return Slack domain mapping
     */
    public SlackDomainMapping getSlackDomainMappingForDomain(String domainId, String channelId) {
        SlackDomainMapping mapping = null;

        String sql = "Select * from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where " + KATIE_DOMAIN_ID + " = '" + domainId + "' and " + SLACK_CHANNEL_ID + "='" + channelId + "'";
        log.info("Try to get Slack domain mappings from database: " + sql);

        int numberOfMappings = 0;
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String teamId = rs.getString(SLACK_TEAM_ID);
                long created = rs.getLong(TIMESTAMP_CREATED);
                ConnectStatus status = ConnectStatus.valueOf(rs.getString("STATUS"));
                String token = rs.getString("APPROVAL_TOKEN");

                mapping = new SlackDomainMapping(teamId, channelId, domainId, new Date(created), status, token);
                numberOfMappings++;
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (mapping == null) {
            log.warn("Katie domain '" + domainId + "' not linked with Slack channel '" + channelId + "'!");
        }

        if (numberOfMappings > 1) {
            log.error("There are more than 1 mappings for domain '" + domainId + "' and Slack channel '" + channelId + "'!");
        }

        return mapping;
    }

    /**
     * Get all domain mappings linked with a particular Slack team (workspace)
     * @param teamId Team Id, e.g. 'T01848J69AP'
     * @return array of domain mappings linked with particular Slack team
     */
    public SlackDomainMapping[] getDomainMappingsForSlackTeam(String teamId) {
        List<SlackDomainMapping> mappings = new ArrayList<SlackDomainMapping>();

        String sql = "Select * from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where " + SLACK_TEAM_ID + " = '" + teamId + "'";
        log.info("Try to get Slack domain mappings from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String channelId = rs.getString(SLACK_CHANNEL_ID);
                String domainId = rs.getString(KATIE_DOMAIN_ID);
                long created = rs.getLong(TIMESTAMP_CREATED);
                ConnectStatus status = ConnectStatus.valueOf(rs.getString("STATUS"));
                String token = rs.getString("APPROVAL_TOKEN");

                mappings.add(new SlackDomainMapping(teamId, channelId, domainId, new Date(created), status, token));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (mappings.size() == 0) {
            log.warn("No domains linked with Slack team Id '" + teamId + "'.");
        }

        return mappings.toArray(new SlackDomainMapping[0]);
    }

    /**
     * Get all Slack channels linked with a particular Katie domain
     * @param domainId Katie domain Id, e.g. '71aa8dc6-0f19-4787-bd91-08fe1e863473'
     * @return array of domain mappings
     */
    public SlackDomainMapping[] getSlackChannelsForDomain(String domainId) {
        List<SlackDomainMapping> mappings = new ArrayList<SlackDomainMapping>();

        String sql = "Select * from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where " + KATIE_DOMAIN_ID + " = '" + domainId + "'";
        log.info("Try to get Slack domain mappings from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String channelId = rs.getString(SLACK_CHANNEL_ID);
                String teamId = rs.getString(SLACK_TEAM_ID);
                long created = rs.getLong(TIMESTAMP_CREATED);
                ConnectStatus status = ConnectStatus.valueOf(rs.getString("STATUS"));
                String token = rs.getString("APPROVAL_TOKEN");

                mappings.add(new SlackDomainMapping(teamId, channelId, domainId, new Date(created), status, token));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (mappings.size() == 0) {
            log.warn("No mappings linked with Katie domain Id '" + domainId + "'.");
        }

        return mappings.toArray(new SlackDomainMapping[0]);
    }

    /**
     * Get domain mapping linked with a particular Slack team / channel
     * @param teamId Team Id, e.g. 'T01848J69AP'
     * @param channelId Channel Id
     * @return domain mapping linked with particular Slack team / channel
     */
    public SlackDomainMapping getDomainMappingForSlackTeamChannel(String teamId, String channelId) {
        SlackDomainMapping mapping = null;

        String sql = "Select * from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where " + SLACK_TEAM_ID + " = '" + teamId + "' and " + SLACK_CHANNEL_ID + " = '" + channelId + "'";
        log.info("Try to get Slack domain mappings from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String domainId = rs.getString(KATIE_DOMAIN_ID);
                long created = rs.getLong(TIMESTAMP_CREATED);
                ConnectStatus status = ConnectStatus.valueOf(rs.getString("STATUS"));
                String token = rs.getString("APPROVAL_TOKEN");

                mapping = new SlackDomainMapping(teamId, channelId, domainId, new Date(created), status, token);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (mapping == null) {
            log.warn("No domain linked with Slack team / channel '" + teamId + " / "+ channelId + "'.");
        }

        return mapping;
    }

    /**
     * Get domain Id linked with team Id, but where Slack channel Id is null. If such a domain Id exists, then set Slack channel Id
     * @param teamId Team Id, e.g. 'T01848J69AP'
     * @param channelId Channel Id, e.g. 'C01BG53KWLA'
     * @return domain Id linked with Slack team Id and channel Id
     */
    public String migrateSetSlackChannelId(String teamId, String channelId) {
        String domainId = null;
        
        String sql = "Select " + KATIE_DOMAIN_ID + " from " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " where " + SLACK_TEAM_ID + " = '" + teamId + "' and " + SLACK_CHANNEL_ID + " is null";
        log.info("Try to get domain Id from database where team Id is '" + teamId + "', but channel Id is null: " + sql);

        try {
            Class.forName(driverClassName);

            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainId = rs.getString(KATIE_DOMAIN_ID);
            }
            rs.close();
            stmt.close();
            conn.close();

            if (domainId != null) {
                log.info("Domain Id found '" + domainId + "' for team Id '" + teamId + "', but where channel Id is null.");

                sql = "Update " + TABLE_SLACK_TEAM_KATIE_DOMAIN + " set " + SLACK_CHANNEL_ID + "='" + channelId + "'  where " + SLACK_TEAM_ID + " = '" + teamId + "' and " + SLACK_CHANNEL_ID + " is null";
                log.info("Set channel Id: " + sql);

                updateTableRecord(sql);
            } else {
                log.warn("No domain linked with Slack team Id '" + teamId + "' where channel Id is null!");
                return null;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return domainId;
    }

    /**
     * Get all domain MS Teams mappings
     * @return array of domain MS Teams mappings
     */
    public MSTeamsDomainMapping[] getDomainMappingsForMSTeams() {
        List<MSTeamsDomainMapping> mappings = new ArrayList<MSTeamsDomainMapping>();

        String sql = "Select * from " + TABLE_MS_TEAM_KATIE_DOMAIN;
        log.info("Try to get MS Teams domain mappings from database: " + sql);

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String teamId = rs.getString(MS_TEAM_ID);
                //String channelId = rs.getString(MS_TEAM_CHANNEL_ID);
                String domainId = rs.getString(KATIE_DOMAIN_ID);
                long created = rs.getLong(TIMESTAMP_CREATED);
                //com.wyona.katie.models.msteams.ConnectStatus status = com.wyona.katie.models.msteams.ConnectStatus.valueOf(rs.getString("STATUS"));
                com.wyona.katie.models.msteams.ConnectStatus status = com.wyona.katie.models.msteams.ConnectStatus.APPROVED;

                // INFO: Do NOT return approval token
                mappings.add(new MSTeamsDomainMapping(teamId, null, domainId, new Date(created), status, null));

            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return mappings.toArray(new MSTeamsDomainMapping[0]);
    }

    /**
     * Get context/domain associated with "MS Teams" team
     * @param teamId Team Id, e.g. '19:6b1b66b1617c4946959c334b4d428fc0@thread.tacv2'
     * @return context/domain Id, e.g. 'wyona'
     */
    public String getDomainIdForMSTeam(String teamId) {
        String domainId = null;
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select " + KATIE_DOMAIN_ID + " from " + TABLE_MS_TEAM_KATIE_DOMAIN + " where " + MS_TEAM_ID + " = '" + teamId + "'";
            log.info("Try to get context/domain Id from database: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainId = rs.getString(KATIE_DOMAIN_ID);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (domainId == null) {
            log.warn("No domain linked with MS team Id '" + teamId + "'.");
        }
        return domainId;
    }

    /**
     * Get Katie domain associated with Matrix room
     * @param roomId Team Id, e.g. '!ZuEJFYVSrFbPWVLQjv:matrix.org'
     * @return Katie domain Id (e.g. 'wyona') and association exists, otherwise return null
     */
    public String getDomainIdForMatrix(String roomId) {
        String domainId = null;
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select " + KATIE_DOMAIN_ID + " from " + TABLE_MATRIX_KATIE_DOMAIN + " where MATRIX_ROOM_ID = '" + roomId + "'";
            log.info("Try to get domain Id from database: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainId = rs.getString(KATIE_DOMAIN_ID);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (domainId == null) {
            log.warn("No domain linked with Matrix room Id '" + roomId + "'.");
        }
        return domainId;
    }

    /**
     * Get Katie domain Id associated with domain tag name
     * @param tagName Tag name of domain, e.g. "apache-lucene"
     * @return Katie domain Id (e.g. '45c6068a-e94b-46d6-zfa1-938f755d446g') if mapping exists, otherwise return null
     */
    public String getDomainIdForTagName(String tagName) {
        String domainId = null;

        String sql = "Select " + "DOMAIN_ID" + " from " + TABLE_DOMAIN_TAG_NAME + " where TAG_NAME = '" + tagName + "'";
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            log.info("Try to get domain Id from database: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                domainId = rs.getString("DOMAIN_ID");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (domainId == null) {
            log.warn("No domain linked with tag name '" + tagName + "'.");
        }
        return domainId;
    }

    /**
     * Get domain tag name of a particular domain
     * @param domainId Domain Id, e.g. '45c6068a-e94b-46d6-zfa1-938f755d446g'
     * @return tag name (e.g. 'apache-lucene') if mapping exists, otherwise return null
     */
    public String getTagName(String domainId) {
        String tagName = null;

        String sql = "Select " + "TAG_NAME" + " from " + TABLE_DOMAIN_TAG_NAME + " where DOMAIN_ID = '" + domainId + "'";
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            log.debug("Try to get tag name from database: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                tagName = rs.getString("TAG_NAME");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (tagName == null) {
            log.warn("No tag name linked with domain Id '" + domainId + "'.");
        }
        return tagName;
    }

    /**
     * Update domain tag name
     * @param domainId Domain Id
     * @param newTagName New tag name, e.g. "apache-lucene"
     */
    public void updateTagName(String domainId, String newTagName) throws Exception {
        if (getDomainIdForTagName(newTagName) != null) {
            throw new Exception("Tag name is not available!");
        }

        String sql = null;
        if (getTagName(domainId) != null) {
            sql = "Update " + TABLE_DOMAIN_TAG_NAME + " set " + "TAG_NAME" + "='" + newTagName + "' where " + "DOMAIN_ID" + "='" + domainId + "'";
            log.info("Update tag name: " + sql);
        } else {
            sql = "Insert Into " + TABLE_DOMAIN_TAG_NAME + " (TAG_NAME, DOMAIN_ID) Values ('" + newTagName + "','" + domainId + "')";
            log.info("Insert tag name: " + sql);
        }

        insert(sql);
    }

    /**
     * Get Slack bearer token of a particular domain
     * @param domainId Katie domain Id
     * @return bearer token
     */
    public String getSlackBearerTokenOfDomain(String domainId, String channelId) {
        String teamId = getSlackDomainMappingForDomain(domainId, channelId).getTeamId();

        return getSlackBearerTokenOfTeam(teamId);
    }

    /**
     * Get bearer token of a particular Slack team, which was received from Slack via OAuth2 callback
     * @param teamId Slack team Id
     * @return bearer token
     */
    public String getSlackBearerTokenOfTeam(String teamId) {
        String bearerToken = null;

        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);

            Statement stmt = conn.createStatement();
            String sql = "Select " + SLACK_BEARER_TOKEN + " from " + TABLE_SLACK_TEAM_TOKEN_USERID + " where " + SLACK_TEAM_ID + " = '" + teamId + "'";
            log.info("Try to get bearer token of a particular Slack team from database: " + sql);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                bearerToken = rs.getString(SLACK_BEARER_TOKEN);
                log.info("Retrieved Bearer token is associated with team '" + teamId + "'.");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (bearerToken == null) {
            log.warn("No Slack bearer token for Slack team '" + teamId + "'.");
        }

        return bearerToken;
    }
}
