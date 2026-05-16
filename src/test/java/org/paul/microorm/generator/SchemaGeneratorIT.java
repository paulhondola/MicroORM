package org.paul.microorm.generator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.paul.demos.company.Customer;
import org.paul.demos.company.Department;
import org.paul.demos.company.Employee;
import org.paul.demos.company.FreelanceEmployee;
import org.paul.demos.company.Party;
import org.paul.demos.company.SalariedEmployee;
import org.paul.demos.personscars.Car;
import org.paul.demos.personscars.Person;
import org.paul.microorm.mapper.jdbc.ConnectionProvider;
import org.paul.microorm.mapper.jdbc.MicroOrmProperties;
import org.paul.microorm.metadata.MetadataRegistry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SchemaGenerator — DDL against H2")
class SchemaGeneratorIT {

    private static ConnectionProvider connectionProvider;

    @BeforeAll
    static void setUp() {
        Properties props = new Properties();
        props.setProperty(MicroOrmProperties.JDBC_URL,
                "jdbc:h2:mem:schema_gen_test;DB_CLOSE_DELAY=-1");
        props.setProperty(MicroOrmProperties.JDBC_USER, "sa");
        props.setProperty(MicroOrmProperties.JDBC_PASSWORD, "");
        connectionProvider = new ConnectionProvider(props);

        MetadataRegistry registry = MetadataRegistry.of(
                Person.class, Car.class,
                Party.class, Customer.class, Employee.class,
                FreelanceEmployee.class, SalariedEmployee.class,
                Department.class);
        new SchemaGenerator(registry, connectionProvider).execute();
    }

    private boolean tableExists(String tableName) throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    @Test
    @DisplayName("person table created")
    void personTableCreated() throws Exception {
        assertThat(tableExists("PERSON")).isTrue();
    }

    @Test
    @DisplayName("car table created")
    void carTableCreated() throws Exception {
        assertThat(tableExists("CAR")).isTrue();
    }

    @Test
    @DisplayName("single party table created (not separate per subtype)")
    void partyTableCreated() throws Exception {
        assertThat(tableExists("PARTY")).isTrue();
        assertThat(tableExists("EMPLOYEE")).isFalse();
        assertThat(tableExists("SALARIED_EMPLOYEE")).isFalse();
    }

    @Test
    @DisplayName("junction table employee_department created")
    void junctionTableCreated() throws Exception {
        assertThat(tableExists("EMPLOYEE_DEPARTMENT")).isTrue();
    }

    @Test
    @DisplayName("department table created")
    void departmentTableCreated() throws Exception {
        assertThat(tableExists("DEPARTMENT")).isTrue();
    }

    @Test
    @DisplayName("party table has discriminator column party_type")
    void partyTableHasDiscriminatorColumn() throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(
                     null, null, "PARTY", "PARTY_TYPE")) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    @DisplayName("party table has embedded address columns")
    void partyTableHasEmbeddedAddressColumns() throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             ResultSet rs = conn.getMetaData().getColumns(
                     null, null, "PARTY", "HOME_ADDRESS_CITY")) {
            assertThat(rs.next()).isTrue();
        }
    }
}
