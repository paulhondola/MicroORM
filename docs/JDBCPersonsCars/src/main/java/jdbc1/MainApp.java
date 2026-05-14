package jdbc1;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MainApp {

    // H2 in-memory database
    private static final String URL = "jdbc:h2:mem:testdb";

    // Another oprion: H2 file-based database
    // private static final String URL = "jdbc:h2:file:./data/testdb";
    // With file-based DB, tables can be created only the first run!!!

    public static void main(String[] args) {

        System.out.println("\n\nSTART JDBC DEMO with manual O-R mapping \n");

        try (Connection conn =
                     DriverManager.getConnection(URL)) {

            System.out.println("DATABASE CONNECTION CREATED to: "+URL);

            createTables(conn);

		 System.out.println("TABLES CREATED \n");

            Person p = new Person("Alice");

            p.addCar(new Car("BMW"));
            p.addCar(new Car("Audi"));


            System.out.println("Person object containing Cars created \n");


            savePerson(conn, p);

            System.out.println("Person with cars saved in database \n");

            Person found =
                    findPersonByName(conn, "Alice");
            
            if (found == null) 
			System.out.println("No such person");
		 else {
            System.out.println("Found Person: "
                    + found.getName());

            System.out.println("Has cars: "
                    + found.getCars().size());

            for (Car c : found.getCars()) {
                System.out.println("Car: "
                        + c.getModel());
            }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



   private static void createTables(Connection conn)
            throws SQLException {

        Statement stmt =
                conn.createStatement();

        stmt.execute("""
                CREATE TABLE person (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100)
                )
                """);

        stmt.execute("""
                CREATE TABLE car (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    model VARCHAR(100),
                    person_id INT,
                    FOREIGN KEY (person_id)
                    REFERENCES person(id)
                )
                """);
    }

    private static int savePerson(Connection conn,
                                 Person person)
            throws SQLException {

        String insertPersonSql =
                "INSERT INTO person(name) VALUES (?)";

        PreparedStatement personStmt =
                conn.prepareStatement(
                        insertPersonSql,
                        Statement.RETURN_GENERATED_KEYS
                );

        personStmt.setString(1,
                person.getName());

        personStmt.executeUpdate();

        ResultSet generatedKeys =
                personStmt.getGeneratedKeys();

        int personId = -1;

        if (generatedKeys.next()) {
            personId = generatedKeys.getInt(1);
        }

        String insertCarSql =
                "INSERT INTO car(model, person_id) VALUES (?, ?)";

        PreparedStatement carStmt =
                conn.prepareStatement(insertCarSql);

        for (Car car : person.getCars()) {

            carStmt.setString(1,
                    car.getModel());

            carStmt.setInt(2, personId);

            carStmt.executeUpdate();
        }

        conn.commit();

        return personId;
    }

    private static Person findPersonByName(
            Connection conn,
            String name)
            throws SQLException {

        String findPersonSql =
                "SELECT * FROM person WHERE name = ?";

        PreparedStatement findPersonStmt =
                conn.prepareStatement(findPersonSql);

        findPersonStmt.setString(1, name);

        ResultSet personRs =
                findPersonStmt.executeQuery();

        Person person = null;

        if (personRs.next()) {

            int personId =
                    personRs.getInt("id");

            String personName =
                    personRs.getString("name");

            person = new Person(personName);

            String findCarsSql =
                    "SELECT * FROM car WHERE person_id = ?";

            PreparedStatement findCarsStmt =
                    conn.prepareStatement(findCarsSql);

            findCarsStmt.setInt(1, personId);

            ResultSet carsRs =
                    findCarsStmt.executeQuery();

            while (carsRs.next()) {

                String model =
                        carsRs.getString("model");

                person.addCar(new Car(model));
            }
        }

        return person;
    }

    
}