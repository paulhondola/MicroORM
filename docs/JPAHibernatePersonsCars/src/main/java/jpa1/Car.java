package jpa1;

import jakarta.persistence.*;

@Entity
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String model;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    public Car() {
    }

    public Car(String model) {
        this.model = model;
    }

    public int getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public Person getPerson() {
        return person;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}