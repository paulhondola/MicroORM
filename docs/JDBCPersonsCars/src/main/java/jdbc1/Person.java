package jdbc1;

import java.util.*;

public class Person {
    private String name;
    private List<Car> cars = new ArrayList<>();

    public Person() {}

    public Person(String name) {
        this.name = name;
    }

    public void addCar(Car car) {
        cars.add(car);
        car.setPerson(this);
    }


    public String getName() { return name; }
    public List<Car> getCars() { return cars; }
}