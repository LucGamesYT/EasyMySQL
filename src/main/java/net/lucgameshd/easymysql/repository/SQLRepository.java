package net.lucgameshd.easymysql.repository;

import java.util.List;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public interface SQLRepository<T> {

    List<T> findAll();

    void save( T object );

    void delete( T object );
}
