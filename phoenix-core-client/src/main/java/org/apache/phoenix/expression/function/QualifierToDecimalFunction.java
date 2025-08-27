package org.apache.phoenix.expression.function;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.phoenix.expression.Expression;
import org.apache.phoenix.parse.FunctionParseNode;
import org.apache.phoenix.schema.tuple.Tuple;
import org.apache.phoenix.schema.types.PDataType;
import org.apache.phoenix.schema.types.PInteger;
import org.apache.phoenix.schema.types.PVarbinary;

import java.util.List;

@FunctionParseNode.BuiltInFunction(
        name = QualifierToDecimalFunction.NAME,
        args = {@FunctionParseNode.Argument(allowedTypes={PVarbinary.class})}
)
public class QualifierToDecimalFunction extends ScalarFunction {

    public static final String NAME = "QUALIFIER_TO_DECIMAL";


    public QualifierToDecimalFunction() { }

    public QualifierToDecimalFunction(List<Expression> children) {
        super(children);
    }

    @Override
    public boolean evaluate(Tuple tuple, ImmutableBytesWritable ptr) {
        Expression arg = getChildren().get(0);
        if (!arg.evaluate(tuple, ptr) || ptr.getLength() == 0) {
            return false;
        }

        byte[] bytes = ptr.copyBytes();
        int ordinal;
        if (bytes.length == 1) {
            ordinal = bytes[0] & 0xFF;
        } else if ((bytes[0] & 0x80) != 0) {
            int high = bytes[0] & 0x7F;
            int low  = bytes[1] & 0xFF;
            ordinal = (high << 8) | low;
        } else {
            ordinal = 0;
            for (byte b : bytes) {
                ordinal = (ordinal << 8) | (b & 0xFF);
            }
        }

        ptr.set(PInteger.INSTANCE.toBytes(ordinal));
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public PDataType getDataType() {
        return PInteger.INSTANCE;
    }
}
