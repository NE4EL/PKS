package ru.mirea.ispteam.repository;

import java.util.List;
import java.util.Optional;

/*
 * Владелец: A.
 * Эталонный контракт из PKS.md разд. 4 — НЕ менять сигнатуры без согласования команды.
 */
public interface CrudRepository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    T update(T entity);

    void deleteById(ID id);
}
