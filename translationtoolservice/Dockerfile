# ========== 运行阶段（用公司 shrbase）==========
# 使用方式：
#   1. 先本机编译：cd translationtoolservice && mvn package -DskipTests
#   2. 再 Docker 构建：docker compose build translationtoolservice
#
FROM nexus.sp5000.com:8082/repository/sieyuan_docker_host/shrbase:0.3

WORKDIR /app

# 复制本机编译好的 JAR
COPY target/translationtoolservice-0.0.1-SNAPSHOT.jar ./app.jar

# 复制配置文件
COPY src/main/resources/application.yml ./config/application.yml
COPY src/main/resources/application-docker.yml ./config/application-docker.yml
COPY src/main/resources/config/ ./config/
COPY src/main/resources/logback-spring.xml ./config/

EXPOSE 18001

ENTRYPOINT ["java", \
    "-Djava.awt.headless=true", \
    "-Duser.timezone=Asia/Shanghai", \
    "-Dfile.encoding=UTF-8", \
    "-Dsun.jnu.encoding=UTF-8", \
    "-Dspring.profiles.active=docker", \
    "-jar", "./app.jar", \
    "--spring.config.location=file:./config/"]
