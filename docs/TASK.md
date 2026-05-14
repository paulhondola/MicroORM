# Object-Relational Mapping (ORM)

## Objectives

This assignment focuses on the Object-Relational Mapping pattern and the typical architecture of ORM frameworks. As a conceptual background, you must understand:
- Data access technologies at different levels of abstraction
- Data access patterns and principles
- (Reference: Lectures weeks 9, 10, 11)

**Assignment 4 Variant:** Implement a simplified ORM framework

---

## Standard Requirements

Implement a **generator** that:
- Takes as input a set of classes (supporting inheritance and association relationships)
- Determines the structure of corresponding database tables and their relationships
- Generates the appropriate database schema
- Applies known patterns for mapping class relationships to table relationships

---

## Bonus Requirements

To create a more complete ORM, add the following features:

1. **EntityManager** — Implement an entity manager that can:
   - Save different objects (instances of entity classes) into generated database tables
   - Manage object-to-relational mapping at runtime

2. **Demonstration Applications** — Write at least 2 different applications using 2 different data models to demonstrate the usability of your ORM

---

## Frameworks & Technologies

- **JDBC** — For database access
- **Database** — Any relational database (recommended: **H2 Database** for easy setup without installation)

---

## Resources

- **Inspiration Reference:** Object-Relational Mapping in JPA (Jakarta Persistence, formerly known as Java Persistence API)
  - Note: Do not directly copy; use only for conceptual inspiration
