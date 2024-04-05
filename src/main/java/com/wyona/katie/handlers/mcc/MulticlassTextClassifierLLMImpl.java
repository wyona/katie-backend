package com.wyona.katie.handlers.mcc;

import com.wyona.katie.models.Context;
import com.wyona.katie.models.HitLabel;
import com.wyona.katie.models.TextSample;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Use LLM respectively a corresponding LLM prompt to classify text
 */
@Slf4j
@Component
public class MulticlassTextClassifierLLMImpl implements MulticlassTextClassifier {

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#predictLabels(Context, String, int)
     */
    public HitLabel[] predictLabels(Context domain, String text, int limit) throws Exception {
        List<HitLabel> hitLabels = new ArrayList<>();
        return hitLabels.toArray(new HitLabel[0]);
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#train(Context, TextSample[])
     */
    public void train(Context domain, TextSample[] samples) throws Exception {
        log.warn("TODO: Implement train method.");
    }

    /**
     * @see com.wyona.katie.handlers.mcc.MulticlassTextClassifier#retrain(Context, String)
     */
    public void retrain(Context domain, String bgProcessId) throws Exception {
        log.info("TODO: Retrain method");
    }
}
