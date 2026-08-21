# Regras de Negócio — Sistema de Requerimentos FeMASS

Este documento descreve o comportamento esperado do sistema de requerimentos. Ele deve orientar a evolução incremental do código atual, evitando que regras institucionais fiquem espalhadas apenas em serviços, entidades ou validações.

## Objetivo

O sistema deve permitir que a FeMASS gerencie modelos de requerimento, receba preenchimentos desses modelos e conduza cada requerimento por um fluxo de análise definido previamente.

O código atual já implementa a base de formulários dinâmicos:

- criação e atualização de templates;
- listagem e consulta de templates;
- submissão de formulários preenchidos;
- validação dos dados enviados contra os campos do template;
- salvamento e recuperação das submissões;
- snapshot das respostas preenchidas.

As próximas evoluções devem transformar essa base em um fluxo institucional de requerimentos.

## Papéis

Papéis previstos:

- `ALUNO`;
- `PROFESSOR`;
- `SECRETARIA`;
- `COORDENADOR`;
- `DIRETOR`.

O sistema não deve cadastrar usuários. Os usuários, suas matrículas, vínculos, cursos e papéis virão futuramente de autenticação externa ou API institucional.

O backend deve guardar apenas referências externas necessárias, como identificador do solicitante, responsável, matrícula ou curso, quando forem necessárias para autorização e rastreabilidade.

## Templates de Requerimento

Um template representa:

- os campos que serão preenchidos pelo solicitante;
- o fluxo de aprovação do próprio template;
- o fluxo de tramitação das submissões criadas a partir dele;
- as regras de quem pode atuar em cada etapa.

### Criação de Template

Todos os papéis, exceto `ALUNO`, podem propor a criação de um template:

- `PROFESSOR`;
- `SECRETARIA`;
- `COORDENADOR`;
- `DIRETOR`.

Criar um template não significa publicá-lo para preenchimento.

Um template recém-criado deve ficar pendente de aprovação, salvo quando a regra de aprovação permitir ativação imediata.

### Aprovação de Template

Apenas `COORDENADOR` e `DIRETOR` podem aprovar um template para uso geral.

Existem templates que exigem aprovação dos dois.

Existem templates que exigem apenas aprovação do `COORDENADOR`.

Existem templates que exigem apenas aprovação do `DIRETOR`.

Por isso, o template precisa armazenar quais aprovações são necessárias para que ele se torne válido e disponível para preenchimento.

Se não houver especificao de quem deve aprovar o padrão será apenas o coordenador que poderá indicar necessidade de aprovação do diretor.

### Criação por Coordenador

Quando um `COORDENADOR` cria um template, a aprovação geral deve exigir apenas o `DIRETOR`, salvo regra explícita em contrário.

### Criação por Diretor

Quando um `DIRETOR` cria um template, a criação pode ser considerada absoluta e suficiente para publicação.

Exceção: o próprio `DIRETOR` pode exigir, no momento da criação, aprovação adicional de `COORDENADOR`, `PROFESSOR` ou `SECRETARIA`.

### Ordem de Aprovação

A aprovação deve seguir escalada institucional do nível mais baixo para o mais alto.

Escala inicial:

1. `SECRETARIA`;
2. `PROFESSOR`;
3. `COORDENADOR`;
4. `DIRETOR`.

O `DIRETOR` é o nível mais alto.

Mesmo com uma ordem padrão, uma etapa pode encaminhar explicitamente a análise para outra pessoa ou outro papel.

Exemplo:

- um `PROFESSOR` pode solicitar interferência do `COORDENADOR`;
- um `COORDENADOR` pode encaminhar para um `PROFESSOR`;
- uma análise pode ser direcionada a uma matrícula específica.

### Reprovação de Template

Quando um `COORDENADOR` reprovar um template, ele deve informar o motivo.

Quando um `DIRETOR` reprovar um template, ele deve informar o motivo.

A reprovação deve alterar o status do template.

As partes afetadas pela reprovação devem ser notificadas futuramente por e-mail ou celular.

Notificação não precisa ser a primeira implementação, mas o sistema deve preservar informação suficiente para que ela seja possível depois.

### Reenvio de Template Reprovado

Um template reprovado pode ser editado e reenviado para avaliação.

O reenvio deve iniciar novamente o fluxo de aprovação necessário.

### Edição de Template Aprovado

Um template aprovado pode ser editado.

A edição de um template aprovado não deve alterar diretamente a versão em uso.

A edição deve gerar uma nova versão superior do template.

