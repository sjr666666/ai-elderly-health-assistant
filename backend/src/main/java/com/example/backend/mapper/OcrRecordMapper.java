package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.OcrRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OcrRecordMapper extends BaseMapper<OcrRecord> {
}