package org.paul.demos.company;

import org.paul.microorm.annotation.*;

/**
 * Inheritance root — "One Inheritance Tree – One Table" (SINGLE_TABLE) example.
 * Course hierarchy: Party → Customer / Employee → FreelanceEmployee / SalariedEmployee.
 */
@Entity
@Table(name = "party")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "party_type")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    public Party() {}

    public Party(String name) { this.name = name; }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", name='" + name + "'}";
    }
}
