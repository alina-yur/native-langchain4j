package com.example.app;

import dev.langchain4j.agent.tool.Tool;

public final class SystemInfoTools {

    @Tool(name = "system_info", value = "Return safe, read-only information about the local JVM runtime and operating system")
    public String systemInfo() {
        return "runtime=" + System.getProperty("java.vm.name")
                + ", runtimeVersion=" + System.getProperty("java.runtime.version")
                + ", os=" + System.getProperty("os.name")
                + ", architecture=" + System.getProperty("os.arch")
                + ", processors=" + Runtime.getRuntime().availableProcessors();
    }
}
