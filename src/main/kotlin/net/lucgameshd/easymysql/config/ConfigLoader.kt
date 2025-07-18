package net.lucgameshd.easymysql.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object ConfigLoader {
    private val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    fun load(path: Path = Paths.get("mysql-config.json")): MySQLConfig {
        if (!Files.exists(path)) {
            val config = MySQLConfig()
            Files.createDirectories(path.parent ?: Paths.get("."))
            mapper.writeValue(path.toFile(), config)
            return config
        }
        return mapper.readValue(path.toFile(), MySQLConfig::class.java)
    }
}
