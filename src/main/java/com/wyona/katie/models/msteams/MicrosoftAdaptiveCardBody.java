package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Slf4j
public class MicrosoftAdaptiveCardBody {

    private List<MicrosoftAdaptiveCardBodyItem> items;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     * @param title Body title, e.g. "Better response"
     */
    public MicrosoftAdaptiveCardBody(String title) {
        items = new ArrayList<MicrosoftAdaptiveCardBodyItem>();

        MicrosoftAdaptiveCardTextBlock bodyTitle = new MicrosoftAdaptiveCardTextBlock(title);
        items.add(bodyTitle);
    }

    /**
     *
     * @param title
     * @param size
     * @param weight
     */
    public MicrosoftAdaptiveCardBody(String title, String size, String weight) {
        items = new ArrayList<MicrosoftAdaptiveCardBodyItem>();

        MicrosoftAdaptiveCardTextBlock bodyTitle = new MicrosoftAdaptiveCardTextBlock(title);
        bodyTitle.setSize(size);
        bodyTitle.setWeight(weight);
        items.add(bodyTitle);
    }

    /**
     *
     */
    public void addItem(MicrosoftAdaptiveCardBodyItem item) {
        items.add(item);
    }

    /**
     *
     */
    public MicrosoftAdaptiveCardBodyItem[] getItems() {
        return items.toArray(new MicrosoftAdaptiveCardBodyItem[0]);
    }

    /**
     *
     */
    public String getSpacing() {
        return "extraLarge";
    }

    /**
     *
     */
    public String getType() {
        return "Container";
    }

    /**
     *
     */
    public String getVerticalContentAlignment() {
        return "center";
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Body: TODO");
        return s.toString();
    }
}
