// 家属端共享工具函数。
// 事件类型映射只在此处定义一份，GuardianElderDetail 与 GuardianNotification
// 均引用此处，保证两个组件使用完全相同的事件类型映射。

import { formatRelativeTime } from './timeUtils';

// 事件类型 → 中文标签（唯一来源，不要在其他文件中重复定义）
export const EVENT_TYPE_LABELS = {
  fall: '跌倒',
  sos: '紧急求助',
  abnormal: '异常行为',
  medication_missed: '漏服药物',
  missed_dose: '漏服药物',
  missed_dose_alert: '漏服药物',
  emergency_alert: '紧急报警',
  expiring_drug: '药品临期',
  expiring_drug_reminder: '药品临期',
  other: '其他',
};

// 统一事件类型标签：未知类型原样返回，避免显示空白
export const getEventTypeLabel = (type) => EVENT_TYPE_LABELS[type] || type;

// 统一时间格式化（相对时间，委托给统一工具函数，确保跨浏览器一致）
export const formatTime = (timeStr) => formatRelativeTime(timeStr);
