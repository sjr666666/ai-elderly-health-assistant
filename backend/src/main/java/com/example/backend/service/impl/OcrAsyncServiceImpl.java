package com.example.backend.service.impl;

import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.OcrRecordMapper;
import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.OcrRecord;
import com.example.backend.service.DrugRecognitionService;
import com.example.backend.service.OcrAsyncService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Iterator;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.example.backend.config.BaiduOcrConfig;

@Service
public class OcrAsyncServiceImpl implements OcrAsyncService {

    private static final Logger logger = LoggerFactory.getLogger(OcrAsyncServiceImpl.class);

    private final OcrRecordMapper ocrRecordMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final BaiduOcrConfig baiduOcrConfig;
    private final ObjectMapper objectMapper;
    private final DrugRecognitionService drugRecognitionService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    private static final String ACCURATE_OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic";

    @Autowired
    public OcrAsyncServiceImpl(
            OcrRecordMapper ocrRecordMapper,
            DrugBaseMapper drugBaseMapper,
            BaiduOcrConfig baiduOcrConfig,
            ObjectMapper objectMapper,
            DrugRecognitionService drugRecognitionService) {
        this.ocrRecordMapper = ocrRecordMapper;
        this.drugBaseMapper = drugBaseMapper;
        this.baiduOcrConfig = baiduOcrConfig;
        this.objectMapper = objectMapper;
        this.drugRecognitionService = drugRecognitionService;
    }

