package net.lucgameshd.easymysql.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize( LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        jsonGenerator.writeString( localDateTime.format( DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S" )) );
    }
}
