package com.wyona.katie;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 *
 */
@Configuration
@SpringBootApplication
@Slf4j
public class Server extends SpringBootServletInitializer {

    @Value("${discord.bot.token}")
    private String discordBotToken;

    @Value("${discord.enabled}")
    private boolean discordEnabled;

    @Value("${http.proxy.enabled}")
    private Boolean httpProxyEnabled;

    @Value("${http.proxy.host}")
    private String httpProxyHost;
    @Value("${http.proxy.port}")
    private String httpProxyPort;

    @Value("${https.proxy.host}")
    private String httpsProxyHost;
    @Value("${https.proxy.port}")
    private String httpsProxyPort;

    @Value("${http.non.proxy.hosts}")
    private String httpNonProxyHosts;

    @Value("${http.proxy.user}")
    private String httpProxyUser;
    @Value("${http.proxy.password}")
    private String httpProxyPassword;

    @Value("${https.proxy.user}")
    private String httpsProxyUser;
    @Value("${https.proxy.password}")
    private String httpsProxyPassword;

    /**
     *
     */
    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    /**
     *
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Server.class);
    }

    @PostConstruct
    public void setOutgoingProxy() {
        if (httpProxyEnabled) {
            // INFO: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/doc-files/net-properties.html
            log.info("Set outgoing proxy ...");

            System.setProperty("http.proxyHost", httpProxyHost);
            System.setProperty("http.proxyPort", httpProxyPort);
            System.setProperty("https.proxyHost", httpsProxyHost);
            System.setProperty("https.proxyPort", httpsProxyPort);

            // INFO: Default: localhost|127.*|[::1]
            System.setProperty("http.nonProxyHosts", httpNonProxyHosts);

            System.setProperty("http.proxyUser", httpProxyUser);
            System.setProperty("http.proxyPassword", httpProxyPassword);
            System.setProperty("https.proxyUser", httpsProxyUser);
            System.setProperty("https.proxyPassword", httpsProxyPassword);

            //log.info("Reactivate Basic by setting disabledSchemes to an empty string ...");
            // INFO: https://stackoverflow.com/questions/41505219/unable-to-tunnel-through-proxy-proxy-returns-http-1-1-407-via-https
            // INFO: /Library/Java/JavaVirtualMachines/jdk-11.0.11.jdk/Contents/Home/conf/net.properties
            //System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
            //System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        } else {
            log.info("No outgoing proxy set.");
        }
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        try {
            if (discordEnabled) {
                log.info("Try to init Gateway Discord client ...");
                return DiscordClientBuilder.create(discordBotToken).build()
                        .gateway()
                        .setInitialPresence(ignore -> ClientPresence.online(ClientActivity.listening("'everything'")))
                        .login()
                        .block();
            } else {
                log.info("Discord initialization disabled (see application property 'discord.enabled').");
                return null;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Bean
    public RestClient discordRestClient(Optional<GatewayDiscordClient> client) {
        log.info("Get Discord REST client ....");
        if (!client.isEmpty()) {
            return client.get().getRestClient();
        } else {
            if (discordEnabled) {
                log.error("No Gateway Discord client available!");
            } else {
                log.info("Discord initialization disabled (see application property 'discord.enabled'), therefore no Discord REST client.");
            }
            return null;
        }
    }
}
