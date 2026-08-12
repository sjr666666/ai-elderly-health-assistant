package com.example.backend.service.sms;

/**
 * 短信发送通道抽象。
 *
 * <p>一期默认使用 {@code mock}（仅记录日志，不真实发送）；二期接入真实短信服务商
 * （如阿里云短信）时新增实现类，并通过 {@code sms.provider} 配置切换，调用方无感知。</p>
 */
public interface SmsProvider {

    /**
     * 通道名称，与配置项 {@code sms.provider} 对应。
     */
    String name();

    /**
     * 发送短信。
     *
     * @param phone   目标手机号（明文）
     * @param content 短信内容
     * @return true 表示发送成功（或已记录待发送），false 表示发送失败
     */
    boolean send(String phone, String content);
}
