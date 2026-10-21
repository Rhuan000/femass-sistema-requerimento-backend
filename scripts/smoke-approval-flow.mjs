// Teste local com as contas do realm. Cria um template e dois requerimentos de teste.
// Não altera usuários, roles ou configuração de autenticação no Keycloak.
import { readFile } from 'node:fs/promises';
import { randomBytes, createHash } from 'node:crypto';
import assert from 'node:assert/strict';

const realm = JSON.parse(await readFile(new URL('../keycloak/realm-femass.json', import.meta.url), 'utf8'));
const api = 'http://localhost:8080';
const issuer = 'http://localhost:8140/realms/femass';

async function login(username) {
  const account = realm.users.find(user => user.username === username);
  assert(account, `Conta ausente no realm: ${username}`);
  const verifier = randomBytes(32).toString('base64url');
  const challenge = createHash('sha256').update(verifier).digest('base64url');
  const redirect = 'http://localhost:5173/';
  const url = new URL(`${issuer}/protocol/openid-connect/auth`);
  url.search = new URLSearchParams({ client_id: 'femass-frontend', redirect_uri: redirect,
    response_type: 'code', scope: 'openid', code_challenge: challenge, code_challenge_method: 'S256' });
  const cookies = new Map();
  const collect = response => {
    for (const cookie of response.headers.getSetCookie()) {
      const [pair] = cookie.split(';'); const index = pair.indexOf('=');
      cookies.set(pair.slice(0, index), pair.slice(index + 1));
    }
  };
  const page = await fetch(url, { redirect: 'manual' }); collect(page);
  const html = await page.text();
  const action = html.match(/<form[^>]*action="([^"]+)"/i)?.[1]?.replaceAll('&amp;', '&');
  assert(action, `Formulário de login indisponível: HTTP ${page.status}`);
  const response = await fetch(action, { method: 'POST', redirect: 'manual',
    headers: { Cookie: [...cookies].map(([key, value]) => `${key}=${value}`).join('; '), 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ username, password: account.credentials.find(c => c.type === 'password').value, credentialId: '' }),
  });
  const location = response.headers.get('location');
  assert(location, `Login falhou para ${username}: HTTP ${response.status}`);
  const code = new URL(location).searchParams.get('code'); assert(code, 'Authorization code ausente');
  const tokenResponse = await fetch(`${issuer}/protocol/openid-connect/token`, { method: 'POST',
    body: new URLSearchParams({ grant_type: 'authorization_code', client_id: 'femass-frontend', redirect_uri: redirect, code, code_verifier: verifier }) });
  assert.equal(tokenResponse.status, 200, 'Falha ao trocar authorization code');
  return (await tokenResponse.json()).access_token;
}

async function request(token, path, method = 'GET', body, expected = 200) {
  const response = await fetch(`${api}${path}`, { method,
    headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined });
  const text = await response.text();
  assert.equal(response.status, expected, `${method} ${path}: HTTP ${response.status} ${text.slice(0, 400)}`);
  return text ? JSON.parse(text) : undefined;
}

const tokens = {};
for (const name of ['secretaria.dev', 'professor.dev', 'Coordernador.dev', 'Diretor.dev', 'aluno.dev']) {
  tokens[name] = await login(name);
  await request(tokens[name], '/me');
}
console.log('Login PKCE e vínculo no banco: cinco contas OK');
const director = tokens['Diretor.dev'];
const student = tokens[process.argv.includes('--professor-requestor') ? 'professor.dev' : 'aluno.dev'];
const secretary = tokens['secretaria.dev']; const professor = tokens['professor.dev'];
const template = await request(secretary, '/templates', 'POST', {
  name: `[TESTE AUTOMATICO] Aprovação ${new Date().toISOString()}`, description: 'Validação local do fluxo de aprovação', category: 'Acadêmico',
  fields: [{ fieldKey: 'motivo', label: 'Motivo', type: 'text', required: true, position: 1 }],
  approvalFlow: ['SECRETARIA', 'PROFESSOR', 'COORDENADOR'].map(role => ({ label: `Análise ${role}`, role, priority: 2 })),
}, 201);
await request(director, `/templates/${template.id}/approve`, 'POST');
await request(director, `/templates/${template.id}/activate`, 'POST');

async function createSubmission() {
  const drafts = await Promise.all(Array.from({ length: 8 }, () =>
    request(student, '/submissions', 'POST', { templateId: template.id, data: { motivo: 'Teste do fluxo' } }, 201)));
  assert.equal(new Set(drafts.map(draft => draft.id)).size, 1, 'Criação concorrente duplicou o rascunho');
  const draft = drafts[0];
  const sent = await request(student, `/submissions/${draft.id}/submit`, 'POST');
  assert.equal(sent.status, 'EM_APROVACAO');
  assert.equal(sent.id, draft.id, 'Envio deve preservar o ID do rascunho');
  const records = await request(student, `/submissions/template/${template.id}`);
  assert(!records.some(record => record.status === 'RASCUNHO'), 'Sobrou uma cópia em rascunho após envio');
  await request(student, `/submissions/${draft.id}`, 'PUT', { data: {} }, 400);
  return draft.id;
}
const flow = (token, id) => request(token, `/approvals/submissions/${id}`);
const decide = (token, id, step, action, extra = {}, expected = 200) => request(token,
  `/approvals/submissions/${id}/steps/${step.id}/decisions`, 'POST', { action, version: step.version, reason: 'Teste automatizado', ...extra }, expected);

const id = await createSubmission();
let state = await flow(student, id);
assert.deepEqual(state.steps.map(item => item.step.status), ['DISPONIVEL', 'PENDENTE', 'PENDENTE']);
await decide(student, id, state.steps[0].step, 'APPROVE', {}, 403);
await decide(secretary, id, state.steps[1].step, 'APPROVE', {}, 400);
await decide(secretary, id, state.steps[0].step, 'APPROVE');
await decide(secretary, id, state.steps[0].step, 'APPROVE', {}, 400);
state = await flow(professor, id); await decide(professor, id, state.steps[1].step, 'APPROVE');
state = await flow(tokens['Coordernador.dev'], id); await decide(tokens['Coordernador.dev'], id, state.steps[2].step, 'APPROVE');
assert.equal((await request(student, `/submissions/${id}`)).status, 'APROVADO');
console.log('Sequência, permissões, decisão repetida e conclusão: OK');

const other = await createSubmission();
state = await flow(director, other);
const people = await request(director, '/approvals/people');
const target = people.find(person => person.roles.includes('PROFESSOR')); assert(target);
await decide(director, other, state.steps[0].step, 'REASSIGN', { targetUserId: target.id });
await decide(secretary, other, state.steps[0].step, 'APPROVE', {}, 409);
state = await flow(professor, other);
assert.equal(state.steps[0].step.userId, target.id);
await decide(professor, other, state.steps[0].step, 'REJECT');
state = await flow(student, other);
assert.deepEqual(state.steps.map(item => item.step.status), ['REJEITADA', 'CANCELADA', 'CANCELADA']);
assert.equal((await request(student, `/submissions/${other}`)).status, 'REJEITADO');
const notes = await request(student, '/approvals/notifications');
assert(notes.some(note => note.submissionId === other));
await request(student, `/approvals/notifications/${notes.find(note => note.submissionId === other).id}/read`, 'POST', undefined, 204);
await request(director, `/templates/${template.id}/deactivate`, 'POST');
console.log('Encaminhamento, versão, rejeição, notificações e histórico: OK');
console.log(JSON.stringify({ templateId: template.id, approvedSubmission: id, rejectedSubmission: other }));
