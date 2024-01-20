package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TemplateArguments {

    Map<String, Object> args;

    /**
     *
     */
    public TemplateArguments() {
        args = new HashMap<>();
    }

    /**
     * @param defaultHostname Default hostname when no domain config provided
     */
    public TemplateArguments(Context domain, String defaultHostname) {
        args = new HashMap<>();
        addBaseArguments(domain, defaultHostname);
    }

    /**
     *
     */
    private void addBaseArguments(Context domain, String defaultHostname) {
        if (domain !=  null) {
            args.put("katie__domain", domain);
            args.put("katie__base_url", domain.getHost());
            args.put("katie__members_link", domain.getHost() + "/#/domain/" + domain.getId() + "/members");
        } else {
            args.put("katie__base_url", defaultHostname);
        }
    }

    /**
     *
     */
    public void add(String key, Object value) {
        args.put(key, value);
    }

    /**
     *
     */
    public Map<String, Object> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,Object> entry : args.entrySet()) {
            if (entry.getValue() instanceof String) {
                sb.append(entry.getKey() + ":" + entry.getValue());
            } else {
                sb.append(entry.getKey() + ":" + "TODO: Value is object");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
