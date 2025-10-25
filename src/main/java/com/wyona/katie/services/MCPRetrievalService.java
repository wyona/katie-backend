package com.wyona.katie.services;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MCPRetrievalService {

    @Tool(name = "katie_text_search", description = "Find relevant content")
    public List<String> findAll() {
        log.info("Finding relevant content ...");
        List<String> relevantContent = new ArrayList<>();
        relevantContent.add("Katharina was born October 18, 1896");
        relevantContent.add("Michael was born February 16, 1969");
        return relevantContent;
    }
}
