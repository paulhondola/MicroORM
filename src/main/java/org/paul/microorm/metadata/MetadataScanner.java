package org.paul.microorm.metadata;

import org.paul.microorm.annotation.*;
import org.paul.microorm.exception.MetadataException;
import org.paul.microorm.exception.UnsupportedMappingException;
import org.paul.microorm.generator.JavaToSqlTypeMapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reflects over an @Entity class and produces an EntityMetadata descriptor.
 * Called once per class at startup by MetadataRegistry.
 */
public class MetadataScanner {

    public static EntityMetadata scan(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new MetadataException("Not @Entity: " + entityClass.getName());
        }

        EntityMetadata meta = new EntityMetadata(entityClass);
        resolveTableName(entityClass, meta);
        scanFields(entityClass, meta);
        resolveInheritanceAnnotations(entityClass, meta);
        return meta;
    }

    // ── table name ───────────────────────────────────────────────────────────

    private static void resolveTableName(Class<?> cls, EntityMetadata meta) {
        Table table = cls.getAnnotation(Table.class);
        if (table != null && !table.name().isBlank()) {
            meta.setTableName(table.name());
        } else {
            meta.setTableName(toSnakeCase(cls.getSimpleName()));
        }
    }

    // ── field scanning ───────────────────────────────────────────────────────

    private static void scanFields(Class<?> entityClass, EntityMetadata meta) {
        // TODO: Walk every field declared in entityClass AND its superclasses (up to Object).
        //
        // For each field (use getAllFields helper):
        //
        //   1. Skip synthetic fields and fields annotated with @Transient.
        //
        //   2. If @Id is present → meta.setIdField(field)
        //      Also check for @GeneratedValue → meta.setGeneratedId(true).
        //      The @Id field itself should NOT be added to meta.getColumns() — the generator
        //      emits it separately as PRIMARY KEY AUTO_INCREMENT.
        //
        //   3. If @ManyToOne is present → build AssociationMetadata(MANY_TO_ONE, ...)
        //      Derive fkColumn from @JoinColumn.name(), falling back to fieldName + "_id".
        //      Target entity = field.getType().
        //
        //   4. If @OneToMany is present → build AssociationMetadata(ONE_TO_MANY, ...)
        //      mappedBy comes from OneToMany.mappedBy(). fkColumn is resolved later in Registry.
        //      Target entity = extract the generic type argument (List<Car> → Car.class).
        //
        //   5. If @ManyToMany is present → build AssociationMetadata(MANY_TO_MANY, ...)
        //      If @JoinTable is present this side owns the junction table.
        //      Target entity = extract the generic type argument.
        //
        //   6. If @Embedded is present → call scanEmbeddedField(field, meta).
        //
        //   7. Otherwise → call JavaToSqlTypeMapper.toSqlType(field) and build ColumnMetadata.

        for (Field field : getAllFields(entityClass)) {
            if (field.isSynthetic()) continue;
            if (field.isAnnotationPresent(Transient.class)) continue;

            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                meta.setIdField(field);
                meta.setGeneratedId(field.isAnnotationPresent(GeneratedValue.class));
                continue;
            }

            if (field.isAnnotationPresent(ManyToOne.class)) {
                meta.getAssociations().add(buildManyToOne(field));
                continue;
            }

            if (field.isAnnotationPresent(OneToMany.class)) {
                meta.getAssociations().add(buildOneToMany(field));
                continue;
            }

            if (field.isAnnotationPresent(ManyToMany.class)) {
                meta.getAssociations().add(buildManyToMany(field));
                continue;
            }

            if (field.isAnnotationPresent(Embedded.class)) {
                scanEmbeddedField(field, meta);
                continue;
            }

            // plain column
            field.setAccessible(true);
            Column col = field.getAnnotation(Column.class);
            String colName = (col != null && !col.name().isBlank()) ? col.name() : toSnakeCase(field.getName());
            boolean nullable = (col == null) || col.nullable();
            int length = (col != null) ? col.length() : 255;
            String sqlType = JavaToSqlTypeMapper.toSqlType(field);
            meta.getColumns().add(new ColumnMetadata(field, colName, sqlType, nullable, length));
        }
    }

    private static AssociationMetadata buildManyToOne(Field field) {
        field.setAccessible(true);
        ManyToOne ann = field.getAnnotation(ManyToOne.class);
        JoinColumn jc = field.getAnnotation(JoinColumn.class);
        String fkCol = (jc != null && !jc.name().isBlank())
                ? jc.name()
                : toSnakeCase(field.getName()) + "_id";
        return new AssociationMetadata(
                AssociationKind.MANY_TO_ONE, field, field.getType(),
                "", fkCol, null, new String[0], new String[0],
                List.of(ann.cascade()), false);
    }

    private static AssociationMetadata buildOneToMany(Field field) {
        field.setAccessible(true);
        OneToMany ann = field.getAnnotation(OneToMany.class);
        Class<?> target = extractGenericType(field);
        return new AssociationMetadata(
                AssociationKind.ONE_TO_MANY, field, target,
                ann.mappedBy(), null, null, new String[0], new String[0],
                List.of(ann.cascade()), ann.orphanRemoval());
    }

    private static AssociationMetadata buildManyToMany(Field field) {
        field.setAccessible(true);
        ManyToMany ann = field.getAnnotation(ManyToMany.class);
        JoinTable jt = field.getAnnotation(JoinTable.class);
        Class<?> target = extractGenericType(field);

        String joinTable = "";
        String[] joinCols = new String[0];
        String[] invCols = new String[0];
        if (jt != null) {
            joinTable = jt.name().isBlank()
                    ? toSnakeCase(field.getDeclaringClass().getSimpleName()) + "_" + toSnakeCase(target.getSimpleName())
                    : jt.name();
            joinCols = Arrays.stream(jt.joinColumns()).map(JoinColumn::name).toArray(String[]::new);
            invCols = Arrays.stream(jt.inverseJoinColumns()).map(JoinColumn::name).toArray(String[]::new);
        }

        return new AssociationMetadata(
                AssociationKind.MANY_TO_MANY, field, target,
                ann.mappedBy(), null, joinTable, joinCols, invCols,
                List.of(ann.cascade()), false);
    }

    private static void scanEmbeddedField(Field field, EntityMetadata meta) {
        field.setAccessible(true);
        Class<?> embeddableClass = field.getType();
        if (!embeddableClass.isAnnotationPresent(Embeddable.class)) {
            throw new MetadataException("@Embedded field '" + field.getName()
                    + "' targets a class not annotated with @Embeddable: " + embeddableClass.getName());
        }
        String prefix = toSnakeCase(field.getName()) + "_";
        List<ColumnMetadata> embCols = new ArrayList<>();

        for (Field ef : embeddableClass.getDeclaredFields()) {
            if (ef.isSynthetic() || ef.isAnnotationPresent(Transient.class)) continue;
            ef.setAccessible(true);
            String colName = prefix + toSnakeCase(ef.getName());
            // @AttributeOverride on the embedding field can override the column name
            AttributeOverrides overrides = field.getAnnotation(AttributeOverrides.class);
            if (overrides != null) {
                for (AttributeOverride ao : overrides.value()) {
                    if (ao.name().equals(ef.getName()) && !ao.column().name().isBlank()) {
                        colName = ao.column().name();
                    }
                }
            }
            String sqlType = JavaToSqlTypeMapper.toSqlType(ef);
            embCols.add(new ColumnMetadata(ef, colName, sqlType, true, 255));
        }
        meta.getEmbeddedList().add(new EmbeddedMetadata(field, prefix, embCols));
    }

    // ── inheritance annotations ───────────────────────────────────────────────

    private static void resolveInheritanceAnnotations(Class<?> cls, EntityMetadata meta) {
        Inheritance inh = cls.getAnnotation(Inheritance.class);
        if (inh != null) {
            if (inh.strategy() != InheritanceType.SINGLE_TABLE) {
                throw new UnsupportedMappingException(inh.strategy());
            }
            DiscriminatorColumn dc = cls.getAnnotation(DiscriminatorColumn.class);
            String dcName = (dc != null) ? dc.name() : "dtype";
            meta.setInheritance(new InheritanceMetadata(inh.strategy(), dcName, cls));
            meta.setInheritanceRoot(cls);
        }

        DiscriminatorValue dv = cls.getAnnotation(DiscriminatorValue.class);
        if (dv != null) {
            meta.setDiscriminatorValue(dv.value());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Returns all declared fields walking up the class hierarchy to Object. */
    static List<Field> getAllFields(Class<?> cls) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(Arrays.asList(c.getDeclaredFields()));
        }
        return fields;
    }

    /** Extracts the first generic type argument, e.g. List<Car> → Car.class. */
    static Class<?> extractGenericType(Field field) {
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType pt) {
            Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?> cls) return cls;
        }
        throw new MetadataException("Cannot determine target entity for collection field: " + field.getName());
    }

    /** CamelCase → snake_case, e.g. "MyClass" → "my_class". */
    public static String toSnakeCase(String name) {
        return name.replaceAll("([A-Z])", "_$1").toLowerCase().replaceAll("^_", "");
    }
}
