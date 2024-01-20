package com.wyona.katie.models.squad;
  
import java.io.Serializable;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Topic implements Serializable {

    private String title;
    private ArrayList<Paragraph> paragraphs;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Topic() {
        paragraphs = new ArrayList<Paragraph>();
    }

    /**
     * @param title Title of topic, e.g. "Frédéric_Chopin"
     */
    public Topic(String title) { 
        this.title = title;
        paragraphs = new ArrayList<Paragraph>();
    }

    /**
     *
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     */
    public Paragraph[] getParagraphs() {
        return paragraphs.toArray(new Paragraph[0]);
    }
}
