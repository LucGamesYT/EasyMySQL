package net.lucgameshd.easymysql;

import net.lucgameshd.easymysql.repository.SQLRepository;
import net.lucgameshd.easymysql.repository.handler.RepositoryInvocationHandler;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class EasyMySQLAPI {

    private static Connection connection = null;

    public static SQLRepository<?> create( Class<?> clazz ) {
        return (SQLRepository<?>) Proxy.newProxyInstance( clazz.getClassLoader(), new Class[]{ clazz }, new RepositoryInvocationHandler( clazz, connection ) );
    }

    public static SQLRepository<?> create( Class<?> clazz, Connection connection ) {
        return (SQLRepository<?>) Proxy.newProxyInstance( clazz.getClassLoader(), new Class[]{ clazz }, new RepositoryInvocationHandler( clazz, connection ) );
    }

    public static void createConnection( String url, String user, String password ) throws SQLException {
        if ( connection == null ) {
            connection = DriverManager.getConnection( url, user, password );
        }
    }

    public static Connection getConnection() {
        return connection;
    }

}
