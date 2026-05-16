package org.paul.microorm.mapper.session;

/**
 * Composite identity-map key: (entity class, primary key value).
 * Used by PersistenceContext to cache and look up already-hydrated entities,
 * which breaks bidirectional association loading cycles.
 */
public record EntityKey(Class<?> entityClass, Object id) {}
