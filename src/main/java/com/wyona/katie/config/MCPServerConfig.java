package com.wyona.katie.config;

import com.wyona.katie.services.MCPRetrievalService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MCPServerConfig {

    @Bean
    public List<ToolCallback> findTools(MCPRetrievalService retrievalService) {
        return List.of(ToolCallbacks.from(retrievalService));
    }
}
