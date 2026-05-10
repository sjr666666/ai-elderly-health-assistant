package com.example.backend.common;

public enum ResponseCode {
    
    SUCCESS(200, "操作成功"),
    FAIL(400, "操作失败"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // 业务错误码
    VALIDATION_ERROR(1001, "参数校验失败"),
    USER_NOT_FOUND(1002, "用户不存在"),
    INVALID_TOKEN(1003, "无效的令牌"),
    FILE_UPLOAD_ERROR(1004, "文件上传失败"),
    OCR_ERROR(1005, "OCR识别失败"),
    API_ERROR(1006, "第三方API调用失败");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}