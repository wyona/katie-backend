package com.wyona.katie.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

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
        this.restTemplate = new RestTemplate();

        if (proxyEnabled) {
            log.info("Set proxy settings ...");

            final int proxyPortNum = Integer.parseInt(proxyPort);
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(proxyHost, proxyPortNum), new UsernamePasswordCredentials(proxyUser, proxyPassword));

            final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
            clientBuilder.useSystemProperties();
            clientBuilder.setProxy(new HttpHost(proxyHost, proxyPortNum));
            clientBuilder.setDefaultCredentialsProvider(credsProvider);
            clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
            final CloseableHttpClient client = clientBuilder.build();

            final HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setHttpClient(client);

            restTemplate.setRequestFactory(factory);
        } else {
            log.info("Proxy not enabled.");
        }
    }

    /**
     * Get RestTemplate
     * @return
     */
    public RestTemplate getRestTemplate() {
        log.info("Get custom RestTemplate ...");
        return restTemplate;
    }
}
