/** 根据 local/docker 分工生成 translation-ui 的 nginx 配置（挂载进容器） */

const fs = require("fs");
const path = require("path");

const API_PATTERN =
  "^/(userLogin|userManage|entry|backendInfo|checkManage|product|configManage|Syk|taskManage|translate|workbench|version|secondClassify|entryInfo|I18Sever|userPartiality|test)/";

function upstream(layer, isLocal) {
  if (isLocal) {
    if (layer === "java") return "host.docker.internal:18001";
    if (layer === "agent") return "host.docker.internal:18002";
  }
  if (layer === "java") return "translationtoolservice:18001";
  if (layer === "agent") return "terminology-agent:18002";
  throw new Error(`unknown layer: ${layer}`);
}

function buildNginxConf(javaLocal, agentLocal) {
  const javaUp = upstream("java", javaLocal);
  const agentUp = upstream("agent", agentLocal);
  return `server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location ~ ${API_PATTERN} {
        proxy_pass http://${javaUp};
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }

    location /agent/ {
        proxy_pass http://${agentUp};
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }
}
`;
}

function writeUiNginxConfig(root, localLayers) {
  const javaLocal = localLayers.includes("java");
  const agentLocal = localLayers.includes("agent");
  const dir = path.join(root, "scripts", "dev", ".generated");
  fs.mkdirSync(dir, { recursive: true });
  const confPath = path.join(dir, "nginx-ui.conf");
  fs.writeFileSync(confPath, buildNginxConf(javaLocal, agentLocal), "utf8");
  return confPath;
}

module.exports = { buildNginxConf, writeUiNginxConfig };
