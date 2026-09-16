# Integração do frontend com o Keycloak e o backend

Este documento descreve como o frontend deve autenticar o usuário no mesmo Keycloak utilizado pelo backend e enviar o token de acesso nas chamadas da API.

## Configuração do ambiente local

| Item | Valor |
| --- | --- |
| Keycloak | `http://localhost:8130` |
| Realm | `femass` |
| Client do frontend | `femass-frontend` |
| API | `http://localhost:8080` |
| Client/audience da API | `backend-service` |

O client `femass-frontend` é público e usa Authorization Code Flow com PKCE (`S256`). O frontend não deve possuir nem solicitar um `client_secret`.

No ambiente local, as URLs permitidas para o frontend são:

- origem: `http://localhost:5173`;
- redirecionamento após login: `http://localhost:5173/*`;
- redirecionamento após logout: `http://localhost:5173/*`.

Se o frontend rodar em outro host ou porta, será necessário alterar tanto o client no Keycloak quanto o CORS do backend.

## Fluxo de autenticação

```text
Frontend → tela de login do Keycloak
         ↓
Keycloak autentica o usuário e devolve o authorization code
         ↓
Frontend troca o code por tokens usando PKCE
         ↓
Frontend chama a API com Authorization: Bearer <access_token>
         ↓
Backend valida assinatura, issuer, audience e roles do token
```

O frontend nunca deve coletar a senha do usuário para enviá-la diretamente ao endpoint de token. O login deve acontecer na página do Keycloak.

## Exemplo com `keycloak-js`

Instalação:

```bash
npm install keycloak-js
```

Configuração sugerida:

```javascript
// auth/keycloak.js
import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

export async function initializeAuthentication() {
  return keycloak.init({
    onLoad: "login-required",
    pkceMethod: "S256",
    checkLoginIframe: false,
  });
}
```

Variáveis locais:

```dotenv
VITE_API_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8130
VITE_KEYCLOAK_REALM=femass
VITE_KEYCLOAK_CLIENT_ID=femass-frontend
```

A inicialização deve terminar antes da renderização das rotas protegidas:

```javascript
const authenticated = await initializeAuthentication();

if (!authenticated) {
  await keycloak.login();
}
```

## Envio do token para a API

Todas as rotas protegidas devem receber o **access token**, e não o ID token:

```http
GET /templates HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJ...
Accept: application/json
```

Exemplo usando `fetch` e renovando o token quando ele estiver próximo de expirar:

```javascript
import { keycloak } from "./auth/keycloak";

export async function apiFetch(path, options = {}) {
  await keycloak.updateToken(30);

  const headers = new Headers(options.headers);
  headers.set("Authorization", `Bearer ${keycloak.token}`);

  if (options.body && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${import.meta.env.VITE_API_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    await keycloak.login();
  }

  return response;
}
```

Uso:

```javascript
const response = await apiFetch("/templates");
const templates = await response.json();
```

Para upload com `FormData`, não defina manualmente o `Content-Type`; o navegador precisa incluir o boundary do multipart.

## Papéis e permissões

As roles ficam no access token em `realm_access.roles`. O backend as lê diretamente desse campo.

No estado atual da API:

| Operação | Roles aceitas |
| --- | --- |
| Listar ou consultar templates | qualquer usuário autenticado |
| Criar e editar templates | `PROFESSOR`, `COORDENADOR`, `DIRETOR` ou `SECRETARIA` |
| Criar, salvar e enviar requerimentos | `ALUNO` ou `PROFESSOR` |
| Enviar ou excluir documentos | `ALUNO` ou `PROFESSOR` |
| Consultar requerimentos | qualquer usuário autenticado |

O frontend pode usar `keycloak.hasRealmRole("ALUNO")` para controlar a interface, mas isso serve apenas para experiência do usuário. A autorização efetiva sempre é feita pelo backend.

## Logout

```javascript
await keycloak.logout({
  redirectUri: window.location.origin,
});
```

Não grave access token ou refresh token em `localStorage` ou `sessionStorage`. O adaptador deve mantê-los em memória.

## Usuários locais de desenvolvimento

O realm importado pelo Docker Compose possui, entre outros, estes usuários:

| Usuário | Senha local | Role |
| --- | --- | --- |
| `aluno.dev` | `femass123` | `ALUNO` |
| `professor.dev` | `femass123` | `PROFESSOR` |
| `analista.dev` | `femass123` | `ADMIN` |

Essas credenciais são exclusivamente para desenvolvimento local e não devem ser reutilizadas em ambientes compartilhados ou de produção.

## Respostas que o frontend deve tratar

- `401 Unauthorized`: token ausente, expirado ou inválido. Tente renovar o token; se isso falhar, inicie novo login.
- `403 Forbidden`: token válido, mas o usuário não possui a role exigida.
- `500 Internal Server Error` com mensagem de identidade não cadastrada: o usuário existe no Keycloak, mas ainda não está vinculado a um usuário do banco da aplicação.

## Pendências conhecidas no backend/realm

Antes de considerar a integração concluída em todos os fluxos, o time do backend precisa resolver estes pontos:

1. O realm possui a role `COODERNADOR`, enquanto a API exige `COORDENADOR`. Usuários com a grafia atual receberão `403` nas rotas de templates.
2. O backend localiza o usuário interno pelo claim `sub` do token e exige uma identidade com `provider = keycloak` e `externalId = <sub>`. A importação do realm não cria esse vínculo automaticamente no banco.
3. Ainda falta um teste ponta a ponta que valide login, audience, roles e respostas `401`/`403` com o Keycloak em execução.

Portanto, o frontend já pode implementar o login e obter/enviar o token conforme este contrato, mas alguns endpoints de requerimento dependerão do cadastro da identidade correspondente no banco.

