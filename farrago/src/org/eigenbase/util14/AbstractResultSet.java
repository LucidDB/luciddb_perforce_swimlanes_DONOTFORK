/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2006 The Eigenbase Project
// Copyright (C) 2002-2006 Disruptive Tech
// Copyright (C) 2005-2006 LucidEra, Inc.
// Portions Copyright (C) 2003-2006 John V. Sichi
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
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

package org.eigenbase.util14;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;

/**
 * <code>AbstractResultSet</code> provides a abstract implementation
 * for a TYPE_FORWARD_ONLY, CONCUR_READ_ONLY ResultSet.
 *
 * @author angel
 * @version $Id$
 * @since Jan 8, 2006
 */
abstract public class AbstractResultSet implements ResultSet
{
    protected boolean wasNull;
    protected int fetchSize = 0;

    /**
     * Returns the raw value of a column as an object.
     */
    abstract protected Object getRaw(int columnIndex) throws SQLException;

    protected Object getRaw(String columnName)
        throws SQLException
    {
        return getRaw(findColumn(columnName));
    }

    public int getConcurrency() throws SQLException
    {
        return ResultSet.CONCUR_READ_ONLY;
    }

    public int getType() throws SQLException
    {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    public void setFetchDirection(int direction)
        throws SQLException
    {
        if (direction != FETCH_FORWARD) {
            throw newDirectionError();
        }
    }

    public int getFetchDirection()
        throws SQLException
    {
        return FETCH_FORWARD;
    }

    public int getFetchSize() throws SQLException
    {
        return fetchSize;
    }

    public void setFetchSize(int rows) throws SQLException
    {
        fetchSize = rows;
    }

    /**
     * A column may have the value of SQL NULL; wasNull reports whether
     * the last column read had this special value.
     * Note that you must first call getXXX on a column to try to read
     * its value and then call wasNull() to find if the value was
     * the SQL NULL.
     *
     * @return true if last column read was SQL NULL
     */
    public boolean wasNull()
        throws SQLException
    {
        return wasNull;
    }

    /**
     * Map a Resultset column name to a ResultSet column index.
     *
     * @param columnName the name of the column
     * @return the column index
     */
    public int findColumn(String columnName)
        throws SQLException
    {
        ResultSetMetaData metaData = getMetaData();
        int n = metaData.getColumnCount();
        for (int i = 1; i <= n; i++) {
            if (columnName.equals(metaData.getColumnName(i))) {
                return i;
            }
        }
        throw new SQLException("column '" + columnName + "' not found");
    }

    /**
     * <p>The first warning reported by calls on this ResultSet is
     * returned. Subsequent ResultSet warnings will be chained to this
     * SQLWarning.
     *
     * <P>The warning chain is automatically cleared each time a new
     * row is read.
     *
     * <P><B>Note:</B> This warning chain only covers warnings caused
     * by ResultSet methods.  Any warning caused by statement methods
     * (such as reading OUT parameters) will be chained on the
     * Statement object.
     *
     * @return the first SQLWarning or null
     */
    public SQLWarning getWarnings()
        throws SQLException
    {
        return null;
    }

    /**
     * After this call getWarnings returns null until a new warning is
     * reported for this ResultSet.
     */
    public void clearWarnings()
        throws SQLException
    {
    }

    public Statement getStatement()
        throws SQLException
    {
        return null;
    }

    public void close()
        throws SQLException
    {
    }

    //======================================================================
    // Individual accessors for columns by name or number
    //======================================================================

    /**
     * Get the value of a column in the current row as a Java String.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public String getString(int columnIndex) throws SQLException
    {
        return toString(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a Java String.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public String getString(String columnName) throws SQLException
    {
        return getString(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a Java byte array.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public byte[] getBytes(int columnIndex) throws SQLException
    {
        return (byte []) getRaw(columnIndex);
    }

    /**
     * Get the value of a column in the current row as a Java byte array.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public byte[] getBytes(String columnName) throws SQLException
    {
        return getBytes(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a Java boolean.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is false
     */
    public boolean getBoolean(int columnIndex) throws SQLException
    {
        return toBoolean(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a Java boolean.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is false
     */
    public boolean getBoolean(String columnName) throws SQLException
    {
        return getBoolean(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a Java byte.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public byte getByte(int columnIndex) throws SQLException
    {
        return toByte(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a Java byte.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public byte getByte(String columnName) throws SQLException
    {
        return getByte(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a Java short.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public short getShort(int columnIndex) throws SQLException
    {
        return toShort(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a Java short.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public short getShort(String columnName) throws SQLException
    {
        return getShort(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a Java int.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public int getInt(int columnIndex) throws SQLException
    {
        return toInt(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a Java int.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public int getInt(String columnName) throws SQLException
    {
        return getInt(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a Java long.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public long getLong(int columnIndex) throws SQLException
    {
        return toLong(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a Java long.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public long getLong(String columnName) throws SQLException
    {
        return getLong(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a Java float.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public float getFloat(int columnIndex) throws SQLException
    {
        return toFloat(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a Java float.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public float getFloat(String columnName) throws SQLException
    {
        return getFloat(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a Java double.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public double getDouble(int columnIndex) throws SQLException
    {
        return toDouble(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a Java double.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
     */
    public double getDouble(String columnName) throws SQLException
    {
        return getDouble(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.lang.BigDecimal object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException
    {
        return toBigDecimal(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a java.lang.BigDecimal object.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public BigDecimal getBigDecimal(String columnName) throws SQLException
    {
        return getBigDecimal(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.lang.BigDecimal object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param scale the number of digits to the right of the decimal
     * @return the column value; if the value is SQL NULL, the result is null
     * @deprecated
     */
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException
    {
        // THIS IS DEPRECATED in the current JDBC spec (use of 'scale')
        BigDecimal bd = getBigDecimal(columnIndex);
        if (bd != null) {
            return NumberUtil.rescaleBigDecimal(bd, scale);
        } else {
            return null;
        }
    }

    /**
     * Get the value of a column in the current row as a java.lang.BigDecimal object.
     *
     * @param columnName is the SQL name of the column
     * @param scale the number of digits to the right of the decimal
     * @return the column value; if the value is SQL NULL, the result is null
     * @deprecated
     */
    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException
    {
        // THIS IS DEPRECATED in the current JDBC spec (use of 'scale')
        return getBigDecimal(findColumn(columnName),scale);
    }

    /**
     * Get the value of a column in the current row as a java.sql.Date object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public java.sql.Date getDate(int columnIndex) throws SQLException
    {
        return toDate(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Date object.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public java.sql.Date getDate(String columnName) throws SQLException
    {
        return getDate(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Date object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal - the java.util.Calendar object to use in constructing the date
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Date getDate(int columnIndex, java.util.Calendar cal) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * Get the value of a column in the current row as a java.sql.Date object.
     *
     * @param columnName is the SQL name of the column
     * @param cal - the java.util.Calendar object to use in constructing the date
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Date getDate(String columnName, java.util.Calendar cal) throws SQLException
    {
        return getDate(findColumn(columnName), cal);
    }

    /**
     * Get the value of a column in the current row as a java.sql.Time object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public java.sql.Time getTime(int columnIndex) throws SQLException
    {
        return toTime(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Time object.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public java.sql.Time getTime(String columnName) throws SQLException
    {
        return getTime(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Time object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal - the java.util.Calendar object to use in constructing the time
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Time getTime(int columnIndex, java.util.Calendar cal) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * Get the value of a column in the current row as a java.sql.Time object.
     *
     * @param columnName is the SQL name of the column
     * @param cal - the java.util.Calendar object to use in constructing the time
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Time getTime(String columnName, java.util.Calendar cal) throws SQLException
    {
        return getTime(findColumn(columnName), cal);
    }

    /**
     * Get the value of a column in the current row as a java.sql.Timestamp object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public java.sql.Timestamp getTimestamp(int columnIndex) throws SQLException
    {
        return toTimestamp(getRaw(columnIndex));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Timestamp object.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public java.sql.Timestamp getTimestamp(String columnName) throws SQLException
    {
        return getTimestamp(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Timestamp object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal - the java.util.Calendar object to use in constructing the timestamp
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Timestamp getTimestamp(int columnIndex, java.util.Calendar cal) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * Get the value of a column in the current row as a java.sql.Timestamp object.
     *
     * @param columnName is the SQL name of the column
     * @param cal - the java.util.Calendar object to use in constructing the timestamp
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Timestamp getTimestamp(String columnName, java.util.Calendar cal) throws SQLException
    {
        return getTimestamp(findColumn(columnName), cal);
    }

    /**
     * Get the value of a column in the current row as an Array object
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Array getArray(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * Get the value of a column in the current row as an Array object
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Array getArray(String columnName) throws SQLException
    {
        return getArray(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Blob object
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Blob getBlob(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * Get the value of a column in the current row as a java.sql.Blob object
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Blob getBlob(String columnName) throws SQLException
    {
        return getBlob(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Clob object
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Clob getClob(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * Get the value of a column in the current row as a java.sql.Clob object
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Clob getClob(String columnName) throws SQLException
    {
        return getClob(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.sql.Ref object
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Ref getRef(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * Get the value of a column in the current row as a java.sql.Ref object
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public Ref getRef(String columnName) throws SQLException
    {
        return getRef(findColumn(columnName));
    }

    /**
     * Get the value of a column in the current row as a java.net.URL object
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public java.net.URL getURL(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * Get the value of a column in the current row as a java.net.URL object
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
     */
    public java.net.URL getURL(String columnName) throws SQLException
    {
        return getURL(findColumn(columnName));
    }

    /**
     * <p>Get the value of a column in the current row as a Java object.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java Object type corresponding to the column's SQL type,
     * following the mapping specified in the JDBC spec.
     *
     * <p>This method may also be used to read database specific abstract
     * data types.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return A java.lang.Object holding the column value.
     */
    public Object getObject(int columnIndex)
        throws SQLException
    {
        Object o = getRaw(columnIndex);
        if (o == null) {
            wasNull = true;
        } else {
            wasNull = false;
        }
        return o;
    }


    /**
     * <p>Get the value of a column in the current row as a Java object.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java Object type corresponding to the column's SQL type,
     * following the mapping specified in the JDBC spec.
     *
     * <p>This method may also be used to read database specific abstract
     * data types.
     *
     * @param columnName is the SQL name of the column
     * @return A java.lang.Object holding the column value.
     */
    public Object getObject(String columnName) throws SQLException
    {
        return getObject(findColumn(columnName));
    }

    /**
     * <p>Get the value of a column in the current row as a Java object.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java Object type corresponding to the column's SQL type,
     * following the mapping specified in the JDBC spec. This method
     * uses the specified Map object for custom mapping if appropriate.
     *
     * <p>This method may also be used to read database specific abstract
     * data types.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param map a java.util.Map object that contains the mapping from SQL
     * type names to classes in the Java programming language
     * @return A java.lang.Object holding the column value.
     */
    public Object getObject(int columnIndex, java.util.Map map) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * <p>Get the value of a column in the current row as a Java object.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java Object type corresponding to the column's SQL type,
     * following the mapping specified in the JDBC spec. This method
     * uses the specified Map object for custom mapping if appropriate.
     *
     * <p>This method may also be used to read database specific abstract
     * data types.
     *
     * @param columnName is the SQL name of the column
     * @param map a java.util.Map object that contains the mapping from SQL
     * type names to classes in the Java programming language
     * @return A java.lang.Object holding the column value.
     */
    public Object getObject(String columnName, java.util.Map map) throws SQLException
    {
        return getObject(findColumn(columnName), map);
    }

    /**
     * A column value can be retrieved as a stream of ASCII characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARCHAR values.  The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a get method implicitly closes the stream. . Also, a
     * stream may return 0 for available() whether there is data
     * available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream of one byte ASCII characters.  If the value is SQL NULL
     * then the result is null.
     */
    public java.io.InputStream getAsciiStream(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * A column value can be retrieved as a stream of ASCII characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARCHAR values.  The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a get method implicitly closes the stream.
     *
     * @param columnName is the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of one byte ASCII characters.  If the value is SQL NULL
     * then the result is null.
     */
    public java.io.InputStream getAsciiStream(String columnName) throws SQLException
    {
        return getAsciiStream(findColumn(columnName));
    }

    /**
     * A column value can be retrieved as a stream of uninterpreted bytes
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARBINARY values.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a get method implicitly closes the stream. Also, a
     * stream may return 0 for available() whether there is data
     * available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream of uninterpreted bytes.  If the value is SQL NULL
     * then the result is null.
     */
    public java.io.InputStream getBinaryStream(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * A column value can be retrieved as a stream of uninterpreted bytes
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARBINARY values.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a get method implicitly closes the stream.
     *
     * @param columnName is the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of uninterpreted bytes.  If the value is SQL NULL
     * then the result is null.
     */
    public java.io.InputStream getBinaryStream(String columnName) throws SQLException
    {
        return getBinaryStream(findColumn(columnName));
    }

    /**
     * A column value can be retrieved as a stream of characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARCHAR values.  The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a get method implicitly closes the stream.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a character stream.  If the value is SQL NULL then the result is null.
     */
    public java.io.Reader getCharacterStream(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * A column value can be retrieved as a stream of characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARCHAR values.  The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a get method implicitly closes the stream.
     *
     * @param columnName is the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a character stream.  If the value is SQL NULL then the result is null.
     */
    public java.io.Reader getCharacterStream(String columnName) throws SQLException
    {
        return getCharacterStream(findColumn(columnName));
    }

    /**
     * A column value can be retrieved as a stream of Unicode characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARCHAR values.  The JDBC driver will
     * do any necessary conversion from the database format into Unicode.
     *
     * <P><B>Note:</B> All the data in the returned stream must be
     * read prior to getting the value of any other column. The next
     * call to a get method implicitly closes the stream. . Also, a
     * stream may return 0 for available() whether there is data
     * available or not.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream of two byte Unicode characters.  If the value is SQL NULL
     * then the result is null.
     * @deprecated
     */
    public java.io.InputStream getUnicodeStream(int columnIndex) throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    /**
     * A column value can be retrieved as a stream of Unicode characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARCHAR values.  The JDBC driver will
     * do any necessary conversion from the database format into Unicode.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a get method implicitly closes the stream.
     *
     * @param columnName is the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of two byte Unicode characters.  If the value is SQL NULL
     * then the result is null.
     * @deprecated
     */
    public java.io.InputStream getUnicodeStream(String columnName) throws SQLException
    {
        return getUnicodeStream(findColumn(columnName));
    }

    //======================================================================
    // Update methods for individual types; these are not supported
    //======================================================================

    public void updateShort(int columnIndex, short x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateShort(String columnName, short x) throws SQLException
    {
        updateShort(findColumn(columnName),x);
    }

    public void updateNull(int columnIndex) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateNull(String columnName) throws SQLException
    {
        updateNull(findColumn(columnName));
    }

    public void updateLong(int columnIndex, long x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateLong(String columnName, long x) throws SQLException
    {
        updateLong(findColumn(columnName),x);
    }

    public void updateInt(int columnIndex, int x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateInt(String columnName, int x) throws SQLException
    {
        updateInt(findColumn(columnName),x);
    }

    public void updateFloat(int columnIndex, float x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateFloat(String columnName, float x) throws SQLException
    {
        updateFloat(findColumn(columnName),x);
    }

    public void updateDouble(int columnIndex, double x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateDouble(String columnName, double x) throws SQLException
    {
        updateDouble(findColumn(columnName),x);
    }

    public void updateString(int columnIndex, String x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateString(String columnName, String x) throws SQLException
    {
        updateString(findColumn(columnName),x);
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException
    {
        updateBigDecimal(findColumn(columnName),x);
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateBoolean(String columnName, boolean x) throws SQLException
    {
        updateBoolean(findColumn(columnName),x);
    }

    public void updateByte(int columnIndex, byte x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateByte(String columnName, byte x) throws SQLException
    {
        updateByte(findColumn(columnName),x);
    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateBytes(String columnName, byte[] x) throws SQLException
    {
        updateBytes(findColumn(columnName),x);
    }

    public void updateDate(int columnIndex, Date x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateDate(String columnName, Date x) throws SQLException
    {
        updateDate(findColumn(columnName),x);
    }

    public void updateTime(int columnIndex, Time x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateTime(String columnName,Time x) throws SQLException
    {
        updateTime(findColumn(columnName),x);
    }

    public void updateTimestamp(int columnIndex,Timestamp x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateTimestamp(String columnName, Timestamp x) throws SQLException
    {
        updateTimestamp(findColumn(columnName),x);
    }

    public void updateArray(int columnIndex, java.sql.Array x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateArray(String columnName, java.sql.Array x) throws SQLException
    {
        updateArray(findColumn(columnName),x);
    }

    public void updateRef(int columnIndex, java.sql.Ref x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateRef(String columnName, java.sql.Ref x) throws SQLException
    {
        updateRef(findColumn(columnName),x);
    }

    public void updateBlob(int columnIndex, java.sql.Blob x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateBlob(String columnName, java.sql.Blob x) throws SQLException
    {
        updateBlob(findColumn(columnName),x);
    }

    public void updateClob(int columnIndex, java.sql.Clob x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateClob(String columnName, java.sql.Clob x) throws SQLException
    {
        updateClob(findColumn(columnName),x);
    }

    public void updateAsciiStream(int columnIndex,
                                  java.io.InputStream x,
                                  int length) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateAsciiStream(String columnName,
                                  java.io.InputStream x,
                                  int length) throws SQLException
    {
        updateAsciiStream(findColumn(columnName),x,length);
    }

    public void updateBinaryStream(int columnIndex,
                                   java.io.InputStream x,
                                   int length) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateBinaryStream(String columnName,
                                   java.io.InputStream x,
                                   int length) throws SQLException
    {
        updateBinaryStream(findColumn(columnName),x,length);
    }

    public void updateCharacterStream(int columnIndex,
                                      java.io.Reader x,
                                      int length) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateCharacterStream(String columnName,
                                      java.io.Reader x,
                                      int length) throws SQLException
    {
        updateCharacterStream(findColumn(columnName),x,length);
    }

    public void updateObject(int columnIndex, Object x) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateObject(String columnName, Object x) throws SQLException
    {
        updateObject(findColumn(columnName),x);
    }

    public void updateObject(int columnIndex, Object x, int scale) throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void updateObject(String columnName, Object x, int scale) throws SQLException
    {
        updateObject(findColumn(columnName),x,scale);
    }

    public void updateRow() throws SQLException
    {
        throw newUpdatabilityError();
    }

    //======================================================================
    // Other update functions
    //======================================================================

    public boolean rowUpdated() throws SQLException
    {
        return false;
    }

    public boolean rowInserted() throws SQLException
    {
        return false;
    }

    public boolean rowDeleted()throws SQLException
    {
        return false;
    }

    //======================================================================
    // Methods for scrolling resultsets
    //======================================================================

    public void afterLast()
        throws SQLException
    {
        throw newDirectionError();
    }

    public void beforeFirst()
        throws SQLException
    {
        throw newDirectionError();
    }

    public boolean first()
        throws SQLException
    {
        throw newDirectionError();
    }

    public boolean last()
        throws SQLException
    {
        throw newDirectionError();
    }

    public boolean previous()
        throws SQLException
    {
        throw newDirectionError();
    }

    public void cancelRowUpdates()
        throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void deleteRow()
        throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void moveToCurrentRow()
        throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void moveToInsertRow()
        throws SQLException
    {
        throw newUpdatabilityError();
    }

    public boolean relative(int rows)
            throws SQLException
    {
        if ((rows < 0) || (getType() == TYPE_FORWARD_ONLY)) {
            throw newDirectionError();
        }
        while (rows-- > 0) {
            if (!next()) {
                return false;
            }
        }
        return true;
    }

    public void refreshRow()
        throws SQLException
    {
        throw newUpdatabilityError();
    }

    public void insertRow()
        throws SQLException
    {
        throw newUpdatabilityError();
    }

    public boolean absolute(int row)
        throws SQLException
    {
        if ((row < 1) || (getType() == TYPE_FORWARD_ONLY)) {
            throw newDirectionError();
        }

        return relative(row - getRow());
    }

    // Unsupported operations
    /**
     * Get the name of the SQL cursor used by this ResultSet.
     *
     * <P>In SQL, a result table is retrieved through a cursor that is
     * named. The current row of a result can be updated or deleted
     * using a positioned update/delete statement that references the
     * cursor name.
     *
     * <P>JDBC supports this SQL feature by providing the name of the
     * SQL cursor used by a ResultSet. The current row of a ResultSet
     * is also the current row of this SQL cursor.
     *
     * <P><B>Note:</B> If positioned update is not supported a
     * java.sql.SQLException is thrown
     *
     * @return the ResultSet's SQL cursor name
     */
    public String getCursorName() throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    public boolean isLast() throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    public boolean isFirst() throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    public boolean isBeforeFirst() throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    public boolean isAfterLast() throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    public int getRow() throws SQLException
    {
        throw new UnsupportedOperationException("Operation not supported right now");
    }

    // Errors

    protected SQLException newConversionError(
        Object o,
        Class clazz)
    {
        return new SQLException("cannot convert " + o.getClass() + "(" + o
            + ") to " + clazz);
    }

    protected SQLException newDirectionError()
    {
        return new SQLException("ResultSet is TYPE_FORWARD_ONLY");
    }

    protected SQLException newUpdatabilityError()
    {
        return new SQLException("ResultSet is CONCUR_READ_ONLY");
    }

    protected SQLException newFetchError(Throwable e)
    {
        final SQLException sqlEx =
            new SQLException("error while fetching from cursor");
        if (e != null) {
            sqlEx.initCause(e);
        }
        return sqlEx;
    }

    // Private conversion routines
    // TODO: Do rounding, generate warnings

    private boolean toBoolean(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return false;
        } else {
            wasNull = false;
        }
        // REVIEW: JDBC spec maps the boolean type into the BOOLEAN or BIT type
        // so it allows conversions from/to numeric types. We treat any non-zero
        // numeric as true, 0 as false.  Strings are converted based on the
        // SQL.2003 standard or to a numeric value, and then to a boolean.
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        } else if (o instanceof String) {
            String s = (String) o;
            s = s.trim();
            // Allow SQL.2003 boolean literal strings to be converted into
            // boolean values
            if (s.equalsIgnoreCase("true")) {
                return true;
            }
            else if (s.equalsIgnoreCase("false")) {
                return false;
            }
            else if (s.equalsIgnoreCase("unknown")) {
                // SQL.2003 Part 2, Section 5.13, General Rules 10 specifies
                // that the literal unknown indicates that boolean truth value
                // is unknown, represented by null
                wasNull = true;
                return false;
            } else {
                // Try numeric
                return (toDouble(o) != 0);
                //throw newConversionError(o,boolean.class);
            }
        } else {
            return (toDouble(o) != 0);
            //throw newConversionError(o,boolean.class);
        }
    }

    private byte toByte(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return 0;
        } else {
            wasNull = false;
        }
        if (o instanceof Byte) {
            return ((Byte) o).byteValue();
        } else {
            return (byte) toLong_(o);
        }
    }

    private Date toDate(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return null;
        } else {
            wasNull = false;
        }
        if (o instanceof Date) {
            return (Date) o;
        } else if (o instanceof java.util.Date) {
            return new Date(((java.util.Date) o).getTime());
        } else {
            throw newConversionError(o, Date.class);
        }
    }

    private double toDouble(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return 0.0;
        } else {
            wasNull = false;
        }
        if (o instanceof Double) {
            return ((Double) o).doubleValue();
        } else if (o instanceof Float) {
            return ((Float) o).doubleValue();
        } else if (o instanceof String) {
            try {
                return Double.parseDouble(((String) o).trim());
            } catch (NumberFormatException e) {
                throw new SQLException(
                    "Fail to convert to internal representation");
            }
        } else {
            return (double) toLong_(o);
        }
    }

    private float toFloat(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return (float) 0;
        } else {
            wasNull = false;
        }
        if (o instanceof Float) {
            return ((Float) o).floatValue();
        } else if (o instanceof Double) {
            return ((Double) o).floatValue();
        } else if (o instanceof String) {
            try {
                return Float.parseFloat(((String) o).trim());
            } catch (NumberFormatException e) {
                throw new SQLException(
                    "Fail to convert to internal representation");
            }
        } else {
            return (float) toLong_(o);
        }
    }

    private BigDecimal toBigDecimal(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return null;
        } else {
            wasNull = false;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        } else if (o instanceof Double) {
            return BigDecimal.valueOf(((Double) o).doubleValue());
        } else if (o instanceof Float) {
            return BigDecimal.valueOf(((Float) o).doubleValue());
        } else if (o instanceof String) {
            return new BigDecimal((String) o);
        } else {
            return BigDecimal.valueOf(toLong_(o));
        }
    }

    private int toInt(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return 0;
        } else {
            wasNull = false;
        }
        if (o instanceof Integer) {
            return ((Integer) o).intValue();
        } else {
            return (int) toLong_(o);
        }
    }

    private long toLong(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return 0;
        } else {
            wasNull = false;
        }
        return toLong_(o);
    }

    private long toLong_(Object o)
        throws SQLException
    {
        if (o instanceof Long) {
            return ((Long) o).longValue();
        } else if (o instanceof Integer) {
            return ((Integer) o).longValue();
        } else if (o instanceof Short) {
            return ((Short) o).longValue();
        } else if (o instanceof Character) {
            return ((Character) o).charValue();
        } else if (o instanceof Byte) {
            return ((Byte) o).longValue();
        } else if (o instanceof Double) {
            return ((Double) o).longValue();
        } else if (o instanceof Float) {
            return ((Float) o).longValue();
        } else if (o instanceof Boolean) {
            if (((Boolean) o).booleanValue()) {
                return 1;
            } else {
                return 0;
            }
        } else if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (NumberFormatException e) {
                throw newConversionError(o, long.class);
            }
        } else {
            throw newConversionError(o, long.class);
        }
    }

    private short toShort(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return 0;
        } else {
            wasNull = false;
        }
        if (o instanceof Short) {
            return ((Short) o).shortValue();
        } else {
            return (short) toLong_(o);
        }
    }

    private String toString(Object o)
    {
        if (o == null) {
            wasNull = true;
            return null;
        } else if (o instanceof byte []) {
            // convert to hex string
            return ConversionUtil.toStringFromByteArray((byte []) o, 16);
        } else {
            wasNull = false;
            return o.toString();
        }
    }

    private Time toTime(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return null;
        } else {
            wasNull = false;
        }
        if (o instanceof Time) {
            return (Time) o;
        } else {
            throw newConversionError(o, Time.class);
        }
    }

    private Timestamp toTimestamp(Object o)
        throws SQLException
    {
        if (o == null) {
            wasNull = true;
            return null;
        } else {
            wasNull = false;
        }
        if (o instanceof Timestamp) {
            return (Timestamp) o;
        } else {
            throw newConversionError(o, Timestamp.class);
        }
    }


}


// End IteratorResultSet.java
