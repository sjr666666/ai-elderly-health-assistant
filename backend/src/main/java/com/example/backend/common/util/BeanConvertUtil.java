package com.example.backend.common.util;

import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

public class BeanConvertUtil {

    public static <T> T convert(Object source, Class<T> target) {
        if (source == null) {
            return null;
        }
        try {
            T instance = target.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Bean转换失败", e);
        }
    }

    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> target) {
        if (sourceList == null || sourceList.isEmpty()) {
            return List.of();
        }
        return sourceList.stream()
                .map(source -> convert(source, target))
                .collect(Collectors.toList());
    }
}
