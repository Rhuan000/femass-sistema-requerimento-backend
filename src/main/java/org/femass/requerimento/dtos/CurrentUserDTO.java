package org.femass.requerimento.dtos;

import java.util.List;
import java.util.UUID;

public class CurrentUserDTO {
    public UUID id;
    public String nome;
    public String email;
    public Boolean ativo;
    public List<String> papeis;
    public Permissions permissions;

    public static class Permissions {
        public boolean canCreateTemplate;
        public boolean canManageTemplates;
        public boolean canApproveTemplates;
        public boolean canCreateSubmission;
    }
}
