const https = require('https');

const apiKey = 'sk-***REDACTED***';
const data = JSON.stringify({
  model: 'deepseek-chat',
  messages: [{ role: 'user', content: '你好' }],
  max_tokens: 1000,
  temperature: 0.3
});

const req = https.request({
  hostname: 'api.deepseek.com',
  port: 443,
  path: '/v1/chat/completions',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(data),
    'Authorization': 'Bearer ' + apiKey
  }
}, res => {
  let d = '';
  res.on('data', c => d += c);
  res.on('end', () => {
    console.log('Status:', res.statusCode);
    console.log('Body:', d.substring(0, 500));
  });
});

req.on('error', e => console.log('Error:', e.message));
req.write(data);
req.end();
