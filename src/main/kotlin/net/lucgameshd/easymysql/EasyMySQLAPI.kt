package net.lucgameshd.easymysql

import net.lucgameshd.easymysql.annotation.AutoRepository
import net.lucgameshd.easymysql.annotation.RegisterRepositories
import net.lucgameshd.easymysql.config.ConfigLoader
import net.lucgameshd.easymysql.config.MySQLConfig
import net.lucgameshd.easymysql.repository.SQLRepository
import net.lucgameshd.easymysql.repository.handler.RepositoryInvocationHandler
import org.reflections.Reflections
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.reflect.KClass

object EasyMySQLAPI {
    private var connection: Connection? = null
    private val repositories = mutableMapOf<KClass<*>, SQLRepository<*>>()

    @JvmStatic
    fun initialize() {
        val config: MySQLConfig = ConfigLoader.load()
        createConnection(config.url, config.user, config.password)
        val mainClassName = System.getProperty("sun.java.command").split(" ")[0]
        try {
            val mainClass = Class.forName(mainClassName)
            val annotation = mainClass.getAnnotation(RegisterRepositories::class.java)
            if (annotation != null) {
                registerRepositories(*annotation.packages)
            }
        } catch (_: Exception) {
        }
    }

    @JvmStatic
    fun createConnection(url: String, user: String, password: String) {
        if (connection == null) {
            connection = DriverManager.getConnection(url, user, password)
        }
    }

    @JvmStatic
    fun getConnection(): Connection? = connection

    @JvmStatic
    fun registerRepositories(vararg packageNames: String) {
        val reflections = Reflections(*packageNames)
        val repoClasses = reflections.getTypesAnnotatedWith(AutoRepository::class.java)
        repoClasses.forEach { clazz ->
            val proxy = Proxy.newProxyInstance(
                clazz.classLoader,
                arrayOf(clazz),
                RepositoryInvocationHandler(clazz, connection)
            ) as SQLRepository<*>
            repositories[clazz.kotlin] = proxy
        }
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T: SQLRepository<*>> getRepository(clazz: KClass<T>): T? = repositories[clazz] as T?
}
