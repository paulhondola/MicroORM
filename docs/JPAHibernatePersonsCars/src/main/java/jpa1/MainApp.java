package jpa1;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

public class MainApp {

    public static void main(String[] args) {

        System.out.println("\nSTART JPA + Hibernate DEMO\n");

        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("demoPU");

        EntityManager em = emf.createEntityManager();

        try {

            em.getTransaction().begin();

            System.out.println("DATABASE CONNECTION CREATED\n");

            // Create objects
            Person p = new Person("Alice");

            p.addCar(new Car("BMW"));
            p.addCar(new Car("Audi"));

            System.out.println("Person object with Cars created\n");

            // Save person + cars
            em.persist(p);

            em.getTransaction().commit();

            System.out.println("Person with cars saved in database\n");

            // Find person by ID
            Person found =
                    em.find(Person.class, p.getId());

            System.out.println("Found Person: "
                    + found.getName());

            System.out.println("Has cars: "
                    + found.getCars().size());

            for (Car c : found.getCars()) {

                System.out.println("Car: "
                        + c.getModel());
            }

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            em.close();
            emf.close();
        }
    }
}