package org.paul.microorm.mapper.session;

import org.paul.microorm.exception.MicroOrmException;

import java.sql.Connection;
import java.sql.SQLException;

/** Wraps JDBC connection autoCommit state for explicit transaction demarcation. */
public class EntityTransaction {

    private final Connection connection;
    private boolean active = false;

    public EntityTransaction(Connection connection) {
        this.connection = connection;
    }

    public void begin() {
        try {
            connection.setAutoCommit(false);
            active = true;
        } catch (SQLException e) {
            throw new MicroOrmException("Cannot begin transaction", e);
        }
    }

    public void commit() {
        try {
            connection.commit();
            active = false;
        } catch (SQLException e) {
            throw new MicroOrmException("Commit failed", e);
        }
    }

    public void rollback() {
        try {
            connection.rollback();
            active = false;
        } catch (SQLException e) {
            throw new MicroOrmException("Rollback failed", e);
        }
    }

    public boolean isActive() { return active; }
}
