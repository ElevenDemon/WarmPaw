FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY 3.petSpringboot/pom.xml 3.petSpringboot/settings.xml ./
RUN mvn -B -ntp -gs settings.xml -s settings.xml dependency:go-offline
COPY 3.petSpringboot/src ./src
# 集成测试依赖隔离 PostgreSQL；镜像构建仅打包，运行时由 Flyway 初始化。
RUN mvn -B -ntp -gs settings.xml -s settings.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p uploads logs && chown -R 10001:10001 /app
COPY --from=build --chown=10001:10001 /build/target/pet-backend-1.0.0.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
