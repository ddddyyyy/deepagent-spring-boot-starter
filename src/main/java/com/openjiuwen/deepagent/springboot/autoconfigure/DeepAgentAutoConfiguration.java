package com.openjiuwen.deepagent.springboot.autoconfigure;

import com.openjiuwen.deepagent.springboot.autoconfigure.client.DeepAgentClient;
import com.openjiuwen.deepagent.springboot.autoconfigure.prompt.PromptProvider;
import com.openjiuwen.deepagent.springboot.autoconfigure.properties.DeepAgentSpringProperties;
import com.openjiuwen.deepagent.springboot.autoconfigure.tool.DeepAgentHeaderConfigurer;
import com.openjiuwen.deepagent.springboot.autoconfigure.tool.DeepAgentRailResolver;
import com.openjiuwen.deepagent.springboot.autoconfigure.tool.DeepAgentSysOperationSupport;
import com.openjiuwen.deepagent.springboot.autoconfigure.tool.DeepAgentToolResolver;
import com.openjiuwen.harness.factory.HarnessFactory;
import com.openjiuwen.harness.deep_agent.DeepAgent;
import com.openjiuwen.harness.schema.config.DeepAgentConfig;
import com.openjiuwen.harness.workspace.Workspace;
import com.openjiuwen.core.singleagent.schema.AgentCard;
import com.openjiuwen.core.sysop.SysOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Boot 自动装配入口。
 * 按以下顺序组装 DeepAgent：
 * 1. 读取 deep-agent.* 配置，构建 AgentCard / Workspace / SysOperation / Rails
 * 2. 通过 HarnessFactory 创建 DeepAgent 实例
 * 3. 配置内部 ReActAgent（提示词、模型客户端、自定义 header）
 * 4. 注入 SysOperation 工具
 * 5. 可选 ensureInitialized()
 */
