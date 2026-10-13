# Build context: repository root (see docker-compose.yml)

# ---- Build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY backend/src src
RUN ./mvnw -q -B -DskipTests package

# ---- Runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Non-root; uid/gid 1000 (already present in the base image) matches typical Linux hosts,
# so the bind-mounted ./backend/data stays writable
COPY --from=build /build/target/*.jar app.jar
# Book flat files and the H2 file live under ./data (mounted as a volume by compose)
RUN mkdir -p data && chown -R 1000:1000 /app
USER 1000:1000
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java \
  -XX:InitialRAMPercentage=50 \
  -XX:MaxRAMPercentage=75 \
  -jar app.jar"]
