# 🏢 Projeto de Gestão de Condomínios

Bem-vindo ao repositório da API do Sistema de Gestão de Condomínios.

## 📖 Descrição

O Gestão de Condomínio é uma plataforma de software robusta e centralizada, projetada para facilitar a administração e a vida em comunidade em condomínios residenciais, comerciais ou mistos. O objetivo é otimizar a comunicação, automatizar processos financeiros e gerenciar os recursos do condomínio de forma eficiente, transparente e acessível para todos os envolvidos.

## ✨ Funcionalidades Principais

* **Gestão de Pessoas e Acessos:** Cadastro de pessoas físicas/jurídicas e um sistema de segurança robusto baseado em papéis (RBAC) com autenticação via JWT.
* **Estrutura de Condomínios:** Gerenciamento de múltiplas empresas administradoras, condomínios e suas respectivas unidades.
* **Controle Financeiro:** Lançamento de cobranças individuais ou em lote (taxa condominial, multas, etc.) e controle de status de pagamento.
* **Comunicação Interna:** Envio de comunicados gerais ou direcionados para os moradores.
* **Reservas e Manutenção:** Gestão de áreas comuns, sistema de reservas e abertura de solicitações de manutenção.
* **Assembleias:** Módulo completo para criação de assembleias, pautas, registro de participantes e votos.

## 🚀 Pilha Tecnológica (Stack)

### **Backend**
* **Java 21**
* **Spring Boot 3.3.1:** Framework principal para a construção da API.
* **Spring Security 6:** Para autenticação (JWT) e autorização (`@PreAuthorize`).
* **Spring Data JPA / Hibernate:** Para persistência de dados e comunicação com o banco.
* **JJWT (Java JWT):** Biblioteca para criação e validação dos tokens JWT.
* **Lombok:** Para reduzir código boilerplate nas entidades e DTOs.

### **Banco de Dados**
* **PostgreSQL**

### **Ambiente de Produção**
* **Render:** Plataforma de nuvem para hospedagem.
* **Docker:** A aplicação é containerizada para garantir consistência entre os ambientes.

## ⚙️ Pré-requisitos

Para rodar este projeto localmente, você precisará ter instalado:
* [**JDK 21**](https://www.oracle.com/java/technologies/downloads/#jdk21-windows) ou superior.
* [**Apache Maven 3.9**](https://maven.apache.org/download.cgi) ou superior.
* [**Docker**](https://www.docker.com/products/docker-desktop/) e **Docker Compose**.
* Uma ferramenta de API, como [**Postman**](https://www.postman.com/downloads/) ou **Insomnia**.
* (Opcional, mas recomendado) Um cliente de banco de dados como o **DBeaver**.

