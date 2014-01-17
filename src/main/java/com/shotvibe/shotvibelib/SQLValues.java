package com.shotvibe.shotvibelib;

import java.util.Iterator;

public final class SQLValues implements Iterable<SQLValues.Val> {

    public enum Type {
        NULL,
        INT,
        LONG,
        DOUBLE,
        STRING
    }

    public static final class Val {
        /**
         * NULL value
         */
        private Val() {
            mType = Type.NULL;
        }

        private Val(int intValue) {
            mType = Type.INT;
            mIntValue = intValue;
        }

        private Val(long longValue) {
            mType = Type.LONG;
            mLongValue = longValue;
        }

        private Val(double doubleValue) {
            mType = Type.DOUBLE;
            mDoubleValue = doubleValue;
        }
        private Val(String stringValue) {
            mType = Type.STRING;
            mStringValue = stringValue;
        }

        private Type mType;
        private int mIntValue;
        private long mLongValue;
        private double mDoubleValue;
        private String mStringValue;

        public Type getType() {
            return mType;
        }

        public int getIntValue() {
            if (getType() != Type.INT) {
                throw new IllegalArgumentException("Tried to get as INT when type is: " + getType());
            }

            return mIntValue;
        }

        public long getLongValue() {
            if (getType() != Type.LONG) {
                throw new IllegalArgumentException("Tried to get as LONG when type is: " + getType());
            }

            return mLongValue;
        }

        public double getDoubleValue() {
            if (getType() != Type.DOUBLE) {
                throw new IllegalArgumentException("Tried to get as DOUBLE when type is: " + getType());
            }

            return mDoubleValue;
        }

        public String getStringValue() {
            if (getType() != Type.STRING) {
                throw new IllegalArgumentException("Tried to get as STRING when type is: " + getType());
            }

            return mStringValue;
        }

    }

    private SQLValues() {
        final int INITIAL_CAPACITY = 15;

        mVals = new ArrayList<Val>(INITIAL_CAPACITY);
    }

    public static SQLValues create() {
        return new SQLValues();
    }

    public SQLValues addNull() {
        mVals.add(new Val());
        return this;
    }

    public SQLValues add(int intValue) {
        mVals.add(new Val(intValue));
        return this;
    }

    public SQLValues add(long longValue) {
        mVals.add(new Val(longValue));
        return this;
    }

    public SQLValues add(double doubleValue) {
        mVals.add(new Val(doubleValue));
        return this;
    }

    public SQLValues add(String stringValue) {
        if (stringValue == null) {
            throw new IllegalArgumentException("stringValue cannot be null");
        }

        mVals.add(new Val(stringValue));
        return this;
    }

    public SQLValues addNullable(Integer nullableIntValue) {
        if (nullableIntValue == null) {
            return addNull();
        } else {
            return add(nullableIntValue.intValue());
        }
    }

    public SQLValues addNullable(Long nullableLongValue) {
        if (nullableLongValue == null) {
            return addNull();
        } else {
            return add(nullableLongValue.longValue());
        }
    }

    public SQLValues addNullable(Double nullableDoubleValue) {
        if (nullableDoubleValue == null) {
            return addNull();
        } else {
            return add(nullableDoubleValue.doubleValue());
        }
    }

    public SQLValues addNullable(String nullableStringValue) {
        if (nullableStringValue == null) {
            return addNull();
        } else {
            return add(nullableStringValue);
        }
    }

    @Override
    public Iterator<Val> iterator() {
        return mVals.iterator();
    }

    public int size() {
        return mVals.size();
    }

    private ArrayList<Val> mVals;

    /*-[

    - (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state objects:(__unsafe_unretained id *)stackbuf count:(NSUInteger)len {
        return [mVals_ countByEnumeratingWithState:state objects:stackbuf count:len];
    }

    ]-*/;
}
