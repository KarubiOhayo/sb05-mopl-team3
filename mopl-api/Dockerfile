FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드 컨텍스트(Root) 기준 경로 사용
# ./gradlew :mopl-api:bootJar 실행 후 생성된 JAR 파일 복사
COPY mopl-api/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
