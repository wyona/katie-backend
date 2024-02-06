package com.wyona.katie.services;

import com.wyona.katie.models.KnowledgeSourceConnector;
import com.wyona.katie.models.KnowledgeSourceMeta;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.io.*;
import java.util.*;

@Slf4j
@Component
public class KnowledgeSourceXMLFileService {

    @Value("${contexts.data_path}")
    private String contextsDataPath;

    @Autowired
    private XMLService xmlService;

    private static final String KATIE_NAMESPACE_1_0_0 = "http://www.wyona.com/askkatie/1.0.0";

    private static final String KNOWLEDGE_SOURCE_TAG = "ks";
    private static final String KNOWLEDGE_SOURCE_ID_ATTR = "id";
    private static final String KNOWLEDGE_SOURCE_CONNECTOR_ATTR = "connector";
    private static final String KNOWLEDGE_SOURCE_ENABLED_ATTR = "enabled";
    private static final String KNOWLEDGE_SOURCE_NAME_TAG = "name";
    private static final String KS_DATE_SYNCED_ATTR = "date-synced-successfully";
    private static final String KS_CHUNKS_ADDED_ATTR = "number-chunks-added";
    private static final String KS_MAX_ITEMS_ATTR = "max-items";

    private static final String MS_GRAPH_TAG = "microsoft-graph";
    private static final String MS_GRAPH_API_TOKEN_ATTR = "api-token";
    private static final String MS_GRAPH_TENANT_ATTR = "tenant";
    private static final String MS_GRAPH_CLIENT_ID_ATTR = "client-id";
    private static final String MS_GRAPH_CLIENT_SECRET_ATTR = "client-secret";
    private static final String MS_GRAPH_SCOPE_ATTR = "scope";
    private static final String MS_GRAPH_ONENOTE_LOCATION_ATTR = "onenote-location";
    private static final String MS_GRAPH_LOCATION_ATTR = "location";
    private static final String MS_GRAPH_SHAREPOINT_SITE_ID_ATTR = "sharepoint-site-id";
    private static final String MS_GRAPH_SHAREPOINT_BASE_URL_ATTR = "sharepoint-base-url";

    private static final String WEBSITE_TAG = "website";
    private static final String WEBSITE_SEED_URL_ATTR = "seed-url";
    private static final String WEBSITE_URLS_ELEMENT = "urls";
    private static final String WEBSITE_URL_ELEMENT = "url";
    private static final String WEBSITE_CHUNK_SIZE_ATTR = "chunk-size";
    private static final String WEBSITE_CHUNK_OVERLAP_ATTR = "chunk-overlap";

    private static final String THIRD_PARTY_RAG_TAG = "third-party-rag";

