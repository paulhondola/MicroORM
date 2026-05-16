package org.paul.demos.company;

import org.paul.microorm.annotation.DiscriminatorValue;
import org.paul.microorm.annotation.Entity;

@Entity
@DiscriminatorValue("FREE")
public class FreelanceEmployee extends Employee {

    private double hourlySalary;

    public FreelanceEmployee() {}

    public FreelanceEmployee(String name, String socSecNo, double hourlySalary) {
        super(name, socSecNo);
        this.hourlySalary = hourlySalary;
    }

    public double getHourlySalary() { return hourlySalary; }
    public void setHourlySalary(double hourlySalary) { this.hourlySalary = hourlySalary; }
}
