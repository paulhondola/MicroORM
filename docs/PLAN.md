# MicroORM — Feature & Structure Plan (course-aligned)

## Context

Academic assignment to build a simplified ORM framework from scratch on **JDBC + H2**, inspired by JPA. The course materials at `docs/JDBC.pdf` (54 slides) and `docs/OORM.pdf` (33 slides) define the exact terminology and mapping patterns the grader expects.

**Course-canonical architecture** (slide titled "Automatic ORM Tools/Frameworks"):

> **ORM = Generator + Mapper**
>
> - **Generator**: Classes → Database structure (DDL)
> - **Mapper API**: Objects ⇄ Data rows (`save` / `find`)
> - Both driven by **Rules/Patterns for Object-Relational Mapping** (Wolfgang Keller, *Mapping Objects to Tables*)

**Course taxonomy** — three categories of class-diagram relationships to map (slide "Pattern for OO-Relational Mapping"):
1. **Aggregation**
2. **Association**
3. **Inheritance**

Each category has named strategies that map to JPA equivalents (slide "Choosing Mapping Pattern"):

| Course term | Strategy | JPA equivalent | In MVP? |
|---|---|---|---|
| Aggregation: Single Table Aggregation | aggregated attrs inlined into aggregating table | `@Embeddable` / `@Embedded` | ✓ |
| Aggregation: Foreign Key Aggregation | separate table + FK | `@OneToOne` w/ FK + cascade | — (collections covered by @OneToMany) |
| Association: Association Table | junction table holding both OIDs | `@ManyToMany` + `@JoinTable` | ✓ |
| Inheritance: One Inheritance Tree – One Table | union of all attrs, NULL for unused | `@Inheritance(SINGLE_TABLE)` + discriminator | ✓ |
| Inheritance: One Class – One Table | each class has its own table, FK to parent | `@Inheritance(JOINED)` | stub |
| Inheritance: One Inheritance Path – One Table | table per concrete class | `@Inheritance(TABLE_PER_CLASS)` | stub |

**Course's exact code examples we must support verbatim:**
- **Persons-Cars (slide "Persons-Cars Example with JPA")**: `Person {@Id @GeneratedValue(IDENTITY) int id; String name; @OneToMany(mappedBy="person", cascade=ALL, orphanRemoval=true) List<Car> cars;}` and `Car {@Id @GeneratedValue(IDENTITY) int id; String model; @ManyToOne @JoinColumn(name="person_id") Person person;}` — identical to `docs/JPAHibernatePersonsCars/`.
- **Party hierarchy (inheritance slides)**: `Party → Customer / Employee → FreelanceEmployee / SalariedEmployee`.
- **Employee n:m Department (association slide)**.
- **Customer with InvoiceAddress + DeliveryAddress (aggregation slide)** — exact Single-Table Aggregation example.

**Course-mandated JDBC discipline (JDBC.pdf):**
- **PreparedStatement everywhere** — the SQL-injection slide explicitly shows `String` concatenation as the anti-pattern. MicroORM must never build SQL by concatenating runtime values.
- DriverManager.getConnection → Connection → PreparedStatement → executeUpdate / executeQuery → ResultSet (cursor: `next()`, `getInt()`, `getString()`, …) → close.
- H2 in-memory URL: `jdbc:h2:mem:testdb`.
- Surrogate key called **Synthetic OID** conceptually; JPA example uses `int id` with IDENTITY auto-increment (we follow this).

**Current MicroORM state:** empty scaffold. `pom.xml` (Java 25, H2 2.2.224, exec-maven-plugin → broken `org.paul.Main`), two stub `Main.java` files (one references undefined `IO.println`), empty `src/main/resources/`.

**TASK.md requirements** (verified against course):
- **Standard:** Generator that, given a set of classes (with inheritance + associations), produces the DB schema using known mapping patterns.
- **Bonus:** EntityManager that saves objects at runtime + 2 demo apps with 2 different data models.

This plan covers both standard and bonus with the minimum scope that hits all three course categories.

---

## Scope Decisions

Aligned to course coverage, not just JPA reference.

