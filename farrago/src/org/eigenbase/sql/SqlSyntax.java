/*
// $Id$
// Package org.eigenbase is a class library of database components.
// Copyright (C) 2002-2004 Disruptive Tech
// Copyright (C) 2003-2004 John V. Sichi
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

package org.eigenbase.sql;

import org.eigenbase.util.EnumeratedValues;


/**
 * Enumeration of possible syntactic types of {@link SqlOperator operators}.
 *
 * @author jhyde
 * @version $Id$
 *
 * @since June 28, 2004
 */
public class SqlSyntax extends EnumeratedValues.BasicValue
{
    //~ Static fields/initializers --------------------------------------------

    public static final int Function_ordinal = 0;

    /** Function syntax, as in "Foo(x, y)". */
    public static final SqlSyntax Function =
        new SqlSyntax("Function", Function_ordinal);
    public static final int Binary_ordinal = 1;

    /** Binary operator syntax, as in "x + y". */
    public static final SqlSyntax Binary =
        new SqlSyntax("Binary", Binary_ordinal);
    public static final int Prefix_ordinal = 2;

    /** Prefix unary operator syntax, as in "- x". */
    public static final SqlSyntax Prefix =
        new SqlSyntax("Prefix", Prefix_ordinal);
    public static final int Postfix_ordinal = 3;

    /** Postfix unary operator syntax, as in "x ++". */
    public static final SqlSyntax Postfix =
        new SqlSyntax("Postfix", Postfix_ordinal);
    public static final int Special_ordinal = 4;

    /** Special syntax, such as that of the SQL CASE operator,
     * "CASE x WHEN 1 THEN 2 ELSE 3 END". */
    public static final SqlSyntax Special =
        new SqlSyntax("Special", Special_ordinal);
    public static final int Internal_ordinal = 5;

    /** syntax of an internal operator, which does not appear in the SQL */
    public static final SqlSyntax Internal =
        new SqlSyntax("Internal", Internal_ordinal);
    public static final EnumeratedValues enumeration =
        new EnumeratedValues(new SqlSyntax [] {
                Function, Binary, Prefix, Postfix, Special, Internal
            });

    //~ Constructors ----------------------------------------------------------

    private SqlSyntax(
        String name,
        int ordinal)
    {
        super(name, ordinal, null);
    }

    //~ Methods ---------------------------------------------------------------

    /**
     * Looks up a syntax from its ordinal.
     */
    public static SqlSyntax get(int ordinal)
    {
        return (SqlSyntax) enumeration.getValue(ordinal);
    }

    /**
     * Looks up a syntax from its name.
     */
    public static SqlSyntax get(String name)
    {
        return (SqlSyntax) enumeration.getValue(name);
    }
}


// End SqlSyntax.java
