package com.wyona.katie.services;
  
import com.wyona.katie.models.DynamicExpression;
import com.wyona.katie.models.Entity;
import com.wyona.katie.models.Sentence;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.*;

@Slf4j
@Component
public class DynamicExpressionEvaluationService {

    public static final String AK_EXEC_START_TAG_WITHOUT_END_BRACKET = "<ak-exec";
    public static final String AK_EXEC_START_TAG_END_BRACKET = ">";
    public static final String AK_EXEC_END_TAG = "</ak-exec>";

    @Autowired
    public DynamicExpressionEvaluationService() {
    }

    /**
     * Check whether answer contains a dynamic expression
     * @param answer Answer
     * @param question Question containing entities
     * @return post processed answer
     */
    public String postProcess(String answer, Sentence question) {
        log.info("Check whether answer contains '" + AK_EXEC_START_TAG_WITHOUT_END_BRACKET + "' ..");
        //log.info("Check whether answer '" + answer + "' contains '" + AK_EXEC_START_TAG + "' ..");
        if (answer != null && answer.indexOf(AK_EXEC_START_TAG_WITHOUT_END_BRACKET) >= 0) {
            return evaluate(answer, question);
        } else {
            // INFO: Return unprocessed answer
            return answer;
        }
    }

    /**
     * Evaluate dynamic expression(s) within answer
     * @param answer Answer containing dynamic expression, e.g. "Michael Wechner is currently <ak-exec>com.wyona.askkatie.answers.DateOfBirth#getAgeByPersonName(String)</ak-exec> years old."
     * @param question Question containing entities, e.g. entity "ak-entity:person_name" with value "Michael Wechner"
     * @return answer containing value of evaluated dynamic expression, e.g. "Michael Wechner is currently 51 years old."
     */
    public String evaluate(String answer, Sentence question) {
        log.info("Evaluate dynamic expression(s) inside answer '" + answer + "' ...");

        String processedAnswer = answer;
        while (processedAnswer.indexOf(AK_EXEC_START_TAG_WITHOUT_END_BRACKET) >= 0) {
            try {
                processedAnswer = evaluateNextExpresssion(processedAnswer, question);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                log.error("Error while processing answer: '" + answer + "'");
                return e.getMessage();
            }
        }

        return processedAnswer;
    }

    /**
     * Evaluate expression which occurs first within answer
     * @param answer Answer, which might contain multiple expression(s), e.g. "The time is <ak-exec>...</ak-exec> and the temperature is <ak-exec>...</ak-exec>."
     * @param question Question containing entities
     * @return answer with evaluated first expression, e.g. "The time is 3pm and the temperature is <ak-exec>...</ak-exec>."
     */
    private String evaluateNextExpresssion(String answer, Sentence question) throws Exception {
        // INFO: Parse for <ak-exec> and <ak-exec xmlns="...">

        String firstExpressionInclStartTagAndBeyond = answer.substring(answer.indexOf(AK_EXEC_START_TAG_WITHOUT_END_BRACKET));
        log.debug("First expression including start tag and beyond: " + firstExpressionInclStartTagAndBeyond);
        String firstExpressionAndBeyond = firstExpressionInclStartTagAndBeyond.substring(firstExpressionInclStartTagAndBeyond.indexOf(AK_EXEC_START_TAG_END_BRACKET) + AK_EXEC_START_TAG_END_BRACKET.length());
        log.debug("First expression and beyond: " + firstExpressionAndBeyond);
        String firstExpression = firstExpressionAndBeyond.substring(0, firstExpressionAndBeyond.indexOf(AK_EXEC_END_TAG));
        log.debug("First expression: " + firstExpression);

        DynamicExpression expr = new DynamicExpression(firstExpression);
        log.info("Dynamic expression: " + expr);

        // TODO: See https://stackoverflow.com/questions/160970/how-do-i-invoke-a-java-method-when-given-the-method-name-as-a-string
/* TODO: Consider that class constructor could also have arguments
        Class[] classArgs = new Class[]{};
        Object[] values = new Object[0];
        java.lang.reflect.Constructor ct = Class.forName(expr.getClazz()).getConstructor(classArgs);
        ct.newInstance(values);
*/
        Class<?> clazz = Class.forName(expr.getClazz());
        Object object = clazz.newInstance();
        Class[] methodArgTypes = new Class[0];
        Object[] methodArgValues = new Object[0];
        String[] args = expr.getArguments();
        if (args.length > 0) {
            log.info("Set arguments by reflection ...");
            methodArgTypes = new Class[args.length];
            methodArgValues = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                methodArgTypes[i] = String.class;
                if (args[i].startsWith(Entity.AK_ENTITY_PREFIX)) {
                    log.info("Get named entity '" + args[i] + "' from question '" + question + "'");
                    Entity[] entities = question.getEntities(args[i]);
                    if (entities != null && entities.length == 1) {
                        methodArgValues[i] = entities[0].getValue();
                    } else {
                        log.warn("Question '" + question + "' does not contain such a named entity '" + args[i] + "'. Maybe message/question analyzer did not detect such a named entity.");
                        methodArgValues[i] = null;
                    }
                } else {
                    log.info("Argument '" + args[i] + "' does not start with Katie entity prefix '" + Entity.AK_ENTITY_PREFIX + "', therefore argument is being copied as it is, instead of being resolved as named entity.");
                    methodArgValues[i] = args[i];
                }
            }
        } else {
            log.info("No arguments.");
        }

        log.info("Try to replace expression by dynamically generated value ...");
        Method method = clazz.getDeclaredMethod(expr.getMethod(), methodArgTypes);
        try {
            StringBuilder evaluatedAnswer = new StringBuilder();

            // INFO: Answer until first dynamic expression
            evaluatedAnswer.append(answer.substring(0, answer.indexOf(AK_EXEC_START_TAG_WITHOUT_END_BRACKET)));
            // INFO: Evaluated dynamic expression
            evaluatedAnswer.append(method.invoke(object, methodArgValues));
            // INFO: Remaining answer, which might contain more dynamic expressions
            evaluatedAnswer.append(answer.substring(answer.indexOf(AK_EXEC_END_TAG) + AK_EXEC_END_TAG.length()));

            return evaluatedAnswer.toString();
        } catch(InvocationTargetException e) {
            throw new Exception(e.getCause().getMessage());
        }
    }
}
