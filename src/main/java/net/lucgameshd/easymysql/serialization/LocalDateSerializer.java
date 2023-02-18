package net.lucgameshd.easymysql.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class LocalDateSerializer extends JsonSerializer<LocalDate> {

    @Override
    public void serialize( LocalDate localDate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider ) throws IOException {
        jsonGenerator.writeString( localDate.format( DateTimeFormatter.ofPattern( "yyyy-MM-dd" ) ) );
    }
}
