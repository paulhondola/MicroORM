package org.paul.microorm.generator;

import org.paul.microorm.metadata.AssociationKind;
import org.paul.microorm.metadata.AssociationMetadata;
import org.paul.microorm.metadata.EntityMetadata;
import org.paul.microorm.metadata.MetadataRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines which FK column definitions belong on a given entity's table.
 * Rule: FK is always emitted on the MANY_TO_ONE (owning) side, never on ONE_TO_MANY.
 */
public class ForeignKeyResolver {

    /**
     * Returns all FK column + constraint DDL lines that should be added to entityMeta's table.
     * Only MANY_TO_ONE associations produce FK columns here.
     */
    public static List<String> fkLinesFor(EntityMetadata entityMeta, MetadataRegistry registry) {
        List<String> lines = new ArrayList<>();
        for (AssociationMetadata assoc : entityMeta.getAssociations()) {
            if (assoc.kind() != AssociationKind.MANY_TO_ONE) continue;

            EntityMetadata targetMeta = registry.get(assoc.targetEntity());
            String referencedPk = targetMeta.getIdField().getName();
            String referencedType = JavaToSqlTypeMapper.toSqlType(
                    targetMeta.getIdField().getType(), 0);

            lines.add(DdlBuilder.fkColumnDef(assoc.fkColumn(), referencedType));
            lines.add(DdlBuilder.foreignKeyConstraint(
                    assoc.fkColumn(),
                    entityMeta.getTableName(),
                    targetMeta.getTableName(),
                    referencedPk));
        }
        return lines;
    }
}
