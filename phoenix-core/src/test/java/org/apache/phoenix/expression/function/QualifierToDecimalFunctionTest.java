package org.apache.phoenix.expression.function;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.phoenix.expression.Expression;
import org.apache.phoenix.expression.LiteralExpression;
import org.apache.phoenix.schema.SortOrder;
import org.apache.phoenix.schema.types.PVarbinary;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class QualifierToDecimalFunctionTest {

    private static Integer evaluateExpression(byte[] value) throws SQLException {
        Expression inputArg = LiteralExpression.newConstant(value, PVarbinary.INSTANCE, SortOrder.ASC);
        List<Expression> args = Collections.<Expression>singletonList(inputArg);
        Expression func = new QualifierToDecimalFunction(args);
        ImmutableBytesWritable ptr = new ImmutableBytesWritable();
        boolean success = func.evaluate(null, ptr);
        if (!success || ptr.getLength() == 0) {
            return null;
        }
        Object obj = func.getDataType().toObject(ptr);
        return (obj instanceof Integer) ? (Integer) obj : null;
    }

    /**
     * Asserts that the UDF returns the expected decimal ordinal for the given bytes.
     */
    private static void assertDecimal(byte[] value, int expected) throws SQLException {
        Integer result = evaluateExpression(value);
        assertNotNull("Expected non-null for input: " + java.util.Arrays.toString(value), result);
        assertEquals("Decimal mismatch for input: " + java.util.Arrays.toString(value),
                expected, result.intValue());
    }

    /**
     * Asserts that the UDF returns null for the given input (null or empty).
     */
    private static void assertNullResult(byte[] value) throws SQLException {
        Integer result = evaluateExpression(value);
        assertNull("Expected null for input: " + java.util.Arrays.toString(value), result);
    }

    @Test
    public void testSingleByteQualifiers() throws SQLException {
        // 1-byte qualifiers (no marker)
        assertDecimal(new byte[]{0x00}, 0);
        assertDecimal(new byte[]{0x01}, 1);
        assertDecimal(new byte[]{0x0A}, 10);
        assertDecimal(new byte[]{(byte) 0x7F}, 127);
        assertDecimal(new byte[]{(byte) 0xFF}, 255);
    }

    @Test
    public void testTwoByteWithMarker() throws SQLException {
        // marker 0x80 + ordinal
        assertDecimal(new byte[]{(byte) 0x80, 0x0B}, 11);
        assertDecimal(new byte[]{(byte) 0x80, 0x0C}, 12);
        assertDecimal(new byte[]{(byte) 0x80, (byte) 0xFF}, 255);
        // marker but second byte zero
        assertDecimal(new byte[]{(byte) 0x80, 0x00}, 0);
    }

    @Test
    public void testMultiByteBigEndian() throws SQLException {
        // no marker pattern: interpret all bytes big-endian
        // e.g. [0x01, 0x02] => 0x0102 = 258
        assertDecimal(new byte[]{0x01, 0x02}, 258);
        // [0x00, 0x01, 0x00] => 0x000100 = 256
        assertDecimal(new byte[]{0x00, 0x01, 0x00}, 256);
        // [0x01, 0x00, 0x01] => 0x010001 = 65537
        assertDecimal(new byte[]{0x01, 0x00, 0x01}, 65537);
    }

    @Test
    public void testNullAndEmpty() throws SQLException {
        // null input
        assertNullResult(null);
        // empty array treated as null/no-value
        assertNullResult(new byte[0]);
    }

    @Test
    public void testRandomExamples() throws SQLException {
        // mixed examples
        byte[] a = new byte[]{(byte) 0x80, 0x10};  // marker + 16
        assertDecimal(a, 16);

        byte[] b = new byte[]{0x01, (byte) 0x80, 0x0F}; // big-endian: 0x01800F = 98319
        assertDecimal(b, (1 << 16) | (0x80 << 8) | 0x0F);

        byte[] c = new byte[]{(byte) 0xA5}; // single byte 165
        assertDecimal(c, 0xA5);
    }
}
