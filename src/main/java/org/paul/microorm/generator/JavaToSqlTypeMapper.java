package org.paul.microorm.generator;

import org.paul.microorm.annotation.Column;
import org.paul.microorm.exception.SchemaException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Maps Java field types to SQL column type strings used in DDL.
 *
 * TODO (your contribution): implement toSqlType(Class<?>, int).
 * Given a Java type and a column length, return the correct SQL type string.
 *
 * Suggested mappings to implement (5-10 lines):
 *   int / Integer          → "INT"
 *   long / Long            → "BIGINT"
 *   boolean / Boolean      → "BOOLEAN"
 *   double / Double        → "DOUBLE"
 *   float / Float          → "FLOAT"
 *   BigDecimal             → "DECIMAL(19,4)"
 *   String                 → "VARCHAR(" + length + ")"
 *   LocalDate / Date       → "DATE"
 *   LocalDateTime          → "TIMESTAMP"
 *
 * For unrecognised types throw SchemaException("Unsupported Java type: " + javaType.getName()).
 */
public class JavaToSqlTypeMapper {

    public static String toSqlType(Field field) {
        Column col = field.getAnnotation(Column.class);
        int length = (col != null) ? col.length() : 255;
        return toSqlType(field.getType(), length);
    }

    public static String toSqlType(Class<?> javaType, int length) {
        if (javaType == int.class || javaType == Integer.class) return "INT";
        if (javaType == long.class || javaType == Long.class)   return "BIGINT";
        if (javaType == boolean.class || javaType == Boolean.class) return "BOOLEAN";
        if (javaType == double.class || javaType == Double.class)  return "DOUBLE";
        if (javaType == float.class  || javaType == Float.class)   return "FLOAT";
        if (javaType == BigDecimal.class) return "DECIMAL(19,4)";
        if (javaType == String.class) return "VARCHAR(" + (length > 0 ? length : 255) + ")";
        if (javaType == LocalDate.class || javaType == Date.class) return "DATE";
        if (javaType == LocalDateTime.class) return "TIMESTAMP";
        throw new SchemaException("Unsupported Java type: " + javaType.getName());
    }
}
