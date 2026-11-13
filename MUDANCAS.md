# O que foi corrigido e adicionado

## ⚠️ Faça primeiro: troque a senha do banco
O `application.properties` do projeto enviado tinha **usuário e senha reais do Azure** escritos no arquivo. Troque essa senha
no Azure (ela foi exposta) e use só o `.env` (que está no `.gitignore`). O `application.properties` novo não tem segredo.

## Bugs que impediam o projeto de rodar
| Onde | Problema | Correção |
|---|---|---|
| backend | `jwtService.java` / `jwtAuthenticationFilter.java` com letra minúscula: o nome do arquivo precisa ser igual ao da classe (erro de compilação) | renomeados |
| backend `pom.xml` | faltavam `spring-boot-starter-security` e `starter-validation` (só havia o starter de **teste**, com `<scope>` vazio) | adicionados; duplicata removida |
| backend `application.properties` | faltava `app.jwt.*` (o servidor não subia); `ddl-auto=update` misturava suas tabelas com as do Python | configuração completa, `ddl-auto=none` + `schema.sql` |
| backend | `Cliente`↔`Consultor` se referenciavam (loop infinito no JSON); `Consultor` guardava **senha em texto puro** e `/consultores/login` devolvia a senha; `Contrato` não tinha cliente nem data | removidos do build (ver "Legado") |
| front `cadastro.vue`, `usuarios.vue` | usavam `api` e `onMounted` **sem importar** | imports corrigidos |
| front `login.vue` | não chamava o backend, só ia para `/dashboard` | login real, com mensagem de erro |
| front `api.js` | apontava para `localhost:3000`; backend roda em `8080/api` | corrigido (`VITE_API_URL`) |
| front `auth.js` | lia `data.perfil`/`data.nome`, mas o backend devolve `data.usuario.perfil` | corrigido |
| front `usuarios.vue` | usava status `ATIVO`; o backend usa `APROVADO` | corrigido |
| front router | só `/usuario` era protegido | `/upload`, `/relatorios`, `/dashboard` exigem login |

## O que faltava (agora existe)
- **Backend:** aprovação de usuários (PENDENTE/APROVADO/BLOQUEADO), `AdminInicial`, `GET/PATCH /api/admin/usuarios`,
  `POST /api/uploads/validadas` (valida de novo + envia ao Python), `GET /api/dashboard` e `/dashboard/lotes`,
  CORS, erros sempre em `{"erro": "..."}`, `/api/health`. Rota de cadastro agora é `/api/auth/register` (a que o front já chamava).
- **Front:** botão **Enviar dados validados** no Upload; **Dashboard com dados reais** (antes eram números fixos no código).
  A "Matriz segmento × nível" virou "Resumo por segmento", porque o Python não calcula esse cruzamento.
- **Pastas novas** (copiadas do professor): `analytics-python/`, `database/`, `sample-data/`, `docs/`, `testes/`.

## Legado (não foi apagado)
`backend/cti_insights/_legado/` guarda suas entidades e CRUDs antigos (Cliente, Consultor, Servico, Contrato, Insight, Telemetria).
Saíram da compilação porque as tabelas `cliente`, `consultor`, `servico` colidem com as que o Python grava (colunas diferentes).
Se a matéria exigir esse modelo, me diga e eu adapto.

## 🔧 Atualização: backend alinhado ao professor
- `pom.xml`: Spring Boot **4.1.1 → 3.5.6** (a mesma versão do projeto do professor, que já é conhecida e estável), `starter-webmvc` → `starter-web`,
  e os starters de teste que só existem no Boot 4 foram trocados por `starter-test` + `spring-security-test`.
  Todos os imports do código já são compatíveis com o 3.5.
- `CtiInsightsApplicationTests`: só roda se `DB_USER` estiver definido, então `mvn package` passa sem banco.

## ✅ O que foi testado
- Front: `vite build` passa; a store rodou com `sample-data/dados.xlsx` e gerou o JSON de envio.
- Validador Java (as mesmas regras do contrato) compilado e executado: **38 linhas → 32 válidas, 6 rejeitadas** (igual ao esperado).
- O JSON gerado pelo front foi processado pelo Python do professor sem erro, com as chaves que o dashboard usa.

## ❗ O que NÃO foi testado
- **O backend inteiro não foi compilado nem executado** aqui (sem acesso ao Maven Central). Só conferi a sintaxe.
  Rode `mvn spring-boot:run`; se aparecer erro de compilação, cole aqui. Ponto de maior risco agora: `PythonEtlService` (usa `RestClient`).
- Nada foi testado com banco, Python ligado e front juntos.
- O teste de contexto só roda com `DB_USER` definido (veja acima).

## Decisão sua: linhas duplicadas
O front marca **as duas** linhas com o mesmo código como erro (36 e 37 da planilha de exemplo), então ele envia 31 clientes.
O backend/contrato do professor aceita a primeira e recusa só a repetida (32 clientes, média 2.261.250 como no README do professor).
Se quiser igualar, é um ajuste pequeno no `uploadStore.js`.
