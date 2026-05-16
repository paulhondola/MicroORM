package org.paul.microorm.generator;

import org.paul.microorm.metadata.AssociationKind;
import org.paul.microorm.metadata.AssociationMetadata;
import org.paul.microorm.metadata.EntityMetadata;
import org.paul.microorm.metadata.MetadataRegistry;
import org.paul.microorm.metadata.MetadataScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Emits CREATE TABLE DDL for MANY_TO_MANY junction tables.
 * Only the OWNING side (isOwner() == true) emits the junction table.
 */
public class JoinTableResolver {

    public static List<String> junctionTableDdl(EntityMetadata entityMeta, MetadataRegistry registry) {
        List<String> ddlStatements = new ArrayList<>();

        for (AssociationMetadata assoc : entityMeta.getAssociations()) {
            if (assoc.kind() != AssociationKind.MANY_TO_MANY) continue;
            if (!assoc.isOwner()) continue;

            EntityMetadata targetMeta = registry.get(assoc.targetEntity());

            String joinTableName = assoc.joinTableName();
            if (joinTableName == null || joinTableName.isBlank()) {
                joinTableName = entityMeta.getTableName() + "_" + targetMeta.getTableName();
            }

            String ownerPkType = JavaToSqlTypeMapper.toSqlType(entityMeta.getIdField().getType(), 0);
            String targetPkType = JavaToSqlTypeMapper.toSqlType(targetMeta.getIdField().getType(), 0);

            String ownerFkCol = assoc.joinColumnNames().length > 0
                    ? assoc.joinColumnNames()[0]
                    : MetadataScanner.toSnakeCase(entityMeta.getEntityClass().getSimpleName()) + "_id";
            String targetFkCol = assoc.inverseJoinColumnNames().length > 0
                    ? assoc.inverseJoinColumnNames()[0]
                    : MetadataScanner.toSnakeCase(targetMeta.getEntityClass().getSimpleName()) + "_id";

            List<String> cols = new ArrayList<>();
            cols.add("  " + ownerFkCol + " " + ownerPkType + " NOT NULL");
            cols.add("  " + targetFkCol + " " + targetPkType + " NOT NULL");
            cols.add("  PRIMARY KEY (" + ownerFkCol + ", " + targetFkCol + ")");
            cols.add("  CONSTRAINT fk_" + joinTableName + "_owner FOREIGN KEY (" + ownerFkCol + ")"
                    + " REFERENCES " + entityMeta.getTableName() + "(" + entityMeta.getIdField().getName() + ")"
                    + " ON DELETE CASCADE");
            cols.add("  CONSTRAINT fk_" + joinTableName + "_target FOREIGN KEY (" + targetFkCol + ")"
                    + " REFERENCES " + targetMeta.getTableName() + "(" + targetMeta.getIdField().getName() + ")"
                    + " ON DELETE CASCADE");

            ddlStatements.add(DdlBuilder.dropTable(joinTableName));
            ddlStatements.add(DdlBuilder.createTable(joinTableName, cols));
        }
        return ddlStatements;
    }
}
