/*
// $Id$
// Farrago is an extensible data management system.
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
package net.sf.farrago.query;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;


/**
 * FarragoJavaUdxRule is a rule for transforming an abstract {@link
 * TableFunctionRel} into a {@link FarragoJavaUdxRel}.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class FarragoJavaUdxRule
    extends RelOptRule
{
    //~ Static fields/initializers ---------------------------------------------

    /**
     * The singleton instance.
     */
    public static final FarragoJavaUdxRule instance = new FarragoJavaUdxRule();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FarragoJavaUdxRule object.
     */
    public FarragoJavaUdxRule()
    {
        super(new RelOptRuleOperand(TableFunctionRel.class, ANY));
    }

    //~ Methods ----------------------------------------------------------------

    // implement RelOptRule
    public CallingConvention getOutConvention()
    {
        return CallingConvention.ITERATOR;
    }

    // implement RelOptRule
    public void onMatch(RelOptRuleCall call)
    {
        TableFunctionRel callRel = (TableFunctionRel) call.rels[0];
        final RelNode [] inputs = callRel.getInputs().clone();

        for (int i = 0; i < inputs.length; i++) {
            inputs[i] =
                convert(
                    inputs[i],
                    inputs[i].getTraits().plus(CallingConvention.ITERATOR));
        }
        FarragoJavaUdxRel javaTableFunctionRel =
            new FarragoJavaUdxRel(
                callRel.getCluster(),
                callRel.getCall(),
                callRel.getRowType(),
                null,
                inputs);
        javaTableFunctionRel.setColumnMappings(callRel.getColumnMappings());
        call.transformTo(javaTableFunctionRel);
    }
}

// End FarragoJavaUdxRule.java
