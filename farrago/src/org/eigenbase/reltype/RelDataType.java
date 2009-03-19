/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005-2009 The Eigenbase Project
// Copyright (C) 2002-2009 SQLstream, Inc.
// Copyright (C) 2005-2009 LucidEra, Inc.
// Portions Copyright (C) 2003-2009 John V. Sichi
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
 * RelDataType represents the type of a scalar expression or entire row returned
 * from a relational expression.
 *
 * <p>This is a somewhat "fat" interface which unions the attributes of many
 * different type classes into one. Inelegant, but since our type system was
 * defined before the advent of Java generics, it avoids a lot of typecasting.
 *
 * @author jhyde
 * @version $Id$
 * @since May 29, 2003
 */
public interface RelDataType
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Queries whether this is a structured type.
     *
     * @return whether this type has fields; examples include rows and
     * user-defined structured types in SQL, and classes in Java
     */
    public boolean isStruct();

    // NOTE jvs 17-Dec-2004:  once we move to Java generics, getFieldList()
    // will be declared to return a read-only List<RelDataTypeField>,
    // and getFields() will be eliminated.  Currently,
    // anyone can mutate a type by poking into the array returned
    // by getFields!

    /**
     * Gets the fields in a struct type. The field count is equal to the size of
     * the returned list.
     *
     * @return read-only list of fields
     *
     * @pre this.isStruct()
     */
    public List<RelDataTypeField> getFieldList();

    /**
     * Gets the fields in a struct type. The field count is equal to the length
     * of the returned array.
     *
     * <p>NOTE jvs 17-Dec-2004: this method will become deprecated once we move
     * to Java generics, and eventually eliminated
     *
     * @return array of fields
     *
     * @pre this.isStruct()
     */
    public RelDataTypeField [] getFields();

    /**
     * Returns the number of fields in a struct type.
     *
     * <p>This method is equivalent to <code>{@link #getFieldList}
     * ().size()</code>.
     */
    public int getFieldCount();

    /**
     * Looks up the ordinal of a field by name.
     *
     * @param fieldName name of field to find
     *
     * @return 0-based ordinal of named field, or -1 if not found
     *
     * @pre this.isStruct()
     */
    public int getFieldOrdinal(String fieldName);

    /**
     * Looks up a field by name.
     *
     * @param fieldName name of field to find
     *
     * @return named field, or null if not found
     *
     * @pre this.isStruct()
     */
    public RelDataTypeField getField(String fieldName);

    /**
     * Queries whether this type allows null values.
     *
     * @return whether type allows null values
     */
    public boolean isNullable();

    /**
     * Gets the component type if this type is a collection, otherwise null.
     *
     * @return canonical type descriptor for components
     */
    public RelDataType getComponentType();

    /**
     * Gets this type's character set, or null if this type cannot carry a
     * character set or has no character set defined.
     *
     * @return charset of type
     */
    public Charset getCharset();

    /**
     * Gets this type's collation, or null if this type cannot carry a collation
     * or has no collation defined.
     *
     * @return collation of type
     */
    public SqlCollation getCollation();

    /**
     * Gets this type's interval qualifier, or null if this is not an interval
     * type.
     *
     * @return interval qualifier
     */
    public SqlIntervalQualifier getIntervalQualifier();

    /**
     * Gets the JDBC-defined precision for values of this type. Note that this
     * is not always the same as the user-specified precision. For example, the
     * type INTEGER has no user-specified precision, but this method returns 10
     * for an INTEGER type.
     *
     * @return number of decimal digits for exact numeric types; number of
     * decimal digits in mantissa for approximate numeric types; number of
     * decimal digits for fractional seconds of datetime types; length in
     * characters for character types; length in bytes for binary types; length
     * in bits for bit types; 1 for BOOLEAN
     */
    public int getPrecision();

    /**
     * Gets the scale of this type.
     *
     * @return number of digits of scale
     */
    public int getScale();

    /**
     * Gets the {@link SqlTypeName} of this type.
     *
     * @return SqlTypeName, or null if this is not an SQL predefined type
     */
    public SqlTypeName getSqlTypeName();

    /**
     * Gets the {@link SqlIdentifier} associated with this type. For a
     * predefined type, this is a simple identifier based on {@link
     * #getSqlTypeName}. For a user-defined type, this is a compound identifier
     * which uniquely names the type.
     *
     * @return SqlIdentifier, or null if this is not an SQL type
     */
    public SqlIdentifier getSqlIdentifier();

    /**
     * Gets a string representation of this type without detail such as
     * character set and nullability.
     *
     * @return abbreviated type string
     */
    public String toString();

    /**
     * Gets a string representation of this type with full detail such as
     * character set and nullability. The string must serve as a "digest" for
     * this type, meaning two types can be considered identical iff their
     * digests are equal.
     *
     * @return full type string
     */
    public String getFullTypeString();

    /**
     * Gets a canonical object representing the family of this type. Two values
     * can be compared if and only if their types are in the same family.
     *
     * @return canonical object representing type family
     */
    public RelDataTypeFamily getFamily();

    /**
     * @return precedence list for this type
     */
    public RelDataTypePrecedenceList getPrecedenceList();

    /**
     * @return the category of comparison operators which make sense when
     * applied to values of this type
     */
    public RelDataTypeComparability getComparability();
}

// End RelDataType.java
