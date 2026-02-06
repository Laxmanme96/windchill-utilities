<%@ include file="/netmarkets/jsp/components/beginWizard.jspf"%>

<!DOCTYPE html>
<html>

<head>
    <title>Employee Table with Vertical Scroll</title>
    <style>
        .table-container {
            width: 50%;
            max-height: 250px; /* Adjust height as needed */
            overflow-y: auto;
            border: 2px solid black;
        }

        table {
            width: 50%;
            border-collapse: collapse;
        }

        thead {
            position: sticky;
            top: 0;
            background-color: white;
            z-index: 2;
        }

        th, td {
            padding: 8px;
            border: 2px solid black;
            text-align: center;
            min-width: 150px;
        }
    </style>
</head>

<body>

    <h2>Employee List</h2>

    <div class="table-container">
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>First Name</th>
                    <th>Last Name</th>
                    <th>Department</th>
                    <th>Salary</th>
                </tr>
            </thead>
            <tbody>
                <%
                    // Hardcoded data
                    String[][] employees = {
                        {"1", "John", "Doe", "IT", "60000"},
                        {"2", "Jane", "Smith", "HR", "55000"},
                        {"3", "Mike", "Brown", "Finance", "70000"},
                        {"4", "Emily", "Davis", "Marketing", "65000"},
                        {"5", "Robert", "Wilson", "Sales", "62000"},
                        {"6", "Sophia", "Lee", "Engineering", "68000"},
                        {"7", "Daniel", "White", "Operations", "59000"},
                        {"8", "Olivia", "Harris", "Support", "61000"},
                        {"9", "James", "Clark", "Management", "72000"},
                        {"10", "Emma", "Lewis", "Admin", "53000"}
                    };

                    // Loop to display table rows
                    for (String[] emp : employees) {
                %>
                <tr>
                    <td><%= emp[0] %></td>
                    <td><%= emp[1] %></td>
                    <td><%= emp[2] %></td>
                    <td><%= emp[3] %></td>
                    <td><%= emp[4] %></td>
                </tr>
                <%
                    }
                %>
            </tbody>
        </table>
    </div>

</body>

</html>
<%@ include file="/netmarkets/jsp/util/end.jspf"%>
