package com.openjiuwen.deepagent.springboot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "deep-agent")
public class DeepAgentSpringProperties {
    /**
     * Whether to create DeepAgent related beans.
     */
    private boolean enabled = true;

    private String systemPrompt = "你是一个中文智能体助手，请基于用户问题给出准确、简洁、可执行的回答。";
    private int maxIterations = 15;
    private boolean taskLoopEnabled = true;
    private boolean taskPlanningEnabled;
    private boolean asyncSubagentEnabled;
    private boolean generalPurposeAgentEnabled;
    private boolean restrictToWorkDir = true;
    private boolean ensureInitialized;
    private String language = "cn";
    private String workspacePath = "./";
    private Double completionTimeout;
    private List<String> skillDirectories = new ArrayList<>();
    private String skillMode = "all";
    private Map<String, Object> model = new LinkedHashMap<>();
    private Map<String, Object> backend = new LinkedHashMap<>();
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, Object> permissions = new LinkedHashMap<>();
    private List<Map<String, Object>> extraPromptSections = new ArrayList<>();
    private List<String> streamModes = List.of("OUTPUT");
    private ToolProperties tools = new ToolProperties();
    private AgentCardProperties agentCard = new AgentCardProperties();
    private SysOperationProperties sysOperation = new SysOperationProperties();
    private RailProperties rails = new RailProperties();
    private ReActAgentProperties reactAgent = new ReActAgentProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public boolean isTaskLoopEnabled() {
        return taskLoopEnabled;
    }

    public void setTaskLoopEnabled(boolean taskLoopEnabled) {
        this.taskLoopEnabled = taskLoopEnabled;
    }

    public boolean isTaskPlanningEnabled() {
        return taskPlanningEnabled;
    }

    public void setTaskPlanningEnabled(boolean taskPlanningEnabled) {
        this.taskPlanningEnabled = taskPlanningEnabled;
    }

    public boolean isAsyncSubagentEnabled() {
        return asyncSubagentEnabled;
    }

    public void setAsyncSubagentEnabled(boolean asyncSubagentEnabled) {
        this.asyncSubagentEnabled = asyncSubagentEnabled;
    }

    public boolean isGeneralPurposeAgentEnabled() {
        return generalPurposeAgentEnabled;
    }

    public void setGeneralPurposeAgentEnabled(boolean generalPurposeAgentEnabled) {
        this.generalPurposeAgentEnabled = generalPurposeAgentEnabled;
    }

    public boolean isRestrictToWorkDir() {
        return restrictToWorkDir;
    }

    public void setRestrictToWorkDir(boolean restrictToWorkDir) {
        this.restrictToWorkDir = restrictToWorkDir;
    }

    public boolean isEnsureInitialized() {
        return ensureInitialized;
    }

