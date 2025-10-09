package com.wyona.katie.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RestProxyTemplate {

    private RestTemplate restTemplate;

    @Value("${http.proxy.enabled}")
    private Boolean proxyEnabled;

    @Value("${https.proxy.host}")
    private String proxyHost;

    @Value("${https.proxy.port}")
    private String proxyPort;

    @Value("${https.proxy.user}")
    private String proxyUser;

    @Value("${https.proxy.password}")
    private String proxyPassword;

    @PostConstruct
    public void init() {
        if (proxyEnabled) {
            log.info("Set proxy settings ...");

            int proxyPortNum = Integer.parseInt(proxyPort);

            // Setup credentials
            BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(proxyHost, proxyPortNum),
                    new UsernamePasswordCredentials(proxyUser, proxyPassword.toCharArray())
            );

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(5, TimeUnit.SECONDS)
                    .setResponseTimeout(5, TimeUnit.SECONDS)
                    .build();

            // Setup HttpClient 5 with proxy
            CloseableHttpClient client = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .setDefaultRequestConfig(config)
                    .setProxy(new HttpHost(proxyHost, proxyPortNum))
                    .build();

            // Setup Spring RestTemplate factory
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);

            restTemplate = new RestTemplate(factory);
        } else {
            log.info("Proxy not enabled.");
            restTemplate = new RestTemplate();
        }
    }

    /**
     * Get RestTemplate connecting through a proxy when a required proxy is configured
     */
    public RestTemplate getRestTemplate() {
        log.info("Get custom RestTemplate ...");
        return restTemplate;
    }
}