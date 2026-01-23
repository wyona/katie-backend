package com.wyona.katie.services;

import com.wyona.katie.answers.OpenERZ;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
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

    /**
     * Get the dates for the city of Zurich’s paper and cardboard collection.
     * @param zipCode Swiss ZIP code within the city of Zurich (e.g. 8044, 8003, 8032).
     * @return A list of upcoming paper and cardboard collection dates
     */
    @Tool(
            name = "katie_paper_cardboard_collection_city_of_zurich",
            description = "Get the dates for the city of Zurich’s paper and cardboard collection."
    )
    public List<String> getPapierKartonSammlungDatesCityOfZurich(
            @ToolParam(description = "Swiss ZIP code within the city of Zurich (e.g. 8044, 8003, 8032).", required = true) Integer zipCode,
            @ToolParam(description = "Type of waste, either 'cardboard' or 'paper'", required = true) String wasteType
    ) {
        log.info("Get the paper and cardboard collection dates for ZIP code " + zipCode + " in the city of Zurich.");
        OpenERZ openERZ = new OpenERZ();
        List<Date> dates = openERZ.getDates(zipCode.toString(), wasteType);
        List<String> datesAsString = new ArrayList<>();
        for (Date date : dates) {
            datesAsString.add(date.toString());
        }

        return datesAsString;
    }
}

