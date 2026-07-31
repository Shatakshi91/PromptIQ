package com.PromptIQ.backend.embedding.entity;
import com.pgvector.PGvector;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;

public class VectorType implements UserType<float[]> {

    @Override
    public int getSqlType() { return Types.OTHER; }

    @Override
    public Class<float[]> returnedClass() { return float[].class; }

    @Override
    public boolean equals(float[] x, float[] y) { return java.util.Arrays.equals(x, y); }

    @Override
    public int hashCode(float[] x) { return java.util.Arrays.hashCode(x); }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        Object obj = rs.getObject(position);
        if (obj == null) return null;
        return new PGvector(obj.toString()).toArray();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index, SharedSessionContractImplementor session) throws SQLException {
        st.setObject(index, value == null ? null : new PGvector(value));
    }

    @Override
    public float[] deepCopy(float[] value) { return value == null ? null : value.clone(); }

    @Override
    public boolean isMutable() { return true; }

    @Override
    public Serializable disassemble(float[] value) { return deepCopy(value); }

    @Override
    public float[] assemble(Serializable cached, Object owner) { return deepCopy((float[]) cached); }
}