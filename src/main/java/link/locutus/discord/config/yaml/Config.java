package link.locutus.discord.config.yaml;

import link.locutus.discord.Logg;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.config.yaml.file.YamlConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Config {

    public Config() {
    save(new PrintWriter(new ByteArrayOutputStream(0)), getClass(), this, 0);
    }

    /**
     * Get the value for a node. Probably throws some error if you try to get a non-existent key.
     */
    private <T> T get(String key, Class<?> root) {
        String[] split = key.split("\\.");
        Object instance = getInstance(split, root);
        if (instance != null) {
            Field field = getField(split, instance);
            if (field != null) {
                try {
                    return (T) field.get(instance);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        Logg.text("Failed to get config option: {}", key);
        return null;
    }

    /**
     * Set the value of a specific node. Probably throws some error if you supply non existing keys or invalid values.
     *
     * @param key   config node
     * @param value value
     */
    private void set(String key, Object value, Class<?> root) {
        String[] split = key.split("\\.");
        Object instance = getInstance(split, root);
        if (instance != null) {
            if (instance instanceof Map map) {
                String[] splitCopy = Arrays.copyOfRange(split, 0, split.length - 1);
                Object mapHolder = getInstance(splitCopy, root);
                Field field = getField(splitCopy, mapHolder);
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Type keyType = type.getActualTypeArguments()[0];
                if (keyType == Integer.class) {
                    map.put(Integer.parseInt(split[split.length - 1]), value);
                    return;
                } else if (keyType == Long.class) {
                    map.put(Long.parseLong(split[split.length - 1]), value);
                    return;
                } else if (keyType == Double.class) {
                    map.put(Double.parseDouble(split[split.length - 1]), value);
                    return;
                } else if (keyType == Float.class) {
                    map.put(Float.parseFloat(split[split.length - 1]), value);
                    return;
                } else if (keyType == Short.class) {
                    map.put(Short.parseShort(split[split.length - 1]), value);
                    return;
                } else if (keyType == Byte.class) {
                    map.put(Byte.parseByte(split[split.length - 1]), value);
                    return;
                } else if (keyType == Boolean.class) {
                    map.put(Boolean.parseBoolean(split[split.length - 1]), value);
                    return;
                } else if (keyType == Character.class) {
                    map.put(split[split.length - 1].charAt(0), value);
                    return;
                } else if (keyType == String.class) {
                    map.put(split[split.length - 1], value);
                    return;
                }
            } else {
                Field field = getField(split, instance);
                if (field != null) {
                    try {
                        if (field.getAnnotation(Final.class) != null) {
                            return;
                        }
                        if (field.getType() == String.class && !(value instanceof String)) {
                            value = value + "";
                        }
                        field.set(instance, value);
                        return;
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Logg.text("Failed to set config option: {}: {} | {} | {}.yml", key, value, instance, root.getSimpleName());
    }

    public boolean load(File file) {
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(true)) {
            Object value = yml.get(key);
            if (value instanceof MemorySection) {
                continue;
            }
            set(key, value, getClass());
        }
        return true;
    }

    /**
     * Set all values in the file (load first to avoid overwriting).
     */
    public void save(File file) {
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }
            PrintWriter writer = new PrintWriter(file);
            Object instance = this;
            save(writer, getClass(), instance, 0);
            writer.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Indicates that a field should be instantiated / created.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Create {

    }

    /**
     * Indicates that a field cannot be modified.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Final {

    }

    /**
     * Creates a comment.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface Comment {

        String[] value();

    }

    /**
     * The names of any default blocks.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    public @interface BlockName {

        String[] value();

    }

    /**
     * Any field or class with is not part of the config.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
    public @interface Ignore {

    }

    @Ignore // This is not part of the config
    public static class ConfigBlock<T> {

        private final HashMap<String, T> INSTANCES = new HashMap<>();

        public T remove(String key) {
            return INSTANCES.remove(key);
        }

        public T get(String key) {
            return INSTANCES.get(key);
        }

        public void put(String key, T value) {
            INSTANCES.put(key, value);
        }

        public Collection<T> getInstances() {
            return INSTANCES.values();
        }

        public Collection<String> getSections() {
            return INSTANCES.keySet();
        }

        private Map<String, T> getRaw() {
            return INSTANCES;
        }

    }

    private String toYamlString(Object value, String spacing) {
        if (value instanceof List) {
            Collection<?> listValue = (Collection<?>) value;
            if (listValue.isEmpty()) {
                return "[]";
            }
            StringBuilder m = new StringBuilder();
            for (Object obj : listValue) {
                m.append(System.lineSeparator()).append(spacing).append("- ").append(toYamlString(obj, spacing));
            }
            return m.toString();
        }
        if (value instanceof String) {
            if (((String) value) .isEmpty()) {
                return "''";
            }
            return "\"" + value + "\"";
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder m = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                m.append(System.lineSeparator()).append(spacing).append("  ").append(toYamlString(key, spacing)).append(": ").append(toYamlString(val, spacing));
            }
            return m.toString();
        }
        return value != null ? value.toString() : "null";
    }

    private void save(PrintWriter writer, Class<?> clazz, final Object instance, int indent) {
        try {
            String CTRF = System.lineSeparator();
            String spacing = StringMan.repeat(" ", indent);
            HashMap<Class<?>, Object> instances = new HashMap<>();
            for (Field field : clazz.getFields()) {
                if (field.getAnnotation(Ignore.class) != null) {
                    continue;
                }
                Class<?> current = field.getType();
                if (field.getAnnotation(Ignore.class) != null) {
                    continue;
                }
                Comment comment = field.getAnnotation(Comment.class);
                if (comment != null) {
                    for (String commentLine : comment.value()) {
                        writer.write(spacing + "# " + commentLine + CTRF);
                    }
                }
                if (current == ConfigBlock.class) {
                    current = (Class<?>) ((ParameterizedType) (field.getGenericType())).getActualTypeArguments()[0];
                    comment = current.getAnnotation(Comment.class);
                    if (comment != null) {
                        for (String commentLine : comment.value()) {
                            writer.write(spacing + "# " + commentLine + CTRF);
                        }
                    }
                    BlockName blockNames = current.getAnnotation(BlockName.class);
                    if (blockNames != null) {
                        writer.write(spacing + toNodeName(current.getSimpleName()) + ":" + CTRF);
                        ConfigBlock configBlock = (ConfigBlock) field.get(instance);
                        if (configBlock == null || configBlock.getInstances().isEmpty()) {
                            configBlock = new ConfigBlock();
                            field.set(instance, configBlock);
                            for (String blockName : blockNames.value()) {
                                configBlock.put(blockName, current.getDeclaredConstructor().newInstance());
                            }
                        }
                        // Save each instance
                        for (Map.Entry<String, Object> entry : ((Map<String, Object>) configBlock.getRaw()).entrySet()) {
                            String key = entry.getKey();
                            writer.write(spacing + "  " + toNodeName(key) + ":" + CTRF);
                            save(writer, current, entry.getValue(), indent + 4);
                        }
                    }
                    continue;
                }
                Create create = field.getAnnotation(Create.class);
                if (create != null) {
                    Object value = field.get(instance);
                    setAccessible(field);
                    if (indent == 0) {
                        writer.write(CTRF);
                    }
                    comment = current.getAnnotation(Comment.class);
                    if (comment != null) {
                        for (String commentLine : comment.value()) {
                            writer.write(spacing + "# " + commentLine + CTRF);
                        }
                    }
                    writer.write(spacing + toNodeName(current.getSimpleName()) + ":" + CTRF);
                    if (value == null) {
                        field.set(instance, value = current.getDeclaredConstructor().newInstance());
                        instances.put(current, value);
                    }
                    save(writer, current, value, indent + 2);
                } else {
                    writer.write(spacing + toNodeName(field.getName() + ": ") + toYamlString(
                            field.get(instance),
                            spacing
                    ) + CTRF);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the field for a specific config node and instance.
     * <p>
     * As expiry can have multiple blocks there will be multiple instances
     *
     * @param split    the node (split by period)
     * @param instance the instance
     */
    private Field getField(String[] split, Object instance) {
        try {
            Field field = instance.getClass().getField(toFieldName(split[split.length - 1]));
            setAccessible(field);
            return field;
        } catch (Throwable ignored) {
            Logg.text(
                    "Invalid config field: {} for {}",
                    StringMan.join(split, "."),
                    toNodeName(instance.getClass().getSimpleName())
            );
            return null;
        }
    }

    /**
     * Get the instance for a specific config node.
     *
     * @param split the node (split by period)
     * @return The instance or null
     */
    private Object getInstance(String[] split, Class<?> root) {
        try {
            Class<?> clazz = root == null ? MethodHandles.lookup().lookupClass() : root;
            Object instance = this;
            while (split.length > 0) {
                if (split.length == 1) {
                    return instance;
                }
                Class<?> found = null;
                Class<?>[] classes = clazz.getDeclaredClasses();
                for (Class<?> current : classes) {
                    if (StringMan.isEqual(current.getSimpleName(), toFieldName(split[0]))) {
                        found = current;
                        break;
                    }
                }
                try {
                    Field instanceField = clazz.getDeclaredField(toFieldName(split[0]));
                    setAccessible(instanceField);
                    if (instanceField.getType() != ConfigBlock.class) {
                        Object value = instanceField.get(instance);
                        if (value == null) {
                            value = found.getDeclaredConstructor().newInstance();
                            instanceField.set(instance, value);
                        }
                        clazz = found;
                        instance = value;
                        split = Arrays.copyOfRange(split, 1, split.length);
                        continue;
                    }
                    ConfigBlock value = (ConfigBlock) instanceField.get(instance);
                    if (value == null) {
                        value = new ConfigBlock();
                        instanceField.set(instance, value);
                    }
                    instance = value.get(split[1]);
                    if (instance == null) {
                        instance = found.getDeclaredConstructor().newInstance();
                        value.put(split[1], instance);
                    }
                    clazz = found;
                    split = Arrays.copyOfRange(split, 2, split.length);
                    continue;
                } catch (NoSuchFieldException ignored) {
                }
                if (found != null) {
                    split = Arrays.copyOfRange(split, 1, split.length);
                    clazz = found;
                    instance = clazz.getDeclaredConstructor().newInstance();
                    continue;
                }
                return null;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Translate a node to a java field name.
     */
    private String toFieldName(String node) {
        return node.toUpperCase(Locale.ROOT).replaceAll("-", "_");
    }

    /**
     * Translate a field to a config node.
     */
    private String toNodeName(String field) {
        return field.toLowerCase(Locale.ROOT).replace("_", "-");
    }

    /**
     * Set some field to be accessible.
     */
    private void setAccessible(Field field) throws NoSuchFieldException, IllegalAccessException {
        field.setAccessible(true);
        if (Modifier.isFinal(field.getModifiers())) {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
    }

}