| Category | MVP strategy (implement) | Stretch | JPA annotation surface |
|---|---|---|---|
| **Aggregation — Single Table** | **`@Embeddable` inlined into aggregating table with column prefix / `@AttributeOverride`** | FK Aggregation (`@OneToOne`) | `@Embeddable`, `@Embedded`, `@AttributeOverride` |
| **Association — 1-N / N-1** | **Foreign Key on owning side** | — | `@ManyToOne`, `@OneToMany(mappedBy=...)`, `@JoinColumn` |
| **Association — N-N** | **Association Table (junction)** | — | `@ManyToMany`, `@JoinTable` |
| **Inheritance** | **SINGLE_TABLE only** (discriminator column) | JOINED, TABLE_PER_CLASS | `@Inheritance(strategy=SINGLE_TABLE)`, `@DiscriminatorColumn`, `@DiscriminatorValue` |
| Loading | Eager only | Lazy (`PersistentCollection` wrapper) | `@FetchType` (LAZY honored as EAGER) |
| Persistence context | Identity map (`Map<EntityKey,Object>`) | snapshots / dirty checking | — |
| Updates | Explicit `merge(entity)` → full-column UPDATE | — | — |
| Cascade | `ALL`, `PERSIST`, `REMOVE` | the rest | `CascadeType` enum |
| Config | `Properties` passed to `EntityManagerFactory.create(props)` | `persistence.xml` parsing | — |
| Schema modes | `create-drop` only | `update`, `validate` | — |
| Java | **25** (current `pom.xml`) | — | — |

**Why these cuts:**

- **Single-Table Aggregation only.** `@Embedded` is single-valued by design — collections of "aggregated" objects (e.g. `Car.wheels`) are modeled as `@OneToMany` to a child `@Entity`, which we need anyway for Persons-Cars. This avoids a separate FK-aggregation code path with its reverse-cascade-ordering bug surface.
- **SINGLE_TABLE inheritance only.** Easiest to implement: one CREATE TABLE per hierarchy root, discriminator column drives subclass dispatch in the row mapper, no JOIN/multi-INSERT machinery. JOINED costs ~150 extra LOC for marginal pedagogical gain. Both alternates declared in `InheritanceType` enum but throw `UnsupportedMappingException` at scan time with a clear message.

---

## Package & File Structure (course-aligned naming)

Top-level packages mirror **Generator + Mapper** from the course canon. All under `org.paul.microorm.*` (lowercase, Java convention). Existing broken `org.paul.MicroORM.Main` is deleted.

