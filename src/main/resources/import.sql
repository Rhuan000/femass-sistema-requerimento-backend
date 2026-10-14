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
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000003', 'keycloak', '30000000-0000-0000-0000-000000000003')
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
    status,
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
    'ATIVO',
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

-- Usuários e papéis do domínio para os cenários de autorização.
INSERT INTO usuario (id, nome, email, ativo, createdAt) VALUES
 ('10000000-0000-0000-0000-000000000004', 'Diretor Desenvolvimento', 'diretor.dev@femass.edu.br', true, CURRENT_TIMESTAMP),
 ('10000000-0000-0000-0000-000000000005', 'Coordenador Desenvolvimento', 'coordenador.dev@femass.edu.br', true, CURRENT_TIMESTAMP),
 ('10000000-0000-0000-0000-000000000006', 'Secretaria Desenvolvimento', 'secretaria.dev@femass.edu.br', true, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO usuario_papel (id, usuario_id, papel_id) VALUES
 ('40000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004', 4),
 ('40000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000005', 3),
 ('40000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000006', 5)
ON CONFLICT DO NOTHING;

-- O externalId corresponde exatamente ao sub do token OIDC (users[].id no realm).
-- O id da linha identidade_usuario é independente do id do Keycloak.
INSERT INTO identidade_usuario (id, usuario_id, provider, externalId) VALUES
 ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', 'keycloak', '30000000-0000-0000-0000-000000000006'),
 ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000005', 'keycloak', '30000000-0000-0000-0000-000000000004'),
 ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000006', 'keycloak', '30000000-0000-0000-0000-000000000005')
ON CONFLICT DO NOTHING;
