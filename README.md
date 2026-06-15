# DeepAgent Spring Boot Starter

基于 Spring Boot 3.5.7 + JDK 17 的 DeepAgent 配置化 SDK，支持通过 `application.yml` 完成完整初始化链路。

## 代码结构

```text
annotation/
  AgentRail                         将 Spring Bean 标记为 DeepAgent Rail 的注解
autoconfigure/
  DeepAgentAutoConfiguration        自动装配入口
properties/
  DeepAgentSpringProperties         deep-agent.* 配置映射
client/
  DeepAgentClient                   业务调用门面
request/
  DeepAgentRequest                  调用请求对象
tool/
  DeepAgentToolResolver             Tool 扫描与配置解析
  DeepAgentHeaderConfigurer         内部 ReActAgent 配置（模型客户端、自定义 header、提示词）
  PromptProvider                   提示词提供者接口，支持从文件/数据库/远程加载系统提示词
  DeepAgentRailResolver             Rail 解析与创建
  DeepAgentSysOperationSupport      SysOperation 创建与工具注入
```

## 依赖方式

先在本地安装主 SDK：

```bash
mvn -DskipTests install
```

再安装 starter：

```bash
cd examples/deep_agent_spring_boot
mvn -DskipTests install
```

业务 Spring Boot 项目引入：

```xml
<dependency>
    <groupId>com.openjiuwen</groupId>
    <artifactId>deepagent-spring-boot-starter</artifactId>
    <version>0.1.12-SNAPSHOT</version>
</dependency>
```

## 完整配置示例

```yaml
deep-agent:
  enabled: true
  language: cn
  workspace-path: ./workspace
  system-prompt: |
    你是一个中文智能体助手，请基于用户问题给出准确、简洁、可执行的回答。
  max-iterations: 15
  task-loop-enabled: true
  task-planning-enabled: false
  restrict-to-work-dir: false
  ensure-initialized: false

  # AgentCard 信息
  agent-card:
    id: deep_agent
    name: DeepAgent
    description: Dynamic Planning Deep Agent

  # 系统操作配置（文件读写、shell、代码执行）
  sys-operation:
    enabled: true
    id: deep_agent_sys_operation
    name: deep_agent_sys_operation
    work-dir: ./workspace
    restrict-to-sandbox: false
    inject-tools: true
    shell-allowlist:
      - echo
      - ls
      - cat
      - curl
    sandbox-root:
      - /tmp/sandbox
    dangerous-patterns:
      - rm.*-rf.*/

  # 内置 Rail 控制
  rails:
    skill-use-enabled: true
    sys-operation-enabled: true
    scan-annotated: true
    bean-names:
      - taskExecutionRail
    class-names:
      - com.example.plugin.CustomRail

  # 模型配置（唯一来源）
  backend:
    client_provider: openai
    api_key: ${DEEPSEEK_API_KEY}
    api_base: https://api.deepseek.com
  model:
    model: deepseek-v4-flash
    thinking:
      type: disabled

  # 自定义请求 header，写入内部 ReActAgentConfig.customHeaders
  headers:
    X-Tenant-Id: tenant-a
    X-Trace-Source: spring-boot

  # DeepAgent 内部行为控制
  react-agent:
    enabled: true
    clear-llm: true
    prompt: |
      你是一个中文智能体助手，请基于用户问题给出准确、简洁、可执行的回答。

  # Skill 目录
  skill-directories:
    - ./skills
  skill-mode: auto_list

  # Tool 注册方式
  tools:
    scan-annotated-beans: true
    bean-names:
      - orderTools
    class-names:
      - com.example.agent.tools.CommonTools

  # 流式模式
  stream-modes:
    - OUTPUT

  # 额外 Prompt 段落
  extra-prompt-sections:
    - name: custom_guidance
      priority: 60
      content:
        cn: |
          请优先使用本地 skill 处理问题。


## 初始化链路

按以下顺序初始化，等价于手写 `AgentCard` → `DeepAgentConfig` → `Workspace` → `HarnessFactory.createDeepAgent(card, config, workspace)` → `ReActAgentConfig` 配置：

```text
读取 deep-agent.* 配置
  -> 创建 AgentCard
  -> 创建 Workspace
  -> 创建 SysOperation 并注册工具
  -> 创建 SkillUseRail / SysOperationRail / 自定义 Rail
  -> 创建 DeepAgentConfig
  -> HarnessFactory.createDeepAgent(card, config, workspace)
  -> 配置内部 ReActAgent（提示词、模型客户端、headers）
  -> 可选 ensureInitialized()
