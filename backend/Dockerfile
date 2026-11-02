# syntax=docker/dockerfile:1
# API Gigaverse3D — imagem de produção (multi-stage, usuário não-root, sem JDK nem fontes no runtime).
# Build:  docker build -t gigaverse3d-api ./backend

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
# dependências primeiro (cache de camadas enquanto o pom não muda)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
# testes rodam no CI (precisam de Docker/Testcontainers); aqui só empacota
RUN ./mvnw -q -B -DskipTests package \
 && java -Djarmode=tools -jar target/backend-*.jar extract --layers --launcher --destination /app

FROM eclipse-temurin:21-jre-alpine AS runtime
# correções de segurança do Alpine publicadas depois da imagem base
RUN apk upgrade --no-cache  && addgroup -S -g 10001 app && adduser -S -u 10001 -G app -h /app app \
 && mkdir -p /data/storage && chown app:app /data/storage
WORKDIR /app
COPY --from=build --chown=app:app /app/dependencies/ ./
COPY --from=build --chown=app:app /app/spring-boot-loader/ ./
COPY --from=build --chown=app:app /app/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /app/application/ ./
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    APP_STORAGE_LOCAL_ROOT=/data/storage \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness | grep -q '"UP"' || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
