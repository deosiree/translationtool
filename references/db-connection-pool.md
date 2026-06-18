# 数据库连接池配置（解决 Communications link failure）

## 问题

Docker 环境启动后，执行查询操作时出现：

```
Communications link failure
The last packet successfully received from the server was 20,025 milliseconds ago.
```

## 原因

Docker 容器间网络存在空闲连接超时（约 20 秒静默断开 TCP 连接）。后端 Druid 连接池未正确验证连接有效性，导致使用了已断开的连接。

两个因素叠加：
1. **Docker 网络层**在约 20 秒无数据后断开空闲 TCP 连接
2. **Druid 连接池**未配置连接有效性检测，继续持有已断开的连接

## 修复

在 `application-docker.yml` 的 `spring.datasource.druid.*` 下显式声明：

```yaml
spring:
  datasource:
    druid:
      initial-size: 5                          # 初始化连接数
      min-idle: 5                              # 最小空闲连接数
      max-active: 20                           # 最大活跃连接数
      max-wait: 60000                          # 获取连接最大等待（毫秒）
      test-on-borrow: true                     # 每次借连接前先 SELECT 1 验证（关键）
      test-on-return: false                    # 归还连接时不检测
      validation-query: SELECT 1               # 检测用 SQL
      validation-query-timeout: 3              # 检测超时（秒）
      keep-alive: false                        # 关闭周期性批量验证（避免 CPU 飙升）
      time-between-eviction-runs-millis: 60000 # 空闲回收检测周期（60秒）
      remove-abandoned: true                   # 移除泄露连接
      remove-abandoned-timeout: 180            # 泄露连接超时（秒）
      log-abandoned: true                      # 记录泄露日志
```

> ⚠️ **关键点**：
> - 必须使用 **`spring.datasource.druid.*`** 前缀（`spring.datasource.*` 扁平写法无效）
> - **不要**设置 `minEvictableIdleTimeMillis`（Druid 强制最小值 30000ms，30 秒，但 Docker 网络 20 秒就断开，设置过低值会被 Druid 拒绝）
> - **`keepAlive=true`** 是关键——它让回收线程无条件验证所有空闲连接，不依赖 `minEvictableIdleTimeMillis`
> - **`testOnBorrow=true`** 是第二道防线——每次借连接前先验证

同时 JDBC URL 追加：
- `connectTimeout=5000` — 连接超时 5 秒
- `autoReconnect=true` — 断开时自动重连
