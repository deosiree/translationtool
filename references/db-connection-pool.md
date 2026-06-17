# 数据库连接池配置（解决 Communications link failure）

## 问题

Docker 环境启动一段时间后，执行查询操作时出现：

```
Communications link failure
The last packet successfully received from the server was 20,012 milliseconds ago.
```

## 原因

后端使用 Druid 连接池连接 MySQL。`application.yml` 中虽然配置了 `test-while-idle`、`test-on-borrow`、`validation-query` 等连接有效性检测参数，但在 Docker 环境下（`application-docker.yml` 激活后），这些参数未被正确继承生效。导致：

1. MySQL 在空闲一段时间后关闭了连接
2. 连接池中仍持有这些已断开的连接
3. 下次查询拿到坏连接 → 报错

## 修复

在 `application-docker.yml` 中显式声明完整的 Druid 连接池配置：

```yaml
spring:
  datasource:
    initial-size: 5                    # 初始化连接数
    min-idle: 5                        # 最小空闲连接数
    max-active: 20                     # 最大活跃连接数
    max-wait: 60000                    # 获取连接的最大等待时间（毫秒）
    test-while-idle: true              # 空闲时检测连接有效性
    test-on-borrow: true               # 获取连接时检测有效性（关键）
    test-on-return: false              # 归还连接时不检测
    validation-query: SELECT 1         # 检测用 SQL
    validation-query-timeout: 3        # 检测超时（秒）
    time-between-eviction-runs-millis: 60000   # 空闲回收检测周期
    min-evictable-idle-time-millis: 1800000    # 连接最小空闲时间（30分钟）
    remove-abandoned: true             # 移除泄露连接
    remove-abandoned-timeout: 180      # 泄露连接超时（秒）
    log-abandoned: true                # 记录泄露日志
```

同时 JDBC URL 追加 `connectTimeout=5000`（连接超时 5 秒，防止网络异常卡死）。
