package net.lucgameshd.easymysql.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.UUID;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class UUIDDeserializer extends JsonDeserializer<UUID> {

    @Override
    public UUID deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) throws IOException, JacksonException {
        return UUID.fromString( jsonParser.getValueAsString() );
    }
}
