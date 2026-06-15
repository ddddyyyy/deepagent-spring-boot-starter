package com.openjiuwen.deepagent.springboot.autoconfigure.prompt;

/**
 * 提示词提供者接口。
 * 如果容器中存在此 Bean，DeepAgent 将使用它提供的提示词，
 * 否则回退到 deep-agent.system-prompt 或 deep-agent.react-agent.prompt 配置。
 *
 * <p>使用示例：从文件加载提示词
 * <pre>{@code
 * @Component
 * public class FilePromptProvider implements PromptProvider {
 *     @Override
 *     public String getSystemPrompt() {
 *         try {
 *             return Files.readString(Path.of("/etc/deepagent/prompt.md"));
 *         } catch (IOException e) {
 *             throw new RuntimeException(e);
 *         }
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface PromptProvider {

    /** 返回系统提示词。返回 null 或空字符串则回退到配置文件。 */
    String getSystemPrompt();
}
