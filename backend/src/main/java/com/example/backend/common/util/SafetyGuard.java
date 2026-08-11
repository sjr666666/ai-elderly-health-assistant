package com.example.backend.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * AI 安全防护工具：统一免责声明 + prompt 注入 / 危险请求检测
 * <p>
 * 背景：健康类 AI 回答必须可问责、可兜底。本工具统一两件事：
 * <ol>
 *   <li><b>统一免责声明</b>：所有 AI 健康回答的兜底文案（DISCLAIMER），
 *       回答本身未带"遵医嘱"字样时自动追加，保证输出合规</li>
 *   <li><b>prompt 注入防护</b>：检测用户输入中的注入攻击特征
 *       （诱导模型忽略系统指令 / 泄露系统提示词）与危险请求
 *       （索要致死剂量 / 自伤 / 伤害他人），命中时<b>不调用 LLM</b>，
 *       直接返回固定拒绝文案并引导就医——比"先调用再过滤输出"更省成本也更安全</li>
 * </ol>
 */
public final class SafetyGuard {

    private static final Logger logger = LoggerFactory.getLogger(SafetyGuard.class);

    /** 统一免责声明：健康类 AI 回答的兜底文案 */
    public static final String DISCLAIMER =
            "以上信息仅供参考，不能替代专业医生的诊断和治疗，具体用药请遵医嘱。如有紧急情况，请立即拨打120。";

    /** prompt 注入攻击模式（诱导忽略系统指令 / 泄露系统提示词） */
    private static final Pattern[] INJECTION_PATTERNS = {
            // 中文：忽略/忘掉指令
            Pattern.compile("忽略(之前|以上|所有|系统|你的)?[^。；\\n]*?(指令|设定|规则|提示词|限制)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("忘掉?(之前|以上|所有|系统|你的)?[^。；\\n]*?(指令|设定|规则|提示词)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(不要|别|无需|不用)遵守(规则|指令|设定)"),
            // 中文：泄露系统提示词
            Pattern.compile("(输出|重复|展示|告诉我|透露|打印).*(系统提示|系统指令|system prompt|初始化提示|你的设定|你的规则|提示词|prompt)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(你现在|你是|扮演)[^。；\\n]{0,20}(不需要|无需|不用)[^。；\\n]{0,20}(遵守|遵循|受限|限制)"),
            Pattern.compile("越狱|jailbreak|DAN模式", Pattern.CASE_INSENSITIVE),
            // 英文
            Pattern.compile("ignore\\s+(all\\s+|any\\s+|the\\s+|previous\\s+)*(instructions|prompts|rules|system)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(reveal|show|print|repeat|output)\\s+(your\\s+|the\\s+)?(system\\s+)?(prompt|instructions)", Pattern.CASE_INSENSITIVE),
    };

    /** 危险请求模式（索要致死剂量 / 自伤 / 伤害他人）。刻意避开"这药会毒死人吗"类正当担忧提问 */
    private static final Pattern[] DANGEROUS_PATTERNS = {
            // 方法索取：怎么/如何 + 致死/自伤动作
            Pattern.compile("(怎么|如何|用什么|怎么用).{0,10}(毒死|杀死|害死|自杀|自尽|轻生)"),
            // 自伤意图 + 手段
            Pattern.compile("(自杀|自尽|轻生).{0,15}(药|剂量|方法|怎么|多少)"),
            // 剂量索取：多少/什么剂量 + 能死/会死/致死
            Pattern.compile("(多少|什么剂量).{0,12}(能死|会死|致死)"),
            // 获取致命物质
            Pattern.compile("(买|获取|弄到).{0,8}(毒药|致命药物)"),
            Pattern.compile("killing\\s+dose|lethal\\s+dose|how\\s+to\\s+die", Pattern.CASE_INSENSITIVE),
    };

    private SafetyGuard() {
    }

    /**
     * 检查用户输入是否安全
     *
     * @param userInput 用户输入（原始问题，不含系统提示）
     * @return true = 安全放行；false = 命中注入攻击或危险请求
     */
    public static boolean isSafe(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return true;
        }
        String text = userInput.trim();
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(text).find()) {
                logger.warn("[SafetyGuard] 检测到 prompt 注入特征 - 模式: {}, 输入: {}", p.pattern(), abbr(text));
                return false;
            }
        }
        for (Pattern p : DANGEROUS_PATTERNS) {
            if (p.matcher(text).find()) {
                logger.warn("[SafetyGuard] 检测到危险请求 - 模式: {}, 输入: {}", p.pattern(), abbr(text));
                return false;
            }
        }
        return true;
    }

    /** 注入/危险请求的固定拒绝文案（不调用 LLM，直接返回） */
    public static String refusalMessage() {
        return "为了您的健康安全，这个问题我无法回答。如果您或家人感到不适，请及时联系医生或拨打120急救电话。";
    }

    /**
     * 为回答追加统一免责声明（回答已含"遵医嘱"字样则跳过，避免重复）
     */
    public static String appendDisclaimer(String answer) {
        if (answer == null || answer.isEmpty()) {
            return answer;
        }
        if (answer.contains("遵医嘱")) {
            return answer;
        }
        return answer + "\n\n" + DISCLAIMER;
    }

    private static String abbr(String s) {
        return s.length() <= 60 ? s : s.substring(0, 60) + "…";
    }
}
