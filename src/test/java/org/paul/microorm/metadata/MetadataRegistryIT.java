package org.paul.microorm.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.paul.demos.personscars.Car;
import org.paul.demos.personscars.Person;
import org.paul.demos.company.Customer;
import org.paul.demos.company.Employee;
import org.paul.demos.company.FreelanceEmployee;
import org.paul.demos.company.Party;
import org.paul.demos.company.SalariedEmployee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MetadataRegistry — resolution passes")
class MetadataRegistryIT {

    @Nested
    @DisplayName("resolveRelationships — Person ↔ Car")
    class ResolveRelationships {

        private final MetadataRegistry registry = MetadataRegistry.of(Person.class, Car.class);

        @Test
        @DisplayName("OneToMany fkColumn is resolved from owning ManyToOne side")
        void oneToManyFkColumnResolved() {
            EntityMetadata personMeta = registry.get(Person.class);
            AssociationMetadata oneToMany = personMeta.getAssociations().stream()
                    .filter(a -> a.kind() == AssociationKind.ONE_TO_MANY)
                    .findFirst()
                    .orElseThrow();
            assertThat(oneToMany.fkColumn()).isEqualTo("person_id");
        }

        @Test
        @DisplayName("Car ManyToOne retains fkColumn person_id")
        void carFkColumn() {
            EntityMetadata carMeta = registry.get(Car.class);
            AssociationMetadata manyToOne = carMeta.getAssociations().stream()
                    .filter(a -> a.kind() == AssociationKind.MANY_TO_ONE)
                    .findFirst()
                    .orElseThrow();
            assertThat(manyToOne.fkColumn()).isEqualTo("person_id");
        }

        @Test
        @DisplayName("unregistered target throws MetadataException")
        void unregisteredTargetThrows() {
            assertThatThrownBy(() -> MetadataRegistry.of(Person.class))
                    .isInstanceOf(org.paul.microorm.exception.MetadataException.class)
                    .hasMessageContaining("OneToMany target not registered");
        }
    }

    @Nested
    @DisplayName("buildInheritanceTrees — Party hierarchy")
    class BuildInheritanceTrees {

        private final MetadataRegistry registry = MetadataRegistry.of(
                Party.class, Customer.class, Employee.class,
                FreelanceEmployee.class, SalariedEmployee.class);

        @Test
        @DisplayName("Party is inheritance root")
        void partyIsRoot() {
            EntityMetadata partyMeta = registry.get(Party.class);
            assertThat(partyMeta.isInheritanceRoot()).isTrue();
            assertThat(partyMeta.getInheritance().getDiscriminatorColumn()).isEqualTo("party_type");
        }

        @Test
        @DisplayName("SalariedEmployee inheritsRoot points to Party")
        void salariedEmployeeInheritsRoot() {
            EntityMetadata salMeta = registry.get(SalariedEmployee.class);
            assertThat(salMeta.getInheritanceRoot()).isEqualTo(Party.class);
        }

        @Test
        @DisplayName("subtypes share the root table name 'party'")
        void subtypesShareRootTableName() {
            assertThat(registry.get(Employee.class).getTableName()).isEqualTo("party");
            assertThat(registry.get(SalariedEmployee.class).getTableName()).isEqualTo("party");
            assertThat(registry.get(Customer.class).getTableName()).isEqualTo("party");
        }

        @Test
        @DisplayName("discriminator map contains all subtypes")
        void discriminatorMapPopulated() {
            InheritanceMetadata inh = registry.get(Party.class).getInheritance();
            assertThat(inh.resolve("SAL")).isEqualTo(SalariedEmployee.class);
            assertThat(inh.resolve("EMP")).isEqualTo(Employee.class);
        }
    }
}
