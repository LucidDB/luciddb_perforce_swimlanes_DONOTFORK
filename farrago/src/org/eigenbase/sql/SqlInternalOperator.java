/*
// $Id$
// Saffron preprocessor and data engine
// (C) Copyright 2002-2004 Disruptive Technologies, Inc.
// (C) Copyright 2003-2004 John V. Sichi
// You must accept the terms in LICENSE.html to use this software.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2.1
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

package org.eigenbase.sql;

/**
 * Generic operator for nodes with internal syntax.
 */
public abstract class SqlInternalOperator extends SqlSpecialOperator
{
    //~ Constructors ----------------------------------------------------------

    public SqlInternalOperator(String name,SqlKind kind)
    {
        super(name,kind,1,true, null,null, null);
    }

    public SqlInternalOperator(String name,SqlKind kind, int pred)
    {
        super(name,kind,pred,true, null,null, null);
    }

    public SqlInternalOperator(String name, SqlKind kind, int pred,
            boolean isLeftAssoc, TypeInference typeInference,
            ParamTypeInference paramTypeInference,
            AllowedArgInference argTypeInference)
    {
        super(name,kind,pred,isLeftAssoc,
              typeInference,paramTypeInference, argTypeInference);
    }

    //~ Methods ---------------------------------------------------------------

    public SqlSyntax getSyntax()
    {
        return SqlSyntax.Internal;
    }

    public void unparse(
        SqlWriter writer,
        SqlNode [] operands,
        int leftPrec,
        int rightPrec)
    {
        throw new UnsupportedOperationException(
            "unparse must be implemented by SqlCall subclass");
    }
}
