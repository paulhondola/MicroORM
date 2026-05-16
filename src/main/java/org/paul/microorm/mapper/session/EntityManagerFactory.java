package org.paul.microorm.mapper.session;

import org.paul.microorm.generator.SchemaGenerator;
import org.paul.microorm.mapper.jdbc.ConnectionProvider;
import org.paul.microorm.mapper.jdbc.MicroOrmProperties;
import org.paul.microorm.metadata.MetadataRegistry;

import java.util.Properties;

/**
 * Heavy-weight factory — created once per database / entity set.
 * Scans metadata, optionally runs schema generation, then vends lightweight EntityManagers.
 */
public class EntityManagerFactory {

    private final MetadataRegistry registry;
    private final ConnectionProvider connectionProvider;

    private EntityManagerFactory(MetadataRegistry registry, ConnectionProvider connectionProvider) {
        this.registry = registry;
        this.connectionProvider = connectionProvider;
    }

    /**
     * Creates the factory, scans the supplied entity classes, and (if schema.mode=create-drop)
     * drops and recreates the schema.
     */
    public static EntityManagerFactory create(Properties properties, Class<?>... entityClasses) {
        MetadataRegistry registry = MetadataRegistry.of(entityClasses);
        ConnectionProvider connectionProvider = new ConnectionProvider(properties);

        EntityManagerFactory emf = new EntityManagerFactory(registry, connectionProvider);

        String schemaMode = properties.getProperty(
                MicroOrmProperties.SCHEMA_MODE, MicroOrmProperties.SCHEMA_MODE_CREATE_DROP);
        if (MicroOrmProperties.SCHEMA_MODE_CREATE_DROP.equals(schemaMode)) {
            new SchemaGenerator(registry, connectionProvider).execute();
        }

        return emf;
    }

    /** Creates a new lightweight EntityManager backed by a fresh JDBC connection. */
    public EntityManager createEntityManager() {
        return new EntityManagerImpl(registry, connectionProvider);
    }

    public MetadataRegistry getRegistry() { return registry; }

    public void close() {
        // nothing to release — no connection pool
    }
}
