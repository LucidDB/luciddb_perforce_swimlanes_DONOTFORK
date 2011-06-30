/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2010 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
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
package org.luciddb.lcs;

import java.util.*;

import net.sf.farrago.fem.fennel.*;
import net.sf.farrago.fennel.rel.*;
import net.sf.farrago.query.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.RelDataTypeField;


/**
 * An aggregate on bitmap data.
 *
 * @author John Pham
 * @version $Id$
 */
public class LcsIndexAggRel
    extends FennelAggRel
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates an LcsIndexAggRel.
     *
     * @param cluster Cluster
     * @param child Child
     * @param systemFieldList List of system fields
     * @param groupSet Bitset of grouping fields
     * @param aggCalls Collection of calls to aggregate functions
     */
    public LcsIndexAggRel(
        RelOptCluster cluster,
        RelNode child,
        List<RelDataTypeField> systemFieldList,
        BitSet groupSet,
        List<AggregateCall> aggCalls)
    {
        super(cluster, child, systemFieldList, groupSet, aggCalls);
    }

    //~ Methods ----------------------------------------------------------------

    // implement AbstractRelNode
    public LcsIndexAggRel clone()
    {
        LcsIndexAggRel clone =
            new LcsIndexAggRel(
                getCluster(),
                getChild(),
                systemFieldList,
                groupSet,
                aggCalls);
        clone.inheritTraitsFrom(this);
        return clone;
    }

    // implement FennelRel
    public FemExecutionStreamDef toStreamDef(FennelRelImplementor implementor)
    {
        FemLbmSortedAggStreamDef aggStream =
            repos.newFemLbmSortedAggStreamDef();
        FennelRelUtil.defineAggStream(aggCalls, groupSet, repos, aggStream);
        implementor.addDataFlowFromProducerToConsumer(
            implementor.visitFennelChild((FennelRel) getChild(), 0),
            aggStream);

        return aggStream;
    }
}

// End LcsIndexAggRel.java
