package com.wyona.katie.services;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
public class MCPRetrievalService {

    @Tool(
            name = "katie_text_search",
            description = "Find relevant content by keyword"
    )
    public List<String> findRelevantContent(String keyword) {
        log.info("Finding relevant content for query: {}", keyword);

        return List.of(
                "Katharina was born October 18, 1896",
                "Michael was born February 16, 1969",
                "Result for keyword: " + keyword
        );
    }
}
