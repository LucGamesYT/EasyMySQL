package net.lucgameshd.easymysql.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.util.UUID

class UUIDDeserializer : JsonDeserializer<UUID>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UUID {
        return UUID.fromString(p.valueAsString)
    }
}
