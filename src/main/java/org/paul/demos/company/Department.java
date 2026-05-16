package org.paul.demos.company;

import org.paul.microorm.annotation.*;

import java.util.HashSet;
import java.util.Set;

/** Association Table example from the course slides (Employee ↔ Department). */
@Entity
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @ManyToMany(mappedBy = "departments")
    private Set<Employee> employees = new HashSet<>();

    public Department() {}

    public Department(String name) { this.name = name; }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Employee> getEmployees() { return employees; }

    @Override
    public String toString() {
        return "Department{id=" + id + ", name='" + name + "'}";
    }
}
