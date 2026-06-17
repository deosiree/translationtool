# Docker 部署

## 安装 Docker

### Windows / WSL2

1. **启用 WSL2**
   ```powershell
   wsl --install
   ```
   重启后设置 WSL2 为默认版本：
   ```powershell
   wsl --set-default-version 2
   ```

2. **安装 Docker Desktop**
   - 下载：https://docs.docker.com/get-docker/
   - 安装后打开 **Settings → Resources → WSL Integration**
   - 启用你的 WSL 发行版（如 `Ubuntu`）

3. **验证安装**
   ```powershell
   wsl -d Ubuntu
   docker --version
   docker compose version
   ```

### Linux (Ubuntu/Debian)

```bash
# 一键安装
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 退出重新登录后验证
docker --version
docker compose version
```

## 部署

### 方式一：导入镜像（推荐，离线可用）

从公司网络导出好的镜像文件：`ENV_package/translationtool-images.tar`

```bash
# 1. 加载镜像（约 2 GB，需等待）
docker load -i ENV_package/translationtool-images.tar

# 2. 验证镜像已加载
docker images
# 应看到: translationtoolservice, translation-ui, mysql, redis

# 3. 启动
docker compose up -d
```

### 方式二：在线构建

```bash
# 1. 克隆
git clone <repo-url> translationtool
cd translationtool

# 2. 先拉基础镜像（国内用户）
.\pull-images.ps1

# 3. 构建并启动
docker compose up -d
```

> 方式二需要外网访问 Docker Hub 及公司 Nexus（shrbase:0.3 可能拉不到）。

## 重新构建

```bash
# 前端改动后
docker compose build translation-ui && docker compose up -d

# 后端改动后（需先本地编译 JAR）
cd translationtoolservice && mvn package -DskipTests && cd ..
docker compose build translationtoolservice && docker compose up -d
```

## 导出镜像（在公司网络打包用）

```bash
docker save translationtoolservice:latest translation-ui:latest mysql:8.0 redis:7-alpine -o translationtool-images.tar
```
