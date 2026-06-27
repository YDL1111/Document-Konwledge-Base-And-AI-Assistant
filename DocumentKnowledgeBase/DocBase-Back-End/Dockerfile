FROM maven:3.9-eclipse-temurin-17 AS build-stage

WORKDIR /app

# 复制 pom.xml 文件
COPY pom.xml .
COPY docbase-admin/pom.xml ./docbase-admin/
COPY docbase-common/pom.xml ./docbase-common/
COPY docbase-domain/pom.xml ./docbase-domain/
COPY docbase-infrastructure/pom.xml ./docbase-infrastructure/

# 下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY docbase-admin ./docbase-admin
COPY docbase-common ./docbase-common
COPY docbase-domain ./docbase-domain
COPY docbase-infrastructure ./docbase-infrastructure

# 构建应用
RUN mvn clean package -DskipTests -B

# 第二阶段：运行
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=build-stage /app/docbase-admin/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
