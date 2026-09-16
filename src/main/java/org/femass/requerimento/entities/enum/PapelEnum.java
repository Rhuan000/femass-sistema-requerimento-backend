public enum PapelEnum {

    ADMIN(1L),
    SERVIDOR(2L),
    ALUNO(3L),
    ANALISTA(4L);

    private final Long id;

    PapelEnum(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
