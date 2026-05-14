# MicroORM – A Simplified Object-Relational Mapping Framework

A lightweight Java implementation of an Object-Relational Mapping (ORM) framework, demonstrating core ORM concepts including schema generation, entity management, and relationship mapping.

## Overview

This project implements a simplified ORM framework that bridges the gap between object-oriented Java code and relational databases. It focuses on understanding the architecture and patterns used by production ORMs like JPA/Hibernate.

### Key Concepts

- **ORM Pattern**: Automatically mapping Java classes to database tables
- **Schema Generation**: Creating database tables from class definitions
- **Entity Management**: Persisting and retrieving objects from a relational database
- **Relationship Mapping**: Supporting inheritance and association relationships

## Requirements

### Standard Requirements ✓

Implement a **schema generator** that:
- Accepts a set of Java classes (with inheritance and association relationships)
- Determines the structure of corresponding database tables
- Generates database schema automatically
- Applies established ORM patterns for relationship mapping

### Bonus Requirements

- **EntityManager**: Runtime entity persistence and retrieval
  - Save objects to database
  - Retrieve objects from database
  - Manage object lifecycle

- **Demonstration Applications**: 2+ sample applications showing practical usage
  - Different data models for each application
  - Proof of ORM framework usability

## Technology Stack

| Component | Technology |
|-----------|------------|
| **Language** | Java |
| **Database Access** | JDBC |
| **Database** | H2 (embedded, no setup required) |
| **Build Tool** | Maven |

## Project Structure

```
MicroORM/
├── README.md
├── pom.xml
├── src/
│   ├── main/java/org/paul/
│   │   ├── Main.java
│   │   ├── orm/
│   │   │   ├── SchemaGenerator.java      # DDL generation
│   │   │   ├── EntityManager.java        # Persistence logic
│   │   │   └── ...
│   │   └── models/
│   │       ├── Entity.java               # Annotations
│   │       ├── Column.java
│   │       └── ...
│   └── test/java/org/paul/
│       └── ...
└── docs/
    └── TASK.md
```

## Getting Started

### Prerequisites
- Java 11+
- Maven 3.6+

### Build

```bash
mvn clean compile
```

### Run

```bash
mvn exec:java -Dexec.mainClass="org.paul.Main"
```

## Implementation Roadmap

- [ ] Design ORM annotations (`@Entity`, `@Column`, `@OneToMany`, etc.)
- [ ] Implement schema generator (class → SQL DDL)
- [ ] Create EntityManager for persistence
- [ ] Add support for relationships (1-to-1, 1-to-many, many-to-many)
- [ ] Implement inheritance mapping strategies
- [ ] Build first demo application
- [ ] Build second demo application
- [ ] Write integration tests

## Example Usage (Target)

```java
@Entity(table = "users")
public class User {
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "name")
    private String name;
    
    @OneToMany(mappedBy = "user")
    private List<Post> posts;
}

// Using the ORM
EntityManager em = new EntityManager(dataSource);
User user = new User(1L, "Alice");
em.save(user);

User retrieved = em.find(User.class, 1L);
```

## Resources

- **JPA/Hibernate** — Production ORM framework (reference for design patterns)
- **JDBC Tutorial** — Core Java database connectivity
- **H2 Database** — Embedded database setup and usage

## Notes

- This is an educational implementation focusing on ORM concepts
- Production ORMs (JPA, Hibernate) have significantly more features and optimizations
- Use this as a learning tool to understand how ORM frameworks work under the hood

## License

Educational project

---

**Start Date:** May 14, 2026  
**Current Status:** In Development
