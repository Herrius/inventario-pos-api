# syntax=docker/dockerfile:1

# ---- Build stage: compila con JDK 21 usando el Maven wrapper del proyecto ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Copiamos primero solo lo necesario para resolver dependencias. Así Docker
# cachea esta capa y no re-descarga Maven/deps en cada cambio de código.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
# Ahora el código fuente y empaquetamos. -DskipTests: los tests corren aparte
# (necesitan Docker para Testcontainers); el build de la imagen no es su lugar.
COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests
# Extraemos las capas del fat-jar para cachearlas por frecuencia de cambio.
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ---- Runtime stage: imagen mínima, solo JRE ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
# Usuario no-root: un contenedor comprometido tiene mucho menos radio de daño.
RUN groupadd --system spring && useradd --system --gid spring spring
# Capas en orden de frecuencia de cambio (estables primero = mejor caché Docker).
COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
