package org.paul.microorm.generator;

import org.paul.microorm.mapper.jdbc.ConnectionProvider;
import org.paul.microorm.metadata.AssociationKind;
import org.paul.microorm.metadata.AssociationMetadata;
import org.paul.microorm.metadata.EntityMetadata;
import org.paul.microorm.metadata.MetadataRegistry;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Course canon "Generator": turns EntityMetadata into DDL and executes it.
 * Currently supports schema mode "create-drop" only.
 */
public class SchemaGenerator {

    private final MetadataRegistry registry;
    private final ConnectionProvider connectionProvider;

    public SchemaGenerator(MetadataRegistry registry, ConnectionProvider connectionProvider) {
        this.registry = registry;
        this.connectionProvider = connectionProvider;
    }

    /** Returns the full list of DDL statements in execution order. */
    public List<String> buildDdl() {
        // Collect table-producing entities (non-subtypes only).
        List<EntityMetadata> tableEntities = new ArrayList<>();
        for (EntityMetadata meta : registry.all()) {
            boolean isSubtype = meta.getInheritanceRoot() != null
                    && !meta.getEntityClass().equals(meta.getInheritanceRoot());
            if (!isSubtype) tableEntities.add(meta);
        }

        // Topological sort: parent tables (referenced by @ManyToOne FK) before child tables.
        // Creates go in topo order; drops go in reverse (children before parents).
        List<EntityMetadata> sorted = topoSort(tableEntities);

        List<String> baseCreates = new ArrayList<>();
        List<String> baseDrops   = new ArrayList<>();
        for (EntityMetadata meta : sorted) {
            List<String> pair = meta.isInheritanceRoot()
                    ? SingleTableInheritanceDdl.generateFor(meta, registry)
                    : generateFlatTable(meta);
            baseCreates.add(pair.get(1));
            baseDrops.add(0, pair.get(0)); // prepend → reverse (children dropped first)
        }

        // Junction tables reference base tables via FK — drop before base, create after base.
        // Deduplicate: subtypes inheriting the same @ManyToMany would emit the same junction.
        List<String> junctionDrops   = new ArrayList<>();
        List<String> junctionCreates = new ArrayList<>();
        Set<String>  seenJunctions   = new LinkedHashSet<>();

        for (EntityMetadata meta : registry.all()) {
            List<String> stmts = JoinTableResolver.junctionTableDdl(meta, registry);
            for (int i = 0; i < stmts.size(); i += 2) {
                String drop      = stmts.get(i);
                String create    = stmts.get(i + 1);
                String tableName = drop.replace("DROP TABLE IF EXISTS ", "").replace(";", "").trim();
                if (seenJunctions.add(tableName)) {
                    junctionDrops.add(drop);
                    junctionCreates.add(create);
                }
            }
        }

        // Final order: junction drops → base drops → base creates → junction creates.
        List<String> ddl = new ArrayList<>(junctionDrops);
        ddl.addAll(baseDrops);
        ddl.addAll(baseCreates);
        ddl.addAll(junctionCreates);
        return ddl;
    }

    /**
     * Topological sort of table-producing entities using Kahn's algorithm.
     * An entity A depends on entity B when A has a @ManyToOne association to B
     * (meaning B's table must exist before A's table can be created).
     * Returns entities in creation order (no-dependency entities first).
     */
    private List<EntityMetadata> topoSort(List<EntityMetadata> entities) {
        Map<Class<?>, EntityMetadata> byClass = new HashMap<>();
        for (EntityMetadata meta : entities) {
            byClass.put(meta.getEntityClass(), meta);
        }

        Map<EntityMetadata, Integer>       inDegree   = new LinkedHashMap<>();
        Map<EntityMetadata, List<EntityMetadata>> dependents = new HashMap<>();
        for (EntityMetadata meta : entities) {
            inDegree.put(meta, 0);
            dependents.put(meta, new ArrayList<>());
        }

        for (EntityMetadata meta : entities) {
            for (AssociationMetadata assoc : meta.getAssociations()) {
                if (assoc.kind() != AssociationKind.MANY_TO_ONE) continue;
                EntityMetadata target = byClass.get(assoc.targetEntity());
                if (target == null || target.equals(meta)) continue;
                inDegree.merge(meta, 1, Integer::sum);
                dependents.get(target).add(meta);
            }
        }

        Deque<EntityMetadata> queue = new ArrayDeque<>();
        for (Map.Entry<EntityMetadata, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<EntityMetadata> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            EntityMetadata curr = queue.poll();
            sorted.add(curr);
            for (EntityMetadata dep : dependents.get(curr)) {
                if (inDegree.merge(dep, -1, Integer::sum) == 0) queue.add(dep);
            }
        }

        return sorted;
    }

    /** Generates DROP + CREATE for a plain (non-inheritance) entity table. */
    private List<String> generateFlatTable(EntityMetadata meta) {
        List<String> cols = new ArrayList<>();

        cols.add(DdlBuilder.primaryKeyColumn(meta));

        for (var col : meta.getColumns()) {
            cols.add(DdlBuilder.columnDef(col));
        }

        cols.addAll(EmbeddedFlattener.columnDefsFor(meta));
        cols.addAll(ForeignKeyResolver.fkLinesFor(meta, registry));
        cols.add(DdlBuilder.primaryKeyConstraint(meta.getIdField().getName()));

        List<String> ddl = new ArrayList<>();
        ddl.add(DdlBuilder.dropTable(meta.getTableName()));
        ddl.add(DdlBuilder.createTable(meta.getTableName(), cols));
        return ddl;
    }

    /** Prints all DDL statements to stdout (useful for debugging). */
    public void printSchema() {
        System.out.println("=== Generated DDL ===");
        buildDdl().forEach(System.out::println);
        System.out.println("=====================");
    }

    /** Executes the DDL against the configured H2 connection. */
    public void execute() {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : buildDdl()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new org.paul.microorm.exception.SchemaException("DDL execution failed", e);
        }
    }
}
