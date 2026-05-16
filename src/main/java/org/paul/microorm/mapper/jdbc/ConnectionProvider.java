package org.paul.microorm.mapper.jdbc;

import org.paul.microorm.exception.MicroOrmException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Thin wrapper around DriverManager.getConnection.
 * Creates a new physical connection per call — no pooling.
 */
public class ConnectionProvider {

    private final String url;
    private final String user;
    private final String password;

    public ConnectionProvider(Properties properties) {
        this.url      = properties.getProperty(MicroOrmProperties.JDBC_URL, "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        this.user     = properties.getProperty(MicroOrmProperties.JDBC_USER, "sa");
        this.password = properties.getProperty(MicroOrmProperties.JDBC_PASSWORD, "");
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new MicroOrmException("Cannot open JDBC connection to " + url, e);
        }
    }

    public String getUrl() { return url; }
}
