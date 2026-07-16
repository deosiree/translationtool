# 架构摘录（业务沙箱）

## 双后端

| 模块 | 路径 | 何时改 |
| --- | --- | --- |
| 前端 UI | `translation/` | 界面、回填、Electron |
| 新后端 Python | `terminology-agent/` | **新 API、新能力默认落点** |
| 遗留 Java | `translationtoolservice/` | **仅维护**，新业务禁止默认堆入 |

## 端口

UI `18000` → Java `18001` → Python `18002`
