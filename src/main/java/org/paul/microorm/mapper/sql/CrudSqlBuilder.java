package org.paul.microorm.mapper.sql;

import org.paul.microorm.metadata.AssociationKind;
import org.paul.microorm.metadata.AssociationMetadata;
import org.paul.microorm.metadata.ColumnMetadata;
import org.paul.microorm.metadata.EmbeddedMetadata;
import org.paul.microorm.metadata.EntityMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds parameterised SQL strings for single-entity CRUD.
 * All ? placeholders are populated by ParameterBinder — no string concatenation of values.
 */
public class CrudSqlBuilder {

    /**
     * INSERT INTO table (col1, col2, ...) VALUES (?, ?, ...)
     * Does NOT include the PK column when the id is generated (AUTO_INCREMENT).
     * Callers must replace the {@code %TABLE%} placeholder with the actual table name.
     */
    public static String insert(EntityMetadata meta) {
        return insert(meta, null);
    }

    /**
     * Same as {@link #insert(EntityMetadata)} but prepends a discriminator column
     * when {@code discriminatorColumn} is non-null. Caller binds that value first.
     */
    public static String insert(EntityMetadata meta, String discriminatorColumn) {
        List<String> cols = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        if (discriminatorColumn != null) {
            cols.add(discriminatorColumn);
            placeholders.add("?");
        }

        for (ColumnMetadata col : meta.getColumns()) {
            cols.add(col.columnName());
            placeholders.add("?");
        }

        for (EmbeddedMetadata emb : meta.getEmbeddedList()) {
            for (ColumnMetadata col : emb.columns()) {
                cols.add(col.columnName());
                placeholders.add("?");
            }
        }

        for (AssociationMetadata assoc : meta.getAssociations()) {
            if (assoc.kind() == AssociationKind.MANY_TO_ONE) {
                cols.add(assoc.fkColumn());
                placeholders.add("?");
            }
        }

        String colList = String.join(", ", cols);
        String valList = String.join(", ", placeholders);
        return "INSERT INTO %TABLE% (" + colList + ") VALUES (" + valList + ")";
    }

    /**
     * SELECT * FROM table WHERE id = ?
     */
    public static String selectById(EntityMetadata meta) {
        String table = meta.getTableName();
        String idCol = meta.getIdField().getName();
        return "SELECT * FROM " + table + " WHERE " + idCol + " = ?";
    }

    /**
     * SELECT * FROM table WHERE fkColumn = ?
     * Used for @OneToMany eager loading.
     */
    public static String selectByFk(String tableName, String fkColumn) {
        return "SELECT * FROM " + tableName + " WHERE " + fkColumn + " = ?";
    }

    /**
     * UPDATE table SET col1=?, col2=?, ... WHERE id=?
     */
    public static String update(EntityMetadata meta) {
        List<String> setClauses = new ArrayList<>();
        meta.getColumns().forEach(col -> setClauses.add(col.columnName() + " = ?"));
        meta.getEmbeddedList().forEach(emb ->
                emb.columns().forEach(col -> setClauses.add(col.columnName() + " = ?")));
        meta.getAssociations().stream()
                .filter(a -> a.kind() == AssociationKind.MANY_TO_ONE)
                .forEach(a -> setClauses.add(a.fkColumn() + " = ?"));

        String setStr = String.join(", ", setClauses);
        return "UPDATE " + meta.getTableName() + " SET " + setStr
                + " WHERE " + meta.getIdField().getName() + " = ?";
    }

    /**
     * DELETE FROM table WHERE id = ?
     */
    public static String deleteById(EntityMetadata meta) {
        return "DELETE FROM " + meta.getTableName()
                + " WHERE " + meta.getIdField().getName() + " = ?";
    }

    /**
     * SELECT targetFkCol FROM junctionTable WHERE ownerFkCol = ?
     * Used for @ManyToMany eager loading.
     */
    public static String selectJunctionByOwner(String junctionTable, String ownerFkCol, String targetFkCol) {
        return "SELECT " + targetFkCol + " FROM " + junctionTable + " WHERE " + ownerFkCol + " = ?";
    }

    /**
     * INSERT INTO junctionTable (ownerFkCol, targetFkCol) VALUES (?, ?)
     */
    public static String insertJunction(String junctionTable, String ownerFkCol, String targetFkCol) {
        return "INSERT INTO " + junctionTable + " (" + ownerFkCol + ", " + targetFkCol + ") VALUES (?, ?)";
    }
}
