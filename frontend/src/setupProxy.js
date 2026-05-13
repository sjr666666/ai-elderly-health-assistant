module.exports = function(app) {
  app.proxy = true;
  app.use(function proxyMiddleware(req, res, next) {
    if (req.path.startsWith('/api')) {
      const http = require('http');
      // 使用 originalUrl 或 url 来保留查询参数
      const options = {
        hostname: 'localhost',
        port: 8080,
        path: req.originalUrl || req.url,
        method: req.method,
        headers: {
          ...req.headers,
          host: 'localhost:8080'
        }
      };
      
      const proxyReq = http.request(options, function(proxyRes) {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res, { end: true });
      });
      
      proxyReq.on('error', function(err) {
        next(err);
      });
      
      req.pipe(proxyReq, { end: true });
    } else {
      next();
    }
  });
};