```

模型配置使用 `backend` 和 `model` 作为唯一来源。`react-agent` 只控制内部行为：

- `prompt`：如果设置，替换系统提示词。
- `clear-llm`：设为 `true` 时将 `inner.setLlm(null)`，让内部 agent 重新按 config 使用模型客户端。
- 自定义 header 通过 `deep-agent.headers` 写入 `ReActAgentConfig.customHeaders`。

## Rail 注册

支持三种方式。

### 方式一：注解自动扫描（推荐）

在 Spring Bean 类上添加 `@AgentRail`，继承 `DeepAgentRail`：

```java
import com.openjiuwen.deepagent.springboot.autoconfigure.annotation.AgentRail;
import com.openjiuwen.harness.rails.DeepAgentRail;
import org.springframework.stereotype.Component;

@Component
@AgentRail
public class TaskValidationRail extends DeepAgentRail {
    @Override
    public int priority() {
        return 80;
    }
}
```

默认开启扫描：

```yaml
deep-agent:
  rails:
    scan-annotated: true
```

### 方式二：配置文件指定 Spring Bean

```yaml
deep-agent:
  rails:
    bean-names:
      - taskExecutionRail
```

### 方式三：配置文件指定 className

```yaml
deep-agent:
  rails:
    class-names:
      - com.example.plugin.CustomRail
```


## Skill 配置

Skill 是 DeepAgent 的一种能力扩展机制，每个 Skill 是一个包含 SKILL.md 的目录。Agent 在推理过程中可以按需调用 Skill 来执行特定任务。

### SkillUseRail

`SkillUseRail` 是 DeepAgent 的内置 Rail，负责加载技能目录下的所有 SKILL.md，并向 Agent 注入两个工具：

- **list_skill** — 列出当前可用的所有技能
- **skill_tool** — 读取某个技能的具体内容

同时，`SkillUseRail` 会把技能列表写入系统提示词，让 LLM 知道有哪些技能可用，并在需要时主动调用上述工具来获取并执行技能。

### 配置项

| 配置路径 | 说明 | 默认值 |
|---|---|---|
| `deep-agent.skill-directories` | Skill 目录列表，`SkillUseRail` 会扫描这些目录下的 SKILL.md | `[]` |
| `deep-agent.skill-mode` | Skill 筛选模式，可选 `all`（全部可用）或 `auto_list`（根据提示词自动选择） | `auto_list` |
| `deep-agent.rails.skill-use-enabled` | 是否启用 `SkillUseRail` | `true` |

`skill-directories` 和 `skill-mode` 会被同时传递给 `SkillUseRail` 和 `DeepAgentConfig`，两者共用同一份数据源。

### Skill 目录结构

每个 Skill 目录下必须包含 `SKILL.md` 文件，描述该技能的用途、入参、执行流程和注意事项。

```text
skills/
  code-review/
    SKILL.md          # 代码审查技能说明
  data-analyzer/
    SKILL.md          # 数据分析技能说明
  shell-executor/
    SKILL.md          # Shell 命令执行技能说明
```

`SKILL.md` 示例（`skills/code-review/SKILL.md`）：

```markdown
# 代码审查技能

分析指定目录下的代码质量、风格一致性和潜在缺陷。

## 使用场景
- 审查 Pull Request 中的代码变更
- 检查代码是否符合团队规范
- 识别性能隐患和反模式
```

### 执行流程

1. `SkillUseRail.init()` 启动时扫描 `skill-directories` 下所有目录，加载 SKILL.md
2. 技能列表被写入系统提示词，LLM 在推理时知道哪些技能可用
3. LLM 调用 `list_skill` 查看技能列表，再用 `skill_tool` 读取具体技能内容
4. 按 SKILL.md 中的流程执行对应任务

> 关闭 `rails.skill-use-enabled: false` 后，Agent 将不再加载任何技能，`list_skill` 和 `skill_tool` 也不会被注入。

## Tool 注册

支持两种方式。

### 方式一：注解扫描（默认开启）

> **注意**：注解扫描基于 Spring Bean 进行，带 `@ToolDefinition` 的类必须先注册为 Spring Bean（如添加 `@Component`），否则不会被扫描到。
> 如果类无法注册为 Bean，请使用方式二（`class-names` 配置）。

在 Spring Bean 方法上使用 `@ToolDefinition`：

```java
import com.openjiuwen.core.foundation.tool.annotation.ToolDefinition;
import org.springframework.stereotype.Component;

@Component
public class OrderTools {
    @ToolDefinition(name = "query_order", description = "根据订单号查询订单状态。")
    public String queryOrder(String orderId) {
        return "订单 " + orderId + " 已支付";
    }
}
```

### 方式二：配置文件指定

```yaml
deep-agent:
  tools:
    scan-annotated-beans: false
    bean-names:
      - orderTools
    class-names:
      - com.example.agent.tools.CommonTools
```

`bean-names` 支持两类：

- Bean 本身是 `Tool` 子类，直接注册。
- Bean 里包含 `@ToolDefinition` 方法，转成 `LocalFunction`。

`class-names` 支持三类：

- 类继承 `Tool`，通过无参构造函数创建。
- 类包含 `@ToolDefinition` 方法，通过无参构造函数创建实例后扫描。
- 类只包含 `static @ToolDefinition` 方法，按类直接扫描。

## 业务代码使用

非流式调用：

```java
@Service
public class ChatService {
    private final DeepAgentClient deepAgentClient;

