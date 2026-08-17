#!/usr/bin/env bash
set -euo pipefail

port=18765
java -cp target/test-classes com.example.app.MockOpenAiServer "$port" &
server_pid=$!
trap 'kill "$server_pid" 2>/dev/null || true' EXIT
sleep 1

output=$(OPENAI_API_KEY=offline-test \
  OPENAI_BASE_URL="http://127.0.0.1:${port}/v1" \
  ./target/native-langchain4j "tell me about the runtime")

printf '%s\n' "$output"
grep -F "tool worked" <<<"$output" >/dev/null