```
src/main/java/
├── org/paul/microorm/
│   ├── annotation/             JPA-compatible annotation set
│   │   ├── Entity.java
│   │   ├── Table.java
│   │   ├── Id.java
│   │   ├── GeneratedValue.java
│   │   ├── GenerationType.java                    enum: IDENTITY
│   │   ├── Column.java                            name, nullable, length
│   │   ├── Transient.java
│   │   ├── Embeddable.java                        marks aggregated value type
│   │   ├── Embedded.java                          marks aggregating field
│   │   ├── AttributeOverride.java                 column name override for embedded fields
│   │   ├── ManyToOne.java                         fetch, cascade
│   │   ├── OneToMany.java                         mappedBy, cascade, orphanRemoval
│   │   ├── ManyToMany.java                        mappedBy, cascade
│   │   ├── JoinColumn.java                        name
│   │   ├── JoinTable.java                         name, joinColumns, inverseJoinColumns
│   │   ├── Inheritance.java                       strategy
│   │   ├── InheritanceType.java                   enum: SINGLE_TABLE (only honored), JOINED, TABLE_PER_CLASS
│   │   ├── DiscriminatorColumn.java               name, default "dtype"
│   │   ├── DiscriminatorValue.java
│   │   ├── CascadeType.java                       enum: ALL, PERSIST, REMOVE
│   │   └── FetchType.java                         enum: EAGER, LAZY (LAZY honored as EAGER)
│   ├── metadata/               class-introspection → EntityMetadata
│   │   ├── EntityMetadata.java                    table, idField, columns, associations, embedded, inheritance
│   │   ├── ColumnMetadata.java                    field, columnName, sqlType, nullable, length
│   │   ├── AssociationMetadata.java               kind (MANY_TO_ONE / ONE_TO_MANY / MANY_TO_MANY), target, mappedBy, fkColumn, joinTable, cascade
│   │   ├── EmbeddedMetadata.java                  field, columnPrefix, columns
│   │   ├── InheritanceMetadata.java               strategy, discriminatorColumn, discriminatorValue, subtypes, root
│   │   ├── MetadataScanner.java                   class → EntityMetadata (reflection)
│   │   └── MetadataRegistry.java                  cache + resolves OneToMany↔ManyToOne pairs + inheritance tree
│   ├── generator/              ← course canon: "Schema Generator"
│   │   ├── SchemaGenerator.java                   entities → DROP + CREATE DDL
│   │   ├── DdlBuilder.java                        column / FK / discriminator / junction-table assembly
│   │   ├── JavaToSqlTypeMapper.java               int→INT, long→BIGINT, String→VARCHAR(255), boolean→BOOLEAN, Date/LocalDate→DATE, etc.
│   │   ├── ForeignKeyResolver.java                FK on owning side (@ManyToOne); rejects orphan @OneToMany
│   │   ├── JoinTableResolver.java                 emits junction table for @ManyToMany
│   │   ├── EmbeddedFlattener.java                 inlines @Embedded fields with optional name prefix
│   │   └── SingleTableInheritanceDdl.java         emits one wide table + discriminator column per hierarchy root
│   ├── mapper/                 ← course canon: "Mapper API"
│   │   ├── jdbc/
│   │   │   ├── ConnectionProvider.java            wraps DriverManager.getConnection
│   │   │   └── MicroOrmProperties.java            jdbc.url / user / password / schema.mode
│   │   ├── sql/
│   │   │   ├── CrudSqlBuilder.java                INSERT / SELECT / UPDATE / DELETE (single class, metadata-driven)
│   │   │   └── ParameterBinder.java               field value → PreparedStatement.setXxx (PreparedStatement ALWAYS)
│   │   ├── hydrate/
│   │   │   ├── RowMapper.java                     ResultSet → POJO (no-arg ctor + reflection + discriminator dispatch)
│   │   │   └── AssociationLoader.java             eager-loads @ManyToOne / @OneToMany / @ManyToMany via identity map
│   │   └── session/
│   │       ├── EntityManagerFactory.java          create(Properties) → EMF
│   │       ├── EntityManager.java                 interface: persist, find, merge, remove, getTransaction
│   │       ├── EntityManagerImpl.java             orchestrates persist (cascade + ID write-back), find (with identity map), merge, remove
│   │       ├── EntityTransaction.java             wraps Connection autoCommit / commit / rollback
│   │       ├── PersistenceContext.java            Map<EntityKey, Object> identity map (cycle-safe hydration)
│   │       └── EntityKey.java                     (Class<?>, Object id)
│   └── exception/
│       └── MicroOrmException.java                 (+ MetadataException, SchemaException, UnsupportedMappingException subtypes)
└── org/paul/demos/
    ├── personscars/                               Demo #1: byte-for-byte parity with both references and the course slide
    │   ├── Person.java                            @Entity, @Id, @OneToMany(mappedBy="person", cascade=ALL, orphanRemoval=true)
    │   ├── Car.java                               @Entity, @Id, @ManyToOne, @JoinColumn("person_id")
    │   └── PersonsCarsDemo.java
    └── company/                                   Demo #2: hits all three course categories in one model
        ├── Address.java                           @Embeddable (street, city, zip, state)        — Single-Table Aggregation
        ├── Department.java                        @Entity, @ManyToMany(mappedBy="departments")
        ├── Party.java                             @Entity, @Inheritance(SINGLE_TABLE),
        │                                            @DiscriminatorColumn("party_type"), @Id   — Inheritance root
        ├── Customer.java                          @DiscriminatorValue("CUST") extends Party — creditState
        ├── Employee.java                          @DiscriminatorValue("EMP")  extends Party — socSecurityNo,
        │                                            @Embedded Address homeAddress,             — Single-Table Aggregation
        │                                            @ManyToMany Set<Department>                — Association Table
        ├── FreelanceEmployee.java                 @DiscriminatorValue("FREE") extends Employee — hourlySalary
        ├── SalariedEmployee.java                  @DiscriminatorValue("SAL")  extends Employee — monthlySalary
        └── CompanyDemo.java

src/main/resources/                                empty — config passed programmatically
```

**Demo #2 design rationale:** the `company` package replicates the course's named examples verbatim — Party/Customer/Employee/FreelanceEmployee/SalariedEmployee is the inheritance example from the slides (course shows it under "One Inheritance Tree — One Table" = SINGLE_TABLE), Employee↔Department is the Association Table example, `@Embedded Address` is the Single-Table Aggregation example (Customer with InvoiceAddress in the slide). **One demo proves all three categories the course teaches**, using the same vocabulary the grader uses.

---

## Architectural Gaps (load-bearing correctness)

1. **Bidirectional hydration cycles.** Loading `Person` loads `cars`; each `Car.person` must reference the *same* `Person` instance. **`PersistenceContext` must be consulted BEFORE every association load** — never re-enter hydration for an `EntityKey` already in the map. Same applies to `Employee ↔ Department`.

