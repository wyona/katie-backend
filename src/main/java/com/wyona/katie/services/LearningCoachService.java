package com.wyona.katie.services;

import com.wyona.katie.models.Context;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Component
public class LearningCoachService {

    /**
     *
     */
    public String[] getConversationStarters(Context domain) {
        List<String> starters = new ArrayList<>();
        // TODO: Get from data repository
        starters.add("Erkläre mir wie man eine analoge Uhr liest!");
        starters.add("Lass uns spielerisch zusammen lernen, wie man eine analoge Uhr liest!");
        return starters.toArray(new String[0]);
    }
}
