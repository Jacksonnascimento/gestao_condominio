Sistema de Gestão de Condomínios
📖 Visão Geral
Este projeto é um sistema web completo para gestão de condomínios, projetado para centralizar e automatizar tarefas administrativas, financeiras e de comunicação. A solução oferece um backend robusto construído com Java e Spring Boot, juntamente com uma interface de usuário dinâmica renderizada no servidor com Thymeleaf.

O objetivo é fornecer uma ferramenta unificada para síndicos, administradores e moradores, simplificando a gestão de unidades, finanças, reservas de áreas comuns, comunicação e muito mais.

✨ Funcionalidades Principais
O sistema oferece um conjunto abrangente de módulos para atender às necessidades de um condomínio moderno:

Painel Administrativo: Interface web para gerenciamento de todas as funcionalidades do sistema.

Gestão de Unidades e Moradores: Cadastro e controle de unidades (apartamentos, casas) e seus respectivos ocupantes (proprietários, inquilinos).

Controle Financeiro:

Lançamento e categorização de despesas.

Geração de cobranças (taxas condominiais) de forma individual ou em lote.

Acompanhamento do status de pagamento das cobranças.

Reservas de Áreas Comuns: Agendamento e gerenciamento de reservas para espaços como salões de festa, churrasqueiras e quadras esportivas.

Assembleias e Votações: Criação de assembleias, definição de pautas (tópicos) e registro de votos para tomada de decisões.

Comunicação Integrada:

Envio de comunicados gerais para todos os moradores ou para unidades específicas.

Sistema de notificação para a chegada de entregas e correspondências.

Solicitações de Manutenção: Abertura e acompanhamento de chamados para reparos e manutenção nas áreas do condomínio.

Gestão de Documentos: Repositório central para armazenamento de documentos importantes, como atas de reunião, regimento interno e balancetes.

Segurança e Autenticação: Controle de acesso seguro baseado em perfis de usuário (roles) com autenticação via JSON Web Tokens (JWT).

🛠️ Tecnologias Utilizadas
Este projeto foi construído utilizando tecnologias modernas e consolidadas no mercado:

Backend:

Java 17

Spring Boot 3

Spring Data JPA (Hibernate)

Spring Security

JSON Web Tokens (JWT)

Maven

Frontend:

Thymeleaf (Renderização no Servidor)

HTML5 & CSS3

Banco de Dados:

PostgreSQL

🚀 Como Executar o Projeto
Siga os passos abaixo para configurar e executar o projeto em seu ambiente local.

Pré-requisitos
JDK 17 ou superior

Maven 3.6+

PostgreSQL 13 ou superior

Uma IDE de sua preferência (ex: IntelliJ IDEA, VS Code)

Passos de Instalação
Clone o repositório:

Bash

git clone https://github.com/jacksonnascimento/gestao_condominio.git
cd gestao_condominio/api
Configure o Banco de Dados:

Certifique-se de que o PostgreSQL está instalado e em execução.

Crie um novo banco de dados (ex: gestao_condominio_db).

Execute o script Banco de dados PostgreSQL.sql (localizado na raiz do projeto) para criar a estrutura de tabelas e inserir dados iniciais.

Configure a Aplicação:

Abra o arquivo src/main/resources/application.properties.

Atualize as propriedades de conexão com o banco de dados com suas credenciais:

Properties

spring.datasource.url=jdbc:postgresql://localhost:5432/gestao_condominio_db
spring.datasource.username=seu_usuario_postgres
spring.datasource.password=sua_senha_postgres
(Opcional) Altere a chave secreta do JWT para maior segurança:

Properties

jwt.secret=sua_chave_secreta_super_segura
Compile e Execute:

Utilize o Maven para iniciar a aplicação Spring Boot:

Bash

mvn spring-boot:run
A aplicação estará disponível em http://localhost:8080.

💻 Acesso ao Sistema
Interface Web: Abra seu navegador e acesse http://localhost:8080.

API Endpoints: Os endpoints da API estão disponíveis no prefixo /api. Para interagir com eles, utilize ferramentas como Postman ou Insomnia, lembrando que a maioria das rotas exige um token de autenticação JWT.
