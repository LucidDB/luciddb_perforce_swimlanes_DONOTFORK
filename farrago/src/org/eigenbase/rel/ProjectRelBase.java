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
package org.eigenbase.rel;

import java.util.*;

import org.eigenbase.oj.rel.*;
import org.eigenbase.rel.metadata.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.fun.SqlStdOperatorTable;


/**
 * <code>ProjectRelBase</code> is an abstract base class for implementations of
 * {@link ProjectRel}.
 *
 * @version $Id$
 * @author jhyde
 * @since March, 2004
 */
public abstract class ProjectRelBase
    extends SingleRel
{
    //~ Instance fields --------------------------------------------------------

    protected RexNode [] exps;

    /**
     * Values defined in {@link Flags}.
     */
    protected int flags;

    private final List<RelCollation> collationList;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a Project.
     *
     * @param cluster Cluster this relational expression belongs to
     * @param traits traits of this rel
     * @param child input relational expression
     * @param exps set of expressions for the input columns
     * @param rowType output row type
     * @param flags values as in {@link Flags}
     * @param collationList List of sort keys
     */
    protected ProjectRelBase(
        RelOptCluster cluster,
        RelTraitSet traits,
        RelNode child,
        RexNode [] exps,
        RelDataType rowType,
        int flags,
        final List<RelCollation> collationList)
    {
        super(cluster, traits, child);
        assert rowType != null;
        assert collationList != null;
        this.exps = exps;
        this.rowType = rowType;
        this.flags = flags;
        this.collationList =
            collationList.isEmpty() ? Collections.<RelCollation>emptyList()
            : collationList;
        assert isValid(true);
    }

    //~ Methods ----------------------------------------------------------------

    public List<RelCollation> getCollationList()
    {
        return collationList;
    }

    public boolean isBoxed()
    {
        return (flags & Flags.Boxed) == Flags.Boxed;
    }

    // override AbstractRelNode
    public RexNode [] getChildExps()
    {
        return getProjectExps();
    }

    /**
     * Returns the project expressions.
     */
    public RexNode [] getProjectExps()
    {
        return exps;
    }

    public int getFlags()
    {
        return flags;
    }

    public boolean isValid(boolean fail)
    {
        if (!super.isValid(fail)) {
            assert !fail;
            return false;
        }
        if (!RexUtil.compatibleTypes(
                exps,
                getRowType(),
                true))
        {
            assert !fail;
            return false;
        }
        RexChecker checker =
            new RexChecker(
                getChild().getRowType(),
                fail);
        for (RexNode exp : exps) {
            exp.accept(checker);
        }
        if (checker.getFailureCount() > 0) {
            assert !fail;
            return false;
        }
        if (!isBoxed()) {
            if (exps.length != 1) {
                assert !fail;
                return false;
            }
        }
        if (collationList == null) {
            assert !fail;
            return false;
        }
        return true;
    }

    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        double dRows = RelMetadataQuery.getRowCount(getChild());
        double dCpu = dRows * exps.length;
        double dIo = 0;
        return planner.makeCost(dRows, dCpu, dIo);
    }

    public void explain(RelOptPlanWriter pw)
    {
        List<String> terms = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();
        terms.add("child");
        for (RelDataTypeField field : rowType.getFields()) {
            String fieldName = field.getName();
            if (fieldName == null) {
                fieldName = "field#" + (terms.size() - 1);
            }
            terms.add(fieldName);
        }

        // If we're generating a digest, include the rowtype. If two projects
        // differ in return type, we don't want to regard them as equivalent,
        // otherwise we will try to put rels of different types into the same
        // planner equivalence set.
        if ((pw.getDetailLevel() == SqlExplainLevel.DIGEST_ATTRIBUTES)
            && false)
        {
            terms.add("type");
            values.add(rowType);
        }

        pw.explain(this, terms, values);
    }

    /**
     * Burrows into a synthetic record and returns the underlying relation which
     * provides the field called <code>fieldName</code>.
     */
    public JavaRel implementFieldAccess(
        JavaRelImplementor implementor,
        String fieldName)
    {
        if (!isBoxed()) {
            return implementor.implementFieldAccess(
                (JavaRel) getChild(),
                fieldName);
        }
        RelDataType type = getRowType();
        int field = type.getFieldOrdinal(fieldName);
        return implementor.findRel((JavaRel) this, exps[field]);
    }

    /**
     * Returns the input field that an output field is populated from, or -1 if
     * it is populated from an expression.
     *
     * @param outputOrdinal Ordinal of output field
     * @return Input field that output fields is populated from; or -1
     */
    public int getSourceField(int outputOrdinal)
    {
        RexNode expr = exps[outputOrdinal];
        while (true) {
            if (expr instanceof RexCall
                && ((RexCall) expr).getOperator()
                == SqlStdOperatorTable.inFennelFunc)
            {
                // drill through identity function
                expr = ((RexCall) expr).getOperands()[0];
            } else if (expr instanceof RexInputRef) {
                return ((RexInputRef) expr).getIndex();
            } else {
                return -1;
            }
        }
    }

    //~ Inner Interfaces -------------------------------------------------------

    public interface Flags
    {
        int AnonFields = 2;

        /**
         * Whether the resulting row is to be a synthetic class whose fields are
         * the aliases of the fields. <code>boxed</code> must be true unless
         * there is only one field: <code>select {dept.deptno} from dept</code>
         * is boxed, <code>select dept.deptno from dept</code> is not.
         */
        int Boxed = 1;
        int None = 0;
    }

}

// End ProjectRelBase.java
