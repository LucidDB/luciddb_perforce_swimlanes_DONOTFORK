/*
// Saffron preprocessor and data engine.
// Copyright (C) 2002-2004 Disruptive Tech
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package net.sf.saffron.jdbc;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import net.sf.saffron.ext.ClassSchema;

import openjava.mop.OJClass;
import openjava.ptree.CastExpression;
import openjava.ptree.Expression;
import openjava.ptree.FieldAccess;

import org.eigenbase.relopt.RelOptConnection;
import org.eigenbase.relopt.RelOptSchema;
import org.eigenbase.util.EnumeratedValues;
import org.eigenbase.util.Util;


/**
 * A <code>SaffronJdbcConnection</code> is a JDBC {@link Connection} to a
 * Saffron database.
 *
 * @author jhyde
 * @version $Id$
 *
 * @since Mar 19, 2003
 */
public class SaffronJdbcConnection implements Connection
{
    public RelOptConnection saffronConnection;

    public SaffronJdbcConnection(
        String url,
        Properties info)
    {
        this();
        initRelOptConnection(url, info);
    }

    protected SaffronJdbcConnection()
    {
    }

    public void setAutoCommit(boolean autoCommit)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean getAutoCommit()
        throws SQLException
    {
        return false;
    }

    public void setCatalog(String catalog)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public String getCatalog()
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isClosed()
        throws SQLException
    {
        return false;
    }

    public void setHoldability(int holdability)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public int getHoldability()
        throws SQLException
    {
        return 0;
    }

    public DatabaseMetaData getMetaData()
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void setReadOnly(boolean readOnly)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isReadOnly()
        throws SQLException
    {
        return false;
    }

    public Savepoint setSavepoint()
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public Savepoint setSavepoint(String name)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void setTransactionIsolation(int level)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public int getTransactionIsolation()
        throws SQLException
    {
        return 0;
    }

    public void setTypeMap(Map map)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public Map getTypeMap()
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public SQLWarning getWarnings()
        throws SQLException
    {
        return null;
    }

    public void clearWarnings()
        throws SQLException
    {
    }

    public void close()
        throws SQLException
    {
    }

    public void commit()
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public Statement createStatement()
        throws SQLException
    {
        return new SaffronJdbcStatement(this);
    }

    public Statement createStatement(
        int resultSetType,
        int resultSetConcurrency)
        throws SQLException
    {
        if (resultSetType != ResultSet.TYPE_FORWARD_ONLY) {
            throw new UnsupportedOperationException();
        }
        if (resultSetConcurrency != ResultSet.CONCUR_READ_ONLY) {
            throw new UnsupportedOperationException();
        }
        return createStatement();
    }

    public Statement createStatement(
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public String nativeSQL(String sql)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public CallableStatement prepareCall(String sql)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public CallableStatement prepareCall(
        String sql,
        int resultSetType,
        int resultSetConcurrency)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public CallableStatement prepareCall(
        String sql,
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(String sql)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(
        String sql,
        int resultSetType,
        int resultSetConcurrency)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(
        String sql,
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(
        String sql,
        int autoGeneratedKeys)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(
        String sql,
        int [] columnIndexes)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public PreparedStatement prepareStatement(
        String sql,
        String [] columnNames)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void releaseSavepoint(Savepoint savepoint)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void rollback()
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void rollback(Savepoint savepoint)
        throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    // New methods in JDBC 4.0

    public Clob createClob() throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public Blob createBlob() throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public NClob createNClob() throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public SQLXML createSQLXML() throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isValid(int timeout) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public void setClientInfo(String name, String value)
        throws SQLClientInfoException
    {
        throw new UnsupportedOperationException();
    }

    public void setClientInfo(Properties properties)
        throws SQLClientInfoException
    {
        throw new UnsupportedOperationException();
    }

    public String getClientInfo(String name) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public Properties getClientInfo() throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public Array createArrayOf(
        String typeName, Object[] elements) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public Struct createStruct(
        String typeName, Object[] attributes) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    // End of JDBC methods

    protected String getUrlPrefix()
    {
        return SaffronJdbcDriver.getUrlPrefixStatic();
    }

    protected void initRelOptConnection(
        String url,
        Properties info)
    {
        Properties properties = parseURL(url, info);
        final String schemaName = properties.getProperty(Property.Schema);
        if (schemaName == null) {
            throw Util.newInternal("Required connection property '"
                + Property.Schema + "' is missing");
        }
        try {
            final Class clazz = Class.forName(schemaName);
            Object o = clazz.newInstance();
            if (o instanceof RelOptConnection) {
                saffronConnection = (RelOptConnection) o;
            } else if (o instanceof RelOptSchema) {
                saffronConnection = new MyConnection(o, (RelOptSchema) o);
            } else {
                final ClassSchema schema =
                    new ClassSchema(clazz, false) {
                        protected Expression getTarget(
                            Expression connectionExp)
                        {
                            return new CastExpression(
                                OJClass.forClass(clazz),
                                new FieldAccess(connectionExp, "target"));
                        }
                    };
                saffronConnection = new MyConnection(o, schema);
            }
        } catch (Throwable e) {
            throw Util.newInternal(e, "Error while connecting to " + url);
        }
    }

    /**
     * Parses the URL to get connection properties, then overrides with the
     * supplied properties set.
     */
    private Properties parseURL(
        String url,
        Properties info)
    {
        Properties properties = new Properties();
        assert (url.startsWith(getUrlPrefix()));
        String s = url.substring(getUrlPrefix().length());
        final StringTokenizer stringTokenizer = new StringTokenizer(s, "&");
        while (stringTokenizer.hasMoreTokens()) {
            final String tokenValue = stringTokenizer.nextToken();
            final int equals = tokenValue.indexOf("=");
            String token;
            String value;
            if (equals < 0) {
                token = tokenValue;
                value = null;
            } else {
                token = tokenValue.substring(0, equals);
                value = tokenValue.substring(equals + 1);
            }
            properties.setProperty(token, value);
        }
        properties.putAll(info);
        return properties;
    }

    public static class MyConnection implements RelOptConnection
    {
        public final Object target;
        private final RelOptSchema schema;

        public MyConnection(
            Object target,
            RelOptSchema schema)
        {
            this.target = target;
            this.schema = schema;
        }

        public RelOptSchema getRelOptSchema()
        {
            return schema;
        }

        public Object contentsAsArray(
            String qualifier,
            String tableName)
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class Property extends EnumeratedValues
    {
        public static final Property instance = new Property();

        /**
         * {@value} is the name of a schema class.
         *
         * <p>
         * The class must have a zero-argument constructor. If it the class
         * implements {@link RelOptSchema}, the fields are deduced using
         * {@link RelOptSchema#getTableForMember}; otherwise, the fields are
         * deduced using reflection.
         * </p>
         */
        public static final String Schema = "schema";

        private Property()
        {
            super(new String [] { Schema });
        }
    }
}


// End SaffronJdbcConnection.java
