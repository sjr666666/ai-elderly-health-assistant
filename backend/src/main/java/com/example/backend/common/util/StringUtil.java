package com.example.backend.common.util;

import cn.hutool.core.util.StrUtil;

public class StringUtil {
    public static boolean isBlank(String str) {
        return StrUtil.isBlank(str);
    }

    public static boolean isNotBlank(String str) {
        return StrUtil.isNotBlank(str);
    }
}
