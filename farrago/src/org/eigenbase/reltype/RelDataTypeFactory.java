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
package org.eigenbase.reltype;

import java.nio.charset.*;

import java.util.*;

import org.eigenbase.sql.*;
import org.eigenbase.sql.type.*;


/**
 * RelDataTypeFactory is a factory for datatype descriptors. It defines methods
 * for instantiating and combining SQL, Java, and collection types. The factory
 * also provides methods for return type inference for arithmetic in cases where
 * SQL 2003 is implementation defined or impractical.
 *
 * <p>This interface is an example of the {@link
 * org.eigenbase.util.Glossary#AbstractFactoryPattern abstract factory pattern}.
 * Any implementation of <code>RelDataTypeFactory</code> must ensure that type
 * objects are canonical: two types are equal if and only if they are
 * represented by the same Java object. This reduces memory consumption and
 * comparison cost.
 *
 * @author jhyde
 * @version $Id$
 * @since May 29, 2003
 */
public interface RelDataTypeFactory
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Creates a type which corresponds to a Java class.
     *
     * @param clazz the Java class used to define the type
     *
     * @return canonical Java type descriptor
     */
    RelDataType createJavaType(Class clazz);

    /**
     * Creates a cartesian product type.
     *
     * @return canonical join type descriptor
     *
     * @pre types array of types to be joined
     * @pre types != null
     * @pre types.length >= 1
     */
    public RelDataType createJoinType(RelDataType [] types);

    /**
     * Creates a type which represents a structured collection of fields.
     *
     * @param types types of the fields
     * @param fieldNames names of the fields
     *
     * @return canonical struct type descriptor
     *
     * @pre types.length == fieldNames.length
     * @post return != null
     */
    public RelDataType createStructType(
        RelDataType [] types,
        String [] fieldNames);

    /**
     * Creates a type which represents a structured collection of fields, given
     * lists of the names and types of the fields.
     *
     * @param typeList types of the fields
     * @param fieldNameList names of the fields
     *
     * @return canonical struct type descriptor
     *
     * @pre typeList.size() == fieldNameList.size()
     * @post return != null
     */
    public RelDataType createStructType(
        List<RelDataType> typeList,
        List<String> fieldNameList);

    /**
     * Creates a type which represents a structured collection of fields,
     * obtaining the field information via a callback.
     *
     * @param fieldInfo callback for field information
     *
     * @return canonical struct type descriptor
     */
    public RelDataType createStructType(FieldInfo fieldInfo);

    /**
     * Creates an array type. Arrays are ordered collections of elements.
     *
     * @param elementType type of the elements of the array
     * @param maxCardinality maximum array size, or -1 for unlimited
     *
     * @return canonical array type descriptor
     */
    public RelDataType createArrayType(
        RelDataType elementType,
        long maxCardinality);

    /**
     * Creates a multiset type. Multisets are unordered collections of elements.
     *
     * @param elementType type of the elements of the multiset
     * @param maxCardinality maximum collection size, or -1 for unlimited
     *
     * @return canonical multiset type descriptor
     */
    public RelDataType createMultisetType(
        RelDataType elementType,
        long maxCardinality);

    /**
     * Duplicates a type, making a deep copy. Normally, this is a no-op, since
     * canonical type objects are returned. However, it is useful when copying a
     * type from one factory to another.
     *
     * @param type input type
     *
     * @return output type, a new object equivalent to input type
     */
    public RelDataType copyType(RelDataType type);

    /**
     * Creates a type which is the same as another type but with possibly
     * different nullability. The output type may be identical to the input
     * type. For type systems without a concept of nullability, the return value
     * is always the same as the input.
     *
     * @param type input type
     * @param nullable true to request a nullable type; false to request a NOT
     * NULL type
     *
     * @return output type, same as input type except with specified nullability
     */
    public RelDataType createTypeWithNullability(
        RelDataType type,
        boolean nullable);

    /**
     * Creates a Type which is the same as another type but with possibily
     * different charset or collation. For types without a concept of charset or
     * collation this function must throw an error.
     *
     * @param type input type
     * @param charset charset to assign
     * @param collation collation to assign
     *
     * @return output type, same as input type except with specified charset and
     * collation
     *
     * @pre SqlTypeUtil.inCharFamily(type)
     */
    public RelDataType createTypeWithCharsetAndCollation(
        RelDataType type,
        Charset charset,
        SqlCollation collation);

    /**
     * @return the default {@link Charset} for string types
     */
    public Charset getDefaultCharset();

    /**
     * Returns the most general of a set of types (that is, one type to which
     * they can all be cast), or null if conversion is not possible. The result
     * may be a new type which is less restrictive than any of the input types,
     * e.g. leastRestrictive(INT, NUMERIC(3,2)) could be NUMERIC(12,2).
     *
     * @param types input types to be unioned
     *
     * @return canonical union type descriptor
     *
     * @pre types != null
     * @pre types.length >= 1
     */
    public RelDataType leastRestrictive(RelDataType [] types);

    /**
     * Creates a SQL type with no precision or scale.
     *
     * @param typeName Name of the type, for example {@link
     * SqlTypeName#BOOLEAN}.
     *
     * @return canonical type descriptor
     *
     * @pre typeName != null
     * @post return != null
     */
    public RelDataType createSqlType(SqlTypeName typeName);

    /**
     * Creates a SQL type with length (precision) but no scale.
     *
     * @param typeName Name of the type, for example {@link
     * org.eigenbase.sql.type.SqlTypeName#VARCHAR}.
     * @param precision maximum length of the value (non-numeric types) or the
     * precision of the value (numeric/datetime types) requires both operands to
     * have exact numeric types.
     *
     * @return canonical type descriptor
     *
     * @pre typeName != null
     * @pre length >= 0
     * @post return != null
     */
    public RelDataType createSqlType(
        SqlTypeName typeName,
        int precision);

    /**
     * Creates a SQL type with precision and scale.
     *
     * @param typeName Name of the type, for example {@link
     * org.eigenbase.sql.type.SqlTypeName#DECIMAL}.
     * @param precision precision of the value
     * @param scale scale of the values, i.e. the number of decimal places to
     * shift the value. For example, a NUMBER(10,3) value of "123.45" is
     * represented "123450" (that is, multiplied by 10^3). A negative scale <em>
     * is</em> valid.
     *
     * @return canonical type descriptor
     *
     * @pre typeName != null
     * @pre length >= 0
     * @post return != null
     */
    public RelDataType createSqlType(
        SqlTypeName typeName,
        int precision,
        int scale);

    /**
     * Creates a SQL interval type.
     *
     * @param intervalQualifier contains information if it is a year-month or a
     * day-time interval along with precision information
     *
     * @return canonical type descriptor
     */
    public RelDataType createSqlIntervalType(
        SqlIntervalQualifier intervalQualifier);

    /**
     * Infers the return type of a decimal multiplication. Decimal
     * multiplication involves at least one decimal operand and requires both
     * operands to have exact numeric types.
     *
     * @param type1 type of the first operand
     * @param type2 type of the second operand
     *
     * @return the result type for a decimal multiplication, or null if decimal
     * multiplication should not be applied to the operands.
     */
    public RelDataType createDecimalProduct(
        RelDataType type1,
        RelDataType type2);

    /**
     * @return whether a decimal multiplication should be implemented by casting
     * arguments to double values.
     *
     * @pre createDecimalProduct(type1, type2) != null
     */
    public boolean useDoubleMultiplication(
        RelDataType type1,
        RelDataType type2);

    /**
     * Infers the return type of a decimal division. Decimal division involves
     * at least one decimal operand and requires both operands to have exact
     * numeric types.
     *
     * @param type1 type of the first operand
     * @param type2 type of the second operand
     *
     * @return the result type for a decimal division, or null if decimal
     * division should not be applied to the operands.
     */
    public RelDataType createDecimalQuotient(
        RelDataType type1,
        RelDataType type2);

    //~ Inner Interfaces -------------------------------------------------------

    /**
     * Callback which provides enough information to create fields.
     */
    public interface FieldInfo
    {
        public int getFieldCount();

        public String getFieldName(int index);

        public RelDataType getFieldType(int index);
    }
}

// End RelDataTypeFactory.java
