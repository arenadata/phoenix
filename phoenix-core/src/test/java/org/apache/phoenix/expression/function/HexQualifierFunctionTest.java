package org.apache.phoenix.expression.function;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.phoenix.expression.Expression;
import org.apache.phoenix.expression.LiteralExpression;
import org.apache.phoenix.schema.SortOrder;

import org.apache.phoenix.schema.types.PVarbinary;
import org.junit.Test;

public class HexQualifierFunctionTest {
    /**
     * Helper that creates a VARBINARY literal, evaluates the UDF,
     * and returns the result as a Java object (String).
     */
    private static Object evaluateExpression(byte[] value) throws SQLException {
        Expression inputArg = LiteralExpression.newConstant(value, PVarbinary.INSTANCE, SortOrder.ASC);
        List<Expression> args = Collections.<Expression>singletonList(inputArg);
        Expression func = new HexQualifierFunction(args);
        ImmutableBytesWritable ptr = new ImmutableBytesWritable();
        boolean success = func.evaluate(null, ptr);
        if (!success) {
            return null;
        }
        return func.getDataType().toObject(ptr);
    }

    /** Asserts that the UDF returns the expected HEX string for the given byte array. */
    private static void assertHex(byte[] value, String expectedHex) throws SQLException {
        Object result = evaluateExpression(value);
        assertNotNull("Expected non-null result for input: " + java.util.Arrays.toString(value), result);
        assertEquals("Hex output mismatch", expectedHex, result);
    }

    /** Asserts that the UDF returns null for the given input (e.g. null or empty). */
    private static void assertNullHex(byte[] value) throws SQLException {
        Object result = evaluateExpression(value);
        assertNull("Expected null for input: " + java.util.Arrays.toString(value), result);
    }

    @Test
    public void testHexQualifierFunction() throws SQLException {
        // Single-byte inputs
        assertHex(new byte[]{0x00}, "00");
        assertHex(new byte[]{0x01}, "01");
        assertHex(new byte[]{0x7F}, "7F");
        assertHex(new byte[]{(byte)0xFF}, "FF");

        // Multi-byte inputs
        assertHex(new byte[]{0x00, 0x0A}, "000A");
        assertHex(new byte[]{(byte)0xAB, (byte)0xCD, (byte)0xEF}, "ABCDEF");

        // Mixed values
        assertHex(new byte[]{0x12, (byte)0x80, 0x0F}, "12800F");

        // Null input should produce null
        assertNullHex(null);

        // Empty array treated as null/empty
        assertNullHex(new byte[0]);
    }
}