    /**
     * Get knowledge sources
     */
    public KnowledgeSourceMeta[] getKnowledgeSources(String domainId) throws Exception {
        Document doc = getKnowledgeSourcesDocument(domainId);
        if (doc == null) {
            log.warn("No knowledge sources configuration exists for domain '" + domainId + "'.");
            return new KnowledgeSourceMeta[0];
        }

        NodeList ksNL = doc.getElementsByTagName(KNOWLEDGE_SOURCE_TAG);
        List<KnowledgeSourceMeta> knowledgeSources = new ArrayList<KnowledgeSourceMeta>();
        for (int i = 0; i < ksNL.getLength(); i++) {
            Element ksEl = (Element)ksNL.item(i);

            String id = ksEl.getAttribute(KNOWLEDGE_SOURCE_ID_ATTR);
            boolean isEnabled = Boolean.parseBoolean(ksEl.getAttribute(KNOWLEDGE_SOURCE_ENABLED_ATTR));
            String name = xmlService.getDirectChildByTagName(ksEl, KNOWLEDGE_SOURCE_NAME_TAG).getTextContent();
            KnowledgeSourceConnector connector = KnowledgeSourceConnector.valueOf(ksEl.getAttribute(KNOWLEDGE_SOURCE_CONNECTOR_ATTR));

            KnowledgeSourceMeta ksMeta = new KnowledgeSourceMeta(id, name, isEnabled, connector, domainId);
            if (ksEl.hasAttribute(KS_DATE_SYNCED_ATTR)) {
                Date date = new Date(Long.parseLong(ksEl.getAttribute(KS_DATE_SYNCED_ATTR)));
                ksMeta.setDateSyncedSuccessfully(date);
            }
            if (ksEl.hasAttribute(KS_CHUNKS_ADDED_ATTR)) {
                ksMeta.setNumberOfChunksAdded(Integer.parseInt(ksEl.getAttribute(KS_CHUNKS_ADDED_ATTR)));
            }

            // INFO: If not set, then default value set by constructor is used
            if (ksEl.hasAttribute(KS_MAX_ITEMS_ATTR)) {
                ksMeta.setMaxItems(Integer.parseInt(ksEl.getAttribute(KS_MAX_ITEMS_ATTR)));
            }

            try {
                if (connector.equals(KnowledgeSourceConnector.DIRECTUS)) {
                    Element directusEl = xmlService.getDirectChildByTagName(ksEl, "directus");
                    ksMeta.setDirectusBaseUrl(directusEl.getAttribute("url"));
                    ksMeta.setDirectusCollection(directusEl.getAttribute("collection"));
                }

                if (connector.equals(KnowledgeSourceConnector.SUPABASE)) {
                    ksMeta.setSupabaseIdName("id"); // TODO: Should this also be configurable?!

                    Element supabaseEl = xmlService.getDirectChildByTagName(ksEl, "supabase");
                    ksMeta.setSupabaseBaseUrl(supabaseEl.getAttribute("url"));

                    String[] questionFieldNames = supabaseEl.getAttribute("question-field-names").split(",");
                    for (int k = 0; k < questionFieldNames.length; k++) {
                        // TODO
                        questionFieldNames[k] = questionFieldNames[k].trim();
                    }
                    ksMeta.setSupabaseQuestionNames(questionFieldNames);

                    String[] answerFieldNames = supabaseEl.getAttribute("answer-field-names").split(",");
                    for (int k = 0; k < answerFieldNames.length; k++) {
                        answerFieldNames[k] = answerFieldNames[k].trim();
                    }
                    ksMeta.setSupabaseAnswerNames(answerFieldNames);

                    String[] classificationFieldNames = supabaseEl.getAttribute("classifications-field-names").split(",");
                    for (int k = 0; k < classificationFieldNames.length; k++) {
                        classificationFieldNames[k] = classificationFieldNames[k].trim();
                    }
                    ksMeta.setSupabaseClassificationsNames(classificationFieldNames);
                }

                if (connector.equals(KnowledgeSourceConnector.ONENOTE) || connector.equals(KnowledgeSourceConnector.SHAREPOINT) || connector.equals(KnowledgeSourceConnector.OUTLOOK)) {
                    Element microsoftGraphEl = xmlService.getDirectChildByTagName(ksEl, MS_GRAPH_TAG);
                    if (microsoftGraphEl.hasAttribute(MS_GRAPH_API_TOKEN_ATTR)) {
                        ksMeta.setMicrosoftGraphApiToken(microsoftGraphEl.getAttribute(MS_GRAPH_API_TOKEN_ATTR));
                    }
                    if (microsoftGraphEl.hasAttribute(MS_GRAPH_TENANT_ATTR)) {
                        ksMeta.setMsTenant(microsoftGraphEl.getAttribute(MS_GRAPH_TENANT_ATTR));
                    }
                    if (microsoftGraphEl.hasAttribute(MS_GRAPH_CLIENT_ID_ATTR)) {
                        ksMeta.setMsClientId(microsoftGraphEl.getAttribute(MS_GRAPH_CLIENT_ID_ATTR));
                    }
                    if (microsoftGraphEl.hasAttribute(MS_GRAPH_CLIENT_SECRET_ATTR)) {
                        ksMeta.setMsClientSecret(microsoftGraphEl.getAttribute(MS_GRAPH_CLIENT_SECRET_ATTR));
                    }
                    if (microsoftGraphEl.hasAttribute(MS_GRAPH_SCOPE_ATTR)) {
                        ksMeta.setMsScope(microsoftGraphEl.getAttribute(MS_GRAPH_SCOPE_ATTR));
                    }
                    if (microsoftGraphEl.hasAttribute(MS_GRAPH_ONENOTE_LOCATION_ATTR)) {
                        ksMeta.setOneNoteLocation(microsoftGraphEl.getAttribute(MS_GRAPH_ONENOTE_LOCATION_ATTR));
                    }
                    if (microsoftGraphEl.hasAttribute(MS_GRAPH_LOCATION_ATTR)) {
                        ksMeta.setMsGraphUrlLocation(microsoftGraphEl.getAttribute(MS_GRAPH_LOCATION_ATTR));
                    }
                    if (microsoftGraphEl.hasAttribute(MS_GRAPH_SHAREPOINT_SITE_ID_ATTR)) {
                        ksMeta.setSharepointSiteId(microsoftGraphEl.getAttribute(MS_GRAPH_SHAREPOINT_SITE_ID_ATTR));
                        ksMeta.setSharepointWebBaseUrl(microsoftGraphEl.getAttribute(MS_GRAPH_SHAREPOINT_BASE_URL_ATTR));

                    }
                }

                if (connector.equals(KnowledgeSourceConnector.CONFLUENCE)) {
                    Element confluenceEl = xmlService.getDirectChildByTagName(ksEl, "confluence");
                    if (confluenceEl != null) {
                        ksMeta.setConfluenceBaseUrl(confluenceEl.getAttribute("url"));
                        ksMeta.setConfluenceUsername(confluenceEl.getAttribute("username"));
                        ksMeta.setConfluenceToken(confluenceEl.getAttribute("token"));
                    } else {
                        ksMeta.setConfigurationErrorMessage("Confluence configuration missing!");
                        //TODO: Disable knowledge source: ksMeta.setIsEnabled(true);
                    }
                }

                if (connector.equals(KnowledgeSourceConnector.GROUNDED_QA)) {
                    Element groundedQAEl = xmlService.getDirectChildByTagName(ksEl, "grounded-qa");
                    ksMeta.setGroundedQAbaseUrl(groundedQAEl.getAttribute("url"));
                    if (groundedQAEl.hasAttribute("site-url")) {
                        ksMeta.setGroundedQASiteUrl(groundedQAEl.getAttribute("site-url"));
                    }
                    if (groundedQAEl.hasAttribute("topic")) {
                        ksMeta.setGroundedQATopic(groundedQAEl.getAttribute("topic"));
                    }
                }

                if (connector.equals(KnowledgeSourceConnector.WEAVIATE_WIKIPEDIA_SEARCH)) {
                    Element weaviateWikipediaSearchEl = xmlService.getDirectChildByTagName(ksEl, "weaviate-wikipedia-search");
                    ksMeta.setWeaviateWikipediaSearchUrl(weaviateWikipediaSearchEl.getAttribute("url"));
                    ksMeta.setWeaviateWikipediaSearchKey(weaviateWikipediaSearchEl.getAttribute("weaviate-key"));
                    ksMeta.setGetWeaviateWikipediaSearchCohereKey(weaviateWikipediaSearchEl.getAttribute("cohere-key"));
                }

                if (connector.equals(KnowledgeSourceConnector.THIRD_PARTY_RAG)) {
                    Element thirdPartyRAGEl = xmlService.getDirectChildByTagName(ksEl, "third-party-rag");
                    ksMeta.setThirdPartyRAGUrl(thirdPartyRAGEl.getAttribute("url"));
                    Element bodyEl = xmlService.getDirectChildByTagName(thirdPartyRAGEl, "body");
                    ksMeta.setGetThirdPartyRAGBody(bodyEl.getFirstChild().getTextContent());
                }

                if (connector.equals(KnowledgeSourceConnector.TOP_DESK)) {
                    Element topDeskEl = xmlService.getDirectChildByTagName(ksEl, "topdesk");
                    ksMeta.setTopDeskBaseUrl(topDeskEl.getAttribute("base-url"));
                    ksMeta.setTopDeskUsername(topDeskEl.getAttribute("username"));
                    ksMeta.setTopDeskAPIPassword(topDeskEl.getAttribute("password"));
                }

                if (connector.equals(KnowledgeSourceConnector.WEBSITE)) {
                    Element websiteEl = xmlService.getDirectChildByTagName(ksEl, WEBSITE_TAG);

                    if (websiteEl.hasAttribute(WEBSITE_SEED_URL_ATTR)) {
                        ksMeta.setWebsiteSeedUrl(websiteEl.getAttribute(WEBSITE_SEED_URL_ATTR));
                    }

                    Element urlsEl = xmlService.getDirectChildByTagName(websiteEl, WEBSITE_URLS_ELEMENT);
                    if (urlsEl != null) {
                        List<Element> urlElements = xmlService.getDirectChildrenByTagName(urlsEl, WEBSITE_URL_ELEMENT);
                        List<String> urls = new ArrayList<String>();
                        for (Element urlEl : urlElements) {
                            urls.add(urlEl.getTextContent());
                        }
                        ksMeta.setWebsiteIndividualURLs(urls.toArray(new String[0]));
                    }

                    // TODO: set extract-css-selector

                    if (websiteEl.hasAttribute(WEBSITE_CHUNK_SIZE_ATTR)) {
                        ksMeta.setChunkSize(Integer.parseInt(websiteEl.getAttribute(WEBSITE_CHUNK_SIZE_ATTR)));
                    }
                    if (websiteEl.hasAttribute(WEBSITE_CHUNK_OVERLAP_ATTR)) {
                        ksMeta.setChunkOverlap(Integer.parseInt(websiteEl.getAttribute(WEBSITE_CHUNK_OVERLAP_ATTR)));
                    }
                    if (websiteEl.hasAttribute("chunk-separator")) {
                        // TODO: Set chunk separator, e.g. '\n' or ' '
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                ksMeta.setConfigurationErrorMessage(e.getMessage());
                //TODO: Disable knowledge source: ksMeta.setIsEnabled(true);
            }

            knowledgeSources.add(ksMeta);
        }

        return knowledgeSources.toArray(new KnowledgeSourceMeta[0]);
    }

    /**
     * @param csv Comma separated values
     */
    private String[] toArray(String csv) {
        String[] values = csv.split(",");
        List<String> list = Arrays.asList(values);
        return list.toArray(new String[0]);
    }

    /**
     *
     */
    public void updateSyncInfo(String domainId, String ksId, int numberOfChunksAdded) throws Exception {
        Document doc = getKnowledgeSourcesDocument(domainId);

        NodeList ksNL = doc.getElementsByTagName(KNOWLEDGE_SOURCE_TAG);
        for (int i = 0; i < ksNL.getLength(); i++) {
            Element ksEl = (Element)ksNL.item(i);
            String id = ksEl.getAttribute(KNOWLEDGE_SOURCE_ID_ATTR);
            if (id.equals(ksId)) {
                ksEl.setAttribute(KS_CHUNKS_ADDED_ATTR, "" + numberOfChunksAdded);
                ksEl.setAttribute(KS_DATE_SYNCED_ATTR, "" + new Date().getTime());

                File config = getKnowledgeSourcesConfig(domainId);
                xmlService.save(doc, config);

                return;
            }
        }
    }

    /**
     * Toggle knowledge source whether to be active or inactive
     * @param ksId Knowledge source Id
     */
    public void toggleEnabled(String domainId, String ksId) throws Exception {
        Document doc = getKnowledgeSourcesDocument(domainId);

        NodeList ksNL = doc.getElementsByTagName(KNOWLEDGE_SOURCE_TAG);
        for (int i = 0; i < ksNL.getLength(); i++) {
            Element ksEl = (Element)ksNL.item(i);
            String id = ksEl.getAttribute(KNOWLEDGE_SOURCE_ID_ATTR);
            if (id.equals(ksId)) {
                boolean isActive = Boolean.parseBoolean(ksEl.getAttribute(KNOWLEDGE_SOURCE_ENABLED_ATTR));
                boolean isActiveToggled = !isActive;
                ksEl.setAttribute(KNOWLEDGE_SOURCE_ENABLED_ATTR, "" + isActiveToggled);

                File config = getKnowledgeSourcesConfig(domainId);
                xmlService.save(doc, config);

                return;
            }
        }
    }

    /**
     * Set Microsoft Graph API Token
     * @param token API Token
     */
    public void setMsGraphApiTokenAttr(String domainId, String ksId, String token) throws Exception {
        Document doc = getKnowledgeSourcesDocument(domainId);
        NodeList ksNL = doc.getElementsByTagName(KNOWLEDGE_SOURCE_TAG);
        for (int i = 0; i < ksNL.getLength(); i++) {
            Element ksEl = (Element)ksNL.item(i);
            String id = ksEl.getAttribute(KNOWLEDGE_SOURCE_ID_ATTR);
            if (id.equals(ksId)) {
                Element microsoftGraphEl = xmlService.getDirectChildByTagName(ksEl, MS_GRAPH_TAG);
                microsoftGraphEl.setAttribute(MS_GRAPH_API_TOKEN_ATTR, token);

                File config = getKnowledgeSourcesConfig(domainId);
                xmlService.save(doc, config);

                return;
            }
        }
    }

    /**
     * Delete knowledge source
     */
    public void delete(String domainId, String ksId) throws Exception {
        log.info("Delete knowledge source '" + ksId + "' from domain '" + domainId + "' ...");
        Document doc = getKnowledgeSourcesDocument(domainId);

        NodeList ksNL = doc.getElementsByTagName(KNOWLEDGE_SOURCE_TAG);
        for (int i = 0; i < ksNL.getLength(); i++) {
            Element ksEl = (Element) ksNL.item(i);
            log.debug("Knowledge source Id: " + ksEl.getAttribute(KNOWLEDGE_SOURCE_ID_ATTR));
            if (ksEl.getAttribute(KNOWLEDGE_SOURCE_ID_ATTR).equals(ksId)) {
                log.info("Remove knowledge source element with id '" + ksId + "' ...");
                doc.getDocumentElement().removeChild(ksEl);
                break;
            }
        }

        File config = getKnowledgeSourcesConfig(domainId);
        xmlService.save(doc, config);
    }

    /**
     * Add OneNote knowledge source
     * @param scope Scope, e.g. "Notes.Read.All"
     * @param location OneNote location, e.g. "groups/c5a3125f-f85a-472a-8561-db2cf74396ea" or "users/michael.wechner@wyona.com"
     * @return knowledge source Id
     */
    public String addOneNote(String domainId, String name, String apiToken, String tenant, String clientId, String clientSecret, String scope, String location) throws Exception {
        log.info("Add OneNote as knowledge source to domain '" + domainId + "' ...");
        Document doc = getKnowledgeSourcesDocument(domainId);
        if (doc == null) {
            doc = xmlService.createDocument(KATIE_NAMESPACE_1_0_0, "knowledge-sources");
        }

        Element ksEl = doc.createElement(KNOWLEDGE_SOURCE_TAG);
        doc.getDocumentElement().appendChild(ksEl);
        String uuid = UUID.randomUUID().toString();
        ksEl.setAttribute(KNOWLEDGE_SOURCE_ID_ATTR, uuid);
        ksEl.setAttribute(KNOWLEDGE_SOURCE_ENABLED_ATTR, "false");
        ksEl.setAttribute(KNOWLEDGE_SOURCE_CONNECTOR_ATTR, KnowledgeSourceConnector.ONENOTE.toString());

        Element nameEl = doc.createElement("name");
        ksEl.appendChild(nameEl);
        nameEl.setTextContent(name);

        Element msGraphEl = doc.createElement(MS_GRAPH_TAG);
        ksEl.appendChild(msGraphEl);
        if (apiToken != null) {
            msGraphEl.setAttribute(MS_GRAPH_API_TOKEN_ATTR, apiToken);
        }
        if (tenant != null) {
            msGraphEl.setAttribute(MS_GRAPH_TENANT_ATTR, tenant);
        }
        if (clientId != null) {
            msGraphEl.setAttribute(MS_GRAPH_CLIENT_ID_ATTR, clientId);
        }
        if (clientSecret != null) {
            msGraphEl.setAttribute(MS_GRAPH_CLIENT_SECRET_ATTR, clientSecret);
        }
        if (scope != null) {
            msGraphEl.setAttribute(MS_GRAPH_SCOPE_ATTR, scope);
        }
        if (location != null) {
            msGraphEl.setAttribute(MS_GRAPH_ONENOTE_LOCATION_ATTR, location); // TODO: Remove, but beware of backwards compatibility!
            msGraphEl.setAttribute(MS_GRAPH_LOCATION_ATTR, location);
        }

        File config = getKnowledgeSourcesConfig(domainId);
        xmlService.save(doc, config);

        return uuid;
    }

    /**
     * Add SharePoint knowledge source
     * @param scope Scope, e.g. "Sites.Read.All"
     * @return knowledge source Id
     */
    public String addSharePoint(String domainId, String name, String apiToken, String tenant, String clientId, String clientSecret, String scope, String siteId, String baseUrl) throws Exception {
        log.info("Add SharePoint as knowledge source to domain '" + domainId + "' ...");
        Document doc = getKnowledgeSourcesDocument(domainId);
        if (doc == null) {
            doc = xmlService.createDocument(KATIE_NAMESPACE_1_0_0, "knowledge-sources");
        }

        Element ksEl = doc.createElement(KNOWLEDGE_SOURCE_TAG);
        doc.getDocumentElement().appendChild(ksEl);
        String uuid = UUID.randomUUID().toString();
        ksEl.setAttribute(KNOWLEDGE_SOURCE_ID_ATTR, uuid);
        ksEl.setAttribute(KNOWLEDGE_SOURCE_ENABLED_ATTR, "false");
        ksEl.setAttribute(KNOWLEDGE_SOURCE_CONNECTOR_ATTR, KnowledgeSourceConnector.SHAREPOINT.toString());

        Element nameEl = doc.createElement("name");
        ksEl.appendChild(nameEl);
        nameEl.setTextContent(name);

        Element msGraphEl = doc.createElement(MS_GRAPH_TAG);
        ksEl.appendChild(msGraphEl);
        if (apiToken != null) {
            msGraphEl.setAttribute(MS_GRAPH_API_TOKEN_ATTR, apiToken);
        }
        if (tenant != null) {
            msGraphEl.setAttribute(MS_GRAPH_TENANT_ATTR, tenant);
        }
        if (clientId != null) {
            msGraphEl.setAttribute(MS_GRAPH_CLIENT_ID_ATTR, clientId);
        }
        if (clientSecret != null) {
            msGraphEl.setAttribute(MS_GRAPH_CLIENT_SECRET_ATTR, clientSecret);
        }
        if (scope != null) {
            msGraphEl.setAttribute(MS_GRAPH_SCOPE_ATTR, scope);
        }
        if (siteId != null) {
            msGraphEl.setAttribute(MS_GRAPH_SHAREPOINT_SITE_ID_ATTR, siteId);
        }
        if (baseUrl != null) {
            msGraphEl.setAttribute(MS_GRAPH_SHAREPOINT_BASE_URL_ATTR, baseUrl);
        }

        File config = getKnowledgeSourcesConfig(domainId);
        xmlService.save(doc, config);

        return uuid;
    }

    /**
     * Add Website knowledge source
     * @return knowledge source Id
     */
    public String addWebsite(String domainId, String name, String seedUrl, String[] pageURLs, Integer chunkSize, Integer chunkOverlap) throws Exception {
        log.info("Add Website as knowledge source to domain '" + domainId + "' ...");
        Document doc = getKnowledgeSourcesDocument(domainId);
        if (doc == null) {
            doc = xmlService.createDocument(KATIE_NAMESPACE_1_0_0, "knowledge-sources");
        }

        Element ksEl = doc.createElement(KNOWLEDGE_SOURCE_TAG);
        doc.getDocumentElement().appendChild(ksEl);
        String uuid = UUID.randomUUID().toString();
        ksEl.setAttribute(KNOWLEDGE_SOURCE_ID_ATTR, uuid);
        ksEl.setAttribute(KNOWLEDGE_SOURCE_ENABLED_ATTR, "false");
        ksEl.setAttribute(KNOWLEDGE_SOURCE_CONNECTOR_ATTR, KnowledgeSourceConnector.WEBSITE.toString());

        Element nameEl = doc.createElement("name");
        ksEl.appendChild(nameEl);
        nameEl.setTextContent(name);

        Element websiteEl = doc.createElement(WEBSITE_TAG);
        ksEl.appendChild(websiteEl);

        if (seedUrl != null && seedUrl.startsWith("http")) {
            websiteEl.setAttribute(WEBSITE_SEED_URL_ATTR, seedUrl);
        }

        Element urlsEl = doc.createElement(WEBSITE_URLS_ELEMENT);
        websiteEl.appendChild(urlsEl);
        if (pageURLs != null && pageURLs.length > 0) {
            // TODO: Check whether URLs are valid
            for (int i = 0; i < pageURLs.length; i ++) {
                Element urlEl = doc.createElement(WEBSITE_URL_ELEMENT);
                urlEl.appendChild(doc.createTextNode(pageURLs[i]));
                urlsEl.appendChild(urlEl);
            }
        }

        if (chunkSize != null) {
            websiteEl.setAttribute(WEBSITE_CHUNK_SIZE_ATTR, chunkSize.toString());
        }

        if (chunkOverlap != null) {
            websiteEl.setAttribute(WEBSITE_CHUNK_OVERLAP_ATTR, chunkOverlap.toString());
        }

        if (true) { // TODO
            websiteEl.setAttribute("extract-css-selector", "");
        }

        File config = getKnowledgeSourcesConfig(domainId);
        xmlService.save(doc, config);

        return uuid;
    }

    /**
     * Add third-party RAG knowledge source
     * @param payload Payload sent to endpoint, e.g. {"message":[{"content":"{{QUESTION}}","role":"user"}],"stream":false}
     * @return knowledge source Id
     */
    public String addThirdPartyRAG(String domainId, String name, String endpointUrl, String payload) throws Exception {
        log.info("Add third-party RAG as knowledge source to domain '" + domainId + "' ...");
        Document doc = getKnowledgeSourcesDocument(domainId);
        if (doc == null) {
            doc = xmlService.createDocument(KATIE_NAMESPACE_1_0_0, "knowledge-sources");
        }

        Element ksEl = doc.createElement(KNOWLEDGE_SOURCE_TAG);
        doc.getDocumentElement().appendChild(ksEl);
        String uuid = UUID.randomUUID().toString();
        ksEl.setAttribute(KNOWLEDGE_SOURCE_ID_ATTR, uuid);
        ksEl.setAttribute(KNOWLEDGE_SOURCE_ENABLED_ATTR, "false");
        ksEl.setAttribute(KNOWLEDGE_SOURCE_CONNECTOR_ATTR, KnowledgeSourceConnector.THIRD_PARTY_RAG.toString());

        Element nameEl = doc.createElement("name");
        ksEl.appendChild(nameEl);
        nameEl.setTextContent(name);

        Element thirdPartyRagEl = doc.createElement(THIRD_PARTY_RAG_TAG);
        ksEl.appendChild(thirdPartyRagEl);
        thirdPartyRagEl.setAttribute("url", endpointUrl);

        Element bodyEl = doc.createElement("body");
        thirdPartyRagEl.appendChild(bodyEl);
        bodyEl.setAttribute("content-type", "application/json"); // TODO: Depending on payload
        bodyEl.setTextContent(payload);

        File config = getKnowledgeSourcesConfig(domainId);
        xmlService.save(doc, config);

        return uuid;
    }

    /**
     * Get XML File containing knowledge sources of a particular domain
     */
    private File getKnowledgeSourcesConfig(String domainId) {
        return new File(contextsDataPath, domainId + "/knowledge-sources.xml");
    }

    /**
     * Get knowledge sources configuration as XML
     */
    private Document getKnowledgeSourcesDocument(String domainId) throws Exception {
        File config = getKnowledgeSourcesConfig(domainId);
        if (!config.isFile()) {
            log.warn("No domain knowledge sources configuration file exists: " + config.getAbsolutePath());
            return null;
        }
        Document doc = xmlService.read(config);
        doc.getDocumentElement().normalize();
        return doc;
    }
}
