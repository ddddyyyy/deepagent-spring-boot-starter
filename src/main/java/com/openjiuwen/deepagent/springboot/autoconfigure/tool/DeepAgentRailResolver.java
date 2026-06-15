package com.openjiuwen.deepagent.springboot.autoconfigure.tool;

import com.openjiuwen.deepagent.springboot.autoconfigure.annotation.AgentRail;
import com.openjiuwen.deepagent.springboot.autoconfigure.properties.DeepAgentSpringProperties;
import com.openjiuwen.harness.rails.SkillUseRail;
import com.openjiuwen.harness.rails.SysOperationRail;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * DeepAgent Rail 解析器。
 * 负责创建内置 Rail（SkillUseRail / SysOperationRail），
 * 并加载 @AgentRail Bean、配置指定和类名指定的自定义 Rail。
 */
public class DeepAgentRailResolver {
    private final ApplicationContext applicationContext;
    private final DeepAgentSpringProperties properties;

    public DeepAgentRailResolver(ApplicationContext applicationContext, DeepAgentSpringProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    /** 解析所有 Rail 并返回列表。 */
    public List<Object> resolveRails() {
        List<Object> rails = new ArrayList<>();

        if (properties.getRails() != null && properties.getRails().isSkillUseEnabled()) {
            rails.add(new SkillUseRail(properties.getSkillDirectories(), properties.getSkillMode()));
        }
        if (properties.getRails() != null && properties.getRails().isSysOperationEnabled()) {
            rails.add(new SysOperationRail());
        }

        addAnnotatedBeanRails(rails);
        addConfiguredBeanRails(rails);
        addConfiguredClassRails(rails);
        return rails;
    }

    /** 扫描 Spring 容器中带 @AgentRail 的 Bean，自动注入。 */
    private void addAnnotatedBeanRails(List<Object> rails) {
        if (properties.getRails() == null || !properties.getRails().isScanAnnotated()) {
            return;
        }
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            if (shouldSkipBean(beanName)) {
                continue;
            }
            Object bean = applicationContext.getBean(beanName);
            if (bean.getClass().isAnnotationPresent(AgentRail.class)) {
                rails.add(bean);
            }
        }
    }

    /** 按配置中的 beanNames 加载自定义 Rail。 */
    private void addConfiguredBeanRails(List<Object> rails) {
        if (properties.getRails() == null || properties.getRails().getBeanNames() == null) {
            return;
        }
        for (String beanName : properties.getRails().getBeanNames()) {
            if (beanName != null && !beanName.isBlank()) {
                rails.add(applicationContext.getBean(beanName));
            }
        }
    }

    /** 按配置中的 classNames 实例化自定义 Rail。 */
    private void addConfiguredClassRails(List<Object> rails) {
        if (properties.getRails() == null || properties.getRails().getClassNames() == null) {
            return;
        }
        ClassLoader classLoader = applicationContext.getClassLoader();
        for (String className : properties.getRails().getClassNames()) {
            if (className == null || className.isBlank()) {
                continue;
            }
            try {
                Class<?> clazz = ClassUtils.forName(className, classLoader);
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                rails.add(constructor.newInstance());
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("无法加载配置的 Rail 类: " + className, e);
            }
        }
    }

    /** 跳过 starter 自身的 Bean，避免循环依赖。 */
    private boolean shouldSkipBean(String beanName) {
        if ("deepAgent".equals(beanName)
                || "deepAgentClient".equals(beanName)
                || "deepAgentToolResolver".equals(beanName)
                || "deepAgentRailResolver".equals(beanName)) {
            return true;
        }
        // 跳过所有正在创建中的业务 Bean，避免循环依赖
        if (beanName.contains("deepAgentDemoApplication") && beanName.contains("Controller")) {
            return true;
        }
        Class<?> type = applicationContext.getType(beanName, false);
        return type == null || type.getName().startsWith("org.springframework");
    }
}
