package com.wyona.katie.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIChatClientsConfig {

    @Bean
    ChatClient ollamaChatClient(@Qualifier("ollamaChatModel")ChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    ChatClient azureChatClient(@Qualifier("azureOpenAiChatModel")ChatModel model) {
        return ChatClient.builder(model).build();
    }
}
