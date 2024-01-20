package com.wyona.katie.services;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.QnA;

import java.net.URI;

/**
 * Get QnAs from a web page, e.g. "https://www.myright.ch/en/legal-tips/corona-private/covid-certificatetrequirement" or "https://cwiki.apache.org/confluence/display/LUCENE/LuceneFAQ"
 */
public interface QnAsFromWebpageService {

    /**
     * @param url URL of web page containing QnAs, e.g. "https://cwiki.apache.org/confluence/display/LUCENE/LuceneFAQ"
     * @param domain Domain the extracted QnAs will be associated with
     * @return QnAs
     */
    public QnA[] getQnAs(URI url, Context domain);
}
