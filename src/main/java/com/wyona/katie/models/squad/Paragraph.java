package com.wyona.katie.models.squad;
  
import java.io.Serializable;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Paragraph implements Serializable {

    private String context;
    private ArrayList<QnAs> qas;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Paragraph() {
        qas = new ArrayList<QnAs>();
    }

    /**
     * @param context A short parapgraph about a particular topic ("Frédéric_Chopin"), e.g. "Frédéric François Chopin (/ˈʃoʊpæn/; French pronunciation: ​[fʁe.de.ʁik fʁɑ̃.swa ʃɔ.pɛ̃]; 22 February or 1 March 1810 – 17 October 1849), born Fryderyk Franciszek Chopin,[n 1] was a Polish and French (by citizenship and birth of father) composer and a virtuoso pianist of the Romantic era, who wrote primarily for the solo piano. He gained and has maintained renown worldwide as one of the leading musicians of his era, whose \"poetic genius was based on a professional technique that was without equal in his generation.\" Chopin was born in what was then the Duchy of Warsaw, and grew up in Warsaw, which after 1815 became part of Congress Poland. A child prodigy, he completed his musical education and composed his earlier works in Warsaw before leaving Poland at the age of 20, less than a month before the outbreak of the November 1830 Uprising."
     */
    public Paragraph(String context) {
        this.context = context;
        qas = new ArrayList<QnAs>();
    }

    /**
     *
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     *
     */
    public String getContext() {
        return context;
    }

    /**
     *
     */
    public QnAs[] getQas() {
        return qas.toArray(new QnAs[0]);
    }
}
