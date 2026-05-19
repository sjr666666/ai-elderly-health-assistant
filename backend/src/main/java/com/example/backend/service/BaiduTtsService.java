package com.example.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 百度语音合成服务接口
 */
public interface BaiduTtsService {

    /**
     * 将文本转换为语音
     *
     * @param text 要转换的文本
     * @param speechRate 语速 (0-15，5为正常)
     * @return 语音文件URL或Base64编码的音频数据
     */
    String textToSpeech(String text, int speechRate);

    /**
     * 将文本转换为语音文件
     *
     * @param text 要转换的文本
     * @param speechRate 语速 (0-15，5为正常)
     * @return 语音文件对象
     */
    MultipartFile textToSpeechFile(String text, int speechRate);
}
