package com.example.app;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class App {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Please provide input for the model as an argument");
        }

        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();

        String answer = model.generate(args[0]);

        System.out.println(answer);

    }

}