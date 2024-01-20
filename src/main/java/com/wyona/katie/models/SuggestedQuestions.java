package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

public class SuggestedQuestions {

    private List<Question> questions;

    /**
     *
     */
    public SuggestedQuestions() {
        questions = new ArrayList<Question>();
    }

    /**
     *
     */
    public void addQuestion(String question) {
        Question q = new Question();
        q.setQuestion(question);
        questions.add(q);
    }

    /**
     *
     */
    public Question[] getQuestions() {
        return questions.toArray(new Question[0]);
    }
}
