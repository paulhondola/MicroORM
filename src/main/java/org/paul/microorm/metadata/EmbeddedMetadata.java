package org.paul.microorm.metadata;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @param columnPrefix Prefix applied to all embedded column names, e.g. "home_address_".
 */
public record EmbeddedMetadata(Field field, String columnPrefix, List<ColumnMetadata> columns) {
}
