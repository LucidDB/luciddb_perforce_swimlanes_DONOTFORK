/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
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
package org.eigenbase.rel;

import java.util.*;

import org.eigenbase.rel.metadata.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;
import org.eigenbase.util.*;
import org.eigenbase.sql.type.SqlTypeName;


/**
 * <code>JoinRelBase</code> is an abstract base class for implementations of
 * {@link JoinRel}.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public abstract class JoinRelBase
    extends AbstractRelNode
{
    //~ Instance fields --------------------------------------------------------

    protected RexNode condition;
    protected RelNode left;
    protected RelNode right;
    protected Set<String> variablesStopped = Collections.emptySet();

    /**
     * Values must be of enumeration {@link JoinRelType}, except that {@link
     * JoinRelType#RIGHT} is disallowed.
     */
    protected JoinRelType joinType;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a JoinRelBase.
     *
     * @param cluster Cluster
     * @param traits Traits
     * @param left Left input
     * @param right Right input
     * @param condition Join condition
     * @param joinType Join type
     * @param variablesStopped Set of names of variables which are set by the
     * LHS and used by the RHS and are not available to nodes above this JoinRel
     * in the tree
     */
    protected JoinRelBase(
        RelOptCluster cluster,
        RelTraitSet traits,
        RelNode left,
        RelNode right,
        RexNode condition,
        JoinRelType joinType,
        Set<String> variablesStopped)
    {
        super(cluster, traits);
        this.left = left;
        this.right = right;
        this.condition = condition;
        this.variablesStopped = variablesStopped;
        assert joinType != null;
        assert condition != null;
        this.joinType = joinType;
    }

    //~ Methods ----------------------------------------------------------------

    public RexNode [] getChildExps()
    {
        return new RexNode[] { condition };
    }

    public RexNode getCondition()
    {
        return condition;
    }

    public RelNode [] getInputs()
    {
        return new RelNode[] { left, right };
    }

    public JoinRelType getJoinType()
    {
        return joinType;
    }

    public RelNode getLeft()
    {
        return left;
    }

    public RelNode getRight()
    {
        return right;
    }

    // TODO: enable
    public boolean _isValid(boolean fail)
    {
        if (!super.isValid(fail)) {
            return false;
        }
        if (getRowType().getFieldCount()
            != getSystemFieldList().size()
            + left.getRowType().getFieldCount()
            + right.getRowType().getFieldCount())
        {
            assert !fail : "field count mismatch";
            return false;
        }
        if (condition != null) {
            if (condition.getType().getSqlTypeName() != SqlTypeName.BOOLEAN) {
                assert !fail
                    : "condition must be boolean: " + condition.getType();
                return false;
            }
            // The input to the condition is a row type consisting of system
            // fields, left fields, and right fields. Very similar to the
            // output row type, except that fields have not yet been made due
            // due to outer joins.
            RexChecker checker =
                new RexChecker(
                    getCluster().getTypeFactory().createStructType(
                        new RelDataTypeFactory.ListFieldInfo(
                            CompositeList.of(
                                getSystemFieldList(),
                                getLeft().getRowType().getFieldList(),
                                getRight().getRowType().getFieldList()))),
                    fail);
            condition.accept(checker);
            if (checker.getFailureCount() > 0) {
                assert !fail
                    : checker.getFailureCount() + " failures in condition "
                    + condition;
                return false;
            }
        }
        return true;
    }

    // implement RelNode
    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        // REVIEW jvs 9-Apr-2006:  Just for now...
        double rowCount = RelMetadataQuery.getRowCount(this);
        return planner.makeCost(rowCount, 0, 0);
    }

    public static double estimateJoinedRows(
        JoinRelBase joinRel,
        RexNode condition)
    {
        double product =
            RelMetadataQuery.getRowCount(joinRel.getLeft())
            * RelMetadataQuery.getRowCount(joinRel.getRight());

        // TODO:  correlation factor
        return product * RelMetadataQuery.getSelectivity(joinRel, condition);
    }

    // implement RelNode
    public double getRows()
    {
        return estimateJoinedRows(this, condition);
    }

    public void setVariablesStopped(Set<String> set)
    {
        variablesStopped = set;
    }

    public Set<String> getVariablesStopped()
    {
        return variablesStopped;
    }

    public void childrenAccept(RelVisitor visitor)
    {
        visitor.visit(left, 0, this);
        visitor.visit(right, 1, this);
    }

    public void explain(RelOptPlanWriter pw)
    {
        final List<String> nameList =
            new ArrayList<String>(Arrays.asList("left", "right", "condition"));
        final List<Object> valueList = new ArrayList<Object>();
        nameList.add("joinType");
        valueList.add(joinType.name().toLowerCase());
        if (!getSystemFieldList().isEmpty()) {
            nameList.add("systemFields");
            valueList.add(getSystemFieldList());
        }
        pw.explain(this, nameList, valueList);
    }

    public void registerStoppedVariable(String name)
    {
        if (variablesStopped.isEmpty()) {
            variablesStopped = new HashSet<String>();
        }
        variablesStopped.add(name);
    }

    public void replaceInput(
        int ordinalInParent,
        RelNode p)
    {
        switch (ordinalInParent) {
        case 0:
            this.left = p;
            break;
        case 1:
            this.right = p;
            break;
        default:
            throw Util.newInternal();
        }
    }

    protected RelDataType deriveRowType()
    {
        return deriveJoinRowType(
            left.getRowType(),
            right.getRowType(),
            joinType,
            getCluster().getTypeFactory(),
            null,
            getSystemFieldList());
    }

    /**
     * Returns a list of system fields that will be prefixed to
     * output row type.
     *
     * @return list of system fields
     */
    public List<RelDataTypeField> getSystemFieldList()
    {
        return Collections.emptyList();
    }

    /**
     * Derives the type of a join relational expression.
     *
     * @param leftType Row type of left input to join
     * @param rightType Row type of right input to join
     * @param joinType Type of join
     * @param typeFactory Type factory
     * @param fieldNameList List of names of fields; if null, field names are
     * inherited and made unique
     * @param systemFieldList List of system fields that will be prefixed to
     * output row type; typically empty but must not be null
     * @return join type
     */
    public static RelDataType deriveJoinRowType(
        RelDataType leftType,
        RelDataType rightType,
        JoinRelType joinType,
        RelDataTypeFactory typeFactory,
        List<String> fieldNameList,
        List<RelDataTypeField> systemFieldList)
    {
        assert systemFieldList != null;
        switch (joinType) {
        case LEFT:
            rightType = typeFactory.createTypeWithNullability(rightType, true);
            break;
        case RIGHT:
            leftType = typeFactory.createTypeWithNullability(leftType, true);
            break;
        case FULL:
            leftType = typeFactory.createTypeWithNullability(leftType, true);
            rightType = typeFactory.createTypeWithNullability(rightType, true);
            break;
        default:
            break;
        }
        return createJoinType(
            typeFactory, leftType, rightType, fieldNameList, systemFieldList);
    }

    /**
     * Returns the type the row which results when two relations are joined.
     *
     * <p>The resulting row type consists of
     * the system fields (if any), followed by
     * the fields of the left type, followed by
     * the fields of the right type. The field name list, if present, overrides
     * the original names of the fields.
     *
     * @param typeFactory Type factory
     * @param leftType Type of left input to join
     * @param rightType Type of right input to join
     * @param fieldNameList If not null, overrides the original names of the
     * fields
     * @param systemFieldList List of system fields that will be prefixed to
     * output row type; typically empty but must not be null
     * @return type of row which results when two relations are joined
     *
     * @pre fieldNameList == null
     * || fieldNameList.size() == systemFieldList.size()
     * + leftType.getFieldCount() + rightType.getFieldCount()
     */
    public static RelDataType createJoinType(
        RelDataTypeFactory typeFactory,
        RelDataType leftType,
        RelDataType rightType,
        List<String> fieldNameList,
        List<RelDataTypeField> systemFieldList)
    {
        assert (fieldNameList == null)
            || (fieldNameList.size()
                == (systemFieldList.size()
                    + leftType.getFieldCount()
                    + rightType.getFieldCount()));
        List<String> nameList = new ArrayList<String>();
        List<RelDataType> typeList = new ArrayList<RelDataType>();

        // use a hashset to keep track of the field names; this is needed
        // to ensure that the contains() call to check for name uniqueness
        // runs in constant time; otherwise, if the number of fields is large,
        // doing a contains() on a list can be expensive
        HashSet<String> uniqueNameList = new HashSet<String>();
        addFields(systemFieldList, typeList, nameList, uniqueNameList);
        addFields(leftType.getFieldList(), typeList, nameList, uniqueNameList);
        if (rightType != null) {
            addFields(
                rightType.getFieldList(), typeList, nameList, uniqueNameList);
        }
        if (fieldNameList != null) {
            assert fieldNameList.size() == nameList.size();
            nameList = fieldNameList;
        }
        return typeFactory.createStructType(typeList, nameList);
    }

    private static void addFields(
        List<RelDataTypeField> fieldList,
        List<RelDataType> typeList,
        List<String> nameList,
        HashSet<String> uniqueNameList)
    {
        for (RelDataTypeField field : fieldList) {
            String name = field.getName();

            // Ensure that name is unique from all previous field names
            if (uniqueNameList.contains(name)) {
                String nameBase = name;
                for (int j = 0; ; j++) {
                    name = nameBase + j;
                    if (!uniqueNameList.contains(name)) {
                        break;
                    }
                }
            }
            nameList.add(name);
            uniqueNameList.add(name);
            typeList.add(field.getType());
        }
    }
}

// End JoinRelBase.java
