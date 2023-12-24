FROM gradle:8.1-jdk17

COPY . .

RUN ["./gradlew", "build", "--refresh-dependencies"]
RUN ["cp", "deps/cowj-0.1-SNAPSHOT.jar", "."]

FROM eclipse-temurin:17-jre
COPY --from=0 /home/gradle/deps deps/
RUN ["cp", "deps/cowj-0.1-SNAPSHOT.jar", "."]

ENTRYPOINT ["java", "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED", "--add-opens", "java.base/java.util.stream=ALL-UNNAMED", "-cp" ,"deps/*:cp_hook", "cowj.App"]