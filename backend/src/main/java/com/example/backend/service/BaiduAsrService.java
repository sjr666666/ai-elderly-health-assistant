package com.example.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 百度语音识别（ASR）服务接口
 * 老人语音输入：把录音（wav/pcm）转成文字，实现"语音问药"（当前只有 TTS 播报）
 */
public interface BaiduAsrService {

    /**
     * 语音识别：音频 → 文字
     *
     * @param file   录音文件（wav/pcm，16k 单声道最佳；≤60 秒 ≤4MB）
     * @return 识别出的文字；未配 Key / 识别失败返回 null
     */
    String recognize(MultipartFile file);

    /**
     * 是否已配置（API Key + Secret Key 齐全）
     */
    boolean isConfigured();
}
