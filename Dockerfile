FROM openjdk:17-slim as builder

WORKDIR /app

RUN apt-get update && apt-get install -y \
    maven \
    tesseract-ocr \
    tesseract-ocr-ind \
    && apt-get clean

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests

FROM openjdk:17-slim

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-ind \
    && apt-get clean

CMD ["java", "-jar", "app.jar"]