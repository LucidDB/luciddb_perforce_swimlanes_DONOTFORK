/*
// $Id$
// Saffron preprocessor and data engine
// (C) Copyright 2002-2003 Disruptive Technologies, Inc.
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

import org.eigenbase.util.Util;

/**
 * A postfix unary operator.
 */
public abstract class SqlPostfixOperator extends SqlOperator
{
    //~ Constructors ----------------------------------------------------------

    public SqlPostfixOperator(
        String name,SqlKind kind,int precedence,
        TypeInference typeInference,
        ParamTypeInference paramTypeInference,
        AllowedArgInference argInference)
    {
        super(name, kind, precedence * 2, 1, typeInference, paramTypeInference,argInference);
    }

    //~ Methods ---------------------------------------------------------------

    public SqlSyntax getSyntax()
    {
        return SqlSyntax.Postfix;
    }

    protected String getSignatureTemplate(final int operandsCount) {
        Util.discard(operandsCount);
        return "{1} {0}";
    }

    public void unparse(
        SqlWriter writer,
        SqlNode [] operands,
        int leftPrec,
        int rightPrec)
    {
        assert(operands.length == 1);
        operands[0].unparse(writer,this.leftPrec,this.rightPrec);
        writer.print(' ');
        writer.print(name);
    }
}


// End SqlPostfixOperator.java
