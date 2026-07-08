/**
 * 老人端API请求工具 - 自动添加JWT token到请求头
 * 处理认证失败自动跳转登录页
 * localStorage仅存储JWT token，用户信息通过API获取
 */

const TOKEN_KEY = 'elderToken';

// 获取token
export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

// 保存token
export function saveToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

// 清除token
export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
}

// 检查是否已认证
export function isAuthenticated() {
  return !!getToken();
}

/**
 * 发起老人端API请求（自动添加Authorization头）
 * @param {string} url - API完整路径
 * @param {object} options - fetch选项
 * @returns {Promise<object>} - 响应数据
 */
export async function elderFetch(url, options = {}) {
  const token = getToken();

  const headers = {
    ...options.headers,
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json',
  };

  // 添加Authorization头
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  const data = await response.json();

  // 处理401未认证错误
  if (response.status === 401 || data.code === 401 || data.message?.includes('未认证') || data.message?.includes('Unauthorized') || data.message?.includes('Access Denied')) {
    clearAuth();
    // 触发重新登录（通过事件通知App组件）
    window.dispatchEvent(new CustomEvent('elder-auth-expired'));
    throw new Error('认证已过期，请重新登录');
  }

  return data;
}