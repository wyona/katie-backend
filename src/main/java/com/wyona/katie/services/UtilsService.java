package com.wyona.katie.services;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.FloatVector;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;

@Slf4j
@Component
public class UtilsService {

    @Value("${http.proxy.enabled}")
    private Boolean proxyEnabled;

    @Value("${https.proxy.user}")
    private String proxyUser;

    @Value("${https.proxy.password}")
    private String proxyPassword;

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
        centroid.scale(new Float(1.0/vectors.length).floatValue());

        return centroid;
    }

    /**
     * Dump content referenced by URL
     * @param domain Katie domain for which content will be indexed
     * @param url Content URL, e.g. "https://graph.microsoft.com/v1.0/users/michael.wechner@wyona.com/onenote/pages/0-a84876c902edd24ab110904594185481!1-8D3F909A0DAE592D!647"
     * @param apiToken Optional API token
     * @return file where content was dumped, e.g. "/Users/michaelwechner/src/wyona/wyona/katie-backend/volume/contexts/946c2355-6def-4fad-8bb1-b512a5f58a4f/urls/graph.microsoft.com/v1.0/users/michael.wechner@wyona.com/onenote/pages/0-a84876c902edd24ab110904594185481!1-8D3F909A0DAE592D!647/content/data"
     */
    public File dumpContent(Context domain, URI url, String apiToken) throws Exception {
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

            InputStream in = connection.getInputStream();
            OutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);
            out.close();
            in.close();
            connection.disconnect();
        } else {
            log.info("URL '" + url + "' already dumped.");
        }

        // INFO: https://github.com/apache/commons-io/blob/master/src/main/java/org/apache/commons/io/FileUtils.java
        //FileUtils.copyURLToFile(new URI(page.getContentURL()).toURL(), file, 1000, 3000);

        return file;
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
