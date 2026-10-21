FROM eclipse-temurin:8-jre-jammy

WORKDIR /app
COPY target/app.jar /app/app.jar

EXPOSE 18001

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
