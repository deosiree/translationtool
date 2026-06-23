# translation

## 环境要求

| 工具 | 版本 |
|------|------|
| Node.js | v20.18.0 (推荐通过 nvm 管理) |
| pnpm | v10.34.3 (随 Node.js 自带) |
| npm | v10.8.2 (随 Node.js 自带) |

> 本项目为纯前端项目，无需 Python 和 Java 环境。

## 镜像源配置（国内加速）

### npm 配置淘宝镜像

```bash
npm config set registry https://registry.npmmirror.com
```

### pnpm 配置淘宝镜像

```bash
pnpm config set registry https://registry.npmmirror.com
```

### pip 配置（如需要）

```bash
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
```

### Electron 镜像源

```bash
pnpm config set electron_mirror https://npmmirror.com/mirrors/electron/
```

## 快速开始

### 1. 安装依赖

```bash
pnpm install
```

> **注意**：如果 `pnpm install` 过程中出现 `electron` 下载失败或卡住，是因为 GitHub 下载慢，请按以下步骤单独安装 electron：

```bash
# 设置淘宝镜像源后重新下载 electron
$env:ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"
node .\node_modules\.pnpm\electron@13.6.9\node_modules\electron\install.js
```

### 2. 启动开发服务器

```bash
pnpm run serve
```

### 3. 构建生产版本

```bash
pnpm run build
```

### 4. Electron 桌面应用

```bash
# 启动 Electron 开发模式
pnpm run electron:serve

# 构建 Electron 桌面应用
pnpm run electron:build
```

## 常用命令

| 命令 | 说明 |
|------|------|
| `pnpm run serve` | 启动 Web 开发服务器 |
| `pnpm run lint` | 代码检查 |
| `pnpm run test` | 运行测试 |
| `pnpm run test:coverage` | 测试覆盖率 |

## 常见问题

### 项目跑起来了但有很多 `[BABEL] Note` 和 `warning`，正常吗？

**正常。** 这些只是警告（warning），不是错误。项目已经成功运行在 `http://localhost:8080/`。

常见的警告及其含义：

| 警告 | 说明 |
|------|------|
| `BABEL Note: The code generator has deoptimised...` | 某些第三方库体积过大，babel 跳过转译，不影响功能 |
| `<tr> cannot be child of <table>` | HTML 规范提示，`<tr>` 外层不应该有 `<div>` 包裹，但浏览器依然能正常渲染 |

### Electron 安装失败

如果遇到 `Electron failed to install correctly` 错误，说明 electron 二进制文件未正确下载。

**原因**：pnpm v10 默认阻止依赖包的 postinstall 脚本执行，导致 electron 无法自动下载。

**解决方案**：

```bash
# 1. 设置镜像源（国内加速）
$env:ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"

# 2. 手动运行 electron 安装脚本
node .\node_modules\.pnpm\electron@13.6.9\node_modules\electron\install.js

# 3. 或者先创建 .npmrc 配置，再重装
echo "onlyBuiltDependencies[]=electron" > .npmrc
pnpm install
```

### pnpm 命令找不到

如果提示 `pnpm: 无法识别`，请确保 Node.js 已正确安装且环境变量已配置：

```bash
nvm install 20.18.0
nvm use 20.18.0
```

### lodash 找不到 `Can't resolve 'lodash'`

`lodash` 可能未安装，执行以下命令：

```bash
pnpm add lodash
```

## 技术栈

- Vue 3 + Composition API
- Ant Design Vue 3
- Vuex 4 + vuex-persistedstate
- Electron 13
- Vite / Vue CLI
