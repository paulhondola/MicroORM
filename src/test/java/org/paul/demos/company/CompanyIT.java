package org.paul.demos.company;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.paul.microorm.mapper.jdbc.MicroOrmProperties;
import org.paul.microorm.mapper.session.EntityManager;
import org.paul.microorm.mapper.session.EntityManagerFactory;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Company — inheritance + embedded + ManyToMany round-trip")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class CompanyIT {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        Properties props = new Properties();
        props.setProperty(MicroOrmProperties.JDBC_URL,
                "jdbc:h2:mem:company_test;DB_CLOSE_DELAY=-1");
        props.setProperty(MicroOrmProperties.JDBC_USER, "sa");
        props.setProperty(MicroOrmProperties.JDBC_PASSWORD, "");
        emf = EntityManagerFactory.create(props,
                Party.class, Customer.class, Employee.class,
                FreelanceEmployee.class, SalariedEmployee.class,
                Department.class);
    }

    @Test
    @DisplayName("1 — persist Customer (simple subtype) and find by Party")
    void persistAndFindCustomer() {
        EntityManager em = emf.createEntityManager();
        Customer cust = new Customer("Acme Corp", "GOOD");
        em.persist(cust);
        int custId = cust.getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        Party found = em2.find(Party.class, custId);
        assertThat(found).isInstanceOf(Customer.class);
        assertThat(found.getName()).isEqualTo("Acme Corp");
        assertThat(((Customer) found).getCreditState()).isEqualTo("GOOD");
        em2.close();
    }

    @Test
    @DisplayName("2 — persist SalariedEmployee with embedded Address")
    void persistSalariedEmployeeWithAddress() {
        EntityManager em = emf.createEntityManager();
        SalariedEmployee emp = new SalariedEmployee("John Doe", "123-45-6789", 5000.0);
        emp.setHomeAddress(new Address("123 Main St", "Springfield", "62701", "IL"));
        em.persist(emp);
        int empId = emp.getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        Party found = em2.find(Party.class, empId);
        assertThat(found).isInstanceOf(SalariedEmployee.class);

        SalariedEmployee salFound = (SalariedEmployee) found;
        assertThat(salFound.getName()).isEqualTo("John Doe");
        assertThat(salFound.getMonthlySalary()).isEqualTo(5000.0);
        assertThat(salFound.getHomeAddress()).isNotNull();
        assertThat(salFound.getHomeAddress().getCity()).isEqualTo("Springfield");
        em2.close();
    }

    @Test
    @DisplayName("3 — find(SalariedEmployee.class, id) works directly")
    void findDirectSubtype() {
        EntityManager em = emf.createEntityManager();
        SalariedEmployee emp = new SalariedEmployee("Jane Smith", "987-65-4321", 6000.0);
        em.persist(emp);
        int empId = emp.getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        SalariedEmployee found = em2.find(SalariedEmployee.class, empId);
        assertThat(found).isNotNull();
        assertThat(found.getMonthlySalary()).isEqualTo(6000.0);
        em2.close();
    }

    @Test
    @DisplayName("4 — persist Employee with ManyToMany departments")
    void persistEmployeeWithDepartments() {
        EntityManager em = emf.createEntityManager();
        Department eng = new Department("Engineering");
        Department hr  = new Department("HR");
        em.persist(eng);
        em.persist(hr);

        SalariedEmployee emp = new SalariedEmployee("Mike Lee", "555-55-5555", 7000.0);
        emp.addDepartment(eng);
        emp.addDepartment(hr);
        em.persist(emp);
        int empId = emp.getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        Party found = em2.find(Party.class, empId);
        assertThat(found).isInstanceOf(SalariedEmployee.class);
        SalariedEmployee salFound = (SalariedEmployee) found;
        assertThat(salFound.getDepartments()).hasSize(2);
        assertThat(salFound.getDepartments())
                .extracting(Department::getName)
                .containsExactlyInAnyOrder("Engineering", "HR");
        em2.close();
    }

    @Test
    @DisplayName("5 — remove Employee cascades junction rows (no FK violation)")
    void removeEmployeeCleansJunction() {
        EntityManager em = emf.createEntityManager();
        Department finance = new Department("Finance");
        em.persist(finance);

        FreelanceEmployee freelancer = new FreelanceEmployee("Tom Green", "444-44-4444", 80.0);
        freelancer.addDepartment(finance);
        em.persist(freelancer);
        int eid = freelancer.getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        Party found = em2.find(Party.class, eid);
        em2.remove(found);
        em2.close();

        EntityManager em3 = emf.createEntityManager();
        assertThat(em3.find(Party.class, eid)).isNull();
        em3.close();
    }
}
