# 本地 Java 后端 Docker

## 快速使用

在仓库根目录运行。

日常启动已有镜像和容器：

```powershell
.\script\build\docker-local.ps1
```

按仓库现有远端与分支流程拉取最新后端代码后，重新打包并更新镜像与容器：

```powershell
.\script\build\docker-local.ps1 -Build
```

更新时必须传 `-Build`。不传时会启动已有镜像；若本地镜像或打包产物不存在，入口脚本会按需构建。

## 环境与数据

- Docker Desktop 必须已启动。
- 首次构建或使用 `-Build` 时需要 JDK 8 和 Maven。JDK 8 可通过 `JAVA_HOME_8_X64` 或 `JAVA8_HOME` 指定。
- 本地配置必须存在于 `translationtoolservice/.env.local`。该文件包含本机凭据，已被 Git 忽略，不要提交。
- Java 后端映射到 `127.0.0.1:18001`；Redis 由 Compose 管理并使用持久卷。
- MySQL 继续使用 `.env.local` 配置的现有数据库。构建、启动或停止容器不会恢复或覆盖 MySQL 数据。
- `application.yml` 以只读方式挂载到后端容器。

## 状态、日志与停止

Compose 命令需要显式加载本地环境文件：

```powershell
docker compose --env-file .\translationtoolservice\.env.local -f .\docker-compose.yml --project-directory . ps
docker compose --env-file .\translationtoolservice\.env.local -f .\docker-compose.yml --project-directory . logs --tail 100 java-backend
docker compose --env-file .\translationtoolservice\.env.local -f .\docker-compose.yml --project-directory . stop
```

`stop` 会停止 Java 和 Redis 容器，不会停止外部 MySQL 服务。重新运行启动脚本即可启动后端。

启动脚本会等待后端 HTTP/CORS 预检通过后返回。浏览器仍按协议、主机和端口判断来源；本地前端与 `18001` 后端使用不同端口，因此请求属于跨域请求。
