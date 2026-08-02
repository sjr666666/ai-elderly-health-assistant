export const AUTH_RULES = {
  username: /^[A-Za-z0-9_]{4,20}$/,
  password: /^.{6,20}$/,
  phone: /^1[3-9]\d{9}$/,
};

export function validateCredentials(username, password) {
  if (!AUTH_RULES.username.test(username.trim())) return '用户名需为4-20位字母、数字或下划线';
  if (!AUTH_RULES.password.test(password)) return '密码长度需为6-20位';
  return '';
}
