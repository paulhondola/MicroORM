package org.paul.microorm.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.paul.microorm.exception.SchemaException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JavaToSqlTypeMapper")
class JavaToSqlTypeMapperTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "int,    255, INT",
        "long,   255, BIGINT",
        "double, 255, DOUBLE",
        "float,  255, FLOAT",
    })
    @DisplayName("primitive numeric types")
    void primitiveNumericTypes(String typeName, int length, String expected) throws ClassNotFoundException {
        Class<?> type = switch (typeName) {
            case "int"    -> int.class;
            case "long"   -> long.class;
            case "double" -> double.class;
            case "float"  -> float.class;
            default       -> throw new IllegalArgumentException(typeName);
        };
        assertThat(JavaToSqlTypeMapper.toSqlType(type, length)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "Integer,  255, INT",
        "Long,     255, BIGINT",
        "Boolean,  255, BOOLEAN",
        "Double,   255, DOUBLE",
        "Float,    255, FLOAT",
    })
    @DisplayName("boxed numeric types")
    void boxedNumericTypes(String typeName, int length, String expected) {
        Class<?> type = switch (typeName) {
            case "Integer" -> Integer.class;
            case "Long"    -> Long.class;
            case "Boolean" -> Boolean.class;
            case "Double"  -> Double.class;
            case "Float"   -> Float.class;
            default        -> throw new IllegalArgumentException(typeName);
        };
        assertThat(JavaToSqlTypeMapper.toSqlType(type, length)).isEqualTo(expected);
    }

    @Test
    @DisplayName("boolean primitive → BOOLEAN")
    void booleanPrimitive() {
        assertThat(JavaToSqlTypeMapper.toSqlType(boolean.class, 0)).isEqualTo("BOOLEAN");
    }

    @Test
    @DisplayName("BigDecimal → DECIMAL(19,4)")
    void bigDecimal() {
        assertThat(JavaToSqlTypeMapper.toSqlType(BigDecimal.class, 255)).isEqualTo("DECIMAL(19,4)");
    }

    @Test
    @DisplayName("String with explicit length → VARCHAR(length)")
    void stringWithLength() {
        assertThat(JavaToSqlTypeMapper.toSqlType(String.class, 100)).isEqualTo("VARCHAR(100)");
    }

    @Test
    @DisplayName("String with length 0 falls back to 255")
    void stringZeroLengthFallback() {
        assertThat(JavaToSqlTypeMapper.toSqlType(String.class, 0)).isEqualTo("VARCHAR(255)");
    }

    @Test
    @DisplayName("LocalDate → DATE")
    void localDate() {
        assertThat(JavaToSqlTypeMapper.toSqlType(LocalDate.class, 0)).isEqualTo("DATE");
    }

    @Test
    @DisplayName("java.util.Date → DATE")
    void utilDate() {
        assertThat(JavaToSqlTypeMapper.toSqlType(Date.class, 0)).isEqualTo("DATE");
    }

    @Test
    @DisplayName("LocalDateTime → TIMESTAMP")
    void localDateTime() {
        assertThat(JavaToSqlTypeMapper.toSqlType(LocalDateTime.class, 0)).isEqualTo("TIMESTAMP");
    }

    @Test
    @DisplayName("unsupported type throws SchemaException")
    void unsupportedTypeThrows() {
        assertThatThrownBy(() -> JavaToSqlTypeMapper.toSqlType(Object.class, 255))
                .isInstanceOf(SchemaException.class)
                .hasMessageContaining("Unsupported Java type");
    }
}
