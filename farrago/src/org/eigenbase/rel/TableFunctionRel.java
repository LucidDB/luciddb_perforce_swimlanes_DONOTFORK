/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2006 The Eigenbase Project
// Copyright (C) 2006 SQLstream, Inc.
// Copyright (C) 2006 Dynamo BI Corporation
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
package org.eigenbase.rel;

import java.util.List;

import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;


/**
 * <code>TableFunctionRel</code> represents a call to a function which returns a
 * result set. Currently, it can only appear as a leaf in a query tree, but
 * eventually we will extend it to take relational inputs.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class TableFunctionRel
    extends TableFunctionRelBase
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a <code>TableFunctionRel</code>.
     *
     * @param cluster Cluster this relational expression belongs to
     * @param rexCall function invocation expression
     * @param rowType row type produced by function
     * @param inputs 0 or more relational inputs
     * @param inputRowTypes Row types of inputs
     */
    public TableFunctionRel(
        RelOptCluster cluster,
        RexNode rexCall,
        RelDataType rowType,
        RelNode[] inputs,
        List<RelDataType> inputRowTypes)
    {
        super(
            cluster,
            CallingConvention.NONE.singletonSet,
            rexCall,
            rowType,
            inputs,
            inputRowTypes);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public TableFunctionRel copy(RelNode... inputs)
    {
        TableFunctionRel clone =
            new TableFunctionRel(
                getCluster(),
                getCall(),
                getRowType(),
                inputs,
                getInputRowTypes());
        clone.setColumnMappings(getColumnMappings());
        return clone;
    }

    public TableFunctionRel clone()
    {
        return copy(RelOptUtil.clone(inputs))
            .inheritTraitsFrom(this);
    }

    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        // REVIEW jvs 8-Jan-2006:  what is supposed to be here
        // for an abstract rel?
        return planner.makeHugeCost();
    }
}

// End TableFunctionRel.java
