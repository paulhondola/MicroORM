package org.paul.microorm.mapper.session;

/**
 * Course canon "Mapper API": persists and retrieves objects.
 * Mirrors the JPA EntityManager surface referenced in the course slides.
 */
public interface EntityManager {

    /**
     * Persists a new entity (and cascades to associations according to CascadeType).
     * Sets the generated PK value back onto the entity after INSERT.
     */
    void persist(Object entity);

    /**
     * Finds an entity by primary key; returns null if not found.
     * Result is cached in the PersistenceContext for the lifetime of this EM.
     */
    <T> T find(Class<T> entityClass, Object primaryKey);

    /**
     * Merges a detached entity state back to the database (full-column UPDATE).
     */
    <T> T merge(T entity);

    /**
     * Removes an entity and cascades orphanRemoval to collections.
     */
    void remove(Object entity);

    EntityTransaction getTransaction();

    void close();
}
