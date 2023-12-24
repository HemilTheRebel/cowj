FROM gradle:8.1-jdk17

COPY . .

RUN ["gradle", "build", "-x", "test"]

FROM eclipse-temurin:17-jre
COPY --from=0 /home/gradle/app/build/libs/deps deps/
COPY --from=0 /home/gradle/app/build/libs/cowj-0.1-SNAPSHOT.jar .
RUN ["cp", "cowj-0.1-SNAPSHOT.jar", "deps/"]

ENTRYPOINT ["java", "--add-opens", "java.base/jdk.internal.loader=ALL-UNNAMED", "--add-opens", "java.base/java.util.stream=ALL-UNNAMED", "-cp" ,"deps/*", "cowj.App"]