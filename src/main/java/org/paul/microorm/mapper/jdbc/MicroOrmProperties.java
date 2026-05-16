package org.paul.microorm.mapper.jdbc;

/** Property keys used when constructing an EntityManagerFactory. */
public final class MicroOrmProperties {

    public static final String JDBC_URL    = "microorm.jdbc.url";
    public static final String JDBC_USER   = "microorm.jdbc.user";
    public static final String JDBC_PASSWORD = "microorm.jdbc.password";
    /** Accepted value: "create-drop" (default). */
    public static final String SCHEMA_MODE = "microorm.schema.mode";

    public static final String SCHEMA_MODE_CREATE_DROP = "create-drop";

    private MicroOrmProperties() {}
}
