package com.flipkart.vitess.jdbc.test;

import com.flipkart.vitess.jdbc.VitessConnection;
import com.flipkart.vitess.jdbc.VitessPreparedStatement;
import com.youtube.vitess.client.Context;
import com.youtube.vitess.client.SQLFuture;
import com.youtube.vitess.client.VTGateConn;
import com.youtube.vitess.client.VTGateTx;
import com.youtube.vitess.client.cursor.Cursor;
import com.youtube.vitess.proto.Query;
import com.youtube.vitess.proto.Topodata;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by harshit.gangal on 09/02/16.
 */
public class VitessPreparedStatementTest {

    private String sqlSelect = "select 1 from test_table";
    private String sqlShow = "show tables";
    private String sqlUpdate = "update test_table set msg = null";

    @Test public void testStatementExecute() throws SQLException {
        VitessConnection mockConn = PowerMockito.mock(VitessConnection.class);
        VitessPreparedStatement preparedStatement;
        try {
            preparedStatement = new VitessPreparedStatement(mockConn, sqlShow);
            preparedStatement.executeQuery(sqlSelect);
            Assert.fail("Should have thrown exception for calling this method");
        } catch (SQLException ex) {
            Assert.assertEquals("This method cannot be called using this class object",
                ex.getMessage());
        }

        try {
            preparedStatement = new VitessPreparedStatement(mockConn, sqlShow);
            preparedStatement.executeUpdate(sqlUpdate);
            Assert.fail("Should have thrown exception for calling this method");
        } catch (SQLException ex) {
            Assert.assertEquals("This method cannot be called using this class object",
                ex.getMessage());
        }

        try {
            preparedStatement = new VitessPreparedStatement(mockConn, sqlShow);
            preparedStatement.execute(sqlShow);
            Assert.fail("Should have thrown exception for calling this method");
        } catch (SQLException ex) {
            Assert.assertEquals("This method cannot be called using this class object",
                ex.getMessage());
        }
    }

