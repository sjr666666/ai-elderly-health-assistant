package com.example.backend.controller;

import com.example.backend.common.ResponseResult;
import com.example.backend.config.BaiduTtsConfig;
import com.example.backend.model.dto.DrugDetailResponse;
import com.example.backend.service.BaiduTtsService;
import com.example.backend.service.DeepSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired(required = false)
    private BaiduTtsService baiduTtsService;

    @Autowired
    private BaiduTtsConfig baiduTtsConfig;

    /**
     * 生成老年友好版本的用药指导
     *
     * @param drugDetail 药品详细信息
     * @return 老年友好的用药指导文本
     */
    @PostMapping("/elderly-guide")
    public ResponseResult<String> generateElderlyGuide(@RequestBody DrugDetailResponse drugDetail) {
        try {
            String guide = deepSeekService.generateElderlyFriendlyGuide(drugDetail);
            return ResponseResult.success(guide);
        } catch (Exception e) {
            return ResponseResult.fail("生成用药指导失败: " + e.getMessage());
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
        try {
            logger.info("百度TTS配置 - appId: {}, apiKey: {}, secretKey: {}",
                    baiduTtsConfig.getAppId(),
                    baiduTtsConfig.getApiKey(),
                    baiduTtsConfig.getSecretKey() != null ? "*****" : "null");

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
        } catch (Exception e) {
            logger.error("语音转换异常: ", e);
            return ResponseResult.fail("语音转换失败: " + e.getMessage());
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
