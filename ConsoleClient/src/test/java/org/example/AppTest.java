package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.AutoCloseable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AppTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private ResultSetMetaData mockMetaData;

    private AutoCloseable closeable; // Объявляем переменную типа AutoCloseable

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this); // Инициализируем моки
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close(); // Закрываем после всех тестов
    }

    @Test
    public void testLoadProperties() throws Exception {
        Properties props = App.loadProperties();

        assertNotNull(props, "Конфигурация не должна быть null");
        assertTrue(props.containsKey("url"), "Отсутствует URL БД");
        assertTrue(props.containsKey("username"), "Отсутствует username");
        assertTrue(props.containsKey("password"), "Отсутствует  password");
    }

    @Test
    public void testMain() throws Exception {
        String input = "SELECT * FROM Users";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        // Запускаем main
        App.main(new String[]{input});

        // Проверяем вывод
        assertTrue(output.toString().contains("Подключение установлено!"));
        assertTrue(output.toString().contains("Введите sql выражение"));

    }

    @Test
    public void testEcxecuteSql_SelectQuery() throws SQLException {
        // Настройка моков
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);

        when(mockMetaData.getColumnCount()).thenReturn(2);
        when(mockMetaData.getColumnName(1)).thenReturn("id");
        when(mockMetaData.getColumnName(2)).thenReturn("name");

        when(mockResultSet.next())
                .thenReturn(true)
                .thenReturn(false);
        when(mockResultSet.getObject(1)).thenReturn(1);
        when(mockResultSet.getObject(2)).thenReturn("Test User");

        // Вызов тестируемого метода
        App.executeSql(mockConnection, "SELECT * FROM Users");

        // Проверки
        verify(mockStatement).executeQuery(contains("SELECT")); // Убедимся, что запрос выполнен
        verify(mockResultSet, atLeastOnce()).next(); // Проверяем, что данные читались
    }

    @Test
    public void testExecuteSql_InsertQuery() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeUpdate(anyString())).thenReturn(1);

        App.executeSql(mockConnection, "INSERT INTO Users VALUES (1, 'Alice'");

        verify(mockStatement).executeUpdate(anyString()); // Проверяем, что executeUpdate() вызван
    }

    @Test
    public void testExecuteSql_SqlExection() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenThrow(new SQLException("Синтаксическая ошибка"));

        // Проверяем, что метод не бросает исключение (ошибка обрабатывается внутри)
        assertDoesNotThrow(() -> App.executeSql(mockConnection, "SELECT * FROM invalide_table"));
    }

    @Test
    public void testExecuteSql_SelectWithLimit() throws SQLException {
        // Настройка моков
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);

        // Задаём имена для всех колонок
        when(mockMetaData.getColumnCount()).thenReturn(3);
        when(mockMetaData.getColumnName(1)).thenReturn("id");
        when(mockMetaData.getColumnName(2)).thenReturn("nameButTheLineIsLongerThan25symbols");
        when(mockMetaData.getColumnName(3)).thenReturn(null);

        when(mockResultSet.next())
                .thenReturn(true)  // Первая строка
                .thenReturn(false); // Конец данных
        when(mockResultSet.getObject(1)).thenReturn(1);
        when(mockResultSet.getObject(2)).thenReturn("Test User");
        when(mockResultSet.getObject(3)).thenReturn("valueButTheLineIsLongerThan25symbols");

        // Вызов тестируемого метода
        App.executeSql(mockConnection, "SELECT * FROM Users");

        // Вызов тестируемого метода с LIMIT
        App.executeSql(mockConnection, "SELECT * FROM Users LIMIT 2");

        // Проверяем, что в запрос добавился LIMIT 11
        verify(mockStatement, times(2)).executeQuery(contains("LIMIT 11"));
    }

    @Test
    public void testExecuteSql_MoreThan10Records() throws SQLException {
        // Настройка моков
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);

        // Задаём имена колонкам
        when(mockMetaData.getColumnCount()).thenReturn(2);
        when(mockMetaData.getColumnName(1)).thenReturn("id");
        when(mockMetaData.getColumnName(2)).thenReturn("name");

        // Создаём 11 строк
        when(mockResultSet.next())
                .thenReturn(true)  // Первая строка
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false); // Конец данных

        when(mockResultSet.getObject(anyInt())).thenReturn("test_value");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        // Вызов тестируемого метода
        App.executeSql(mockConnection, "SELECT * FROM Users");

        assertTrue(output.toString().contains("В базе данных ещё есть записи"));

    }

}