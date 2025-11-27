package com.wyona.katie.services;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
public class MCPRetrievalService {

    @Tool(
            name = "katie_text_search",
            description = "Find relevant content by natural language query"
    )
    public List<String> findRelevantContent(
            @ToolParam(description = "The question to search for", required = true) String question
            //@ToolParam(description = "The Katie knowledge base Id", required = false) String domainId
    ) {
        String domainId = "TODO";
        log.info("Finding relevant content for question '" + question + "' inside domain '" + domainId + "' ...");

        return List.of(
                "Katherina was born October 18, 1896",
                "Michael was born February 16, 1969",
                "Result for question: " + question
        );
    }
}

