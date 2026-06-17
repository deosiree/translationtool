# ========== 构建阶段 ==========
FROM node:16-alpine AS builder

WORKDIR /app

# 先复制 package.json 和 lock 文件，利用 Docker 缓存层
COPY package*.json ./
RUN npm install

# 复制源码
COPY public/ ./public/
COPY src/ ./src/
COPY *.config.js *.json ./

# 构建
RUN npm run build

# ========== 运行阶段 ==========
FROM nginx:alpine

# 将构建产物复制到 nginx 目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 nginx 反向代理配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
