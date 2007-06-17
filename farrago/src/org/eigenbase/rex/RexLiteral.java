/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2007 The Eigenbase Project
// Copyright (C) 2002-2007 Disruptive Tech
// Copyright (C) 2005-2007 LucidEra, Inc.
// Portions Copyright (C) 2003-2007 John V. Sichi
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
package org.eigenbase.rex;

import java.io.*;

import java.math.*;

import java.nio.*;
import java.nio.charset.*;

import java.util.*;

import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.fun.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.util.*;
import org.eigenbase.util14.*;


/**
 * Constant value in a row-expression.
 *
 * <p>There are several methods for creating literals in {@link RexBuilder}:
 * {@link RexBuilder#makeLiteral(boolean)} and so forth.</p>
 *
 * <p>How is the value stored? In that respect, the class is somewhat of a black
 * box. There is a {@link #getValue} method which returns the value as an
 * object, but the type of that value is implementation detail, and it is best
 * that your code does not depend upon that knowledge. It is better to use
 * task-oriented methods such as {@link #getValue2} and {@link
 * #toJavaString}.</p>
 *
 * <p>The allowable types and combinations are:
 *
 * <table>
 * <tr>
 * <th>TypeName</th>
 * <th>Meaing</th>
 * <th>Value type</th>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#NULL}</td>
 * <td>The null value. It has its own special type.</td>
 * <td>null</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#BOOLEAN}</td>
 * <td>Boolean, namely <code>TRUE</code>, <code>FALSE</code> or <code>
 * UNKNOWN</code>.</td>
 * <td>{@link Boolean}, or null represents the UNKNOWN value</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#DECIMAL}</td>
 * <td>Exact number, for example <code>0</code>, <code>-.5</code>, <code>
 * 12345</code>.</td>
 * <td>{@link BigDecimal}</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#DOUBLE}</td>
 * <td>Approximate number, for example <code>6.023E-23</code>.</td>
 * <td>{@link BigDecimal}</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#DATE}</td>
 * <td>Date, for example <code>DATE '1969-04'29'</code></td>
 * <td>{@link Calendar}</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#TIME}</td>
 * <td>Time, for example <code>TIME '18:37:42.567'</code></td>
 * <td>{@link Calendar}</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#TIMESTAMP}</td>
 * <td>Timestamp, for example <code>TIMESTAMP '1969-04-29
 * 18:37:42.567'</code></td>
 * <td>{@link Calendar}</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#CHAR}</td>
 * <td>Character constant, for example <code>'Hello, world!'</code>, <code>
 * ''</code>, <code>_N'Bonjour'</code>, <code>_ISO-8859-1'It''s superman!'
 * COLLATE SHIFT_JIS$ja_JP$2</code>. These are always CHAR, never VARCHAR.</td>
 * <td>{@link NlsString}</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#BINARY}</td>
 * <td>Binary constant, for example <code>X'7F34'</code>. (The number of hexits
 * must be even; see above.) These constants are always BINARY, never
 * VARBINARY.</td>
 * <td>{@link ByteBuffer}</td>
 * </tr>
 * <tr>
 * <td>{@link SqlTypeName#SYMBOL}</td>
 * <td>A symbol is a special type used to make parsing easier; it is not part of
 * the SQL standard, and is not exposed to end-users. It is used to hold a flag,
 * such as the LEADING flag in a call to the function <code>
 * TRIM([LEADING|TRAILING|BOTH] chars FROM string)</code>.</td>
 * <td>A class which implements the {@link org.eigenbase.util14.Enum14.Value}
 * interface</td>
 * </tr>
 * </table>
 *
 * @author jhyde
 * @version $Id$
 * @since Nov 24, 2003
 */
