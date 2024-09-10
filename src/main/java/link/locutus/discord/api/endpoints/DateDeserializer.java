package link.locutus.discord.api.endpoints;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateDeserializer extends JsonDeserializer<Date> {
    private static final String[] DATE_FORMATS = new String[]{
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
    };

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String date = p.getText();
        for (String format : DATE_FORMATS) {
            try {
                return new SimpleDateFormat(format, Locale.ENGLISH).parse(date);
            } catch (ParseException e) {
                // Continue to the next format
            }
        }
        throw new IOException("Unable to parse date: " + date);
    }
}