/*
// Farrago is a relational database management system.
// Copyright (C) 2003-2004 John V. Sichi.
// Copyright (C) 2003-2004 Disruptive Tech
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
package net.sf.farrago.query;

import java.util.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.fem.fennel.*;
import net.sf.farrago.util.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.util.*;


/**
 * FennelSortRel is the relational expression corresponding to a sort
 * implemented inside of Fennel.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class FennelSortRel extends FennelPullSingleRel
{
    //~ Instance fields -------------------------------------------------------

    /**
     * 0-based ordinals of fields on which to sort, from most significant to
     * least significant.
     */
    public final Integer [] keyProjection;

    /** Whether to discard tuples with duplicate keys. */
    public final boolean discardDuplicates;

    //~ Constructors ----------------------------------------------------------

    /**
     * Creates a new FennelSortRel object.
     *
     * @param cluster RelOptCluster for this rel
     * @param child rel producing rows to be sorted
     * @param keyProjection 0-based ordinals of fields making up sort key
     * @param discardDuplicates whether to discard duplicates based on key
     */
    public FennelSortRel(
        RelOptCluster cluster,
        RelNode child,
        Integer [] keyProjection,
        boolean discardDuplicates)
    {
        super(cluster, child);

        // TODO:  validate that keyProject references are distinct
        this.keyProjection = keyProjection;
        this.discardDuplicates = discardDuplicates;
    }

    //~ Methods ---------------------------------------------------------------

    // override Rel
    public boolean isDistinct()
    {
        // sort results are distinct if duplicates are discarded AND
        // the sort key is the whole tuple
        return discardDuplicates
            && (keyProjection.length == getRowType().getFieldCount());
    }

    // implement Cloneable
    public Object clone()
    {
        return new FennelSortRel(
            cluster,
            RelOptUtil.clone(child),
            keyProjection,
            discardDuplicates);
    }

    // implement RelNode
    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        // TODO:  the real thing
        double rowCount = getRows();
        double bytesPerRow = 1;
        return planner.makeCost(
            rowCount,
            Util.nLogN(rowCount),
            rowCount * bytesPerRow);
    }

    // override RelNode
    public void explain(RelOptPlanWriter pw)
    {
        pw.explain(
            this,
            new String [] { "child", "key", "discardDuplicates" },
            new Object [] {
                Arrays.asList(keyProjection),
                Boolean.valueOf(discardDuplicates)
            });
    }

    // implement FennelRel
    public FemExecutionStreamDef toStreamDef(FennelRelImplementor implementor)
    {
        FemSortingStreamDef sortingStream =
            getRepos().newFemSortingStreamDef();

        sortingStream.setDistinctness(discardDuplicates
            ? DistinctnessEnum.DUP_DISCARD : DistinctnessEnum.DUP_ALLOW);
        sortingStream.setKeyProj(
            FennelRelUtil.createTupleProjection(
                getRepos(),
                keyProjection));
        sortingStream.getInput().add(
            implementor.visitFennelChild((FennelRel) child));
        return sortingStream;
    }

    // implement FennelRel
    public RelFieldCollation [] getCollations()
    {
        RelFieldCollation [] collations =
            new RelFieldCollation[keyProjection.length];
        for (int i = 0; i < keyProjection.length; ++i) {
            collations[i] = new RelFieldCollation(keyProjection[i].intValue());
        }
        return collations;
    }
}


// End FennelSortRel.java
