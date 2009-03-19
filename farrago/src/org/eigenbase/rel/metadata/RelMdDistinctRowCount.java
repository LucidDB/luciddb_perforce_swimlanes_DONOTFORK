/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2006-2007 The Eigenbase Project
// Copyright (C) 2006-2007 SQLstream, Inc.
// Copyright (C) 2006-2007 LucidEra, Inc.
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
package org.eigenbase.rel.metadata;

import java.util.*;

import org.eigenbase.rel.*;
import org.eigenbase.rel.rules.*;
import org.eigenbase.relopt.*;
import org.eigenbase.rex.*;
import org.eigenbase.sql.fun.*;
import org.eigenbase.util14.*;


/**
 * RelMdDistinctRowCount supplies a default implementation of {@link
 * RelMetadataQuery#getDistinctRowCount} for the standard logical algebra.
 *
 * @author Zelaine Fong
 * @version $Id$
 */
public class RelMdDistinctRowCount
    extends ReflectiveRelMetadataProvider
{
    //~ Constructors -----------------------------------------------------------

    public RelMdDistinctRowCount()
    {
        // Tell superclass reflection about parameter types expected
        // for various metadata queries.

        // This corresponds to getDistinctRowCount(rel, RexNode predicate);
        // note that we don't specify the rel type because we always overload
        // on that.
        List<Class> args = new ArrayList<Class>();
        args.add((Class) BitSet.class);
        args.add((Class) RexNode.class);
        mapParameterTypes("getDistinctRowCount", args);
    }

    //~ Methods ----------------------------------------------------------------

    public Double getDistinctRowCount(
        UnionRelBase rel,
        BitSet groupKey,
        RexNode predicate)
    {
        Double rowCount = 0.0;
        int [] adjustments = new int[rel.getRowType().getFieldCount()];
        RexBuilder rexBuilder = rel.getCluster().getRexBuilder();
        for (RelNode input : rel.getInputs()) {
            // convert the predicate to reference the types of the union child
            RexNode modifiedPred;
            if (predicate == null) {
                modifiedPred = null;
            } else {
                modifiedPred =
                    predicate.accept(
                        new RelOptUtil.RexInputConverter(
                            rexBuilder,
                            null,
                            input.getRowType().getFields(),
                            adjustments));
            }
            Double partialRowCount =
                RelMetadataQuery.getDistinctRowCount(
                    input,
                    groupKey,
                    modifiedPred);
            if (partialRowCount == null) {
                return null;
            }
            rowCount += partialRowCount;
        }
        return rowCount;
    }

    public Double getDistinctRowCount(
        SortRel rel,
        BitSet groupKey,
        RexNode predicate)
    {
        return RelMetadataQuery.getDistinctRowCount(
            rel.getChild(),
            groupKey,
            predicate);
    }

    public Double getDistinctRowCount(
        FilterRelBase rel,
        BitSet groupKey,
        RexNode predicate)
    {
        // REVIEW zfong 4/18/06 - In the Broadbase code, duplicates are not
        // removed from the two filter lists.  However, the code below is
        // doing so.
        RexNode unionPreds =
            RelMdUtil.unionPreds(
                rel.getCluster().getRexBuilder(),
                predicate,
                rel.getCondition());

        return RelMetadataQuery.getDistinctRowCount(
            rel.getChild(),
            groupKey,
            unionPreds);
    }

    public Double getDistinctRowCount(
        JoinRelBase rel,
        BitSet groupKey,
        RexNode predicate)
    {
        return RelMdUtil.getJoinDistinctRowCount(
            rel,
            rel.getJoinType(),
            groupKey,
            predicate);
    }

    public Double getDistinctRowCount(
        SemiJoinRel rel,
        BitSet groupKey,
        RexNode predicate)
    {
        // create a RexNode representing the selectivity of the
        // semijoin filter and pass it to getDistinctRowCount
        RexNode newPred = RelMdUtil.makeSemiJoinSelectivityRexNode(rel);
        if (predicate != null) {
            RexBuilder rexBuilder = rel.getCluster().getRexBuilder();
            newPred =
                rexBuilder.makeCall(
                    SqlStdOperatorTable.andOperator,
                    newPred,
                    predicate);
        }

        return RelMetadataQuery.getDistinctRowCount(
            rel.getLeft(),
            groupKey,
            newPred);
    }

    public Double getDistinctRowCount(
        AggregateRelBase rel,
        BitSet groupKey,
        RexNode predicate)
    {
        // determine which predicates can be applied on the child of the
        // aggregate
        List<RexNode> notPushable = new ArrayList<RexNode>();
        List<RexNode> pushable = new ArrayList<RexNode>();
        RelOptUtil.splitFilters(
            rel.getGroupCount(),
            predicate,
            pushable,
            notPushable);
        RexNode childPreds =
            RexUtil.andRexNodeList(
                rel.getCluster().getRexBuilder(),
                pushable);

        // set the bits as they correspond to the child input
        BitSet childKey = new BitSet();
        RelMdUtil.setAggChildKeys(groupKey, rel, childKey);

        Double distinctRowCount =
            RelMetadataQuery.getDistinctRowCount(
                rel.getChild(),
                childKey,
                childPreds);
        if (distinctRowCount == null) {
            return null;
        } else if (notPushable.isEmpty()) {
            return distinctRowCount;
        } else {
            RexNode preds =
                RexUtil.andRexNodeList(
                    rel.getCluster().getRexBuilder(),
                    notPushable);
            return distinctRowCount * RelMdUtil.guessSelectivity(preds);
        }
    }

    public Double getDistinctRowCount(
        ValuesRelBase rel,
        BitSet groupKey,
        RexNode predicate)
    {
        Double selectivity = RelMdUtil.guessSelectivity(predicate);

        // assume half the rows are duplicates
        Double nRows = rel.getRows() / 2;
        return RelMdUtil.numDistinctVals(nRows, nRows * selectivity);
    }

    public Double getDistinctRowCount(
        ProjectRelBase rel,
        BitSet groupKey,
        RexNode predicate)
    {
        BitSet baseCols = new BitSet();
        BitSet projCols = new BitSet();
        RexNode [] projExprs = rel.getChildExps();
        RelMdUtil.splitCols(projExprs, groupKey, baseCols, projCols);

        List<RexNode> notPushable = new ArrayList<RexNode>();
        List<RexNode> pushable = new ArrayList<RexNode>();
        RelOptUtil.splitFilters(
            rel.getRowType().getFieldCount(),
            predicate,
            pushable,
            notPushable);
        RexBuilder rexBuilder = rel.getCluster().getRexBuilder();

        // get the distinct row count of the child input, passing in the
        // columns and filters that only reference the child; convert the
        // filter to reference the children projection expressions
        RexNode childPred = RexUtil.andRexNodeList(rexBuilder, pushable);
        RexNode modifiedPred;
        if (childPred == null) {
            modifiedPred = null;
        } else {
            modifiedPred = RelOptUtil.pushFilterPastProject(childPred, rel);
        }
        Double distinctRowCount =
            RelMetadataQuery.getDistinctRowCount(
                rel.getChild(),
                baseCols,
                modifiedPred);

        if (distinctRowCount == null) {
            return null;
        } else if (!notPushable.isEmpty()) {
            RexNode preds =
                RexUtil.andRexNodeList(
                    rel.getCluster().getRexBuilder(),
                    notPushable);
            distinctRowCount *= RelMdUtil.guessSelectivity(preds);
        }

        // No further computation required if the projection expressions
        // are all column references
        if (projCols.cardinality() == 0) {
            return distinctRowCount;
        }

        // multiply by the cardinality of the non-child projection expressions
        for (
            int bit = projCols.nextSetBit(0);
            bit >= 0;
            bit = projCols.nextSetBit(bit + 1))
        {
            Double subRowCount = RelMdUtil.cardOfProjExpr(rel, projExprs[bit]);
            if (subRowCount == null) {
                return null;
            }
            distinctRowCount *= subRowCount;
        }

        return RelMdUtil.numDistinctVals(
            distinctRowCount,
            RelMetadataQuery.getRowCount(rel));
    }

    // Catch-all rule when none of the others apply.
    public Double getDistinctRowCount(
        RelNode rel,
        BitSet groupKey,
        RexNode predicate)
    {
        // REVIEW zfong 4/19/06 - Broadbase code does not take into
        // consideration selectivity of predicates passed in.  Also, they
        // assume the rows are unique even if the table is not
        boolean uniq = RelMdUtil.areColumnsDefinitelyUnique(rel, groupKey);
        if (uniq) {
            return NumberUtil.multiply(
                RelMetadataQuery.getRowCount(rel),
                RelMetadataQuery.getSelectivity(rel, predicate));
        }
        return null;
    }
}

// End RelMdDistinctRowCount.java
