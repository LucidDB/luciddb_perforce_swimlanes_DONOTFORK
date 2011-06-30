/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 2003 John V. Sichi
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
package net.sf.farrago.fennel.rel;

import java.util.*;

import net.sf.farrago.query.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;


/**
 * FennelCartesianJoinRule is a rule for converting an INNER JoinRel with no
 * join condition into a FennelCartesianProductRel.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class FennelCartesianJoinRule
    extends RelOptRule
{
    public static final FennelCartesianJoinRule instance =
        new FennelCartesianJoinRule();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a FennelCartesianJoinRule.
     */
    private FennelCartesianJoinRule()
    {
        super(
            new RelOptRuleOperand(
                JoinRel.class,
                RelOptRuleOperand.hasSystemFields(false),
                false,
                new RelOptRuleOperand(RelNode.class, ANY),
                new RelOptRuleOperand(RelNode.class, ANY)));
    }

    //~ Methods ----------------------------------------------------------------

    // implement RelOptRule
    public CallingConvention getOutConvention()
    {
        return FennelRel.FENNEL_EXEC_CONVENTION;
    }

    // implement RelOptRule
    public void onMatch(RelOptRuleCall call)
    {
        JoinRel joinRel = (JoinRel) call.rels[0];

        // Cannot convert joins that generate system fields.
        assert joinRel.getSystemFieldList().isEmpty()
            : "Operand predicate should ensure no sys fields";

        RelNode leftRel = call.rels[1];
        RelNode rightRel = call.rels[2];

        /*
         * Joins that can use CartesianProduct will have only TRUE condition
         * in JoinRel. Any other join conditions have to be extracted out
         * already. This implies that only ON TRUE condition is suported for
         * LeftOuterJoins or RightOuterJoins.
         */
        JoinRelType joinType = joinRel.getJoinType();
        boolean joinTypeFeasible = !(joinType == JoinRelType.FULL);
        boolean swapped = false;
        if (joinType == JoinRelType.RIGHT) {
            swapped = true;
            RelNode tmp = leftRel;
            leftRel = rightRel;
            rightRel = tmp;
            joinType = JoinRelType.LEFT;
        }

        /*
         * CartesianProduct relies on a post filter to do the join filtering.
         * If the join condition is not extracted to a post filter(and is still
         * in JoinRel), CartesianProduct can not be used.
         */
        boolean joinConditionFeasible =
            joinRel.getCondition().equals(
                joinRel.getCluster().getRexBuilder().makeLiteral(true));

        if (!joinTypeFeasible || !joinConditionFeasible) {
            return;
        }

        if (!joinRel.getVariablesStopped().isEmpty()) {
            return;
        }

        leftRel =
            convert(
                leftRel,
                joinRel.getTraits().plus(FennelRel.FENNEL_EXEC_CONVENTION));
        if (leftRel == null) {
            return;
        }

        rightRel =
            convert(
                rightRel,
                joinRel.getTraits().plus(FennelRel.FENNEL_EXEC_CONVENTION));
        if (rightRel == null) {
            return;
        }

        // see if it makes sense to buffer the existing RHS; if not, try
        // the LHS, swapping the join operands if it does make sense to buffer
        // the LHS; but only if the join isn't a left outer join (since we
        // can't do cartesian right outer joins)
        FennelBufferRel bufRel = FennelRelUtil.bufferRight(leftRel, rightRel);
        if (bufRel != null) {
            rightRel = bufRel;
        } else if (joinType != JoinRelType.LEFT) {
            bufRel = FennelRelUtil.bufferRight(rightRel, leftRel);
            if (bufRel != null) {
                swapped = true;
                leftRel = rightRel;
                rightRel = bufRel;
            }
        }

        RelDataType joinRowType;
        if (swapped) {
            joinRowType =
                JoinRel.deriveJoinRowType(
                    leftRel.getRowType(),
                    rightRel.getRowType(),
                    joinType,
                    joinRel.getCluster().getTypeFactory(),
                    null,
                    Collections.<RelDataTypeField>emptyList());
        } else {
            joinRowType = joinRel.getRowType();
        }
        FennelCartesianProductRel productRel =
            new FennelCartesianProductRel(
                joinRel.getCluster(),
                leftRel,
                rightRel,
                joinType,
                RelOptUtil.getFieldNameList(joinRowType));

        RelNode newRel;
        if (swapped) {
            // if the join inputs were swapped, create a CalcRel on top of
            // the new cartesian join that reflects the original join
            // projection
            final RexNode [] exps =
                RelOptUtil.createSwappedJoinExprs(
                    productRel,
                    joinRel,
                    true);
            final RexProgram program =
                RexProgram.create(
                    productRel.getRowType(),
                    exps,
                    null,
                    joinRel.getRowType(),
                    productRel.getCluster().getRexBuilder());
            newRel =
                new FennelCalcRel(
                    productRel.getCluster(),
                    productRel,
                    joinRel.getRowType(),
                    program);
        } else {
            newRel = productRel;
        }
        call.transformTo(newRel);
    }
}

// End FennelCartesianJoinRule.java