    public void setEnsureInitialized(boolean ensureInitialized) {
        this.ensureInitialized = ensureInitialized;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public Double getCompletionTimeout() {
        return completionTimeout;
    }

    public void setCompletionTimeout(Double completionTimeout) {
        this.completionTimeout = completionTimeout;
    }

    public List<String> getSkillDirectories() {
        return skillDirectories;
    }

    public void setSkillDirectories(List<String> skillDirectories) {
        this.skillDirectories = skillDirectories;
    }

    public String getSkillMode() {
        return skillMode;
    }

    public void setSkillMode(String skillMode) {
        this.skillMode = skillMode;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    public Map<String, Object> getBackend() {
        return backend;
    }

    public void setBackend(Map<String, Object> backend) {
        this.backend = backend;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, Object> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Object> permissions) {
        this.permissions = permissions;
    }

    public List<Map<String, Object>> getExtraPromptSections() {
        return extraPromptSections;
    }

    public void setExtraPromptSections(List<Map<String, Object>> extraPromptSections) {
        this.extraPromptSections = extraPromptSections;
    }

    public List<String> getStreamModes() {
        return streamModes;
    }

    public void setStreamModes(List<String> streamModes) {
        this.streamModes = streamModes;
    }

    public ToolProperties getTools() {
        return tools;
    }

    public void setTools(ToolProperties tools) {
        this.tools = tools;
    }

    public AgentCardProperties getAgentCard() {
        return agentCard;
    }

    public void setAgentCard(AgentCardProperties agentCard) {
        this.agentCard = agentCard;
    }

    public SysOperationProperties getSysOperation() {
        return sysOperation;
    }

    public void setSysOperation(SysOperationProperties sysOperation) {
        this.sysOperation = sysOperation;
    }

    public RailProperties getRails() {
        return rails;
    }

    public void setRails(RailProperties rails) {
        this.rails = rails;
    }

    public ReActAgentProperties getReactAgent() {
        return reactAgent;
    }

    public void setReactAgent(ReActAgentProperties reactAgent) {
        this.reactAgent = reactAgent;
    }

    public static class ToolProperties {
        private boolean scanAnnotatedBeans = true;
        private List<String> beanNames = new ArrayList<>();
        private List<String> classNames = new ArrayList<>();

        public boolean isScanAnnotatedBeans() {
            return scanAnnotatedBeans;
        }

        public void setScanAnnotatedBeans(boolean scanAnnotatedBeans) {
            this.scanAnnotatedBeans = scanAnnotatedBeans;
        }

        public List<String> getBeanNames() {
            return beanNames;
        }

        public void setBeanNames(List<String> beanNames) {
            this.beanNames = beanNames;
        }

        public List<String> getClassNames() {
            return classNames;
        }

        public void setClassNames(List<String> classNames) {
            this.classNames = classNames;
        }
    }

    public static class AgentCardProperties {
        private String id = "deep_agent";
        private String name = "DeepAgent";
        private String description = "Dynamic Planning Deep Agent";

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class SysOperationProperties {
        private boolean enabled = true;
        private String id = "deep_agent_sys_operation";
        private String name = "deep_agent_sys_operation";
        private String workDir;
        private boolean restrictToSandbox;
        private boolean injectTools = true;
        private List<String> shellAllowlist = new ArrayList<>();
        private List<String> sandboxRoot = new ArrayList<>();
        private List<String> dangerousPatterns = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWorkDir() {
            return workDir;
        }

        public void setWorkDir(String workDir) {
            this.workDir = workDir;
        }

        public boolean isRestrictToSandbox() {
            return restrictToSandbox;
        }

        public void setRestrictToSandbox(boolean restrictToSandbox) {
            this.restrictToSandbox = restrictToSandbox;
        }

        public boolean isInjectTools() {
            return injectTools;
        }

        public void setInjectTools(boolean injectTools) {
            this.injectTools = injectTools;
        }

        public List<String> getShellAllowlist() {
            return shellAllowlist;
        }

        public void setShellAllowlist(List<String> shellAllowlist) {
            this.shellAllowlist = shellAllowlist;
        }

        public List<String> getSandboxRoot() {
            return sandboxRoot;
        }

        public void setSandboxRoot(List<String> sandboxRoot) {
            this.sandboxRoot = sandboxRoot;
        }

        public List<String> getDangerousPatterns() {
            return dangerousPatterns;
        }

        public void setDangerousPatterns(List<String> dangerousPatterns) {
            this.dangerousPatterns = dangerousPatterns;
        }
    }

    public static class RailProperties {
        private boolean skillUseEnabled = true;
        private boolean scanAnnotated = true;
        private boolean sysOperationEnabled = true;
        private List<String> beanNames = new ArrayList<>();
        private List<String> classNames = new ArrayList<>();

        public boolean isSkillUseEnabled() {
            return skillUseEnabled;
        }

        public void setSkillUseEnabled(boolean skillUseEnabled) {
            this.skillUseEnabled = skillUseEnabled;
        }

        public boolean isScanAnnotated() {
            return scanAnnotated;
        }

        public void setScanAnnotated(boolean scanAnnotated) {
            this.scanAnnotated = scanAnnotated;
        }

        public boolean isSysOperationEnabled() {
            return sysOperationEnabled;
        }

        public void setSysOperationEnabled(boolean sysOperationEnabled) {
            this.sysOperationEnabled = sysOperationEnabled;
        }

        public List<String> getBeanNames() {
            return beanNames;
        }

        public void setBeanNames(List<String> beanNames) {
            this.beanNames = beanNames;
        }

        public List<String> getClassNames() {
            return classNames;
        }

        public void setClassNames(List<String> classNames) {
            this.classNames = classNames;
        }
    }

    public static class ReActAgentProperties {
        private boolean enabled = true;
        private boolean clearLlm = true;
        private String prompt;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isClearLlm() {
            return clearLlm;
        }

        public void setClearLlm(boolean clearLlm) {
            this.clearLlm = clearLlm;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }
    }
}
