package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * TODO: Subclass this class for the various implementations
 */
@Slf4j
public class KnowledgeSourceMeta {

    private String id;
    private String name;
    private boolean isEnabled;
    private KnowledgeSourceConnector connector;
    private Date dateSyncedSuccessfully;
    private int numberOfChunksAdded;
    private int maxItems;

    private String directusBaseUrl;
    private String directusCollection;

    private String supabaseIdName;
    private String[] supabaseQuestionNames;
    private String[] supabaseAnswerNames;
    private String supabaseBaseUrl;
    private String[] supabaseClassificationsNames;

    private String confluenceBaseUrl;
    private String confluenceUsername;
    private String confluenceToken;

    private String groundedQAbaseUrl;
    private String groundedQASiteUrl;
    private String groundedQATopic;

    private String websiteSeedUrl;
    private List<String> websiteURLs;

    private int chunkSize;
    private int chunkOverlap;
    private Character chunkSeparator;

    private String microsoftGraphApiToken;
    private String msTenant;
    private String msClientId;
    private String msClientSecret;
    private String msScope;

    private String oneNoteLocation;
    private String msGraphUrlLocation;

    private String sharepointSiteId;
    private String sharepointWebBaseUrl;

    private String weaviateWikipediaSearchUrl;
    private String weaviateWikipediaSearchKey;
    private String weaviateWikipediaSearchCohereKey;

    private String thirdPartyRAGUrl;
    private String thirdPartyRAGBody;

    private String configurationErrorMessage;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public KnowledgeSourceMeta() {
    }

    /**
     * @param id UUID
     * @param name Display name, e.g. "AXA-ARAG Sharepoint" or "Weaviate documentation website"
     * @param isEnabled True when knowledge source is enabled
     */
    public KnowledgeSourceMeta(String id, String name, boolean isEnabled, KnowledgeSourceConnector connector) {
        this.id = id;
        this.name = name;
        this.isEnabled = isEnabled;
        this.connector = connector;
        this.dateSyncedSuccessfully = null;
        this.numberOfChunksAdded = -1;
        this.maxItems = 10; // INFO: Default

        this.directusBaseUrl = null;
        this.directusCollection = null;

        this.confluenceBaseUrl = null;
        this.confluenceUsername = null;
        this.confluenceToken = null;

        this.groundedQAbaseUrl = null;
        this.groundedQASiteUrl = null;
        this.groundedQATopic = null;

        this.websiteSeedUrl = null;
        this.websiteURLs = new ArrayList<String>();

        chunkSize = -1;
        chunkOverlap = -1;
        chunkSeparator = null;

        this.microsoftGraphApiToken = null;
        this.msTenant = null;
        this.msClientId = null;
        this.msClientSecret = null;
        this.msScope = null;

        this.oneNoteLocation = null;
        this.msGraphUrlLocation = null;

        this.sharepointSiteId = null;
        this.sharepointWebBaseUrl = null;

        this.weaviateWikipediaSearchUrl = null;
        this.weaviateWikipediaSearchKey = null;
        this.weaviateWikipediaSearchCohereKey = null;

        this.thirdPartyRAGUrl = null;
        this.thirdPartyRAGBody = null;
    }

    /**
     *
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Get display name of knowledge source
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public boolean getIsEnabled() {
        return isEnabled;
    }

    /**
     *
     */
    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * @return date when knowledge base was synchronized successfully
     */
    public Date getDateSyncedSuccessfully() {
        return dateSyncedSuccessfully;
    }

    /**
     * @param date Date when knowledge base was synchronized successfully
     */
    public void setDateSyncedSuccessfully(Date date) {
        this.dateSyncedSuccessfully = date;
    }

    /**
     * @return number of chunks that got added during last successful synchronization
     */
    public int getNumberOfChunksAdded() {
        return numberOfChunksAdded;
    }

