const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        logLevel: 'debug',
        pathRewrite: {
            '^/api': '/api'  // 关键：明确保留 /api 前缀
        },
        onProxyReq: (proxyReq, req, res) => {
          console.log('[Proxy] Forwarding:', req.method, req.url);
          // 只在Content-Type为application/json时设置charset
          const contentType = proxyReq.getHeader('Content-Type');
          if (contentType && contentType.includes('application/json')) {
            proxyReq.setHeader('Content-Type', 'application/json;charset=UTF-8');
          }
          // 对于multipart/form-data，不要修改Content-Type，保留boundary参数
        },
        onError: (err, req, res) => {
          console.error('[Proxy Error]', err.message);
        }
    })
  );
};
