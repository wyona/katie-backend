package com.wyona.katie.models;

public final class Word {
	final private String word;
	final private String feats;
	final private String upos;
	final private String xpos;
	
	/**
	 * 
	 * @param word
	 * @param feats
	 * @param upos
	 * @param xpos
	 */
	public Word(String word, String feats, String upos, String xpos) {
		super();
		this.word = word;
		this.feats = feats;
		this.upos = upos;
		this.xpos = xpos;
	}
	
	/**
	 * ex. "What"
	 * @return
	 */
	public String getWord() {
		return word;
	}
	/**
	 * ex. "Definite=Def|PronType=Art"
	 * @return
	 */
	public String getFeats() {
		return feats;
	}
	/**
	 * ex. "DET"
	 * @return
	 */
	public String getUpos() {
		return upos;
	}
	/**
	 * ex. "DT"
	 * @return
	 */
	public String getXpos() {
		return xpos;
	}
}