public class RexLiteral
    extends RexNode
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The value of this literal. Must be consistent with its type, as per
     * {@link #valueMatchesType}. For example, you can't store an {@link
     * Integer} value here just because you feel like it -- all numbers are
     * represented by a {@link BigDecimal}. But since this field is private, it
     * doesn't really matter how the values are stored.
     */
    private final Comparable value;

    /**
     * The real type of this literal, as reported by {@link #getType}.
     */
    private final RelDataType type;

    // TODO jvs 26-May-2006:  Use SqlTypeFamily instead; it exists
    // for exactly this purpose (to avoid the confusion which results
    // from overloading SqlTypeName).
    /**
     * An indication of the broad type of this literal -- even if its type isn't
     * a SQL type. Sometimes this will be different than the SQL type; for
     * example, all exact numbers, including integers have typeName {@link
     * SqlTypeName#DECIMAL}. See {@link #valueMatchesType} for the definitive
     * story.
     */
    private final SqlTypeName typeName;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a <code>RexLiteral</code>.
     *
     * @pre type != null
     * @pre valueMatchesType(value,typeName)
     * @pre (value == null) == type.isNullable()
     */
    RexLiteral(
        Comparable value,
        RelDataType type,
        SqlTypeName typeName)
    {
        Util.pre(type != null, "type != null");
        Util.pre(
            valueMatchesType(value, typeName),
            "valueMatchesType(value,typeName)");
        Util.pre(
            (value == null) == type.isNullable(),
            "(value == null) == type.isNullable()");
        this.value = value;
        this.type = type;
        this.typeName = typeName;
        this.digest = toJavaString(value, typeName);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * @return whether value is appropriate for its type (we have rules about
     * these things)
     */
    public static boolean valueMatchesType(
        Comparable value,
        SqlTypeName typeName)
    {
        switch (typeName) {
        case BOOLEAN:

            // Unlike SqlLiteral, we do not allow boolean null.
            return value instanceof Boolean;
        case NULL:
            return value == null;
        case DECIMAL:
        case DOUBLE:
        case BIGINT:
            return value instanceof BigDecimal;
        case DATE:
        case TIME:
        case TIMESTAMP:
            return value instanceof Calendar;
        case INTERVAL_DAY_TIME:
        case INTERVAL_YEAR_MONTH:

            // REVIEW: angel 2006-08-27 - why is interval sometimes null?
            return (value instanceof BigDecimal) || (value == null);
        case BINARY:
            return value instanceof ByteBuffer;
        case CHAR:

            // A SqlLiteral's charset and collation are optional; not so a
            // RexLiteral.
            return (value instanceof NlsString)
                && (((NlsString) value).getCharset() != null)
                && (((NlsString) value).getCollation() != null);
        case SYMBOL:
            return (value instanceof EnumeratedValues.Value)
                || (value instanceof Enum);
        case INTEGER: // not allowed -- use Decimal
        case VARCHAR: // not allowed -- use Char
        case VARBINARY: // not allowed -- use Binary
        default:
            throw Util.unexpected(typeName);
        }
    }

    private static String toJavaString(
        Comparable value,
        SqlTypeName typeName)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        printAsJava(value, pw, typeName, false);
        pw.flush();
        return sw.toString();
    }

    /**
     * Prints the value this literal as a Java string constant.
     */
    public void printAsJava(PrintWriter pw)
    {
        printAsJava(value, pw, typeName, true);
    }

    /**
     * Prints a value as a Java string. The value must be consistent with the
     * type, as per {@link #valueMatchesType}.
     *
     * <p>Typical return values:
     *
     * <ul>
     * <li>true</li>
     * <li>null</li>
     * <li>"Hello, world!"</li>
     * <li>1.25</li>
     * <li>1234ABCD</li>
     * </ul>
     *
     * @param value Value
     * @param pw Writer to write to
     * @param typeName Type family
     */
    private static void printAsJava(
        Comparable value,
        PrintWriter pw,
        SqlTypeName typeName,
        boolean java)
    {
        switch (typeName) {
        case CHAR:
            NlsString nlsString = (NlsString) value;
            if (java) {
                Util.printJavaString(
                    pw,
                    nlsString.getValue(),
                    true);
            } else {
                boolean includeCharset =
                    (nlsString.getCharsetName() != null)
                    && !nlsString.getCharsetName().equals(
                        SaffronProperties.instance().defaultCharset.get());
                pw.print(nlsString.asSql(includeCharset, false));
            }
            break;
        case BOOLEAN:
            assert value instanceof Boolean;
            pw.print(((Boolean) value).booleanValue() ? "true" : "false");
            break;
        case DECIMAL:
            assert value instanceof BigDecimal;
            pw.print(value.toString());
            break;
        case DOUBLE:
            assert value instanceof BigDecimal;
            pw.print(Util.toScientificNotation((BigDecimal) value));
            break;
        case BINARY:
            assert value instanceof ByteBuffer;
            pw.print("X'");
            pw.print(
                ConversionUtil.toStringFromByteArray(
                    ((ByteBuffer) value).array(),
                    16));
            pw.print("'");
            break;
        case NULL:
            assert value == null;
            pw.print("null");
            break;
        case SYMBOL:
            assert value instanceof SqlLiteral.SqlSymbol;
            pw.print("FLAG(");
            pw.print(value);
            pw.print(")");
            break;
        case DATE:
            printDatetime(pw, new ZonelessDate(), value);
            break;
        case TIME:
            printDatetime(pw, new ZonelessTime(), value);
            break;
        case TIMESTAMP:
            printDatetime(pw, new ZonelessTimestamp(), value);
            break;
        case INTERVAL_DAY_TIME:
        case INTERVAL_YEAR_MONTH:
            if (value instanceof BigDecimal) {
                pw.print(value.toString());
            } else {
                assert value == null;
                pw.print("null");
            }
            break;
        default:
            Util.pre(
                valueMatchesType(value, typeName),
                "valueMatchesType(value, typeName)");
            throw Util.needToImplement(typeName);
        }
    }

    private static void printDatetime(
        PrintWriter pw,
        ZonelessDatetime datetime,
        Comparable value)
    {
        assert (value instanceof Calendar);
        datetime.setZonelessTime(
            ((Calendar) value).getTimeInMillis());
        pw.print(datetime);
    }

    /**
     * Converts a Jdbc string into a RexLiteral. This method accepts a string,
     * as returned by the Jdbc method ResultSet.getString(), and restores the
     * string into an equivalent RexLiteral. It allows one to use Jdbc strings
     * as a common format for data.
     *
     * <p>If a null literal is provided, then a null pointer will be returned.
     *
     * @param type data type of literal to be read
     * @param typeName type family of literal
     * @param literal the (non-SQL encoded) string representation, as returned
     * by the Jdbc call to return a column as a string
     *
     * @return a typed RexLiteral, or null
     */
    public static RexLiteral fromJdbcString(
        RelDataType type,
        SqlTypeName typeName,
        String literal)
    {
        if (literal == null) {
            return null;
        }

        switch (typeName) {
        case CHAR:
            Charset charset = type.getCharset();
            SqlCollation collation = type.getCollation();
            NlsString str =
                new NlsString(
                    literal,
                    charset.name(),
                    collation);
            return new RexLiteral(str, type, typeName);
        case BOOLEAN:
            boolean b = ConversionUtil.toBoolean(literal);
            return new RexLiteral(b, type, typeName);
        case DECIMAL:
        case DOUBLE:
            BigDecimal d = new BigDecimal(literal);
            return new RexLiteral(d, type, typeName);
        case BINARY:
            byte [] bytes = ConversionUtil.toByteArrayFromString(literal, 16);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new RexLiteral(buffer, type, typeName);
        case NULL:
            return new RexLiteral(null, type, typeName);
        case INTERVAL_DAY_TIME:
            long millis =
                SqlParserUtil.intervalToMillis(
                    literal,
                    type.getIntervalQualifier());
            return new RexLiteral(BigDecimal.valueOf(millis), type, typeName);
        case INTERVAL_YEAR_MONTH:
            long months =
                SqlParserUtil.intervalToMonths(
                    literal,
                    type.getIntervalQualifier());
            return new RexLiteral(BigDecimal.valueOf(months), type, typeName);
        case DATE:
        case TIME:
        case TIMESTAMP:
            String format = getCalendarFormat(typeName);
            TimeZone tz = DateTimeUtil.gmtZone;
            Calendar cal = null;
            if (typeName == SqlTypeName.DATE) {
                cal =
                    DateTimeUtil.parseDateFormat(
                        literal,
                        format,
                        tz);
            } else {
                // Allow fractional seconds for times and timestamps
                DateTimeUtil.PrecisionTime ts =
                    DateTimeUtil.parsePrecisionDateTimeLiteral(
                        literal,
                        format,
                        tz);
                if (ts != null) {
                    cal = ts.getCalendar();
                }
            }
            if (cal == null) {
                throw Util.newInternal(
                    "fromJdbcString: invalid date/time value '"
                    + literal + "'");
            }
            return new RexLiteral(cal, type, typeName);
        case SYMBOL:

        // Symbols are for internal use
        default:
            throw Util.newInternal("fromJdbcString: unsupported type");
        }
    }

    private static String getCalendarFormat(SqlTypeName typeName)
    {
        switch (typeName) {
        case DATE:
            return SqlParserUtil.DateFormatStr;
        case TIME:
            return SqlParserUtil.TimeFormatStr;
        case TIMESTAMP:
            return SqlParserUtil.TimestampFormatStr;
        default:
            throw Util.newInternal("getCalendarFormat: unknown type");
        }
    }

    public SqlTypeName getTypeName()
    {
        return typeName;
    }

    public RelDataType getType()
    {
        return type;
    }

    public RexKind getKind()
    {
        return RexKind.Literal;
    }

    /**
     * Returns the value of this literal.
     *
     * @post valueMatchesType(return, typeName)
     */
    public Comparable getValue()
    {
        assert valueMatchesType(value, typeName) : value;
        return value;
    }

    /**
     * Returns the value of this literal, in the form that the calculator
     * program builder wants it.
     */
    public Object getValue2()
    {
        switch (typeName) {
        case BINARY:
            return ((ByteBuffer) value).array();
        case CHAR:
            return ((NlsString) value).getValue();
        case DECIMAL:
            return new Long(((BigDecimal) value).unscaledValue().longValue());
        case DATE:
        case TIME:
        case TIMESTAMP:
            return new Long(((Calendar) value).getTimeInMillis());
        default:
            return value;
        }
    }

    public static boolean booleanValue(RexNode node)
    {
        return ((Boolean) ((RexLiteral) node).value).booleanValue();
    }

    public boolean isAlwaysTrue()
    {
        Util.pre(
            typeName == SqlTypeName.BOOLEAN,
            "typeName.getOrdinal() == SqlTypeName.Boolean_ordinal");
        return booleanValue(this);
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof RexLiteral)
            && equals(((RexLiteral) obj).value, value);
    }

    public int hashCode()
    {
        return (value == null) ? 0 : value.hashCode();
    }

    public static int intValue(RexNode node)
    {
        final Comparable value = findValue(node);
        return ((Number) value).intValue();
    }

    public static String stringValue(RexNode node)
    {
        final Comparable value = findValue(node);
        return (value == null) ? null : ((NlsString) value).getValue();
    }

    private static Comparable findValue(RexNode node)
    {
        if (node instanceof RexLiteral) {
            return ((RexLiteral) node).value;
        }
        if (node instanceof RexCall) {
            final RexCall call = (RexCall) node;
            final SqlOperator operator = call.getOperator();
            if (operator == SqlStdOperatorTable.castFunc) {
                return findValue(call.getOperands()[0]);
            }
            if (operator == SqlStdOperatorTable.prefixMinusOperator) {
                final BigDecimal value =
                    (BigDecimal) findValue(call.getOperands()[0]);
                return value.negate();
            }
        }
        throw Util.newInternal("not a literal: " + node);
    }

    public static boolean isNullLiteral(RexNode node)
    {
        return (node instanceof RexLiteral)
            && (((RexLiteral) node).value == null);
    }

    public RexLiteral clone()
    {
        return new RexLiteral(value, type, typeName);
    }

    private static boolean equals(
        Object o1,
        Object o2)
    {
        return (o1 == null) ? (o2 == null) : o1.equals(o2);
    }

    public <R> R accept(RexVisitor<R> visitor)
    {
        return visitor.visitLiteral(this);
    }
}

// End RexLiteral.java
