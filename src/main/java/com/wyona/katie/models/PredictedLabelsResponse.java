package com.wyona.katie.models;

import com.wyona.katie.services.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object to a request to get predicted labels / classifications
 */
@Slf4j
public class PredictedLabelsResponse {

    private List<HitLabel> predictedLabels = new ArrayList<>();
    private ClassificationImpl classificationImpl;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public PredictedLabelsResponse() {
    }

    /**
     *
     */
    public void setClassificationImpl(ClassificationImpl classificationImpl) {
        this.classificationImpl = classificationImpl;
    }

    /**
     *
     */
    public ClassificationImpl getClassificationImpl() {
        return classificationImpl;
    }

    /**
     *
     */
    public void setPredictedLabels(HitLabel[] predictedLabels) {
        for (HitLabel hitLabel : predictedLabels) {
            this.predictedLabels.add(hitLabel);
        }
    }

    /**
     * Get predicted labels / classifications, based on submitted question / message
     */
    public HitLabel[] getPredictedLabels() {
        return predictedLabels.toArray(new HitLabel[0]);
    }

    /**
     * WARNING: Only temporarily!
     */
    public String getPredictedLabelsAsTopDeskHtml() {
        //ContentType.TEXT_TOPDESK_HTML
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for (HitLabel hitLabel : predictedLabels) {
            sb.append("<li>" + hitLabel.getLabel().getTerm() + " (Score: " + hitLabel.getScore() + ")</li>");
        }
        sb.append("</ul>");
        return Utils.convertHtmlToTOPdeskHtml(sb.toString());
    }
}