# 前端配置

## 配置文件位置

`translation/public/config/index.js`

## 配置项

```javascript
exports.app = {
    electron: false,      // true = Electron 客户端模式, false = Web 模式
    serverURL: ''         // 后端 API 基础 URL
}
```

## serverURL 说明

| 值 | 场景 |
|---|---|
| `''`（空字符串） | **当前 Docker 部署**：同源请求，通过 nginx 代理到后端 |
| `'http://localhost:18001/'` | 单机调试（前端在浏览器直接访问后端端口） |
| `'http://10.17.x.x:18001/'` | 旧的非 Docker 部署（直连后端服务器） |

### 为什么要改为空字符串

最初 `serverURL` 设置为 `http://10.17.196.125:18001/`，打包到 Docker 后浏览器无法访问这个公司内网 IP。改为空字符串后：

1. 所有 API 请求变成**同源**（如 `http://localhost:18000/userLogin/login`）
2. nginx 收到请求后根据 URL 前缀匹配，**反向代理**到 `http://translationtoolservice:18001`

这样无论部署在哪个 IP，都不需要修改前端配置。

## 注意事项

- `public/config/index.js` 被 **Webpack 编译时内联**进 JS bundle
- 修改后必须 `npm run build` + `docker compose build translation-ui` 才生效
- 可通过 `docker compose logs translation-ui` 确认 nginx 是否正常工作
