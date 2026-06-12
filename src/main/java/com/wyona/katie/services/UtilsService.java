package com.wyona.katie.services;

import com.wyona.katie.models.ContentType;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.FloatVector;
import com.wyona.katie.models.URLMeta;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.Date;

@Slf4j
@Component
public class UtilsService {

    @Value("${http.proxy.enabled}")
    private Boolean proxyEnabled;

    @Value("${https.proxy.user}")
    private String proxyUser;

    @Value("${https.proxy.password}")
    private String proxyPassword;

    @Autowired
    XMLService xmlService;

    /**
     * Get length of vector
     */
    public static float getVectorLength(float[] vector) {
        float length = 0;
        for (int i = 0; i < vector.length; i++) {
            length += vector[i] * vector[i];
        }
        return (float) Math.sqrt(length);
    }

    /**
     *
     */
    public static float getDotProduct(float[] vectorOne, float[] vectorTwo) throws Exception {
        if (vectorOne.length !=  vectorTwo.length) {
            throw new Exception("Vectors do not have the size!");
        }
        float dotProduct = 0;
        for (int i = 0; i < vectorOne.length; i++) {
            dotProduct += vectorOne[i] * vectorTwo[i];
        }

        return dotProduct;
    }

    /**
     * https://en.wikipedia.org/wiki/Centroid#Determination
     * https://en.wikipedia.org/wiki/Center_of_mass
     */
    public static FloatVector getCentroid(FloatVector[] vectors) {
        FloatVector centroid = new FloatVector(vectors[0].getDimension());
        for (FloatVector vector : vectors) {
            centroid.add(vector);
            //log.debug("Centroid: " + centroid);
        }
        centroid.scale((float)(1.0/vectors.length));

        return centroid;
    }

    /**
     * Dump content referenced by URL
     * @param domain Katie domain for which content will be indexed
     * @param url Content URL, e.g. "https://graph.microsoft.com/v1.0/users/michael.wechner@wyona.com/onenote/pages/0-a84876c902edd24ab110904594185481!1-8D3F909A0DAE592D!647"
     * @param apiToken Optional API token
     */
    public void dumpContent(Context domain, URI url, String webUrl, String mimeType, String apiToken) throws Exception {
        log.info("Dump page content referenced by URL: " + url);

        File file = domain.getUrlDumpFile(url);
        if (!file.getParentFile().isDirectory()) {
            file.getParentFile().mkdirs();
        }

        boolean dump = true;
        if (file.isFile()) {
            log.info("TODO: Check last modified, whether URL content has changed since last dump.");
            if (true) { // INFO: Content has changed since last sync
                dump = true;
            } else {
                dump = false;
            }
        }

        if (dump) {
            log.info("TODO: Limit size of downloaded data.");
            log.info("Download data referenced by URL '" + url + "' and save to '" + file.getAbsolutePath() + "' ...");

            // INFO: Proxy is set automatically, but not proxy authentication
            if (proxyEnabled) {
                // INFO: Configure proxy authentication
                log.info("Configure proxy authentication ...");
                Authenticator authenticator = new Authenticator() {
                    public PasswordAuthentication getPasswordAuthentication() {
                        return (new PasswordAuthentication(proxyUser, proxyPassword.toCharArray()));
                    }
                };
                Authenticator.setDefault(authenticator);
            }
            HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
            if (apiToken != null) {
                connection.setRequestProperty("Authorization", "Bearer " + apiToken);
            }

            ContentType contentType = null;
            if (mimeType != null) {
                try {
                    contentType = ContentType.fromString(mimeType);
                } catch (Exception e) {
                    log.warn("Content type '" + mimeType + "' not supported yet by Katie");
                }
            } else {
                String contentTypeFromResponse = connection.getContentType();
                try {
                    contentType = ContentType.fromString(contentTypeFromResponse);
                } catch (Exception e) {
                    log.warn("Content type '" + contentTypeFromResponse + "' not supported yet by Katie");
                }
            }
            log.info("Content type: " + contentType.toString());

            InputStream in = connection.getInputStream();
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            out.close();
            in.close();
            connection.disconnect();

            saveMetaInformation(url.toString(), webUrl, new Date(), contentType, domain);
        } else {
            log.info("URL '" + url + "' already dumped.");
        }

        // INFO: https://github.com/apache/commons-io/blob/master/src/main/java/org/apache/commons/io/FileUtils.java
        //FileUtils.copyURLToFile(new URI(page.getContentURL()).toURL(), file, 1000, 3000);
    }

    /**
     * Save meta information re extraction of text / QnAs from URL
     * @param contentUrl Content URL, e.g. "https://graph.microsoft.com/v1.0/groups/c5a3125f-f85a-472a-8561-db2cf74396ea/onenote/pages/1-fd1e338afe640a3219c58b850ad3c4f6!1-5aaade12-a1fc-478c-b98c-1f888fed25a0/content"
     * @param webUrl Web URL, e.g. "https://szhglobal.sharepoint.com/sites/MSGR-00000778/Shared%20Documents/General/WIKI%20Energieberatung?wd=target%28F%C3%B6rderprogramme.one%7Cfb8f3fb7-e89b-4d08-b9f2-b52248c15f1e%2FFAQ%20F%C3%B6rderprogramme%7C9d034704-bbf1-43f6-8208-e5a29c649b04%2F%29"
     * @param date Date when text / QnAs got extracted
     */
    protected URLMeta saveMetaInformation(String contentUrl, String webUrl, Date date, ContentType contentType, Context domain) {
        File metaFile = domain.getUrlMetaFile(URI.create(contentUrl));
        if (!metaFile.getParentFile().isDirectory()) {
            metaFile.getParentFile().mkdirs();
        }
        // TODO: Add file where content was dumped, e.g. "/Users/USERNAME/src/katie-backend/volume/contexts/946c2355-6def-4fad-8bb1-b512a5f58a4f/urls/graph.microsoft.com/v1.0/users/michael.wechner@wyona.com/onenote/pages/0-a84876c902edd24ab110904594185481!1-8D3F909A0DAE592D!647/content/data"
        return xmlService.createUrlMeta(metaFile, contentUrl, webUrl, date.getTime(), contentType);
    }

    /**
     * Pre-process question
     * @param question Original question
     * @return pre-processed question
     */
    public String preProcessQuestion(String question) {
        question = Utils.replaceNewLines(question, " ");

        // INFO: Replace question mark by space
        //question = question.replace("?", " "); // TODO: Why do we replace the question mark by a space?!

        if (question != null) {
            question = question.trim();
        }

        return question;
    }
}
