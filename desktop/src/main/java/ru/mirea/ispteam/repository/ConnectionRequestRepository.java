package ru.mirea.ispteam.repository;

import ru.mirea.ispteam.model.ConnectionRequest;

import java.util.List;

/*
 * Владелец: A.
 * Эталонный контракт из PKS.md разд. 4.
 */
public interface ConnectionRequestRepository extends CrudRepository<ConnectionRequest, Long> {

    List<ConnectionRequest> findBySubscriberId(Long subscriberId);
}
