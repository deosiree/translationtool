#!/bin/bash

docker rm -f translationtoolservice

docker run -it -d -e TZ=Asia/Shanghai  \
--restart always \
-p 18001:18001 \
-p 10023:10023 \
--workdir /app \
-v /home/shr/translationtoolservice:/app \
-v /home/shr/translationtoolservice/config/*:/config \
--name translationtoolservice \
nexus.sp5000.com:8082/repository/sieyuan_docker_host/shrbase:0.3 \
sh -c "java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10023 -Djava.awt.headless=true -Duser.timezone=Asia/Shanghai -Dspring.profiles.active=pro -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8  -jar ./translationtoolservice-0.0.1-SNAPSHOT.jar --spring.config.location=./config/application.yml" 
