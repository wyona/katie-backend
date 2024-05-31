package com.wyona.katie.models;

/**
 * Additional context provided by user when user is asking a question
 */
public class QuestionContext {

    private Sentence question;
    private Sentence context;

    /**
     * @param question Question, e.g. "Can Weaviate do these sorts of things?"
     * @param context Context re question, e.g. "I'm hoping to feed it text from around 100 different speakers and determine things like: who sounds the most similar to who and what topics does each person talk about the most."
     */
    public QuestionContext(Sentence question, Sentence context) {
        this.question = question;
        this.context = context;
    }

    /**
     *
     */
    public Sentence getQuestion() {
        return question;
    }

    /**
     *
     */
    public Sentence getContext() {
        return context;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Q: " + getQuestion());
        sb.append(" ");
        if (getContext() != null && getContext().getSentence() != null && getContext().getSentence().length() > 0) {
            sb.append("C: " + getContext());
        } else {
            sb.append("C: NOT_AVAILABLE");
        }
        return sb.toString();
    }
}
