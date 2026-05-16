package org.paul.demos.company;

import org.paul.microorm.annotation.DiscriminatorValue;
import org.paul.microorm.annotation.Entity;

@Entity
@DiscriminatorValue("CUST")
public class Customer extends Party {

    private String creditState;

    public Customer() {}

    public Customer(String name, String creditState) {
        super(name);
        this.creditState = creditState;
    }

    public String getCreditState() { return creditState; }
    public void setCreditState(String creditState) { this.creditState = creditState; }
}
