package link.locutus.discord.db;

import com.ptsmods.mysqlw.table.ColumnStructure;
import com.ptsmods.mysqlw.table.ColumnType;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.db.entities.DBNation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SQLUtil {

    public static long castLong(Object object) {
        return ((Number) object).longValue();
    }

    public static String createTable(DBEntity<?, ?> entity) {
        ArrayList<Map.Entry<String, Class<?>>> types = new ArrayList<>(entity.getTypes().entrySet());
        return SQLUtil.createTable(entity.getTableName(), types, types.get(0).getKey());
    }

    public static String createTable(String tableName, List<Map.Entry<String, Class<?>>> types, String primaryKey) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sb.append(tableName).append(" (");
        for (int i = 0; i < types.size(); i++) {
            Map.Entry<String, Class<?>> entry = types.get(i);
            boolean isPrimaryKey = entry.getKey().equals(primaryKey);
            sb.append(entry.getKey()).append(" ").append(toSqlType(entry.getValue()));
            if (isPrimaryKey) {
                sb.append(" PRIMARY KEY");
            }
            if (i < types.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(");");
        return sb.toString();
    }

    public static String toSqlType(Class clazz) {
        if (clazz == String.class) {
            return "TEXT";
        } else if (clazz == Integer.class || clazz == int.class) {
            return "INTEGER";
        } else if (clazz == Long.class || clazz == long.class) {
            return "BIGINT";
        } else if (clazz == Double.class || clazz == double.class) {
            return "DOUBLE";
        } else if (clazz == Float.class || clazz == float.class) {
            return "FLOAT";
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return "BOOLEAN";
        } else if (clazz == Short.class || clazz == short.class) {
            return "SMALLINT";
        } else if (clazz == Byte.class || clazz == byte.class) {
            return "TINYINT";
        } else if (clazz == byte[].class) {
            return "BLOB";
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + clazz.getName());
        }
    }

    public static void set(PreparedStatement preparedStatement, int index, Object value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.NULL);
        } else {
            preparedStatement.setObject(index, value);
        }
    }
    public static <T> int[] executeBatch(Connection connection, Collection<T> objects, String query, BiConsumer<T, PreparedStatement> consumer) {
        try {
            if (objects.size() == 1) {
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    consumer.accept(objects.iterator().next(), ps);
                    int result = ps.executeUpdate();
                    return new int[]{result};
                }
            }
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                boolean clear = false;
                for (T object : objects) {
                    if (clear) ps.clearParameters();
                    clear = true;
                    consumer.accept(object, ps);
                    ps.addBatch();
                }
                return ps.executeBatch();
            }
            finally {
                try {
                    connection.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
