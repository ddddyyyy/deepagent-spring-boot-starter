package com.openjiuwen.deepagent.springboot.autoconfigure.tool;

import com.openjiuwen.core.singleagent.agents.ReActAgent;
import com.openjiuwen.core.singleagent.agents.ReActAgentConfig;
import com.openjiuwen.deepagent.springboot.autoconfigure.properties.DeepAgentSpringProperties;
import com.openjiuwen.harness.deep_agent.DeepAgent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepAgent 内部 ReActAgent 的配置器。
 * 负责写入自定义请求 header 和重新配置 ReActAgent（模型客户端、提示词、迭代次数等）。
 */
public final class DeepAgentHeaderConfigurer {
    private DeepAgentHeaderConfigurer() {
    }

    /** 将自定义 header 写入内部 ReActAgentConfig.customHeaders。 */
    public static void configure(DeepAgent deepAgent, Map<String, String> headers) {
        if (deepAgent == null || headers == null || headers.isEmpty()) {
            return;
        }
        ReActAgent reactAgent = deepAgent.getAgent();
        Object config = reactAgent.getConfig();
        if (config instanceof ReActAgentConfig reactAgentConfig) {
            reactAgentConfig.configureCustomHeaders(headers);
            reactAgent.configure(reactAgentConfig);
            return;
        }
        throw new IllegalStateException("DeepAgent internal ReActAgent config is not ReActAgentConfig");
    }

    /** 按配置重新配置内部 ReActAgent（使用外部传入的提示词）。 */
    public static void configureReActAgent(
            DeepAgent deepAgent, DeepAgentSpringProperties properties, String effectivePrompt
    ) {
        if (deepAgent == null
                || properties.getReactAgent() == null
                || !properties.getReactAgent().isEnabled()) {
            return;
        }

        ReActAgentConfig reactConfig = ReActAgentConfig.builder()
                .promptTemplate(List.of(Map.of(
                        "role", "system",
                        "content", effectivePrompt
                )))
                .maxIterations(properties.getMaxIterations())
                .build()
                .configureCustomHeaders(new LinkedHashMap<>(properties.getHeaders()));

        // 从 backend 和 model 配置中提取模型客户端参数
        if (hasModelClientConfig(properties)) {
            reactConfig.configureModelClient(
                    string(firstPresent(properties.getBackend(), "client_provider", "clientProvider")),
                    string(firstPresent(properties.getBackend(), "api_key", "apiKey")),
                    string(firstPresent(properties.getBackend(), "api_base", "apiBase", "base_url", "baseUrl")),
                    string(firstPresent(properties.getModel(), "model", "model_name", "modelName")),
                    booleanValue(firstPresent(properties.getBackend(), "verify_ssl", "verifySsl"), true)
            );
        }

        ReActAgent inner = deepAgent.getAgent();
        inner.configure(reactConfig);
        if (properties.getReactAgent().isClearLlm()) {
            // 清除 LLM 缓存，让内部 ReActAgent 重新按 config 创建模型客户端
            inner.setLlm(null);
        }
    }

    /** 判断 backend+model 是否提供了完整的模型客户端参数。 */
    private static boolean hasModelClientConfig(DeepAgentSpringProperties properties) {
        return hasText(string(firstPresent(properties.getBackend(), "client_provider", "clientProvider")))
                && hasText(string(firstPresent(properties.getBackend(), "api_key", "apiKey")))
                && hasText(string(firstPresent(properties.getBackend(), "api_base", "apiBase", "base_url", "baseUrl")))
                && hasText(string(firstPresent(properties.getModel(), "model", "model_name", "modelName")));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 从 Map 中按多个 key 依次查找，返回第一个非空值。 */
    private static Object firstPresent(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
