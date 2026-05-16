package org.paul.microorm.mapper.session;

import org.paul.microorm.annotation.CascadeType;
import org.paul.microorm.exception.MicroOrmException;
import org.paul.microorm.mapper.hydrate.AssociationLoader;
import org.paul.microorm.mapper.hydrate.RowMapper;
import org.paul.microorm.mapper.jdbc.ConnectionProvider;
import org.paul.microorm.mapper.sql.CrudSqlBuilder;
import org.paul.microorm.mapper.sql.ParameterBinder;
import org.paul.microorm.metadata.AssociationKind;
import org.paul.microorm.metadata.AssociationMetadata;
import org.paul.microorm.metadata.ColumnMetadata;
import org.paul.microorm.metadata.EmbeddedMetadata;
import org.paul.microorm.metadata.EntityMetadata;
import org.paul.microorm.metadata.MetadataRegistry;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

/**
 * Core implementation of the Mapper API.
 * Orchestrates: PreparedStatement construction, parameter binding, ID write-back,
 * cascade persist/remove, and eager association loading via the identity map.
 */
public class EntityManagerImpl implements EntityManager {

    private final MetadataRegistry registry;
    private final ConnectionProvider connectionProvider;
    private final PersistenceContext context = new PersistenceContext();
    private Connection sharedConnection;
    private EntityTransaction transaction;

    public EntityManagerImpl(MetadataRegistry registry, ConnectionProvider connectionProvider) {
        this.registry = registry;
        this.connectionProvider = connectionProvider;
    }

    // ── persist ──────────────────────────────────────────────────────────────

