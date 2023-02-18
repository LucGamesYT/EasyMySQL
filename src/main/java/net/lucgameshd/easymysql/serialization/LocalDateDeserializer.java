package net.lucgameshd.easymysql.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize( JsonParser jsonParser, DeserializationContext deserializationContext ) throws IOException {
        return LocalDate.parse( jsonParser.getValueAsString(), DateTimeFormatter.ofPattern( "yyyy-MM-dd" ) );
    }
}
