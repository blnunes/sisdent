package br.com.itbn.sisdent.comum;

import java.util.List;

public interface ServiceSisdent<T extends SisdentDTO> {
    List<T> findAll();
    T getOne(String id) throws Exception;
    T update(String id, T dto) throws Exception;
    T create (T dto);
    void delete(String id);

}
