package org.paul.demos.company;

import org.paul.microorm.annotation.DiscriminatorValue;
import org.paul.microorm.annotation.Entity;

@Entity
@DiscriminatorValue("SAL")
public class SalariedEmployee extends Employee {

    private double monthlySalary;

    public SalariedEmployee() {}

    public SalariedEmployee(String name, String socSecNo, double monthlySalary) {
        super(name, socSecNo);
        this.monthlySalary = monthlySalary;
    }

    public double getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(double monthlySalary) { this.monthlySalary = monthlySalary; }
}
