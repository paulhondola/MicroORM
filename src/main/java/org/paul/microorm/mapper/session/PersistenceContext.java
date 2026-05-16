package org.paul.microorm.mapper.session;

import java.util.HashMap;
import java.util.Map;

/**
 * First-level identity map scoped to one EntityManager session.
 * Guarantees that two find() calls for the same (class, id) return the same Java instance.
 */
public class PersistenceContext {

    private final Map<EntityKey, Object> identityMap = new HashMap<>();

    public boolean contains(EntityKey key) {
        return identityMap.containsKey(key);
    }

    public Object get(EntityKey key) {
        return identityMap.get(key);
    }

    public void put(EntityKey key, Object entity) {
        identityMap.put(key, entity);
    }

    public void remove(EntityKey key) {
        identityMap.remove(key);
    }

    public void clear() {
        identityMap.clear();
    }
}