Quando a nova versão for aprovada:

- a nova versão passa a ser a versão ativa para novos requerimentos;
- a versão anterior deve ser desativada para novos preenchimentos;
- submissões já abertas com a versão anterior devem continuar tramitando normalmente.

### Significado de `isActive`

Atualmente, `isActive` indica para o frontend se um template pode receber novos requerimentos.

Esse significado deve ser preservado:

- `isActive = true`: pode ser usado para novas submissões;
- `isActive = false`: não deve aparecer para preenchimento de novos requerimentos.

`isActive` não deve substituir o status de aprovação.

O sistema deve separar:

- existência do template;
- aprovação do template;
- disponibilidade para preenchimento.

### Consulta de Templates

Funcionários podem consultar templates, inclusive templates não ativos, desde que usem filtros explícitos para evitar confusão.

Alunos devem visualizar apenas templates aprovados e ativos para preenchimento, salvo regra futura em contrário.

## Submissões de Requerimento

Uma submissão representa um requerimento preenchido a partir de um template.

Inicialmente, apenas `ALUNO` e `PROFESSOR` podem criar submissões.

### Criação de Submissão

Uma submissão só pode ser criada a partir de um template aprovado e ativo.

Ao criar uma submissão, o sistema deve:

- validar que o template existe;
- validar que o template está aprovado;
- validar que o template está ativo;
- validar os dados enviados contra os campos do template;
- criar snapshot das respostas;
- copiar o fluxo de tramitação definido no template;
- definir a etapa inicial do fluxo;
- definir o status inicial.

### Identidade do Solicitante

O solicitante não deve ser confiado cegamente a partir do corpo da requisição.

Quando houver autenticação integrada, o identificador do solicitante deve vir da identidade autenticada.

O corpo da requisição pode conter dados complementares, mas a autoria real da submissão deve ser derivada do token ou da API institucional.

### Estados da Submissão

Estados iniciais previstos:

- `PENDENTE_ABERTURA`;
- `ABERTO_PARA_AVALIACAO`;
- `EM_ANDAMENTO`;
- `CONCLUIDO`;
- `CANCELADO`;
- `REPROVADO`.

`PENDENTE_ABERTURA` significa que o requerimento foi recebido, mas o responsável ainda não abriu para leitura ou tratamento.

### Edição de Submissão

O solicitante pode editar a submissão dentro de 1 hora após o envio.

A edição deve ser bloqueada antes de completar 1 hora se o responsável já tiver aberto o requerimento para leitura ou iniciado tratamento.

Depois que o responsável abrir ou iniciar o tratamento, a submissão não deve ser editável livremente pelo solicitante, apenas se solicitado pelo responsável.

### Cancelamento de Submissão

O solicitante pode cancelar a submissão enquanto o responsável ainda não abriu o requerimento.

Depois que o responsável abrir o requerimento, o cancelamento ou exclusão precisa de aprovação do responsável.

### Fluxo de Tramitação da Submissão

A ordem do fluxo de tramitação não é fixa.

A ordem deve ser descrita no momento de criação do template.

Ao criar uma submissão, o fluxo do template deve ser copiado para a submissão.

Essa cópia é necessária para que alterações futuras no template não mudem o fluxo de requerimentos já abertos.

### Visibilidade de Submissões

O `ALUNO` deve ver apenas as próprias submissões.

O `PROFESSOR` deve ver:

- as próprias submissões;
- submissões que envolvam matéria lecionada por ele;
- submissões direcionadas à sua matrícula;
- submissões em que esteja definido como responsável no fluxo.

O `COORDENADOR` deve ver submissões vinculadas ao seu curso.

O `DIRETOR` deve ver submissões conforme a regra institucional de direção, inicialmente podendo atuar como nível mais alto do fluxo.

A `SECRETARIA` deve ver submissões quando estiver definida no fluxo ou quando houver regra administrativa explícita.

## Proteção de Rotas

Rotas de leitura ampla podem exigir apenas autenticação, desde que a filtragem de visibilidade seja aplicada no serviço.

Rotas que executam ações administrativas devem exigir papéis específicos.

### Templates

Regras iniciais:

- listar templates disponíveis: usuário autenticado;
- consultar template por ID: usuário autenticado, com filtro conforme papel;
- propor template: `PROFESSOR`, `SECRETARIA`, `COORDENADOR`, `DIRETOR`;
- editar template: `PROFESSOR`, `SECRETARIA`, `COORDENADOR`, `DIRETOR`, com restrição por status;
- aprovar template como coordenador: `COORDENADOR`;
- aprovar template como diretor: `DIRETOR`;
- reprovar template: aprovador responsável pela etapa atual.

