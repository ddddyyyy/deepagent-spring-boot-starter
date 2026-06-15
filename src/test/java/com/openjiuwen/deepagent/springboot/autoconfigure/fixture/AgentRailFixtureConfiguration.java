package com.openjiuwen.deepagent.springboot.autoconfigure.fixture;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AgentRailFixtureConfiguration {
    @Bean
    AnnotatedCustomRail annotatedCustomRail() {
        return new AnnotatedCustomRail();
    }
}
