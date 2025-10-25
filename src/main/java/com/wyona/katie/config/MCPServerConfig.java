package com.wyona.katie.config;

import com.wyona.katie.services.MCPRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class MCPServerConfig {

    /*
    @Bean
    public List<ToolCallback> retrievalTools(MCPRetrievalService retrievalService) {
        return List.of(ToolCallbacks.from(retrievalService));
    }
    */

    @Bean
    ToolCallbackProvider retrievalTools(MCPRetrievalService retrievalService) {
        log.info("Get all MCP retrieval tools ...");
        return MethodToolCallbackProvider.builder()
                .toolObjects(retrievalService)
                .build();
    }
}
