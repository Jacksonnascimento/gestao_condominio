# CONDIGTAL
Sistema de Gestão de Condomínios.

## 🚧 Status do Projeto

**Em Construção**

O CONDIGTAL é um sistema completo para gestão de condomínios, desenvolvido em Java com Spring Boot e utilizando Thymeleaf para renderização no servidor (Server-Side Rendering).

---

## 🛠️ Tecnologias Utilizadas

O projeto é construído com as seguintes tecnologias principais:

* **Back-end:**
    * Java 21
    * Spring Boot
    * Spring Data JPA (Hibernate)
    * Spring Security
    * PostgreSQL
* **Front-end:**
    * Next.js 16, React 19 e Tailwind CSS 4, na pasta `web/` (sistema novo, em migração)
    * Thymeleaf e Bootstrap 5 (telas antigas, removidas ao fim da migração)

---

## ▶️ Como rodar no computador

1. Copie `.env.example` para `.env` na raiz e preencha (banco, clientes, administrador inicial, segredo dos tokens).
2. API (porta 8080): `cd api` e `.\mvnw.cmd spring-boot:run`. Os bancos dos clientes são criados na primeira subida.
3. Sistema web (porta 3000): `cd web`, `npm install` e `npm run dev`.
4. Abra pelo endereço do cliente, por exemplo `http://modelo.localhost:3000`. O subdomínio escolhe o cliente (e o banco);
   `http://localhost:3000` também leva ao `modelo`, que tem `localhost` como domínio no `CLIENTES` do `.env`.

Antes de subir mudanças no `web/`, rode `npm run conferir` (tipos e lint).

---

## 📄 Licença e Copyright

Este software é um produto comercial.

**Copyright (c) 2025 Horizon AJ Soluções Digitais Ltda. Todos os direitos reservados.**
