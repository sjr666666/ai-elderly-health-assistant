将正式环境证书文件放在本目录：

- `fullchain.pem`
- `privkey.pem`

证书不提交到 Git。生产启动命令：

```powershell
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d --build
```
