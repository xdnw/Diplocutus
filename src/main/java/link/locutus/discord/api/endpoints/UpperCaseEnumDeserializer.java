package link.locutus.discord.api.endpoints;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.base.CaseFormat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpperCaseEnumDeserializer<T extends Enum<T>> extends JsonDeserializer<T> {
    private final Class<T> enumType;
    private final Map<String, T> cache = new ConcurrentHashMap<>();
    private Method parseMethodOrNull;

    public UpperCaseEnumDeserializer(Class<T> enumType) {
        this.enumType = enumType;
        for (T value : enumType.getEnumConstants()) {
            cache.put(value.name().toUpperCase(Locale.ROOT), value);
            String UPPER_CASE = value.name().toUpperCase(Locale.ROOT);
            String CamelCase = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, UPPER_CASE);
            cache.put(CamelCase.toUpperCase(Locale.ROOT), value);
        }
        try {
            parseMethodOrNull = enumType.getMethod("parse", String.class);
        } catch (NoSuchMethodException e) {
        }
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String value = p.getText();
        T cached = cache.get(value.toUpperCase(Locale.ROOT).replace(" ", "_"));
        if (cached != null) {
            return cached;
        }
        if (parseMethodOrNull != null) {
            try {
                return (T) parseMethodOrNull.invoke(null, value);
            } catch (Exception e) {
                throw new IOException("Unable to deserialize enum: " + value, e);
            }
        }
        throw new IOException("Unable to deserialize enum: " + value);
    }
}