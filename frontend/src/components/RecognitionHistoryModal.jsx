import React, { useState, useImperativeHandle, forwardRef } from 'react';
import { getToken } from '../utils/elderApi';
import { formatDateTime } from '../utils/timeUtils';

/**
 * 识药历史弹窗 - 独立组件
 * 从 App.js renderUploadTab 拆分而来
 * 负责：历史记录列表、详情查看、跳转识别结果
 *
 * 通过 ref 暴露 loadHistory()，由父组件按钮触发打开
 *
 * @param {object} props
 * @param {Function} props.authFetch - 带认证的 fetch 封装
 * @param {Function} props.onJumpToRecognition - 跳转到识别结果页（由父组件处理路由与状态）
 */
const RecognitionHistoryModal = forwardRef(({ authFetch, onJumpToRecognition }, ref) => {
  const [show, setShow] = useState(false);
  const [recognitionHistory, setRecognitionHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [detailLog, setDetailLog] = useState(null);

  // 加载识药历史
  const loadHistory = async () => {
    setShow(true);
    setIsLoading(true);
    try {
      const response = await fetch('/api/v1/drug/recognize/history?limit=20', {
        headers: { 'Authorization': `Bearer ${getToken()}` }
      });
      const data = await response.json();
      if (data.code === 200 && data.data) {
        setRecognitionHistory(data.data);
      }
    } catch (e) {
      console.error('获取识药历史失败', e);
    } finally {
      setIsLoading(false);
    }
  };

  // 暴露给父组件的方法
  useImperativeHandle(ref, () => ({ loadHistory }));

  // 查看单条历史详情（尝试附带 OcrRecord 图片）
  const openDetail = async (log) => {
    let imageUrl = null;
    if (log.ocrRecordId) {
      try {
        const { data: d } = await authFetch(`/api/v1/drug/recognize/result/${log.ocrRecordId}`);
        if (d.code === 200 && d.data && d.data.imageUrl) {
          imageUrl = d.data.imageUrl;
        }
      } catch (e) { /* ignore */ }
    }
    setDetailLog({ ...log, _imageUrl: imageUrl });
  };

  // 跳转到识别结果页
  const jumpToRecognition = async (drugName) => {
    try {
      const { data } = await authFetch(`/api/v1/drug/detail?drugName=${encodeURIComponent(drugName)}`);
      if (data.code === 200 && data.data) {
        onJumpToRecognition(data.data);
        setShow(false);
        setDetailLog(null);
      }
    } catch (e) {
      console.error('跳转识别结果失败', e);
    }
  };

  // 状态样式映射
  const statusMap = {
    matched: { label: '✅ 已匹配', color: '#16a34a', bg: '#f0fdf4' },
    unmatched: { label: '❌ 未匹配', color: '#dc2626', bg: '#fef2f2' },
    imported: { label: '📥 已入库', color: '#2563eb', bg: '#eff6ff' },
    pending: { label: '⏳ 处理中', color: '#d97706', bg: '#fffbeb' }
  };

  if (!show) return null;

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(0,0,0,0.5)', zIndex: 1000,
      display: 'flex', alignItems: 'center', justifyContent: 'center'
    }}
      onClick={() => setShow(false)}
    >
      <div style={{
        background: 'white', borderRadius: '16px', padding: '24px',
        width: '90%', maxWidth: '500px', maxHeight: '80vh',
        overflowY: 'auto', boxShadow: '0 20px 60px rgba(0,0,0,0.3)'
      }}
        onClick={e => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>📋 识药历史记录</h3>
          <button onClick={() => setShow(false)} style={{ background: 'none', border: 'none', fontSize: '20px', cursor: 'pointer', color: '#9ca3af' }}>✕</button>
        </div>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '32px', color: '#9ca3af' }}>
            <div className="loading-spinner" style={{ margin: '0 auto 12px' }}></div>
            <p>加载中...</p>
          </div>
        ) : detailLog ? (
          /* 历史记录详情视图 */
          <div>
            <button onClick={() => setDetailLog(null)} style={{ background: 'none', border: 'none', fontSize: '14px', color: '#6366f1', cursor: 'pointer', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '4px' }}>
              ← 返回列表
            </button>
            <div style={{ border: '1px solid #e5e7eb', borderRadius: '12px', padding: '16px', background: '#fafafa' }}>
              {/* 药品图片 */}
              {detailLog._imageUrl && (
                <div style={{ marginBottom: '12px', textAlign: 'center' }}>
                  <img src={detailLog._imageUrl} alt="识别图片" style={{ maxWidth: '100%', maxHeight: '200px', borderRadius: '8px', objectFit: 'contain' }} />
                </div>
              )}
              {/* 药品名称和状态 */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <span style={{ fontSize: '17px', fontWeight: 'bold', color: '#1f2937' }}>
                  {detailLog.matchedDrugName || detailLog.normalizedName || '未知药品'}
                </span>
                {(() => {
                  const st = statusMap[detailLog.status] || statusMap.pending;
                  return <span style={{ fontSize: '12px', padding: '3px 10px', borderRadius: '8px', background: st.bg, color: st.color }}>{st.label}</span>;
                })()}
              </div>
              {/* OCR原文 */}
              {detailLog.rawText && (
                <div style={{ marginBottom: '10px' }}>
                  <p style={{ fontSize: '13px', fontWeight: 'bold', color: '#6b7280', marginBottom: '4px' }}>OCR识别原文：</p>
                  <p style={{ fontSize: '13px', color: '#374151', background: '#f3f4f6', padding: '10px', borderRadius: '8px', whiteSpace: 'pre-wrap', lineHeight: '1.6', margin: 0 }}>
                    {detailLog.rawText}
                  </p>
                </div>
              )}
              {/* 匹配信息 */}
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#9ca3af', marginBottom: '12px' }}>
                <span>{detailLog.createdAt ? formatDateTime(detailLog.createdAt) : ''}</span>
                {detailLog.matchScore && <span>匹配度: {(detailLog.matchScore * 100).toFixed(0)}%</span>}
              </div>
              {/* 操作按钮 */}
              {(detailLog.status === 'matched' || detailLog.status === 'imported') && detailLog.matchedDrugName && (
                <button
                  onClick={() => jumpToRecognition(detailLog.matchedDrugName)}
                  style={{
                    width: '100%', padding: '12px', borderRadius: '10px', border: 'none',
                    background: '#4f46e5', color: 'white', fontSize: '15px', fontWeight: 'bold',
                    cursor: 'pointer'
                  }}
                >
                  🔍 查看识别结果
                </button>
              )}
            </div>
          </div>
        ) : recognitionHistory.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '32px', color: '#9ca3af' }}>
            <span style={{ fontSize: '40px' }}>📭</span>
            <p style={{ marginTop: '8px' }}>暂无识药历史记录</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {recognitionHistory.map(log => {
              const st = statusMap[log.status] || statusMap.pending;
              return (
                <div key={log.id} style={{
                  border: '1px solid #e5e7eb', borderRadius: '12px', padding: '14px',
                  background: '#fafafa', cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
                  onClick={() => openDetail(log)}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                    <span style={{ fontSize: '15px', fontWeight: 'bold', color: '#1f2937' }}>
                      {log.matchedDrugName || log.normalizedName || '未知药品'}
                    </span>
                    <span style={{ fontSize: '11px', padding: '2px 8px', borderRadius: '8px', background: st.bg, color: st.color }}>
                      {st.label}
                    </span>
                  </div>
                  {log.rawText && (
                    <p style={{ fontSize: '12px', color: '#9ca3af', margin: '0 0 4px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      OCR: {log.rawText.substring(0, 60)}{log.rawText.length > 60 ? '...' : ''}
                    </p>
                  )}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '11px', color: '#d1d5db' }}>
                      {log.createdAt ? formatDateTime(log.createdAt) : ''}
                    </span>
                    {log.matchScore && (
                      <span style={{ fontSize: '11px', color: '#6b7280' }}>
                        匹配度: {(log.matchScore * 100).toFixed(0)}%
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
});

export default RecognitionHistoryModal;
