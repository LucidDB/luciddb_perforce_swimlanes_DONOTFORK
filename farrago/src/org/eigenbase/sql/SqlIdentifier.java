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

import org.eigenbase.sql.parser.ParserPosition;


/**
 * A <code>SqlIdentifier</code> is an identifier, possibly compound.
 */
public class SqlIdentifier extends SqlNode
{
    //~ Instance fields -------------------------------------------------------

    // REVIEW jvs 16-Jan-2004: I removed the final modifier to make it possibly
    // to expand qualifiers in place.  The original array was final, but its
    // contents weren't, so I've further degraded an imperfect situation.
    public String [] names;

    /** This identifiers collation (if any) */
    SqlCollation collation;

    //~ Constructors ----------------------------------------------------------

    /**
     * Creates a compound identifier, for example <code>foo.bar</code>.
     *
     * @param names Parts of the identifier, length &gt;= 1
     */
    public SqlIdentifier(
        String [] names,
        SqlCollation collation,
        ParserPosition pos)
    {
        super(pos);
        this.names = names;
        this.collation = collation;
    }

    public SqlIdentifier(
        String [] names,
        ParserPosition pos)
    {
        super(pos);
        this.names = names;
        this.collation = null;
    }

    /**
     * Creates a simple identifier, for example <code>foo</code>.
     */
    public SqlIdentifier(
        String name,
        SqlCollation collation,
        ParserPosition pos)
    {
        this(new String [] { name }, collation, pos);
    }

    /**
     * Creates a simple identifier, for example <code>foo</code>.
     */
    public SqlIdentifier(
        String name,
        ParserPosition pos)
    {
        this(new String [] { name }, null, pos);
    }

    //~ Methods ---------------------------------------------------------------

    public SqlKind getKind()
    {
        return SqlKind.Identifier;
    }

    public Object clone()
    {
        if (null != collation) {
            return new SqlIdentifier(
                (String []) names.clone(),
                (SqlCollation) collation.clone(),
                getParserPosition());
        }
        return new SqlIdentifier((String []) names.clone(), null);
    }

    public String toString()
    {
        String s = names[0];
        for (int i = 1; i < names.length; i++) {
            s += ("." + names[i]);
        }
        return s;
    }

    public void unparse(
        SqlWriter writer,
        int leftPrec,
        int rightPrec)
    {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (i > 0) {
                writer.print('.');
            }
            if (name.equals("*")) {
                writer.print(name);
            } else {
                writer.printIdentifier(name);
            }
        }

        if (null != collation) {
            writer.print(" ");
            collation.unparse(writer, leftPrec, rightPrec);
        }
    }

    public SqlCollation getCollation()
    {
        return collation;
    }

    public void setCollation(SqlCollation collation)
    {
        this.collation = collation;
    }

    public String getSimple()
    {
        assert (names.length == 1);
        return names[0];
    }
}


// End SqlIdentifier.java
