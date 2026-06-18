# 发布与部署流程

本文档说明如何将本项目打包、发布到 GitHub Release，以及在其他机器上部署。

---

## 一、发布到 GitHub Release

### 前置条件

- 提交所有改动到 `docker` 分支并推送
- 确认 `.gitignore` 已排除 `ENV_package/`、`*.tar`、`target/`、`node_modules/` 等

### 步骤

#### 1. 打 Tag 并推送

```bash
# 在 docker 分支上打版本标签
git tag v1.0.0-docker

# 推送到 GitHub
git push origin v1.0.0-docker
```

#### 2. 在 GitHub 网页创建 Release

1. 打开 https://github.com/deosiree/translationtool/releases
2. 点击 **Create a new release**（或 **Draft a new release**）
3. **Choose a tag**：选择刚推送的 `v1.0.0-docker`
4. **Release title**：`词条翻译工具 Docker 版 v1.0.0`
5. **描述内容参考**：

```markdown
## 词条翻译工具 Docker 版 v1.0.0

### 部署步骤
1. 下载 `translationtool-images.tar.gz`
2. `docker load -i translationtool-images.tar.gz`
3. `docker compose up -d`
4. 访问 http://localhost:18000
5. 登录：`admin / admin123`

### 系统要求
- Docker & Docker Compose v2
- WSL2（Windows）或 Linux

### 包含的镜像
- mysql:8.0
- redis:7-alpine
- translationtoolservice:latest（Spring Boot 后端）
- translation-ui:latest（Vue 3 + nginx 前端）
```

6. **上传附件**：拖拽 `translationtool-images.tar.gz` 到附件区域
7. 点击 **Publish release**

---

## 二、在其他机器上部署

### 方式一：从 Release 下载镜像包（推荐）

```bash
# 1. 克隆代码
git clone git@github.com:deosiree/translationtool.git
cd translationtool

# 2. 下载 Release 中的 translationtool-images.tar.gz

# 3. 加载镜像
docker load -i translationtool-images.tar.gz

# 4. 启动
docker compose up -d

# 5. 访问 http://localhost:18000，用 admin / admin123 登录
```

### 方式二：从源代码构建

```bash
# 1. 克隆
git clone git@github.com:deosiree/translationtool.git
cd translationtool

# 2. 构建并启动
docker compose up -d
```

> 方式二需要外网访问 Docker Hub。后端基础镜像 `shrbase:0.3` 在公司 Nexus 内网，外网不可用时会构建失败。

---

## 三、数据库说明

GitHub 仓库中的 `db/init/schema.sql` 是**仅含表结构的空数据库脚本**：

| 文件 | 大小 | 内容 | 用途 |
|---|---|---|---|
| `db/init/schema.sql` | ~368 KB | CREATE TABLE + 种子数据（角色、菜单、兜底账户） | **推送到 GitHub，Release 使用** |
| `db/init/run.sql` | ~390 MB | 全量数据（40 万词条 + 30 万翻译） | 本地保留，**不提交到 GitHub** |

### 关键理解：镜像不含业务数据

`docker save` 导出的是 **Docker 镜像**，不包含 `docker compose` 的 volume 数据。MySQL 的业务数据（词条、翻译）存储在 `mysql-data` volume 中，**不会被打包进 tar.gz**。

所以导出的 `translationtool-images.tar.gz` 已经是"空库"了。别人下载后：
1. `docker load -i translationtool-images.tar.gz` → 加载 4 个镜像
2. `docker compose up -d` → MySQL 首次启动，自动执行 `db/init/schema.sql`
3. 得到一张只有表结构 + admin 兜底账户的空数据库

### 本地开发 vs Release 打包

```
本地开发：db/init/run.sql（全量数据） → docker compose up → 带数据运行
Release：  db/init/schema.sql（空库） → docker save → 导出不带数据的镜像
```

切换方式：
```bash
# 本地带数据开发（用 run.sql）
cp db/init/run.sql db/init/init.sql
docker compose up -d

# 打包 Release（用 schema.sql）
cp db/init/schema.sql db/init/init.sql
# 如已运行过带数据的容器，先清空 volume
docker compose down -v
# 重新导出并压缩（参考下方第四节）
```

> `.gitignore` 已排除 `db/init/run.sql`，不会提交到 GitHub。

---

## 四、Docker 镜像导出与压缩

### 导出命令

```bash
# 导出 4 个镜像为 tar 包
docker save translationtoolservice:latest translation-ui:latest mysql:8.0 redis:7-alpine -o translationtool-images.tar
```

### 压缩

```bash
# 压缩为 .tar.gz（通常可缩小 30-50%）
gzip translationtool-images.tar
# 得到 translationtool-images.tar.gz
```

### 加载

```bash
# 支持 .tar 和 .tar.gz
docker load -i translationtool-images.tar.gz
```

---

## 五、重新打包（更新镜像后）

如果修改了代码并重新构建了镜像，需要重新导出：

```bash
# 1. 重新构建后端（需要先 mvn package）
cd translationtoolservice && mvn package -DskipTests && cd ..
docker compose build translationtoolservice

# 2. 重新构建前端
docker compose build translation-ui

# 3. 导出镜像
rm -f ENV_package/translationtool-images.tar.gz
docker save translationtoolservice:latest translation-ui:latest mysql:8.0 redis:7-alpine | gzip > ENV_package/translationtool-images.tar.gz

# 4. 上传到 GitHub Release
```
