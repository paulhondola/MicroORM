package org.paul.demos.personscars;

import org.paul.microorm.mapper.jdbc.MicroOrmProperties;
import org.paul.microorm.mapper.session.EntityManager;
import org.paul.microorm.mapper.session.EntityManagerFactory;

import java.util.Properties;

/**
 * Demo #1 — Persons and Cars.
 * Exercises @OneToMany / @ManyToOne with cascade ALL and orphanRemoval.
 * Expected output mirrors the JPAHibernatePersonsCars reference.
 */
public class PersonsCarsDemo {

    static void main() {
        Properties props = new Properties();
        props.setProperty(MicroOrmProperties.JDBC_URL, "jdbc:h2:./data/personscars");
        props.setProperty(MicroOrmProperties.SCHEMA_MODE, MicroOrmProperties.SCHEMA_MODE_CREATE_DROP);

        EntityManagerFactory emf = EntityManagerFactory.create(props, Person.class, Car.class);

        // ── persist ──────────────────────────────────────────────────────────
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Person alice = new Person("Alice");
        alice.addCar(new Car("Tesla Model S"));
        alice.addCar(new Car("BMW i3"));
        em.persist(alice);

        em.getTransaction().commit();
        System.out.println("Persisted: " + alice);

        int aliceId = alice.getId();
        em.close();

        // ── find ─────────────────────────────────────────────────────────────
        em = emf.createEntityManager();
        Person found = em.find(Person.class, aliceId);
        System.out.println("Found: " + found);
        System.out.println("Cars: " + found.getCars());
        System.out.println("Identity check (car.person == person): "
                + (found.getCars().get(0).getPerson() == found));

        em.close();
        emf.close();
    }
}
