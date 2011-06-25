/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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


/**
 * FennelCorrelatorRule is a rule to implement the join of two correlated
 * streams.
 *
 * @author Wael Chatila
 * @version $Id$
 * @since Feb 1, 2005
 */
public class FennelCorrelatorRule
    extends RelOptRule
{
    public static final FennelCorrelatorRule instance =
        new FennelCorrelatorRule();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a FennelCorrelatorRule.
     */
    private FennelCorrelatorRule()
    {
        super(
            new RelOptRuleOperand(
                CorrelatorRel.class,
                ANY));
    }

    //~ Methods ----------------------------------------------------------------

    // implement RelOptRule
    public CallingConvention getOutConvention()
    {
        return FennelRel.FENNEL_EXEC_CONVENTION;
    }

    public void onMatch(RelOptRuleCall call)
    {
        CorrelatorRel correlatorRel = (CorrelatorRel) call.rels[0];
        if (correlatorRel.getJoinType() != JoinRelType.INNER) {
            // TODO: FennelPullCorrelatorRel could potentially also support
            // LEFT JOIN, but does not at present.
            return;
        }
        RelNode relLeftInput = correlatorRel.getLeft();
        RelNode fennelLeftInput =
            convert(
                relLeftInput,
                correlatorRel.getTraits().plus(
                    FennelRel.FENNEL_EXEC_CONVENTION));
        if (fennelLeftInput == null) {
            return;
        }

        RelNode relRightInput = correlatorRel.getRight();
        RelNode fennelRightInput =
            convert(
                relRightInput,
                correlatorRel.getTraits().plus(
                    FennelRel.FENNEL_EXEC_CONVENTION));
        if (fennelRightInput == null) {
            return;
        }

        FennelPullCorrelatorRel fennelPullCorrelatorRel =
            new FennelPullCorrelatorRel(
                correlatorRel.getCluster(),
                fennelLeftInput,
                fennelRightInput,
                new ArrayList<CorrelatorRel.Correlation>(
                    correlatorRel.getCorrelations()));
        call.transformTo(fennelPullCorrelatorRel);
    }
}

// End FennelCorrelatorRule.java