    public ChatService(DeepAgentClient deepAgentClient) {
        this.deepAgentClient = deepAgentClient;
    }

    public Map<String, Object> chat(String sessionId, String question) {
        return deepAgentClient.invoke(DeepAgentRequest.builder()
                .conversationId(sessionId)
                .query(question)
                .build());
    }
}
```

流式调用：

```java
Iterator<Object> iterator = deepAgentClient.stream(DeepAgentRequest.builder()
        .conversationId("user-10001-session-001")
        .query("请分析这个问题")
        .build());

while (iterator.hasNext()) {
    Object event = iterator.next();
    // 在这里写 SSE / WebSocket
}
```

也可直接注入原生对象：

```java
@Autowired
private DeepAgent deepAgent;

public void chat(String sessionId, String question) {
    deepAgent.invoke(Map.of(
            "query", question,
            "conversation_id", sessionId,
            "LANGUAGE", "cn"
    ));
}
```

## 配置项说明

### 基础设置

| 配置路径 | 说明 | 默认值 |
|---|---|---|
| `deep-agent.enabled` | 是否启用 DeepAgent 自动装配 | `true` |
| `deep-agent.language` | 智能体语言，可选 `cn`（中文）或 `en`（英文） | `cn` |
| `deep-agent.workspace-path` | 智能体工作目录路径 | `./` |
| `deep-agent.system-prompt` | 系统提示词 | （默认提示词） |
| `deep-agent.max-iterations` | 最大迭代次数 | `15` |
| `deep-agent.task-loop-enabled` | 是否启用任务循环模式 | `true` |
| `deep-agent.task-planning-enabled` | 是否启用任务规划模式 | `false` |
| `deep-agent.restrict-to-work-dir` | 是否限制智能体只在工作目录内操作 | `true` |
| `deep-agent.ensure-initialized` | 是否在启动时调用 ensureInitialized() | `false` |

### 异步与多智能体

| 配置路径 | 说明 | 默认值 |
|---|---|---|
| `deep-agent.async-subagent-enabled` | 启用异步子智能体，允许主智能体并发调度多个子任务 | `false` |
| `deep-agent.general-purpose-agent-enabled` | 启用通用智能体，作为备用智能体处理主智能体无法处理的任务 | `false` |

### 超时控制

| 配置路径 | 说明 | 默认值 |
|---|---|---|
| `deep-agent.completion-timeout` | 智能体单次完成的超时时间（秒），超过后自动终止 | 无（不超时） |

### 流式输出

| 配置路径 | 说明 | 默认值 |
|---|---|---|
| `deep-agent.stream-modes` | 流式输出模式列表，可选 `OUTPUT`、`THINKING`、`TOOL_CALL`、`FULL` | `[OUTPUT]` |

### 额外 Prompt 段落

通过 `extra-prompt-sections` 可以向系统提示词中插入自定义段落，控制智能体的行为倾向。

```yaml
deep-agent:
  extra-prompt-sections:
    - name: custom_guidance
      priority: 60
      content:
        cn: |
          请优先使用本地 skill 处理问题。
        en: |
          Please prioritize local skills for problem solving.
```

每个段落包含：

- `name`：段落名称，用于程序识别和替换
- `priority`：优先级，数值越大越靠前
- `content.cn`：中文提示内容
- `content.en`：英文提示内容（可选，不配置时中文也用于英文模式）


## 自定义提示词

如果系统提示词需要从文件、数据库或远程接口动态加载，可以实现 `PromptProvider` 接口并注册为 Spring Bean。

```java
@Component
public class FilePromptProvider implements PromptProvider {
    @Override
    public String getSystemPrompt() {
        try {
            return Files.readString(Path.of("/etc/deepagent/prompt.md"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

优先级顺序：**PromptProvider Bean → react-agent.prompt 配置 → system-prompt 配置**。
如果 `PromptProvider` 返回 null 或空字符串，自动回退到配置文件。

## 设计约定


- 默认单例，不需要每个请求重新初始化。
- `conversationId` 为必填，不同会话使用不同的 ID，避免上下文串。
- `LANGUAGE` 随配置写入 inputs，默认 `cn`。
- starter 不直接依赖 Spring MVC，不内置 Controller；业务方自行选择 REST、SSE、WebSocket。
- DeepAgent 当前顶层 `stream` 不是完整 token 级实时流式，适合迭代式结果输出。
- 自定义 header 通过 `deep-agent.headers` 配置，最终写入内部 `ReActAgentConfig.customHeaders`，不污染 `backend`。
- 模型配置使用 `deep-agent.backend` + `deep-agent.model` 作为唯一来源，`react-agent` 不包含模型字段。
