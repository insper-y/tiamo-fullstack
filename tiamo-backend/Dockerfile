# ============================================================
# Tiamo AI 后端 - Dockerfile
# 多阶段构建：Maven 编译 + JRE 运行
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并编译
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 安装必要工具
RUN apk add --no-cache tzdata curl

# 设置时区
ENV TZ=Asia/Shanghai
RUN cp /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "-Xmx384m", "-Xms128m", "app.jar"]
