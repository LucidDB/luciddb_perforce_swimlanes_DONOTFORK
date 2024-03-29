/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2002 SQLstream, Inc.
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
package org.eigenbase.rel.rules;

import java.util.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;


/**
 * PushProjectPastJoinRule implements the rule for pushing a projection past a
 * join by splitting the projection into a projection on top of each child of
 * the join.
 *
 * @author Zelaine Fong
 * @version $Id$
 */
public class PushProjectPastJoinRule
    extends RelOptRule
{
    public static final PushProjectPastJoinRule instance =
        new PushProjectPastJoinRule();

    //~ Instance fields --------------------------------------------------------

    /**
     * Condition for expressions that should be preserved in the projection.
     */
    private final PushProjector.ExprCondition preserveExprCondition;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a PushProjectPastJoinRule.
     */
    private PushProjectPastJoinRule()
    {
        this(PushProjector.ExprCondition.FALSE);
    }

    /**
     * Creates a PushProjectPastJoinRule with an explicit condition.
     *
     * @param preserveExprCondition Condition for expressions that should be
     * preserved in the projection
     */
    public PushProjectPastJoinRule(
        PushProjector.ExprCondition preserveExprCondition)
    {
        super(
            new RelOptRuleOperand(
                ProjectRel.class,
                new RelOptRuleOperand(JoinRel.class, ANY)));
        this.preserveExprCondition = preserveExprCondition;
    }

    //~ Methods ----------------------------------------------------------------

    // implement RelOptRule
    public void onMatch(RelOptRuleCall call)
    {
        ProjectRel origProj = (ProjectRel) call.rels[0];
        JoinRel joinRel = (JoinRel) call.rels[1];

        // locate all fields referenced in the projection and join condition;
        // determine which inputs are referenced in the projection and
        // join condition; if all fields are being referenced and there are no
        // special expressions, no point in proceeding any further
        PushProjector pushProject =
            new PushProjector(
                origProj,
                joinRel.getCondition(),
                joinRel,
                preserveExprCondition);
        if (pushProject.locateAllRefs()) {
            return;
        }

        // create left and right projections, projecting only those
        // fields referenced on each side
        RelNode leftProjRel =
            pushProject.createProjectRefsAndExprs(
                joinRel.getLeft(),
                true,
                false);
        RelNode rightProjRel =
            pushProject.createProjectRefsAndExprs(
                joinRel.getRight(),
                true,
                true);

        // convert the join condition to reference the projected columns
        RexNode newJoinFilter = null;
        int [] adjustments = pushProject.getAdjustments();
        if (joinRel.getCondition() != null) {
            List<RelDataTypeField> projJoinFieldList =
                new ArrayList<RelDataTypeField>();
            projJoinFieldList.addAll(
                joinRel.getSystemFieldList());
            projJoinFieldList.addAll(
                leftProjRel.getRowType().getFieldList());
            projJoinFieldList.addAll(
                rightProjRel.getRowType().getFieldList());
            RelDataTypeField [] projJoinFields =
                projJoinFieldList.toArray(
                    new RelDataTypeField[projJoinFieldList.size()]);
            newJoinFilter =
                pushProject.convertRefsAndExprs(
                    joinRel.getCondition(),
                    projJoinFields,
                    adjustments);
        }

        // create a new joinrel with the projected children
        JoinRel newJoinRel =
            new JoinRel(
                joinRel.getCluster(),
                leftProjRel,
                rightProjRel,
                newJoinFilter,
                joinRel.getJoinType(),
                Collections.<String>emptySet(),
                joinRel.isSemiJoinDone(),
                joinRel.getSystemFieldList());

        // put the original project on top of the join, converting it to
        // reference the modified projection list
        ProjectRel topProject =
            pushProject.createNewProject(newJoinRel, adjustments);

        call.transformTo(topProject);
    }
}

// End PushProjectPastJoinRule.java
