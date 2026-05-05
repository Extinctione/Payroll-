# Employee Payroll System (Java Swing)

A minimal Employee Payroll System built using Java Swing with file-based storage.  
This project demonstrates Object-Oriented Programming, GUI development, and file handling.

---

## Features

- Login system (email and password)
- Employee registration
- Dashboard displaying employee details
- Salary display
- File-based storage using `employees.txt`
- Logout functionality
- Modular OOP structure

---

## Project Structure

vs-prog-main/
│
├── src/
│   ├── Main.java
│   ├── Employee.java
│   ├── EmployeeService.java
│   ├── LoginScreen.java
│   ├── RegistrationScreen.java
│   └── DashboardScreen.java
│
├── bin/               (compiled files)
├── employees.txt      (data storage)
└── README.md

---

## Technologies Used

- Java (JDK 8 or higher)
- Java Swing
- File handling (text file)

---

## Data Format

Employee data is stored in:

employees.txt

Format:

id,name,email,password,salary

Example:

E001,John Doe,john@gmail.com,1234,50000

---

## How to Run

1. Navigate to project folder

cd vs-prog-main

2. Clean compiled files (optional)

PowerShell:
gci -r *.class | rm

3. Compile

javac -d bin src/*.java

4. Run

java -cp bin Main

---

## Notes

- Source files are inside the `src/` folder
- Compiled `.class` files are generated in `bin/`
- `.class` files should not be tracked in Git
- Ensure `employees.txt` exists or is created during runtime

---

## Future Improvements

- Attendance-based salary calculation
- Admin panel
- Payslip generation
- Database integration
- Improved UI design

---

## Author
core
