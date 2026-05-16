package org.paul.microorm.mapper.sql;

import org.paul.microorm.exception.MicroOrmException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Binds a Java value to a PreparedStatement parameter slot.
 * NEVER uses string concatenation — all values go through setXxx().
 * This satisfies the course's PreparedStatement discipline from the JDBC slides.
 */
public class ParameterBinder {

    /**
     * Binds {@code value} at parameter index {@code paramIndex} (1-based).
     * Handles null via setNull with a best-guess SQL type.
     */
    public static void bind(PreparedStatement ps, int paramIndex, Object value) {
        try {
            if (value == null) {
                ps.setNull(paramIndex, Types.NULL);
                return;
            }

            switch (value) {
                case Integer i  -> ps.setInt(paramIndex, i);
                case Long l     -> ps.setLong(paramIndex, l);
                case Double d   -> ps.setDouble(paramIndex, d);
                case Float f    -> ps.setFloat(paramIndex, f);
                case Boolean b  -> ps.setBoolean(paramIndex, b);
                case String s   -> ps.setString(paramIndex, s);
                case BigDecimal bd -> ps.setBigDecimal(paramIndex, bd);
                case LocalDate ld  -> ps.setObject(paramIndex, ld);
                case LocalDateTime ldt -> ps.setObject(paramIndex, ldt);
                case Date dt -> ps.setDate(paramIndex, new java.sql.Date(dt.getTime()));
                default -> ps.setObject(paramIndex, value);
            }
        } catch (SQLException e) {
            throw new MicroOrmException("Failed to bind parameter " + paramIndex + " with value " + value, e);
        }
    }
}
