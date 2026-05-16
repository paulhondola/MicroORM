package org.paul.microorm.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.paul.microorm.exception.MetadataException;
import org.paul.demos.personscars.Car;
import org.paul.demos.personscars.Person;
import org.paul.demos.company.Employee;
import org.paul.demos.company.SalariedEmployee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MetadataScanner")
class MetadataScannerTest {

    @Nested
    @DisplayName("toSnakeCase")
    class SnakeCase {

        @Test void simpleWord() {
            assertThat(MetadataScanner.toSnakeCase("Person")).isEqualTo("person");
        }

        @Test void camelCase() {
            assertThat(MetadataScanner.toSnakeCase("SalariedEmployee")).isEqualTo("salaried_employee");
        }

        @Test void alreadyLower() {
            assertThat(MetadataScanner.toSnakeCase("car")).isEqualTo("car");
        }

        @Test void multiWordCamel() {
            assertThat(MetadataScanner.toSnakeCase("homeAddress")).isEqualTo("home_address");
        }
    }

    @Nested
    @DisplayName("scan(Person)")
    class ScanPerson {

        private final EntityMetadata meta = MetadataScanner.scan(Person.class);

        @Test void tableName() {
            assertThat(meta.getTableName()).isEqualTo("person");
        }

        @Test void idField() {
            assertThat(meta.getIdField().getName()).isEqualTo("id");
        }

        @Test void generatedId() {
            assertThat(meta.isGeneratedId()).isTrue();
        }

        @Test void hasNameColumn() {
            assertThat(meta.getColumns())
                    .extracting(ColumnMetadata::columnName)
                    .contains("name");
        }

        @Test void idNotInColumns() {
            assertThat(meta.getColumns())
                    .extracting(ColumnMetadata::columnName)
                    .doesNotContain("id");
        }

        @Test void oneToManyCarsMappedBy() {
            AssociationMetadata assoc = meta.getAssociations().stream()
                    .filter(a -> a.kind() == AssociationKind.ONE_TO_MANY)
                    .findFirst()
                    .orElseThrow();
            assertThat(assoc.targetEntity()).isEqualTo(Car.class);
            assertThat(assoc.mappedBy()).isEqualTo("person");
        }
    }

    @Nested
    @DisplayName("scan(Car)")
    class ScanCar {

        private final EntityMetadata meta = MetadataScanner.scan(Car.class);

        @Test void tableName() {
            assertThat(meta.getTableName()).isEqualTo("car");
        }

        @Test void manyToOnePersonFk() {
            AssociationMetadata assoc = meta.getAssociations().stream()
                    .filter(a -> a.kind() == AssociationKind.MANY_TO_ONE)
                    .findFirst()
                    .orElseThrow();
            assertThat(assoc.targetEntity()).isEqualTo(Person.class);
            assertThat(assoc.fkColumn()).isEqualTo("person_id");
        }
    }

    @Nested
    @DisplayName("scan(Employee) — embedded + ManyToMany")
    class ScanEmployee {

        private final EntityMetadata meta = MetadataScanner.scan(Employee.class);

        @Test void hasEmbeddedHomeAddress() {
            assertThat(meta.getEmbeddedList())
                    .extracting(emb -> emb.field().getName())
                    .contains("homeAddress");
        }

        @Test void embeddedColumnsHavePrefix() {
            EmbeddedMetadata emb = meta.getEmbeddedList().stream()
                    .filter(e -> e.field().getName().equals("homeAddress"))
                    .findFirst()
                    .orElseThrow();
            assertThat(emb.columns())
                    .extracting(ColumnMetadata::columnName)
                    .allMatch(name -> name.startsWith("home_address_"));
        }

        @Test void hasManyToManyDepartments() {
            AssociationMetadata assoc = meta.getAssociations().stream()
                    .filter(a -> a.kind() == AssociationKind.MANY_TO_MANY)
                    .findFirst()
                    .orElseThrow();
            assertThat(assoc.joinTableName()).isEqualTo("employee_department");
            assertThat(assoc.joinColumnNames()).contains("employee_id");
            assertThat(assoc.inverseJoinColumnNames()).contains("department_id");
        }
    }

    @Nested
    @DisplayName("scan(SalariedEmployee) — inheritance subtype")
    class ScanSalariedEmployee {

        private final EntityMetadata meta = MetadataScanner.scan(SalariedEmployee.class);

        @Test void discriminatorValue() {
            assertThat(meta.getDiscriminatorValue()).isEqualTo("SAL");
        }

        @Test void hasOwnColumn() {
            assertThat(meta.getColumns())
                    .extracting(ColumnMetadata::columnName)
                    .contains("monthly_salary");
        }

        @Test void inheritsParentColumns() {
            assertThat(meta.getColumns())
                    .extracting(ColumnMetadata::columnName)
                    .contains("name", "soc_security_no");
        }
    }

    @Test
    @DisplayName("scan non-@Entity class throws MetadataException")
    void nonEntityThrows() {
        assertThatThrownBy(() -> MetadataScanner.scan(String.class))
                .isInstanceOf(MetadataException.class)
                .hasMessageContaining("Not @Entity");
    }
}
