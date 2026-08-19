package com.example.backend.service;

public interface OcrAsyncService {
    /**
     * 同步执行 OCR 识别，返回是否成功（供 stream 消费者决定 ack / 重投 / 死信）。
     */
    boolean processOcrAsync(Long recordId);
}