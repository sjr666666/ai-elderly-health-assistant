/**
 * 家属端API请求工具 - 自动添加JWT token到请求头
 * 处理认证失败自动跳转登录页
 * sessionStorage仅存储JWT token，关闭标签页自动清除，更安全
 */

const TOKEN_KEY = 'guardianToken'; // 家属端独立token，与老人端隔离

// 获取token
export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

// 保存token（使用sessionStorage，关闭标签页自动清除，更安全）
export function saveToken(token) {
  sessionStorage.setItem(TOKEN_KEY, token);
}

// 清除token
export function clearAuth() {
  sessionStorage.removeItem(TOKEN_KEY);
}

// 检查是否已认证
export function isAuthenticated() {
  return !!getToken();
}

/**
 * 发起家属端API请求（自动添加Authorization头）
 * @param {string} url - API路径（不含/api/v1/guardian前缀）
 * @param {object} options - fetch选项
 * @returns {Promise<object>} - 响应数据
 */
export async function guardianFetch(url, options = {}) {
  const token = getToken();
  const fullUrl = url.startsWith('/api/') ? url : `/api/v1/guardian${url}`;

  const headers = {
    ...options.headers,
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json',
  };

  // 添加Authorization头
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(fullUrl, {
    ...options,
    headers,
  });

  const data = await response.json();

  // 处理401未认证错误
  if (response.status === 401 || data.code === 401 || data.message?.includes('未认证') || data.message?.includes('Unauthorized')) {
    clearAuth();
    // 触发重新登录（通过事件通知GuardianApp）
    window.dispatchEvent(new CustomEvent('guardian-auth-expired'));
    throw new Error('认证已过期，请重新登录');
  }

  return data;
}

// 常用API封装
export const guardianApi = {
  // 获取仪表盘
  getDashboard: () => guardianFetch('/dashboard'),

  // 获取关联老人列表
  getElderList: () => guardianFetch('/elders'),

  // 获取老人详情
  getElderDetail: (elderId) => guardianFetch(`/elders/${elderId}`),

  // 绑定老人
  bindElder: (elderUsername, relationType) => guardianFetch('/bind', {
    method: 'POST',
    body: JSON.stringify({ elderUsername, relationType }),
  }),

  // 解绑老人
  unbindElder: (elderId) => guardianFetch(`/unbind?elderId=${elderId}`, { method: 'DELETE' }),

  // 获取老人紧急事件
  getElderEvents: (elderId, limit = 10) => guardianFetch(`/elders/${elderId}/events?limit=${limit}`),

  // 处理紧急事件
  resolveEvent: (eventId) => guardianFetch(`/events/${eventId}/resolve`, { method: 'PUT' }),

  // 获取临期药品
  getExpiringDrugs: (elderId) => guardianFetch(`/elders/${elderId}/expiring-drugs`),

  // 获取用药计划
  getMedicationPlan: (elderId) => guardianFetch(`/elders/${elderId}/medication-plan`),

  // 获取通知列表
  getNotifications: (limit = 20) => guardianFetch(`/notifications?limit=${limit}`),

  // 获取未读通知数
  getUnreadCount: () => guardianFetch('/notifications/unread-count'),

  // 标记全部已读
  markAllAsRead: () => guardianFetch('/notifications/read-all', { method: 'PUT' }),

  // 标记单条已读
  markOneAsRead: (id) => guardianFetch(`/notifications/${id}/read`, { method: 'PUT' }),

  // 清除已读通知
  clearReadNotifications: () => guardianFetch('/notifications/read', { method: 'DELETE' }),
};