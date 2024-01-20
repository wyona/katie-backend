package com.wyona.katie.integrations.discord;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class GlobalCommandRegistrar implements ApplicationRunner {

    private final Optional<RestClient> client;

    //Use the rest client provided by our Bean
    public GlobalCommandRegistrar(Optional<RestClient> client) {
        this.client = client;
    }

    //This method will run only once on each start up and is automatically called with Spring so blocking is okay.
    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!client.isEmpty()) {
            //Create an ObjectMapper that supported Discord4J classes
            final JacksonResources d4jMapper = JacksonResources.create();

            // Convenience variables for the sake of easier to read code below.
            PathMatchingResourcePatternResolver matcher = new PathMatchingResourcePatternResolver();
            final ApplicationService applicationService = client.get().getApplicationService();
            final long applicationId = client.get().getApplicationId().block();

            //Get our commands json from resources as command data
            List<ApplicationCommandRequest> commands = new ArrayList<>();
            for (Resource resource : matcher.getResources("discord/commands/*.json")) {
                ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                        .readValue(resource.getInputStream(), ApplicationCommandRequest.class);

                log.info("Register Katie Discord command: " + request.name());
                commands.add(request);
            }

        /* Bulk overwrite commands. This is now idempotent, so it is safe to use this even when only 1 command
        is changed/added/removed
        */
            applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
                    .doOnNext(ignore -> log.info("Successfully registered global Discord commands"))
                    .doOnError(e -> log.error("Failed to register global Discord commands", e))
                    .subscribe();
        } else {
            log.warn("No Discord REST client available.");
        }
    }
}

