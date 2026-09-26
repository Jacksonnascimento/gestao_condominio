# Carga de demonstração

Gera e carrega dados fictícios, mas com cara de reais, num cliente do CONDIGTAL: três condomínios em Goiânia e
Aparecida de Goiânia, com unidades, famílias, síndico, administradora, portaria e seis meses de movimento.

| Condomínio | Unidades | O que tem |
|---|---|---|
| Residencial Parque das Palmeiras (Jardim Goiás) | 96 apartamentos em 2 torres | 6 áreas comuns (uma em reforma), 4 porteiros e um ex-porteiro com acesso desativado, gerente predial |
| Edifício Mirante do Bosque (Setor Bueno) | 32 apartamentos e 3 lojas | misto; duas lojas alugadas por empresas; sauna fechada para manutenção |
| Condomínio Villaggio Toscana (Aparecida de Goiânia) | 54 casas em 3 quadras | quiosques, campo society e salão reservado pelo dia inteiro |

E, em cada um: ocupantes (proprietários moradores, inquilinos, cônjuges, dependentes, inquilinos antigos), acessos
ao sistema, contratos de prestadores (alguns vencendo nos próximos dias), comunicados com PDFs anexos, reservas (seis
meses de histórico e as próximas semanas), encomendas e visitantes dos últimos 60 dias e ocorrências com as
respostas da gestão.

A carga entra pela API, como faria quem usa o sistema: o morador pede a reserva e o síndico aprova, o porteiro do
turno registra a encomenda e a retirada, o morador registra a ocorrência e a gestão responde. O banco só é usado
para o que a API grava com a hora do momento (a entrada do visitante, a data do comunicado, o registro da
ocorrência...) e para levar ao passado as reservas do histórico, que a API só aceita dentro da antecedência da área.

## Como rodar

1. A API precisa estar no ar (`cd api` e `.\mvnw.cmd spring-boot:run`).
2. No `.env` da raiz, defina `CARGA_SENHA_DEMONSTRACAO` (pelo menos 6 caracteres): é a senha de todos os acessos da
   demonstração. O administrador geral é o de `ADMINISTRADOR_INICIAL_EMAIL`/`ADMINISTRADOR_INICIAL_SENHA`; noutro
   ambiente, use `CARGA_ADMIN_EMAIL`/`CARGA_ADMIN_SENHA` ou grave um token em `token-<cliente>.txt`.
3. Rode para um dos clientes de `CLIENTES`:

   ```bash
   node carregar.mjs modelo
   ```

   Ou só alguns módulos, na ordem do prefixo: `node carregar.mjs modelo reservas encomendas`. Para cada módulo sai
   `+incluídos =já existiam xrecusados`, e as recusas ficam marcadas com `<<<`.

O banco é acessado pelo psql do contêiner de desenvolvimento (`portal_transparencia_db`). Noutro ambiente,
`CARGA_PSQL` troca o comando (`{banco}` vira o banco do cliente), e `CARGA_API` e `CARGA_HOST` trocam o endereço da
API e do cliente. Os valores estão comentados no `.env.example`.

## Entrar como alguém da demonstração

Todos entram com a senha de `CARGA_SENHA_DEMONSTRACAO`. Os e-mails ficam no domínio `email.test`, reservado, que não
existe: a carga pode rodar com o envio de e-mail ligado sem escrever para ninguém. Os moradores estão na tela de usuários de cada
condomínio. Alguns acessos, que saem sempre iguais:

- síndica do Palmeiras: `tatiane.barbosa@email.test`;
- síndico do Mirante: `marcio.oliveira@email.test`;
- síndica do Villaggio: `carolina.alves@email.test`;
- administradora do Palmeiras e do Mirante: `luciana.prado@email.test`;
- administrador do Villaggio: `rodrigo.campos@email.test`;
- porteiro do Palmeiras (turno da manhã): `jose.nunes@email.test`.

## Rodar de novo é seguro

- **Cadastros** (condomínios, unidades, ocupantes, acessos, áreas, contratos, comunicados): o sorteio é
  reproduzível, e o que já existe (pelo nome, pelo número da unidade, pelo e-mail, pelo título) fica como está. A
  segunda execução completa volta tudo como "já existia".
- **Movimento** (reservas, encomendas, visitantes, ocorrências): só é carregado num condomínio que ainda não tem
  nenhum registro daquele tipo, porque não há chave para saber se um visitante já foi registrado. As datas são
  contadas a partir do dia em que a carga roda: o histórico termina hoje e as reservas futuras começam amanhã.

## Organização

- `comum.mjs`: configuração (lida do `.env`), sorteio, datas, CPF e CNPJ válidos, nomes, acesso à API e ao banco.
- `mundo.mjs`: os condomínios, as unidades, as famílias e a equipe de cada condomínio. Todos os módulos usam o mesmo
  mundo.
- `pdf.mjs`: gerador de PDF simples, para as atas, os regulamentos e os orçamentos anexados.
- `carregar.mjs`: carregador, com o contexto que os módulos usam.
- `modulos/`: um arquivo por módulo, na ordem do prefixo.
