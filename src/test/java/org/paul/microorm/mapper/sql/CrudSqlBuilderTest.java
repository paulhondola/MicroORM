package org.paul.microorm.mapper.sql;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.paul.microorm.metadata.EntityMetadata;
import org.paul.microorm.metadata.MetadataRegistry;
import org.paul.demos.personscars.Car;
import org.paul.demos.personscars.Person;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CrudSqlBuilder")
class CrudSqlBuilderTest {

    private static MetadataRegistry registry;
    private static EntityMetadata personMeta;
    private static EntityMetadata carMeta;

    @BeforeAll
    static void setup() {
        registry = MetadataRegistry.of(Person.class, Car.class);
        personMeta = registry.get(Person.class);
        carMeta = registry.get(Car.class);
    }

    @Nested
    @DisplayName("insert")
    class Insert {

        @Test
        @DisplayName("uses only ? placeholders — no value concatenation")
        void onlyPlaceholders() {
            String sql = CrudSqlBuilder.insert(personMeta);
            String valuesPart = sql.substring(sql.indexOf("VALUES"));
            assertThat(valuesPart).doesNotContainPattern("['\"]");
            assertThat(valuesPart).contains("?");
        }

        @Test
        @DisplayName("INSERT structure is correct")
        void insertStructure() {
            String sql = CrudSqlBuilder.insert(personMeta).replace("%TABLE%", "person");
            assertThat(sql).startsWith("INSERT INTO person (");
            assertThat(sql).contains("VALUES (");
            assertThat(sql).contains("name");
        }

        @Test
        @DisplayName("id column excluded from INSERT for generated PK")
        void idExcluded() {
            String sql = CrudSqlBuilder.insert(personMeta);
            String colsPart = sql.substring(sql.indexOf("(") + 1, sql.indexOf(")"));
            assertThat(colsPart).doesNotContain("id");
        }

        @Test
        @DisplayName("FK column included for @ManyToOne side")
        void fkColumnIncluded() {
            String sql = CrudSqlBuilder.insert(carMeta);
            assertThat(sql).contains("person_id");
        }

        @Test
        @DisplayName("discriminator column prepended when provided")
        void discriminatorPrepended() {
            String sql = CrudSqlBuilder.insert(personMeta, "party_type").replace("%TABLE%", "party");
            int parenOpen = sql.indexOf("(");
            int parenClose = sql.indexOf(")");
            String cols = sql.substring(parenOpen + 1, parenClose);
            assertThat(cols.trim()).startsWith("party_type");
        }
    }

    @Nested
    @DisplayName("selectById")
    class SelectById {

        @Test
        @DisplayName("generates correct WHERE clause")
        void whereClause() {
            String sql = CrudSqlBuilder.selectById(personMeta);
            assertThat(sql).isEqualTo("SELECT * FROM person WHERE id = ?");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("UPDATE has SET and WHERE with ? placeholders")
        void updateStructure() {
            String sql = CrudSqlBuilder.update(personMeta);
            assertThat(sql).startsWith("UPDATE person SET ");
            assertThat(sql).contains("WHERE id = ?");
            assertThat(sql).contains("name = ?");
            assertThat(sql).doesNotContain("'");
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("DELETE uses parameterized WHERE")
        void deleteStructure() {
            String sql = CrudSqlBuilder.deleteById(personMeta);
            assertThat(sql).isEqualTo("DELETE FROM person WHERE id = ?");
        }
    }

    @Nested
    @DisplayName("junction table helpers")
    class JunctionHelpers {

        @Test
        @DisplayName("selectJunctionByOwner selects target FK")
        void selectJunction() {
            String sql = CrudSqlBuilder.selectJunctionByOwner("employee_department", "employee_id", "department_id");
            assertThat(sql).isEqualTo(
                    "SELECT department_id FROM employee_department WHERE employee_id = ?");
        }

        @Test
        @DisplayName("insertJunction has two placeholders")
        void insertJunction() {
            String sql = CrudSqlBuilder.insertJunction("employee_department", "employee_id", "department_id");
            assertThat(sql).isEqualTo(
                    "INSERT INTO employee_department (employee_id, department_id) VALUES (?, ?)");
        }
    }
}
