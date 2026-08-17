package com.example.app;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.memory.ChatMemoryAccess;

import java.io.Console;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class App {

    private static final String DEFAULT_MODEL = "gpt-5.6-terra";
    private static final String INTERACTIVE_MEMORY_ID = "interactive";
    private static final BufferedReader STDIN = new BufferedReader(new InputStreamReader(System.in));

    private App() {
    }

    public static void main(String[] args) {
        if (args.length == 1 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            printUsage();
            return;
        }

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("OPENAI_API_KEY is required. Use --help for usage.");
            System.exit(2);
        }

        AssistantAgent assistant = createAssistant(apiKey);
        if (args.length == 0) {
            runInteractive(assistant);
        } else {
            String prompt = String.join(" ", args).trim();
            if (!prompt.isEmpty()) {
                try {
                    stream(assistant.chat("one-shot", prompt));
                } catch (RuntimeException e) {
                    System.err.println("Request failed: " + rootMessage(e));
                    System.exit(1);
                }
            }
        }
    }

    static AssistantAgent createAssistant(String apiKey) {
        String modelName = valueOrDefault(System.getenv("OPENAI_MODEL"), DEFAULT_MODEL);
        String baseUrl = System.getenv("OPENAI_BASE_URL");

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .strictTools(true);

        String reasoningEffort = System.getenv("OPENAI_REASONING_EFFORT");
        if (reasoningEffort != null && !reasoningEffort.isBlank()) {
            builder.reasoningEffort(reasoningEffort);
        } else if (modelName.startsWith("gpt-5.6")) {
            // GPT-5.6 function tools on Chat Completions require reasoning=none.
            builder.reasoningEffort("none");
        }

        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        StreamingChatModel model = builder.build();
        return AgenticServices.agentBuilder(AssistantAgent.class)
                .streamingChatModel(model)
                .chatMemoryProvider(memoryId -> dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(20))
                .tools(new SystemInfoTools())
                .outputKey("answer")
                .build();
    }

    private static void runInteractive(AssistantAgent assistant) {
        System.out.println("Native LangChain4j assistant (type /help, /reset, or /exit)");
        while (true) {
            String input = readLine("You> ");
            if (input == null || "/exit".equalsIgnoreCase(input) || "/quit".equalsIgnoreCase(input)) {
                return;
            }
            if (input.isBlank()) {
                continue;
            }
            if ("/help".equalsIgnoreCase(input)) {
                System.out.println("Commands: /help, /reset, /exit");
                continue;
            }
            if ("/reset".equalsIgnoreCase(input)) {
                ((ChatMemoryAccess) assistant).evictChatMemory(INTERACTIVE_MEMORY_ID);
                System.out.println("Conversation memory reset.");
                continue;
            }

            System.out.print("Assistant> ");
            try {
                stream(assistant.chat(INTERACTIVE_MEMORY_ID, input));
            } catch (RuntimeException e) {
                System.err.println("Request failed: " + rootMessage(e));
            }
        }
    }

    static void stream(TokenStream tokenStream) {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        tokenStream
                .onPartialResponse(System.out::print)
                .onCompleteResponse(response -> {
                    System.out.println();
                    completed.complete(null);
                })
                .onError(completed::completeExceptionally)
                .start();

        try {
            completed.get(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Streaming request interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(rootMessage(e), e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Streaming request timed out", e);
        }
    }

    private static String readLine(String prompt) {
        Console console = System.console();
        if (console != null) {
            return console.readLine(prompt);
        }
        System.out.print(prompt);
        System.out.flush();
        try {
            return STDIN.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = Objects.requireNonNull(throwable);
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static void printUsage() {
        System.out.println("Usage: native-langchain4j [prompt ...]");
        System.out.println("       native-langchain4j --help");
        System.out.println();
        System.out.println("With no prompt, starts an interactive session.");
        System.out.println("Environment: OPENAI_API_KEY (required), OPENAI_MODEL, OPENAI_BASE_URL,");
        System.out.println("             OPENAI_REASONING_EFFORT (optional)");
    }
}
