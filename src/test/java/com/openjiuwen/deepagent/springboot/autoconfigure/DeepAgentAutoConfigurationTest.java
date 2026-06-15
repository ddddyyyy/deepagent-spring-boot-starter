package com.openjiuwen.deepagent.springboot.autoconfigure;

import com.openjiuwen.core.foundation.tool.Tool;
import com.openjiuwen.core.singleagent.agents.ReActAgent;
import com.openjiuwen.core.singleagent.agents.ReActAgentConfig;
import com.openjiuwen.deepagent.springboot.autoconfigure.client.DeepAgentClient;
import com.openjiuwen.deepagent.springboot.autoconfigure.fixture.ConfiguredAnnotatedTools;
import com.openjiuwen.deepagent.springboot.autoconfigure.fixture.AgentRailFixtureConfiguration;
import com.openjiuwen.deepagent.springboot.autoconfigure.fixture.ScanAnnotatedTools;
import com.openjiuwen.deepagent.springboot.autoconfigure.properties.DeepAgentSpringProperties;
import com.openjiuwen.harness.deep_agent.DeepAgent;
import com.openjiuwen.harness.schema.config.DeepAgentConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeepAgentAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DeepAgentAutoConfiguration.class));

    @Test
    void shouldCreateDeepAgentBeansFromProperties() {
        contextRunner.withPropertyValues(
                        "deep-agent.language=cn",
                        "deep-agent.workspace-path=./target/workspace",
                        "deep-agent.system-prompt=你是一个测试助手",
                        "deep-agent.task-loop-enabled=false",
                        "deep-agent.model.model=deepseek-v4-flash",
                        "deep-agent.backend.client_provider=openai",
                        "deep-agent.backend.api_base=https://api.deepseek.com"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(DeepAgent.class)
                        .hasSingleBean(DeepAgentClient.class)
                        .hasSingleBean(DeepAgentSpringProperties.class));
    }

    @Test
    void shouldRespectEnabledSwitch() {
        contextRunner.withPropertyValues("deep-agent.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(DeepAgent.class)
                        .doesNotHaveBean(DeepAgentClient.class));
    }

    @Test
    void shouldRegisterAnnotatedBeanAndConfiguredClassTools() {
        contextRunner.withUserConfiguration(ToolFixtureConfiguration.class)
                .withPropertyValues(
                        "deep-agent.task-loop-enabled=false",
                        "deep-agent.tools.class-names[0]=" + ConfiguredAnnotatedTools.class.getName()
                )
                .run(context -> {
                    DeepAgentConfig config = readConfig(context.getBean(DeepAgent.class));
                    assertThat(config.getTools())
                            .hasSize(2)
                            .allMatch(Tool.class::isInstance);
                    assertThat(config.getTools().stream()
                            .map(Tool.class::cast)
                            .map(tool -> tool.getCard().getName()))
                            .contains("scan_echo", "configured_echo");
                });
    }

    @Test
    void shouldApplyCustomHeadersToInternalReActAgentConfig() {
        contextRunner.withPropertyValues(
                        "deep-agent.task-loop-enabled=false",
                        "deep-agent.backend.client_provider=openai",
                        "deep-agent.backend.headers.X-From-Backend=backend",
                        "deep-agent.headers.X-Trace-Source=spring",
                        "deep-agent.headers.X-Tenant-Id=tenant-a"
                )
                .run(context -> {
                    DeepAgent deepAgent = context.getBean(DeepAgent.class);
                    DeepAgentConfig config = readConfig(deepAgent);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> backend = (Map<String, Object>) config.getBackend();
                    @SuppressWarnings("unchecked")
                    Map<String, String> headers = (Map<String, String>) backend.get("headers");
                    assertThat(headers).containsEntry("X-From-Backend", "backend");

                    ReActAgentConfig reactAgentConfig = readReActAgentConfig(deepAgent);
                    assertThat(reactAgentConfig.getCustomHeaders())
                            .containsEntry("X-Trace-Source", "spring")
                            .containsEntry("X-Tenant-Id", "tenant-a");
                    assertThat(backend.toString()).doesNotContain("X-Trace-Source", "X-Tenant-Id");
                });
    }

    @Test
    void shouldAutoRegisterAnnotatedRail() {
        contextRunner.withUserConfiguration(AgentRailFixtureConfiguration.class)
                .withPropertyValues(
                        "deep-agent.task-loop-enabled=false"
                )
                .run(context -> {
                    DeepAgentConfig config = readConfig(context.getBean(DeepAgent.class));
                    assertThat(config.getRails())
                            .anyMatch(rail -> rail.getClass().getSimpleName().equals("AnnotatedCustomRail"));
                });
    }


    private DeepAgentConfig readConfig(DeepAgent deepAgent) {
        try {
            Field field = DeepAgent.class.getDeclaredField("config");
            field.setAccessible(true);
            return (DeepAgentConfig) field.get(deepAgent);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read DeepAgent config", e);
        }
    }

    private ReActAgentConfig readReActAgentConfig(DeepAgent deepAgent) {
        ReActAgent reactAgent = deepAgent.getAgent();
        return (ReActAgentConfig) reactAgent.getConfig();
    }

    @Configuration(proxyBeanMethods = false)
    static class ToolFixtureConfiguration {
        @Bean
        ScanAnnotatedTools scanAnnotatedTools() {
            return new ScanAnnotatedTools();
        }
    }
}
