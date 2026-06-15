package com.openjiuwen.deepagent.springboot.autoconfigure.fixture;

import com.openjiuwen.core.foundation.tool.annotation.ToolDefinition;

public class ScanAnnotatedTools {
    @ToolDefinition(name = "scan_echo", description = "回显输入文本。")
    public String echo(String text) {
        return text;
    }
}
