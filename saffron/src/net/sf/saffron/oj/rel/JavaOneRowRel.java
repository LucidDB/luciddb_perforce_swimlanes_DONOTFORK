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

package net.sf.saffron.oj.rel;

import openjava.ptree.*;

import org.eigenbase.oj.rel.*;
import org.eigenbase.oj.util.*;
import org.eigenbase.rel.*;
import org.eigenbase.relopt.CallingConvention;
import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelTraitSet;
import org.eigenbase.util.Util;


/**
 * <code>JavaOneRowRel</code> implements {@link OneRowRel} inline.
 */
public class JavaOneRowRel extends OneRowRelBase implements JavaLoopRel,
    JavaSelfRel
{
    public JavaOneRowRel(RelOptCluster cluster)
    {
        super(cluster, new RelTraitSet(CallingConvention.JAVA));
    }

    // implement RelNode
    public JavaOneRowRel clone()
    {
        JavaOneRowRel clone = new JavaOneRowRel(getCluster());
        clone.inheritTraitsFrom(this);
        return clone;
    }

    public ParseTree implement(JavaRelImplementor implementor)
    {
        // Generate
        //	   <<parent>>
        StatementList stmtList = implementor.getStatementList();
        implementor.generateParentBody(this, stmtList);
        return null;
    }

    public void implementJavaParent(
        JavaRelImplementor implementor,
        int ordinal)
    {
        throw Util.newInternal("should never be called");
    }

    public Expression implementSelf(JavaRelImplementor implementor)
    {
        return new AllocationExpression(
            OJUtil.typeToOJClass(
                getRowType(),
                implementor.getTypeFactory()),
            new ExpressionList());
    }
}


// End JavaOneRowRel.java
