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
package net.sf.farrago.namespace.ftrs;

import java.util.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.cwm.relational.*;
import net.sf.farrago.fem.med.*;
import net.sf.farrago.fennel.rel.*;
import net.sf.farrago.query.*;
import net.sf.farrago.type.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;


/**
 * FtrsIndexJoinRule is a rule for converting a JoinRel into a
 * FtrsIndexSearchRel when the inputs have the appropriate form.
 *
 * @author John V. Sichi
 * @version $Id$
 */
class FtrsIndexJoinRule
    extends RelOptRule
{
    public static final FtrsIndexJoinRule instance =
        new FtrsIndexJoinRule();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a FtrsIndexJoinRule.
     */
    private FtrsIndexJoinRule()
    {
        super(
            new RelOptRuleOperand(
                JoinRel.class,
                new RelOptRuleOperand(RelNode.class, ANY),
                new RelOptRuleOperand(FtrsIndexScanRel.class, ANY)));
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
        RelNode leftRel = call.rels[1];
        FtrsIndexScanRel scanRel = (FtrsIndexScanRel) call.rels[2];

        if (!joinRel.getVariablesStopped().isEmpty()) {
            return;
        }

        switch (joinRel.getJoinType()) {
        case INNER:
        case LEFT:
            break;
        default:
            return;
        }

        // TODO:  share more code with FtrsScanToSearchRule, and expand
        // set of supported join conditions
        if (scanRel.isOrderPreserving) {
            // index join is guaranteed to destroy scan ordering
            return;
        }

        FarragoRepos repos = FennelRelUtil.getRepos(scanRel);
        int [] joinFieldOrdinals = new int[2];
        if (!RelOptUtil.analyzeSimpleEquiJoin(joinRel, joinFieldOrdinals)) {
            return;
        }
        int leftOrdinal = joinFieldOrdinals[0];
        int rightOrdinal = joinFieldOrdinals[1];

        CwmColumn indexColumn = scanRel.getColumnForFieldAccess(rightOrdinal);
        assert (indexColumn != null);

        if (!scanRel.index.isClustered()) {
            // TODO:  support direct join against an unclustered index scan
            return;
        }

        // if we're working with a clustered index scan, consider all of
        // the unclustered indexes as well
        Iterator iter =
            FarragoCatalogUtil.getTableIndexes(
                repos,
                scanRel.ftrsTable.getCwmColumnSet()).iterator();
        while (iter.hasNext()) {
            FemLocalIndex index = (FemLocalIndex) iter.next();
            considerIndex(
                joinRel,
                index,
                scanRel,
                indexColumn,
                leftOrdinal,
                rightOrdinal,
                leftRel,
                call);
        }
    }

    private void considerIndex(
        JoinRel joinRel,
        FemLocalIndex index,
        FtrsIndexScanRel scanRel,
        CwmColumn indexColumn,
        int leftOrdinal,
        int rightOrdinal,
        RelNode leftRel,
        RelOptRuleCall call)
    {
        // TODO:  support compound keys
        boolean isUnique =
            index.isUnique() && (index.getIndexedFeature().size() == 1);

        boolean isOuter = (joinRel.getJoinType() == JoinRelType.LEFT);

        if (!FtrsScanToSearchRule.testIndexColumn(index, indexColumn)) {
            return;
        }

        // tell the index search how to project the key from its input
        Integer [] inputKeyProj = new Integer[] { leftOrdinal };

        RelDataTypeField [] leftFields = leftRel.getRowType().getFields();
        RelDataType leftType = leftFields[leftOrdinal].getType();
        final RelDataTypeField rightField =
            scanRel.getRowType().getFields()[rightOrdinal];
        RelDataType rightType = rightField.getType();

        FarragoPreparingStmt stmt = FennelRelUtil.getPreparingStmt(scanRel);
        FarragoTypeFactory typeFactory = stmt.getFarragoTypeFactory();

        // decide what to do with nulls
        RelNode nullFilterRel;
        if (isOuter) {
            // can't filter out nulls when isOuter; instead, let Fennel
            // handle the null semantics
            nullFilterRel = leftRel;
            rightType =
                typeFactory.createTypeWithNullability(
                    rightType,
                    leftType.isNullable());
        } else {
            // filter out null search keys, since they never match
            nullFilterRel = RelOptUtil.createNullFilter(leftRel, inputKeyProj);
        }

        // cast the search keys from the left to the type of the search column
        // on the right
        RelNode castRel;
        int leftFieldCount = leftRel.getRowType().getFieldList().size();
        if (leftType.equals(rightType)) {
            // no cast required
            castRel = nullFilterRel;
        } else {
            RexNode [] castExps = new RexNode[leftFieldCount + 1];
            String [] fieldNames = new String[leftFieldCount + 1];
            RexBuilder rexBuilder = leftRel.getCluster().getRexBuilder();
            for (int i = 0; i < leftFieldCount; ++i) {
                castExps[i] =
                    rexBuilder.makeInputRef(
                        leftFields[i].getType(),
                        i);
                fieldNames[i] = leftFields[i].getName();
            }
            castExps[leftFieldCount] =
                rexBuilder.makeCast(rightType, castExps[leftOrdinal]);
            fieldNames[leftFieldCount] = rightField.getName();
            castRel =
                CalcRel.createProject(
                    nullFilterRel,
                    castExps,
                    fieldNames);

            // key now comes from extra cast field instead
            inputKeyProj = new Integer[] { leftFieldCount };
        }

        RelNode fennelInput =
            convert(
                castRel,
                joinRel.getTraits().plus(FennelRel.FENNEL_EXEC_CONVENTION));

        // tell the index search to propagate everything from its input as join
        // fields
        Integer [] inputJoinProj =
            FennelRelUtil.newIotaProjection(leftFieldCount);

        if (!index.isClustered() && scanRel.index.isClustered()) {
            Integer [] clusteredKeyColumns =
                scanRel.ftrsTable.getIndexGuide().getClusteredDistinctKeyArray(
                    scanRel.index);

            // REVIEW:  in many cases it would probably be more efficient to
            // hide the unclustered-to-clustered translation inside a special
            // TupleStream, otherwise the left-hand join fields get
            // propagated one extra time.
            FtrsIndexScanRel unclusteredScan =
                new FtrsIndexScanRel(
                    scanRel.getCluster(),
                    scanRel.ftrsTable,
                    index,
                    scanRel.getConnection(),
                    clusteredKeyColumns,
                    scanRel.isOrderPreserving);
            FtrsIndexSearchRel unclusteredSearch =
                new FtrsIndexSearchRel(
                    unclusteredScan,
                    fennelInput,
                    isUnique,
                    isOuter,
                    inputKeyProj,
                    inputJoinProj,
                    null);

            // tell the search against the clustered index where to find the
            // keys in the output of the unclustered index search, and what to
            // propagate (everything BUT the clustered index key which was
            // tacked onto the end)
            Integer [] clusteredInputKeyProj =
                FennelRelUtil.newBiasedIotaProjection(
                    clusteredKeyColumns.length,
                    inputJoinProj.length);

            FtrsIndexSearchRel clusteredSearch =
                new FtrsIndexSearchRel(
                    scanRel,
                    unclusteredSearch,
                    true,
                    isOuter,
                    clusteredInputKeyProj,
                    inputJoinProj,
                    null);

            call.transformTo(clusteredSearch);
        } else {
            FtrsIndexSearchRel searchRel =
                new FtrsIndexSearchRel(
                    scanRel,
                    fennelInput,
                    isUnique,
                    isOuter,
                    inputKeyProj,
                    inputJoinProj,
                    null);
            call.transformTo(searchRel);
        }
    }
}

// End FtrsIndexJoinRule.java
