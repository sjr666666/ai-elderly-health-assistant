const https = require('https');

const data = JSON.stringify({
  model: 'deepseek-chat',
  messages: [{ role: 'user', content: '你好' }]
});

const req = https.request({
  hostname: 'api.deepseek.com',
  port: 443,
  path: '/v1/chat/completions',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(data),
    'Authorization': 'Bearer sk-***REDACTED***'
  }
}, res => {
  let d = '';
  res.on('data', c => d += c);
  res.on('end', () => {
    console.log('Status:', res.statusCode);
    console.log('Body:', d.substring(0, 300));
  });
});

req.on('error', e => console.log('Error:', e.message));
req.write(data);
req.end();
