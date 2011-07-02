/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2004 SQLstream, Inc.
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

import net.sf.farrago.*;
import net.sf.farrago.fem.fennel.*;
import net.sf.farrago.fennel.calc.*;
import net.sf.farrago.query.*;

import org.eigenbase.rel.*;
import org.eigenbase.rel.metadata.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;
import org.eigenbase.sql.*;
import org.eigenbase.util.*;


/**
 * FennelWindowRel is the relational expression which computes windowed
 * aggregates inside of Fennel.
 *
 * <p>A window rel can handle several window aggregate functions, over several
 * partitions, with pre- and post-expressions, and an optional post-filter. Each
 * of the partitions is defined by a partition key (zero or more columns) and a
 * range (logical or physical). The partitions expect the data to be sorted
 * correctly on input to the relational expression.
 *
 * <p>Rules:
 *
 * <ul>
 * <li>{@link FennelWindowRule} creates this from a {@link CalcRel}</li>
 * <li>{@link WindowedAggSplitterRule} decomposes a {@link CalcRel} which
 * contains windowed aggregates into a {@link FennelWindowRel} and zero or more
 * {@link CalcRel}s which do not contain windowed aggregates</li>
 * </ul>
 * </p>
 *
 * @author jhyde
 * @version $Id$
 * @since Dec 6, 2004
 */
