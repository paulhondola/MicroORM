package org.paul.microorm.metadata;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntityMetadata {
    private final Class<?> entityClass;
    private String tableName;
    private Field idField;
    private boolean generatedId;
    private final List<ColumnMetadata> columns = new ArrayList<>();
    private final List<AssociationMetadata> associations = new ArrayList<>();
    private final List<EmbeddedMetadata> embeddedList = new ArrayList<>();
    /** Non-null only on the inheritance root entity. */
    private InheritanceMetadata inheritance;
    /** The root class in the hierarchy (may be this class itself). */
    private Class<?> inheritanceRoot;
    /** Value written to the discriminator column for rows of this class. */
    private String discriminatorValue;

    public EntityMetadata(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<?> getEntityClass() { return entityClass; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public Field getIdField() { return idField; }
    public void setIdField(Field idField) { this.idField = idField; }

    public boolean isGeneratedId() { return generatedId; }
    public void setGeneratedId(boolean generatedId) { this.generatedId = generatedId; }

    public List<ColumnMetadata> getColumns() { return columns; }
    public List<AssociationMetadata> getAssociations() { return associations; }
    public List<EmbeddedMetadata> getEmbeddedList() { return embeddedList; }

    public InheritanceMetadata getInheritance() { return inheritance; }
    public void setInheritance(InheritanceMetadata inheritance) { this.inheritance = inheritance; }

    public boolean isInheritanceRoot() { return inheritance != null; }

    public Class<?> getInheritanceRoot() { return inheritanceRoot; }
    public void setInheritanceRoot(Class<?> inheritanceRoot) { this.inheritanceRoot = inheritanceRoot; }

    public String getDiscriminatorValue() { return discriminatorValue; }
    public void setDiscriminatorValue(String discriminatorValue) { this.discriminatorValue = discriminatorValue; }

    @Override
    public String toString() {
        return "EntityMetadata{" + entityClass.getSimpleName() + ", table=" + tableName + "}";
    }
}
