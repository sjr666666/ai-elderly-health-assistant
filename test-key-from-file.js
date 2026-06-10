const https = require('https');
const fs = require('fs');

// 直接从配置文件读取 key，模拟 Spring Boot 的行为
const content = fs.readFileSync('d:\\Develop\\aaagame\\backend\\src\\main\\resources\\application-local.properties', 'utf8');
const lines = content.split('\n');
let apiKey = '';
for (const line of lines) {
  if (line.startsWith('deepseek.api-key=')) {
    apiKey = line.substring('deepseek.api-key='.length).trim();
    break;
  }
}

console.log('Key 长度:', apiKey.length);
console.log('Key:', apiKey);
console.log('Key bytes:', Buffer.from(apiKey).toString('hex'));

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