public class FennelWindowRel
    extends FennelSingleRel
{
    //~ Instance fields --------------------------------------------------------

    /**
     * Program which is applied on an incoming row event.
     *
     * <p>It provides the necessary expressions for the windows to work on. The
     * program always outputs the input fields first, then any necessary
     * expressions.
     *
     * <p>For example, for the query
     *
     * <blockquote><code>select ticker,<br/>
     * sum(amount * price) over last3<br/>
     * from Bids<br/>
     * window last3 as (order by orderid rows 3 preceding),<br/>
     * lastHour (order by orderdate range interval '1' hour preceding),<br/>
     * </code></blockquote>
     *
     * the program will output the expressions
     *
     * <blockquote><code>{orderid, ticker, amount, price, amount *
     * price}</code></blockquote>
     */
    private final RexProgram inputProgram;

    /**
     * Program which is applied on an 'row output' event.
     *
     * <p>Its input is the columns of the current input row, and all
     * accumulators of all windows of all partitions. Its output is the output
     * row.
     *
     * <p>For example, consider the query
     *
     * <blockquote><code>select ticker,<br/>
     * sum(amount * price) over last3,<br/>
     * 2 * sum(amount + 1) over lastHour,<br/>
     * from Bids<br/>
     * window last3 as (order by orderid rows 3 preceding),<br/>
     * lastHour (order by orderdate range interval '1' hour preceding),<br/>
     * </code></blockquote>
     *
     * The program has inputs
     *
     * <blockquote><code>{orderid, ticker, amount, price, sum(amount * price)
     * over last3, sum(amount + 1) over lastHour}</code></blockquote>
     *
     * and outputs
     *
     * <blockquote><code>{$1, $4, 2 * $5}</code></blockquote>
     */
    private final RexProgram outputProgram;
    private final Window [] windows;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a window relational expression.
     *
     * <p>Each {@link Window} has a set of {@link Partition} objects, and each
     * {@link Partition} object has a set of {@link RexOver} objects.
     *
     * @param cluster Cluster
     * @param child Input relational expression
     * @param rowType Output row type
     * @param inputProgram Program which computes input expressions for all
     * windows
     * @param windows Windows
     * @param outputProgram Program which computes output row from input columns
     * and all windows
     *
     * @pre inputProgram.getCondition() == null
     */
    protected FennelWindowRel(
        RelOptCluster cluster,
        RelNode child,
        RelDataType rowType,
        RexProgram inputProgram,
        Window [] windows,
        RexProgram outputProgram)
    {
        super(
            cluster,
            FennelRel.FENNEL_EXEC_CONVENTION.singletonSet,
            child);
        assert rowType != null : "precondition: rowType != null";
        assert outputProgram != null : "precondition: outputExprs != null";
        assert inputProgram != null : "precondition: inputProgram != null";
        assert inputProgram.projectsIdentity(true);
        assert inputProgram.getCondition() == null
            : "precondition: inputProgram.getCondition() == null";
        assert windows != null : "precondition: windows != null";
        assert windows.length > 0 : "precondition : windows.length > 0";

        // FIXME: jhyde, 2006/8/1: Assert is still valid for normal usage, but
        // I disabled assert to make it easier to construct a mock
        // FennelWindowRel for testing. See checkWinAgg in Rex2CalcPlanTest.
        assert true
            || (child.getConvention() == FennelRel.FENNEL_EXEC_CONVENTION);

        assert !RexOver.containsOver(inputProgram);
        assert !RexOver.containsOver(outputProgram);
        assert RelOptUtil.getFieldTypeList(outputProgram.getInputRowType())
            .equals(outputProgramInputTypes(child.getRowType(), windows));
        assert RelOptUtil.eq(
            "type1",
            outputProgram.getOutputRowType(),
            "type2",
            rowType,
            true);
        assert RelOptUtil.equal(
            "input row type", child.getRowType(),
            "input program row type", inputProgram.getInputRowType(),
            true);
        this.rowType = rowType;
        this.outputProgram = outputProgram;
        this.inputProgram = inputProgram;
        this.windows = windows;
    }

    //~ Methods ----------------------------------------------------------------

    private static List<RelDataType> outputProgramInputTypes(
        RelDataType rowType,
        Window [] windows)
    {
        List<RelDataType> typeList =
            new ArrayList<RelDataType>(RelOptUtil.getFieldTypeList(rowType));
        for (Window window : windows) {
            for (Partition partition : window.partitionList) {
                for (RexWinAggCall over : partition.overList) {
                    typeList.add(over.getType());
                }
            }
        }
        return typeList;
    }

    // override Object (public, does not throw CloneNotSupportedException)
    public FennelWindowRel clone()
    {
        return copy(getChild())
            .inheritTraitsFrom(this);
    }

    @Override
    public FennelWindowRel copy(
        RelNode... inputs)
    {
        assert inputs.length == 1;
        return new FennelWindowRel(
            getCluster(),
            inputs[0],
            rowType,
            inputProgram,
            windows,
            outputProgram);
    }

    protected Window[] getWindows()
    {
        return windows;
    }

    protected RexProgram getInputProgram()
    {
        return inputProgram;
    }

    protected RexProgram getOutputProgram()
    {
        return outputProgram;
    }

    public boolean isValid(boolean fail)
    {
        if (!super.isValid(fail)) {
            return false;
        }
        if (!inputProgram.isValid(fail)) {
            return false;
        }
        if (!outputProgram.isValid(fail)) {
            return false;
        }

        // In the window specifications, an aggregate call such as
        // 'SUM(RexInputRef #10)' refers to expression #10 of inputProgram.
        // (Not its projections.)
        final RexChecker checker =
            new RexChecker(
                inputProgram.getOutputRowType(),
                fail);
        int count = 0;
        for (Window window : windows) {
            for (Partition partition : window.partitionList) {
                for (RexWinAggCall over : partition.overList) {
                    ++count;
                    if (!checker.isValid(over)) {
                        return false;
                    }
                }
            }
        }
        if (count == 0) {
            assert !fail : "FennelWindowRel is empty";
            return false;
        }

        Set<Object> queueKeys = new LinkedHashSet<Object>();
        for (Window window : windows) {
            for (Partition partition : window.partitionList) {
                queueKeys.add(partition.makeQueueKey(window));
            }
        }
        if (queueKeys.size() != 1) {
            assert !fail
                : "windows require incompatible queues: "
                  + "windows=" + Arrays.toString(windows)
                  + ", queues=" + queueKeys;
            return false;
        }

        return true;
    }

    public RexNode [] getChildExps()
    {
        // Do not return any child exps. inputExprs, outputExprs and
        // conditionExpr (which are RexNode[]s) are handled along with windows
        // (which is not a RexNode[]) by explain.
        return RexNode.EMPTY_ARRAY;
    }

    public void explain(RelOptPlanWriter pw)
    {
        final List<Object> valueList = new ArrayList<Object>();
        final List<String> termList = new ArrayList<String>();
        getExplainTerms(termList, valueList);
        pw.explain(
            this,
            termList.toArray(new String[termList.size()]),
            valueList.toArray(new Object[valueList.size()]));
    }

    protected void getExplainTerms(List<String> terms, List<Object> values)
    {
        terms.add("child");
        inputProgram.collectExplainTerms("in-", terms, values);
        outputProgram.collectExplainTerms("out-", terms, values);
        for (int i = 0; i < windows.length; i++) {
            Window window = windows[i];
            terms.add("window#" + i);
            values.add(window.toString());
        }
    }

    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        // Cost is proportional to the number of rows and the number of
        // components (windows, partitions, and aggregate functions). There is
        // no I/O cost.
        //
        // TODO #1. Add memory cost. Memory cost is higher for MIN and MAX
        //    than say SUM and COUNT (because they maintain a binary tree).
        // TODO #2. MIN and MAX have higher CPU cost than SUM and COUNT.
        final double rowsIn = RelMetadataQuery.getRowCount(getChild());
        int count = windows.length;
        for (int i = 0; i < windows.length; i++) {
            Window window = windows[i];
            count += window.partitionList.size();
            for (Partition partition : window.partitionList) {
                count += partition.overList.size();
            }
        }
        if (outputProgram.getCondition() != null) {
            ++count;
        }
        return planner.makeCost(rowsIn, rowsIn * count, 0);
    }

    // for toStreamDef()
    protected void fillStreamDef(
        FemWindowStreamDef def,
        FennelRelImplementor implementor)
    {
        final FarragoMetadataFactory repos = implementor.getRepos();

        implementor.addDataFlowFromProducerToConsumer(
            implementor.visitFennelChild((FennelRel) getChild(), 0),
            def);

        def.setFilter(outputProgram.getCondition() != null);

        // Generate output program.
        RexToCalcTranslator translator =
            new RexToCalcTranslator(
                getCluster().getRexBuilder(),
                this);
        String program =
            translator.generateProgram(
                outputProgram.getInputRowType(),
                outputProgram);
        def.setOutputProgram(program);

        // Setup sort list.
        final List<RelCollation> collationList = getChild().getCollationList();
        List<Integer> sortFieldList = new ArrayList<Integer>();
        if (!collationList.isEmpty()) {
            final RelCollation collation = collationList.get(0);
            for (RelFieldCollation fc : collation.getFieldCollations()) {
                sortFieldList.add(fc.getFieldIndex());
            }
        }
        Integer [] sortFields =
            sortFieldList.toArray(new Integer[sortFieldList.size()]);
        def.setInputOrderKeyList(
            FennelRelUtil.createTupleProjection(repos, sortFields));

        // For each window...
        for (int i = 0; i < windows.length; i++) {
            Window window = windows[i];
            final FemWindowDef windowDef = repos.newFemWindowDef();
            def.getWindow().add(windowDef);
            windowDef.setPhysical(window.physical);
            windowDef.setOrderKeyList(
                FennelRelUtil.createTupleProjection(repos, window.orderKeys));

            // Range is increased by 1 if it is physical and it spans the
            // current row.
            //
            // Case                                  offset range
            // ===================================== ====== =====
            // 3 preceding:                               0     4
            // 3 following:                               3     4
            // between 10 preceding and 2 preceding:     -2     8
            // between 3 preceding and 2 following:       2     6
            // between 2 following and 6 following:       6     4
            // between current row and current row:       0     1
            // between unbounded preceding
            //     and 3 preceding                       -3  +inf
            // between unbounded preceding
            //     and 3 following                        3  +inf
            // between 3 preceding
            //     and unbounded following      not representable
            SqlWindowOperator.OffsetRange offsetAndRange =
                SqlWindowOperator.getOffsetAndRange(
                    window.lowerBound,
                    window.upperBound,
                    window.physical);
            assert offsetAndRange != null;
            assert offsetAndRange.range >= 0;
            windowDef.setOffset((int) offsetAndRange.offset);
            windowDef.setRange(String.valueOf(offsetAndRange.range));

            RelDataType inputRowType = getChild().getRowType();
            assert RelOptUtil.equal(
                "input row type", inputRowType,
                "input program row type", inputProgram.getInputRowType(),
                true);

            // For each partition...
            for (Partition partition : window.partitionList) {
                final FemWindowPartitionDef windowPartitionDef =
                    repos.newFemWindowPartitionDef();
                windowDef.getPartition().add(windowPartitionDef);
                translator =
                    new RexToCalcTranslator(
                        getCluster().getRexBuilder(),
                        this);

                // Create a program for the window partition to init, add, drop
                // rows. Does not include the expression to form the output
                // record.
                RexProgram combinedProgram =
                    makeProgram(
                        getCluster().getRexBuilder(),
                        inputProgram,
                        partition.overList);

                final String initProgram =
                    translator.getAggProgram(combinedProgram, AggOp.Init, null);
                final String addProgram =
                    translator.getAggProgram(combinedProgram, AggOp.Add, null);
                final BitSet bitSet;
                final boolean ENABLE_SLIM_DROP_QUEUE = false;
                if (ENABLE_SLIM_DROP_QUEUE) {
                    bitSet = new BitSet();
                } else {
                    bitSet = null;
                }
                final String dropProgram =
                    translator.getAggProgram(
                        combinedProgram, AggOp.Drop, bitSet);

                windowPartitionDef.setInitializeProgram(initProgram);
                windowPartitionDef.setAddProgram(addProgram);
                windowPartitionDef.setDropProgram(dropProgram);
                if (ENABLE_SLIM_DROP_QUEUE) {
                    windowPartitionDef.setDropProgramKeyList(
                        FennelRelUtil.createTupleProjection(
                            repos, Util.toList(bitSet)));
                }

                List<RexNode> dups =
                    removeDuplicates(translator, partition.overList);
                final FemTupleDescriptor bucketDesc =
                    FennelRelUtil.createTupleDescriptorFromRexNode(repos, dups);
                windowPartitionDef.setBucketDesc(bucketDesc);

                windowPartitionDef.setPartitionKeyList(
                    FennelRelUtil.createTupleProjection(
                        repos,
                        partition.partitionKeys));
            }
        }
    }

    public FemExecutionStreamDef toStreamDef(FennelRelImplementor implementor)
    {
        final FarragoMetadataFactory repos = implementor.getRepos();
        final FemWindowStreamDef def = repos.newFemWindowStreamDef();
        fillStreamDef(def, implementor);
        return def;
    }

    /**
     * Assuming that this FennelWindowRel contains one window with one
     * partition, return the add/init/drop programs for that partition. For
     * testing purposes.
     */
    public String [] getSoleProgram()
    {
        for (Window window : windows) {
            for (Partition partition : window.partitionList) {
                RexToCalcTranslator translator =
                    new RexToCalcTranslator(
                        getCluster().getRexBuilder(),
                        this);

                RexProgram combinedProgram =
                    makeProgram(
                        getCluster().getRexBuilder(),
                        inputProgram,
                        partition.overList);

                String outputProgramString =
                    translator.generateProgram(
                        outputProgram.getInputRowType(),
                        outputProgram);

                final String initProgram =
                    translator.getAggProgram(combinedProgram, AggOp.Init, null);
                final String addProgram =
                    translator.getAggProgram(combinedProgram, AggOp.Add, null);
                final String dropProgram =
                    translator.getAggProgram(combinedProgram, AggOp.Drop, null);
                final BitSet bitSet = new BitSet();
                final String dropProgramOptimized =
                    translator.getAggProgram(
                        combinedProgram, AggOp.Drop, bitSet);
                final String dropProgramMapping = bitSet.toString();
                return new String[] {
                    initProgram, addProgram, dropProgram, dropProgramOptimized,
                    dropProgramMapping, outputProgramString};
            }
        }

        throw Util.newInternal("expected one window with one partition");
    }

    /**
     * Creates a program with one output field per windowed aggregate
     * expression.
     *
     * @param rexBuilder Expression builder
     * @param bottomProgram Calculates the inputs to the program
     * @param overList Aggregate expressions
     *
     * @return Combined program
     *
     * @see RexProgramBuilder#mergePrograms(RexProgram, RexProgram, RexBuilder)
     * @pre bottomPogram.getCondition() == null
     * @post return.getProjectList().size() == overList.size()
     */
    public static RexProgram makeProgram(
        RexBuilder rexBuilder,
        RexProgram bottomProgram,
        List<RexWinAggCall> overList)
    {
        assert bottomProgram.getCondition() == null
            : "pre: bottomPogram.getCondition() == null";
        assert bottomProgram.isValid(true);

        final RexProgramBuilder topProgramBuilder =
            new RexProgramBuilder(
                bottomProgram.getOutputRowType(),
                rexBuilder);
        for (int i = 0; i < overList.size(); i++) {
            RexCall over = overList.get(i);
            topProgramBuilder.addProject(
                over,
                RexInputRef.createName(i));
        }
        final RexProgram topProgram = topProgramBuilder.getProgram();

        // Merge the programs.
        final RexProgram mergedProgram =
            RexProgramBuilder.mergePrograms(
                topProgram,
                bottomProgram,
                rexBuilder);

        assert mergedProgram.getProjectList().size() == overList.size()
            : "post: return.getProjectList().size() == overList.size()";
        return mergedProgram;
    }

    // TODO: add a duplicate-elimination feature to RexProgram, use that, and
    //   obsolete this method
    private List<RexNode> removeDuplicates(
        RexToCalcTranslator translator,
        List<RexWinAggCall> outputExps)
    {
        Map<Object, RexWinAggCall> dups = new HashMap<Object, RexWinAggCall>();
        for (RexWinAggCall node : outputExps) {
            // This should be aggregate input.
            Object key = translator.getKey(node);
            if (dups.containsKey(key)) {
                continue;
            }
            dups.put(key, node);
        }
        List<RexNode> nodes = new ArrayList<RexNode>(dups.size());
        for (RexWinAggCall node : outputExps) {
            Object key = translator.getKey(node);
            if (dups.containsKey(key)) {
                nodes.add(node);
                dups.remove(key);
            }
        }
        return nodes;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * A Window is a range of input rows, defined by an upper and lower bound.
     * It also contains a list of {@link Partition} objects.
     *
     * <p>A window is either logical or physical. A physical window is measured
     * in terms of row count. A logical window is measured in terms of rows
     * within a certain distance from the current sort key.
     *
     * <p>For example:
     *
     * <ul>
     * <li><code>ROWS BETWEEN 10 PRECEDING and 5 FOLLOWING</code> is a physical
     * window with an upper and lower bound;
     * <li><code>RANGE BETWEEN INTERVAL '1' HOUR PRECEDING AND UNBOUNDED
     * FOLLOWING</code> is a logical window with only a lower bound;
     * <li><code>RANGE INTERVAL '10' MINUTES PRECEDING</code> (which is
     * equivalent to <code>RANGE BETWEEN INTERVAL '10' MINUTES PRECEDING AND
     * CURRENT ROW</code>) is a logical window with an upper and lower bound.
     * </ul>
     */
    public static class Window
    {
        /**
         * The partitions which make up this window.
         */
        private final List<Partition> partitionList =
            new ArrayList<Partition>();
        final boolean physical;
        final SqlNode lowerBound;
        final SqlNode upperBound;
        public final Integer [] orderKeys;
        private String digest;

        Window(
            boolean physical,
            SqlNode lowerBound,
            SqlNode upperBound,
            Integer [] ordinals)
        {
            assert ordinals != null : "precondition: ordinals != null";
            this.physical = physical;
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.orderKeys = ordinals;
        }

        public String toString()
        {
            return digest;
        }

        public void computeDigest()
        {
            final StringBuilder buf = new StringBuilder();
            computeDigest(buf);
            this.digest = buf.toString();
        }

        private void computeDigest(StringBuilder buf)
        {
            buf.append("window(");
            buf.append("order by {");
            for (int i = 0; i < orderKeys.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append("$");
                buf.append(orderKeys[i].intValue());
            }
            buf.append("}");
            buf.append(physical ? " rows " : " range ");
            if (lowerBound != null) {
                if (upperBound != null) {
                    buf.append("between ");
                    buf.append(lowerBound.toString());
                    buf.append(" and ");
                } else {
                    buf.append(lowerBound.toString());
                }
            }
            if (upperBound != null) {
                buf.append(upperBound.toString());
            }
            buf.append(" partitions(");
            int i = 0;
            for (Partition partition : partitionList) {
                if (i++ > 0) {
                    buf.append(", ");
                }
                partition.computeDigest(buf);
            }
            buf.append(")");
            buf.append(")");
        }

        public boolean equals(Object obj)
        {
            return (obj instanceof Window)
                && this.digest.equals(((Window) obj).digest);
        }

        public Partition lookupOrCreatePartition(Integer [] partitionKeys)
        {
            for (Partition partition : partitionList) {
                if (Arrays.equals(partition.partitionKeys, partitionKeys)) {
                    return partition;
                }
            }
            Partition partition = new Partition(partitionKeys);
            partitionList.add(partition);
            return partition;
        }

        public List<Partition> getPartitionList()
        {
            return partitionList;
        }
    }

    /**
     * A Partition is a collection of windowed aggregate expressions which
     * belong to the same {@link Window} and have the same partitioning keys.
     */
    public static class Partition
    {
        /**
         * Array of {@link RexWinAggCall} objects, each of which is a call to a
         * {@link SqlAggFunction}.
         */
        final List<RexWinAggCall> overList = new ArrayList<RexWinAggCall>();

        /**
         * The ordinals of the input columns which uniquely identify rows in
         * this partition. May be empty. Must not be null.
         */
        final Integer [] partitionKeys;

        Partition(Integer [] partitionKeys)
        {
            assert partitionKeys != null;
            this.partitionKeys = partitionKeys;
        }

        public boolean equals(Object obj)
        {
            if (obj instanceof Partition) {
                Partition that = (Partition) obj;
                if (Arrays.equals(this.partitionKeys, that.partitionKeys)) {
                    return true;
                }
            }
            return false;
        }

        public Object makeQueueKey(Window window)
        {
            final ArrayList<Object> list = new ArrayList<Object>();
            list.add(window.physical);
            list.add(window.lowerBound);
            list.add(window.upperBound);
            if (window.physical) {
                list.add(Arrays.toString(partitionKeys));
            }
            return list;
        }

        private void computeDigest(StringBuilder buf)
        {
            buf.append("partition(");
            buf.append("partition by {");
            for (int i = 0; i < partitionKeys.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append("$");
                buf.append(partitionKeys[i].intValue());
            }
            buf.append("} aggs {");
            int i = -1;
            for (RexCall aggCall : overList) {
                if (++i > 0) {
                    buf.append(", ");
                }
                buf.append(aggCall.toString());
            }
            buf.append("}");
            buf.append(")");
        }

        public RexWinAggCall addOver(
            RelDataType type,
            SqlAggFunction operator,
            RexNode [] operands,
            RexProgramBuilder programBuilder)
        {
            // Convert operands to inputRefs -- they will refer to the output
            // fields of a lower program.
            RexNode [] clonedOperands = operands.clone();
            for (int i = 0; i < operands.length; i++) {
                RexLocalRef operand = (RexLocalRef) operands[i];
                List<RexLocalRef> projectList = programBuilder.getProjectList();
                int index = projectList.indexOf(operand);
                if (index < 0) {
                    index = projectList.size();
                    programBuilder.addProject(operand, null);
                }
                clonedOperands[i] =
                    new RexInputRef(
                        index,
                        operand.getType());
            }
            final RexWinAggCall aggCall =
                new RexWinAggCall(
                    operator,
                    type,
                    clonedOperands,
                    overList.size());
            overList.add(aggCall);
            return aggCall;
        }

        public List<RexWinAggCall> getOvers()
        {
            return overList;
        }
    }

    /**
     * A call to a windowed aggregate function.
     *
     * <p>Belongs to a {@link Partition}.
     *
     * <p>It's a bastard son of a {@link RexCall}; similar enough that it gets
     * visited by a {@link RexVisitor}, but it also has some extra data members.
     */
    public static class RexWinAggCall
        extends RexCall
    {
        /**
         * Ordinal of this aggregate within its partition.
         */
        public final int ordinal;

        /**
         * Creates a RexWinAggCall.
         *
         * @param aggFun Aggregate function
         * @param type Result type
         * @param operands Operands to call
         * @param ordinal Ordinal within its partition
         */
        public RexWinAggCall(
            SqlAggFunction aggFun,
            RelDataType type,
            RexNode [] operands,
            int ordinal)
        {
            super(type, aggFun, operands);
            this.ordinal = ordinal;
        }
    }
}

// End FennelWindowRel.java
