package net.lucgameshd.easymysql.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.util.UUID

class UUIDSerializer : JsonSerializer<UUID>() {
    override fun serialize(value: UUID, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.toString())
    }
}