    @Test public void testExecuteQuery() throws SQLException {
        VitessConnection mockConn = PowerMockito.mock(VitessConnection.class);
        VTGateConn mockVtGateConn = PowerMockito.mock(VTGateConn.class);
        VTGateTx mockVtGateTx = PowerMockito.mock(VTGateTx.class);
        Cursor mockCursor = PowerMockito.mock(Cursor.class);
        SQLFuture mockSqlFutureCursor = PowerMockito.mock(SQLFuture.class);
        SQLFuture mockSqlFutureVtGateTx = PowerMockito.mock(SQLFuture.class);

        PowerMockito.when(mockConn.getVtGateConn()).thenReturn(mockVtGateConn);
        PowerMockito.when(mockVtGateConn
            .executeKeyspaceIds(Matchers.any(Context.class), Matchers.anyString(),
                Matchers.anyString(), Matchers.anyCollection(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFutureCursor);
        PowerMockito.when(mockVtGateConn
            .execute(Matchers.any(Context.class), Matchers.anyString(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFutureCursor);
        PowerMockito.when(mockConn.getVtGateTx()).thenReturn(null);
        PowerMockito.when(mockVtGateConn.begin(Matchers.any(Context.class)))
            .thenReturn(mockSqlFutureVtGateTx);
        PowerMockito.when(mockVtGateTx
            .execute(Matchers.any(Context.class), Matchers.anyString(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFutureCursor);
        PowerMockito.when(mockSqlFutureCursor.checkedGet()).thenReturn(mockCursor);
        PowerMockito.when(mockSqlFutureVtGateTx.checkedGet()).thenReturn(mockVtGateTx);

        VitessPreparedStatement preparedStatement;
        try {

            //Empty Sql Statement
            try {
                new VitessPreparedStatement(mockConn, "");
                Assert.fail("Should have thrown exception for empty sql");
            } catch (SQLException ex) {
                Assert.assertEquals("SQL statement is not valid", ex.getMessage());
            }

            //show query
            preparedStatement = new VitessPreparedStatement(mockConn, sqlShow);
            ResultSet rs = preparedStatement.executeQuery();
            Assert.assertEquals(-1, preparedStatement.getUpdateCount());

            //select on replica with bind variables
            preparedStatement =
                new VitessPreparedStatement(mockConn, sqlSelect, ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY, true);
            PowerMockito.when(mockConn.getTabletType()).thenReturn(Topodata.TabletType.REPLICA);
            rs = preparedStatement.executeQuery();
            Assert.assertEquals(-1, preparedStatement.getUpdateCount());

            //select on replica without bind variables
            preparedStatement =
                new VitessPreparedStatement(mockConn, sqlSelect, ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
            rs = preparedStatement.executeQuery();
            Assert.assertEquals(-1, preparedStatement.getUpdateCount());


            //select on master
            PowerMockito.when(mockConn.getTabletType()).thenReturn(Topodata.TabletType.MASTER);
            rs = preparedStatement.executeQuery();
            Assert.assertEquals(-1, preparedStatement.getUpdateCount());

            try {
                //when returned cursor is null
                PowerMockito.when(mockSqlFutureCursor.checkedGet()).thenReturn(null);
                preparedStatement.executeQuery();
                Assert.fail("Should have thrown exception for cursor null");
            } catch (SQLException ex) {
                Assert.assertEquals("Failed to execute this method", ex.getMessage());
            }

        } catch (SQLException e) {
            Assert.fail("Test failed " + e.getMessage());
        }
    }

    @Test public void testExecuteUpdate() throws SQLException {
        VitessConnection mockConn = PowerMockito.mock(VitessConnection.class);
        VTGateConn mockVtGateConn = PowerMockito.mock(VTGateConn.class);
        VTGateTx mockVtGateTx = PowerMockito.mock(VTGateTx.class);
        Cursor mockCursor = PowerMockito.mock(Cursor.class);
        SQLFuture mockSqlFutureCursor = PowerMockito.mock(SQLFuture.class);
        SQLFuture mockSqlFutureVtGateTx = PowerMockito.mock(SQLFuture.class);
        List<Query.Field> fieldList = new ArrayList<>();

        PowerMockito.when(mockConn.getVtGateConn()).thenReturn(mockVtGateConn);
        PowerMockito.when(mockConn.getVtGateTx()).thenReturn(mockVtGateTx);
        PowerMockito.when(mockVtGateTx
            .execute(Matchers.any(Context.class), Matchers.anyString(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFutureCursor);
        PowerMockito.when(mockVtGateConn
            .execute(Matchers.any(Context.class), Matchers.anyString(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFutureCursor);
        PowerMockito.when(mockVtGateConn
            .executeKeyspaceIds(Matchers.any(Context.class), Matchers.anyString(),
                Matchers.anyString(), Matchers.anyCollection(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFutureCursor);
        PowerMockito.when(mockSqlFutureCursor.checkedGet()).thenReturn(mockCursor);
        PowerMockito.when(mockSqlFutureVtGateTx.checkedGet()).thenReturn(mockVtGateTx);
        PowerMockito.when(mockCursor.getFields()).thenReturn(null);

        VitessPreparedStatement preparedStatement =
            new VitessPreparedStatement(mockConn, sqlUpdate);
        try {

            //exception on executing dml on non master
            PowerMockito.when(mockConn.getTabletType()).thenReturn(Topodata.TabletType.REPLICA);
            try {
                preparedStatement.executeUpdate();
                Assert.fail("Should have thrown exception for tablet type not being master");
            } catch (SQLException ex) {
                Assert.assertEquals("DML Statement cannot be executed on non master instance type",
                    ex.getMessage());
            }

            //executing dml on master
            PowerMockito.when(mockConn.getTabletType()).thenReturn(Topodata.TabletType.MASTER);
            preparedStatement =
                new VitessPreparedStatement(mockConn, sqlUpdate, ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY, true);
            int updateCount = preparedStatement.executeUpdate();
            Assert.assertEquals(0, updateCount);

            //tx is null & autoCommit is true
            PowerMockito.when(mockConn.getVtGateTx()).thenReturn(null);
            PowerMockito.when(mockVtGateConn.begin(Matchers.any(Context.class)))
                .thenReturn(mockSqlFutureVtGateTx);
            PowerMockito.when(mockConn.getAutoCommit()).thenReturn(true);
            PowerMockito.when(mockVtGateTx.commit(Matchers.any(Context.class)))
                .thenReturn(mockSqlFutureCursor);
            preparedStatement = new VitessPreparedStatement(mockConn, sqlUpdate);
            updateCount = preparedStatement.executeUpdate();
            Assert.assertEquals(0, updateCount);

            //cursor fields is not null
            PowerMockito.when(mockCursor.getFields()).thenReturn(fieldList);
            try {
                preparedStatement.executeUpdate();
                Assert.fail("Should have thrown exception for field not null");
            } catch (SQLException ex) {
                Assert.assertEquals("ResultSet generation is not allowed through this method",
                    ex.getMessage());
            }

            //cursor is null
            PowerMockito.when(mockSqlFutureCursor.checkedGet()).thenReturn(null);
            try {
                preparedStatement.executeUpdate();
                Assert.fail("Should have thrown exception for cursor null");
            } catch (SQLException ex) {
                Assert.assertEquals("Failed to execute this method", ex.getMessage());
            }

        } catch (SQLException e) {
            Assert.fail("Test failed " + e.getMessage());
        }
    }

    @Test public void testExecute() throws SQLException {
        VitessConnection mockConn = PowerMockito.mock(VitessConnection.class);
        VTGateConn mockVtGateConn = PowerMockito.mock(VTGateConn.class);
        VTGateTx mockVtGateTx = PowerMockito.mock(VTGateTx.class);
        Cursor mockCursor = PowerMockito.mock(Cursor.class);
        SQLFuture mockSqlFutureCursor = PowerMockito.mock(SQLFuture.class);
        SQLFuture mockSqlFutureVtGateTx = PowerMockito.mock(SQLFuture.class);
        List<Query.Field> mockFieldList = PowerMockito.mock(ArrayList.class);

        PowerMockito.when(mockConn.getVtGateConn()).thenReturn(mockVtGateConn);
        PowerMockito.when(mockConn.getTabletType()).thenReturn(Topodata.TabletType.MASTER);
        PowerMockito.when(mockVtGateConn
            .streamExecute(Matchers.any(Context.class), Matchers.anyString(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockCursor);
        PowerMockito.when(mockVtGateConn
            .executeKeyspaceIds(Matchers.any(Context.class), Matchers.anyString(),
                Matchers.anyString(), Matchers.anyCollection(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFutureCursor);
        PowerMockito.when(mockConn.getVtGateTx()).thenReturn(null);
        PowerMockito.when(mockVtGateConn.begin(Matchers.any(Context.class)))
            .thenReturn(mockSqlFutureVtGateTx);

        PowerMockito.when(mockVtGateTx
            .execute(Matchers.any(Context.class), Matchers.anyString(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFutureCursor);
        PowerMockito.when(mockConn.getAutoCommit()).thenReturn(true);
        PowerMockito.when(mockVtGateTx.commit(Matchers.any(Context.class)))
            .thenReturn(mockSqlFutureCursor);

        PowerMockito.when(mockSqlFutureCursor.checkedGet()).thenReturn(mockCursor);
        PowerMockito.when(mockSqlFutureVtGateTx.checkedGet()).thenReturn(mockVtGateTx);
        PowerMockito.when(mockCursor.getFields()).thenReturn(mockFieldList);

        VitessPreparedStatement preparedStatement =
            new VitessPreparedStatement(mockConn, sqlSelect, ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY, true);
        try {

            int fieldSize = 5;
            PowerMockito.when(mockCursor.getFields()).thenReturn(mockFieldList);
            PowerMockito.when(mockFieldList.size()).thenReturn(fieldSize);
            boolean hasResultSet = preparedStatement.execute();
            Assert.assertTrue(hasResultSet);
            Assert.assertNotNull(preparedStatement.getResultSet());

            preparedStatement = new VitessPreparedStatement(mockConn, sqlSelect);
            hasResultSet = preparedStatement.execute();
            Assert.assertTrue(hasResultSet);
            Assert.assertNotNull(preparedStatement.getResultSet());

            int mockUpdateCount = 10;
            PowerMockito.when(mockCursor.getFields()).thenReturn(null);
            PowerMockito.when(mockCursor.getRowsAffected()).thenReturn((long) mockUpdateCount);
            preparedStatement = new VitessPreparedStatement(mockConn, sqlUpdate);
            hasResultSet = preparedStatement.execute();
            Assert.assertFalse(hasResultSet);
            Assert.assertNull(preparedStatement.getResultSet());
            Assert.assertEquals(mockUpdateCount, preparedStatement.getUpdateCount());

            //cursor is null
            PowerMockito.when(mockSqlFutureCursor.checkedGet()).thenReturn(null);
            try {
                preparedStatement = new VitessPreparedStatement(mockConn, sqlShow);
                preparedStatement.execute();
                Assert.fail("Should have thrown exception for cursor null");
            } catch (SQLException ex) {
                Assert.assertEquals("Failed to execute this method", ex.getMessage());
            }

        } catch (SQLException e) {
            Assert.fail("Test failed " + e.getMessage());
        }
    }

    @Test public void testGetUpdateCount() throws SQLException {
        VitessConnection mockConn = PowerMockito.mock(VitessConnection.class);
        VTGateConn mockVtGateConn = PowerMockito.mock(VTGateConn.class);
        VTGateTx mockVtGateTx = PowerMockito.mock(VTGateTx.class);
        Cursor mockCursor = PowerMockito.mock(Cursor.class);
        SQLFuture mockSqlFuture = PowerMockito.mock(SQLFuture.class);

        PowerMockito.when(mockConn.getVtGateConn()).thenReturn(mockVtGateConn);
        PowerMockito.when(mockConn.getTabletType()).thenReturn(Topodata.TabletType.MASTER);
        PowerMockito.when(mockConn.getVtGateTx()).thenReturn(mockVtGateTx);
        PowerMockito.when(mockVtGateTx
            .execute(Matchers.any(Context.class), Matchers.anyString(), Matchers.anyMap(),
                Matchers.any(Topodata.TabletType.class))).thenReturn(mockSqlFuture);
        PowerMockito.when(mockSqlFuture.checkedGet()).thenReturn(mockCursor);
        PowerMockito.when(mockCursor.getFields()).thenReturn(null);

        VitessPreparedStatement preparedStatement =
            new VitessPreparedStatement(mockConn, sqlSelect);
        try {

            PowerMockito.when(mockCursor.getRowsAffected()).thenReturn(10L);
            int updateCount = preparedStatement.executeUpdate();
            Assert.assertEquals(10L, updateCount);
            Assert.assertEquals(10L, preparedStatement.getUpdateCount());

            // Truncated Update Count
            PowerMockito.when(mockCursor.getRowsAffected())
                .thenReturn((long) Integer.MAX_VALUE + 10);
            updateCount = preparedStatement.executeUpdate();
            Assert.assertEquals(Integer.MAX_VALUE, updateCount);
            Assert.assertEquals(Integer.MAX_VALUE, preparedStatement.getUpdateCount());

            preparedStatement.executeQuery();
            Assert.assertEquals(-1, preparedStatement.getUpdateCount());

        } catch (SQLException e) {
            Assert.fail("Test failed " + e.getMessage());
        }
    }

    @Test public void testSetParameters() throws Exception {
        VitessConnection mockConn = PowerMockito.mock(VitessConnection.class);
        VitessPreparedStatement preparedStatement =
            new VitessPreparedStatement(mockConn, sqlSelect);
        Boolean boolValue = Boolean.TRUE;
        Byte byteValue = Byte.MAX_VALUE;
        Short shortValue = Short.MAX_VALUE;
        Integer intValue = Integer.MAX_VALUE;
        Long longValue = Long.MAX_VALUE;
        Float floatValue = Float.MAX_VALUE;
        Double doubleValue = Double.MAX_VALUE;
        BigDecimal bigDecimalValue = BigDecimal.TEN;
        String stringValue = "vitess";
        byte[] bytesValue = stringValue.getBytes();
        Date dateValue = new Date(0);
        Time timeValue = new Time(0);
        Timestamp timestampValue = new Timestamp(0);


        preparedStatement.setNull(1, Types.INTEGER);
        preparedStatement.setBoolean(2, boolValue);
        preparedStatement.setByte(3, byteValue);
        preparedStatement.setShort(4, shortValue);
        preparedStatement.setInt(5, intValue);
        preparedStatement.setLong(6, longValue);
        preparedStatement.setFloat(7, floatValue);
        preparedStatement.setDouble(8, doubleValue);
        preparedStatement.setBigDecimal(9, bigDecimalValue);
        preparedStatement.setString(10, stringValue);
        preparedStatement.setBytes(11, bytesValue);
        preparedStatement.setDate(12, dateValue);
        preparedStatement.setTime(13, timeValue);
        preparedStatement.setTimestamp(14, timestampValue);
        preparedStatement.setDate(15, dateValue, Calendar.getInstance(TimeZone.getDefault()));
        preparedStatement.setTime(16, timeValue, Calendar.getInstance(TimeZone.getDefault()));
        preparedStatement
            .setTimestamp(17, timestampValue, Calendar.getInstance(TimeZone.getDefault()));
        preparedStatement.setObject(18, boolValue);
        preparedStatement.setObject(19, byteValue);
        preparedStatement.setObject(20, shortValue);
        preparedStatement.setObject(21, intValue);
        preparedStatement.setObject(22, longValue);
        preparedStatement.setObject(23, floatValue);
        preparedStatement.setObject(24, doubleValue);
        preparedStatement.setObject(25, bigDecimalValue);
        preparedStatement.setObject(26, stringValue);
        preparedStatement.setObject(27, dateValue);
        preparedStatement.setObject(28, timeValue);
        preparedStatement.setObject(29, timestampValue);
        preparedStatement.setObject(30, 'a');
        preparedStatement.setObject(31, null);
        try {
            preparedStatement.setObject(32, bytesValue);
            Assert.fail("Shown have thrown exception for not able to set byte[] parameter");
        } catch (SQLException ex) {
            Assert.assertEquals("Cannot infer the SQL type to use for an instance of byte[]",
                ex.getMessage());
        }
        preparedStatement.clearParameters();
    }
}
