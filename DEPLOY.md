# Deploy do CTI Insights

São **4 peças** e a ordem importa, porque cada uma precisa da URL da anterior:

| # | Peça | Onde | Pasta (Root Directory) |
|---|---|---|---|
| 1 | Banco PostgreSQL | Neon (ou Azure) | — |
| 2 | ETL em Python (FastAPI) | Render · Docker | `analytics-python` |
| 3 | API em Java (Spring Boot) | Render · Docker | `backend/cti_insights` |
| 4 | Front (Vue + Vite) | Vercel | `FrontEnd` |

> Os nomes das pastas têm maiúsculas (`FrontEnd`, `backend/cti_insights`). O build
> roda em Linux, onde `frontend` e `FrontEnd` são pastas **diferentes**: digite igual.

Antes de começar: dê `git push` de tudo. Render e Vercel constroem a partir do
GitHub, não da sua máquina.

---

## 1. Banco — Neon

1. Crie um projeto em [neon.tech](https://neon.tech) e copie a connection string:
   `postgresql://USUARIO:SENHA@HOST/BANCO?sslmode=require&channel_binding=require`
2. Guarde as 5 partes, você vai usar nas etapas 2 e 3: `USUARIO`, `SENHA`, `HOST`, `BANCO`, porta `5432`.

Não precisa criar tabela nenhuma: o backend Java roda o `schema.sql` sozinho ao ligar
(`spring.sql.init`), e é seguro rodar várias vezes (`CREATE TABLE IF NOT EXISTS`).

---

## 2. ETL em Python — Render

**New > Web Service** → conecte o repositório do GitHub.

| Campo | Valor |
|---|---|
| Language / Runtime | **Docker** |
| Root Directory | `analytics-python` |
| Health Check Path | `/saude` |

Environment Variables:

| Nome | Valor |
|---|---|
| `DB_HOST` | o `HOST` do Neon |
| `DB_PORT` | `5432` |
| `DB_NAME` | o `BANCO` do Neon |
| `DB_USER` | o `USUARIO` do Neon |
| `DB_PASSWORD` | a `SENHA` do Neon |
| `DB_SSLMODE` | `require` |
| `ETL_API_KEY` | invente uma senha longa — **anote, a etapa 3 usa a mesma** |

Conferir: abra `https://SEU-PYTHON.onrender.com/saude` → `{"status":"ok"}`.

> `ETL_API_KEY` vazia deixa qualquer pessoa que descobrir essa URL gravar no seu
> banco. Em produção preencha.

---

## 3. API em Java — Render

**New > Web Service** → mesmo repositório.

| Campo | Valor |
|---|---|
| Language / Runtime | **Docker** |
| Root Directory | `backend/cti_insights` |
| Health Check Path | `/api/health` |

Environment Variables:

| Nome | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://HOST/BANCO?sslmode=require` — **sem** usuário, senha e `channel_binding` |
| `DB_USER` | o `USUARIO` do Neon |
| `DB_PASSWORD` | a `SENHA` do Neon |
| `JWT_SECRET` | 32+ caracteres. Gere: `[Convert]::ToBase64String((1..48 \| % { Get-Random -Max 256 }))` |
| `ETL_URL` | `https://SEU-PYTHON.onrender.com/etl` — precisa terminar em **`/etl`** |
| `ETL_KEY` | o **mesmo** valor do `ETL_API_KEY` da etapa 2 |
| `ADMIN_EMAIL` | o e-mail do primeiro admin |
| `ADMIN_PASSWORD` | a senha dele (não deixe a padrão) |
| `CORS_ORIGINS` | por enquanto `http://localhost:5173` — você volta aqui na etapa 5 |

Não precisa de `PORT`: o Render injeta sozinho, e o `application.properties` lê.

Conferir: `https://SEU-BACKEND.onrender.com/api/health`.
Nos logs deve aparecer `Administrador inicial criado: ...` no primeiro deploy
(só cria se `ADMIN_EMAIL` e `ADMIN_PASSWORD` existirem e o e-mail ainda não estiver no banco).

---

## 4. Front — Vercel

**Add New > Project** → mesmo repositório.

| Campo | Valor |
|---|---|
| Framework Preset | Vite |
| Root Directory | `FrontEnd` |
| Build Command | `npm run build` |
| Output Directory | `dist` |

Environment Variables:

| Nome | Valor |
|---|---|
| `VITE_API_URL` | `https://SEU-BACKEND.onrender.com/api` — com **`/api`** no fim, sem barra depois |

O `vercel.json` manda todo endereço para o `index.html`. Sem ele, abrir
`/dashboard` direto (ou dar F5 numa rota) dá **404**, porque quem cuida das rotas
é o `createWebHistory` do Vue, dentro do navegador.

> `VITE_*` é lido **na hora do build**, não quando a página abre. Se trocar essa
> variável depois, precisa de **Redeploy**.

---

## 5. Fechar o CORS (não esqueça esta)

Volte no serviço Java do Render e troque:

| Nome | Valor |
|---|---|
| `CORS_ORIGINS` | `https://SEU-FRONT.vercel.app` (sem barra no final; várias URLs = separe por vírgula) |

Salvar reinicia o serviço. Sem isso o login falha no navegador com erro de CORS,
mesmo com a API funcionando.

---

## Conferir se está tudo de pé

1. `https://SEU-PYTHON.onrender.com/saude` → `{"status":"ok"}`
2. `https://SEU-BACKEND.onrender.com/api/health` → responde
3. Abra o front, entre com `ADMIN_EMAIL` / `ADMIN_PASSWORD`
4. **Upload** → `sample-data/dados.xlsx` → o dashboard enche
5. No Neon: `SELECT status, total_linhas, linhas_validas FROM upload_lote;` → `PROCESSADO, 38, 32`

Quem se cadastra pelo site nasce **PENDENTE**: o admin libera em **Usuários**.

---

## Quando der errado

| Sintoma | Causa |
|---|---|
| `no open ports detected` no Render | Root Directory errado, ou a app não leu `PORT` |
| Deploy do Java cai sozinho / `Out of memory` | instância free tem 512 MB; o `-XX:MaxRAMPercentage=75` do Dockerfile já trata |
| `Error: no main manifest attribute` | o build pulou o `spring-boot-maven-plugin`; use o Dockerfile desta pasta |
| 404 ao dar F5 em `/dashboard` | falta o `FrontEnd/vercel.json` |
| Erro de CORS no console do navegador | `CORS_ORIGINS` não é exatamente a URL da Vercel (barra no final conta) |
| Login dá `401` com a senha certa | o usuário está `PENDENTE`, não `APROVADO` |
| `Não foi possível falar com o serviço Python` | `ETL_URL` sem `/etl` no fim, ou o serviço Python está dormindo |
| `Chave do ETL inválida` (401) | `ETL_KEY` (Java) ≠ `ETL_API_KEY` (Python) |
| Primeiro acesso demora ~1 minuto | free tier do Render dorme quando fica parado; o Neon também |
| `password authentication failed` | senha do Neon trocada, ou `DB_URL` ainda com `USUARIO:SENHA@` dentro |

Nunca comite `.env` — só os `.env.example`. A connection string do Neon tem a senha dentro.
