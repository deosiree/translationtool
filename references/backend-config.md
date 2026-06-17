# 后端配置

## 配置文件

Docker 环境下后端加载两个配置文件的合并且 `docker` profile 配置覆盖 base：

| 文件 | 用途 |
|---|---|
| `application.yml` | 基础配置（非 Docker 环境） |
| `application-docker.yml` | Docker 环境专用配置（激活 spring.profiles.active=docker） |

配置文件在容器内路径：`/app/config/application*.yml`

## 关键配置项

### 数据源 (`spring.datasource`)

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/translationtool?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true&socketTimeout=60000&connectTimeout=5000
    username: root
    password: 123456
```

- `mysql` 是 Docker Compose 中的服务名，Docker DNS 自动解析
- `socketTimeout=60000`：查询超时 60 秒
- `connectTimeout=5000`：连接超时 5 秒

### LDAP

```yaml
ldap:
  ldapURL: ldap://10.17.196.129:389
  accountSuffix: "@sp5000.com"
  base: "DC=sp5000,DC=com"
  user: "Administrator"
  password: "Admin@1234"
```

> 这是公司内网 LDAP。外网无法访问时会自动降级到本地兜底账户（admin/admin123）。

### I18n 服务器

```yaml
I18server:
  url: http://10.17.14.250:18099/
```

> 公司内网 I18n 服务，外网不可用。部分功能（如从 i18n 服务器拉取词条源文件）会受到限制。

### Redis

```yaml
spring:
  redis:
    host: redis        # Docker 服务名
    port: 6379
    password: 210093
```

## 连接池（Druid）

详见 [[references/db-connection-pool]]。
