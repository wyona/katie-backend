package com.wyona.katie.models;

import java.util.LinkedList;

/**
 *
 */
public class QuestionClassification {
	final private Sentence context;
	final private LinkedList<Entity> contextNamedEntities;
	final private boolean isQuestion;
	final private LinkedList<QuestionSentence> questions;

	/**
	 *
	 * @param context Text containing questions and context what the questions are about
	 * @param contextNamedEntities
	 * @param isQuestion True when text contains question(s) and false otherwise
	 * @param questions
	 */
    public QuestionClassification(Sentence context, LinkedList<Entity> contextNamedEntities, boolean isQuestion, LinkedList<QuestionSentence> questions) {
    	super();
		this.context = context;
		this.contextNamedEntities = contextNamedEntities;
		this.isQuestion = isQuestion;
		this.questions = questions;
	}

	/**
	 * @return text
	 */
	public Sentence getContext() {
		return context;
	}

	public LinkedList<Entity> getContextNamedEntities() {
		return contextNamedEntities;
	}

	/**
	 * @return true when text contains question(s)
	 */
	public boolean isQuestion() {
		return isQuestion;
	}

	/**
	 * @return all questions contained by text
	 */
	public LinkedList<QuestionSentence> getQuestions() {
		return questions;
	}
    
    
}
