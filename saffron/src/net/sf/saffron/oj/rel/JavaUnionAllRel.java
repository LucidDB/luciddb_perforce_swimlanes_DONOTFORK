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

import openjava.ptree.Expression;
import openjava.ptree.ParseTree;
import openjava.ptree.StatementList;

import org.eigenbase.oj.rel.*;
import org.eigenbase.rel.*;
import org.eigenbase.relopt.CallingConvention;
import org.eigenbase.relopt.RelOptCluster;
import org.eigenbase.relopt.RelOptCost;
import org.eigenbase.relopt.RelOptPlanner;
import org.eigenbase.relopt.RelTraitSet;
import org.eigenbase.util.Util;


/**
 * <code>JavaUnionAllRel</code> implements a {@link UnionRel} inline, without
 * eliminating duplicates.
 */
public class JavaUnionAllRel extends UnionRelBase implements JavaLoopRel
{
    public JavaUnionAllRel(
        RelOptCluster cluster,
        RelNode [] inputs)
    {
        super(cluster, new RelTraitSet(CallingConvention.JAVA), inputs, true);
    }

    // implement RelNode
    public JavaUnionAllRel clone()
    {
        JavaUnionAllRel clone = new JavaUnionAllRel(getCluster(), inputs);
        clone.inheritTraitsFrom(this);
        return clone;
    }

    public JavaUnionAllRel clone(RelNode[] inputs, boolean all)
    {
        assert all;
        JavaUnionAllRel clone = new JavaUnionAllRel(getCluster(), inputs);
        clone.inheritTraitsFrom(this);
        return clone;
    }

    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        double dRows = getRows();
        double dCpu = 0;
        double dIo = 0;
        return planner.makeCost(dRows, dCpu, dIo);
    }

    // Generate
    //   for (int i = 0; i < a.length; i++) {
    //      T row = a[i];
    //      stuff
    //   }
    //   for (int j = 0; j < b.length; j++) {
    //      T row = b[j];
    //      stuff
    //   }
    //
    public ParseTree implement(JavaRelImplementor implementor)
    {
        for (int i = 0; i < inputs.length; i++) {
            Expression expr =
                implementor.visitJavaChild(this, i, (JavaRel) inputs[i]);
            assert expr == null;
        }
        return null;
    }

    public void implementJavaParent(
        JavaRelImplementor implementor,
        int ordinal)
    {
        if ((ordinal < 0) || (ordinal >= inputs.length)) {
            throw Util.newInternal("ordinal '" + ordinal + "' out of bounds");
        }

        // Generate
        //   <<child loop>> {
        //     Type var = <<child row>>
        //     <<parent-handler>>
        //   }
        //   <<next child>>
        StatementList stmtList = implementor.getStatementList();
        implementor.bind(this, inputs[ordinal]);
        implementor.generateParentBody(this, stmtList);
    }
}


// End JavaUnionAllRel.java
