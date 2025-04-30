package org.apache.phoenix.expression.function;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.phoenix.expression.Expression;
import org.apache.phoenix.parse.FunctionParseNode;
import org.apache.phoenix.schema.tuple.Tuple;
import org.apache.phoenix.schema.types.PDataType;
import org.apache.phoenix.schema.types.PVarbinary;
import org.apache.phoenix.schema.types.PVarchar;
import org.apache.phoenix.util.ByteUtil;

import java.util.List;

@FunctionParseNode.BuiltInFunction(name=LengthFunction.NAME, args={
        @FunctionParseNode.Argument(allowedTypes={ PVarbinary.class })} )
public class HexQualifierFunction extends ScalarFunction {

    public static final String NAME = "TO_HEX";

    public HexQualifierFunction() { }
    public HexQualifierFunction(List<Expression> children) {
        super(children);
    }

    @Override
    public boolean evaluate(Tuple tuple, ImmutableBytesWritable ptr) {
        Expression arg = getChildren().get(0);
        if (!arg.evaluate(tuple, ptr)) {
            return false;
        }

        if (ptr.getLength() == 0) {
            ptr.set(ByteUtil.EMPTY_BYTE_ARRAY);
            return true;
        }

        byte[] bytes = ptr.copyBytes();
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }

        ptr.set(PVarchar.INSTANCE.toBytes(hex.toString()));
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public PDataType getDataType() {
        return PVarchar.INSTANCE;
    }
}
