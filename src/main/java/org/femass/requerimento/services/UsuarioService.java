package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.femass.requerimento.entities.*;
import org.femass.requerimento.repositories.*;

import java.util.UUID;

@ApplicationScoped
public class UsuarioService {

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    IdentidadeUsuarioRepository identidadeRepository;

    @Inject
    PapelRepository papelRepository;

    @Transactional
    public Usuario criarOuObterPorIdentidade(
            String provider,
            String externalId,
            String nome,
            String email
    ) {

        IdentidadeUsuario identidade =
                identidadeRepository.findByProviderAndExternalId(
                        provider,
                        externalId
                );

        if (identidade != null) {
            return identidade.usuario;
        }

        Usuario usuario = new Usuario();

        usuario.nome = nome;
        usuario.email = email;
        usuario.ativo = true;

        usuario.adicionarIdentidade(
                provider,
                externalId
        );

        usuarioRepository.persist(usuario);

        return usuario;
    }

    @Transactional
    public void adicionarPapel(
            UUID usuarioId,
            String nomePapel
    ) {

        Usuario usuario =
                usuarioRepository.findById(usuarioId);

        if (usuario == null) {
            throw new RuntimeException("Usuário não encontrado");
        }

        Papel papel =
                papelRepository.findByNome(nomePapel);

        if (papel == null) {
            throw new RuntimeException(
                    "Papel não encontrado: " + nomePapel
            );
        }

        boolean possuiPapel = usuario.papeis.stream()
                .anyMatch(
                        up -> up.papel.id.equals(papel.id)
                );

        if (!possuiPapel) {
            usuario.adicionarPapel(papel);
        }
    }
}
