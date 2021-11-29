package br.com.itbn.sisdent.comum;

public interface MapperSisdent<T extends EntitySisdent, R extends FormSisdent, E extends SisdentDTO> {
    E formToDTO(R body);
    E entityToDTO(T entity);
    T dtoToEntity(E dto);
}
