package com.example.backend.service.impl;

import com.example.backend.common.BusinessException;
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

            String userMsg = buildUserFriendlyMessage(e);
            try {
                OcrRecord ocrRecord = ocrRecordMapper.selectById(recordId);
                if (ocrRecord != null) {
                    ocrRecord.setStatus(OcrRecord.Status.FAILED.getCode());
                    // 超过数据库字段长度时截断（预留 500 字符安全上限）
                    if (userMsg.length() > 500) {
                        userMsg = userMsg.substring(0, 500);
                    }
                    ocrRecord.setRawText(userMsg);
                    ocrRecordMapper.updateById(ocrRecord);
                    logger.info("已更新OCR记录为失败状态并写入错误消息 - recordId: {}", recordId);
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

        // 图片尺寸预校验：最小 15×15，最大 4096×4096（百度OCR要求）
        try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageData)) {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(bais);
            if (img != null) {
                int w = img.getWidth();
                int h = img.getHeight();
                if (w < 15 || h < 15) {
                    throw new BusinessException("图片太小（" + w + "×" + h + " 像素），请使用至少 15×15 像素的图片");
                }
                if (w > 4096 || h > 4096) {
                    throw new BusinessException("图片尺寸过大（" + w + "×" + h + " 像素），请使用不超过 4096×4096 像素的图片");
                }
                logger.info("图片尺寸校验通过: {}×{} 像素", w, h);
            } else {
                throw new BusinessException("无法解析图片，请确认上传的是有效的图片文件");
            }
        }

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
            throw new BusinessException("百度OCR HTTP错误: " + response.getStatus() + ", 请稍后重试");
        }

        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.has("error_code")) {
            String errorMsg = jsonNode.get("error_msg").asText();
            String friendlyMsg = mapBaiduErrorToUserMessage(errorMsg);
            throw new RuntimeException(friendlyMsg);
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

    /**
     * 百度OCR错误码/错误消息 -> 中文用户可读消息
     */
    private String mapBaiduErrorToUserMessage(String errorMsg) {
        if (errorMsg == null) return "识别失败，请稍后重试";
        String msg = errorMsg.toLowerCase();
        if (msg.contains("image size error")) return "图片尺寸不符合要求，请换一张清晰的药品标签或说明书（最小 15×15，最大 4096×4096 像素）";
        if (msg.contains("image format error")) return "图片格式不支持，请使用 JPG/PNG/BMP 格式";
        if (msg.contains("access token failed") || msg.contains("invalid access token") || msg.contains("access token expired")) return "系统访问凭证失效，请稍后重试";
        if (msg.contains("internal error") || msg.contains("system error")) return "识别服务暂时不可用，请稍后重试";
        if (msg.contains("no permission") || msg.contains("not support") || msg.contains("unsupported openapi")) return "识别服务未授权，请联系管理员";
        if (msg.contains("url length error") || msg.contains("base64")) return "图片过大或编码错误，请压缩图片后再上传";
        return "识别失败：" + errorMsg + "，请换一张清晰的图片再试";
    }

    /**
     * 将底层异常（百度OCR、ImageIO、IO、JSON 等）统一转换为用户可读的中文消息。
     */
    private String buildUserFriendlyMessage(Exception e) {
        if (e == null) return "识别失败，请稍后重试";
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            return "识别失败，请稍后重试";
        }
        String lower = message.toLowerCase();
        // 图片解码失败（ImageIO、PNG/JPEG 解码器异常）
        if (lower.contains("error reading") || lower.contains("imageio") || lower.contains("cannot read")
                || lower.contains("invalid image") || lower.contains("bad file descriptor")
                || lower.contains("not a jpeg") || lower.contains("png") && lower.contains("error")) {
            return "无法识别该图片（文件可能已损坏），请换一张清晰的图片再上传";
        }
        // 文件不存在 / IO 错误
        if (lower.contains("no such file") || lower.contains("file not found") || lower.contains("路径不存在")
                || lower.contains("找不到")) {
            return "图片保存失败，请重试";
        }
        // 百度 OCR 超时 / 网络
        if (lower.contains("timeout") || lower.contains("connection timed out") || lower.contains("connect refused")
                || lower.contains("unknown host") || lower.contains("network is unreachable")) {
            return "识别服务连接超时，请检查网络后重试";
        }
        // JSON 解析失败
        if (lower.contains("json") || lower.contains("unexpected character") || lower.contains("parse")) {
            return "识别服务响应异常，请稍后重试";
        }
        // 百度 OCR 返回的原始错误消息
        if (lower.contains("baidu") || lower.contains("error code") || lower.contains("error_msg")
                || lower.contains("image size") || lower.contains("image format") || lower.contains("access token")) {
            return mapBaiduErrorToUserMessage(message);
        }
        // 其它异常：显示原消息的中文部分，若无中文则兜底
        boolean hasChinese = message.chars().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
        if (hasChinese) return message;
        return "识别失败，请稍后重试（建议换一张清晰的药品标签或说明书照片）";
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

        throw new BusinessException("获取百度OCR AccessToken失败");
    }
}
