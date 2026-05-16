package org.paul.microorm.generator;

import org.paul.microorm.metadata.ColumnMetadata;
import org.paul.microorm.metadata.EntityMetadata;
import org.paul.microorm.metadata.InheritanceMetadata;
import org.paul.microorm.metadata.MetadataRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Emits a single CREATE TABLE for an entire SINGLE_TABLE inheritance hierarchy.
 * The table lives at the root's table name and includes columns from ALL subtypes,
 * plus the discriminator column. Subtype-specific columns are NULLable by default.
 */
public class SingleTableInheritanceDdl {

    /**
     * Generates DROP + CREATE DDL for the root entity's wide table.
     * Called once per inheritance root; skips non-root entities.
     */
    public static List<String> generateFor(EntityMetadata rootMeta, MetadataRegistry registry) {
        if (!rootMeta.isInheritanceRoot()) return List.of();

        InheritanceMetadata inh = rootMeta.getInheritance();
        List<String> cols = new ArrayList<>();
        Set<String> seenColumns = new HashSet<>();

        // PK
        cols.add(DdlBuilder.primaryKeyColumn(rootMeta));
        seenColumns.add(rootMeta.getIdField().getName());

        // discriminator column
        cols.add(DdlBuilder.discriminatorColumnDef(inh.getDiscriminatorColumn()));
        seenColumns.add(inh.getDiscriminatorColumn());

        // root's own columns
        for (ColumnMetadata col : rootMeta.getColumns()) {
            if (seenColumns.add(col.columnName())) {
                cols.add(DdlBuilder.columnDef(col));
            }
        }
        for (String embLine : EmbeddedFlattener.columnDefsForWithNames(rootMeta)) {
            String colName = embLine.trim().split(" ")[0];
            if (seenColumns.add(colName)) cols.add(embLine);
        }

        // each subtype contributes only columns not yet in the wide table (all nullable)
        for (Class<?> subtype : inh.getSubtypes()) {
            EntityMetadata subMeta = registry.get(subtype);
            for (ColumnMetadata col : subMeta.getColumns()) {
                if (seenColumns.add(col.columnName())) {
                    ColumnMetadata nullable = new ColumnMetadata(
                            col.field(), col.columnName(), col.sqlType(), true, col.length());
                    cols.add(DdlBuilder.columnDef(nullable));
                }
            }
            for (String embLine : EmbeddedFlattener.columnDefsForWithNames(subMeta)) {
                String colName = embLine.trim().split(" ")[0];
                if (seenColumns.add(colName)) cols.add(embLine);
            }
        }

        cols.add(DdlBuilder.primaryKeyConstraint(rootMeta.getIdField().getName()));

        List<String> ddl = new ArrayList<>();
        ddl.add(DdlBuilder.dropTable(rootMeta.getTableName()));
        ddl.add(DdlBuilder.createTable(rootMeta.getTableName(), cols));
        return ddl;
    }
}
