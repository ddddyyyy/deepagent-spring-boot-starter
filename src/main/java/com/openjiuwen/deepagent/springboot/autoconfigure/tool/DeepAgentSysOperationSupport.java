package com.openjiuwen.deepagent.springboot.autoconfigure.tool;

import com.openjiuwen.core.sysop.OperationMode;
import com.openjiuwen.core.sysop.SysOperation;
import com.openjiuwen.core.sysop.SysOperationCard;
import com.openjiuwen.core.sysop.SysOperationToolAdapter;
import com.openjiuwen.core.sysop.config.LocalWorkConfig;
import com.openjiuwen.deepagent.springboot.autoconfigure.properties.DeepAgentSpringProperties;
import com.openjiuwen.harness.deep_agent.DeepAgent;

import java.util.List;

/**
 * SysOperation 创建与工具注入支持。
 * SysOperationRail.init() 在当前版本中为空实现，
 * 这个配置器负责手动创建 SysOperation 并注册其工具到 DeepAgent。
 */
public final class DeepAgentSysOperationSupport {
    private DeepAgentSysOperationSupport() {
    }

    /** 根据配置创建 SysOperation。如果 sys-operation 关闭，返回 null。 */
    public static SysOperation createSysOperation(DeepAgentSpringProperties properties) {
        if (properties.getSysOperation() == null || !properties.getSysOperation().isEnabled()) {
            return null;
        }
        return new SysOperation(toSysOperationCard(properties));
    }

    /** 将 SysOperation 中的操作工具（文件、Shell、代码等）注册到 DeepAgent。 */
    public static void injectSysOperationTools(
            DeepAgent deepAgent,
            DeepAgentSpringProperties properties,
            SysOperation sysOperation
    ) {
        if (deepAgent == null
                || sysOperation == null
                || properties.getSysOperation() == null
                || !properties.getSysOperation().isInjectTools()) {
            return;
        }
        SysOperationCard card = toSysOperationCard(properties);
        for (SysOperationToolAdapter.ToolEntry entry : SysOperationToolAdapter.extractTools(card, sysOperation)) {
            deepAgent.registerHarnessTool(entry.localFunction());
        }
    }

    private static SysOperationCard toSysOperationCard(DeepAgentSpringProperties properties) {
        DeepAgentSpringProperties.SysOperationProperties sys = properties.getSysOperation();
        return SysOperationCard.builder()
                .id(sys.getId())
                .name(sys.getName())
                .mode(OperationMode.LOCAL)
                .workConfig(toLocalWorkConfig(properties))
                .build();
    }

    private static LocalWorkConfig toLocalWorkConfig(DeepAgentSpringProperties properties) {
        DeepAgentSpringProperties.SysOperationProperties sys = properties.getSysOperation();
        LocalWorkConfig.LocalWorkConfigBuilder builder = LocalWorkConfig.builder()
                .workDir(resolveWorkDir(properties))
                .restrictToSandbox(sys.isRestrictToSandbox());
        if (sys.getShellAllowlist() != null && !sys.getShellAllowlist().isEmpty()) {
            builder.shellAllowlist(sys.getShellAllowlist());
        }
        if (sys.getSandboxRoot() != null && !sys.getSandboxRoot().isEmpty()) {
            builder.sandboxRoot(sys.getSandboxRoot());
        }
        if (sys.getDangerousPatterns() != null && !sys.getDangerousPatterns().isEmpty()) {
            builder.dangerousPatterns(sys.getDangerousPatterns());
        }
        return builder.build();
    }

    /** 获取工作目录：优先 sys-operation.work-dir，否则 workspace-path。 */
    private static String resolveWorkDir(DeepAgentSpringProperties properties) {
        String workDir = properties.getSysOperation().getWorkDir();
        if (workDir != null && !workDir.isBlank()) {
            return workDir;
        }
        return properties.getWorkspacePath();
    }
}
