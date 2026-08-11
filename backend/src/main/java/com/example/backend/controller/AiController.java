package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.config.BaiduTtsConfig;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.model.dto.FollowUpQuestionRequest;
import com.example.backend.service.BaiduTtsService;
import com.example.backend.service.DeepSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired(required = false)
    private BaiduTtsService baiduTtsService;

    @Autowired
    private BaiduTtsConfig baiduTtsConfig;

    @Autowired(required = false)
    private com.example.backend.service.BaiduAsrService baiduAsrService;

    /**
     * 语音识别（ASR）：老人语音输入 → 文字
     * multipart 表单上传，字段名 file；返回 {text}
     */
    @PostMapping("/asr")
    public ResponseResult<Map<String, String>> speechToText(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (baiduAsrService == null) {
            return ResponseResult.fail("语音识别服务暂不可用");
        }
        if (file == null || file.isEmpty()) {
            return ResponseResult.fail("音频文件为空");
        }
        String text = baiduAsrService.recognize(file);
        if (text == null || text.isEmpty()) {
            return ResponseResult.fail("语音识别失败，请重试或使用文字输入");
        }
        Map<String, String> data = new java.util.HashMap<>();
        data.put("text", text);
        return ResponseResult.success(data);
    }

    /**
     * ASR 能力状态：前端据此决定用百度识别还是降级浏览器 Web Speech API
     */
    @GetMapping("/asr/config")
    public ResponseResult<Map<String, Object>> asrConfig() {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("baiduAsrEnabled", baiduAsrService != null && baiduAsrService.isConfigured());
        return ResponseResult.success(data);
    }

    /**
     * 生成老年友好版本的用药指导
     *
     * @param drugDetail 药品详细信息
     * @return 老年友好的用药指导文本
     */
    @PostMapping("/elderly-guide")
    public ResponseResult<String> generateElderlyGuide(@RequestBody DrugDetailResponse drugDetail) {
        String guide = deepSeekService.generateElderlyFriendlyGuide(drugDetail);
        return ResponseResult.success(guide);
    }

    /**
     * 药品追问接口
     * 用户在查看用药说明后，可以对当前药品进行追问
     *
     * @param request 包含药品信息、用户问题和对话历史
     * @return AI回答
     */
    @PostMapping("/follow-up-question")
    public ResponseResult<String> followUpQuestion(@RequestBody FollowUpQuestionRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseResult.fail("问题不能为空");
        }
        String answer = deepSeekService.answerFollowUpQuestion(
                request.getDrugDetail(),
                request.getQuestion(),
                request.getConversationHistory()
        );
        if (answer != null) {
            return ResponseResult.success(answer);
        } else {
            return ResponseResult.fail("AI服务暂时不可用，请稍后再试");
        }
    }

    /**
     * 将文本转换为语音
     *
     * @param text 要转换的文本
     * @param speechRate 语速 (0-15，5为正常语速，3为较慢适合老年人)
     * @return Base64编码的音频数据
     */
    @GetMapping("/tts")
    public ResponseResult<String> textToSpeech(
            @RequestParam String text,
            @RequestParam(defaultValue = "5") int speechRate) {
        logger.debug("百度TTS配置已加载: appIdPresent={}, apiKeyPresent={}, secretKeyPresent={}",
                baiduTtsConfig.getAppId() != null && !baiduTtsConfig.getAppId().isBlank(),
                baiduTtsConfig.getApiKey() != null && !baiduTtsConfig.getApiKey().isBlank(),
                baiduTtsConfig.getSecretKey() != null && !baiduTtsConfig.getSecretKey().isBlank());

        if (baiduTtsService == null) {
            logger.error("BaiduTtsService 为 null");
            return ResponseResult.fail("语音服务暂不可用");
        }

        String audioData = baiduTtsService.textToSpeech(text, speechRate);

        if (audioData != null) {
            return ResponseResult.success(audioData);
        } else {
            logger.error("百度TTS返回空数据");
            return ResponseResult.fail("语音转换失败，请稍后重试");
        }
    }

    /**
     * 测试药品分类（处方药/非处方药）
     */
    @GetMapping("/classify-drug")
    public ResponseResult<String> classifyDrug(@RequestParam String drugName) {
        String category = deepSeekService.classifyDrugCategory(drugName);
        if (category != null) {
            return ResponseResult.success(category);
        } else {
            return ResponseResult.fail("AI分类失败，返回null");
        }
    }

    /**
     * 测试百度TTS配置
     */
    @GetMapping("/tts/test")
    public ResponseResult<String> testTtsConfig() {
        return ResponseResult.success("百度TTS配置 - appId: " + baiduTtsConfig.getAppId() +
                ", apiKey: " + (baiduTtsConfig.getApiKey() != null ? "*****" : "null") +
                ", secretKey: " + (baiduTtsConfig.getSecretKey() != null ? "*****" : "null"));
    }
}

