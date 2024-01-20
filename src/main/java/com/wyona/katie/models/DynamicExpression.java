package com.wyona.katie.models;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class DynamicExpression {

    String clazz;
    String method;
    String[] arguments;

    /**
     * @param expression Dynamic expression, e.g. 'com.wyona.askkatie.answers.DateOfBirth#getAgeByPersonName(String)'
     */
    public DynamicExpression(String expression) {
        parse(expression);
    }

    /**
     * @param expression Dynamic expression, e.g. 'com.wyona.askkatie.answers.DateOfBirth#getAgeByPersonName(String)'
     */
    public void parse(String expression) {
        log.info("Parse expression: " + expression);
        clazz = expression.substring(0, expression.indexOf("#"));
        log.debug("Class: " + clazz);
        method = expression.substring(expression.indexOf("#") + 1, expression.indexOf("("));
        String argumentsStr = expression.substring(expression.indexOf("(") + 1, expression.indexOf(")"));
        if (argumentsStr.trim().length() > 0) {
            arguments = argumentsStr.split(",");
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i].startsWith("'")) {
                    // INFO: Argument is a regular string, whereas remove quotes for further processing
                    arguments[i] = removeQuotes(arguments[i]);
                } else if (arguments[i].startsWith(Entity.AK_ENTITY_PREFIX)) {
                    log.info("TODO: Check whether such a system entity '" + arguments[i] + "' is supported");
                    // INFO: Compare with https://cloud.google.com/dialogflow/docs/reference/system-entities or https://developers.google.com/freebase/guide/basic_concepts
                } else if (arguments[i].startsWith(Entity.CUSTOM_ENTITY_PREFIX)) {
                    log.info("TODO: Check whether such a custom entity '" + arguments[i] + "' is configured");
                } else {
                    log.error("No such argument type '" + arguments[i] + "' supported!");
                }
            }
        } else {
            arguments = new String[0];
        }
    }

    /**
     * Remove quotes from string
     * @param s String with quotes, e.g. '1969.02.16'
     * @return string without quotes, e.g. 1969.02.16
     */
    private String removeQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }

    /**
     *
     */
    public String getClazz() {
        return clazz;
    }

    /**
     *
     */
    public String getMethod() {
        return method;
    }

    /**
     *
     */
    public String[] getArguments() {
        return arguments;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Class: " + clazz + ", Method: " + method);
        if (arguments.length > 0) {
            sb.append(", Arguments: ");
            for (int i = 0; i < arguments.length; i++) {
                sb.append(arguments[i]);
                if (i < arguments.length - 1) {
                    sb.append(",");
                }
            }
        } else {
            sb.append(", No arguments");
        }
        return sb.toString();
    }
}
