/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2002 SQLstream, Inc.
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

import java.lang.reflect.Constructor;
import java.util.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.RelDataType;
import org.eigenbase.rex.*;
import org.eigenbase.sql.SqlNode;
import org.eigenbase.util.Util;
import net.sf.farrago.query.FennelRel;


/**
 * FennelWindowRule is a rule for implementing a {@link CalcRel} which contains
 * windowed aggregates via a {@link FennelWindowRel}.
 *
 * <p>There are several instances of the rule ({@link #CalcOnWinOnCalc}, {@link
 * #CalcOnWin}, {@link #WinOnCalc}, {@link #Win}), which pull in any {@link
 * CalcRel} objects above or below. (It may be better to write a rule which
 * merges {@link CalcRel} and {@link WindowedAggregateRel} objects together,
 * thereby dealing with this problem at the logical level.)
 *
 * By default the rule produces a {@link FennelWindowRel}, but each instance can
 * be altered to produce a subclass instead: see {@link #setResultClass}.

 * @author jhyde
 * @version $Id$
 */
public abstract class FennelWindowRule
    extends RelOptRule
{
    //~ Instance fields

    // class of RelNode to produce
    private Class<FennelWindowRel> resultFactoryClass = FennelWindowRel.class;

    //~ Static fields/initializers ---------------------------------------------

    /**
     * Instance of the rule which matches {@link CalcRel} on top of {@link
     * WindowedAggregateRel} on top of {@link CalcRel}.
     */
    public static final FennelWindowRule CalcOnWinOnCalc =
        new FennelWindowRule(
            new RelOptRuleOperand(
                CalcRel.class,
                new RelOptRuleOperand(
                    WindowedAggregateRel.class,
                    new RelOptRuleOperand(
                        CalcRel.class,
                        ANY))),
            "FennelWindowRule.CalcOnWinOnCalc")
        {
            // implement RelOptRule
            public void onMatch(RelOptRuleCall call)
            {
                final CalcRel outCalc = (CalcRel) call.rels[0];
                final WindowedAggregateRel winAgg =
                    (WindowedAggregateRel) call.rels[1];
                final CalcRel inCalc = (CalcRel) call.rels[2];
                final RelNode child = inCalc.getChild();
                if (inCalc.getProgram().getCondition() != null) {
                    // FennelWindowRel cannot filter its input. Leave it to
                    // the Calc-on-Win rule.
                    return;
                }
                createRels(call, outCalc, winAgg, inCalc, child);
            }
        };

    /**
     * Instance of the rule which matches a {@link CalcRel} on top of a {@link
     * WindowedAggregateRel}.
     */
    public static final FennelWindowRule CalcOnWin =
        new FennelWindowRule(
            new RelOptRuleOperand(
                CalcRel.class,
                new RelOptRuleOperand(
                    WindowedAggregateRel.class,
                    new RelOptRuleOperand(
                        RelNode.class,
                        ANY))),
            "FennelWindowRule.CalcOnWin")
        {
            // implement RelOptRule
            public void onMatch(RelOptRuleCall call)
            {
                if (call.rels[2] instanceof CalcRel) {
                    // The Calc-on-Win-on-Calc rule will have dealt with this.
                    return;
                }
                final CalcRel outCalc = (CalcRel) call.rels[0];
                if (RexOver.containsOver(outCalc.getProgram())) {
                    return;
                }
                final WindowedAggregateRel winAggRel =
                    (WindowedAggregateRel) call.rels[1];
                final RelNode child = call.rels[2];
                if (child instanceof CalcRel) {
                    CalcRel calcRel = (CalcRel) child;
                    if (calcRel.getProgram().getCondition() == null) {
                        // The Calc-on-Win-on-Calc rule will deal with this.
                        return;
                    }
                }
                createRels(call, outCalc, winAggRel, null, child);
            }
        };

    /**
     * Instance of the rule which matches a {@link WindowedAggregateRel} on top
     * of a {@link CalcRel}.
     */
    public static final FennelWindowRule WinOnCalc =
        new FennelWindowRule(
            new RelOptRuleOperand(
                WindowedAggregateRel.class,
                new RelOptRuleOperand(
                    CalcRel.class,
                    ANY)),
            "FennelWindowRule.WinOnCalc")
        {
            // implement RelOptRule
            public void onMatch(RelOptRuleCall call)
            {
                final WindowedAggregateRel winAgg =
                    (WindowedAggregateRel) call.rels[0];
                final CalcRel inCalc = (CalcRel) call.rels[1];
                if (inCalc.getProgram().getCondition() != null) {
                    return;
                }
                final RelNode child = inCalc.getChild();
                createRels(call, null, winAgg, inCalc, child);
            }
        };

    /**
     * Instance of the rule which matches a {@link WindowedAggregateRel} on top
     * of a {@link RelNode}.
     */
    public static final FennelWindowRule Win =
        new FennelWindowRule(
            new RelOptRuleOperand(
                WindowedAggregateRel.class,
                new RelOptRuleOperand(
                    RelNode.class,
                    ANY)),
            "FennelWindowRule.Win")
        {
            // implement RelOptRule
            public void onMatch(RelOptRuleCall call)
            {
                final WindowedAggregateRel winAgg =
                    (WindowedAggregateRel) call.rels[0];
                final RelNode child = call.rels[1];
                if (child instanceof CalcRel) {
                    // The Win-Calc rule will deal with this.
                    return;
                }
                createRels(call, null, winAgg, null, child);
            }
        };

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a FennelWindowRule.
     *
     * @param operand root operand, must not be null
     *
     * @param description Description, or null to guess description
     */
    private FennelWindowRule(
        RelOptRuleOperand operand,
        String description)
    {
        super(operand, description);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Changes the class of result RelNode.
     * @return the changed rule.
     * @param c new class; must be a subtype of FennelWindowRel.
     */
    public FennelWindowRule setResultClass(Class c)
    {
        assert FennelWindowRel.class.isAssignableFrom(c) : "invalid arg class";
        this.resultFactoryClass = c;
        return this;
    }

    // implement RelOptRule
    public CallingConvention getOutConvention()
    {
        return FennelRel.FENNEL_EXEC_CONVENTION;
    }

    protected void createRels(
        RelOptRuleCall call,
        CalcRel outCalc,
        WindowedAggregateRel winAggRel,
        CalcRel inCalc,
        final RelNode child)
    {
        assert winAggRel != null;
        final RelOptCluster cluster = winAggRel.getCluster();
        RelTraitSet traits = null;
        if (child.getInputs().length > 0) {
            traits = RelOptUtil.clone(child.getInput(0).getTraits());
        } else {
            traits = RelOptUtil.clone(child.getTraits());
        }
        traits.setTrait(
            CallingConventionTraitDef.instance,
            FennelRel.FENNEL_EXEC_CONVENTION);
        RelNode fennelInput = child;
        if (!child.getTraits().equals(traits)) {
            fennelInput = mergeTraitsAndConvert(
                child.getTraits(),
                traits,
                child);
            if (fennelInput == null) {
                return;
            }
        }

        // The input calculations will be implemented using a Fennel
        // calculator. If they contain Java-specific operations, quit now. The
        // splitter rule will carve out the Java parts.
        if ((inCalc != null)
            && !FarragoAutoCalcRule.instance.canImplementInFennel(inCalc))
        {
            return;
        }

        // Likewise, the output calculations need to be implementable in a
        // Fennel calculator.
        if ((outCalc != null)
            && !FarragoAutoCalcRule.instance.canImplementInFennel(outCalc))
        {
            return;
        }

        // Build the input program.
        final RexProgram inProgram;
        if (inCalc == null) {
            inProgram = RexProgram.createIdentity(child.getRowType());
        } else {
            inProgram = inCalc.getProgram();
        }

        // Build a list of distinct windows, partitions and aggregate
        // functions.
        List<FennelWindowRel.Window> windowList =
            new ArrayList<FennelWindowRel.Window>();
        final Map<RexOver, FennelWindowRel.RexWinAggCall> aggMap =
            new HashMap<RexOver, FennelWindowRel.RexWinAggCall>();
        final RexProgram aggProgram =
            RexProgramBuilder.mergePrograms(
                winAggRel.getProgram(),
                inProgram,
                cluster.getRexBuilder(),
                false);

        // Make sure the orderkey is among the input expressions from the child.
        // Merged program creates intermediate rowtype below. OutputProgram is
        // then built based on the intermediate row which also holds winAgg
        // results. orderKey is resolved before intermediate rowtype is
        // constructed and "orderKey ordinal" may point to wrong offset within
        // intermediate row. dtbug #2209.
        for (RexNode expr : aggProgram.getExprList()) {
            if (expr instanceof RexOver) {
                RexOver over = (RexOver) expr;
                RexNode[] orderKeys = over.getWindow().orderKeys;
                for (RexNode orderKey : orderKeys) {
                    if (orderKey instanceof RexLocalRef
                        && ((RexLocalRef)orderKey).getIndex()
                            >= child.getRowType().getFieldCount())
                    {
                        // do not merge inCalc and winAggRel.
                        return;
                    }
                }
            }
        }

        // The purpose of the input program is to provide the expressions
        // needed by all of the aggregate functions. Its outputs are (a) all
        // of the input fields, followed by (b) the input expressions for the
        // aggregate functions.
        final RexProgramBuilder inputProgramBuilder =
            new RexProgramBuilder(
                aggProgram.getInputRowType(),
                cluster.getRexBuilder());
        int i = -1;
        for (RexNode expr : aggProgram.getExprList()) {
            ++i;
            if (expr instanceof RexInputRef) {
                inputProgramBuilder.addProject(
                    i,
                    aggProgram.getInputRowType().getFields()[i].getName());
            } else {
                inputProgramBuilder.addExpr(expr);
            }
        }

        // Build a list of windows, partitions, and aggregate functions. Each
        // aggregate function will add its arguments as outputs of the input
        // program.
        for (RexNode agg : aggProgram.getExprList()) {
            if (agg instanceof RexOver) {
                final RexOver over = (RexOver) agg;
                FennelWindowRel.RexWinAggCall aggCall =
                    addWindows(windowList, over, inputProgramBuilder);
                aggMap.put(over, aggCall);
            }
        }
        final RexProgram inputProgram = inputProgramBuilder.getProgram();

        // Partitioning expressions must be evaluated before rows enter the XO.
        // If windows partition on expressions defined in inCalc, don't try to
        // merge the expressions into the WinAgg XO: make inCalc its own XO.
        if ((inCalc != null)
            && isPartitioningOnCalcField(inCalc, windowList))
        {
            createRels(call, outCalc, winAggRel, null, inCalc);
            return;
        }

        // Now the windows are complete, compute their digests.
        for (FennelWindowRel.Window window : windowList) {
            window.computeDigest();
        }

        // Figure out the type of the inputs to the output program.
        // They are: the inputs to this rel, followed by the outputs of
        // each window.
        final List<FennelWindowRel.RexWinAggCall> flattenedAggCallList =
            new ArrayList<FennelWindowRel.RexWinAggCall>();
        List<String> intermediateNameList =
            new ArrayList<String>(
                RelOptUtil.getFieldNameList(child.getRowType()));
        final List<RelDataType> intermediateTypeList =
            new ArrayList<RelDataType>(
                RelOptUtil.getFieldTypeList(child.getRowType()));

        i = -1;
        for (FennelWindowRel.Window window : windowList) {
            ++i;
            int j = -1;
            for (FennelWindowRel.Partition p : window.getPartitionList()) {
                ++j;
                int k = -1;
                for (FennelWindowRel.RexWinAggCall over : p.overList) {
                    ++k;

                    // Add the k'th over expression of the j'th partition of
                    // the i'th window to the output of the program.
                    intermediateNameList.add("w" + i + "$p" + j + "$o" + k);
                    intermediateTypeList.add(over.getType());
                    flattenedAggCallList.add(over);
                }
            }
        }
        RelDataType intermediateRowType =
            cluster.getTypeFactory().createStructType(
                intermediateTypeList,
                intermediateNameList);

        // The output program is the windowed agg's program, combined with
        // the output calc (if it exists).
        RexProgramBuilder outputProgramBuilder =
            new RexProgramBuilder(
                intermediateRowType,
                cluster.getRexBuilder());
        final int inputFieldCount = child.getRowType().getFields().length;
        RexShuttle shuttle =
            new RexShuttle() {
                public RexNode visitOver(RexOver over)
                {
                    // Look up the aggCall which this expr was translated to.
                    final FennelWindowRel.RexWinAggCall aggCall =
                        aggMap.get(over);
                    assert aggCall != null;
                    assert RelOptUtil.eq(
                        "over",
                        over.getType(),
                        "aggCall",
                        aggCall.getType(),
                        true);

                    // Find the index of the aggCall among all partitions of all
                    // windows.
                    final int aggCallIndex =
                        flattenedAggCallList.indexOf(aggCall);
                    assert aggCallIndex >= 0;

                    // Replace expression with a reference to the window slot.
                    final int index = inputFieldCount + aggCallIndex;
                    assert RelOptUtil.eq(
                        "over",
                        over.getType(),
                        "intermed",
                        intermediateTypeList.get(index),
                        true);
                    return new RexInputRef(
                        index,
                        over.getType());
                }

                public RexNode visitLocalRef(RexLocalRef localRef)
                {
                    final int index = localRef.getIndex();
                    if (index < inputFieldCount) {
                        // Reference to input field.
                        return localRef;
                    }
                    return new RexLocalRef(
                        flattenedAggCallList.size() + index,
                        localRef.getType());
                }
            };
        for (RexNode expr : aggProgram.getExprList()) {
            expr = expr.accept(shuttle);
            outputProgramBuilder.registerInput(expr);
        }

        final List<String> fieldNames =
            RelOptUtil.getFieldNameList(winAggRel.getRowType());
        i = -1;
        for (RexLocalRef ref : aggProgram.getProjectList()) {
            ++i;
            int index = ref.getIndex();
            final RexNode expr = aggProgram.getExprList().get(index);
            RexNode expr2 = expr.accept(shuttle);
            outputProgramBuilder.addProject(
                outputProgramBuilder.registerInput(expr2),
                fieldNames.get(i));
        }

        // Create the output program.
        final RexProgram outputProgram;
        if (outCalc == null) {
            outputProgram = outputProgramBuilder.getProgram();
            assert RelOptUtil.eq(
                "type1",
                outputProgram.getOutputRowType(),
                "type2",
                winAggRel.getRowType(),
                true);
        } else {
            // Merge intermediate program (from winAggRel) with output program
            // (from outCalc).
            RexProgram intermediateProgram = outputProgramBuilder.getProgram();
            outputProgram =
                RexProgramBuilder.mergePrograms(
                    outCalc.getProgram(),
                    intermediateProgram,
                    cluster.getRexBuilder());
            assert RelOptUtil.eq(
                "type1",
                outputProgram.getInputRowType(),
                "type2",
                intermediateRowType,
                true);
            assert RelOptUtil.eq(
                "type1",
                outputProgram.getOutputRowType(),
                "type2",
                outCalc.getRowType(),
                true);
        }

        // Put all these programs together in the final relational expression.
        FennelWindowRel fennelCalcRel =
            newFennelWindowRel(
                cluster,
                fennelInput,
                windowList.toArray(
                    new FennelWindowRel.Window[windowList.size()]),
                inputProgram,
                outputProgram);
        RelTraitSet outTraits = fennelCalcRel.getTraits().clone();
        // copy over other traits from the child
        for (i = 0; i < traits.size(); i++) {
            RelTrait trait = traits.getTrait(i);
            if (trait.getTraitDef() != CallingConventionTraitDef.instance) {
                outTraits.addTrait(trait);
            }
        }
        // convert to the traits of the calling rel to which we are equivalent
        // and thus must have the same traits
        RelNode mergedFennelCalcRel = fennelCalcRel;
        if (!fennelCalcRel.getTraits().equals(outTraits)) {
            mergedFennelCalcRel = mergeTraitsAndConvert(
                fennelCalcRel.getTraits(),
                outTraits,
                fennelCalcRel);
        }
        call.transformTo(mergedFennelCalcRel);
    }

    /**
     * Returns whether any of the partitions in <code>windowList</code> uses a
     * field calculated in <code>inCalc</code>.
     *
     * @param inCalc Calculator relational expression
     * @param windowList List of windows
     *
     * @return Whether any partition has a calculated field
     */
    private boolean isPartitioningOnCalcField(
        CalcRel inCalc,
        List<FennelWindowRel.Window> windowList)
    {
        int inputFieldCount =
            inCalc.getProgram().getInputRowType().getFieldCount();
        for (FennelWindowRel.Window window : windowList) {
            for (FennelWindowRel.Partition p : window.getPartitionList()) {
                for (Integer partitionKey : p.partitionKeys) {
                    if (partitionKey >= inputFieldCount) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private FennelWindowRel.RexWinAggCall addWindows(
        List<FennelWindowRel.Window> windowList,
        RexOver over,
        RexProgramBuilder programBuilder)
    {
        final RexWindow aggWindow = over.getWindow();

        // Look up or create a window.
        Integer [] orderKeys =
            getProjectOrdinals(programBuilder, aggWindow.orderKeys);
        FennelWindowRel.Window fennelWindow =
            lookupWindow(
                windowList,
                aggWindow.isRows(),
                aggWindow.getLowerBound(),
                aggWindow.getUpperBound(),
                orderKeys);

        // Lookup or create a partition within the window.
        Integer [] partitionKeys =
            getProjectOrdinals(programBuilder, aggWindow.partitionKeys);
        FennelWindowRel.Partition fennelPartition =
            fennelWindow.lookupOrCreatePartition(partitionKeys);
        Util.discard(fennelPartition);

        // Create a clone the 'over' expression, omitting the window (which is
        // already part of the partition spec), and add the clone to the
        // partition.
        return fennelPartition.addOver(
            over.getType(),
            over.getAggOperator(),
            over.getOperands(),
            programBuilder);
    }

    /**
     * Converts a list of expressions into a list of ordinals that these
     * expressions are projected from a {@link RexProgramBuilder}. If an
     * expression is not projected, adds it.
     *
     * @param programBuilder Program builder
     * @param exprs List of expressions
     *
     * @return List of ordinals where expressions are projected
     */
    private Integer [] getProjectOrdinals(
        RexProgramBuilder programBuilder,
        RexNode [] exprs)
    {
        Integer [] newKeys = new Integer[exprs.length];
        for (int i = 0; i < newKeys.length; i++) {
            RexLocalRef operand = (RexLocalRef) exprs[i];
            List<RexLocalRef> projectList = programBuilder.getProjectList();
            int index = projectList.indexOf(operand);
            if (index < 0) {
                index = projectList.size();
                programBuilder.addProject(operand, null);
            }
            newKeys[i] = index;
        }
        return newKeys;
    }

    private FennelWindowRel.Window lookupWindow(
        List<FennelWindowRel.Window> windowList,
        boolean physical,
        SqlNode lowerBound,
        SqlNode upperBound,
        Integer [] orderKeys)
    {
        for (FennelWindowRel.Window window : windowList) {
            if ((physical == window.physical)
                && Util.equal(lowerBound, window.lowerBound)
                && Util.equal(upperBound, window.upperBound)
                && Arrays.equals(orderKeys, window.orderKeys))
            {
                return window;
            }
        }
        final FennelWindowRel.Window window =
            new FennelWindowRel.Window(
                physical,
                lowerBound,
                upperBound,
                orderKeys);
        windowList.add(window);
        return window;
    }

    /**
     * factory method for a FennelWindowRel
     */
    private FennelWindowRel newFennelWindowRel(
        RelOptCluster cluster,
        RelNode child,
        FennelWindowRel.Window[] windows,
        RexProgram inputProgram,
        RexProgram outputProgram)
    {
        if (resultFactoryClass == FennelWindowRel.class) {
            // default case; normal java to show what it means
            return new FennelWindowRel(
                cluster,
                child,
                outputProgram.getOutputRowType(),
                inputProgram,
                windows,
                outputProgram);
        } else {
            // otherwise call constructor by reflection
            try {
                final Constructor<FennelWindowRel> ctor;
                ctor = resultFactoryClass.getConstructor(
                    RelOptCluster.class,
                    RelNode.class,
                    RelDataType.class,
                    RexProgram.class,
                    FennelWindowRel.Window[].class,
                    RexProgram.class);
                return ctor.newInstance(
                    cluster,
                    child,
                    outputProgram.getOutputRowType(),
                    inputProgram,
                    windows,
                    outputProgram);
            } catch (Exception e) {
                assert false : "Internal error"; // FIX
                e.printStackTrace();
                return null;
            }
        }
    }
}

// End FennelWindowRule.java
