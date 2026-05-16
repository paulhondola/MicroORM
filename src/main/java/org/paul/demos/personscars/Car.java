package org.paul.demos.personscars;

import org.paul.microorm.annotation.*;

/** Course slide "Persons-Cars Example with JPA" — exact field/annotation parity. */
@Entity
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String model;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    public Car() {}

    public Car(String model) { this.model = model; }

    public int getId() { return id; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    @Override
    public String toString() {
        return "Car{id=" + id + ", model='" + model + "'}";
    }
}
