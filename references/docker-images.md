# Docker 镜像说明

## 使用的镜像

| 镜像 | 来源 | 大小（约） | 用途 |
|---|---|---|---|
| `mysql:8.0` | Docker Hub | 600 MB | 数据库 |
| `redis:7-alpine` | Docker Hub | 30 MB | 缓存 |
| `translationtoolservice:latest` | 本地构建 | 450 MB | 后端服务（基于 `shrbase:0.3`） |
| `translation-ui:latest` | 本地构建 | 150 MB | 前端服务（基于 nginx:alpine） |
| `node:16-alpine` | Docker Hub | 120 MB | 构建阶段使用（不会留在运行镜像中） |

## 基础镜像说明

### shrbase:0.3（公司内网）

后端 Dockerfile 基于 `nexus.sp5000.com:8082/repository/sieyuan_docker_host/shrbase:0.3`。

- 内网地址：`nexus.sp5000.com:8082`
- 只可在公司网络内拉取
- 已被合并到 `translationtoolservice:latest` 中，导出后无需单独搬运

### node:16-alpine

前端构建阶段使用，打包后 `dist` 产物复制到 `nginx:alpine` 镜像中运行。构建用镜像不会影响运行镜像大小。

## 国内镜像源

`pull-images.ps1` 已配置多个镜像源，自动切换：

| 公共镜像 | 阿里云镜像 | 网易镜像 |
|---|---|---|
| mysql:8.0 | registry.cn-hangzhou.aliyuncs.com/library/mysql:8.0 | hub-mirror.c.163.com/library/mysql:8.0 |
| redis:7-alpine | registry.cn-hangzhou.aliyuncs.com/library/redis:7-alpine | hub-mirror.c.163.com/library/redis:7-alpine |
| openjdk:11-jre-slim | registry.cn-hangzhou.aliyuncs.com/library/openjdk:11-jre-slim | hub-mirror.c.163.com/library/openjdk:11-jre-slim |

## 在公司网络导出镜像（搬运到迷你主机）

```bash
# 在已构建好的机器上执行
docker save translationtoolservice:latest translation-ui:latest mysql:8.0 redis:7-alpine -o translationtool-images.tar
```

## 在迷你主机上导入镜像

```bash
docker load -i ENV_package/translationtool-images.tar
```
