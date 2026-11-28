# O Render não tem Java pronto: este arquivo ensina a compilar e ligar a API.
# No Render: Root Directory = backend/cti_insights  (o build roda em Linux, onde
# maiúsculas e minúsculas são pastas diferentes) e Language/Runtime = Docker.

# Etapa 1: compila com Maven e Java 21 (mesma versão do <java.version> do pom).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copia só o pom primeiro: se as dependências não mudarem, o Render reaproveita
# esta camada e o deploy seguinte fica muito mais rápido.
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# Etapa 2: imagem leve, só o Java para rodar o .jar (sem Maven dentro).
FROM eclipse-temurin:21-jre
WORKDIR /app
# Glob em vez do nome fixo: o artefato é cti_insights-0.0.1-SNAPSHOT.jar
# (artifactId + version do pom). O ".jar.original" não casa com "*.jar".
COPY --from=build /app/target/*.jar app.jar
# Informativo: quem manda de verdade é a variável PORT do Render, lida pelo
# server.port=${PORT:...} do application.properties.
EXPOSE 8080
# MaxRAMPercentage: a instância free do Render tem 512 MB; sem isso a JVM
# costuma estourar a memória e o serviço reinicia sozinho.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
