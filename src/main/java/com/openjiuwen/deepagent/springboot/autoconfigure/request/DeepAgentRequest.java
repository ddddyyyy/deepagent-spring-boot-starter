package com.openjiuwen.deepagent.springboot.autoconfigure.request;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DeepAgent 调用请求参数。
 * 业务方使用 builder 构造请求，无需直接处理框架的 Map 输入结构。
 */
public class DeepAgentRequest {
    /** 用户查询文本。 */
    private String query;
    /** 会话 ID，必填。不同用户、不同任务应使用隔离的 ID。 */
    private String conversationId;
    /** 额外输入参数，会合并到 DeepAgent inputs 中。 */
    private Map<String, Object> extraInputs = new LinkedHashMap<>();

    public static Builder builder() {
        return new Builder();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Map<String, Object> getExtraInputs() {
        return extraInputs;
    }

    public void setExtraInputs(Map<String, Object> extraInputs) {
        this.extraInputs = extraInputs;
    }

    public static final class Builder {
        private final DeepAgentRequest request = new DeepAgentRequest();

        private Builder() {
        }

        public Builder query(String query) {
            request.setQuery(query);
            return this;
        }

        public Builder conversationId(String conversationId) {
            request.setConversationId(conversationId);
            return this;
        }

        public Builder extraInput(String key, Object value) {
            request.getExtraInputs().put(key, value);
            return this;
        }

        public Builder extraInputs(Map<String, Object> extraInputs) {
            request.setExtraInputs(extraInputs);
            return this;
        }

        public DeepAgentRequest build() {
            return request;
        }
    }
}
