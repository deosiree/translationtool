#!/bin/bash

docker rm -f mysql 

docker run -it -d \
  -v /etc/localtime:/etc/localtime:ro \
  -v /etc/timezone:/etc/timezone:ro \
  -v /home/shr/mydata/mysql/conf:/etc/mysql/conf.d \
  -v /mydata/mysql/data:/var/lib/mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  --name mysql \
  mysql