    /**
     *
     */
    public void setNumberOfChunksAdded(int numberOfChunksAdded) {
        this.numberOfChunksAdded = numberOfChunksAdded;
    }

    /**
     *
     */
    public int getMaxItems() {
        return maxItems;
    }

    /**
     *
     */
    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    /**
     *
     */
    public KnowledgeSourceConnector getConnector() {
        return connector;
    }

    /**
     * @param name Custom Supabase Id name, e.g. "id"
     */
    public void setSupabaseIdName(String name) {
        this.supabaseIdName = name;
    }

    /**
     *
     */
    public String getSupabaseIdName() {
        return supabaseIdName;
    }

    /**
     *
     */
    public void setSupabaseQuestionNames(String[] names) {
        this.supabaseQuestionNames = names;
    }

    /**
     *
     */
    public String[] getSupabaseQuestionNames() {
        return supabaseQuestionNames;
    }

    /**
     *
     */
    public void setSupabaseAnswerNames(String[] names) {
        this.supabaseAnswerNames = names;
    }

    /**
     *
     */
    public String[] getSupabaseAnswerNames() {
        return supabaseAnswerNames;
    }

    /**
     * @param names Array of field names, e.g. "tags", "classifications", "authors"
     */
    public void setSupabaseClassificationsNames(String[] names) {
        this.supabaseClassificationsNames = names;
    }

    /**
     *
     */
    public String[] getSupabaseClassificationsNames() {
        return supabaseClassificationsNames;
    }

    /**
     *
     */
    public void setSupabaseBaseUrl(String url) {
        this.supabaseBaseUrl = url;
    }

    /**
     *
     */
    public String getSupabaseBaseUrl() {
        return supabaseBaseUrl;
    }

    /**
     *
     */
    public void setDirectusBaseUrl(String directusBaseUrl) {
        this.directusBaseUrl = directusBaseUrl;
    }

    /**
     * @return Directus base URL, e.g. "https://repositorium.directus.app"
     */
    public String getDirectusBaseUrl() {
        return directusBaseUrl;
    }

    /**
     *
     */
    public void setDirectusCollection(String directusCollection) {
        this.directusCollection = directusCollection;
    }

    /**
     * @return Directus collection name, e.g. "Repositorium"
     */
    public String getDirectusCollection() {
        return directusCollection;
    }

    /**
     * @param confluenceBaseUrl Confluence base URL, e.g. "https://wyona.atlassian.net"
     */
    public void setConfluenceBaseUrl(String confluenceBaseUrl) {
        this.confluenceBaseUrl = confluenceBaseUrl;
    }

    /**
     * @return Confluence base URL, e.g. "https://wyona.atlassian.net"
     */
    public String getConfluenceBaseUrl() {
        return confluenceBaseUrl;
    }

    /**
     *
     */
    public String getConfluenceUsername() {
        return confluenceUsername;
    }

    /**
     *
     */
    public void setConfluenceUsername(String username) {
        this.confluenceUsername = username;
    }

    /**
     * https://id.atlassian.com/manage-profile/security/api-tokens
     *
     */
    public String getConfluenceToken() {
        return confluenceToken;
    }

    /**
     *
     */
    public void setConfluenceToken(String token) {
        this.confluenceToken = token;
    }

    /**
     * @return grounded QA base URL, e.g. "http://localhost:5007" or "https://grounded-qa.ukatie.com"
     */
    public String getGroundedQAbaseUrl() {
        return groundedQAbaseUrl;
    }

    /**
     *
     */
    public void setGroundedQAbaseUrl(String baseUrl) {
        this.groundedQAbaseUrl = baseUrl;
    }

    /**
     * @return site URL to limit retrieval to a particular web site, e.g. "https://tnfd.global"
     */
    public String getGroundedQASiteUrl() {
        return groundedQASiteUrl;
    }

    /**
     *
     */
    public void setGroundedQASiteUrl(String siteUrl) {
        this.groundedQASiteUrl = siteUrl;
    }

