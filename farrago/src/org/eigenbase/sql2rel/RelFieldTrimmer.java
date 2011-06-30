/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2010 The Eigenbase Project
// Copyright (C) 2010 SQLstream, Inc.
// Copyright (C) 2010 Dynamo BI Corporation
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
package org.eigenbase.sql2rel;

import java.util.*;
import java.math.BigDecimal;

import org.eigenbase.rel.*;
import org.eigenbase.rel.rules.RemoveTrivialProjectRule;
import org.eigenbase.rex.*;
import org.eigenbase.sql.validate.SqlValidator;
import org.eigenbase.util.*;
import org.eigenbase.util.mapping.*;
import org.eigenbase.relopt.RelOptUtil;
import org.eigenbase.reltype.*;

/**
 * Transformer that walks over a tree of relational expressions, replacing each
 * {@link RelNode} with a 'slimmed down' relational expression that projects
 * only the columns required by its consumer.
 *
 * <p>Uses multi-methods to fire the right rule for each type of relational
 * expression. This allows the transformer to be extended without having to
 * add a new method to RelNode, and without requiring a collection of rule
 * classes scattered to the four winds.
 *
 * <p>REVIEW: jhyde, 2009/7/28: Is sql2rel the correct package for this class?
 * Trimming fields is not an essential part of SQL-to-Rel translation, and
 * arguably belongs in the optimization phase. But this transformer does not
 * obey the usual pattern for planner rules; it is difficult to do so, because
 * each {@link RelNode} needs to return a different set of fields after
 * trimming.
 *
 * <p>TODO: Change 2nd arg of the {@link #trimFields} method from BitSet to
 * Mapping. Sometimes it helps the consumer if you return the columns in a
 * particular order. For instance, it may avoid a project at the top of the
 * tree just for reordering. Could ease the transition by writing methods that
 * convert BitSet to Mapping and vice versa.
 *
 * @author Julian Hyde
 * @version $Id$
 */
