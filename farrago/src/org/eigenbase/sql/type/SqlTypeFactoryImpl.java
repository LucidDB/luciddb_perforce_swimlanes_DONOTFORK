/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2007 The Eigenbase Project
// Copyright (C) 2004-2007 Disruptive Tech
// Copyright (C) 2005-2007 LucidEra, Inc.
// Portions Copyright (C) 2004-2007 John V. Sichi
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
package org.eigenbase.sql.type;

import java.nio.charset.*;

import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;
import org.eigenbase.util.*;


/**
 * SqlTypeFactoryImpl provides a default implementation of {@link
 * RelDataTypeFactory} which supports SQL types.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class SqlTypeFactoryImpl
    extends RelDataTypeFactoryImpl
{
    //~ Constructors -----------------------------------------------------------

    public SqlTypeFactoryImpl()
    {
    }

    //~ Methods ----------------------------------------------------------------

    // implement RelDataTypeFactory
    public RelDataType createSqlType(SqlTypeName typeName)
    {
        assertBasic(typeName);
        RelDataType newType = new BasicSqlType(typeName);
        return canonize(newType);
    }

    // implement RelDataTypeFactory
    public RelDataType createSqlType(
        SqlTypeName typeName,
        int precision)
    {
        assertBasic(typeName);
        Util.pre(precision >= 0, "precision >= 0");
        RelDataType newType = new BasicSqlType(typeName, precision);
        newType = SqlTypeUtil.addCharsetAndCollation(newType, this);
        return canonize(newType);
    }

    // implement RelDataTypeFactory
    public RelDataType createSqlType(
        SqlTypeName typeName,
        int precision,
        int scale)
    {
        assertBasic(typeName);
        Util.pre(precision >= 0, "precision >= 0");
        RelDataType newType = new BasicSqlType(typeName, precision, scale);
        newType = SqlTypeUtil.addCharsetAndCollation(newType, this);
        return canonize(newType);
    }

    // implement RelDataTypeFactory
    public RelDataType createMultisetType(RelDataType type,
        long maxCardinality)
    {
        assert (maxCardinality == -1);
        RelDataType newType = new MultisetSqlType(type, false);
        return canonize(newType);
    }

    // implement RelDataTypeFactory
    public RelDataType createSqlIntervalType(
        SqlIntervalQualifier intervalQualifier)
    {
        RelDataType newType = new IntervalSqlType(intervalQualifier, false);
        return canonize(newType);
    }

    // implement RelDataTypeFactory
    public RelDataType createTypeWithCharsetAndCollation(
        RelDataType type,
        Charset charset,
        SqlCollation collation)
    {
        Util.pre(
            SqlTypeUtil.inCharFamily(type),
            "Not a chartype");
        Util.pre(charset != null, "charset!=null");
        Util.pre(collation != null, "collation!=null");
        RelDataType newType;
        if (type instanceof BasicSqlType) {
            BasicSqlType sqlType = (BasicSqlType) type;
            newType = sqlType.createWithCharsetAndCollation(charset, collation);
        } else if (type instanceof JavaType) {
            JavaType javaType = (JavaType) type;
            newType =
                new JavaType(
                    javaType.getJavaClass(),
                    javaType.isNullable(),
                    charset,
                    collation);
        } else {
            throw Util.needToImplement("need to implement " + type);
        }
        return canonize(newType);
    }

    // implement RelDataTypeFactory
    public RelDataType leastRestrictive(RelDataType [] types)
    {
        assert (types != null);
        assert (types.length >= 1);

        RelDataType type0 = types[0];
        if (type0.getSqlTypeName() != null) {
            RelDataType resultType = leastRestrictiveSqlType(types);
            if (resultType != null) {
                return resultType;
            }
            return leastRestrictiveByCast(types);
        }

        return super.leastRestrictive(types);
    }

    private RelDataType leastRestrictiveByCast(RelDataType [] types)
    {
        RelDataType resultType = types[0];
        boolean anyNullable = resultType.isNullable();
        for (int i = 1; i < types.length; i++) {
            RelDataType type = types[i];
            if (type.getSqlTypeName() == SqlTypeName.NULL) {
                anyNullable = true;
                continue;
            }

            if (type.isNullable()) {
                anyNullable = true;
            }

            if (SqlTypeUtil.canCastFrom(type, resultType, false)) {
                resultType = type;
            } else {
                if (!SqlTypeUtil.canCastFrom(resultType, type, false)) {
                    return null;
                }
            }
        }
        if (anyNullable) {
            return createTypeWithNullability(resultType, true);
        } else {
            return resultType;
        }
    }

    // implement RelDataTypeFactory
    public RelDataType createTypeWithNullability(
        final RelDataType type,
        final boolean nullable)
    {
        RelDataType newType;
        if (type instanceof BasicSqlType) {
            BasicSqlType sqlType = (BasicSqlType) type;
            newType = sqlType.createWithNullability(nullable);
        } else if (type instanceof MultisetSqlType) {
            newType = copyMultisetType(type, nullable);
        } else if (type instanceof IntervalSqlType) {
            newType = copyIntervalType(type, nullable);
        } else if (type instanceof ObjectSqlType) {
            newType = copyObjectType(type, nullable);
        } else {
            return super.createTypeWithNullability(type, nullable);
        }
        return canonize(newType);
    }

    private void assertBasic(SqlTypeName typeName)
    {
        assert (typeName != null);
        assert (typeName != SqlTypeName.MULTISET) : "use createMultisetType() instead";
        assert (typeName != SqlTypeName.INTERVAL_DAY_TIME) : "use createSqlIntervalType() instead";
        assert (typeName != SqlTypeName.INTERVAL_YEAR_MONTH) : "use createSqlIntervalType() instead";
    }

    private RelDataType leastRestrictiveSqlType(RelDataType [] types)
    {
        RelDataType resultType = null;
        boolean anyNullable = false;

        for (int i = 0; i < types.length; ++i) {
            RelDataType type = types[i];
            RelDataTypeFamily family = type.getFamily();

            SqlTypeName typeName = type.getSqlTypeName();
            if (typeName == null) {
                return null;
            }

            if (type.isNullable()) {
                anyNullable = true;
            }

            if (typeName == SqlTypeName.NULL) {
                continue;
            }

            if (resultType == null) {
                resultType = type;
                if (resultType.getSqlTypeName() == SqlTypeName.ROW) {
                    return leastRestrictiveStructuredType(types);
                }
            }

            RelDataTypeFamily resultFamily = resultType.getFamily();
            SqlTypeName resultTypeName = resultType.getSqlTypeName();

            if (resultFamily != family) {
                return null;
            }
            if (SqlTypeUtil.inCharOrBinaryFamilies(type)) {
                // TODO:  character set, collation
                int precision =
                    Math.max(
                        resultType.getPrecision(),
                        type.getPrecision());

                // If either type is LOB, then result is LOB with no precision.
                // Otherwise, if either is variable width, result is variable
                // width.  Otherwise, result is fixed width.
                if (SqlTypeUtil.isLob(resultType)) {
                    resultType = createSqlType(resultType.getSqlTypeName());
                } else if (SqlTypeUtil.isLob(type)) {
                    resultType = createSqlType(type.getSqlTypeName());
                } else if (SqlTypeUtil.isBoundedVariableWidth(resultType)) {
                    resultType =
                        createSqlType(
                            resultType.getSqlTypeName(),
                            precision);
                } else {
                    // this catch-all case covers type variable, and both fixed

                    SqlTypeName newTypeName = type.getSqlTypeName();

                    if (shouldRaggedFixedLengthValueUnionBeVariable()) {
                        if (resultType.getPrecision() != type.getPrecision()) {
                            if (newTypeName == SqlTypeName.CHAR) {
                                newTypeName = SqlTypeName.VARCHAR;
                            } else if (newTypeName == SqlTypeName.BINARY) {
                                newTypeName = SqlTypeName.VARBINARY;
                            }
                        }
                    }

                    resultType =
                        createSqlType(
                            newTypeName,
                            precision);
                }
            } else if (SqlTypeUtil.isExactNumeric(type)) {
                if (SqlTypeUtil.isExactNumeric(resultType)) {
                    // TODO: come up with a cleaner way to support
                    // interval + datetime = datetime
                    if (types.length > (i + 1)) {
                        RelDataType type1 = types[i + 1];
                        if (SqlTypeUtil.isDatetime(type1)) {
                            resultType = type1;
                            return resultType;
                        }
                    }
                    if (!type.equals(resultType)) {
                        if (!typeName.allowsPrec()
                            && !resultTypeName.allowsPrec())
                        {
                            // use the bigger primitive
                            if (type.getPrecision()
                                > resultType.getPrecision())
                            {
                                resultType = type;
                            }
                        } else {
                            // Let the result type have precision (p), scale (s)
                            // and number of whole digits (d) as follows: d =
                            // max(p1 - s1, p2 - s2) s <= max(s1, s2) p = s + d

                            int p1 = resultType.getPrecision();
                            int p2 = type.getPrecision();
                            int s1 = resultType.getScale();
                            int s2 = type.getScale();

                            int dout = Math.max(p1 - s1, p2 - s2);
                            dout =
                                Math.min(
                                    dout,
                                    SqlTypeName.MAX_NUMERIC_PRECISION);

                            int scale = Math.max(s1, s2);
                            scale =
                                Math.min(
                                    scale,
                                    SqlTypeName.MAX_NUMERIC_PRECISION - dout);
                            scale =
                                Math.min(scale, SqlTypeName.MAX_NUMERIC_SCALE);

                            int precision = dout + scale;
                            assert (precision
                                <= SqlTypeName.MAX_NUMERIC_PRECISION);
                            assert (precision > 0);

                            resultType =
                                createSqlType(
                                    SqlTypeName.DECIMAL,
                                    precision,
                                    scale);
                        }
                    }
                } else if (SqlTypeUtil.isApproximateNumeric(resultType)) {
                    // already approximate; promote to double just in case
                    // TODO:  only promote when required
                    if (SqlTypeUtil.isDecimal(type)) {
                        // Only promote to double for decimal types
                        resultType = createDoublePrecisionType();
                    }
                } else {
                    return null;
                }
            } else if (SqlTypeUtil.isApproximateNumeric(type)) {
                if (SqlTypeUtil.isApproximateNumeric(resultType)) {
                    if (type.getPrecision() > resultType.getPrecision()) {
                        resultType = type;
                    }
                } else if (SqlTypeUtil.isExactNumeric(resultType)) {
                    if (SqlTypeUtil.isDecimal(resultType)) {
                        resultType = createDoublePrecisionType();
                    } else {
                        resultType = type;
                    }
                } else {
                    return null;
                }
            } else if (SqlTypeUtil.isInterval(type)) {
                // TODO: come up with a cleaner way to support
                // interval + datetime = datetime
                if (types.length > (i + 1)) {
                    RelDataType type1 = types[i + 1];
                    if (SqlTypeUtil.isDatetime(type1)) {
                        resultType = type1;
                        return resultType;
                    }
                }

                if (!type.equals(resultType)) {
                    // TODO jvs 4-June-2005:  This shouldn't be necessary;
                    // move logic into IntervalSqlType.combine
                    Object type1 = resultType;
                    resultType =
                        ((IntervalSqlType) resultType).combine(
                            this,
                            (IntervalSqlType) type);
                    resultType =
                        ((IntervalSqlType) resultType).combine(
                            this,
                            (IntervalSqlType) type1);
                }
            } else if (SqlTypeUtil.isDatetime(type)) {
                // TODO: come up with a cleaner way to support
                // datetime +/- interval (or integer) = datetime
                if (types.length > (i + 1)) {
                    RelDataType type1 = types[i + 1];
                    if (SqlTypeUtil.isInterval(type1)
                        || SqlTypeUtil.isIntType(type1))
                    {
                        resultType = type;
                        return resultType;
                    }
                }
            } else {
                // TODO:  datetime precision details; for now we let
                // leastRestrictiveByCast handle it
                return null;
            }
        }
        if (anyNullable) {
            return createTypeWithNullability(resultType, true);
        } else {
            return resultType;
        }
    }

    /**
     * Controls behavior discussed <a
     * href="http://sf.net/mailarchive/message.php?msg_id=13337379">here</a>.
     *
     * @return false (the default) to provide strict SQL:2003 behavior; true to
     * provide pragmatic behavior
     *
     * @sql.2003 Part 2 Section 9.3 Syntax Rule 3.a.iii.3
     */
    protected boolean shouldRaggedFixedLengthValueUnionBeVariable()
    {
        // TODO jvs 30-Nov-2006:  implement SQL-Flagger support
        // for warning about non-standard usage
        return false;
    }

    private RelDataType createDoublePrecisionType()
    {
        return createSqlType(SqlTypeName.DOUBLE);
    }

    private RelDataType copyMultisetType(RelDataType type, boolean nullable)
    {
        MultisetSqlType mt = (MultisetSqlType) type;
        RelDataType elementType = copyType(mt.getComponentType());
        return new MultisetSqlType(elementType, nullable);
    }

    private RelDataType copyIntervalType(RelDataType type, boolean nullable)
    {
        return new IntervalSqlType(
            type.getIntervalQualifier(),
            nullable);
    }

    private RelDataType copyObjectType(RelDataType type, boolean nullable)
    {
        return new ObjectSqlType(
            type.getSqlTypeName(),
            type.getSqlIdentifier(),
            nullable,
            type.getFields(),
            type.getComparability());
    }

    // override RelDataTypeFactoryImpl
    protected RelDataType canonize(RelDataType type)
    {
        type = super.canonize(type);
        if (!(type instanceof ObjectSqlType)) {
            return type;
        }
        ObjectSqlType objectType = (ObjectSqlType) type;
        if (!objectType.isNullable()) {
            objectType.setFamily(objectType);
        } else {
            objectType.setFamily(
                (RelDataTypeFamily) createTypeWithNullability(
                    objectType,
                    false));
        }
        return type;
    }
}

// End SqlTypeFactoryImpl.java
