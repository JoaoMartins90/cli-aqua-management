# syntax=docker/dockerfile:1

# ---------- build: compila com o Gradle Wrapper ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

# o cache guarda a distribuicao do Gradle e as dependencias entre um build e outro
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew installDist --no-daemon --console=plain

# ---------- runtime: so o JRE e o app ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system gerenciador
COPY --from=build /workspace/build/install/gerenciador ./

USER gerenciador
ENV LANG=C.UTF-8
ENTRYPOINT ["/app/bin/gerenciador"]