2. **ID write-back on cascade insert.** `persist(person)` flow: (a) execute parent INSERT via PreparedStatement, (b) read generated id from `getGeneratedKeys()`, (c) reflectively set it on the parent, (d) for each cascade child, set `child.person_id = parent.id` before its INSERT. Lives in `EntityManagerImpl.persist`, **not** in `CrudSqlBuilder`.

3. **`@OneToMany(mappedBy=...)` owns no FK column.** FK lives on the child's `@ManyToOne`. `SchemaGenerator` must emit FK on the child table and skip the parent's collection field. `MetadataScanner` enforces: for every `@OneToMany` with `mappedBy`, locate the matching `@ManyToOne` on the target — reject if not found.

4. **SINGLE_TABLE subclass dispatch.** `SingleTableInheritanceDdl` emits one wide table per root (union of all subclass columns + discriminator); INSERT writes the discriminator from the runtime class; `RowMapper` reads the discriminator and instantiates the matching subclass via `Map<discriminatorValue, Class<?>>` from `InheritanceMetadata`. Columns not declared on the actual runtime class are written as `NULL`.

5. **@Embedded field flattening.** `MetadataScanner` walks an `@Embeddable` class's fields and registers each as `ColumnMetadata` under the embedding entity with column-name prefix (`home_address_street`, `home_address_city`, …) or with `@AttributeOverride` honored. `CrudSqlBuilder` reads/writes these columns transparently. **Embedded fields are not separate rows.** Scanner rejects `@Embedded` on collection-typed fields with a clear error.

6. **@ManyToMany junction table I/O.** On persist of `Employee` with departments, `EntityManagerImpl` (a) inserts the Employee, (b) cascades-or-validates each Department exists, (c) inserts a row into `employee_department(employee_id, department_id)` per association. On find, hydration issues `SELECT department_id FROM employee_department WHERE employee_id = ?` and eagerly loads each `Department` through the identity map.

---

## Build Phases

| Phase | Deliverable | Acceptance |
|---|---|---|
| **1** | `annotation/` (full set: `@Embeddable`, `@ManyToMany`, `@Inheritance`, etc.) + `metadata/` scanner | `MetadataRegistry.of(Person, Car, Party, Customer, Employee, FreelanceEmployee, SalariedEmployee, Department, Address)` produces a printable tree; all `mappedBy` pairs resolved; inheritance root + subtypes linked with discriminator values. |
| **2** | `generator/SchemaGenerator` for **flat entities** (no inheritance, no associations) | Generated DDL for `Person` alone matches the JDBC baseline byte-for-byte. |
| **3** | Generator: FK columns (@ManyToOne) + junction tables (@ManyToMany) + @Embedded column flattening | Full `person` + `car` DDL + `employee_department` junction + Employee's `home_address_street`, `home_address_city`, … inline columns. |
| **4** | `mapper/session` MVP: persist + find for single flat entity (no associations) | Round-trip a `Department` through H2 via PreparedStatement. |
| **5** | Associations at runtime: @ManyToOne FK write, @OneToMany cascade insert, eager hydration via identity map | **Demo #1 (PersonsCars) runs end-to-end**; `person.cars.get(0).person == person`. |
| **6** | SINGLE_TABLE inheritance: one wide table per hierarchy + discriminator DDL + discriminator-driven persist + polymorphic find | `em.find(Party.class, id)` returns a `SalariedEmployee` instance when row's discriminator = `"SAL"`. |
| **7** | @ManyToMany junction I/O + @Embedded persist/load | **Demo #2 (Company) runs end-to-end** — saves a SalariedEmployee with embedded Address and 2 Departments; finds it back; all populated; subclass type correct. |
| **8** | `remove` (with `@OneToMany` orphanRemoval), `merge` (full-column UPDATE), `EntityTransaction` rollback path | Both demos exercise update + delete cleanly. |

Phases 2 → 3 → 5 sequence puts flat-entity schema green before adding FK resolution, which is the trickiest schema step.

---

## Critical Files to Create/Modify

