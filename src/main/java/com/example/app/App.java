package com.example.app;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class App {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("Please provide input for the model as an argument");
        }

    ChatLanguageModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY")) // or replace with "demo" for demo purposes
        .modelName("gpt-4")
        .build();

    String answer = model.generate(args[0]);

    System.out.println(answer);

    }

}