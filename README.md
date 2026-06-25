# 词条翻译工具 (Translation Tool)

企业级多语种词条翻译管理平台，提供词条管理、翻译、审核、版本管理、分支新建等功能。

适用于 **WSL2 / Docker** 环境部署。

---

## 技术栈

| 层 | 技术 | 版本 | 目录 |
|---|---|---|---|
| **前端** | Vue 3 + Ant Design Vue + Electron | Node.js 20.x | `translation/` |
| **AI 术语代理** | Python FastAPI + LangGraph | Python >= 3.11 | `terminology-agent/` |
| **后端服务** | Spring Boot 2.7.7 + MyBatis-Plus + Druid | JDK 11 (编译), JDK 8 (运行) | `translationtoolservice/` |
| **数据库** | MySQL 8.0 | — | — |
| **缓存** | Redis 7 (Alpine) | — | — |
| **反向代理** | nginx Alpine (内嵌于前端容器) | — | — |

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

## 镜像源配置（国内加速）

国内首次搭建时，npm / pip / Maven 下载依赖极慢，建议先配置镜像源：

### npm / pnpm 镜像

```powershell
# npm 配置淘宝镜像
npm config set registry https://registry.npmmirror.com

# pnpm 配置淘宝镜像
pnpm config set registry https://registry.npmmirror.com

# Electron 二进制文件镜像
pnpm config set electron_mirror https://npmmirror.com/mirrors/electron/
```

### pip 镜像（terminology-agent）

```bash
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
```

### Maven 镜像（translationtoolservice）

在 `%USERPROFILE%\.m2\settings.xml` 中添加：

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>central</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

---

## 本地开发

### 一键全栈（推荐）

```powershell
cd F:\Documents\Repertory\Sieyuan\translationtool
pnpm setup && pnpm install
pnpm -C translationtoolservice install && pnpm -C terminology-agent install
pnpm dev
# → http://localhost:18000
```

根目录 7 种 `pnpm dev:*` 命令保证 **UI + Java + Agent + infra 四层全栈**；详见 [[references/本地开发]] · [references/本地开发.md](references/本地开发.md)。

| 命令 | 本地 | Docker |
|------|------|--------|
| `pnpm dev` | UI + Java + Agent | infra |
| `pnpm dev:ui` | UI | Java + Agent + infra |
| `pnpm dev:java` | Java | UI + Agent + infra |
| `pnpm dev:agent` | Agent | UI + Java + infra |

需 **Windows Terminal**（`wt.exe`）自动分 pane；无 wt 时脚本会打印手动命令。

### 仅前端（translation/）

```powershell
cd translation
pnpm install
pnpm dev:ui   # 或在仓库根目录执行
```

**dev server proxy 四档**（根命令自动选择，手动调试见 `translation/package.json`）：

| 命令 | Java | Agent |
|------|------|-------|
| `serve` | Docker :18001 | Docker :18002 |
| `dev` | 本机 :18001 | 本机 :18002 |
| `dev:dockerJava` | :18001 | :18002 |
| `dev:dockerPy` | :18001 | :18002 |

访问 **http://localhost:18000**（webpack dev server 与 Docker UI 统一端口）。

整体验收（不改代码）：`docker compose up -d` → http://localhost:18000

> 如果 `pnpm install` 卡住，见上方 [镜像源配置](#镜像源配置国内加速)。

### Python AI 代理（terminology-agent）

```powershell
cd terminology-agent

# 创建虚拟环境并安装依赖（含 pytest 等开发工具）
pip install -e ".[dev]"
# 或使用 venv:
python -m venv .venv
.venv\Scripts\activate
pip install -e ".[dev]"

# 复制环境变量配置
copy .env.example .env
# 编辑 .env 填入你的 LLM_API_KEY（DeepSeek / OpenAI 等）

# 运行测试（无需 MySQL / LLM Key）
pytest -v

# 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 18002 --reload
```

> 测试用例清单与 Agent Trace 可视化见 [[references/agent-testing]] · [references/agent-testing.md](references/agent-testing.md)。Agent 模块详情见 [[terminology-agent/README]] · [terminology-agent/README.md](terminology-agent/README.md)。

### Java 后端（translationtoolservice）

```powershell
# 1. 确保已安装 JDK（项目提供）
# 项目提供了 JDK 切换脚本：
.\use-jdk8.ps1    # 切换到 JDK 8（运行时兼容）
.\use-jdk20.ps1   # 切换到 JDK 20（如需新版）

# 2. 确保已安装 Maven（ENV_package 中提供了 apache-maven-3.9.16）

# 3. 编译打包
cd translationtoolservice
mvn clean package -DskipTests

# 4. 运行
java -jar target\translationtoolservice-0.0.1-SNAPSHOT.jar
```

> 首次 `mvn package` 会下载大量依赖，请先配置 [Maven 镜像源](#maven-镜像translationtoolservice)。

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

> 本系统默认走 LDAP 认证。内网无 LDAP 时自动降级到本地兜底账户（即上表）。

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

## 参考文档 (References)

详细开发、部署、配置与 Agent 测试文档见 `references/` 目录。

| 文档 | 说明 |
|------|------|
| [[references/README]] | References 总索引 |
| [references/README.md](references/README.md) | ↑ 同上（Markdown 链接） |
| [[references/本地开发]] | Agent 优先本地开发、场景 A/B |
| [references/本地开发.md](references/本地开发.md) | ↑ 同上 |
| [[references/agent-testing]] | **Agent 测试用例与 Trace 可视化** |
| [references/agent-testing.md](references/agent-testing.md) | ↑ 同上 |
| [[references/architecture]] | 系统架构 |
| [references/architecture.md](references/architecture.md) | ↑ 同上 |
| [[references/docker-deploy]] | Docker 部署 |
| [references/docker-deploy.md](references/docker-deploy.md) | ↑ 同上 |
| [[references/release-guide]] | 发布流程 |
| [references/release-guide.md](references/release-guide.md) | ↑ 同上 |

Agent 模块入口：[[terminology-agent/README]] · [terminology-agent/README.md](terminology-agent/README.md)

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

- **Q: 登录报 `Communications link failure`？** → 连接池配置问题，使用 `keepAlive=true` + `testOnBorrow=true` 解决
- **Q: 登录显示"部门信息未找到"？** → 数据库编码问题，用 `UNHEX('E9809A...')` 写入 UTF-8 字节
- **Q: 访问后返回 502？** → 后端尚未启动完成，等 30 秒再刷新
- **Q: 外网如何部署？** → 见上方式一（[使用已导出的镜像](#方式一使用已导出的镜像推荐最快)）