public class RelFieldTrimmer
    implements ReflectiveVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    //~ Instance fields --------------------------------------------------------

    private final ReflectUtil.MethodDispatcher<TrimResult> trimFieldsDispatcher;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a RelFieldTrimmer.
     *
     * @param validator Validator
     */
    public RelFieldTrimmer(SqlValidator validator)
    {
        Util.discard(validator); // may be useful one day
        this.trimFieldsDispatcher =
            ReflectUtil.createMethodDispatcher(
                TrimResult.class,
                this,
                "trimFields",
                RelNode.class,
                BitSet.class);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Trims unused fields from a relational expression.
     *
     * <p>We presume that all fields of the relational expression are wanted by
     * its consumer, so only trim fields that are not used within the tree.
     *
     * @param root Root node of relational expression
     * @return Trimmed relational expression
     */
    public RelNode trim(RelNode root)
    {
        final int fieldCount = root.getRowType().getFieldCount();
        final BitSet fieldsUsed = Util.bitSetBetween(0, fieldCount);
        final TrimResult trimResult = dispatchTrimFields(root, fieldsUsed);
        if (!trimResult.right.isIdentity()) {
            throw new IllegalArgumentException();
        }
        return trimResult.left;
    }

    /**
     * Trims the fields of an input relational expression.
     *
     * @param rel Relational expression
     * @param input Input relational expression, whose fields to trim
     * @param fieldsUsed Bitmap of fields needed by the consumer
     * @return New relational expression and its field mapping
     */
    protected TrimResult trimChild(
        RelNode rel,
        RelNode input,
        BitSet fieldsUsed)
    {
        Util.discard(rel);
        if (input.getClass().getName().endsWith("MedMdrClassExtentRel")) {
            // MedMdrJoinRule cannot handle Join of Project of
            // MedMdrClassExtentRel, only naked MedMdrClassExtentRel.
            // So, disable trimming.
            fieldsUsed =
                Util.bitSetBetween(0, input.getRowType().getFieldCount());
        }
        return dispatchTrimFields(input, fieldsUsed);
    }

    /**
     * Trims a child relational expression, then adds back a dummy project to
     * restore the fields that were removed.
     *
     * <p>Sounds pointless? It causes unused fields to be removed
     * further down the tree (towards the leaves), but it ensure that the
     * consuming relational expression continues to see the same fields.
     *
     * @param rel Relational expression
     * @param input Input relational expression, whose fields to trim
     * @param fieldsUsed Bitmap of fields needed by the consumer
     * @return New relational expression and its field mapping
     */
    protected TrimResult trimChildRestore(
        RelNode rel,
        RelNode input,
        BitSet fieldsUsed)
    {
        TrimResult trimResult = trimChild(rel, input, fieldsUsed);
        if (trimResult.right.isIdentity()) {
            return trimResult;
        }
        final RelDataType rowType = input.getRowType();
        List<RelDataTypeField> fieldList = rowType.getFieldList();
        final List<RexNode> exprList = new ArrayList<RexNode>();
        final List<String> nameList = RelOptUtil.getFieldNameList(rowType);
        RexBuilder rexBuilder = rel.getCluster().getRexBuilder();
        assert trimResult.right.getSourceCount() == fieldList.size();
        for (int i = 0; i < fieldList.size(); i++) {
            int source = trimResult.right.getTargetOpt(i);
            RelDataTypeField field = fieldList.get(i);
            exprList.add(
                source < 0
                ? rexBuilder.makeZeroLiteral(
                    field.getType())
                : rexBuilder.makeInputRef(
                    field.getType(), source));
        }
        RelNode project =
            CalcRel.createProject(
                trimResult.left, exprList, nameList);
        return new TrimResult(
            project,
            Mappings.createIdentity(fieldList.size()));
    }

    /**
     * Invokes {@link #trimFields}, or the appropriate method for the type
     * of the rel parameter, using multi-method dispatch.
     *
     * @param rel Relational expression
     * @param fieldsUsed Bitmap of fields needed by the consumer
     * @return New relational expression and its field mapping
     */
    protected final TrimResult dispatchTrimFields(
        RelNode rel,
        BitSet fieldsUsed)
    {
        final TrimResult trimResult =
            trimFieldsDispatcher.invoke(rel, fieldsUsed);
        final RelNode newRel = trimResult.left;
        final Mapping mapping = trimResult.right;
        final int fieldCount = rel.getRowType().getFieldCount();
        assert mapping.getSourceCount() == fieldCount
            : "source: " + mapping.getSourceCount() + " != " + fieldCount;
        final int newFieldCount = newRel.getRowType().getFieldCount();
        assert mapping.getTargetCount() == newFieldCount
            : "target: " + mapping.getTargetCount() + " != " + newFieldCount;
        assert newFieldCount > 0 : "rel has no fields after trim: " + rel;
        if (newRel.equals(rel)) {
            return new TrimResult(rel, mapping);
        }
        return trimResult;
    }

    /**
     * Visit method, per {@link org.eigenbase.util.ReflectiveVisitor}.
     *
     * <p>This method is invoked reflectively, so there may not be any apparent
     * calls to it. The class (or derived classes) may contain overloads of
     * this method with more specific types for the {@code rel} parameter.
     *
     * <p>Returns a pair: the relational expression created, and the mapping
     * between the original fields and the fields of the newly created
     * relational expression.
     *
     * @param rel Relational expression
     * @param fieldsUsed Fields needed by the consumer
     * @return relational expression and mapping
     */
    public TrimResult trimFields(
        RelNode rel,
        BitSet fieldsUsed)
    {
        // We don't know how to trim this kind of relational expression, so give
        // it back intact.
        Util.discard(fieldsUsed);
        return new TrimResult(
            rel,
            Mappings.createIdentity(
                rel.getRowType().getFieldCount()));
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for {@link ProjectRel}.
     */
    public TrimResult trimFields(
        ProjectRel project,
        BitSet fieldsUsed)
    {
        final RelDataType rowType = project.getRowType();
        final int fieldCount = rowType.getFieldCount();
        final RexNode[] projectExprs = project.getProjectExps();
        final RelNode input = project.getChild();
        final RelDataType inputRowType = input.getRowType();

        // Which fields are required from the input?
        BitSet inputFieldsUsed = new BitSet(inputRowType.getFieldCount());
        RelOptUtil.InputFinder inputFinder =
            new RelOptUtil.InputFinder(inputFieldsUsed);
        for (int i = 0; i < projectExprs.length; i++) {
            final RexNode projectExpr = projectExprs[i];
            if (fieldsUsed.get(i)) {
                projectExpr.accept(inputFinder);
            }
        }

        // Create input with trimmed columns.
        TrimResult trimResult =
            trimChild(project, input, inputFieldsUsed);
        RelNode newInput = trimResult.left;
        final Mapping inputMapping = trimResult.right;

        // If the input is unchanged, and we need to project all columns,
        // there's nothing we can do.
        if (newInput == input
            && fieldsUsed.cardinality() == fieldCount)
        {
            return new TrimResult(
                project,
                Mappings.createIdentity(fieldCount));
        }

        // Some parts of the system can't handle rows with zero fields, so
        // pretend that one field is used.
        if (fieldsUsed.cardinality() == 0) {
            final Mapping mapping =
                Mappings.create(
                    MappingType.InverseSurjection,
                    fieldCount,
                    1);
            final RexLiteral expr =
                project.getCluster().getRexBuilder().makeExactLiteral(
                    BigDecimal.ZERO);
            RelDataType newRowType =
                project.getCluster().getTypeFactory().createStructType(
                    Collections.singletonList(expr.getType()),
                    Collections.singletonList("DUMMY"));
            ProjectRel newProject = new ProjectRel(
                project.getCluster(),
                newInput,
                new RexNode[]{expr},
                newRowType,
                project.getFlags(),
                Collections.<RelCollation>emptyList());
            return new TrimResult(newProject, mapping);
        }

        // Build new project expressions, and populate the mapping.
        List<RexNode> newProjectExprList =
            new ArrayList<RexNode>();
        final RexVisitor<RexNode> shuttle =
            new RexPermuteInputsShuttle(
                (Mappings.TargetMapping) inputMapping);
        final Mapping mapping =
            Mappings.create(
                MappingType.InverseSurjection,
                fieldCount,
                fieldsUsed.cardinality());
        for (int i = 0; i < projectExprs.length; i++) {
            final RexNode projectExpr = projectExprs[i];
            if (fieldsUsed.get(i)) {
                mapping.set(i, newProjectExprList.size());
                RexNode newProjectExpr =
                    projectExpr.accept(shuttle);
                newProjectExprList.add(newProjectExpr);
            }
        }

        final RexNode[] newProjectExprs =
            newProjectExprList.toArray(new RexNode[newProjectExprList.size()]);
        final RelDataType newRowType =
            project.getCluster().getTypeFactory().createStructType(
                Mappings.apply3(mapping, rowType.getFieldList()));

        final List<RelCollation> newCollations =
            RexUtil.apply(inputMapping, project.getCollationList());

        final RelNode newProject;
        if (RemoveTrivialProjectRule.isIdentity(
                newProjectExprs,
                newRowType,
                newInput.getRowType()))
        {
            // The new project would be the identity. It is equivalent to return
            // its child.
            newProject = newInput;
        } else {
            newProject = new ProjectRel(
                project.getCluster(),
                newInput,
                newProjectExprs,
                newRowType,
                project.getFlags(),
                newCollations);
            assert newProject.getClass() == project.getClass();
        }
        return new TrimResult(newProject, mapping);
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for {@link FilterRel}.
     */
    public TrimResult trimFields(
        FilterRel filter,
        BitSet fieldsUsed)
    {
        final RelDataType rowType = filter.getRowType();
        final int fieldCount = rowType.getFieldCount();
        final RexNode conditionExpr = filter.getCondition();
        final RelNode input = filter.getChild();

        // We use the fields used by the consumer, plus any fields used in the
        // filter.
        BitSet inputFieldsUsed = (BitSet) fieldsUsed.clone();
        RelOptUtil.InputFinder inputFinder =
            new RelOptUtil.InputFinder(inputFieldsUsed);
        conditionExpr.accept(inputFinder);

        // Create input with trimmed columns.
        TrimResult trimResult =
            trimChild(filter, input, inputFieldsUsed);
        RelNode newInput = trimResult.left;
        final Mapping inputMapping = trimResult.right;

        // If the input is unchanged, and we need to project all columns,
        // there's nothing we can do.
        if (newInput == input
            && fieldsUsed.cardinality() == fieldCount)
        {
            return new TrimResult(
                filter,
                Mappings.createIdentity(fieldCount));
        }

        // Build new project expressions, and populate the mapping.
        final RexVisitor<RexNode> shuttle =
            new RexPermuteInputsShuttle(
                (Mappings.TargetMapping) inputMapping);
        RexNode newConditionExpr =
            conditionExpr.accept(shuttle);

        final FilterRel newFilter =
            new FilterRel(
                filter.getCluster(),
                newInput,
                newConditionExpr);
        assert newFilter.getClass() == filter.getClass();

        // The result has the same mapping as the input gave us. Sometimes we
        // return fields that the consumer didn't ask for, because the filter
        // needs them for its condition.
        return new TrimResult(newFilter, inputMapping);
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for {@link SortRel}.
     */
    public TrimResult trimFields(
        SortRel sort,
        BitSet fieldsUsed)
    {
        final RelDataType rowType = sort.getRowType();
        final int fieldCount = rowType.getFieldCount();
        final RelFieldCollation[] collations = sort.getCollations();
        final RelNode input = sort.getChild();

        // We use the fields used by the consumer, plus any fields used as sort
        // keys.
        BitSet inputFieldsUsed = (BitSet) fieldsUsed.clone();
        for (RelFieldCollation collation : collations) {
            inputFieldsUsed.set(collation.getFieldIndex());
        }

        // Create input with trimmed columns.
        TrimResult trimResult =
            trimChild(sort, input, inputFieldsUsed);
        RelNode newInput = trimResult.left;
        final Mapping inputMapping = trimResult.right;

        // If the input is unchanged, and we need to project all columns,
        // there's nothing we can do.
        if (newInput == input
            && inputMapping.isIdentity()
            && fieldsUsed.cardinality() == fieldCount)
        {
            return new TrimResult(
                sort,
                Mappings.createIdentity(fieldCount));
        }

        final SortRel newSort =
            new SortRel(
                sort.getCluster(),
                newInput,
                RexUtil.apply(inputMapping, collations));
        assert newSort.getClass() == sort.getClass();

        // The result has the same mapping as the input gave us. Sometimes we
        // return fields that the consumer didn't ask for, because the filter
        // needs them for its condition.
        return new TrimResult(newSort, inputMapping);
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for {@link JoinRel}.
     */
    public TrimResult trimFields(
        JoinRel join,
        BitSet fieldsUsed)
    {
        final RelDataType rowType = join.getRowType();
        final int fieldCount = rowType.getFieldCount();
        final RexNode conditionExpr = join.getCondition();
        final int systemFieldCount = join.getSystemFieldList().size();

        // Add in fields used in the condition.
        BitSet fieldsUsedPlus = (BitSet) fieldsUsed.clone();
        RelOptUtil.InputFinder inputFinder =
            new RelOptUtil.InputFinder(fieldsUsedPlus);
        conditionExpr.accept(inputFinder);

        // If no system fields are used, we can remove them.
        int systemFieldUsedCount = 0;
        for (int i = 0; i < systemFieldCount; ++i) {
            if (fieldsUsed.get(i)) {
                ++systemFieldUsedCount;
            }
        }
        final int newSystemFieldCount;
        final List<RelDataTypeField> newSystemFieldList;
        if (systemFieldUsedCount == 0) {
            newSystemFieldCount = 0;
            newSystemFieldList = Collections.emptyList();
        } else {
            newSystemFieldCount = systemFieldCount;
            newSystemFieldList = join.getSystemFieldList();
        }

        int offset = systemFieldCount;
        int changeCount = 0;
        int newFieldCount = newSystemFieldCount;
        RelNode[] newInputs = join.getInputs().clone();
        List<Mapping> inputMappings = new ArrayList<Mapping>();
        for (int i = 0; i < join.getInputs().length; i++) {
            RelNode input = join.getInputs()[i];
            final RelDataType inputRowType = input.getRowType();
            final int inputFieldCount = inputRowType.getFieldCount();

            // Compute required mapping.
            BitSet inputFieldsUsed = new BitSet(inputFieldCount);
            for (int bit : Util.toIter(fieldsUsedPlus)) {
                if (bit >= offset && bit < offset + inputFieldCount) {
                    inputFieldsUsed.set(bit - offset);
                }
            }

            // If there are system fields, we automatically use the
            // corresponding field in each input.
            if (newSystemFieldCount > 0) {
                // calling with newSystemFieldCount == 0 should be safe but hits
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6222207
                inputFieldsUsed.set(0, newSystemFieldCount);
            }

            TrimResult trimResult = trimChild(join, input, inputFieldsUsed);
            if (trimResult.left != input) {
                ++changeCount;
                newInputs[i] = trimResult.left;
            }

            final Mapping inputMapping = trimResult.right;
            inputMappings.add(inputMapping);

            // Move offset to point to start of next input.
            offset += inputFieldCount;
            newFieldCount += inputMapping.getTargetCount();
        }

        Mapping mapping =
            Mappings.create(
                MappingType.InverseSurjection,
                fieldCount,
                newFieldCount);
        for (int i = 0; i < newSystemFieldCount; ++i) {
            mapping.set(i, i);
        }
        offset = systemFieldCount;
        int newOffset = newSystemFieldCount;
        for (int i = 0; i < inputMappings.size(); i++) {
            Mapping inputMapping = inputMappings.get(i);
            for (IntPair pair : inputMapping) {
                mapping.set(pair.source + offset, pair.target + newOffset);
            }
            offset += inputMapping.getSourceCount();
            newOffset += inputMapping.getTargetCount();
        }

        if (changeCount == 0
            && mapping.isIdentity())
        {
            return new TrimResult(join, Mappings.createIdentity(fieldCount));
        }

        // Build new join.
        final RexVisitor<RexNode> shuttle =
            new RexPermuteInputsShuttle(
                (Mappings.TargetMapping) mapping);
        RexNode newConditionExpr =
            conditionExpr.accept(shuttle);

        final JoinRel newJoin =
            join.copy(
                newConditionExpr, newSystemFieldList, newInputs[0],
                newInputs[1]);

        return new TrimResult(newJoin, mapping);
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for {@link SetOpRel}
     * (including UNION and UNION ALL).
     */
    public TrimResult trimFields(
        SetOpRel setOp,
        BitSet fieldsUsed)
    {
        final RelDataType rowType = setOp.getRowType();
        final int fieldCount = rowType.getFieldCount();
        int changeCount = 0;

        // Fennel abhors an empty row type, so pretend that the parent rel
        // wants the last field. (The last field is the least likely to be a
        // system field.)
        if (fieldsUsed.isEmpty()) {
            fieldsUsed.set(rowType.getFieldCount() - 1);
        }

        // Compute the desired field mapping. Give the consumer the fields they
        // want, in the order that they appear in the bitset.
        Mapping mapping =
            Mappings.create(
                MappingType.InverseSurjection,
                fieldCount,
                fieldsUsed.cardinality());
        int n = 0;
        for (int field : Util.toIter(fieldsUsed)) {
            mapping.set(field, n++);
        }

        // Create input with trimmed columns.
        final RelNode[] newInputs = new RelNode[setOp.getInputs().length];
        for (int i = 0; i < setOp.getInputs().length; i++) {
            RelNode input = setOp.getInputs()[i];
            TrimResult trimResult = trimChild(setOp, input, fieldsUsed);
            RelNode newInput = trimResult.left;
            final Mapping inputMapping = trimResult.right;

            // We want "mapping", the input gave us "inputMapping", compute
            // "remaining" mapping.
            //    |                   |                |
            //    |---------------- mapping ---------->|
            //    |-- inputMapping -->|                |
            //    |                   |-- remaining -->|
            //
            // For instance, suppose we have columns [a, b, c, d],
            // the consumer asked for mapping = [b, d],
            // and the transformed input has columns inputMapping = [d, a, b].
            // remaining will permute [b, d] to [d, a, b].
            Mapping remaining = Mappings.divide(mapping, inputMapping);

            // Create a projection; does nothing if remaining is identity.
            newInput = CalcRel.projectMapping(newInput, remaining, null);

            if (newInputs[i] != newInput) {
                ++changeCount;
            }
            newInputs[i] = newInput;
        }

        // If the input is unchanged, and we need to project all columns,
        // there's to do.
        if (changeCount == 0
            && mapping.isIdentity())
        {
            return new TrimResult(
                setOp,
                mapping);
        }

        RelNode newSetOp = setOp.copy(newInputs);
        return new TrimResult(newSetOp, mapping);
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for {@link AggregateRel}.
     */
    public TrimResult trimFields(
        AggregateRel aggregate,
        BitSet fieldsUsed)
    {
        // Fields:
        //
        // | sys fields | group fields | agg functions |
        //
        // Two kinds of trimming:
        //
        // 1. If agg rel has system fields but none of these are used, create an
        // agg rel with no system fields.
        //
        // 2. If aggregate functions are not used, remove them.
        //
        // But grouping fields stay, even if they are not used.

        final RelDataType rowType = aggregate.getRowType();
        final List<RelDataTypeField> sysFieldList =
            aggregate.getSystemFieldList();

        // Compute which input fields are used.
        // 1. system fields
        BitSet inputFieldsUsed = new BitSet();
        for (int i = 0; i < sysFieldList.size(); ++i) {
            if (fieldsUsed.get(i)) {
                inputFieldsUsed.set(i);
            }
        }
        // 2. group fields are always used
        for (int i : Util.toIter(aggregate.getGroupSet())) {
            inputFieldsUsed.set(i);
        }
        // 3. agg functions
        for (AggregateCall aggCall : aggregate.getAggCallList()) {
            for (int i : aggCall.getArgList()) {
                inputFieldsUsed.set(i);
            }
        }

        // Create input with trimmed columns.
        final RelNode input = aggregate.getInput(0);
        final TrimResult trimResult =
            trimChild(aggregate, input, inputFieldsUsed);
        final RelNode newInput = trimResult.left;
        final Mapping inputMapping = trimResult.right;

        // If the input is unchanged, and we need to project all columns,
        // there's nothing to do.
        if (input == newInput
            && fieldsUsed.equals(
                Util.bitSetBetween(0, rowType.getFieldCount())))
        {
            return new TrimResult(
                aggregate,
                Mappings.createIdentity(rowType.getFieldCount()));
        }

        // Pare down the system fields to the ones actually used by our
        // consumer.
        List<RelDataTypeField> newSysFieldList =
            new ArrayList<RelDataTypeField>();
        int j = 0;
        for (RelDataTypeField sysField : sysFieldList) {
            if (fieldsUsed.get(j++)) {
                newSysFieldList.add(sysField);
            }
        }

        // Which agg calls are used by our consumer?
        final int groupCount = aggregate.getGroupSet().cardinality();
        j = sysFieldList.size()
            + groupCount;
        int usedAggCallCount = 0;
        for (int i = 0; i < aggregate.getAggCallList().size(); i++) {
            if (fieldsUsed.get(j++)) {
                ++usedAggCallCount;
            }
        }

        // Offset due to the number of system fields having changed.
        Mapping mapping =
            Mappings.create(
                MappingType.InverseSurjection,
                rowType.getFieldCount(),
                newSysFieldList.size()
                + groupCount
                + usedAggCallCount);

        final BitSet newGroupSet =
            Mappings.apply(inputMapping, aggregate.getGroupSet());

        // Populate mapping of where to find the fields. System and grouping
        // fields first.
        for (IntPair pair : inputMapping) {
            if (pair.source
                < sysFieldList.size() + groupCount)
            {
                mapping.set(pair.source, pair.target);
            }
        }

        // Now create new agg calls, and populate mapping for them.
        final List<AggregateCall> newAggCallList =
            new ArrayList<AggregateCall>();
        j = sysFieldList.size()
            + groupCount;
        for (AggregateCall aggCall : aggregate.getAggCallList()) {
            if (fieldsUsed.get(j)) {
                AggregateCall newAggCall =
                    new AggregateCall(
                        aggCall.getAggregation(),
                        aggCall.isDistinct(),
                        Mappings.apply2(inputMapping, aggCall.getArgList()),
                        aggCall.getType(),
                        aggCall.getName());
                if (newAggCall.equals(aggCall)) {
                    newAggCall = aggCall; // immutable -> canonize to save space
                }
                mapping.set(
                    j,
                    newSysFieldList.size()
                    + groupCount
                    + newAggCallList.size());
                newAggCallList.add(newAggCall);
            }
            ++j;
        }

        RelNode newAggregate =
            new AggregateRel(
                aggregate.getCluster(),
                newInput,
                newSysFieldList,
                newGroupSet,
                newAggCallList);

        assert newAggregate.getClass() == aggregate.getClass();

        return new TrimResult(newAggregate, mapping);
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for
     * {@link TableModificationRel}.
     */
    public TrimResult trimFields(
        TableModificationRel modifier,
        BitSet fieldsUsed)
    {
        // Ignore what consumer wants. We always project all columns.
        Util.discard(fieldsUsed);

        final RelDataType rowType = modifier.getRowType();
        final int fieldCount = rowType.getFieldCount();
        RelNode input = modifier.getChild();

        // We want all fields from the child.
        final int inputFieldCount = input.getRowType().getFieldCount();
        BitSet inputFieldsUsed = Util.bitSetBetween(0, inputFieldCount);

        // Create input with trimmed columns.
        TrimResult trimResult = trimChild(modifier, input, inputFieldsUsed);
        RelNode newInput = trimResult.left;
        final Mapping inputMapping = trimResult.right;
        if (!inputMapping.isIdentity()) {
            // We asked for all fields. Can't believe that the child decided
            // to permute them!
            throw Util.newInternal(
                "Expected identity mapping, got " + inputMapping);
            /*
            Mapping remaining = Mappings.divide(mapping, inputMapping);

            // Create a projection; does nothing if remaining is identity.
            newInput = CalcRel.projectMapping(newInput, remaining, null);

             */
        }

        TableModificationRel newModifier = modifier;
        if (newInput != input) {
            newModifier =
                modifier.copy(newInput);
        }
        assert newModifier.getClass() == modifier.getClass();

        // Always project all fields.
        Mapping mapping = Mappings.createIdentity(fieldCount);
        return new TrimResult(newModifier, mapping);
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for
     * {@link TableFunctionRel}.
     */
    public TrimResult trimFields(
        TableFunctionRel tabFun,
        BitSet fieldsUsed)
    {
        final RelDataType rowType = tabFun.getRowType();
        final int fieldCount = rowType.getFieldCount();
        RelNode[] inputs = tabFun.getInputs();
        RelNode[] newInputs = inputs.clone();

        for (int i = 0; i < inputs.length; i++) {
            RelNode input = inputs[i];
            final int inputFieldCount = input.getRowType().getFieldCount();
            BitSet inputFieldsUsed = Util.bitSetBetween(0, inputFieldCount);

            // Create input with trimmed columns.
            TrimResult trimResult =
                trimChildRestore(tabFun, input, inputFieldsUsed);
            assert trimResult.right.isIdentity();
            newInputs[i] = trimResult.left;
        }

        TableFunctionRel newTabFun = tabFun;
        if (!Arrays.equals(inputs, newInputs)) {
            newTabFun = tabFun.copy(newInputs);
        }
        assert newTabFun.getClass() == tabFun.getClass();

        // Always project all fields.
        Mapping mapping = Mappings.createIdentity(fieldCount);
        return new TrimResult(newTabFun, mapping);
    }

    /**
     * Variant of {@link #trimFields(RelNode, BitSet)} for
     * {@link org.eigenbase.rel.ValuesRel}.
     */
    public TrimResult trimFields(
        ValuesRel values,
        BitSet fieldsUsed)
    {
        final RelDataType rowType = values.getRowType();
        final int fieldCount = rowType.getFieldCount();

        // If they are asking for no fields, we can't give them what they want,
        // because zero-column records are illegal. Give them the last field,
        // which is unlikely to be a system field.
        if (fieldsUsed.isEmpty()) {
            fieldsUsed = Util.bitSetBetween(fieldCount - 1, fieldCount);
        }

        // If all fields are used, return unchanged.
        if (fieldsUsed.equals(Util.bitSetBetween(0, fieldCount))) {
            Mapping mapping = Mappings.createIdentity(fieldCount);
            return new TrimResult(values, mapping);
        }

        List<List<RexLiteral>> newTuples = new ArrayList<List<RexLiteral>>();
        for (List<RexLiteral> tuple : values.getTuples()) {
            List<RexLiteral> newTuple = new ArrayList<RexLiteral>();
            for (int field : Util.toIter(fieldsUsed)) {
                newTuple.add(tuple.get(field));
            }
            newTuples.add(newTuple);
        }

        final Mapping mapping =
            Mappings.create(
                MappingType.InverseSurjection,
                fieldCount,
                fieldsUsed.cardinality());
        int i = 0;
        for (int field : Util.toIter(fieldsUsed)) {
            mapping.set(field, i++);
        }
        RelDataType newRowType =
            values.getCluster().getTypeFactory().createStructType(
                Mappings.apply3(mapping, rowType.getFieldList()));
        final ValuesRel newValues =
            new ValuesRel(values.getCluster(), newRowType, newTuples);
        return new TrimResult(newValues, mapping);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Result of an attempt to trim columns from a relational expression.
     *
     * <p>The mapping describes where to find the columns wanted by the parent
     * of the current relational expression.
     *
     * <p>The mapping is a
     * {@link org.eigenbase.util.mapping.Mappings.SourceMapping}, which means
     * that no column can be used more than once, and some columns are not used.
     * {@code columnsUsed.getSource(i)} returns the source of the i'th output
     * field.
     *
     * <p>For example, consider the mapping for a relational expression that
     * has 4 output columns but only two are being used. The mapping
     * {2 &rarr; 1, 3 &rarr; 0} would give the following behavior:</p>
     *
     * <ul>
     * <li>columnsUsed.getSourceCount() returns 4
     * <li>columnsUsed.getTargetCount() returns 2
     * <li>columnsUsed.getSource(0) returns 3
     * <li>columnsUsed.getSource(1) returns 2
     * <li>columnsUsed.getSource(2) throws IndexOutOfBounds
     * <li>columnsUsed.getTargetOpt(3) returns 0
     * <li>columnsUsed.getTargetOpt(0) returns -1
     * </ul>
     */
    protected static class TrimResult extends Pair<RelNode, Mapping>
    {
        /**
         * Creates a TrimResult.
         *
         * @param left New relational expression
         * @param right Mapping of fields onto original fields
         */
        public TrimResult(RelNode left, Mapping right)
        {
            super(left, right);
        }
    }
}

// End RelFieldTrimmer.java
