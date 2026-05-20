const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false,
      logLevel: 'debug',
      onProxyReq: (proxyReq, req, res) => {
        console.log('[Proxy] Forwarding:', req.method, req.url);
        // 确保UTF-8编码
        if (proxyReq.getHeader('Content-Type')) {
          proxyReq.setHeader('Content-Type', 'application/json;charset=UTF-8');
        }
      },
      onError: (err, req, res) => {
        console.error('[Proxy Error]', err.message);
      }
    })
  );
};
