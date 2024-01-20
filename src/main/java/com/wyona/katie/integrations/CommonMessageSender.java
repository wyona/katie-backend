package com.wyona.katie.integrations;

import com.wyona.katie.models.ResponseAnswer;

import com.wyona.katie.services.Utils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 *
 */
@Slf4j
@Component
public class CommonMessageSender {

    @Autowired
    private ResourceBundleMessageSource messageSource;

    /**
     * Get answer including meta information as XHTML
     * @param answer Answer
     * @param originalQuestion Original question to answer
     * @param originalQuestionDate Date when original question was asked
     * @param sourceUrl Source URL, e.g. "https://en.wikipedia.org/wiki?curid=25936"
     * @param sourceSeparator Spacing between content and source, e.g. "<br/>"
     * @param metaSeparator Horizontal separator between content/source and meta information, e.g. "<hr/>"
     */
    protected String getAnswerInclMetaInformation(String answer, String originalQuestion, long originalQuestionDate, Locale locale, String sourceUrl, String sourceSeparator, String metaSeparator) {
        StringBuilder answerInclMetaInfo = new StringBuilder(answer);

        // INFO: Add source information
        if (sourceUrl != null && sourceUrl.length() > 0) {
            answerInclMetaInfo.append(sourceSeparator + "<p>" + messageSource.getMessage("source", null, locale) + ": ");
            if (sourceUrl.startsWith("http")) {
                answerInclMetaInfo.append("<a href=\"" + sourceUrl + "\">" + sourceUrl + "</a>");
            } else {
                answerInclMetaInfo.append(sourceUrl);
            }
            answerInclMetaInfo.append("</p>");

        }

        // INFO: Add meta information
        if (originalQuestion != null) {
            answerInclMetaInfo.append(metaSeparator + getMetaInfo(originalQuestion, originalQuestionDate, locale));
        } else {
            log.info("No original question available.");
        }

        return answerInclMetaInfo.toString();
    }

    /**
     * Get meta information, e.g. original question, date when original question was answered
     * @return meta information as XHTML
     */
    private String getMetaInfo(String originalQuestion, long originalQuestionDate, Locale locale) {

        StringBuilder metaInfo = new StringBuilder("<p>");
        metaInfo.append(messageSource.getMessage("original.question", null, locale) + ": <strong>" + originalQuestion + "</strong>");

        metaInfo.append("<br/>");
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date oqDate = new Date(originalQuestionDate);
        metaInfo.append(messageSource.getMessage("answered.date", null, locale) + ": <strong>" + dateFormat.format(oqDate) + "</strong>");

        metaInfo.append("</p>");
        return metaInfo.toString();
    }
}
