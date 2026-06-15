package com.openjiuwen.deepagent.springboot.autoconfigure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将 Spring Bean 标记为 DeepAgent Rail，starter 启动时会自动扫描并注入到 DeepAgentConfig 中。
 * 被标记的类需要继承 com.openjiuwen.harness.rails.DeepAgentRail（或其子类）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AgentRail {
}
