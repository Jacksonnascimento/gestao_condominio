# Estágio 1: Build da aplicação com Maven e JDK
FROM maven:3.9-eclipse-temurin-21 AS builder

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o arquivo de definição do projeto e baixa as dependências
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o restante do código-fonte da API
COPY src ./src

# Executa o build do projeto, gerando o .jar
RUN mvn package -DskipTests

# Estágio 2: Imagem final, otimizada e leve
FROM eclipse-temurin:21-jre-jammy

# Define o diretório de trabalho
WORKDIR /app

# Copia apenas o .jar gerado no estágio anterior
COPY --from=builder /app/target/api-0.0.1-SNAPSHOT.jar ./app.jar

# Expõe a porta que a aplicação Spring Boot usa
EXPOSE 8080

# Comando para iniciar a aplicação quando o container rodar
ENTRYPOINT ["java", "-jar", "app.jar"]