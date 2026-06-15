package com.openjiuwen.deepagent.springboot.autoconfigure.fixture;

import com.openjiuwen.deepagent.springboot.autoconfigure.annotation.AgentRail;
import com.openjiuwen.harness.rails.DeepAgentRail;

@AgentRail
public class AnnotatedCustomRail extends DeepAgentRail {
    @Override
    public int priority() {
        return 80;
    }
}
