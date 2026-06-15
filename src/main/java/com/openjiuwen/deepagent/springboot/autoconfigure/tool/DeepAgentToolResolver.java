package com.openjiuwen.deepagent.springboot.autoconfigure.tool;

import com.openjiuwen.core.foundation.tool.Tool;
import com.openjiuwen.core.foundation.tool.annotation.ToolDefinition;
import com.openjiuwen.core.foundation.tool.function.AnnotatedToolFactory;
import com.openjiuwen.deepagents.DeepAgentsFactory;
import com.openjiuwen.deepagent.springboot.autoconfigure.client.DeepAgentClient;
import com.openjiuwen.deepagent.springboot.autoconfigure.properties.DeepAgentSpringProperties;
import com.openjiuwen.harness.deep_agent.DeepAgent;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeepAgent Tool 解析器。
 * 支持三种注册方式：
 * 1. 自动扫描 Spring Bean 上的 @ToolDefinition 注解
 * 2. 通过 beanNames 精确指定 Spring Bean
 * 3. 通过 classNames 精确指定工具类
 */
public class DeepAgentToolResolver {
    private static final Logger log = LoggerFactory.getLogger(DeepAgentToolResolver.class);
    private final ApplicationContext applicationContext;
    private final DeepAgentSpringProperties properties;

    public DeepAgentToolResolver(ApplicationContext applicationContext, DeepAgentSpringProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    /** 解析所有 Tool 并返回列表。同名工具以最后一个来源为准。 */
    public List<Object> resolveTools() {
        Map<String, Object> tools = new LinkedHashMap<>();
        addAnnotatedBeanTools(tools);
        addConfiguredBeanTools(tools);
        addConfiguredClassTools(tools);
        return new ArrayList<>(tools.values());
    }

    /** 扫描 Spring 容器中所有带 @ToolDefinition 方法的 Bean。 */
    private void addAnnotatedBeanTools(Map<String, Object> tools) {
        if (properties.getTools() == null || !properties.getTools().isScanAnnotatedBeans()) {
            return;
        }
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            if (shouldSkipBean(beanName)) {
                continue;
            }
            Object bean = applicationContext.getBean(beanName);
            int before = tools.size();
            addToolsFromTarget(tools, bean);
            int added = tools.size() - before;
            if (added > 0) {
                List<String> names = new ArrayList<>();
                tools.values().stream().skip(tools.size() - added).forEach(
                    t -> names.add(t instanceof Tool tool ? tool.getCard().getName() : t.getClass().getSimpleName()));
                log.info("[DeepAgent Starter] 从 Bean [{}] 扫描到 {} 个 Tool: {}", beanName, added, names);
            }
        }
    }

    /** 按配置中的 beanNames 精确加载工具。 */
    private void addConfiguredBeanTools(Map<String, Object> tools) {
        if (properties.getTools() == null || properties.getTools().getBeanNames() == null) {
            return;
        }
        for (String beanName : properties.getTools().getBeanNames()) {
            if (beanName == null || beanName.isBlank()) {
                continue;
            }
            Object bean = applicationContext.getBean(beanName);
            if (bean instanceof Tool tool) {
                tools.put(tool.getCard().getName(), tool);
                log.info("[DeepAgent Starter] 通过 bean-name [{}] 注册 Tool: {}", beanName, tool.getCard().getName());
                continue;
            }
            int before = tools.size();
            addToolsFromTarget(tools, bean);
            int added = tools.size() - before;
            if (added > 0) {
                List<String> names = new ArrayList<>();
                tools.values().stream().skip(tools.size() - added).forEach(
                    t -> names.add(t instanceof Tool tool ? tool.getCard().getName() : t.getClass().getSimpleName()));
                log.info("[DeepAgent Starter] 从 bean-name [{}] 扫描到 {} 个 Tool: {}", beanName, added, names);
            }
        }
    }

    /** 按配置中的 classNames 实例化工具类。 */
    private void addConfiguredClassTools(Map<String, Object> tools) {
        if (properties.getTools() == null || properties.getTools().getClassNames() == null) {
            return;
        }
        ClassLoader classLoader = applicationContext.getClassLoader();
        for (String className : properties.getTools().getClassNames()) {
            if (className == null || className.isBlank()) {
                continue;
            }
            Object target = instantiate(className, classLoader);
            if (target instanceof Tool tool) {
                tools.put(tool.getCard().getName(), tool);
                log.info("[DeepAgent Starter] 通过 class-name [{}] 注册 Tool: {}", className, tool.getCard().getName());
                continue;
            }
            int before = tools.size();
            addToolsFromTarget(tools, target);
            int added = tools.size() - before;
            if (added > 0) {
                List<String> names = new ArrayList<>();
                tools.values().stream().skip(tools.size() - added).forEach(
                    t -> names.add(t instanceof Tool t2 ? t2.getCard().getName() : t.getClass().getSimpleName()));
                log.info("[DeepAgent Starter] 从 class-name [{}] 扫描到 {} 个 Tool: {}", className, added, names);
            }
        }
    }

    /** 从目标对象中提取 @ToolDefinition 方法并注册为工具。 */
    private void addToolsFromTarget(Map<String, Object> tools, Object target) {
        Class<?> targetClass = target instanceof Class<?> clazz ? clazz : ClassUtils.getUserClass(target);
        if (!hasAnnotatedToolMethod(targetClass)) {
            return;
        }
        AnnotatedToolFactory.scan(target).forEach(tool -> tools.put(tool.getCard().getName(), tool));
    }

    /** 检查类是否包含 @ToolDefinition 方法。 */
    private boolean hasAnnotatedToolMethod(Class<?> targetClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ToolDefinition.class)) {
                return true;
            }
        }
        return false;
    }

    /** 根据 className 实例化对象。支持 Tool 子类、@ToolDefinition 实例类和静态方法类。 */
    private Object instantiate(String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = ClassUtils.forName(className, classLoader);
            if (hasStaticAnnotatedToolMethod(clazz)) {
                return clazz;
            }
            if (Tool.class.isAssignableFrom(clazz) || hasAnnotatedToolMethod(clazz)) {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            }
            throw new IllegalArgumentException("配置的工具类没有 Tool 实现或 @ToolDefinition 方法: " + className);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("无法加载配置的工具类: " + className, e);
        }
    }

    /** 检查类是否包含静态 @ToolDefinition 方法（此时不需要实例化类）。 */
    private boolean hasStaticAnnotatedToolMethod(Class<?> targetClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ToolDefinition.class) && Modifier.isStatic(method.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    /** 跳过 starter 自身的 Bean，避免循环依赖。 */
    private boolean shouldSkipBean(String beanName) {
        if ("deepAgent".equals(beanName)
                || "deepAgentClient".equals(beanName)
                || "deepAgentsFactory".equals(beanName)
                || "deepAgentToolResolver".equals(beanName)) {
            return true;
        }
        // 跳过所有正在创建中的业务 Bean，避免循环依赖
        if (beanName.contains("deepAgentDemoApplication") && beanName.contains("Controller")) {
            return true;
        }
        Class<?> type = applicationContext.getType(beanName, false);
        return type == null
                || DeepAgent.class.isAssignableFrom(type)
                || DeepAgentClient.class.isAssignableFrom(type)
                || DeepAgentsFactory.class.isAssignableFrom(type)
                || DeepAgentToolResolver.class.isAssignableFrom(type);
    }
}
