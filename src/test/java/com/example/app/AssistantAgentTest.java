package com.example.app;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantAgentTest {

    @Test
    void streamsAnswerThroughAgentProxy() throws Exception {
        StreamingChatModel model = new DeterministicStreamingModel("hello from the test model");
        AssistantAgent assistant = AgenticServices.agentBuilder(AssistantAgent.class)
                .streamingChatModel(model)
                .chatMemoryProvider(memoryId -> dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(20))
                .tools(new SystemInfoTools())
                .outputKey("answer")
                .build();

        CompletableFuture<String> answer = new CompletableFuture<>();
        StringBuilder text = new StringBuilder();
        TokenStream stream = assistant.chat("test", "hello");
        stream.onPartialResponse(text::append)
                .onCompleteResponse(response -> answer.complete(text.toString()))
                .onError(answer::completeExceptionally)
                .start();

        assertEquals("hello from the test model", answer.get(5, TimeUnit.SECONDS));
        assertTrue(((DeterministicStreamingModel) model).lastRequest().messages().size() >= 2);
    }

    private static final class DeterministicStreamingModel implements StreamingChatModel {
        private final String answer;
        private ChatRequest lastRequest;

        private DeterministicStreamingModel(String answer) {
            this.answer = answer;
        }

        @Override
        public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
            lastRequest = request;
            handler.onPartialResponse(answer.substring(0, 5));
            handler.onPartialResponse(answer.substring(5));
            handler.onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse.builder()
                    .aiMessage(AiMessage.from(answer))
                    .build());
        }

        ChatRequest lastRequest() {
            return lastRequest;
        }
    }
}