    @Override
    @Async("taskExecutor")
    public void processOcrAsync(Long recordId) {
        logger.info("========== 开始异步OCR处理 ========== - recordId: {}, 线程: {}", recordId, Thread.currentThread().getName());

        try {
            logger.info("步骤1: 查询OCR记录 - recordId: {}", recordId);
            OcrRecord ocrRecord = ocrRecordMapper.selectById(recordId);

            if (ocrRecord == null) {
                logger.error("步骤1失败: 未找到OCR记录 - recordId: {}", recordId);
                return;
            }

            String imageUrl = ocrRecord.getImageUrl();
            logger.info("步骤1成功: 找到OCR记录 - imageUrl: {}", imageUrl);

            String rawText = null;

            logger.info("步骤2: 开始调用百度高精度OCR - recordId: {}, imageUrl: {}", recordId, imageUrl);
            rawText = performOcr(imageUrl);
            logger.info("步骤2成功: 高精度OCR识别完成 - recordId: {}, rawText长度: {} characters", recordId, 
                    rawText != null ? rawText.length() : 0);

            logger.info("步骤3: 设置识别文本到记录");
            ocrRecord.setRawText(rawText);

            logger.info("步骤4: 调用药品识别服务 - recordId: {}", recordId);
            OcrResultResponse recognitionResult = drugRecognitionService.processRecognition(recordId, rawText);

            if (recognitionResult != null) {
                ocrRecord.setStatus(recognitionResult.getStatus());
                if (recognitionResult.getMatchedDrugId() != null) {
                    ocrRecord.setMatchedDrugId(recognitionResult.getMatchedDrugId());
                    ocrRecord.setMatchScore(recognitionResult.getMatchScore());
                }
                logger.info("步骤4成功: 药品识别服务处理完成 - recordId: {}, status: {}, drugId: {}",
                        recordId, recognitionResult.getStatus(), recognitionResult.getMatchedDrugId());
            } else {
                if (rawText == null || rawText.isEmpty()) {
                    ocrRecord.setStatus(OcrRecord.Status.UNMATCHED.getCode());
                    logger.info("步骤4结果: 未能识别出文字 - recordId: {}", recordId);
                } else {
                    ocrRecord.setStatus(OcrRecord.Status.UNMATCHED.getCode());
                    logger.info("步骤4结果: 药品识别服务返回null - recordId: {}", recordId);
                }
            }

            logger.info("步骤5: 更新OCR记录到数据库 - recordId: {}", recordId);
            ocrRecordMapper.updateById(ocrRecord);
            logger.info("步骤5成功: OCR记录更新完成 - recordId: {}", recordId);

            logger.info("========== 异步OCR处理完成 ========== - recordId: {}", recordId);

        } catch (Exception e) {
            logger.error("========== OCR异步处理失败 ========== - recordId: {}", recordId);
            logger.error("错误类型: {}", e.getClass().getName());
            logger.error("错误消息: {}", e.getMessage());
            logger.error("详细堆栈:", e);
            
            // 打印完整堆栈
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                stackTrace.append("\tat ").append(element.toString()).append("\n");
            }
            logger.error("完整堆栈:\n{}", stackTrace.toString());
            
            try {
                OcrRecord ocrRecord = ocrRecordMapper.selectById(recordId);
                if (ocrRecord != null) {
                    ocrRecord.setStatus(OcrRecord.Status.FAILED.getCode());
                    ocrRecordMapper.updateById(ocrRecord);
                    logger.info("已更新OCR记录为失败状态 - recordId: {}", recordId);
                }
            } catch (Exception ex) {
                logger.error("更新OCR状态失败 - recordId: {}, 错误: {}", recordId, ex.getMessage());
            }
        }
    }

    private String performOcr(String imagePath) throws Exception {
        logger.info("开始获取百度OCR AccessToken...");
        String accessToken = getAccessToken();
        logger.info("AccessToken获取成功");

        byte[] imageData = readImageData(imagePath);
        String imageBase64 = Base64.getEncoder().encodeToString(imageData);

        String url = ACCURATE_OCR_URL + "?access_token=" + accessToken;
        logger.info("开始调用百度高精度OCR API, URL: {}", url);

        String encodedImage = URLEncoder.encode(imageBase64, "UTF-8");
        
        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("image=" + encodedImage)
                .timeout(30000)
                .execute();

        String responseBody = response.body();
        logger.info("百度OCR响应状态码: {}, 响应体长度: {} bytes", response.getStatus(), responseBody.length());

        if (response.getStatus() != 200) {
            throw new RuntimeException("百度OCR HTTP错误: " + response.getStatus() + ", " + responseBody);
        }

        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.has("error_code")) {
            throw new RuntimeException("百度OCR API错误: " + jsonNode.get("error_msg").asText());
        }

        JsonNode wordsResult = jsonNode.get("words_result");
        if (wordsResult == null || !wordsResult.isArray()) {
            logger.warn("百度OCR未返回识别结果 - words_result为空");
            return "";
        }

        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode wordNode : wordsResult) {
            if (wordNode.has("words")) {
                textBuilder.append(wordNode.get("words").asText()).append("\n");
            }
        }

        String result = textBuilder.toString().trim();
        logger.info("OCR识别文字: {}", result);
        return result;
    }

    private byte[] readImageData(String imagePath) throws Exception {
        Path path = Paths.get(imagePath);
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".webp")) {
            logger.info("检测到WebP格式图片，正在尝试转换为JPEG格式...");
            return convertWebpToJpeg(imagePath);
        }

        byte[] data = Files.readAllBytes(path);
        logger.info("读取图片数据完成，大小: {} bytes", data.length);
        return data;
    }

    private byte[] convertWebpToJpeg(String imagePath) throws Exception {
        File file = new File(imagePath);
        
        try {
            ensureImageIoPluginsLoaded();
            
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] result = baos.toByteArray();
                logger.info("WebP图片已成功转换为JPEG格式，转换后大小: {} bytes", result.length);
                return result;
            }
            logger.warn("ImageIO.read()返回null，尝试其他方法");
        } catch (Exception e) {
            logger.warn("方法1失败: {}", e.getMessage());
        }

        try {
            ImageReader reader = getWebpImageReader();
            if (reader != null) {
                try (FileInputStream fis = new FileInputStream(file);
                     ImageInputStream iis = ImageIO.createImageInputStream(fis)) {
                    reader.setInput(iis);
                    BufferedImage image = reader.read(0);
                    if (image != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", baos);
                        byte[] result = baos.toByteArray();
                        logger.info("WebP图片通过ImageReader成功转换为JPEG格式，转换后大小: {} bytes", result.length);
                        return result;
                    }
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception e) {
            logger.warn("方法2失败: {}", e.getMessage());
        }

        logger.warn("所有WebP转换方法都失败，直接返回原始字节");
        return Files.readAllBytes(file.toPath());
    }

    private void ensureImageIoPluginsLoaded() {
        try {
            ImageIO.scanForPlugins();
            String[] formats = ImageIO.getReaderFormatNames();
            boolean webpAvailable = false;
            for (String format : formats) {
                if ("webp".equalsIgnoreCase(format)) {
                    webpAvailable = true;
                    break;
                }
            }
            logger.debug("WebP格式支持: {}", webpAvailable);
        } catch (Exception e) {
            logger.warn("扫描ImageIO插件失败: {}", e.getMessage());
        }
    }

    private ImageReader getWebpImageReader() {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("webp");
        if (readers.hasNext()) {
            return readers.next();
        }
        
        readers = ImageIO.getImageReadersByMIMEType("image/webp");
        if (readers.hasNext()) {
            return readers.next();
        }
        
        logger.warn("未找到WebP ImageReader");
        return null;
    }

    private String getAccessToken() throws Exception {
        String url = baiduOcrConfig.getAccessTokenUrl() +
                "?grant_type=client_credentials" +
                "&client_id=" + baiduOcrConfig.getApiKey() +
                "&client_secret=" + baiduOcrConfig.getSecretKey();

        logger.info("请求百度AccessToken, URL: {}", url);

        HttpResponse response = HttpRequest.get(url)
                .timeout(10000)
                .execute();

        String responseBody = response.body();
        logger.info("百度AccessToken响应状态码: {}", response.getStatus());

        if (response.getStatus() != 200) {
            throw new RuntimeException("百度AccessToken HTTP错误: " + response.getStatus() + ", " + responseBody);
        }

        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.has("access_token")) {
            return jsonNode.get("access_token").asText();
        }

        if (jsonNode.has("error")) {
            throw new RuntimeException("百度AccessToken API错误: " + jsonNode.get("error").asText() + ", " + jsonNode.get("error_description").asText());
        }

        throw new RuntimeException("获取百度OCR AccessToken失败");
    }
}