- `pom.xml` — keep Java 25; keep H2 2.2.224; either delete `exec-maven-plugin` mainClass or wire to a launcher.
- `src/main/java/org/paul/microorm/metadata/MetadataRegistry.java` — Phase 1 keystone.
- `src/main/java/org/paul/microorm/generator/SchemaGenerator.java` — Phases 2–3 (the **Standard** requirement of TASK.md).
- `src/main/java/org/paul/microorm/generator/SingleTableInheritanceDdl.java` — Phase 6 (the part TASK.md explicitly names: "inheritance").
- `src/main/java/org/paul/microorm/mapper/session/EntityManagerImpl.java` — Phases 4–8 (the **Bonus** requirement); owns cascade + ID write-back.
- `src/main/java/org/paul/microorm/mapper/hydrate/AssociationLoader.java` — cycle correctness for all three association kinds.
- `src/main/java/org/paul/microorm/mapper/sql/CrudSqlBuilder.java` + `ParameterBinder.java` — **always emits `?` placeholders, never concatenates values** (course-mandated discipline).
- **Delete** `src/main/java/org/paul/MicroORM/Main.java` (broken — references undefined `IO.println`).
- Repurpose `src/main/java/org/paul/Main.java` as a launcher menu, or delete and adjust `exec-maven-plugin`.

`README.md` already exists with the roadmap; align after implementation. `docs/TASK.md` was already reformatted in the prior session.

---

## Reuse from References & Course

- **DDL shape** for `person` / `car` tables — generated by `SchemaGenerator` to byte-match the JDBC baseline (Phase 3 acceptance).
- **`getGeneratedKeys()` flow** for IDENTITY id retrieval — directly from the JDBC baseline, generalized inside `EntityManagerImpl.persist`.
- **JPA's `EntityManagerFactory` / `EntityManager` / `EntityTransaction` API surface** — mirrored to course's "Simple Example with JPA" slide.
- **Persons-Cars JPA code** from the OORM slides — Demo #1 uses identical class structure (`int id`, `@GeneratedValue(IDENTITY)`, `@OneToMany(mappedBy="person", cascade=ALL, orphanRemoval=true)`, `@ManyToOne @JoinColumn("person_id")`).
- **PreparedStatement discipline** — `mapper/sql` never concatenates user-supplied values; every parameter goes through `ParameterBinder` to a `?` placeholder.

---

## Verification Plan

1. **Demo #1 parity** — `mvn exec:java -Dexec.mainClass=org.paul.demos.personscars.PersonsCarsDemo`; stdout should mirror the JPAHibernatePersonsCars reference's output.
2. **Demo #2 generality** — `mvn exec:java -Dexec.mainClass=org.paul.demos.company.CompanyDemo`; persists a `SalariedEmployee` (subclass of Employee → Party) with `@Embedded Address homeAddress` and two `Department` references, then `find(Party.class, id)` returns a fully-populated `SalariedEmployee` with: correct subclass type (discriminator = `"SAL"`), populated home_address_* fields, exactly two departments, and `department.employees.contains(employee)` true.
3. **Generated DDL inspection** — `SchemaGenerator.printSchema()` invoked at startup; visually verify:
   - FK on owning side only (no column on parent's collection field)
   - Discriminator column `party_type` on the single `party` table; columns from Customer + Employee + FreelanceEmployee + SalariedEmployee all present (with NULLs at runtime for unused-by-this-row subclass cols)
   - Junction table `employee_department(employee_id INT, department_id INT, PRIMARY KEY (employee_id, department_id), FK …)`
   - `Address` fields inlined as `home_address_street`, `home_address_city`, `home_address_zip`, `home_address_state`
4. **Cycle test** — after `find(Person.class, id)`, assert `person.cars.get(0).person == person` (identity map proof).
5. **PreparedStatement audit** — grep `mapper/sql` for `Statement.execute` calls outside DDL emission; expect zero hits. All runtime SQL goes through `PreparedStatement.set*` + `?`.
6. **Build sanity** — `mvn clean compile` and `mvn package` succeed on Java 25.

---

## Top 3 Risks & Mitigations

1. **Inheritance × associations interaction.** When `Employee` is a SINGLE_TABLE subclass AND has `@ManyToMany Set<Department>`, a `find(Employee.class, id)` must (a) read the row from `party`, (b) check discriminator dispatches to `Employee` / `SalariedEmployee` / `FreelanceEmployee`, (c) hydrate via the matching row mapper, (d) load departments through the identity map. **Mitigation:** build inheritance (Phase 6) green before adding M-N (Phase 7); add an explicit integration test (`SalariedEmployee with departments`).

2. **Reflection-on-private-fields friction on Java 17+.** `setAccessible(true)` works in the unnamed module but emits warnings. **Mitigation:** no `module-info.java`; document constraint in README; guard with `Field.canAccess()` and emit clear `MicroOrmException` if blocked.

3. **Schema regeneration interference between demos.** Two demos against shared H2 in-memory state may collide. **Mitigation:** each demo's `EntityManagerFactory` uses a distinct in-memory URL (`jdbc:h2:mem:personscars` vs `jdbc:h2:mem:company`); fresh schema per factory.
