package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class QuestionSentence extends Sentence {
	final private LinkedList<Word> words;
	

	public QuestionSentence(String sentence, ArrayList<Entity> entities, List<String> classifications, LinkedList<Word> words) {
		super(sentence, entities, classifications);
		this.words = words;
	}


	public LinkedList<Word> getWords() {
		return words;
	}
}
