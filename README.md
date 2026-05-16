# MicroORM — A Simplified Object-Relational Mapping Framework

A lightweight, JPA-inspired ORM for Java built from scratch on top of JDBC + H2.
Implements a **Generator + Mapper** architecture: a schema
generator that produces DDL from annotated classes, plus a runtime entity
manager that persists and retrieves objects.

## Features

- **Schema generator** — Classes annotated with `@Entity` are scanned and
  compiled into `DROP` + `CREATE TABLE` DDL via `PreparedStatement`.
- **EntityManager** — Runtime `persist`, `find`, `merge`, `remove` with an
  identity-map–backed `PersistenceContext` (cycle-safe hydration).
- **Two demo applications** with two different data models that together
  exercise all three mapping categories (aggregation, association,
  inheritance).

### Mapping Patterns Covered

| Category    | Strategy                                        | Annotations                                                       |
|-------------|--------------------------------------------------|-------------------------------------------------------------------|
| Aggregation | Single-Table Aggregation (`@Embedded` inlined)  | `@Embeddable`, `@Embedded`, `@AttributeOverride`                  |
| Association | Foreign Key on owning side (1-N / N-1)          | `@ManyToOne`, `@OneToMany(mappedBy=…)`, `@JoinColumn`             |
| Association | Association Table (N-N junction)                | `@ManyToMany`, `@JoinTable`                                       |
| Inheritance | One Inheritance Tree → One Table (SINGLE_TABLE) | `@Inheritance(SINGLE_TABLE)`, `@DiscriminatorColumn/Value`        |
| Identity    | IDENTITY-generated synthetic OID                | `@Id`, `@GeneratedValue(strategy = IDENTITY)`                     |
| Cascade     | `ALL`, `PERSIST`, `REMOVE` + `orphanRemoval`    | `CascadeType`                                                     |

`JOINED` and `TABLE_PER_CLASS` inheritance strategies are declared but reject
at scan time with `UnsupportedMappingException`.

## Technology Stack

| Component       | Choice                                   |
|-----------------|------------------------------------------|
| Language        | Java 25                                  |
| Database access | JDBC                                     |
| Database        | H2 2.2.224 (file-backed under `./data/`) |
| Build           | Maven                                    |
| Tests           | JUnit 5 + AssertJ                        |

## Project Structure

```
MicroORM/
├── pom.xml
├── data/                                  # H2 file-backed DBs (gitignored)
├── docs/
│   ├── TASK.md                            # original assignment
│   └── PLAN.md                            # implementation plan
└── src/
    ├── main/java/org/paul/
    │   ├── Main.java                      # demo launcher menu
    │   ├── microorm/
    │   │   ├── annotation/                # @Entity, @Id, @OneToMany, …
    │   │   ├── metadata/                  # scanner + registry (reflection)
    │   │   ├── generator/                 # SchemaGenerator + DDL builders
    │   │   ├── mapper/
    │   │   │   ├── jdbc/                  # ConnectionProvider, properties
    │   │   │   ├── sql/                   # CrudSqlBuilder, ParameterBinder
    │   │   │   ├── hydrate/               # RowMapper, AssociationLoader
    │   │   │   └── session/               # EntityManager(Factory|Impl|Tx)
    │   │   └── exception/                 # MicroOrm/Metadata/Schema exceptions
    │   └── demos/
    │       ├── personscars/               # Demo #1 — @OneToMany / @ManyToOne
    │       └── company/                   # Demo #2 — inheritance + embed + N-N
    └── test/java/                         # JUnit 5 + AssertJ unit & integration tests
```

## Getting Started

### Prerequisites

- Java 25 (set `maven.compiler.source/target` in `pom.xml`)
- Maven 3.6+

### Build

```bash
mvn clean compile
```

### Run the demos

```bash
# Demo #1 — Persons & Cars (1-N association with cascade ALL + orphanRemoval)
mvn exec:java -Dexec.mainClass=org.paul.demos.personscars.PersonsCarsDemo

# Demo #2 — Company model (SINGLE_TABLE inheritance + @Embedded + @ManyToMany)
mvn exec:java -Dexec.mainClass=org.paul.demos.company.CompanyDemo
```

Each demo uses its own file-backed H2 database (`./data/personscars`,
`./data/company`) so they never collide. Schemas are recreated on every run
(`SCHEMA_MODE=create-drop`).

### Run the tests

```bash
mvn test
```

## Example Usage

### Defining entities

```java
@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @OneToMany(mappedBy = "person", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<Car> cars = new ArrayList<>();

    // …getters, setters, addCar(…)
}

@Entity
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String model;

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;
}
```

### Bootstrapping and persisting

```java
Properties props = new Properties();
props.setProperty(MicroOrmProperties.JDBC_URL, "jdbc:h2:./data/personscars");
props.setProperty(MicroOrmProperties.SCHEMA_MODE, MicroOrmProperties.SCHEMA_MODE_CREATE_DROP);

EntityManagerFactory emf = EntityManagerFactory.create(props, Person.class, Car.class);
EntityManager em = emf.createEntityManager();

em.getTransaction().begin();
Person alice = new Person("Alice");
alice.addCar(new Car("Tesla Model S"));
alice.addCar(new Car("BMW i3"));
em.persist(alice);                       // cascades to both cars, writes back IDs
em.getTransaction().commit();

Person found = em.find(Person.class, alice.getId());
assert found.getCars().get(0).getPerson() == found;   // identity-map proof

em.close();
emf.close();
```

## Architectural Notes

- **Generator + Mapper split.** `generator/` is the "Schema
  Generator" (DDL from metadata). `mapper/` is the "Mapper
  API" (object ⇄ row).
- **PreparedStatement discipline.** All runtime SQL is parameterized to avoid SQL-injection, so values
  always flow through `ParameterBinder` to a `?` placeholder.
- **Identity map for cycle safety.** `PersistenceContext` is consulted before
  every association load, so `person.getCars().get(0).getPerson() == person`.
- **ID write-back on cascade insert.** `EntityManagerImpl.persist` reads
  `getGeneratedKeys()` and reflectively sets the new id on the parent before
  inserting children that point to it via FK.
- **SINGLE_TABLE inheritance.** One wide table per hierarchy root + a
  discriminator column. `RowMapper` reads the discriminator and instantiates
  the matching subclass via no-arg constructor + reflection.