@AutoConfiguration
@ConditionalOnClass(DeepAgent.class)
@EnableConfigurationProperties(DeepAgentSpringProperties.class)
@ConditionalOnProperty(prefix = "deep-agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeepAgentAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DeepAgentAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public DeepAgent deepAgent(
            DeepAgentSpringProperties properties,
            DeepAgentToolResolver toolResolver,
            DeepAgentRailResolver railResolver,
            Optional<PromptProvider> promptProvider
    ) {
        log.info("[DeepAgent Starter] 开始初始化 DeepAgent...");
        log.info("[DeepAgent Starter] Agent ID: {}, workspace: {}",
                properties.getAgentCard().getId(), properties.getWorkspacePath());

        // 优先使用 PromptProvider（用户自定义，可从文件/数据库加载）
        String effectivePrompt = null;
        if (promptProvider.isPresent()) {
            effectivePrompt = promptProvider.get().getSystemPrompt();
            log.info("[DeepAgent Starter] 使用 PromptProvider 注入的提示词");
        }
        if (effectivePrompt == null || effectivePrompt.isBlank()) {
            effectivePrompt = properties.getReactAgent() != null
                    && properties.getReactAgent().getPrompt() != null
                    && !properties.getReactAgent().getPrompt().isBlank()
                    ? properties.getReactAgent().getPrompt()
                    : properties.getSystemPrompt();
            log.info("[DeepAgent Starter] 使用配置文件的提示词");
        }

        // 创建 SysOperation
        log.info("[DeepAgent Starter] 创建 SysOperation ...");
        SysOperation sysOperation = DeepAgentSysOperationSupport.createSysOperation(properties);
        if (sysOperation != null) {
            log.info("[DeepAgent Starter] SysOperation 创建完成: id={}, workDir={}",
                    properties.getSysOperation().getId(),
                    properties.getSysOperation().getWorkDir());
        } else {
            log.info("[DeepAgent Starter] SysOperation 已关闭（enabled=false），跳过");
        }

        // 解析 Tool 和 Rail
        log.info("[DeepAgent Starter] 解析 Tool 和 Rail ...");
        List<Object> tools = toolResolver.resolveTools();
        List<Object> rails = railResolver.resolveRails();
        log.info("[DeepAgent Starter] 解析完成: tools={}, rails={}", tools.size(), rails.size());

        // 构建配置
        DeepAgentConfig config = toDeepAgentConfig(
                properties,
                tools,
                rails,
                sysOperation);
        log.info("[DeepAgent Starter] DeepAgentConfig 构建完成: taskLoop={}, taskPlanning={}, language={}",
                config.isEnableTaskLoop(), config.isEnableTaskPlanning(), config.getLanguage());

        // 创建 DeepAgent
        AgentCard agentCard = toAgentCard(properties);
        Workspace workspace = toWorkspace(properties);
        log.info("[DeepAgent Starter] AgentCard: id={}, name={}", agentCard.getId(), agentCard.getName());
        log.info("[DeepAgent Starter] Workspace: rootPath={}, language={}", workspace.getRootPath(), workspace.getLanguage());

        DeepAgent agent = HarnessFactory.createDeepAgent(agentCard, config, workspace);
        log.info("[DeepAgent Starter] DeepAgent 实例创建完成");

        // 配置内部 ReActAgent
        log.info("[DeepAgent Starter] 配置内部 ReActAgent ...");
        DeepAgentHeaderConfigurer.configure(agent, properties.getHeaders());
        DeepAgentHeaderConfigurer.configureReActAgent(agent, properties, effectivePrompt);
        log.info("[DeepAgent Starter] ReActAgent 配置完成");

        // 注入 SysOperation 工具
        if (sysOperation != null && properties.getSysOperation() != null
                && properties.getSysOperation().isInjectTools()) {
            log.info("[DeepAgent Starter] 注入 SysOperation 工具 ...");
            DeepAgentSysOperationSupport.injectSysOperationTools(agent, properties, sysOperation);
            log.info("[DeepAgent Starter] SysOperation 工具注入完成");
        }

        // 确保初始化
        if (properties.isEnsureInitialized()) {
            log.info("[DeepAgent Starter] 执行 ensureInitialized() ...");
            agent.ensureInitialized();
            log.info("[DeepAgent Starter] ensureInitialized() 完成");
        }

        log.info("[DeepAgent Starter] DeepAgent 初始化全部完成");
        return agent;
    }

    @Bean
    @ConditionalOnBean(DeepAgent.class)
    @ConditionalOnMissingBean
    public DeepAgentClient deepAgentClient(DeepAgent deepAgent, DeepAgentSpringProperties properties) {
        log.info("[DeepAgent Starter] 创建 DeepAgentClient");
        return new DeepAgentClient(deepAgent, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DeepAgentToolResolver deepAgentToolResolver(
            org.springframework.context.ApplicationContext applicationContext,
            DeepAgentSpringProperties properties
    ) {
        return new DeepAgentToolResolver(applicationContext, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DeepAgentRailResolver deepAgentRailResolver(
            org.springframework.context.ApplicationContext applicationContext,
            DeepAgentSpringProperties properties
    ) {
        return new DeepAgentRailResolver(applicationContext, properties);
    }

    /** 将配置转为 DeepAgentConfig 对象。 */
    private DeepAgentConfig toDeepAgentConfig(
            DeepAgentSpringProperties properties,
            List<Object> tools,
            List<Object> rails,
            SysOperation sysOperation
    ) {
        return DeepAgentConfig.builder()
                .systemPrompt(properties.getSystemPrompt())
                .maxIterations(properties.getMaxIterations())
                .enableTaskLoop(properties.isTaskLoopEnabled())
                .enableTaskPlanning(properties.isTaskPlanningEnabled())
                .enableAsyncSubagent(properties.isAsyncSubagentEnabled())
                .addGeneralPurposeAgent(properties.isGeneralPurposeAgentEnabled())
                .restrictToWorkDir(properties.isRestrictToWorkDir())
                .language(properties.getLanguage())
                .workspacePath(properties.getWorkspacePath())
                .completionTimeout(properties.getCompletionTimeout())
                .skillDirectories(copyList(properties.getSkillDirectories()))
                .skillMode(properties.getSkillMode())
                .model(copyMap(properties.getModel()))
                .backend(copyMap(properties.getBackend()))
                .tools(tools)
                .rails(rails)
                .sysOperation(sysOperation)
                .extraPromptSections(copyPromptSections(properties.getExtraPromptSections()))
                .build();
    }

    /** 将配置转为 AgentCard。 */
    private AgentCard toAgentCard(DeepAgentSpringProperties properties) {
        return AgentCard.builder()
                .id(properties.getAgentCard().getId())
                .name(properties.getAgentCard().getName())
                .description(properties.getAgentCard().getDescription())
                .build();
    }

    /** 将配置转为 Workspace。 */
    private Workspace toWorkspace(DeepAgentSpringProperties properties) {
        return Workspace.builder()
                .rootPath(properties.getWorkspacePath())
                .language(properties.getLanguage())
                .build();
    }

    // ---------- 集合浅拷贝工具方法 ----------

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private List<Map<String, Object>> copyPromptSections(List<Map<String, Object>> source) {
        if (source == null) {
            return new ArrayList<>();
        }
        return source.stream()
                .map(LinkedHashMap::new)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }
}
