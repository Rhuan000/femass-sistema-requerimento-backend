# Integração frontend — rascunho de requerimento

Este fluxo permite criar o requerimento antes de o formulário estar completo. Assim que o usuário começar o preenchimento, o frontend deve criar um rascunho e guardar o `id` retornado para salvar respostas e gerenciar anexos.

Todas as requisições devem enviar o token do usuário:

```http
Authorization: Bearer <token>
```

O usuário é identificado pelo token. O frontend não deve enviar `usuarioId`.

## 1. Criar o rascunho

```http
POST /submissions
Content-Type: application/json
```

```json
{
  "templateId": "UUID_DO_TEMPLATE",
  "data": {}
}
```

A resposta é `201 Created`:

```json
{
  "id": "71a8f179-6ba8-44c9-b260-912344fc17f8",
  "templateId": "UUID_DO_TEMPLATE",
  "usuarioId": "UUID_DO_USUARIO",
  "status": "RASCUNHO",
  "data": {},
  "answers": [],
  "documentos": [],
  "createdAt": "2026-09-16T18:00:00Z",
  "updatedAt": "2026-09-16T18:00:00Z",
  "submittedAt": null
}
```

O frontend deve guardar o `id` do rascunho enquanto estiver nessa tela. Preferencialmente, inclua esse UUID na rota da página para permitir a recuperação após uma atualização do navegador.

Para recarregar o rascunho e seus documentos:

```http
GET /submissions/{submissionId}
```

## 2. Salvar o preenchimento parcial

```http
PUT /submissions/{submissionId}
Content-Type: application/json
```

```json
{
  "data": {
    "nome": "Maria da Silva",
    "justificativa": "Texto ainda em preenchimento"
  }
}
```

O objeto `data` enviado substitui os dados anteriormente salvos. Portanto, o frontend deve enviar todos os campos preenchidos até aquele momento, não somente o último campo alterado.

Campos obrigatórios não são cobrados nesta etapa. É recomendável salvar automaticamente usando debounce, por exemplo, entre 500 e 1000 ms após a última alteração.

## 3. Enviar um documento

```http
POST /submissions/{submissionId}/documents
Content-Type: multipart/form-data
```

O arquivo deve ser enviado no campo `file`:

```javascript
const formData = new FormData();
formData.append("file", arquivo);

const { data: documento } = await api.post(
  `/submissions/${submissionId}/documents`,
  formData
);
```

Formatos aceitos: `pdf`, `doc`, `docx`, `jpg`, `jpeg`, `png` e `txt`. O tamanho máximo é 10 MiB.

A resposta é `201 Created` e contém o `id` necessário para excluir o documento posteriormente:

```json
{
  "id": "UUID_DO_DOCUMENTO",
  "submissionId": "UUID_DO_RASCUNHO",
  "nomeOriginal": "comprovante.pdf",
  "contentType": "application/pdf",
  "tamanho": 123456,
  "bucket": "documentos",
  "objectName": "UUID_DO_RASCUNHO/UUID-comprovante.pdf",
  "etag": "etag-do-objeto",
  "createdAt": "2026-09-16T18:05:00Z"
}
```

Para a interface, normalmente basta usar `id`, `nomeOriginal`, `contentType`, `tamanho` e `createdAt`. Os campos do MinIO não precisam ser armazenados pelo frontend.

## 4. Excluir um documento

```http
DELETE /submissions/{submissionId}/documents/{documentId}
```

Em caso de sucesso, a resposta é `204 No Content`.

## 5. Finalizar e enviar o requerimento

Antes de finalizar, aguarde o último salvamento automático e todos os uploads pendentes. Depois execute:

```http
POST /submissions/{submissionId}/submit
```

Não é necessário enviar corpo. Nesse momento o backend valida todos os campos obrigatórios e seus tipos. Se estiver tudo correto, a resposta terá:

```json
{
  "id": "71a8f179-6ba8-44c9-b260-912344fc17f8",
  "status": "ENVIADO",
  "submittedAt": "2026-09-16T18:10:00Z"
}
```

Após o status mudar para `ENVIADO`, respostas e documentos não podem mais ser alterados.

## Tratamento de erros

- `400 Bad Request`: formulário inválido, campo obrigatório ausente, tipo de arquivo não permitido ou tentativa de alterar um requerimento já enviado.
- `401 Unauthorized`: token ausente ou inválido.
- `403 Forbidden`: o rascunho pertence a outro usuário ou o usuário não possui o papel necessário.
- `404 Not Found`: requerimento, template ou documento não encontrado.
- `413 Payload Too Large`: arquivo maior que o limite HTTP.

O frontend deve exibir a mensagem retornada pela API e manter o usuário na tela caso a finalização falhe.

## Resumo do fluxo

```text
Abrir formulário
    ↓
POST /submissions → guardar submissionId
    ↓
PUT /submissions/{id} → salvar respostas parciais
    ↓
POST/DELETE /submissions/{id}/documents → gerenciar anexos
    ↓
POST /submissions/{id}/submit
    ↓
status = ENVIADO
```
