package jdbc1;

public class Car {
    private String model;

    private Person person=null;

    public Car() {}

    public Car(String model) {
        this.model = model;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getModel() { return model; }
}