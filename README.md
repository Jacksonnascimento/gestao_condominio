# 🏢 Projeto de Gestão de Condomínios

Bem-vindo ao repositório da API do Sistema de Gestão de Condomínios.

## 📖 Descrição

O Gestão de Condomínio é um sistema de backend completo, desenvolvido para facilitar a administração de condomínios residenciais, comerciais ou mistos. A API oferece um conjunto robusto de funcionalidades para gerenciar cadastros, finanças, comunicação e recursos, com o objetivo de tornar a gestão mais eficiente, organizada e transparente para síndicos, administradoras e moradores.

## ✨ Funcionalidades Principais

* **Gestão de Pessoas e Acessos:** Cadastro de pessoas físicas/jurídicas e um sistema de segurança robusto baseado em papéis (RBAC) com autenticação via JWT.
* **Estrutura de Condomínios:** Gerenciamento de múltiplas empresas administradoras, condomínios e suas respectivas unidades.
* **Controle Financeiro:** Lançamento de cobranças individuais ou em lote (taxa condominial, multas, etc.) e controle de status de pagamento.
* **Comunicação Interna:** Envio de comunicados gerais ou direcionados para os moradores.
* **Reservas e Manutenção:** Gestão de áreas comuns, sistema de reservas e abertura de solicitações de manutenção.
* **Assembleias:** Módulo completo para criação de assembleias, pautas, registro de participantes e votos.

## 🚀 Pilha Tecnológica (Stack)

### **Backend**
* **Java 24**
* **Spring Boot 3.5:** Framework principal para a construção da API.
* **Spring Security 6:** Para autenticação (JWT) e autorização (`@PreAuthorize`).
* **Spring Data JPA / Hibernate:** Para persistência de dados e comunicação com o banco.
* **JJWT (Java JWT):** Biblioteca para criação e validação dos tokens JWT.
* **Lombok:** Para reduzir código boilerplate nas entidades e DTOs.

### **Banco de Dados**
* **Microsoft SQL Server**

## ⚙️ Pré-requisitos

Para rodar este projeto localmente, você precisará ter instalado:
* [**JDK 24**](https://www.oracle.com/java/technologies/downloads/#jdk24-windows) ou superior.
* [**Apache Maven 3.9**](https://maven.apache.org/download.cgi) ou superior.
* [**Microsoft SQL Server**](https://www.microsoft.com/pt-br/sql-server/sql-server-downloads) (qualquer edição, incluindo a Express ou Developer).
* Uma ferramenta de API, como [**Postman**](https://www.postman.com/downloads/) ou [**Insomnia**](https://insomnia.rest/download).

## ▶️ Como Executar o Projeto

1.  **Clone o Repositório**
    ```bash
    git clone [URL_DO_SEU_REPOSITORIO]
    cd [NOME_DA_PASTA_DO_PROJETO]
    ```

2.  **Configure o Banco de Dados**
    * Certifique-se de que seu SQL Server está rodando.
    * Crie um novo banco de dados chamado `GESTAO_CONDOMINIO`.
    * Execute o script `Banco de dados.sql` (que nós já temos) neste banco de dados para criar todas as tabelas.

3.  **Configure a Aplicação**
    * Abra o arquivo `src/main/resources/application.properties`.
    * Ajuste as propriedades de conexão com o banco de dados e a chave secreta do JWT conforme o seu ambiente. O arquivo deve se parecer com isto:

    ```properties
    # Configuração da Conexão com o SQL Server
    spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=GESTAO_CONDOMINIO;encrypt=false;trustServerCertificate=true
    spring.datasource.username=seu_usuario_sql
    spring.datasource.password=sua_senha_sql

    # Configuração do Hibernate
    spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
    spring.jpa.hibernate.ddl-auto=validate
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true
    spring.jpa.open-in-view=false

    # Configurações de Segurança da API - JWT
    # IMPORTANTE: Em produção, esta chave deve vir de uma variável de ambiente.
    api.security.jwt.secret-key=NzE3NjRBNUIyRDYxNTM3NzVENzI0NDJEN0E2MzU3NkM1QjMzNkI1ODU5MzI1MTZFNEM1MzZERDgxNEE3REQ1NA==
    api.security.jwt.expiration-time-ms=3600000

    # Configuração de Log
    logging.level.org.springframework.security.config.annotation.authentication.configuration=ERROR
    ```

4.  **Execute a Aplicação**
    * Abra um terminal na raiz do projeto.
    * Execute o comando Maven para iniciar a aplicação:
    ```bash
    mvn spring-boot:run
    ```
    * A API estará rodando em `http://localhost:8080`.

## 🔌 Estrutura da API

A API é RESTful e protegida por JWT. Para interagir com ela:

1.  **Faça login:** Envie uma requisição `POST` para `/api/auth/login` com `{ "email": "...", "senha": "..." }` para obter um token.
2.  **Envie o Token:** Em todas as outras requisições, inclua o cabeçalho `Authorization: Bearer <seu_token>`.

Para uma documentação completa de todos os endpoints e exemplos de JSON, consulte o **Manual do Desenvolvedor Frontend**.
