package com.example.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemInfoToolsTest {

    @Test
    void reportsOnlySafeRuntimeFacts() {
        String result = new SystemInfoTools().systemInfo();

        assertTrue(result.contains("runtime="));
        assertTrue(result.contains("runtimeVersion="));
        assertTrue(result.contains("os="));
        assertTrue(result.contains("architecture="));
        assertTrue(result.contains("processors="));
    }
}
