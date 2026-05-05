public class Employee {

    private String id;
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
    private String department;
    private String position;
    private double salary;
    private String role;  // "admin" | "employee"

    public Employee(String id, String name, String email, String password,
                    String phoneNumber, String department, String position,
                    double salary, String role) {
        this.id          = id;
        this.name        = name;
        this.email       = email;
        this.password    = password;
        this.phoneNumber = phoneNumber;
        this.department  = department;
        this.position    = position;
        this.salary      = salary;
        this.role        = role;
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPassword()    { return password; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDepartment()  { return department; }
    public String getPosition()    { return position; }
    public double getSalary()      { return salary; }
    public String getRole()        { return role; }

    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }

    /** Format: id,name,email,password,phone,department,position,salary,role */
    public String toFileString() {
        return String.join(",", id, name, email, password,
                phoneNumber, department, position,
                String.valueOf(salary), role);
    }

    public static Employee fromFileString(String line) {
        String[] p = line.split(",", 9);
        if (p.length != 9) return null;
        try {
            return new Employee(
                p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(),
                p[4].trim(), p[5].trim(), p[6].trim(),
                Double.parseDouble(p[7].trim()), p[8].trim()
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
