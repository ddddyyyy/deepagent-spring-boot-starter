package com.openjiuwen.deepagent.springboot.autoconfigure.client;

import com.openjiuwen.core.session.stream.StreamMode;
import com.openjiuwen.deepagent.springboot.autoconfigure.properties.DeepAgentSpringProperties;
import com.openjiuwen.deepagent.springboot.autoconfigure.request.DeepAgentRequest;
import com.openjiuwen.harness.deep_agent.DeepAgent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 面向业务代码的轻量调用门面。
 * 封装 query、conversation_id、LANGUAGE 等输入结构，避免业务方直接依赖框架 Map 结构。
 */
public class DeepAgentClient {
    private final DeepAgent deepAgent;
    private final DeepAgentSpringProperties properties;

    public DeepAgentClient(DeepAgent deepAgent, DeepAgentSpringProperties properties) {
        this.deepAgent = deepAgent;
        this.properties = properties;
    }

    /** 非流式调用。 */
    public Map<String, Object> invoke(DeepAgentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return deepAgent.invoke(toInputs(request));
    }

    /** 流式调用。 */
    public Iterator<Object> stream(DeepAgentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return deepAgent.stream(toInputs(request), resolveStreamModes());
    }

    /** 获取原生 DeepAgent 实例，方便需要完整控制的场景。 */
    public DeepAgent getNativeAgent() {
        return deepAgent;
    }

    /** 将 DeepAgentRequest 转为 DeepAgent.invoke/stream 所需的 inputs Map。 */
    private Map<String, Object> toInputs(DeepAgentRequest request) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("query", request.getQuery() == null ? "" : request.getQuery());
        inputs.put("conversation_id", request.getConversationId());
        inputs.put("LANGUAGE", properties.getLanguage());
        if (request.getExtraInputs() != null) {
            inputs.putAll(request.getExtraInputs());
        }
        return inputs;
    }

    /** 解析配置中指定的流式模式。 */
    private List<StreamMode> resolveStreamModes() {
        if (properties.getStreamModes() == null || properties.getStreamModes().isEmpty()) {
            return List.of(StreamMode.OUTPUT);
        }
        return properties.getStreamModes().stream()
                .map(mode -> StreamMode.valueOf(mode.trim().toUpperCase()))
                .toList();
    }
}
