package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.entity.DailyLesson;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 今日一课 Mapper
 *
 * @author backend
 * @since 1.1.0
 */
@Mapper
public interface DailyLessonMapper extends BaseMapper<DailyLesson> {

    /**
     * 物理删除指定用户指定日期的记录（绕过逻辑删除）
     */
    @Delete("DELETE FROM daily_lesson WHERE user_id = #{userId} AND lesson_date = #{lessonDate}")
    int physicalDeleteByUserAndDate(@Param("userId") Long userId, @Param("lessonDate") String lessonDate);
}
