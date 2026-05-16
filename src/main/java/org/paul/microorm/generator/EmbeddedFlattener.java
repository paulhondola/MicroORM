package org.paul.microorm.generator;

import org.paul.microorm.metadata.ColumnMetadata;
import org.paul.microorm.metadata.EmbeddedMetadata;
import org.paul.microorm.metadata.EntityMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces the DDL column definition lines for @Embedded fields.
 * Embedded fields are inlined into the owning entity's table — no separate row.
 */
public class EmbeddedFlattener {

    /** Returns all column definition lines for every @Embedded field on entityMeta. */
    public static List<String> columnDefsFor(EntityMetadata entityMeta) {
        return columnDefsForWithNames(entityMeta);
    }

    /**
     * Same as {@link #columnDefsFor} — returned strings start with the column name,
     * allowing callers to extract the name for deduplication.
     */
    public static List<String> columnDefsForWithNames(EntityMetadata entityMeta) {
        List<String> lines = new ArrayList<>();
        for (EmbeddedMetadata emb : entityMeta.getEmbeddedList()) {
            for (ColumnMetadata col : emb.columns()) {
                lines.add(DdlBuilder.columnDef(col));
            }
        }
        return lines;
    }
}
