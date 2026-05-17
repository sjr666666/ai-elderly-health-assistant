package com.example.backend.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.util.SnowflakeIdGenerator;
import com.example.backend.config.BaiduOcrConfig;
import com.example.backend.mapper.DrugBaseMapper;
import com.example.backend.mapper.OcrRecordMapper;
import com.example.backend.model.dto.OcrResultResponse;
import com.example.backend.model.dto.OcrUploadResponse;
import com.example.backend.model.entity.DrugBase;
import com.example.backend.model.entity.OcrRecord;
import com.example.backend.service.OcrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service
public class OcrServiceImpl implements OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrServiceImpl.class);

    private final OcrRecordMapper ocrRecordMapper;
    private final DrugBaseMapper drugBaseMapper;
    private final BaiduOcrConfig baiduOcrConfig;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final ObjectMapper objectMapper;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Autowired
    public OcrServiceImpl(
            OcrRecordMapper ocrRecordMapper,
            DrugBaseMapper drugBaseMapper,
            BaiduOcrConfig baiduOcrConfig,
            ObjectMapper objectMapper) {
        this.ocrRecordMapper = ocrRecordMapper;
        this.drugBaseMapper = drugBaseMapper;
        this.baiduOcrConfig = baiduOcrConfig;
        this.objectMapper = objectMapper;
        this.snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);
    }

    @Override
    public OcrUploadResponse uploadAndRecognize(MultipartFile file, Long userId) {
        try {
            String fileId = String.valueOf(snowflakeIdGenerator.nextId());
            logger.info("开始处理OCR识别任务 - fileId: {}, userId: {}, fileName: {}",
                    fileId, userId, file.getOriginalFilename());

            String imageUrl = saveFileLocally(file, fileId);

            OcrRecord ocrRecord = new OcrRecord();
            ocrRecord.setUserId(userId);
            ocrRecord.setImageUrl(imageUrl);
            ocrRecord.setStatus(OcrRecord.Status.PENDING.getCode());
            ocrRecordMapper.insert(ocrRecord);

            Long dbRecordId = ocrRecord.getId();
            logger.info("OCR记录已创建 - dbRecordId: {}, imageUrl: {}", dbRecordId, imageUrl);

            processOcrAsync(dbRecordId);

            return OcrUploadResponse.builder()
                    .taskId(String.valueOf(dbRecordId))
                    .status(OcrRecord.Status.PENDING.getCode())
                    .build();

        } catch (Exception e) {
            logger.error("OCR上传失败", e);
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    @Override
    @Async("taskExecutor")
    public void processOcrAsync(Long recordId) {
        logger.info("开始异步OCR处理 - recordId: {}", recordId);

        try {
            OcrRecord ocrRecord = ocrRecordMapper.selectById(recordId);

            if (ocrRecord == null) {
                logger.error("未找到OCR记录 - recordId: {}", recordId);
                return;
            }

            String imageUrl = ocrRecord.getImageUrl();
            String rawText = null;
            String drugName = null;

            try {
                DrugRecognitionResult drugResult = performDrugRecognition(imageUrl);
                drugName = drugResult.getDrugName();
                logger.info("药品识别API返回结果 - recordId: {}, drugName: {}", recordId, drugName);
            } catch (Exception drugEx) {
                logger.warn("药品识别API调用失败，使用通用OCR作为备选 - recordId: {}, error: {}", recordId, drugEx.getMessage());
            }

            if (drugName != null && !drugName.isEmpty()) {
                rawText = drugName;
            } else {
                logger.info("药品识别未返回结果，使用通用OCR - recordId: {}", recordId);
                rawText = performOcr(imageUrl);
                logger.info("通用OCR识别完成 - recordId: {}, rawText: {}", recordId, rawText);
            }

            ocrRecord.setRawText(rawText);

            if (rawText == null || rawText.isEmpty()) {
                ocrRecord.setStatus(OcrRecord.Status.UNMATCHED.getCode());
                logger.info("未能识别出文字 - recordId: {}", recordId);
            } else {
                DrugMatchResult matchResult = matchDrug(rawText);
                if (matchResult.isMatched()) {
                    ocrRecord.setMatchedDrugId(matchResult.getDrugId());
                    ocrRecord.setMatchScore(matchResult.getScore());
                    ocrRecord.setStatus(OcrRecord.Status.MATCHED.getCode());
                    logger.info("药品匹配成功 - recordId: {}, drugId: {}, score: {}",
                            recordId, matchResult.getDrugId(), matchResult.getScore());
                } else {
                    ocrRecord.setStatus(OcrRecord.Status.UNMATCHED.getCode());
                    logger.info("药品未匹配 - recordId: {}, rawText: {}", recordId, rawText);
                }
            }

            ocrRecordMapper.updateById(ocrRecord);

        } catch (Exception e) {
            logger.error("OCR异步处理失败 - recordId: {}", recordId, e);
            try {
                OcrRecord ocrRecord = ocrRecordMapper.selectById(recordId);
                if (ocrRecord != null) {
                    ocrRecord.setStatus(OcrRecord.Status.FAILED.getCode());
                    ocrRecordMapper.updateById(ocrRecord);
                }
            } catch (Exception ex) {
                logger.error("更新OCR状态失败 - recordId: {}", recordId, ex);
            }
        }
    }

    private String saveFileLocally(MultipartFile file, String fileId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String fileName = fileId + extension;
        Path filePath = uploadDir.resolve(fileName);

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath);
        }

        String imageUrl = filePath.toAbsolutePath().toString();
        logger.info("图片已保存到本地 - fileId: {}, path: {}", fileId, imageUrl);
        return imageUrl;
    }

    private static final String DRUG_RECOGNITION_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/drug";

    private String performOcr(String imagePath) throws Exception {
        logger.info("开始获取百度OCR AccessToken...");
        String accessToken = getAccessToken();
        logger.info("AccessToken获取成功");

        byte[] imageData = Files.readAllBytes(Paths.get(imagePath));
        String imageBase64 = Base64.getEncoder().encodeToString(imageData);

        String url = baiduOcrConfig.getOcrUrl() + "?access_token=" + accessToken;
        logger.info("开始调用百度OCR API, URL: {}", url);

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form("image", imageBase64)
                .timeout(30000)
                .execute();

        String responseBody = response.body();
        logger.info("百度OCR响应状态码: {}, 响应体: {}", response.getStatus(), responseBody);

        if (response.getStatus() != 200) {
            throw new RuntimeException("百度OCR HTTP错误: " + response.getStatus() + ", " + responseBody);
        }

        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.has("error_code")) {
            throw new RuntimeException("百度OCR API错误: " + jsonNode.get("error_msg").asText());
        }

        JsonNode wordsResult = jsonNode.get("words_result");
        if (wordsResult == null || !wordsResult.isArray()) {
            return "";
        }

        StringBuilder textBuilder = new StringBuilder();
        for (JsonNode wordNode : wordsResult) {
            if (wordNode.has("words")) {
                textBuilder.append(wordNode.get("words").asText()).append("\n");
            }
        }

        return textBuilder.toString().trim();
    }

    private DrugRecognitionResult performDrugRecognition(String imagePath) throws Exception {
        logger.info("开始获取百度OCR AccessToken...");
        String accessToken = getAccessToken();
        logger.info("AccessToken获取成功");

        byte[] imageData = Files.readAllBytes(Paths.get(imagePath));
        String imageBase64 = Base64.getEncoder().encodeToString(imageData);

        String url = DRUG_RECOGNITION_URL + "?access_token=" + accessToken;
        logger.info("开始调用百度药品识别 API, URL: {}", url);

        HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form("image", imageBase64)
                .timeout(30000)
                .execute();

        String responseBody = response.body();
        logger.info("百度药品识别响应状态码: {}, 响应体: {}", response.getStatus(), responseBody);

        if (response.getStatus() != 200) {
            throw new RuntimeException("百度药品识别 HTTP错误: " + response.getStatus() + ", " + responseBody);
        }

        JsonNode jsonNode = objectMapper.readTree(responseBody);

        if (jsonNode.has("error_code")) {
            throw new RuntimeException("百度药品识别 API错误: " + jsonNode.get("error_msg").asText());
        }

        DrugRecognitionResult result = new DrugRecognitionResult();

        JsonNode resultNode = jsonNode.get("result");
        if (resultNode != null) {
            if (resultNode.has("drug_name")) {
                result.setDrugName(resultNode.get("drug_name").asText());
            }
            if (resultNode.has("drug_type")) {
                result.setDrugType(resultNode.get("drug_type").asText());
            }
            if (resultNode.has("batch_number")) {
                result.setBatchNumber(resultNode.get("batch_number").asText());
            }
            if (resultNode.has("production_date")) {
                result.setProductionDate(resultNode.get("production_date").asText());
            }
            if (resultNode.has("expiration_date")) {
                result.setExpirationDate(resultNode.get("expiration_date").asText());
            }
            if (resultNode.has("manufacturer")) {
                result.setManufacturer(resultNode.get("manufacturer").asText());
            }
            if (resultNode.has("specification")) {
                result.setSpecification(resultNode.get("specification").asText());
            }
        }

        logger.info("药品识别结果: {}", result);
        return result;
    }

    private static class DrugRecognitionResult {
        private String drugName;
        private String drugType;
        private String batchNumber;
        private String productionDate;
        private String expirationDate;
        private String manufacturer;
        private String specification;

        public String getDrugName() { return drugName; }
        public void setDrugName(String drugName) { this.drugName = drugName; }
        public String getDrugType() { return drugType; }
        public void setDrugType(String drugType) { this.drugType = drugType; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public String getProductionDate() { return productionDate; }
        public void setProductionDate(String productionDate) { this.productionDate = productionDate; }
        public String getExpirationDate() { return expirationDate; }
        public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }

        @Override
        public String toString() {
            return "DrugRecognitionResult{drugName='" + drugName + "', drugType='" + drugType +
                    "', batchNumber='" + batchNumber + "', manufacturer='" + manufacturer +
                    "', specification='" + specification + "'}";
        }
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
        logger.info("百度AccessToken响应状态码: {}, 响应体: {}", response.getStatus(), responseBody);

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

    private DrugMatchResult matchDrug(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return new DrugMatchResult(false, null, BigDecimal.ZERO);
        }

        String[] lines = rawText.split("\n");
        String searchText = rawText.toLowerCase().replaceAll("[\\s\\n]", "");

        DrugBase bestMatch = null;
        BigDecimal bestScore = BigDecimal.ZERO;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.length() < 2) continue;

            String lineForSearch = trimmedLine.toLowerCase().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");

            LambdaQueryWrapper<DrugBase> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.and(wrapper -> wrapper
                    .like(DrugBase::getGenericName, lineForSearch)
                    .or()
                    .like(DrugBase::getTradeName, lineForSearch)
                    .or()
                    .like(DrugBase::getCommonName, lineForSearch));

            List<DrugBase> drugList = drugBaseMapper.selectList(queryWrapper);

            for (DrugBase drug : drugList) {
                BigDecimal score = calculateMatchScore(lineForSearch, drug);
                if (score.compareTo(bestScore) > 0) {
                    bestScore = score;
                    bestMatch = drug;
                }
            }
        }

        if (bestMatch != null && bestScore.compareTo(new BigDecimal("0.2")) >= 0) {
            return new DrugMatchResult(true, bestMatch.getId(), bestScore);
        }

        return new DrugMatchResult(false, null, BigDecimal.ZERO);
    }

    private BigDecimal calculateMatchScore(String searchText, DrugBase drug) {
        int score = 0;

        String genericName = drug.getGenericName() != null ?
                drug.getGenericName().toLowerCase().replaceAll("[\\s]", "") : "";
        String tradeName = drug.getTradeName() != null ?
                drug.getTradeName().toLowerCase().replaceAll("[\\s]", "") : "";
        String commonName = drug.getCommonName() != null ?
                drug.getCommonName().toLowerCase().replaceAll("[\\s]", "") : "";

        if (searchText.contains(genericName) || genericName.contains(searchText)) {
            score += 10;
        } else if (fuzzyMatch(searchText, genericName)) {
            score += 5;
        }

        if (searchText.contains(tradeName) || tradeName.contains(searchText)) {
            score += 8;
        } else if (fuzzyMatch(searchText, tradeName)) {
            score += 4;
        }

        if (searchText.contains(commonName) || commonName.contains(searchText)) {
            score += 6;
        } else if (fuzzyMatch(searchText, commonName)) {
            score += 3;
        }

        return BigDecimal.valueOf(score).divide(BigDecimal.valueOf(10), 2, BigDecimal.ROUND_HALF_UP);
    }

    private boolean fuzzyMatch(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return false;
        }

        int matchCount = 0;
        for (char c : text2.toCharArray()) {
            if (text1.indexOf(c) >= 0) {
                matchCount++;
            }
        }

        return matchCount >= text2.length() * 0.6;
    }

    @Override
    public OcrResultResponse getOcrResult(String taskId) {
        try {
            Long recordId = Long.parseLong(taskId);
            OcrRecord ocrRecord = ocrRecordMapper.selectById(recordId);

            if (ocrRecord == null) {
                return null;
            }

            OcrResultResponse response = OcrResultResponse.fromEntity(ocrRecord);

            if (ocrRecord.getMatchedDrugId() != null) {
                DrugBase drug = drugBaseMapper.selectById(ocrRecord.getMatchedDrugId());
                if (drug != null) {
                    response.setMatchedDrugName(drug.getGenericName());
                    response.setMatchedDrugSpec(drug.getSpecification());
                }
            }

            return response;

        } catch (Exception e) {
            logger.error("查询OCR结果失败 - taskId: {}", taskId, e);
            return null;
        }
    }

    private static class DrugMatchResult {
        private final boolean matched;
        private final Long drugId;
        private final BigDecimal score;

        public DrugMatchResult(boolean matched, Long drugId, BigDecimal score) {
            this.matched = matched;
            this.drugId = drugId;
            this.score = score;
        }

        public boolean isMatched() {
            return matched;
        }

        public Long getDrugId() {
            return drugId;
        }

        public BigDecimal getScore() {
            return score;
        }
    }
}