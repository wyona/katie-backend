package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Slf4j
public class MicrosoftAdaptiveCard {

    private String type;
    private List<MicrosoftAdaptiveCardBody> body;
    private List<MicrosoftAdaptiveCardAction> actions;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     * @param title Card title, e.g. "Answer helpful?"
     */
    public MicrosoftAdaptiveCard(String title) {
        this.type = "AdaptiveCard";

        body = new ArrayList<MicrosoftAdaptiveCardBody>();
        body.add(new MicrosoftAdaptiveCardBody(title, "large", "bolder"));

        actions = new ArrayList<MicrosoftAdaptiveCardAction>();
    }

    /**
     *
     */
    public void addAction(MicrosoftAdaptiveCardAction action) {
        actions.add(action);
    }

    /**
     *
     */
    public MicrosoftAdaptiveCardAction[] getActions() {
        return actions.toArray(new MicrosoftAdaptiveCardAction[0]);
    }

    /**
     *
     */
    public void addBody(MicrosoftAdaptiveCardBody b) {
        this.body.add(b);
    }

    /**
     *
     */
    public MicrosoftAdaptiveCardBody[] getBody() {
        return body.toArray(new MicrosoftAdaptiveCardBody[0]);
    }

    /**
     *
     */
    public String getType() {
        return type;
    }

    /**
     *
     */
    public String get$schema() {
        return "http://adaptivecards.io/schemas/adaptive-card.json";
    }

    /**
     *
     */
    public String getVersion() {
        return "1.4";
    }
}
