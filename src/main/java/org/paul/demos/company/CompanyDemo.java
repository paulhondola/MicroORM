package org.paul.demos.company;

import org.paul.microorm.mapper.jdbc.MicroOrmProperties;
import org.paul.microorm.mapper.session.EntityManager;
import org.paul.microorm.mapper.session.EntityManagerFactory;

import java.util.Properties;

/**
 * Demo #2 — Company model.
 * Exercises all three course categories in one scenario:
 *   - SINGLE_TABLE inheritance (Party hierarchy)
 *   - @Embedded (Address inlined into party table)
 *   - @ManyToMany (Employee ↔ Department via junction table)
 */
public class CompanyDemo {

    static void main() {
        Properties props = new Properties();
        props.setProperty(MicroOrmProperties.JDBC_URL, "jdbc:h2:./data/company");
        props.setProperty(MicroOrmProperties.SCHEMA_MODE, MicroOrmProperties.SCHEMA_MODE_CREATE_DROP);

        EntityManagerFactory emf = EntityManagerFactory.create(props,
                Party.class, Customer.class, Employee.class,
                FreelanceEmployee.class, SalariedEmployee.class,
                Department.class);

        // print the generated schema for inspection
        emf.getRegistry().printTree();

        // ── persist ──────────────────────────────────────────────────────────
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Department engineering = new Department("Engineering");
        Department product = new Department("Product");
        em.persist(engineering);
        em.persist(product);

        SalariedEmployee bob = new SalariedEmployee("Bob", "123-45-6789", 5000.0);
        bob.setHomeAddress(new Address("123 Main St", "Springfield", "12345", "IL"));
        bob.addDepartment(engineering);
        bob.addDepartment(product);
        em.persist(bob);

        em.getTransaction().commit();
        System.out.println("Persisted: " + bob);

        int bobId = bob.getId();
        em.close();

        // ── find ─────────────────────────────────────────────────────────────
        em = emf.createEntityManager();
        Party found = em.find(Party.class, bobId);
        System.out.println("Found as Party:  " + found);
        System.out.println("Runtime type:    " + found.getClass().getSimpleName()); // must be SalariedEmployee

        SalariedEmployee sal = (SalariedEmployee) found;
        System.out.println("Monthly salary:  " + sal.getMonthlySalary());
        System.out.println("Home address:    " + sal.getHomeAddress());
        System.out.println("Departments:     " + sal.getDepartments());

        em.close();
        emf.close();
    }
}
