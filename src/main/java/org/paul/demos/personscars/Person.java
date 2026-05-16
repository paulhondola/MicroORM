package org.paul.demos.personscars;

import org.paul.microorm.annotation.*;

import java.util.ArrayList;
import java.util.List;

/** Course slide "Persons-Cars Example with JPA" — exact field/annotation parity. */
@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @OneToMany(mappedBy = "person", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<Car> cars = new ArrayList<>();

    public Person() {}

    public Person(String name) { this.name = name; }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Car> getCars() { return cars; }
    public void addCar(Car car) { cars.add(car); car.setPerson(this); }

    @Override
    public String toString() {
        return "Person{id=" + id + ", name='" + name + "', cars=" + cars.size() + "}";
    }
}
