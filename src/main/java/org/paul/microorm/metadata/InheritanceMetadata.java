package org.paul.microorm.metadata;

import org.paul.microorm.annotation.InheritanceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InheritanceMetadata {
    private final InheritanceType strategy;
    private final String discriminatorColumn;
    private final Class<?> root;
    private final List<Class<?>> subtypes = new ArrayList<>();
    /** discriminatorValue → concrete class */
    private final Map<String, Class<?>> discriminatorMap = new HashMap<>();

    public InheritanceMetadata(InheritanceType strategy, String discriminatorColumn, Class<?> root) {
        this.strategy = strategy;
        this.discriminatorColumn = discriminatorColumn;
        this.root = root;
    }

    public void registerSubtype(String discriminatorValue, Class<?> subtype) {
        subtypes.add(subtype);
        discriminatorMap.put(discriminatorValue, subtype);
    }

    /** Returns the concrete class for a discriminator value, falling back to root. */
    public Class<?> resolve(String discriminatorValue) {
        return discriminatorMap.getOrDefault(discriminatorValue, root);
    }

    public InheritanceType getStrategy() { return strategy; }
    public String getDiscriminatorColumn() { return discriminatorColumn; }
    public Class<?> getRoot() { return root; }
    public List<Class<?>> getSubtypes() { return Collections.unmodifiableList(subtypes); }
    public Map<String, Class<?>> getDiscriminatorMap() { return Collections.unmodifiableMap(discriminatorMap); }
}