    /**
     * @return topic, e.g. "Weaviate" or "Moodle"
     */
    public String getGroundedQATopic() {
        return groundedQATopic;
    }

    /**
     *
     */
    public void setGroundedQATopic(String topic) {
        this.groundedQATopic = topic;
    }

    /**
     * @param url Website seed URL (e.g. "https://www.stadt-zuerich.ch"), from which a web crawler will begin to traverse the website.
     */
    public void setWebsiteSeedUrl(String url) {
        this.websiteSeedUrl = url;
    }

    /**
     * @return website seed URL (e.g. "https://www.stadt-zuerich.ch"), from which a web crawler will begin to traverse the website.
     */
    public String getWebsiteSeedUrl() {
        return websiteSeedUrl;
    }

    /**
     * @param urls Comma separated list of individual URLs, which are supposed to be dumped, but not used by web crawler to begin traversal
     */
    public void setWebsiteIndividualURLs(String[] urls) {
        for (String url : urls) {
            this.websiteURLs.add(url);
        }
    }

    /**
     *
     */
    public String[] getWebsiteIndividualURLs() {
        return websiteURLs.toArray(new String[0]);
    }

    /**
     *
     */
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     *
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     *
     */
    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    /**
     *
     */
    public int getChunkOverlap() {
        return chunkOverlap;
    }

    /**
     *
     */
    public void setChunkSeparator(Character chunkSeparator) {
        this.chunkSeparator = chunkSeparator;
    }

    /**
     *
     */
    public Character getChunkSeparator() {
        return chunkSeparator;
    }

    /**
     *
     */
    public void setMicrosoftGraphApiToken(String apiToken) {
        this.microsoftGraphApiToken = apiToken;
    }

    /**
     *
     */
    public String getMicrosoftGraphApiToken() {
        return this.microsoftGraphApiToken;
    }

    /**
     *
     */
    public void setMsTenant(String msTenant) {
        this.msTenant = msTenant;
    }

    /**
     * @return Microsoft tenant, e.g. "c5dce9b8-8095-444d-9730-5ccb69b43413"
     */
    public String getMsTenant() {
        return msTenant;
    }

    /**
     *
     */
    public void setMsClientId(String msClientId) {
        this.msClientId = msClientId;
    }

    /**
     * @return Microsoft client id, e.g. "046d7c6c-c3c0-40f0-93a1-abcd73ce5cbe"
     */
    public String getMsClientId() {
        return msClientId;
    }

    /**
     *
     */
    public void setMsClientSecret(String msClientSecret) {
        this.msClientSecret = msClientSecret;
    }

    /**
     * @return Microsoft client secret
     */
    public String getMsClientSecret() {
        return msClientSecret;
    }

    /**
     * @param scope Scope, e.g. "Notes.Read.All"
     */
    public void setMsScope(String scope) {
        this.msScope = scope;
    }

    /**
     * @return scope, e.g. "Notes.Read.All"
     */
    public String getMsScope() {
        return this.msScope;
    }

    /**
     * See https://learn.microsoft.com/en-us/graph/api/resources/onenote-api-overview?view=graph-rest-1.0
     * @param oneNoteLocation OneNote location, e.g. "groups/c5a3125f-f85a-472a-8561-db2cf74396ea" or "users/MichaelWechner@Wyona.onmicrosoft.com"
     */
    public void setOneNoteLocation(String oneNoteLocation) {
        this.oneNoteLocation = oneNoteLocation;
    }

    /**
     * See https://learn.microsoft.com/en-us/graph/api/resources/onenote-api-overview?view=graph-rest-1.0
     * @return OneNote location, e.g. "groups/c5a3125f-f85a-472a-8561-db2cf74396ea" or "users/MichaelWechner@Wyona.onmicrosoft.com"
     */
    public String getOneNoteLocation() {
        return oneNoteLocation;
    }

