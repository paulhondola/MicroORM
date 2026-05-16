package org.paul.microorm.metadata;

import org.paul.microorm.exception.MetadataException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ListIterator;

/**
 * Holds all scanned EntityMetadata objects, resolves bidirectional relationships,
 * and builds inheritance trees. Entry point: MetadataRegistry.of(Class<?>...).
 */
public class MetadataRegistry {

    private final Map<Class<?>, EntityMetadata> cache = new LinkedHashMap<>();

    private MetadataRegistry() {}

    public static MetadataRegistry of(Class<?>... classes) {
        MetadataRegistry registry = new MetadataRegistry();
        for (Class<?> cls : classes) {
            EntityMetadata meta = MetadataScanner.scan(cls);
            registry.cache.put(cls, meta);
        }
        registry.resolveRelationships();
        registry.buildInheritanceTrees();
        return registry;
    }

    public EntityMetadata get(Class<?> entityClass) {
        EntityMetadata meta = cache.get(entityClass);
        if (meta == null) {
            throw new MetadataException("No metadata registered for: " + entityClass.getName());
        }
        return meta;
    }

    public Collection<EntityMetadata> all() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public boolean contains(Class<?> entityClass) {
        return cache.containsKey(entityClass);
    }

    // ── post-scan resolution ──────────────────────────────────────────────────

    private void resolveRelationships() {
        for (EntityMetadata meta : cache.values()) {
            var it = meta.getAssociations().listIterator();
            while (it.hasNext()) {
                AssociationMetadata assoc = it.next();
                if (assoc.kind() != AssociationKind.ONE_TO_MANY) continue;
                if (assoc.mappedBy() == null || assoc.mappedBy().isBlank()) continue;

                EntityMetadata targetMeta = cache.get(assoc.targetEntity());
                if (targetMeta == null) {
                    throw new MetadataException("OneToMany target not registered: " + assoc.targetEntity().getName());
                }

                AssociationMetadata owningAssoc = targetMeta.getAssociations().stream()
                        .filter(a -> a.kind() == AssociationKind.MANY_TO_ONE
                                && a.field().getName().equals(assoc.mappedBy()))
                        .findFirst()
                        .orElseThrow(() -> new MetadataException(
                                "mappedBy field '" + assoc.mappedBy() + "' not found as @ManyToOne on "
                                + assoc.targetEntity().getSimpleName()));

                it.set(new AssociationMetadata(
                        assoc.kind(), assoc.field(), assoc.targetEntity(),
                        assoc.mappedBy(), owningAssoc.fkColumn(), assoc.joinTableName(),
                        assoc.joinColumnNames(), assoc.inverseJoinColumnNames(),
                        assoc.cascade(), assoc.orphanRemoval()));
            }
        }
    }

    private void buildInheritanceTrees() {
        for (EntityMetadata rootMeta : cache.values()) {
            if (!rootMeta.isInheritanceRoot()) continue;

            for (EntityMetadata subMeta : cache.values()) {
                if (subMeta == rootMeta) continue;
                if (!rootMeta.getEntityClass().isAssignableFrom(subMeta.getEntityClass())) continue;

                String dv = subMeta.getDiscriminatorValue() != null
                        ? subMeta.getDiscriminatorValue()
                        : subMeta.getEntityClass().getSimpleName();
                rootMeta.getInheritance().registerSubtype(dv, subMeta.getEntityClass());
                subMeta.setInheritanceRoot(rootMeta.getEntityClass());
                // Subtypes share the root's physical table
                subMeta.setTableName(rootMeta.getTableName());
            }
        }
    }

    // ── diagnostics ───────────────────────────────────────────────────────────

    public void printTree() {
        System.out.println("=== MetadataRegistry ===");
        cache.values().forEach(meta -> {
            System.out.println("Entity : " + meta.getEntityClass().getSimpleName()
                    + " → table: " + meta.getTableName()
                    + (meta.getDiscriminatorValue() != null ? " [dtype=" + meta.getDiscriminatorValue() + "]" : ""));
            meta.getColumns().forEach(col -> System.out.println("  col      : " + col));
            meta.getAssociations().forEach(a ->
                    System.out.println("  assoc    : " + a));
            meta.getEmbeddedList().forEach(e ->
                    System.out.println("  embedded : " + e.field().getName()
                            + " (prefix=" + e.columnPrefix() + ")"));
            if (meta.isInheritanceRoot()) {
                System.out.println("  inh-root : strategy="
                        + meta.getInheritance().getStrategy()
                        + " dc=" + meta.getInheritance().getDiscriminatorColumn());
            }
        });
        System.out.println("========================");
    }
}
