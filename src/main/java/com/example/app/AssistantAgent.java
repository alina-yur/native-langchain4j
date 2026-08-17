package com.example.app;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AssistantAgent {

    @SystemMessage("""
            You are a concise command-line assistant.
            Answer the user's request accurately and directly.
            When the user asks about this local runtime or operating system, use the system_info tool;
            do not guess local facts. The tool is read-only and exposes only safe diagnostic information.
            """)
    @UserMessage("{{message}}")
    @Agent(name = "assistant", outputKey = "answer", description = "Answers user questions and can inspect safe local runtime information")
    TokenStream chat(@MemoryId String memoryId, @V("message") String message);
}
