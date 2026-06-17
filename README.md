# 词条翻译工具 (Translation Tool)

企业级多语种词条翻译管理平台，提供词条管理、翻译、审核、版本管理、分支新建等功能。

适用于 **WSL2 / Docker** 环境部署。

---

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| **前端** | Vue 3 + Ant Design Vue | Node.js 16.x (构建), nginx (运行) |
| **后端** | Spring Boot 2.7.7 + MyBatis-Plus + Druid | JDK 11 (运行时) |
| **数据库** | MySQL 8.0 | — |
| **缓存** | Redis 7 (Alpine) | — |
| **反向代理** | nginx Alpine (内嵌于前端容器) | — |

---

## 物料清单 (ENV_package)

项目根目录下的 `ENV_package/` 已包含所有离线安装所需文件：

| 文件 | 大小 | 用途 |
|---|---|---|
| `translationtool-images.tar` | ~2 GB | 4 个 Docker 镜像（mysql:8.0, redis:7-alpine, translationtoolservice:latest, translation-ui:latest） |
| `OpenJDK11U-jdk_x64_windows_hotspot_11.0.26_4.msi` | 175 MB | JDK 11（如需本地编译后端） |
| `apache-maven-3.9.16-bin.zip` | 9.4 MB | Maven（如需本地编译后端） |
| ~~`OpenJDK8U-...msi`~~ | ~~89 MB~~ | **不需要**，JDK 11 向下兼容 1.8 |

---

## Docker 部署

### 前提条件

#### 安装 Docker

**Windows / WSL2：**

```bash
# 1. 安装 WSL2（如果未安装）
wsl --install

# 2. 安装 Docker Desktop
#    从 https://docs.docker.com/get-docker/ 下载安装
#    安装后 设置 → Resources → WSL Integration → 启用你的 WSL 发行版
```

**Linux：**

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
# 退出重新登录
```

验证安装：`docker --version && docker compose version`

#### 安装 Git

```bash
# Windows: https://git-scm.com/download/win
# Linux: sudo apt install git
# 验证: git --version
```

### 部署步骤

#### 方式一：使用已导出的镜像（推荐，最快）

如果你有 `ENV_package/translationtool-images.tar`（从公司网络导出）：

```bash
# 1. 克隆仓库（或直接拷贝项目文件夹）
git clone <repo-url> translationtool
# 或直接复制整个 translationtool 文件夹到迷你主机

# 2. 加载镜像
docker load -i ENV_package/translationtool-images.tar

# 3. 启动
docker compose up -d
```

#### 方式二：在线拉取 + 构建

```bash
# 1. 克隆仓库
git clone <repo-url> translationtool
cd translationtool

# 2. 【可选】国内用户先拉取基础镜像
# Windows PowerShell:
.\pull-images.ps1

# 3. 一键启动（首次会自动构建）
docker compose up -d
```

> 方式二需要外网访问 Docker Hub 和公司 Nexus 仓库（shrbase:0.3）。
> 如果 `shrbase:0.3` 拉不到，请回到公司网络用方式一导出镜像。

### 验证

```bash
# 查看所有容器状态
docker compose ps

# 查看后端日志
docker compose logs translationtoolservice
```

等待约 30 秒后端启动完成后，访问 **http://localhost:18000**。

---

## 登录

| 账户 | 密码 | 角色 | 部门 |
|---|---|---|---|
| `admin` | `admin123` | 管理员 | 通用平台部 |

> 本系统默认走 LDAP 认证。内网无 LDAP 时自动降级到本地兜底账户（即上表），详见 [[references/auth-fallback]]。

---

## 重新构建

修改代码后需要重新构建对应镜像：

```bash
# 仅前端改动
docker compose build translation-ui && docker compose up -d

# 仅后端改动（需先本机编译 JAR）
cd translationtoolservice && mvn package -DskipTests && cd ..
docker compose build translationtoolservice && docker compose up -d
```

---

## 端口映射

| 服务 | 容器端口 | 宿主机端口 |
|---|---|---|
| translation-ui (前端) | 80 | 18000 |
| translationtoolservice (后端) | 18001 | 18001 |
| mysql | 3306 | 3306 |
| redis | 6379 | 6379 |

---

## 常见问题

- **Q: 登录报 `Communications link failure`？** → [[references/db-connection-pool]]
- **Q: 登录显示"部门信息未找到"？** → [[references/department-setup]]
- **Q: 访问后返回 502？** → 后端尚未启动完成，等 30 秒再刷新
- **Q: 外网如何部署？** → 见上方式一（[使用已导出的镜像](#方式一使用已导出的镜像推荐最快)）
