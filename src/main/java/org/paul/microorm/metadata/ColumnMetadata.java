package org.paul.microorm.metadata;

import java.lang.reflect.Field;

public record ColumnMetadata(Field field, String columnName, String sqlType, boolean nullable, int length) {

    @Override
    public String toString() {
        return columnName + " " + sqlType + (nullable ? "" : " NOT NULL");
    }
}
