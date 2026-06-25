# 系统架构

← [[README]] · [项目根 README](../README.md) | Agent 测试 → [[references/agent-testing]] · [agent-testing.md](agent-testing.md)

## 容器架构

```
┌─ 浏览器 ──────────────────────────────────────────┐
│  http://localhost:18000                            │
└──────────────────────┬────────────────────────────┘
                       │
┌──────────────────────▼────────────────────────────┐
│      translation-ui (nginx:alpine)                │
│                                                    │
│  ┌──────────────────────────────────────────────┐  │
│  │  Location / → /usr/share/nginx/html (SPA)    │  │
│  │                                               │  │
│  │  Location ~ ^/(userLogin|entry|taskManage     │  │
│  │              |...)/                           │  │
│  │    → proxy_pass http://translationtoolservice │  │
│  │                :18001                         │  │
│  └──────────────────────┬───────────────────────┘  │
└─────────────────────────┼──────────────────────────┘
                          │
┌─────────────────────────▼──────────────────────────┐
│     translationtoolservice (Spring Boot, JDK 11)    │
│                                                      │
│  ┌────────────────┐  ┌────────────────────────────┐ │
│  │ MySQL 8.0      │  │  Redis 7 (Alpine)          │ │
│  │ translation-   │  │  translation-redis          │ │
│  │ mysql          │  │  Port: 6379                 │ │
│  │ Port: 3306     │  │  Password: 210093           │ │
│  │ User: root     │  │                             │ │
│  │ Pass: 123456   │  │                             │ │
│  └────────────────┘  └────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

## 端口映射

| 服务 | 内部端口 | 宿主机端口 | 说明 |
|---|---|---|---|
| translation-ui | 80 | 18000 | 前端页面（浏览器访问） |
| translationtoolservice | 18001 | 18001 | 后端 API（一般不直接访问） |
| mysql | 3306 | 3306 | 数据库（仅调试用） |
| redis | 6379 | 6379 | 缓存（仅调试用） |

## 数据流

```
用户操作 → 浏览器 → localhost:18000
                → nginx 提供静态资源 (SPA)
                → JS 发起 API 请求（同源 /xxx/xxx）
                → nginx 匹配 API 前缀 → 转发到 translationtoolservice:18001
                → 后端处理 → MySQL/Redis
                → 响应原路返回
```

## 项目目录结构

```
translationtool/
├── translation/                  # 前端 Vue 3 项目
│   ├── public/config/index.js    # 前端配置（serverURL 等）
│   ├── nginx.conf                # nginx 反向代理配置
│   ├── Dockerfile                # 前端容器构建（node:16 → nginx）
│   ├── src/                      # 源码
│   └── package.json
├── translationtoolservice/       # 后端 Spring Boot 项目
│   ├── src/main/
│   │   ├── java/                 # Java 源码 (298 个文件)
│   │   └── resources/            # 配置文件
│   │       ├── application.yml         # 基础配置
│   │       └── application-docker.yml  # Docker 环境配置
│   ├── Dockerfile                # 后端容器构建（shrbase:0.3 → JDK 11）
│   └── pom.xml
├── terminology-agent/            # AI 术语 Agent（FastAPI + LangGraph）
│   ├── app/                      # 业务代码 + 共置 tests/
│   ├── devtools/                 # Trace Demo（不参与 pytest）
│   └── README.md                 # 模块入口
├── references/                   # 开发/部署/测试文档沉淀
│   ├── agent-testing.md          # Agent 测试与 Trace 可视化
│   └── 本地开发.md
├── db/init/                      # MySQL 初始化脚本
├── ENV_package/                  # 离线环境包（镜像、JDK、Maven）
├── docker-compose.yml            # Docker Compose 编排
├── pull-images.ps1               # 国内镜像拉取脚本
└── README.md
```

Agent 测试与轨迹可视化详见 [[references/agent-testing]] · [agent-testing.md](agent-testing.md)。
