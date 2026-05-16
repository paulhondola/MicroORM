package org.paul.microorm.metadata;

import org.paul.microorm.annotation.CascadeType;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @param fkColumn      FK column name on the owning table (MANY_TO_ONE and ONE_TO_MANY owning side).
 * @param joinTableName Junction table name for MANY_TO_MANY.
 */
public record AssociationMetadata(AssociationKind kind, Field field, Class<?> targetEntity, String mappedBy,
                                  String fkColumn, String joinTableName, String[] joinColumnNames,
                                  String[] inverseJoinColumnNames, List<CascadeType> cascade, boolean orphanRemoval) {

    /**
     * True if this side owns the FK or junction table (not a mappedBy mirror).
     */
    public boolean isOwner() {
        return mappedBy == null || mappedBy.isBlank();
    }

    public boolean hasCascade(CascadeType type) {
        return cascade.contains(CascadeType.ALL) || cascade.contains(type);
    }

    @Override
    public String toString() {
        return kind + "[" + field.getName() + " → " + targetEntity.getSimpleName() + "]";
    }
}
