package com.wyona.katie.models.insights;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain insights
 */
public class DomainInsights {

    private List<LanguagePageviews> faqPageviews;
    private int numberOfQnAs;
    private int numberOfReceivedMessages;
    private int numberOfAskedQuestions;
    private int numberOfAskedQuestionsWithoutAnswer;
    private int numberOfNextBestAnswer;
    private int numberOfAnsweredQuestions;
    private int numberOfQuestionsSentToExpert;

    private int numberOfPositiveFeedbacks;
    private int numberOfNegativeFeedbacks;

    private int numberOfPositiveFeedbacksRePredictedLabels;
    private int numberOfNegativeFeedbacksRePredictedLabels;

    private int numberOfApprovedAnswers;
    private int numberOfDiscardedAnswers;
    private int numberOfCorrectedAnswers;
    private int numberOfIgnoredAnswers;

    /**
     *
     */
    public DomainInsights() {
        this.faqPageviews = new ArrayList<LanguagePageviews>();
        this.numberOfQnAs = 0;
        this.numberOfAskedQuestions = 0;
        this.numberOfAskedQuestionsWithoutAnswer = 0;
        this.numberOfNextBestAnswer = 0;
        this.numberOfReceivedMessages = 0;
        this.numberOfAnsweredQuestions = 0;
        this.numberOfQuestionsSentToExpert = 0;

        this.numberOfPositiveFeedbacks = 0;
        this.numberOfNegativeFeedbacks = 0;

        this.numberOfPositiveFeedbacksRePredictedLabels = 0;
        this.numberOfNegativeFeedbacksRePredictedLabels = 0;

        this.numberOfApprovedAnswers = 0;
        this.numberOfDiscardedAnswers = 0;
        this.numberOfCorrectedAnswers = 0;
        this.numberOfIgnoredAnswers = 0;
    }

    /**
     * @param pageviews Number of pageviews
     */
    public void addFaqPageviews(String language, int pageviews) {
        faqPageviews.add(new LanguagePageviews(language, pageviews));
    }

    /**
     *
     */
    public LanguagePageviews[] getFaqPageviews() {
        return faqPageviews.toArray(new LanguagePageviews[0]);
    }

    /**
     *
     */
    public void setNumberOfNextBestAnswer(int numberOfNextBestAnswer) {
        this.numberOfNextBestAnswer = numberOfNextBestAnswer;
    }

    /**
     * Get number of next best answer
     */
    public int getNumberofnextbestanswer() {
        return numberOfNextBestAnswer;
    }

    /**
     *
     */
    public void setNumberOfQnAs(int numberOfQnAs) {
        this.numberOfQnAs = numberOfQnAs;
    }

    /**
     * Get total number of QnAs contained by a particular domain
     */
    public int getNumberofqnas() {
        return numberOfQnAs;
    }

    /**
     *
     */
    public void setNumberOfAskedQuestions(int numberOfAskedQuestions) {
        this.numberOfAskedQuestions = numberOfAskedQuestions;
    }

    /**
     * Get number of unanswered questions
     */
    public int getNumberofaskedquestionswithoutanswer() { // INFO: No CamelCase for method name, because freemarker does not work properly with CamelCase method names
        return numberOfAskedQuestionsWithoutAnswer;
    }

    /**
     * Set number of unanswered questions
     */
    public void setNumberOfAskedQuestionsWithoutAnswer(int numberOfAskedQuestionsWithoutAnswer) {
        this.numberOfAskedQuestionsWithoutAnswer = numberOfAskedQuestionsWithoutAnswer;
    }

    /**
     * Get number of asked questions
     */
    public int getNumberofaskedquestions() { // INFO: No CamelCase for method name, because freemarker does not work properly with CamelCase method names
        return numberOfAskedQuestions;
    }

    /**
     *
     */
    public void setNumberOfReceivedMessages(int numberOfReceivedMessages) {
        this.numberOfReceivedMessages = numberOfReceivedMessages;
    }

    /**
     *
     */
    public int getNumberOfReceivedMessages() {
        return numberOfReceivedMessages;
    }

    /**
     *
     */
    public void setNumberOfAnsweredQuestions(int numberOfAnsweredQuestions) {
        this.numberOfAnsweredQuestions = numberOfAnsweredQuestions;
    }

    /**
     *
     */
    public int getNumberOfAnsweredQuestions() {
        return numberOfAnsweredQuestions;
    }

    /**
     * Set number of questions sent to expert
     */
    public void setNumberOfQuestionsSentToExpert(int numberOfQuestionsSentToExpert) {
        this.numberOfQuestionsSentToExpert = numberOfQuestionsSentToExpert;
    }

    /**
     * Get number of questions sent to expert
     */
    public int getNumberOfQuestionsSentToExpert() {
        return numberOfQuestionsSentToExpert;
    }

    /**
     *
     */
    public void setNumberOfPositiveFeedbacks(int numberOfPositiveFeedbacks) {
        this.numberOfPositiveFeedbacks = numberOfPositiveFeedbacks;
    }

    /**
     * Get number of positive feedbacks re answers
     */
    public int getNumberOfPositiveFeedbacks() {
        return numberOfPositiveFeedbacks;
    }

    /**
     *
     */
    public void setNumberOfNegativeFeedbacks(int numberOfNegativeFeedbacks) {
        this.numberOfNegativeFeedbacks = numberOfNegativeFeedbacks;
    }

    /**
     * Get number of negative feedbacks re answers
     */
    public int getNumberOfNegativeFeedbacks() {
        return numberOfNegativeFeedbacks;
    }

    /**
     *
     */
    public void setNumberOfPositiveFeedbacksRePredictedLabels(int numberOfPositiveFeedbacksRePredictedLabels) {
        this.numberOfPositiveFeedbacksRePredictedLabels = numberOfPositiveFeedbacksRePredictedLabels;
    }

    /**
     *
     */
    public int getNumberOfPositiveFeedbacksRePredictedLabels() {
        return numberOfPositiveFeedbacksRePredictedLabels;
    }

    /**
     *
     */
    public void setNumberOfNegativeFeedbacksRePredictedLabels(int numberOfNegativeFeedbacksRePredictedLabels) {
        this.numberOfNegativeFeedbacksRePredictedLabels = numberOfNegativeFeedbacksRePredictedLabels;
    }

    /**
     *
     */
    public int getNumberOfNegativeFeedbacksRePredictedLabels() {
        return numberOfNegativeFeedbacksRePredictedLabels;
    }

    /**
     *
     */
    public void setNumberOfApprovedAnswers(int number) {
        this.numberOfApprovedAnswers = number;
    }

    /**
     *
     */
    public int getNumberOfApprovedAnswers() {
        return numberOfApprovedAnswers;
    }

    /**
     *
     */
    public void setNumberOfDiscardedAnswers(int number) {
        this.numberOfDiscardedAnswers = number;
    }

    /**
     *
     */
    public int getNumberOfDiscardedAnswers() {
        return numberOfDiscardedAnswers;
    }

    /**
     *
     */
    public void setNumberOfCorrectedAnswers(int number) {
        this.numberOfCorrectedAnswers = number;
    }

    /**
     *
     */
    public int getNumberOfCorrectedAnswers() {
        return numberOfCorrectedAnswers;
    }

    /**
     *
     */
    public void setNumberOfIgnoredAnswers(int number) {
        this.numberOfIgnoredAnswers = number;
    }

    /**
     *
     */
    public int getNumberOfIgnoredAnswers() {
        return numberOfIgnoredAnswers;
    }
}
