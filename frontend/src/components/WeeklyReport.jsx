import React, { useState, useEffect, useRef } from 'react';
import html2canvas from 'html2canvas';
import { useToast } from './Toast';
import { getToken } from '../utils/elderApi';
import './WeeklyReport.css';

/**
 * AI用药周报展示组件
 * @param {Object} props - 组件属性
 * @param {boolean} props.compact - 是否使用紧凑模式（用于嵌入其他页面）
 */
const WeeklyReport = ({ compact = false }) => {
  const { showToast } = useToast();
  const [report, setReport] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const reportRef = useRef(null);

  // 加载最新周报
  useEffect(() => {
    loadLatestReport();
  }, []);

  const loadLatestReport = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await fetch(`/api/weekly-report/latest`, {
        headers: { 'Authorization': `Bearer ${getToken()}` },
      });
      const data = await response.json();
      
      if (data.code === 200 && data.data) {
        setReport(data.data);
      } else {
        setError(data.message || '获取周报失败');
      }
    } catch (err) {
      console.error('加载周报失败:', err);
      setError('网络连接失败，请稍后重试');
    } finally {
      setIsLoading(false);
    }
  };

  // 截图下载功能
  const handleScreenshot = async () => {
    if (!reportRef.current) return;

    try {
      const canvas = await html2canvas(reportRef.current, {
        backgroundColor: '#ffffff',
        scale: 2,
        useCORS: true,
        logging: false
      });

      const link = document.createElement('a');
      const dateStr = report?.startDate ? `${report.startDate}_${report.endDate}` : 'weekly';
      link.download = `用药周报_${dateStr}.png`;
      link.href = canvas.toDataURL('image/png');
      link.click();
    } catch (err) {
      console.error('截图失败:', err);
      showToast('截图失败，请稍后重试', 'error');
    }
  };

  // 复制文本报告
  const handleCopyText = async () => {
    if (!report?.fullReportText) return;

    try {
      await navigator.clipboard.writeText(report.fullReportText);
      showToast('报告已复制到剪贴板！', 'success');
    } catch (err) {
      console.error('复制失败:', err);
      showToast('复制失败，请手动选择文本复制', 'error');
    }
  };

  // 格式化日期范围
  const formatDateRange = (startDate, endDate) => {
    if (!startDate || !endDate) return '';
    return `${startDate} 至 ${endDate}`;
  };

  // 获取合规率颜色
  const getComplianceColor = (rate) => {
    if (rate >= 90) return '#16a34a'; // 绿色
    if (rate >= 70) return '#eab308'; // 黄色
    if (rate >= 50) return '#f97316'; // 橙色
    return '#dc2626'; // 红色
  };

  // 获取合规率评价
  const getComplianceLabel = (rate) => {
    if (rate >= 90) return '优秀';
    if (rate >= 70) return '良好';
    if (rate >= 50) return '一般';
    return '需改进';
  };

  if (isLoading) {
    return (
      <div className="weekly-report-loading">
        <div className="loading-spinner"></div>
        <p>正在生成AI用药周报...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="weekly-report-error">
        <div className="error-icon">⚠️</div>
        <p>{error}</p>
        <button className="btn btn-primary" onClick={loadLatestReport}>
          重新加载
        </button>
      </div>
    );
  }

  if (!report) {
    return (
      <div className="weekly-report-empty">
        <div className="empty-icon">📊</div>
        <h3>暂无用药周报</h3>
        <p>请先在药箱中添加药品并设置用药计划</p>
        <button className="btn btn-primary" onClick={() => window.location.hash = '#drugs'}>
          去添加药品
        </button>
      </div>
    );
  }

  const { statistics, dailySummaries, missedDrugs, bestTimeSlot, needsImprovementTimeSlot, aiSummary } = report;
  const complianceColor = getComplianceColor(statistics.complianceRate);
  const complianceLabel = getComplianceLabel(statistics.complianceRate);

  return (
    <div className={`weekly-report-container ${compact ? 'compact' : ''}`}>
      {/* 操作按钮 - 仅在非紧凑模式显示 */}
      {!compact && (
        <div className="weekly-report-actions">
          <button className="btn btn-primary" onClick={handleScreenshot}>
            📷 截图保存
          </button>
          <button className="btn btn-secondary" onClick={handleCopyText}>
            📋 复制文本
          </button>
          <button className="btn btn-outline" onClick={loadLatestReport}>
            🔄 刷新
          </button>
        </div>
      )}

      {/* 周报内容（截图区域） */}
      <div className="weekly-report-content" ref={reportRef}>
        {/* 头部 */}
        <div className="report-header">
          <div className="report-title">
            <span className="title-icon">📊</span>
            <h2>AI用药周报</h2>
          </div>
          <div className="report-date">
            {formatDateRange(report.startDate, report.endDate)}
          </div>
        </div>

        {/* 总体统计卡片 */}
        <div className="report-section statistics-section">
          <h3 className="section-title">📈 总体统计</h3>
          
          <div className="compliance-card" style={{ borderColor: complianceColor }}>
            <div className="compliance-rate" style={{ color: complianceColor }}>
              {statistics.complianceRate}%
            </div>
            <div className="compliance-label" style={{ color: complianceColor }}>
              {complianceLabel}
            </div>
            <div className="compliance-detail">
              按时服药率
            </div>
            {/* 进度条 */}
            <div className="compliance-progress-bar">
              <div 
                className="progress-fill"
                style={{ 
                  width: `${statistics.complianceRate}%`,
                  backgroundColor: complianceColor
                }}
              ></div>
            </div>
          </div>

          <div className="stats-grid">
            <div className="stat-item stat-total">
              <div className="stat-value">{statistics.totalPlans}</div>
              <div className="stat-label">总计划数</div>
              <div className="stat-color-bar" style={{ backgroundColor: '#2196F3' }}></div>
            </div>
            <div className="stat-item stat-taken">
              <div className="stat-value">{statistics.takenCount}</div>
              <div className="stat-label">已服用</div>
              <div className="stat-color-bar" style={{ backgroundColor: '#4CAF50' }}></div>
            </div>
            <div className="stat-item stat-missed">
              <div className="stat-value">{statistics.missedCount}</div>
              <div className="stat-label">漏服</div>
              <div className="stat-color-bar" style={{ backgroundColor: '#F44336' }}></div>
            </div>
            <div className="stat-item stat-skipped">
              <div className="stat-value">{statistics.skippedCount}</div>
              <div className="stat-label">跳过</div>
              <div className="stat-color-bar" style={{ backgroundColor: '#FF9800' }}></div>
            </div>
            <div className="stat-item stat-drugs">
              <div className="stat-value">{statistics.drugVarietyCount}</div>
              <div className="stat-label">药品种类</div>
              <div className="stat-color-bar" style={{ backgroundColor: '#9C27B0' }}></div>
            </div>
          </div>

          {/* AI建议 - 整合到总体统计区域 */}
          {aiSummary && (
            <div className="ai-summary-box">
              <div className="ai-icon">💡</div>
              <div className="ai-content">
                {aiSummary.split('\n').map((line, index) => (
                  <p key={index}>{line}</p>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* 时段分析 */}
        <div className="report-section timeslot-section">
          <h3 className="section-title">⏰ 时段分析</h3>
          <div className="timeslot-grid">
            <div className="timeslot-card best">
              <div className="timeslot-icon">✅</div>
              <div className="timeslot-label">表现最好</div>
              <div className="timeslot-value">{bestTimeSlot}</div>
            </div>
            <div className="timeslot-card improve">
              <div className="timeslot-icon">⚠️</div>
              <div className="timeslot-label">需改进</div>
              <div className="timeslot-value">{needsImprovementTimeSlot}</div>
            </div>
          </div>
        </div>

        {/* 漏服药品 */}
        {missedDrugs && missedDrugs.length > 0 && (
          <div className="report-section missed-section">
            <h3 className="section-title">⚠️ 漏服药品</h3>
            <div className="missed-drugs-list">
              {missedDrugs.map((drug, index) => (
                <div key={index} className="missed-drug-item">
                  <span className="drug-bullet">•</span>
                  <span className="drug-name">{drug}</span>
                </div>
              ))}
            </div>
            <p className="missed-tip">
              建议设置手机闹钟或使用智能药盒提醒，避免漏服
            </p>
          </div>
        )}

        {/* 每日详情 */}
        {dailySummaries && dailySummaries.length > 0 && (
          <div className="report-section daily-section">
            <h3 className="section-title">📅 每日详情</h3>
            <div className="daily-list">
              {dailySummaries.map((day, index) => {
                const dayColor = getComplianceColor(day.complianceRate);
                return (
                  <div key={index} className="daily-item">
                    <div className="daily-date">
                      <div className="date-text">{day.date}</div>
                      <div className="date-week">{day.dayOfWeek}</div>
                    </div>
                    <div className="daily-progress">
                      <div 
                        className="progress-bar"
                        style={{ 
                          width: `${day.complianceRate}%`,
                          backgroundColor: dayColor
                        }}
                      ></div>
                    </div>
                    <div className="daily-stats">
                      <span className="daily-taken">{day.takenCount}/{day.totalPlans}</span>
                      <span className="daily-rate" style={{ color: dayColor }}>
                        {day.complianceRate}%
                      </span>
                    </div>
                    {day.drugs && day.drugs.length > 0 && (
                      <div className="daily-drugs">
                        {day.drugs.join('、')}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}



        {/* 底部信息 */}
        <div className="report-footer">
          <div className="footer-text">
            数据来源：AI药管家
          </div>
          <div className="footer-time">
            生成时间：{new Date(report.generatedAt).toLocaleString('zh-CN')}
          </div>
        </div>
      </div>
    </div>
  );
};

export default WeeklyReport;
