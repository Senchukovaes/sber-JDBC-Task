package org.example;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.*;
import java.util.Scanner;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App {
        //private static final String jdbcUrl = "jdbc:h2:~/DATA_BASE;INIT=RUNSCRIPT FROM 'classpath:schema.sql'";
        //private static final String username = "sa";
        //private static final String password = "";


    public static void main( String[] args ) throws SQLException {
        try {
            Properties props = loadProperties();

            String url = props.getProperty("url");
            String username = props.getProperty("username");
            String password = props.getProperty("password");

            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                Statement stmt = conn.createStatement();
                Scanner scanner = new Scanner(System.in);
                System.out.print("Подключение установлено! ");
                //System.out.println(DriverManager.getDriver(jdbcUrl));

                while (true) {
                    System.out.println("Введите sql выражение");
                    String sqlExpression = scanner.nextLine();

                    if (sqlExpression.equalsIgnoreCase("QUIT")) break;

                    executeSql(conn, sqlExpression);
                }

            }

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

    }

    static void executeSql(Connection conn, String sqlExp) throws SQLException{
        try(Statement stmt = conn.createStatement()) {

            if (sqlExp.toUpperCase().startsWith("SELECT")) {

                if (sqlExp.toUpperCase().contains("LIMIT")) {
                    sqlExp = sqlExp.replaceAll("LIMIT.*", "");
                }

                sqlExp = sqlExp + " LIMIT 11;";


                try(ResultSet rs = stmt.executeQuery(sqlExp)) {
                    final int COLUMN_WIDTH = 25;

                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    // Выводим заголовки столбцов
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        if (columnName == null) {
                            columnName = "column_" + i;
                        }
                        if (columnName.length() > COLUMN_WIDTH) {
                            columnName = columnName.substring(0, COLUMN_WIDTH - 3) + "...";
                        }
                        String formattedColumnName = String.format("%-" + COLUMN_WIDTH + "s", columnName);
                        System.out.print(formattedColumnName);
                    }
                    System.out.println();

                    // Выводи разделительную строку
                    int k = COLUMN_WIDTH * columnCount;
                    for (int i = 1; i <= k; i++) {
                        System.out.print("-");
                    }
                    System.out.println();

                    int i = 0;
                    while (i < 10 && rs.next()) {
                        for (int j = 1; j <= columnCount; j++) {
                            String value =  (rs.getObject(j) != null) ? rs.getObject(j).toString() : "null";
                            if (value.length() > COLUMN_WIDTH) {
                                value = value.substring(0, COLUMN_WIDTH - 3) + "...";
                            }
                            String formattedValue = String.format("%-" + COLUMN_WIDTH + "s", value);
                            System.out.print(formattedValue);
                        }
                        System.out.println();
                        i++;
                    }
                    if (rs.next()) {
                        System.out.println("В базе данных ещё есть записи");
                    }
                }

            } else {
                stmt.executeUpdate(sqlExp);
            }


        } catch (SQLException e) {
            System.out.println("Ошибка при выполнении SQL: " + e.getMessage());
        }

    }

    static Properties loadProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new FileNotFoundException("Файл конфигурации config.properties не найден");
            }
            props.load(input);
        }
        return props;
    }

}