    @Override
    public void persist(Object entity) {
        EntityMetadata meta = registry.get(entity.getClass());
        try {
            // Skip if already persisted in this session
            Object existingId = getIdValue(entity, meta);
            if (!isNew(existingId)) {
                EntityKey existingKey = new EntityKey(entity.getClass(), existingId);
                if (context.contains(existingKey)) return;
            }

            // 1. Cascade PERSIST to @ManyToOne targets (parent before child)
            for (AssociationMetadata assoc : meta.getAssociations()) {
                if (assoc.kind() != AssociationKind.MANY_TO_ONE) continue;
                if (!assoc.hasCascade(CascadeType.PERSIST)) continue;
                Field f = assoc.field();
                f.setAccessible(true);
                Object target = f.get(entity);
                if (target != null && isNew(getIdValue(target, registry.get(assoc.targetEntity())))) {
                    persist(target);
                }
            }

            // 2. Determine table name and discriminator
            String discriminatorColumn = null;
            String discriminatorValue  = null;
            boolean isInheritanceMember = meta.getInheritanceRoot() != null;

            if (isInheritanceMember) {
                EntityMetadata rootMeta = registry.get(meta.getInheritanceRoot());
                discriminatorColumn = rootMeta.getInheritance().getDiscriminatorColumn();
                discriminatorValue  = meta.getDiscriminatorValue() != null
                        ? meta.getDiscriminatorValue()
                        : meta.getEntityClass().getSimpleName();
            }

            // 3. Build SQL  (tableName already correct for subtypes after buildInheritanceTrees)
            String sql = CrudSqlBuilder.insert(meta, discriminatorColumn)
                    .replace("%TABLE%", meta.getTableName());

            // 4. Prepare + bind + execute
            Connection conn = openConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                int idx = 1;

                if (discriminatorColumn != null) {
                    ParameterBinder.bind(ps, idx++, discriminatorValue);
                }
                for (ColumnMetadata col : meta.getColumns()) {
                    Field f = col.field();
                    f.setAccessible(true);
                    ParameterBinder.bind(ps, idx++, f.get(entity));
                }
                for (EmbeddedMetadata emb : meta.getEmbeddedList()) {
                    Field embField = emb.field();
                    embField.setAccessible(true);
                    Object embInst = embField.get(entity);
                    for (ColumnMetadata col : emb.columns()) {
                        Field cf = col.field();
                        cf.setAccessible(true);
                        ParameterBinder.bind(ps, idx++, embInst != null ? cf.get(embInst) : null);
                    }
                }
                for (AssociationMetadata assoc : meta.getAssociations()) {
                    if (assoc.kind() != AssociationKind.MANY_TO_ONE) continue;
                    Field f = assoc.field();
                    f.setAccessible(true);
                    Object target = f.get(entity);
                    Object fkVal = (target != null)
                            ? getIdValue(target, registry.get(assoc.targetEntity()))
                            : null;
                    ParameterBinder.bind(ps, idx++, fkVal);
                }

                ps.executeUpdate();

                // 5. Write generated PK back to entity
                if (meta.isGeneratedId()) {
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            Object generatedId = keys.getObject(1);
                            Field idField = meta.getIdField();
                            idField.setAccessible(true);
                            if (idField.getType() == int.class || idField.getType() == Integer.class) {
                                idField.set(entity, ((Number) generatedId).intValue());
                            } else {
                                idField.set(entity, generatedId);
                            }
                        }
                    }
                }
            } finally {
                if (conn != sharedConnection) {
                    try { conn.close(); } catch (SQLException ignored) {}
                }
            }

            // 6. Put in identity map (after ID is set)
            Object id = getIdValue(entity, meta);
            context.put(new EntityKey(entity.getClass(), id), entity);

            // 7. Cascade PERSIST to @OneToMany children
            for (AssociationMetadata assoc : meta.getAssociations()) {
                if (assoc.kind() != AssociationKind.ONE_TO_MANY) continue;
                if (!assoc.hasCascade(CascadeType.PERSIST)) continue;
                Field f = assoc.field();
                f.setAccessible(true);
                Object collection = f.get(entity);
                if (!(collection instanceof Iterable<?> children)) continue;
                EntityMetadata childMeta = registry.get(assoc.targetEntity());
                for (Object child : children) {
                    // Wire back-reference so child's FK is set before its INSERT
                    for (AssociationMetadata childAssoc : childMeta.getAssociations()) {
                        if (childAssoc.kind() == AssociationKind.MANY_TO_ONE
                                && childAssoc.targetEntity().isAssignableFrom(entity.getClass())) {
                            Field backRef = childAssoc.field();
                            backRef.setAccessible(true);
                            backRef.set(child, entity);
                            break;
                        }
                    }
                    persist(child);
                }
            }

            // 8. Cascade PERSIST to @ManyToMany + insert junction rows
            for (AssociationMetadata assoc : meta.getAssociations()) {
                if (assoc.kind() != AssociationKind.MANY_TO_MANY) continue;
                if (!assoc.isOwner()) continue;
                Field f = assoc.field();
                f.setAccessible(true);
                Object collection = f.get(entity);
                if (!(collection instanceof Iterable<?> targets)) continue;

                EntityMetadata targetMeta = registry.get(assoc.targetEntity());
                for (Object target : targets) {
                    Object targetId = getIdValue(target, targetMeta);
                    if (isNew(targetId) && assoc.hasCascade(CascadeType.PERSIST)) {
                        persist(target);
                        targetId = getIdValue(target, targetMeta);
                    }
                    // Insert junction row
                    String junctionSql = CrudSqlBuilder.insertJunction(
                            assoc.joinTableName(),
                            assoc.joinColumnNames()[0],
                            assoc.inverseJoinColumnNames()[0]);
                    Connection jconn = openConnection();
                    try (PreparedStatement jps = jconn.prepareStatement(junctionSql)) {
                        ParameterBinder.bind(jps, 1, id);
                        ParameterBinder.bind(jps, 2, targetId);
                        jps.executeUpdate();
                    } finally {
                        if (jconn != sharedConnection) {
                            try { jconn.close(); } catch (SQLException ignored) {}
                        }
                    }
                }
            }

        } catch (MicroOrmException e) {
            throw e;
        } catch (Exception e) {
            throw new MicroOrmException("persist() failed for " + entity.getClass().getSimpleName(), e);
        }
    }

    // ── find ─────────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        EntityKey key = new EntityKey(entityClass, primaryKey);
        if (context.contains(key)) {
            return (T) context.get(key);
        }

        EntityMetadata meta = registry.get(entityClass);
        String sql = CrudSqlBuilder.selectById(meta);

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ParameterBinder.bind(ps, 1, primaryKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Object entity = RowMapper.mapRow(rs, meta, registry);
                context.put(key, entity);
                // Also index under the actual resolved class for polymorphic look-ups
                EntityKey actualKey = new EntityKey(entity.getClass(), primaryKey);
                if (!actualKey.equals(key)) context.put(actualKey, entity);
                // Use actual metadata so subtype associations are loaded
                EntityMetadata actualMeta = registry.contains(entity.getClass())
                        ? registry.get(entity.getClass()) : meta;
                AssociationLoader loader = new AssociationLoader(registry, connectionProvider, context);
                loader.loadAssociations(entity, actualMeta);
                return (T) entity;
            }
        } catch (SQLException e) {
            throw new MicroOrmException("find() failed for " + entityClass.getSimpleName() + " id=" + primaryKey, e);
        }
    }

    // ── merge ────────────────────────────────────────────────────────────────

    @Override
    public <T> T merge(T entity) {
        EntityMetadata meta = registry.get(entity.getClass());
        try {
            String sql = CrudSqlBuilder.update(meta);
            Connection conn = openConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                for (ColumnMetadata col : meta.getColumns()) {
                    Field f = col.field();
                    f.setAccessible(true);
                    ParameterBinder.bind(ps, idx++, f.get(entity));
                }
                for (EmbeddedMetadata emb : meta.getEmbeddedList()) {
                    Field embField = emb.field();
                    embField.setAccessible(true);
                    Object embInst = embField.get(entity);
                    for (ColumnMetadata col : emb.columns()) {
                        Field cf = col.field();
                        cf.setAccessible(true);
                        ParameterBinder.bind(ps, idx++, embInst != null ? cf.get(embInst) : null);
                    }
                }
                for (AssociationMetadata assoc : meta.getAssociations()) {
                    if (assoc.kind() != AssociationKind.MANY_TO_ONE) continue;
                    Field f = assoc.field();
                    f.setAccessible(true);
                    Object target = f.get(entity);
                    Object fkVal = (target != null)
                            ? getIdValue(target, registry.get(assoc.targetEntity()))
                            : null;
                    ParameterBinder.bind(ps, idx++, fkVal);
                }
                ParameterBinder.bind(ps, idx, getIdValue(entity, meta));
                ps.executeUpdate();
            } finally {
                if (conn != sharedConnection) {
                    try { conn.close(); } catch (SQLException ignored) {}
                }
            }
            return entity;
        } catch (MicroOrmException e) {
            throw e;
        } catch (Exception e) {
            throw new MicroOrmException("merge() failed for " + entity.getClass().getSimpleName(), e);
        }
    }

    // ── remove ───────────────────────────────────────────────────────────────

    @Override
    public void remove(Object entity) {
        EntityMetadata meta = registry.get(entity.getClass());
        try {
            Object id = getIdValue(entity, meta);

            // Cascade orphanRemoval to @OneToMany children first (FK from child to us)
            for (AssociationMetadata assoc : meta.getAssociations()) {
                if (assoc.kind() != AssociationKind.ONE_TO_MANY) continue;
                if (!assoc.orphanRemoval()) continue;
                Field f = assoc.field();
                f.setAccessible(true);
                Object collection = f.get(entity);
                if (collection instanceof Iterable<?> children) {
                    for (Object child : new java.util.ArrayList<>((Collection<?>) children)) {
                        remove(child);
                    }
                }
            }

            // Delete self (junction rows cascade via ON DELETE CASCADE in DDL)
            String sql = CrudSqlBuilder.deleteById(meta);
            Connection conn = openConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ParameterBinder.bind(ps, 1, id);
                ps.executeUpdate();
            } finally {
                if (conn != sharedConnection) {
                    try { conn.close(); } catch (SQLException ignored) {}
                }
            }

            context.remove(new EntityKey(entity.getClass(), id));

        } catch (MicroOrmException e) {
            throw e;
        } catch (Exception e) {
            throw new MicroOrmException("remove() failed for " + entity.getClass().getSimpleName(), e);
        }
    }

    // ── transaction ───────────────────────────────────────────────────────────

    @Override
    public EntityTransaction getTransaction() {
        if (sharedConnection == null) {
            sharedConnection = connectionProvider.getConnection();
            transaction = new EntityTransaction(sharedConnection);
        }
        return transaction;
    }

    @Override
    public void close() {
        context.clear();
        if (sharedConnection != null) {
            try { sharedConnection.close(); } catch (SQLException ignored) {}
            sharedConnection = null;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Object getIdValue(Object entity, EntityMetadata meta) throws Exception {
        Field idField = meta.getIdField();
        idField.setAccessible(true);
        return idField.get(entity);
    }

    private static boolean isNew(Object id) {
        if (id == null) return true;
        if (id instanceof Number n) return n.longValue() == 0L;
        return false;
    }

    /** Returns sharedConnection if a transaction is active, otherwise opens a fresh one. */
    private Connection openConnection() {
        return sharedConnection != null ? sharedConnection : connectionProvider.getConnection();
    }
}
