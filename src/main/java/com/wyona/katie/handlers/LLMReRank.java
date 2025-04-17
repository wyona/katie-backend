package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import com.wyona.katie.services.GenerativeAIService;
import com.wyona.katie.services.Utils;
import com.wyona.katie.models.CompletionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;

/**
 *
 */
@Slf4j
@Component
public class LLMReRank implements ReRankProvider {

    @Autowired
    GenerativeAIService generativeAIService;

    @Value("${re_rank.llm.temperature}")
    private Double temperature;

    @Value("${re_rank.llm.impl}")
    private CompletionImpl completionImpl;

    /**
     * @see ReRankProvider#getReRankedAnswers(String, String[], int, com.wyona.katie.models.Context)
     */
    public Integer[] getReRankedAnswers(String question, String[] answers, int limit, Context domain) {
        log.info("Re-rank answers using a LLM (" + domain.getCompletionConfig().getModel() + ")...");

        List<Integer> reRankedIndex = new ArrayList<Integer>();

        int item_number_none_of_the_answers = answers.length + 1;
        List<PromptMessage> promptMessages = new ArrayList<>();
        promptMessages.add(new PromptMessage(PromptMessageRole.USER, getMultipleChoicePrompt(question, answers, item_number_none_of_the_answers)));
        log.info("Prompt: " + promptMessages.get(0).getContent());
        try {
            String completedText = null;
            GenerateProvider generateProvider = generativeAIService.getGenAIImplementation(completionImpl);
            if (generateProvider != null) {
                // TODO: Use response_format json, see for example https://platform.openai.com/docs/guides/structured-outputs
                completedText = generateProvider.getCompletion(promptMessages,null, domain.getCompletionConfig(), temperature).getText();
            } else {
                log.error("Completion provider '" + completionImpl + "' not implemented yet!");
                return reRankedIndex.toArray(new Integer[0]);
            }

            //String completedText = "The best answer is (1) or (2), as they both provide clear instructions on how Michael can reset his password for the Identity Manager system by contacting the UZH Service Desk via phone or alternative methods. The information provided in both answers is identical, so either one could be considered the best answer. Therefore, neither option (3) "None of the above" is the correct answer."
            //String completedText = "The best answer would be (1) Wo wurde Levi geboren? Levi wurde in Zürich geboren. This question asks for the place of Levi's birth, and the answer provides the specific location, which is Zürich. The other answer option (2) asks for the time of Levi's birth, which is not relevant to the given question. Thus, (3) None of the answers above is not the correct answer in this case.";
            //String completedText = "The question is written in German and appears to be a personal message, expressing emotions and inviting someone to join a \"virtual communication\" or world, described as a source of passion and joy. The given options are technical support questions and answers related to the University of Zürich (UZH).\n\nSince the question is not related to the options provided, the best answer is (7) \"None of the answers above.\"";
            //String completedText = "The given question 'Was sollen wir heute zum Abendessen machen?' (What should we have for dinner tonight?) is unrelated to the given list of question and answer pairs. Therefore, the best answer would be (7) None of the answers above.";
            //String completedText = "The given question 'Was sollen wir heute zum Abendessen machen?' (What should we have for dinner tonight?) is unrelated to the given list of question and answer pairs. Therefore, the best answer would be (7x) None of the answers above.";
            log.info("Completed prompt: " + completedText);

            int selectedAnswer = findSelectedAnswer(completedText, item_number_none_of_the_answers);
            if (selectedAnswer > 0 && selectedAnswer < item_number_none_of_the_answers) {
                reRankedIndex.add(selectedAnswer - 1);
            } else {
                log.info("None of the answers matched.");
            }
        } catch (Exception e) {
            log.error("No answer was selected, because error occured while asking LLM!");
            log.error(e.getMessage(), e);
        }

        return reRankedIndex.toArray(new Integer[0]);
    }

    /**
     * https://huggingface.co/docs/transformers/main/tasks/prompting
     */
    private String getMultipleChoicePrompt(String question, String[] answers, int item_number_none_of_the_answers)  {

        StringBuilder prompt = new StringBuilder("Please analyze the following question '" + question + "' and choose the best answer from the following list of question and answer pairs: ");

        for (int i = 0; i < answers.length; i++) {
            prompt.append("\n(" + (i + 1) + ") " + Utils.removeTabsAndDoubleSpaces(Utils.replaceNewLines(answers[i], " ")));
        }
        // TODO: Consider changing the prompt, such that the list only contains the question and answer pairs and instruct the LLM re the response format
        prompt.append("\n(" + item_number_none_of_the_answers + ") None of the question and answer pairs above");

        prompt.append("\nand return the selected list number within parentheses.");

        return prompt.toString();
    }

    /**
     * Find selected answer within text generated by LLM
     * @param text Text containing suggesting which is the best answer, e.g. "Based on the given question and the provided answer options, the best answer is (4) How can I reset my password? The question states that Michael Wechner is having trouble logging into the Identity Manager of the USZ and trying to change her password but failing to do so. The answer option (4) provides steps for resetting the password, which matches the issue described in the question. Therefore, the best answer is (4)." or "The given question 'Was sollen wir heute zum Abendessen machen?' (What should we have for dinner tonight?) is unrelated to the given list of question and answer pairs. Therefore, the best answer would be (7) None of the answers above."
     * @param item_number_none_of_the_answers Item number of "None of the answers above"
     * @return number of selected answer
     */
    private int findSelectedAnswer(String text, int item_number_none_of_the_answers) {
        int posOpen = text.indexOf("(");
        if (posOpen < 0) {
            log.warn("Text does not contain an open parenthesis!");
            return -1;
        }

        if (text.indexOf("(" + item_number_none_of_the_answers + ")") >= 0) { // TODO: Also check for word "none" or "None" ...
            log.info("Text does contain item number " + item_number_none_of_the_answers + " of 'None of the answers above'");
            return item_number_none_of_the_answers;
        }

        String _text = text.substring(posOpen);
        log.info("Remaining text: " + _text);
        int posClose = _text.indexOf(")");
        String selectedAnswerSt = _text.substring(1, posClose);
        log.info("Selected answer: " + selectedAnswerSt);
        try {
            Integer selectedAnswer = Integer.parseInt(selectedAnswerSt);
            return selectedAnswer.intValue();
        } catch (Exception e) {
            log.warn("Text within brackets '" + selectedAnswerSt + "' is not an integer.");
            String remainingText = _text.substring(posClose + 1);
            log.info("Try to find selected answer within remaining text: " + remainingText);
            if (remainingText.length() > 0) {
                return findSelectedAnswer(remainingText, item_number_none_of_the_answers);
            } else {
                return -1;
            }
        }
    }
}
