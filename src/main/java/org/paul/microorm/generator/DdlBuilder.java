package org.paul.microorm.generator;

import org.paul.microorm.metadata.ColumnMetadata;
import org.paul.microorm.metadata.EntityMetadata;

import java.util.List;
import java.util.StringJoiner;

/**
 * Builds individual DDL fragments (column definitions, FK constraints, etc.).
 * SchemaGenerator assembles these into full CREATE TABLE statements.
 */
public class DdlBuilder {

    /** Returns "id INT NOT NULL AUTO_INCREMENT" or equivalent for the PK column. */
    public static String primaryKeyColumn(EntityMetadata meta) {
        String idType = JavaToSqlTypeMapper.toSqlType(meta.getIdField().getType(), 0);
        return "  " + meta.getIdField().getName() + " " + idType + " NOT NULL AUTO_INCREMENT";
    }

    /** Returns a single column definition line, e.g. "  name VARCHAR(255)". */
    public static String columnDef(ColumnMetadata col) {
        StringBuilder sb = new StringBuilder("  ");
        sb.append(col.columnName()).append(" ").append(col.sqlType());
        if (!col.nullable()) sb.append(" NOT NULL");
        return sb.toString();
    }

    /** Returns an FK column definition line, e.g. "  person_id INT". */
    public static String fkColumnDef(String fkColName, String referencedType) {
        return "  " + fkColName + " " + referencedType;
    }

    /**
     * Returns a FOREIGN KEY constraint fragment, e.g.:
     *   CONSTRAINT fk_car_person FOREIGN KEY (person_id) REFERENCES person(id)
     */
    public static String foreignKeyConstraint(String fkColName, String owningTable,
                                               String referencedTable, String referencedPk) {
        return "  CONSTRAINT fk_" + owningTable + "_" + fkColName
                + " FOREIGN KEY (" + fkColName + ") REFERENCES " + referencedTable + "(" + referencedPk + ")";
    }

    /** Returns the discriminator column definition for SINGLE_TABLE inheritance. */
    public static String discriminatorColumnDef(String columnName) {
        return "  " + columnName + " VARCHAR(31) NOT NULL";
    }

    /** Returns "  PRIMARY KEY (id)" */
    public static String primaryKeyConstraint(String idColumnName) {
        return "  PRIMARY KEY (" + idColumnName + ")";
    }

    /** Builds a full DROP TABLE IF EXISTS statement. */
    public static String dropTable(String tableName) {
        return "DROP TABLE IF EXISTS " + tableName + ";";
    }

    /** Assembles a full CREATE TABLE statement from already-built column/constraint lines. */
    public static String createTable(String tableName, List<String> columnDefs) {
        StringJoiner sj = new StringJoiner(",\n", "CREATE TABLE " + tableName + " (\n", "\n);");
        columnDefs.forEach(sj::add);
        return sj.toString();
    }
}
