FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle

RUN chmod +x gradlew

COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test \
 && mv build/libs/lutiano-api-*.jar build/app.jar \
 && java -Djarmode=tools -jar build/app.jar extract --layers --destination /layers

FROM eclipse-temurin:21-jre AS run
RUN useradd --system --uid 10001 --no-create-home lutiano

WORKDIR /app

COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./

USER 10001
EXPOSE 8080 8081

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8081/actuator/health/liveness | grep -q UP || exit 1"]

ENTRYPOINT ["java", "-jar", "app.jar"]