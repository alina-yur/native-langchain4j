package com.example.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Small offline OpenAI-compatible SSE server used by native-smoke.sh. */
public final class MockOpenAiServer {

    private MockOpenAiServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/chat/completions", exchange -> respond(exchange, calls.incrementAndGet()));
        server.start();
        Thread.currentThread().join();
    }

    private static void respond(HttpExchange exchange, int call) throws IOException {
        exchange.getRequestBody().readAllBytes();
        String[] events = call == 1
                ? new String[]{
                "{\"id\":\"x\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-5.6-terra\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"system_info\",\"arguments\":\"{}\"}}]},\"finish_reason\":null}]}",
                "{\"id\":\"x\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-5.6-terra\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"tool_calls\"}]}",
                "[DONE]"
        }
                : new String[]{
                "{\"id\":\"x\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-5.6-terra\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"tool worked\"},\"finish_reason\":null}]}",
                "{\"id\":\"x\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"gpt-5.6-terra\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}",
                "[DONE]"
        };

        String body = String.join("\n\n", java.util.Arrays.stream(events)
                .map(event -> "data: " + event)
                .toList()) + "\n\n";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
