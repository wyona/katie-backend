package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object to a request to get predicted labels / classifications
 */
@Slf4j
public class PredictedLabelsResponse {

    private String uuid;
    private List<HitLabel> predictedLabels = new ArrayList<>();
    private ClassificationImpl classificationImpl;

    private String topDeskHtml;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public PredictedLabelsResponse() {
    }

    /**
     *
     */
    public void setRequestUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     *
     */
    public String getRequestUuid() {
        return uuid;
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
     *
     */
    public void setPredictedLabelsAsTopDeskHtml(String topDeskHtml) {
        this.topDeskHtml = topDeskHtml;
    }

    /**
     * WARNING: Only temporarily!
     */
    public String getPredictedLabelsAsTopDeskHtml() {
        return topDeskHtml;
    }
}
