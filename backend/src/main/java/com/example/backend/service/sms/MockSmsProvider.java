package com.example.backend.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 模拟短信通道：仅记录日志，不真实发送。
 *
 * <p>上线初期默认使用本通道——老人端提醒主要靠家属端内通知转达；
 * 待接入真实短信服务商后，通过 {@code sms.provider} 配置切换到新实现。</p>
 */
@Slf4j
@Component("mock")
public class MockSmsProvider implements SmsProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean send(String phone, String content) {
        log.info("[MockSmsProvider] 模拟发送短信 - phone(脱敏): {}, content: {}", mask(phone), content);
        return true;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.length() == 11
                ? phone.substring(0, 3) + "****" + phone.substring(7)
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
}
