package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/*
{
  "mentioned":{
    "id":"28:a888d256-6f0e-4358-b23e-9b644fe0fd64",
    "name":"katie"
  },
  "text":"<at>katie</at>",
  "type":"mention"
}

or

{
  "locale":"de-CH",
  "country":"CH",
  "platform":"Web",
  "timezone":"Europe/Zurich",
  "type":"clientInfo"
}
 */

/**
 *
 */
@Slf4j
public class MicrosoftEntity {

    private String type;

    private String text;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftEntity() {
    }

    /**
     *
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return type, e.g. "mention" or "clientInfo"
     */
    public String getType() {
        return type;
    }

    /**
     *
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     *
     */
    public String getText() {
        return text;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "Type: " + type + ", Text: " + text;
    }
}