### Submissões

Regras iniciais:

- criar submissão: `ALUNO`, `PROFESSOR`;
- consultar submissão por ID: usuário autenticado, com filtro no serviço;
- listar submissões por template: usuário autenticado, com filtro no serviço;
- abrir para avaliação: responsável da etapa atual;
- movimentar etapa: responsável da etapa atual;
- concluir: responsável autorizado pela etapa final;
- cancelar antes de abertura: solicitante;
- aprovar cancelamento após abertura: responsável atual.

## Incrementos Sugeridos no Código Atual

Esta seção descreve incrementos conceituais. Ela não define implementação obrigatória.

### Incremento 1 — Status de Aprovação de Template

Problema atual:

- `RequerimentoTemplateService.create` cria template com `isActive = true`.

Comportamento desejado:

- template proposto deve nascer pendente de aprovação;
- `isActive` deve permanecer falso até aprovação final, salvo regra de criação absoluta pelo diretor.

### Incremento 2 — Aprovadores Necessários do Template

Problema atual:

- o template não armazena quem precisa aprovar sua criação ou edição.

Comportamento desejado:

- template deve armazenar etapas de aprovação necessárias;
- cada etapa deve indicar papel ou pessoa responsável;
- cada etapa deve guardar status, data, decisão e motivo quando houver reprovação.

### Incremento 3 — Versionamento de Template

Problema atual:

- `RequerimentoTemplateService.update` altera diretamente o template existente.

Comportamento desejado:

- edição de template aprovado deve gerar nova versão;
- versão anterior deve continuar válida para submissões já criadas;
- nova versão só fica ativa após aprovação.

### Incremento 4 — Criação de Submissão Apenas com Template Ativo

Problema atual:

- `RequerimentoSubmissionService.create` verifica apenas se o template existe.

Comportamento desejado:

- submissão só pode ser criada se o template estiver aprovado e ativo.

### Incremento 5 — Autoria da Submissão

Problema atual:

- `submittedBy` pode vir do DTO enviado pelo cliente.

Comportamento desejado:

- autoria deve vir da identidade autenticada;
- o DTO não deve ser a fonte confiável da autoria.

### Incremento 6 — Snapshot do Fluxo

Problema atual:

- a submissão salva snapshot das respostas, mas não do fluxo.

Comportamento desejado:

- ao criar submissão, copiar o fluxo de tramitação do template para a submissão.

### Incremento 7 — Regras de Visibilidade

Problema atual:

- `RequerimentoSubmissionService.get` e `findByTemplate` retornam dados sem considerar papel, dono, curso ou responsável.

Comportamento desejado:

- aplicar filtros por papel e identidade no serviço;
- aluno vê apenas o próprio;
- professor vê próprias, atribuídas, matérias lecionadas ou matrícula direcionada;
- coordenador vê escopo do curso;
- secretaria e diretor seguem regras de fluxo e escopo institucional.

### Incremento 8 — Abertura, Edição e Cancelamento

Problema atual:

- não existe controle de abertura pelo responsável;
- não existe edição de submissão;
- não existe cancelamento.

Comportamento desejado:

- registrar quando o responsável abriu o requerimento;
- permitir edição por até 1 hora se ainda não houve abertura;
- permitir cancelamento antes da abertura;
- exigir aprovação para cancelamento após abertura.

## Dúvidas Pendentes

As perguntas abaixo ainda precisam de definição antes de virar código estável.

1. Como o sistema identificará o curso do aluno, professor e coordenador enquanto a API institucional não estiver integrada?
2. A `SECRETARIA` pode ver todos os requerimentos ou apenas os que estiverem em etapa de secretaria?
3. O `DIRETOR` vê todos os cursos ou apenas cursos sob sua direção no contexto do sistema?
4. Quais informações identificam uma matéria lecionada pelo professor?
5. Como representar uma submissão direcionada à matrícula de um professor?
6. A aprovação de template por professor ou secretaria é uma aprovação real ou apenas parecer consultivo?
7. Quando o diretor cria um template sem exigir aprovações adicionais, ele nasce aprovado e ativo automaticamente?
8. Quem recebe notificação quando um template é reprovado?
9. Quem recebe notificação quando uma submissão muda de etapa?
10. O sistema deve guardar histórico completo de todas as decisões, encaminhamentos e alterações?
