package com.example.hw8;

import com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootTest
@Slf4j
class Hw8ApplicationTests {

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void init() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        connection.createStatement().executeUpdate("""
            DROP TABLE IF EXISTS trans_test;
        """);
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS trans_test (
              id int NOT NULL AUTO_INCREMENT,
              number int DEFAULT NULL,
              PRIMARY KEY (id)
            ) ENGINE=InnoDB AUTO_INCREMENT=1
        """);

        connection.commit();
        connection.close();
    }

    Executor executor = Executors.newCachedThreadPool();

    @ParameterizedTest
    @ValueSource(ints = {
            Connection.TRANSACTION_SERIALIZABLE,
            Connection.TRANSACTION_REPEATABLE_READ,
            Connection.TRANSACTION_READ_COMMITTED,
            Connection.TRANSACTION_READ_UNCOMMITTED
    })
    public void testDirtyRead(int isolation) throws SQLException {

        Connection connection0 = dataSource.getConnection();
        connection0.setAutoCommit(false);
        connection0.createStatement().executeUpdate("insert into trans_test(number) values (10)");
        connection0.commit();
        connection0.close();

        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        connection1.setAutoCommit(false);
        connection1.setTransactionIsolation(isolation);
        connection2.setAutoCommit(false);
        connection2.setTransactionIsolation(isolation);

        try {
            Integer number = getFirstOrNull(connection1, "Select number from trans_test where id = 1");
            executor.execute(() -> {
                try {
                    connection2.createStatement().executeUpdate("update trans_test set number=25 where id=1");
                    Thread.sleep(1_000);
                    connection2.commit();
                    connection2.close();
                } catch (Exception ex) {
                    log.error("{}", ex);
                }
            });
            Thread.sleep(500);
            Integer numberAgain = getFirstOrNull(connection1, "Select number from trans_test where id = 1");
            log.info("Dirty read check: Isolation: {}, value {} vs. {}", isolationToString(isolation), number, numberAgain);
            connection1.commit();
        } catch (MySQLTransactionRollbackException | InterruptedException ex) {
            log.info("{}", ex);

        }
        connection1.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {
            Connection.TRANSACTION_SERIALIZABLE,
            Connection.TRANSACTION_REPEATABLE_READ,
            Connection.TRANSACTION_READ_COMMITTED,
            Connection.TRANSACTION_READ_UNCOMMITTED
    })
    public void testNonRepeatableRead(int isolation) throws SQLException {

        Connection connection0 = dataSource.getConnection();
        connection0.setAutoCommit(false);
        connection0.createStatement().executeUpdate("insert into trans_test(number) values (10)");
        connection0.commit();
        connection0.close();

        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        connection1.setAutoCommit(false);
        connection1.setTransactionIsolation(isolation);
        connection2.setAutoCommit(false);
        connection2.setTransactionIsolation(isolation);

        try {
            Integer number = getFirstOrNull(connection1, "Select number from trans_test where id = 1");
            executor.execute(() -> {
                                try {
                                    connection2.createStatement().executeUpdate("update trans_test set number=25 where id=1");
                                    connection2.commit();
                                    connection2.close();
                                } catch (Exception e) {
                                    log.error("{}", e);
                                }
                             });
            Thread.sleep(1_000);
            Integer numberAgain = getFirstOrNull(connection1, "Select number from trans_test where id = 1");
            log.info("Unrepeatable read: Isolation: {}, value {} vs. {}", isolationToString(isolation), number, numberAgain);
            connection1.commit();
        } catch (MySQLTransactionRollbackException | InterruptedException ex) {
            log.info("{}", ex);

        }
        connection1.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {
            Connection.TRANSACTION_SERIALIZABLE,
            Connection.TRANSACTION_REPEATABLE_READ,
            Connection.TRANSACTION_READ_COMMITTED,
            Connection.TRANSACTION_READ_UNCOMMITTED
    })
    public void testPhantomRead(int isolation) throws SQLException {

        Connection connection0 = dataSource.getConnection();
        connection0.setAutoCommit(false);
        connection0.createStatement().executeUpdate("insert into trans_test(number) values (10), (15), (20)");
        connection0.commit();
        connection0.close();

        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        connection1.setAutoCommit(false);
        connection1.setTransactionIsolation(isolation);
        connection2.setAutoCommit(false);
        connection2.setTransactionIsolation(isolation);

        try {
            List<Integer> number = getAll(connection1, "Select number from trans_test where number BETWEEN 10 AND 30");
            executor.execute(() -> {
                try {
                    connection2.createStatement().executeUpdate("insert into trans_test(number) values (25)");
                    connection2.commit();
                    connection2.close();
                } catch (Exception e) {
                    log.error("{}", e);
                }
            });
            Thread.sleep(1_000);
            if (isolation == Connection.TRANSACTION_REPEATABLE_READ) {
                // https://stackoverflow.com/questions/5444915/how-to-produce-phantom-reads
                connection1.createStatement().executeUpdate("update trans_test set number=26 where id=4");
            }
            List<Integer> numberAgain = getAll(connection1, "Select number from trans_test where number BETWEEN 10 AND 30");
            log.info("Phantom read: Isolation: {}, value {} vs. {}", isolationToString(isolation), number, numberAgain);
            connection1.commit();
        } catch (MySQLTransactionRollbackException | InterruptedException ex) {
            log.info("{}", ex);

        }
        connection1.close();
    }

    @ParameterizedTest
    @ValueSource(ints = {
            Connection.TRANSACTION_SERIALIZABLE,
            Connection.TRANSACTION_REPEATABLE_READ,
            Connection.TRANSACTION_READ_COMMITTED,
            Connection.TRANSACTION_READ_UNCOMMITTED
    })
    public void lostUpdateTest(int isolation) throws SQLException {
        Connection connection0 = dataSource.getConnection();
        connection0.setAutoCommit(false);
        connection0.createStatement().executeUpdate("insert into trans_test(number) values (10)");
        connection0.commit();
        connection0.close();

        Connection connection = dataSource.getConnection();
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();

        connection1.setAutoCommit(false);
        connection1.setTransactionIsolation(isolation);
        connection2.setAutoCommit(false);
        connection2.setTransactionIsolation(isolation);
        connection.setAutoCommit(false);


        try {
            executor.execute(() -> {
                try {
                    connection1.createStatement().executeUpdate("INSERT INTO trans_test SELECT id, number + 10 + sleep(2) from trans_test where id=1 on duplicate key update number=values(number)");
                    connection1.commit();
                } catch (Exception e) {
                    log.error("{}", e);
                }
            });
            executor.execute(() -> {
                try {
                    connection2.createStatement().executeUpdate("UPDATE trans_test SET number=number + 10 WHERE id=1;");
                    connection2.commit();
                } catch (Exception e) {
                    log.error("{}", e);
                }
            });
            Thread.sleep(5_000);

            Integer numberAgain = getFirstOrNull(connection, "Select number from trans_test where id = 1");

            log.info("Lost update: Isolation: {}, value {} vs. {}", isolationToString(isolation), 30, numberAgain);
        } catch (MySQLTransactionRollbackException | InterruptedException ex) {
            log.info("{}", ex);
        }
        connection2.close();
        connection1.close();
        connection.close();
    }

    private List<Integer> getAll(Connection connection, String sql) throws SQLException {
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        List<Integer> numbers = new ArrayList<>();
        while (resultSet.next()) {
            numbers.add(resultSet.getInt(1));
        }
        return Collections.unmodifiableList(numbers);
    }

    static Integer getFirstOrNull(Connection connection, String sql) throws SQLException {
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        return resultSet.next() ? resultSet.getInt(1) : null;
    }

    static String isolationToString(int isolation) {
        switch (isolation) {
            case 0:
                return "NONE";
            case 1:
                return "READ_UNCOMMITTED";
            case 2:
                return "READ_COMMITTED";
            case 4:
                return "REPEATABLE_READ";
            case 8:
                return "SERIALIZABLE";
        }

        return "UNDEFINED";
    }

}
