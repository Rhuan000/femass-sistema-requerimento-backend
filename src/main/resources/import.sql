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
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'keycloak', '30000000-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;

INSERT INTO usuario_papel (id, usuario_id, papel_id)
VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', 2),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003', 6)
ON CONFLICT DO NOTHING;
