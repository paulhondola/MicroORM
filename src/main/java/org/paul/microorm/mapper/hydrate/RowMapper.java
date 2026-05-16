package org.paul.microorm.mapper.hydrate;

import org.paul.microorm.exception.MicroOrmException;
import org.paul.microorm.metadata.ColumnMetadata;
import org.paul.microorm.metadata.EmbeddedMetadata;
import org.paul.microorm.metadata.EntityMetadata;
import org.paul.microorm.metadata.InheritanceMetadata;
import org.paul.microorm.metadata.MetadataRegistry;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts a JDBC ResultSet row into a populated POJO instance.
 * Uses no-arg constructors and reflection — entities must have a public no-arg constructor.
 */
public class RowMapper {

    /**
     * Instantiates the correct class (using the discriminator for inheritance hierarchies),
     * sets the PK field, populates all mapped columns and embedded fields.
     * When {@code registry} is non-null and the discriminator resolves to a subtype,
     * that subtype's metadata is used to populate all columns correctly.
     * Association fields are left null — AssociationLoader fills them in a second pass.
     */
    public static Object mapRow(ResultSet rs, EntityMetadata meta, MetadataRegistry registry) {
        try {
            Class<?> targetClass = resolveClass(rs, meta);
            Object entity = targetClass.getDeclaredConstructor().newInstance();

            // For inheritance: use the resolved subtype's full metadata to read all its columns
            EntityMetadata actualMeta = meta;
            if (targetClass != meta.getEntityClass() && registry != null && registry.contains(targetClass)) {
                actualMeta = registry.get(targetClass);
            }

            // PK
            Field idField = actualMeta.getIdField();
            idField.setAccessible(true);
            idField.set(entity, rs.getObject(idField.getName()));

            // plain columns
            for (ColumnMetadata col : actualMeta.getColumns()) {
                Field field = col.field();
                field.setAccessible(true);
                field.set(entity, rs.getObject(col.columnName()));
            }

            // embedded columns
            for (EmbeddedMetadata emb : actualMeta.getEmbeddedList()) {
                Field embField = emb.field();
                embField.setAccessible(true);
                Object embInstance = emb.field().getType().getDeclaredConstructor().newInstance();
                for (ColumnMetadata col : emb.columns()) {
                    Field f = col.field();
                    f.setAccessible(true);
                    f.set(embInstance, rs.getObject(col.columnName()));
                }
                embField.set(entity, embInstance);
            }

            return entity;
        } catch (Exception e) {
            throw new MicroOrmException("Failed to map ResultSet row to " + meta.getEntityClass().getSimpleName(), e);
        }
    }

    /** Returns root class or the concrete subclass dispatched by the discriminator value. */
    private static Class<?> resolveClass(ResultSet rs, EntityMetadata meta) throws SQLException {
        InheritanceMetadata inh = meta.getInheritance();
        if (inh == null) return meta.getEntityClass();
        String dtype = rs.getString(inh.getDiscriminatorColumn());
        return (dtype != null) ? inh.resolve(dtype) : meta.getEntityClass();
    }
}
