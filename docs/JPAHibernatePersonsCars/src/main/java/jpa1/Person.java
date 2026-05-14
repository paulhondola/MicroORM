package jpa1;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @OneToMany(
            mappedBy = "person",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Car> cars = new ArrayList<>();

    public Person() {
    }

    public Person(String name) {
        this.name = name;
    }

    public void addCar(Car car) {

        cars.add(car);
        car.setPerson(this);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Car> getCars() {
        return cars;
    }

    public void setName(String name) {
        this.name = name;
    }
}