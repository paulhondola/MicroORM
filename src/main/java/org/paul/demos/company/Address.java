package org.paul.demos.company;

import org.paul.microorm.annotation.Embeddable;

/** Single-Table Aggregation example from the course slides. */
@Embeddable
public class Address {

    private String street;
    private String city;
    private String zip;
    private String state;

    public Address() {}

    public Address(String street, String city, String zip, String state) {
        this.street = street;
        this.city = city;
        this.zip = zip;
        this.state = state;
    }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    @Override
    public String toString() {
        return street + ", " + city + " " + zip + " " + state;
    }
}
