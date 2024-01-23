#/bin/bash

gradle clean build -x test -x javadoc

cd app/build/libs

java --add-opens java.base/jdk.internal.loader=ALL-UNNAMED --add-opens java.base/java.util.stream=ALL-UNNAMED -jar cowj-0.1-SNAPSHOT.jar $@
