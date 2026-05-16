package org.paul.demos.company;

import org.paul.microorm.annotation.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("EMP")
public class Employee extends Party {

    private String socSecurityNo;

    /** Single-Table Aggregation: Address columns inlined into the party table. */
    @Embedded
    private Address homeAddress;

    /** Association Table: junction table employee_department. */
    @ManyToMany
    @JoinTable(
        name = "employee_department",
        joinColumns = @JoinColumn(name = "employee_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> departments = new HashSet<>();

    public Employee() {}

    public Employee(String name, String socSecurityNo) {
        super(name);
        this.socSecurityNo = socSecurityNo;
    }

    public String getSocSecurityNo() { return socSecurityNo; }
    public void setSocSecurityNo(String socSecurityNo) { this.socSecurityNo = socSecurityNo; }
    public Address getHomeAddress() { return homeAddress; }
    public void setHomeAddress(Address homeAddress) { this.homeAddress = homeAddress; }
    public Set<Department> getDepartments() { return departments; }
    public void addDepartment(Department d) { departments.add(d); }
}
