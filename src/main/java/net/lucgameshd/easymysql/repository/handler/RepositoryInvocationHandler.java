package net.lucgameshd.easymysql.repository.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lucgameshd.easymysql.annotation.Column;
import net.lucgameshd.easymysql.annotation.Id;
import net.lucgameshd.easymysql.annotation.Table;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public record RepositoryInvocationHandler(Class<?> repository, Connection connection) implements InvocationHandler {

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        String methodName = method.getName();
        String className = this.repository.getGenericInterfaces()[0].getTypeName().split( "<" )[1].replace( ">", "" );
        Class<?> clazz = Class.forName( className );
        String tableName = clazz.getAnnotation( Table.class ).name();

        if ( methodName.startsWith( "findAll" ) ) {
            return this.findAll( tableName, clazz );
        } else if ( methodName.startsWith( "findAllBy" ) ) {
            return this.findAllBy( tableName, methodName, args, clazz );
        } else if ( methodName.startsWith( "findOneBy" ) ) {
            return this.findOneBy( tableName, methodName, args, clazz );
        } else if ( methodName.equalsIgnoreCase( "save" ) ) {
            this.save( tableName, args );
        } else if ( methodName.equalsIgnoreCase( "delete" ) ) {
            this.delete( tableName, args );
        } else if ( methodName.startsWith( "deleteBy" ) ) {
            this.deleteBy( tableName, methodName, args );
        }
        return "N/A";
    }

    private List<Object> findAll( String tableName, Class<?> clazz ) {
        String query = "SELECT * FROM " + tableName;
        List<Object> objectList = new ArrayList<>();
        try ( PreparedStatement statement = this.connection.prepareStatement( query ) ) {
            ResultSet resultSet = statement.executeQuery();
            while ( resultSet.next() ) {
                JSONObject obj = new JSONObject();
                int totalRows = resultSet.getMetaData().getColumnCount();
                for ( int i = 0; i < totalRows; i++ ) {
                    obj.put( resultSet.getMetaData().getColumnLabel( i + 1 ), resultSet.getObject( i + 1 ) );
                }
                ObjectMapper objectMapper = new ObjectMapper();
                objectList.add( objectMapper.readValue( obj.toString(), clazz ) );
            }
            resultSet.close();
            return objectList;
        } catch ( SQLException | JsonProcessingException e ) {
            e.printStackTrace();
        }
        return objectList;
    }

    private List<Object> findAllBy( String tableName, String methodName, Object[] args, Class<?> clazz ) {
        List<String> methodeWords = new ArrayList<>( Arrays.asList( methodName.substring( 9 ).split( "(?=\\p{Upper})" ) ) );
        methodeWords.removeIf( text -> text.equalsIgnoreCase( "and" ) );

        StringBuilder stringBuilder = new StringBuilder();
        for ( int i = 0; i < methodeWords.size(); i++ ) {
            String word = methodeWords.get( i );
            Object argument = args[i];
            stringBuilder.append( " " ).append( word.toLowerCase() ).append( "='" ).append( argument ).append( "'" ).append( " AND" );
        }
        stringBuilder.setLength( stringBuilder.length() - 4 );
        String query = "SELECT * FROM " + tableName + " WHERE" + stringBuilder;
        List<Object> objectList = new ArrayList<>();
        try ( PreparedStatement statement = this.connection.prepareStatement( query ) ) {
            ResultSet resultSet = statement.executeQuery();
            while ( resultSet.next() ) {
                JSONObject obj = new JSONObject();
                int totalRows = resultSet.getMetaData().getColumnCount();
                for ( int i = 0; i < totalRows; i++ ) {
                    obj.put( resultSet.getMetaData().getColumnLabel( i + 1 ), resultSet.getObject( i + 1 ) );
                }
                ObjectMapper objectMapper = new ObjectMapper();
                objectList.add( objectMapper.readValue( obj.toString(), clazz ) );
            }
            resultSet.close();
        } catch ( SQLException | JsonProcessingException e ) {
            e.printStackTrace();
        }
        return objectList;
    }

    private Optional<?> findOneBy( String tableName, String methodName, Object[] args, Class<?> clazz ) {
        List<String> methodeWords = new ArrayList<>( Arrays.asList( methodName.substring( 9 ).split( "(?=\\p{Upper})" ) ) );
        methodeWords.removeIf( text -> text.equalsIgnoreCase( "and" ) );
        StringBuilder stringBuilder = new StringBuilder();
        for ( int i = 0; i < methodeWords.size(); i++ ) {
            String word = methodeWords.get( i );
            Object argument = args[i];
            stringBuilder.append( " " ).append( word.toLowerCase() ).append( "='" ).append( argument ).append( "'" ).append( " AND" );
        }
        stringBuilder.setLength( stringBuilder.length() - 4 );

        String query = "SELECT * FROM " + tableName + " WHERE" + stringBuilder;
        try ( PreparedStatement statement = this.connection.prepareStatement( query ) ) {
            ResultSet resultSet = statement.executeQuery();
            if ( resultSet.next() ) {
                JSONObject obj = new JSONObject();
                int totalRows = resultSet.getMetaData().getColumnCount();
                for ( int i = 0; i < totalRows; i++ ) {
                    obj.put( resultSet.getMetaData().getColumnLabel( i + 1 ), resultSet.getObject( i + 1 ) );
                }
                ObjectMapper objectMapper = new ObjectMapper();
                resultSet.close();
                return Optional.ofNullable( objectMapper.readValue( obj.toString(), clazz ) );
            } else {
                resultSet.close();
                return Optional.empty();
            }
        } catch ( SQLException | JsonProcessingException e ) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private String getType( Class<?> clazz, Column column ) {
        if ( clazz.equals( String.class ) ) {
            if ( column != null ) {
                return "VARCHAR(" + column.length() + ")";
            } else {
                return "VARCHAR(255)";
            }
        } else if ( clazz.equals( int.class ) || clazz.equals( Integer.class ) ) {
            return "INT";
        } else if ( clazz.equals( Long.class ) ) {
            return "BIGINT";
        } else if ( clazz.equals( Float.class ) ) {
            return "FLOAT";
        } else if ( clazz.equals( Double.class ) ) {
            return "DOUBLE";
        } else if ( clazz.equals( Boolean.class ) || clazz.equals( boolean.class ) ) {
            return "TINYINT";
        }
        return "LONGTEXT";
    }

    private void save( String tableName, Object[] args ) throws Exception {
        Class<?> parameter = args[0].getClass();
        Field idField = null;
        for ( Field field : parameter.getDeclaredFields() ) {
            field.setAccessible( true );
            if ( field.isAnnotationPresent( Id.class ) ) {
                idField = field;
            }
        }
        if ( idField != null ) {
            try ( PreparedStatement statement = this.connection.prepareStatement( "SHOW TABLES LIKE '" + tableName + "'" ) ) {
                ResultSet resultSet = statement.executeQuery();
                if ( !resultSet.next() ) {
                    StringBuilder builder = new StringBuilder( "CREATE TABLE IF NOT EXISTS " + tableName + "(" );
                    builder.append( idField.getName() ).append( " BIGINT AUTO_INCREMENT NOT NULL," );
                    for ( Field field : parameter.getDeclaredFields() ) {
                        field.setAccessible( true );
                        if ( !field.isAnnotationPresent( Id.class ) ) {
                            Column column = field.isAnnotationPresent( Column.class ) ? field.getAnnotation( Column.class ) : null;
                            String columnName = column != null && !column.name().isEmpty() ? column.name() : field.getName();
                            builder.append( " " ).append( columnName ).append( " " ).append( this.getType( field.getType(), column ) ).append( "," );
                        } else {
                            if ( !( field.getType().equals( int.class ) || field.getType().equals( Integer.class ) || field.getType().equals( Long.class ) || field.getType().equals( long.class ) ) ) {
                                throw new Exception( "Id column must be a integer or long" );
                            }
                        }
                    }
                    builder.append( " primary key(" ).append( idField.getName() ).append( "));" );
                    try ( PreparedStatement createTableStatement = this.connection.prepareStatement( builder.toString() ) ) {
                        createTableStatement.executeUpdate();
                    } catch ( SQLException e ) {
                        e.printStackTrace();
                    }
                }
                resultSet.close();
            } catch ( SQLException e ) {
                e.printStackTrace();
            }

            String checkQuery = "SELECT * FROM " + tableName + " WHERE " + idField.getName() + "='" + idField.get( args[0] ) + "'";
            try ( PreparedStatement checkStatement = this.connection.prepareStatement( checkQuery ) ) {
                ResultSet checkResultSet = checkStatement.executeQuery();
                if ( !checkResultSet.next() ) {
                    StringBuilder firstBuilder = new StringBuilder();
                    StringBuilder secondBuilder = new StringBuilder();
                    for ( Field field : parameter.getDeclaredFields() ) {
                        field.setAccessible( true );
                        if ( !field.isAnnotationPresent( Id.class ) ) {
                            firstBuilder.append( field.getName() ).append( ", " );
                            secondBuilder.append( "'" ).append( field.get( args[0] ) ).append( "'" ).append( ", " );
                        }
                    }
                    firstBuilder.setLength( firstBuilder.length() - 2 );
                    secondBuilder.setLength( secondBuilder.length() - 2 );
                    String insertQuery = "INSERT INTO " + tableName + " (" + firstBuilder + ") VALUES(" + secondBuilder + ")";
                    try ( PreparedStatement insertStatement = this.connection.prepareStatement( insertQuery ) ) {
                        insertStatement.executeUpdate();
                    } catch ( SQLException e ) {
                        e.printStackTrace();
                    }
                } else {
                    StringBuilder firstBuilder = new StringBuilder();
                    for ( Field field : parameter.getDeclaredFields() ) {
                        field.setAccessible( true );
                        if ( !field.isAnnotationPresent( Id.class ) ) {
                            firstBuilder.append( field.getName() ).append( "='" ).append( field.get( args[0] ) ).append( "', " );
                        }
                    }
                    firstBuilder.setLength( firstBuilder.length() - 2 );
                    String updateQuery = "UPDATE " + tableName + " SET " + firstBuilder + " WHERE " + idField.getName() + "='" + idField.get( args[0] ) + "'";

                    try ( PreparedStatement updateStatement = this.connection.prepareStatement( updateQuery ) ) {
                        updateStatement.executeUpdate();
                    } catch ( SQLException e ) {
                        e.printStackTrace();
                    }
                }
                checkResultSet.close();
            } catch ( SQLException e ) {
                e.printStackTrace();
            }
        } else {
            throw new Exception( "Id annotation not found" );
        }
    }

    public void delete( String tableName, Object[] args ) throws IllegalAccessException {
        Class<?> parameter = args[0].getClass();
        StringBuilder builder = new StringBuilder();
        for ( Field field : parameter.getDeclaredFields() ) {
            field.setAccessible( true );
            builder.append( field.getName() ).append( "='" ).append( field.get( args[0] ) ).append( "'" ).append( " AND " );
        }
        builder.setLength( builder.length() - 4 );
        String deleteQuery = "DELETE FROM " + tableName + " WHERE " + builder;
        try ( PreparedStatement deleteStatement = this.connection.prepareStatement( deleteQuery ) ) {
            deleteStatement.executeUpdate();
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }

    public void deleteBy( String tableName, String methodName, Object[] args ) {
        String[] methodeWords = methodName.substring( 8 ).split( "(?=\\p{Upper})" );
        StringBuilder stringBuilder = new StringBuilder();
        for ( int i = 0; i < methodeWords.length; i++ ) {
            String word = methodeWords[i];
            Object argument = args[i];
            stringBuilder.append( " " ).append( word.toLowerCase() ).append( "='" ).append( argument ).append( "'" ).append( " AND " );
        }
        stringBuilder.setLength( stringBuilder.length() - 5 );
        String deleteQuery = "DELETE FROM " + tableName + " WHERE" + stringBuilder;
        try ( PreparedStatement deleteStatement = this.connection.prepareStatement( deleteQuery ) ) {
            deleteStatement.executeUpdate();
        } catch ( SQLException e ) {
            e.printStackTrace();
        }
    }
}
