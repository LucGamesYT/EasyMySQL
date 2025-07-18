package net.lucgameshd.easymysql.repository.handler

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import net.lucgameshd.easymysql.annotation.Column
import net.lucgameshd.easymysql.annotation.Id
import net.lucgameshd.easymysql.annotation.Table
import net.lucgameshd.easymysql.serialization.*
import org.json.JSONObject
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class RepositoryInvocationHandler(private val repository: Class<*>, private val connection: Connection?) : InvocationHandler {

    private val objectMapper: ObjectMapper

    init {
        val localDateTimeModul = SimpleModule()
            .addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer())
            .addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer())
        val localDateModul = SimpleModule()
            .addSerializer(LocalDate::class.java, LocalDateSerializer())
            .addDeserializer(LocalDate::class.java, LocalDateDeserializer())
        val uuidModul = SimpleModule()
            .addSerializer(UUID::class.java, UUIDSerializer())
            .addDeserializer(UUID::class.java, UUIDDeserializer())

        this.objectMapper = ObjectMapper()
        this.objectMapper.registerModules(localDateTimeModul, localDateModul, uuidModul)
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val methodName = method.name
        val className = repository.genericInterfaces[0].typeName.split("<")[1].replace(">", "")
        val clazz = Class.forName(className)
        val tableName = clazz.getAnnotation(Table::class.java).name

        return when {
            methodName.startsWith("findAll") -> findAll(tableName, clazz)
            methodName.startsWith("findAllBy") -> findAllBy(tableName, methodName, args, clazz)
            methodName.startsWith("findOneBy") -> findOneBy(tableName, methodName, args, clazz)
            methodName.equals("save", ignoreCase = true) -> { save(tableName, args); null }
            methodName.equals("delete", ignoreCase = true) -> { delete(tableName, args); null }
            methodName.startsWith("deleteBy") -> { deleteBy(tableName, methodName, args); null }
            else -> "N/A"
        }
    }

    private fun getType(clazz: Class<*>, column: Column?): String = when {
        clazz == String::class.java || clazz == UUID::class.java -> {
            if (column != null) "VARCHAR(${column.length})" else "VARCHAR(255)"
        }
        clazz == Int::class.java || clazz == Integer.TYPE -> "INT"
        clazz == Long::class.java || clazz == java.lang.Long.TYPE -> "BIGINT"
        clazz == Float::class.java || clazz == java.lang.Float.TYPE -> "FLOAT"
        clazz == Double::class.java || clazz == java.lang.Double.TYPE -> "DOUBLE"
        clazz == Boolean::class.java || clazz == java.lang.Boolean.TYPE -> "TINYINT"
        clazz == LocalDateTime::class.java -> "TIMESTAMP"
        else -> "LONGTEXT"
    }

    private fun findAll(tableName: String, clazz: Class<*>): List<Any> {
        val query = "SELECT * FROM $tableName"
        val objectList = mutableListOf<Any>()
        if (!tableExists(tableName)) return objectList
        try {
            connection?.prepareStatement(query).use { statement ->
                val resultSet = statement?.executeQuery()
                while (resultSet != null && resultSet.next()) {
                    val obj = JSONObject()
                    val totalRows = resultSet.metaData.columnCount
                    for (i in 0 until totalRows) {
                        obj.put(resultSet.metaData.getColumnLabel(i + 1), resultSet.getObject(i + 1))
                    }
                    objectList.add(objectMapper.readValue(obj.toString(), clazz))
                }
                resultSet?.close()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        return objectList
    }

    private fun findAllBy(tableName: String, methodName: String, args: Array<out Any>?, clazz: Class<*>): List<Any> {
        val objectList = mutableListOf<Any>()
        if (!tableExists(tableName)) return objectList
        val methodeWords = methodName.substring(9).split("(?=\\p{Upper})".toRegex()).filter { !it.equals("and", true) }
        val builder = StringBuilder()
        for (i in methodeWords.indices) {
            val word = methodeWords[i]
            val argument = args?.get(i)
            builder.append(" ").append(word.lowercase()).append("='").append(argument).append("' AND")
        }
        if (builder.isNotEmpty()) builder.setLength(builder.length - 4)
        val query = "SELECT * FROM $tableName WHERE$builder"
        try {
            connection?.prepareStatement(query).use { statement ->
                val resultSet = statement?.executeQuery()
                while (resultSet != null && resultSet.next()) {
                    val obj = JSONObject()
                    val totalRows = resultSet.metaData.columnCount
                    for (i in 0 until totalRows) {
                        obj.put(resultSet.metaData.getColumnLabel(i + 1), resultSet.getObject(i + 1))
                    }
                    objectList.add(objectMapper.readValue(obj.toString(), clazz))
                }
                resultSet?.close()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        return objectList
    }

    private fun findOneBy(tableName: String, methodName: String, args: Array<out Any>?, clazz: Class<*>): Optional<Any> {
        if (!tableExists(tableName)) return Optional.empty()
        val methodeWords = methodName.substring(9).split("(?=\\p{Upper})".toRegex()).filter { !it.equals("and", true) }
        val builder = StringBuilder()
        for (i in methodeWords.indices) {
            val word = methodeWords[i]
            val argument = args?.get(i)
            builder.append(" ").append(word.lowercase()).append("='").append(argument).append("' AND")
        }
        if (builder.isNotEmpty()) builder.setLength(builder.length - 4)
        val query = "SELECT * FROM $tableName WHERE$builder"
        try {
            connection?.prepareStatement(query).use { statement ->
                val resultSet = statement?.executeQuery()
                if (resultSet != null && resultSet.next()) {
                    val obj = JSONObject()
                    val totalRows = resultSet.metaData.columnCount
                    for (i in 0 until totalRows) {
                        obj.put(resultSet.metaData.getColumnLabel(i + 1), resultSet.getObject(i + 1))
                    }
                    resultSet.close()
                    return Optional.ofNullable(objectMapper.readValue(obj.toString(), clazz))
                } else {
                    resultSet?.close()
                    return Optional.empty()
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        return Optional.empty()
    }

    private fun save(tableName: String, args: Array<out Any>?) {
        val parameter = args?.get(0)?.javaClass ?: return
        var idField: Field? = null
        for (field in parameter.declaredFields) {
            field.isAccessible = true
            if (field.isAnnotationPresent(Id::class.java)) {
                idField = field
            }
        }
        if (idField != null) {
            try {
                connection?.prepareStatement("SHOW TABLES LIKE '$tableName'").use { statement ->
                    val resultSet = statement?.executeQuery()
                    if (resultSet != null && !resultSet.next()) {
                        val builder = StringBuilder("CREATE TABLE IF NOT EXISTS $tableName(")
                        builder.append(idField.name).append(" BIGINT AUTO_INCREMENT NOT NULL,")
                        for (field in parameter.declaredFields) {
                            field.isAccessible = true
                            if (!field.isAnnotationPresent(Id::class.java)) {
                                val column = if (field.isAnnotationPresent(Column::class.java)) field.getAnnotation(Column::class.java) else null
                                val columnName = if (column != null && column.name.isNotEmpty()) column.name else field.name
                                builder.append(" ").append(columnName).append(" ").append(getType(field.type, column)).append(",")
                            } else {
                                if (!(field.type == Int::class.java || field.type == Integer.TYPE || field.type == Long::class.java || field.type == java.lang.Long.TYPE)) {
                                    throw Exception("Id column must be a integer or long")
                                }
                            }
                        }
                        builder.append(" primary key(").append(idField.name).append("));")
                        connection?.prepareStatement(builder.toString()).use { createTableStatement ->
                            createTableStatement?.executeUpdate()
                        }
                    }
                    resultSet?.close()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }

            val checkQuery = "SELECT * FROM $tableName WHERE ${idField.name}='${idField.get(args[0])}'"
            try {
                connection?.prepareStatement(checkQuery).use { checkStatement ->
                    val checkResultSet = checkStatement?.executeQuery()
                    if (checkResultSet != null && !checkResultSet.next()) {
                        val firstBuilder = StringBuilder()
                        val secondBuilder = StringBuilder()
                        for (field in parameter.declaredFields) {
                            field.isAccessible = true
                            if (!field.isAnnotationPresent(Id::class.java)) {
                                firstBuilder.append(field.name).append(", ")
                                secondBuilder.append("'").append(field.get(args[0])).append("'").append(", ")
                            }
                        }
                        if (firstBuilder.isNotEmpty()) firstBuilder.setLength(firstBuilder.length - 2)
                        if (secondBuilder.isNotEmpty()) secondBuilder.setLength(secondBuilder.length - 2)
                        val insertQuery = "INSERT INTO $tableName (${firstBuilder}) VALUES(${secondBuilder})"
                        connection?.prepareStatement(insertQuery).use { insertStatement ->
                            insertStatement?.executeUpdate()
                        }
                    } else {
                        val firstBuilder = StringBuilder()
                        for (field in parameter.declaredFields) {
                            field.isAccessible = true
                            if (!field.isAnnotationPresent(Id::class.java)) {
                                firstBuilder.append(field.name).append("='").append(field.get(args[0])).append("', ")
                            }
                        }
                        if (firstBuilder.isNotEmpty()) firstBuilder.setLength(firstBuilder.length - 2)
                        val updateQuery = "UPDATE $tableName SET $firstBuilder WHERE ${idField.name}='${idField.get(args[0])}'"
                        connection?.prepareStatement(updateQuery).use { updateStatement ->
                            updateStatement?.executeUpdate()
                        }
                    }
                    checkResultSet?.close()
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        } else {
            throw Exception("Id annotation not found")
        }
    }

    private fun delete(tableName: String, args: Array<out Any>?) {
        val parameter = args?.get(0)?.javaClass ?: return
        val builder = StringBuilder()
        for (field in parameter.declaredFields) {
            field.isAccessible = true
            builder.append(field.name).append("='").append(field.get(args[0])).append("' AND ")
        }
        if (builder.isNotEmpty()) builder.setLength(builder.length - 4)
        val deleteQuery = "DELETE FROM $tableName WHERE $builder"
        try {
            connection?.prepareStatement(deleteQuery).use { deleteStatement ->
                deleteStatement?.executeUpdate()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    private fun deleteBy(tableName: String, methodName: String, args: Array<out Any>?) {
        val methodeWords = methodName.substring(8).split("(?=\\p{Upper})".toRegex())
        val builder = StringBuilder()
        for (i in methodeWords.indices) {
            val word = methodeWords[i]
            val argument = args?.get(i)
            builder.append(" ").append(word.lowercase()).append("='").append(argument).append("' AND ")
        }
        if (builder.isNotEmpty()) builder.setLength(builder.length - 5)
        val deleteQuery = "DELETE FROM $tableName WHERE$builder"
        try {
            connection?.prepareStatement(deleteQuery).use { deleteStatement ->
                deleteStatement?.executeUpdate()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    private fun tableExists(tableName: String): Boolean {
        return try {
            connection?.prepareStatement("SHOW TABLES LIKE '$tableName'").use { statement ->
                val resultSet = statement?.executeQuery()
                val exists = resultSet?.next() ?: false
                resultSet?.close()
                exists
            } ?: false
        } catch (e: SQLException) {
            false
        }
    }
}
