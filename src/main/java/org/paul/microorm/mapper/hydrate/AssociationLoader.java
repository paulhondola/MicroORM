package org.paul.microorm.mapper.hydrate;

import org.paul.microorm.mapper.jdbc.ConnectionProvider;
import org.paul.microorm.mapper.session.PersistenceContext;
import org.paul.microorm.mapper.sql.CrudSqlBuilder;
import org.paul.microorm.mapper.sql.ParameterBinder;
import org.paul.microorm.metadata.AssociationMetadata;
import org.paul.microorm.metadata.EntityMetadata;
import org.paul.microorm.metadata.MetadataRegistry;
import org.paul.microorm.exception.MicroOrmException;
import org.paul.microorm.mapper.session.EntityKey;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Eagerly loads all associations for a freshly hydrated entity.
 * Uses the PersistenceContext identity map to break bidirectional cycles:
 * if an EntityKey is already in the map, return the cached instance instead of re-hydrating.
 */
public class AssociationLoader {

    private final MetadataRegistry registry;
    private final ConnectionProvider connectionProvider;
    private final PersistenceContext context;

    public AssociationLoader(MetadataRegistry registry,
                             ConnectionProvider connectionProvider,
                             PersistenceContext context) {
        this.registry = registry;
        this.connectionProvider = connectionProvider;
        this.context = context;
    }

    /**
     * Populates all @ManyToOne, @OneToMany, and @ManyToMany fields on {@code entity}.
     *
     * @param entity  the partially-hydrated entity (PK + columns already set)
     * @param meta    metadata for entity's class
     */
    public void loadAssociations(Object entity, EntityMetadata meta) {
        try {
            Object id = getIdValue(entity, meta);
            for (AssociationMetadata assoc : meta.getAssociations()) {
                switch (assoc.kind()) {
                    case MANY_TO_ONE -> loadManyToOne(entity, id, assoc, meta);
                    case ONE_TO_MANY -> loadOneToMany(entity, id, assoc, meta);
                    case MANY_TO_MANY -> loadManyToMany(entity, id, assoc, meta);
                }
            }
        } catch (Exception e) {
            throw new MicroOrmException("Failed to load associations for " + meta.getEntityClass().getSimpleName(), e);
        }
    }

    // ── MANY_TO_ONE ───────────────────────────────────────────────────────────

    private void loadManyToOne(Object entity, Object ownerId,
                               AssociationMetadata assoc, EntityMetadata ownerMeta) throws Exception {
        // First: read the FK value from the owner's row via a targeted query
        String fkSql = "SELECT " + assoc.fkColumn()
                + " FROM " + ownerMeta.getTableName()
                + " WHERE " + ownerMeta.getIdField().getName() + " = ?";
        Object targetId;
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(fkSql)) {
            ParameterBinder.bind(ps, 1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                targetId = rs.getObject(1);
            }
        }
        if (targetId == null) return;

        EntityKey targetKey = new EntityKey(assoc.targetEntity(), targetId);
        if (context.contains(targetKey)) {
            setField(entity, assoc.field(), context.get(targetKey));
            return;
        }

        // Load the target entity by PK
        EntityMetadata targetMeta = registry.get(assoc.targetEntity());
        String selectSql = CrudSqlBuilder.selectById(targetMeta);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ParameterBinder.bind(ps, 1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                Object target = hydrate(rs, targetMeta, targetKey);
                setField(entity, assoc.field(), target);
            }
        }
    }

    // ── ONE_TO_MANY ───────────────────────────────────────────────────────────

    private void loadOneToMany(Object entity, Object ownerId,
                               AssociationMetadata assoc, EntityMetadata meta) throws Exception {
        EntityMetadata targetMeta = registry.get(assoc.targetEntity());
        String sql = CrudSqlBuilder.selectByFk(targetMeta.getTableName(), assoc.fkColumn());
        List<Object> children = new ArrayList<>();

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ParameterBinder.bind(ps, 1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Field idField = targetMeta.getIdField();
                    Object childId = rs.getObject(idField.getName());
                    EntityKey key = new EntityKey(assoc.targetEntity(), childId);
                    Object child = context.contains(key)
                            ? context.get(key)
                            : hydrate(rs, targetMeta, key);
                    children.add(child);
                }
            }
        }
        setField(entity, assoc.field(), children);
    }

    // ── MANY_TO_MANY ──────────────────────────────────────────────────────────

    private void loadManyToMany(Object entity, Object ownerId,
                                AssociationMetadata assoc, EntityMetadata ownerMeta) throws Exception {
        if (!assoc.isOwner()) return; // non-owner side is populated when owner is loaded

        String junctionTable = assoc.joinTableName();
        String ownerFkCol  = assoc.joinColumnNames().length > 0
                ? assoc.joinColumnNames()[0]
                : ownerMeta.getTableName() + "_id";
        String targetFkCol = assoc.inverseJoinColumnNames().length > 0
                ? assoc.inverseJoinColumnNames()[0]
                : null;

        if (targetFkCol == null) return;

        String sql = CrudSqlBuilder.selectJunctionByOwner(junctionTable, ownerFkCol, targetFkCol);
        EntityMetadata targetMeta = registry.get(assoc.targetEntity());
        Set<Object> targets = new HashSet<>();

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ParameterBinder.bind(ps, 1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object targetId = rs.getObject(1);
                    EntityKey targetKey = new EntityKey(assoc.targetEntity(), targetId);
                    Object target;
                    if (context.contains(targetKey)) {
                        target = context.get(targetKey);
                    } else {
                        target = loadById(targetMeta, targetId, targetKey);
                    }
                    if (target != null) targets.add(target);
                }
            }
        }
        // Preserve the declared collection type (Set vs List)
        Object collection = assoc.field().getType().isAssignableFrom(Set.class)
                ? targets
                : new ArrayList<>(targets);
        setField(entity, assoc.field(), collection);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Object hydrate(ResultSet rs, EntityMetadata meta, EntityKey key) throws Exception {
        Object entity = RowMapper.mapRow(rs, meta, registry);
        context.put(key, entity);
        // Put under the actual entity class too (important for polymorphic finds)
        EntityKey actualKey = new EntityKey(entity.getClass(), key.id());
        if (!actualKey.equals(key)) context.put(actualKey, entity);
        // Use the actual resolved class's metadata for loading associations
        EntityMetadata actualMeta = registry.contains(entity.getClass())
                ? registry.get(entity.getClass()) : meta;
        loadAssociations(entity, actualMeta);
        return entity;
    }

    /** Loads a single entity by PK via a fresh SELECT, then hydrates it. */
    private Object loadById(EntityMetadata meta, Object id, EntityKey key) throws Exception {
        String sql = CrudSqlBuilder.selectById(meta);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ParameterBinder.bind(ps, 1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return hydrate(rs, meta, key);
            }
        }
    }

    private static Object getIdValue(Object entity, EntityMetadata meta) throws Exception {
        Field idField = meta.getIdField();
        idField.setAccessible(true);
        return idField.get(entity);
    }

    private static void setField(Object target, Field field, Object value) throws Exception {
        field.setAccessible(true);
        field.set(target, value);
    }
}
