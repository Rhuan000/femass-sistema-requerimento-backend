INSERT INTO papel (id, nome, descricao)
VALUES
    (1, 'ADMIN', 'Administrador'),
    (2, 'PROFESSOR', 'professor'),
    (3, 'COORDENADOR', 'coordenador'),
    (4, 'DIRETOR', 'diretor'),
    (5, 'SECRETARIA', 'Secretaria'),
    (6, 'ORIENTADOR', 'Orientador')
ON CONFLICT DO NOTHING;

INSERT INTO usuario (id, nome, email, ativo, createdAt)
VALUES
    ('10000000-0000-0000-0000-000000000003', 'Professor Desenvolvimento', 'professor.dev@femass.edu.br', true, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO identidade_usuario (id, usuario_id, provider, externalId)
VALUES
    ('6ceaf7ee-b077-4e40-9a5d-eae09aef5c56', '10000000-0000-0000-0000-000000000003', 'keycloak', '6ceaf7ee-b077-4e40-9a5d-eae09aef5c56')
ON CONFLICT DO NOTHING;

INSERT INTO usuario_papel (id, usuario_id, papel_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', 2),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003', 6)
ON CONFLICT DO NOTHING;

INSERT INTO requerimento_template (
    id,
    name,
    description,
    category,
    version,
    isActive,
    fields,
    createdAt,
    updatedAt
)
VALUES (
    '50000000-0000-0000-0000-000000000001',
    'Solicitação de declaração',
    'Solicitação simples de declaração acadêmica.',
    'ACADEMICO',
    1,
    true,
    '[
        {
            "id": "finalidade",
            "fieldKey": "finalidade",
            "label": "Finalidade da declaração",
            "type": "text",
            "required": true,
            "placeholder": "Informe para que a declaração será utilizada",
            "description": "Descreva brevemente a finalidade do documento.",
            "options": [],
            "position": 1
        }
    ]'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;

INSERT INTO requerimento_submission (
    id,
    templateId,
    usuario_id,
    status,
    data,
    answers,
    createdAt,
    updatedAt,
    submittedAt
)
VALUES (
    '60000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000003',
    'ENVIADO',
    '{"finalidade": "Apresentação no local de trabalho"}'::jsonb,
    '[
        {
            "fieldKey": "finalidade",
            "label": "Finalidade da declaração",
            "value": "Apresentação no local de trabalho"
        }
    ]'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT DO NOTHING;
