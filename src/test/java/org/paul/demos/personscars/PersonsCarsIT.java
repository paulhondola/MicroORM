package org.paul.demos.personscars;

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

@DisplayName("Persons-Cars — full CRUD round-trip")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class PersonsCarsIT {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        Properties props = new Properties();
        props.setProperty(MicroOrmProperties.JDBC_URL,
                "jdbc:h2:mem:persons_cars_test;DB_CLOSE_DELAY=-1");
        props.setProperty(MicroOrmProperties.JDBC_USER, "sa");
        props.setProperty(MicroOrmProperties.JDBC_PASSWORD, "");
        emf = EntityManagerFactory.create(props, Person.class, Car.class);
    }

    @Test
    @DisplayName("1 — persist person with cascaded cars")
    void persistPersonWithCars() {
        EntityManager em = emf.createEntityManager();
        Person alice = new Person("Alice");
        alice.addCar(new Car("Tesla Model 3"));
        alice.addCar(new Car("BMW X5"));

        em.persist(alice);

        assertThat(alice.getId()).isGreaterThan(0);
        assertThat(alice.getCars().get(0).getId()).isGreaterThan(0);
        assertThat(alice.getCars().get(1).getId()).isGreaterThan(0);
        em.close();
    }

    @Test
    @DisplayName("2 — find person loads associated cars eagerly")
    void findPersonLoadsEagerCars() {
        EntityManager em = emf.createEntityManager();
        Person alice = new Person("Alice");
        alice.addCar(new Car("Tesla Model 3"));
        em.persist(alice);
        int aliceId = alice.getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        Person found = em2.find(Person.class, aliceId);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Alice");
        assertThat(found.getCars()).hasSize(1);
        assertThat(found.getCars().get(0).getModel()).isEqualTo("Tesla Model 3");
        em2.close();
    }

    @Test
    @DisplayName("3 — bidirectional identity: car.person == person")
    void bidirectionalIdentityMap() {
        EntityManager em = emf.createEntityManager();
        Person bob = new Person("Bob");
        bob.addCar(new Car("Audi A4"));
        em.persist(bob);
        int bobId = bob.getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        Person found = em2.find(Person.class, bobId);

        assertThat(found.getCars().get(0).getPerson()).isSameAs(found);
        em2.close();
    }

    @Test
    @DisplayName("4 — merge updates person name in database")
    void mergeUpdatesPerson() {
        EntityManager em = emf.createEntityManager();
        Person carol = new Person("Carol");
        em.persist(carol);
        int carolId = carol.getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        Person found = em2.find(Person.class, carolId);
        found.setName("Caroline");
        em2.merge(found);
        em2.close();

        EntityManager em3 = emf.createEntityManager();
        Person updated = em3.find(Person.class, carolId);
        assertThat(updated.getName()).isEqualTo("Caroline");
        em3.close();
    }

    @Test
    @DisplayName("5 — remove person cascades orphan cars")
    void removePersonCascadesCars() {
        EntityManager em = emf.createEntityManager();
        Person dave = new Person("Dave");
        dave.addCar(new Car("Ford Mustang"));
        em.persist(dave);
        int daveId = dave.getId();
        int carId  = dave.getCars().get(0).getId();
        em.close();

        EntityManager em2 = emf.createEntityManager();
        Person found = em2.find(Person.class, daveId);
        em2.remove(found);
        em2.close();

        EntityManager em3 = emf.createEntityManager();
        assertThat(em3.find(Person.class, daveId)).isNull();
        assertThat(em3.find(Car.class, carId)).isNull();
        em3.close();
    }
}
