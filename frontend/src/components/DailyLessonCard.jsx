import React, { useState } from 'react';

/**
 * 今日一课卡片组件
 * 显示每日AI生成的慢病科普内容
 *
 * Props:
 *   lesson    - DailyLessonDTO 或 null（未加载）
 *   loading   - boolean 是否正在加载
 *   onRefresh - function 点击"换一篇"或重试时的回调
 *   onGoProfile - function 点击"完善档案"时的回调
 */
function DailyLessonCard({ lesson, loading, onRefresh, onGoProfile }) {
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await onRefresh();
    } finally {
      setRefreshing(false);
    }
  };

  // 加载中状态 - 骨架屏
  if (loading || (!lesson && !refreshing)) {
    return (
      <div className="daily-lesson-card daily-lesson-loading">
        <div className="daily-lesson-header">
          <span className="daily-lesson-badge">📖 今日一课</span>
        </div>
        <div className="daily-lesson-skeleton">
          <div className="skeleton-line skeleton-title"></div>
          <div className="skeleton-line skeleton-text"></div>
          <div className="skeleton-line skeleton-text"></div>
          <div className="skeleton-line skeleton-text skeleton-text-short"></div>
        </div>
        <div className="daily-lesson-footer">
          <span className="daily-lesson-loading-text">⏳ 正在为您准备今日科普...</span>
        </div>
      </div>
    );
  }

  // 无慢病史 - 引导完善档案
  if (lesson && !lesson.generated && lesson.errorMsg && lesson.errorMsg.includes('慢病史')) {
    return (
      <div className="daily-lesson-card daily-lesson-prompt">
        <div className="daily-lesson-header">
          <span className="daily-lesson-badge">📖 今日一课</span>
          <span className="daily-lesson-date">{lesson.lessonDate}</span>
        </div>
        <div className="daily-lesson-body">
          <div className="daily-lesson-empty">
            <span className="daily-lesson-empty-icon">📋</span>
            <p className="daily-lesson-empty-title">开启您的每日健康科普</p>
            <p className="daily-lesson-empty-desc">
              请先完善您的健康档案，包括<strong>慢性病史</strong>信息，我们将为您生成个性化的每日科普文章。
            </p>
            <button
              className="daily-lesson-profile-btn"
              onClick={onGoProfile}
            >
              ✏️ 去完善健康档案
            </button>
          </div>
        </div>
      </div>
    );
  }

  // 生成失败 - 显示错误和重试按钮
  if (lesson && !lesson.generated) {
    return (
      <div className="daily-lesson-card daily-lesson-error">
        <div className="daily-lesson-header">
          <span className="daily-lesson-badge">📖 今日一课</span>
          <span className="daily-lesson-date">{lesson.lessonDate}</span>
        </div>
        <div className="daily-lesson-body">
          <div className="daily-lesson-empty">
            <span className="daily-lesson-empty-icon">🤔</span>
            <p className="daily-lesson-empty-title">科普内容生成中</p>
            <p className="daily-lesson-empty-desc">
              今天的科普暂时还没准备好，点击下方按钮刷新试试。
            </p>
            <button
              className="daily-lesson-retry-btn"
              onClick={handleRefresh}
              disabled={refreshing}
            >
              {refreshing ? '⏳ 正在生成...' : '🔄 重新生成'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  // 正常展示状态
  if (lesson && lesson.generated) {
    return (
      <div className="daily-lesson-card">
        <div className="daily-lesson-header">
          <span className="daily-lesson-badge">📖 今日一课</span>
          <span className="daily-lesson-date">{lesson.lessonDate}</span>
        </div>
        <div className="daily-lesson-body">
          {lesson.chronicDisease && (
            <span className="daily-lesson-disease-tag">
              🏷️ {lesson.chronicDisease}
            </span>
          )}
          <h3 className="daily-lesson-title">{lesson.title}</h3>
          <div className="daily-lesson-content">
            {lesson.content && lesson.content.split('\n').map((line, i) => (
              <p key={i}>{line}</p>
            ))}
          </div>
        </div>
        <div className="daily-lesson-footer">
          <span className="daily-lesson-attribution">由 DeepSeek AI 生成</span>
          <button
            className="daily-lesson-refresh-btn"
            onClick={handleRefresh}
            disabled={refreshing}
            title="换一篇"
          >
            {refreshing ? '⏳' : '🔄'} 换一篇
          </button>
        </div>
      </div>
    );
  }

  return null;
}

export default DailyLessonCard;
