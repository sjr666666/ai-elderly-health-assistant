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
        console.log('[Proxy] Forwarding:', req.method, req.url, '->', proxyReq.path);
      },
      onError: (err, req, res) => {
        console.error('[Proxy Error]', err.message);
      }
    })
  );
};