    /**
     * @param msGraphUrlLocation Microsoft Graph URL location, e.g. "users/MichaelWechner@Wyona.onmicrosoft.com"
     */
    public void setMsGraphUrlLocation(String msGraphUrlLocation) {
        this.msGraphUrlLocation = msGraphUrlLocation;
    }

    /**
     * @return Microsoft Graph URL location, e.g. "users/MichaelWechner@Wyona.onmicrosoft.com"
     */
    public String getMsGraphUrlLocation() {
        return this.msGraphUrlLocation;
    }

    /**
     * GET https://graph.microsoft.com/v1.0/sites/root (https://wyona.sharepoint.com)
     * @param sharepointSiteId Sharepoint site Id, e.g. "ee203dc1-cfd8-4fce-8179-c11dd4db249c"
     */
    public void setSharepointSiteId(String sharepointSiteId) {
        this.sharepointSiteId = sharepointSiteId;
    }

    /**
     * A site id can be obtained for example using Microsoft Graph Explorer
     * GET v1.0 https://graph.microsoft.com/v1.0/sites/root
     * GET https://graph.microsoft.com/v1.0/sites/wyona.sharepoint.com:/sites/KatieTest?$select=id
     * GET https://wyona.sharepoint.com/_api/site/id
     * @return SharePoint site Id, e.g. "ee203dc1-cfd8-4fce-8179-c11dd4db249c"
     */
    public String getSharepointSiteId() {
        return sharepointSiteId;
    }

    /**
     * @param sharepointWebBaseUrl SharePoint base URL, e.g. "https://wyona.sharepoint.com"
     */
    public void setSharepointWebBaseUrl(String sharepointWebBaseUrl) {
        this.sharepointWebBaseUrl = sharepointWebBaseUrl;
    }

    /**
     * @return SharePoint base URL, e.g. "https://wyona.sharepoint.com"
     */
    public String getSharepointWebBaseUrl() {
        return sharepointWebBaseUrl;
    }

    /**
     * @param weaviateWikipediaSearchUrl URL, e.g. https://cohere-demo.weaviate.network
     */
    public void setWeaviateWikipediaSearchUrl(String weaviateWikipediaSearchUrl) {
        this.weaviateWikipediaSearchUrl = weaviateWikipediaSearchUrl;
    }

    /**
     * @return request URL, e.g. https://cohere-demo.weaviate.network
     */
    public String getWeaviateWikipediaSearchUrl() {
        return weaviateWikipediaSearchUrl;
    }

    /**
     *
     */
    public void setWeaviateWikipediaSearchKey(String key) {
        this.weaviateWikipediaSearchKey = key;
    }

    /**
     *
     */
    public String getWeaviateWikipediaSearchKey() {
        return weaviateWikipediaSearchKey;
    }

    /**
     *
     */
    public void setGetWeaviateWikipediaSearchCohereKey(String key) {
        this.weaviateWikipediaSearchCohereKey = key;
    }

    /**
     *
     */
    public String getWeaviateWikipediaSearchCohereKey() {
        return weaviateWikipediaSearchCohereKey;
    }

    /**
     *
     */
    public void setThirdPartyRAGUrl(String thirdPartyRAGUrl) {
        this.thirdPartyRAGUrl = thirdPartyRAGUrl;
    }

    /**
     *
     */
    public String getThirdPartyRAGUrl() {
        return thirdPartyRAGUrl;
    }

    /**
     *
     */
    public void setGetThirdPartyRAGBody(String thirdPartyRAGBody) {
        this.thirdPartyRAGBody = thirdPartyRAGBody;
    }

    /**
     *
     */
    public String getGetThirdPartyRAGBody() {
        return thirdPartyRAGBody;
    }

    /**
     *
     */
    public void setConfigurationErrorMessage(String message) {
        this.configurationErrorMessage = message;
    }

    /**
     *
     */
    public String getConfigurationErrorMessage() {
        return configurationErrorMessage;
    }
}
