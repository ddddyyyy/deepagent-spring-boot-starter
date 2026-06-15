package com.openjiuwen.deepagent.springboot.autoconfigure.fixture;

import com.openjiuwen.core.foundation.tool.annotation.ToolDefinition;

public class ConfiguredAnnotatedTools {
    @ToolDefinition(name = "configured_echo", description = "通过配置文件加载的回显工具。")
    public String echo(String text) {
        return text;
    }
}
