package lk.ijse.gdse.aad.backend.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lk.ijse.gdse.aad.backend.dto.StudentDTO;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet(urlPatterns = "/student")
public class StudentHandle extends HttpServlet {
    Connection connection;
    private static final String SaveStudentData = "INSERT INTO student(name,city,email,level) VALUES (?,?,?,?)";


    @Override
    public void init() throws ServletException {

//        try {
//            Class.forName(getServletContext().getInitParameter("mysql-driver"));
//            String username = getServletContext().getInitParameter("db-user");
//            String password = getServletContext().getInitParameter("db-pw");
//            String url = getServletContext().getInitParameter("db-url");
//            this.connection = DriverManager.getConnection(url,username,password);
//
//        } catch (ClassNotFoundException | SQLException ex) {
//            throw new RuntimeException(ex);
//        }
        try {
            InitialContext ctx = new InitialContext();
            DataSource pool = (DataSource) ctx.lookup("java:comp/env/jdbc/student");
            this.connection = pool.getConnection();
            System.out.println("connection");


        } catch (NamingException | SQLException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {

             if(req.getContentType() == null || !req.getContentType().toLowerCase().startsWith("application/json")){
                 rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
             }
        try {

        Jsonb jsonb = JsonbBuilder.create();
        StudentDTO studentObj = jsonb.fromJson(req.getReader(), StudentDTO.class); 
        //validation
        if(studentObj.getName() == null || !studentObj.getName().matches("[A-Za-z ]+")){
            throw new RuntimeException("Invalid Name");
        } else if (studentObj.getCity() == null || !studentObj.getCity().matches("[A-Za-z ]+")) {
            throw new RuntimeException("Invalid City");
        } else if (studentObj.getEmail()==null) {
            throw new RuntimeException("Invalid Email");
        } else if (studentObj.getLevel() <= 0) {
            throw new RemoteException("Invalid Level");
        }
        //save data in db
            System.out.println("ok");
            PreparedStatement ps =
                    connection.prepareStatement(SaveStudentData, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,studentObj.getName());
            ps.setString(2,studentObj.getCity());
            ps.setString(3,studentObj.getEmail());
            ps.setInt(4,studentObj.getLevel());

            if(ps.executeUpdate() != 1){
                throw new RuntimeException("Save Failed");
            }
            ResultSet rst = ps.getGeneratedKeys();
            rst.next();
            int generatedKey = rst.getInt(1);

            // Create a StudentDTO object with the generated key and other data
            StudentDTO studentDBObj = new StudentDTO(generatedKey, studentObj.getName(), studentObj.getCity(), studentObj.getEmail(), studentObj.getLevel());

            rsp.setStatus(HttpServletResponse.SC_CREATED);
            //the created json is sent to frontend
            rsp.setContentType("application/json");
            jsonb.toJson(studentDBObj,rsp.getWriter());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //Todo:Exception Handle

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String queryString = req.getQueryString();

        // Check the pathInfo or query parameters and perform different actions accordingly
        if(queryString == null) {

            try {
                System.out.println("All");
                List<StudentDTO> students = getAllStudents();

                Jsonb jsonb = JsonbBuilder.create();
                String jsonData = jsonb.toJson(students);

                // Set the appropriate response headers.
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");

                // Send the JSON data back to the client (front-end).
                resp.getWriter().write(jsonData);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        if (!queryString.equals(null)) {
            try {
                String idParam = req.getParameter("id");

                // Check if the "id" query parameter is provided and not empty
                if (idParam == null || idParam.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
                    return;
                }

                int id = Integer.parseInt(idParam);

                // Fetch the student details based on the provided ID
                StudentDTO student = getStudentById(id);

                if (student != null) {
                    Jsonb jsonb = JsonbBuilder.create();

                    // Convert the student object to JSON
                    String jsonData = jsonb.toJson(student);

                    // Set the appropriate response headers.
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");

                    // Send the JSON data back to the client (front-end).
                    resp.getWriter().write(jsonData);
                } else {
                    // If the student with the given ID is not found, return a 404 Not Found response
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 Not Found
                    resp.getWriter().write("Student not found");
                }
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
                resp.getWriter().write("Invalid student ID format");
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
                resp.getWriter().write("Error occurred while fetching the student");
            }
        }

        System.out.println("between");


    }

    // Method to fetch a student by ID from the database
    private StudentDTO getStudentById(int id) throws SQLException {
        String query = "SELECT * FROM student WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    String name = resultSet.getString("name");
                    String city = resultSet.getString("city");
                    String email = resultSet.getString("email");
                    int level = resultSet.getInt("level");

                    // Create and return the StudentDTO object
                    return new StudentDTO(id, name, city, email, level);
                }
            }
        }

        return null; // Return null if student with the given ID is not found
    }

    private List<StudentDTO> getAllStudents() throws SQLException {
        List<StudentDTO> students = new ArrayList<>();
        String query = "SELECT * FROM student";

        try (Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query)) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String city = resultSet.getString("city");
                String email = resultSet.getString("email");
                int level = resultSet.getInt("level");

                // Create a StudentDTO object and add it to the list.
                StudentDTO student = new StudentDTO(id, name, city, email, level);
                students.add(student);
            }
        }

        return students;
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Assuming the "id" is passed as a query parameter in the request URL
        String idParam = req.getParameter("id");

        if (idParam == null || idParam.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            return;
        }

        try {
            int id = Integer.parseInt(idParam);

            // Prepare the SQL statement to delete the student with the given ID
            String deleteQuery = "DELETE FROM student WHERE id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery);
            preparedStatement.setInt(1, id);

            // Execute the delete operation
            int rowsDeleted = preparedStatement.executeUpdate();
            preparedStatement.close();

            if (rowsDeleted > 0) {
                resp.setStatus(HttpServletResponse.SC_OK); // 200 OK
                resp.getWriter().write("Student deleted successfully");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 Not Found
                resp.getWriter().write("Student not found");
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            resp.getWriter().write("Invalid student ID format");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
            resp.getWriter().write("Error occurred while deleting the student");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getContentType() == null || !req.getContentType().toLowerCase().startsWith("application/json")) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        try {
            Jsonb jsonb = JsonbBuilder.create();
            StudentDTO updatedStudent = jsonb.fromJson(req.getReader(), StudentDTO.class);

            // Validation (same as in doPost)
            if (updatedStudent.getName() == null || !updatedStudent.getName().matches("[A-Za-z ]+")) {
                throw new RuntimeException("Invalid Name");
            } else if (updatedStudent.getCity() == null || !updatedStudent.getCity().matches("[A-Za-z ]+")) {
                throw new RuntimeException("Invalid City");
            } else if (updatedStudent.getEmail() == null) {
                throw new RuntimeException("Invalid Email");
            } else if (updatedStudent.getLevel() <= 0) {
                throw new RuntimeException("Invalid Level");
            }

            // Update data in the database
            String updateQuery = "UPDATE student SET name=?, city=?, email=?, level=? WHERE id=?";
            PreparedStatement ps = connection.prepareStatement(updateQuery);
            ps.setString(1, updatedStudent.getName());
            ps.setString(2, updatedStudent.getCity());
            ps.setString(3, updatedStudent.getEmail());
            ps.setInt(4, updatedStudent.getLevel());
            ps.setInt(5, updatedStudent.getId());

            int rowsUpdated = ps.executeUpdate();
            ps.close();

            if (rowsUpdated > 0) {
                // If the update was successful, send a response indicating success
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Student updated successfully");
            } else {
                // If the update was not successful, return a 404 Not Found response
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("Student not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        // Todo: Exception Handle
    }
}
