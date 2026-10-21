# Fluxo de aprovação de requerimentos

Implementação local em 21/10/2026. O fluxo de aprovação do requerimento é independente da aprovação/publicação do template, que continua exigindo o diretor.

## Comportamento

- Cada template configura até 30 etapas sequenciais. A posição define a ordem; prioridade 1 (alta), 2 (normal) ou 3 (baixa) apenas sinaliza urgência.
- Cada etapa tem um papel OU uma pessoa ativa. Para papel, basta uma pessoa elegível decidir a etapa. Não há quórum ou etapas paralelas nesta versão.
- Ao enviar o rascunho, o backend valida o template ativo e os destinatários e copia as regras para `approval_step`. Alterações posteriores no template não alteram os envios anteriores.
- Apenas a primeira etapa fica DISPONIVEL; as seguintes ficam PENDENTE. Aprovar libera a próxima. A última aprovação deixa o requerimento APROVADO.
- Rejeitar exige motivo, marca o requerimento REJEITADO e cancela as etapas restantes.
- O responsável atual ou o diretor pode encaminhar a etapa disponível para outra pessoa ativa. O encaminhamento substitui o responsável; não insere uma etapa e não pula as seguintes. Exige motivo, registra histórico e notifica o novo responsável.
- Decisões são serializadas por bloqueio transacional da submissão. O cliente precisa enviar a versão da etapa; uma versão desatualizada recebe 409.
- O solicitante acompanha o status e histórico. A leitura de requerimentos e anexos é limitada ao solicitante, participantes do fluxo, atores do histórico, diretor e administrador. A regra vem dos papéis locais no banco.
- Notificações são persistidas e exibidas na consulta de envios, com atualização a cada 30 segundos enquanto a tela estiver aberta. Sem e-mail, SMS ou push.
- Templates antigos sem etapas continuam gerando ENVIADO. Não recebem aprovação automática nem fluxo retroativo.

## Modelo

- `template_approval_rule`: regras ordenadas do template, com FK para o template.
- `approval_step`: execução congelada, estado, responsável, prioridade, versão e decisão.
- `approval_event`: histórico de envio, decisões e encaminhamentos.
- `approval_notification`: avisos por usuário e data de leitura.

O schema local usa Hibernate `update` e `sql-load-script=no-file`. Não é necessário apagar ou recriar volumes. Antes de produção, versionar as alterações com migrações e adicionar paginação às listagens.

## Endpoints

Todos exigem autenticação. O serviço aplica a autorização contextual usando o usuário e papéis do banco.

- `GET /approvals/people`: pessoas ativas para seleção.
- `GET /approvals/submissions/{id}`: etapas, permissões por etapa e histórico.
- `POST /approvals/submissions/{id}/steps/{stepId}/decisions`: `{ "action": "APPROVE|REJECT|REASSIGN", "reason": "...", "version": 0, "targetUserId": "..." }`. O destino só é usado em REASSIGN.
- `GET /approvals/notifications`: últimas 100 notificações do usuário.
- `POST /approvals/notifications/{id}/read`: marca uma notificação própria como lida.
- `approvalFlow` é enviado nos endpoints existentes de criação/edição de template: `[{ "label": "Conferência", "role": "SECRETARIA", "userId": null, "priority": 2 }]`.

## Teste manual

1. Entrar como `secretaria.dev` e criar um template com um campo obrigatório e etapas SECRETARIA → PROFESSOR → COORDENADOR.
2. Entrar como `Diretor.dev`, aprovar o template e depois ativá-lo.
3. Entrar como `aluno.dev`, preencher e enviar um novo requerimento.
4. Na consulta de envios, conferir EM_APROVACAO e as etapas no detalhe. O aluno não recebe botões de decisão.
5. Entrar como `secretaria.dev`, abrir a notificação e aprovar a primeira etapa.
6. Entrar como `professor.dev` e aprovar a segunda etapa.
7. Entrar como `Coordernador.dev` (grafia atual do realm) e aprovar a terceira. O aluno passa a ver APROVADO.
8. Em outro envio, usar o diretor ou responsável para encaminhar a etapa disponível a uma pessoa, informando o motivo. Só essa pessoa passa a decidir a etapa.
9. Rejeitar com motivo e conferir REJEITADO, etapas seguintes CANCELADA e histórico.

Senha das contas estritamente locais: a definida no realm de desenvolvimento. O login continua usando Keycloak; a remoção do provedor não faz parte deste incremento.

## Verificação automatizada

No backend: `mvn clean verify` (30 testes). No frontend: `npm run build`.

Com o Compose ativo e as contas semeadas: `node scripts/smoke-approval-flow.mjs`.

Para criar os registros de teste na conta do professor, sem gerar novos avisos na conta do aluno: `node scripts/smoke-approval-flow.mjs --professor-requestor`.

O smoke autentica cinco contas por Authorization Code + PKCE, cria um template e dois requerimentos, verifica a sequência, recusas 400/403/409, encaminhamento, conclusão, rejeição e notificações. Desativa o template de teste ao terminar e mantém os requerimentos para inspeção. Não muda configurações de autenticação.

Cada criação do smoke dispara oito requisições concorrentes e verifica que todas retornam o mesmo rascunho. O envio mantém o ID e não deixa cópia em RASCUNHO. A criação é serializada pelo usuário no banco; salvamento, envio e alterações de anexo são serializados pelo requerimento. A interface compartilha o carregamento em andamento no StrictMode e aguarda os salvamentos automáticos antes de enviar.

## Limites desta versão

Curso e disciplina ainda não têm cadastro/vínculo para resolver responsáveis. Seleções por curso/disciplina, etapas paralelas, quórum, retorno para correção, novas ramificações, prazos e notificações externas precisam de incrementos próprios. Não foi feita publicação externa.
