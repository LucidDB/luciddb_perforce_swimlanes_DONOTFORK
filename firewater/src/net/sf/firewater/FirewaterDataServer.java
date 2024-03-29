/*
// $Id$
// Firewater is a scaleout column store DBMS.
// Copyright (C) 2009-2009 John V. Sichi
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
package net.sf.firewater;

import java.math.*;
import java.sql.*;
import java.util.*;

import net.sf.farrago.fem.med.*;
import net.sf.farrago.fennel.*;
import net.sf.farrago.namespace.*;
import net.sf.farrago.namespace.jdbc.*;
import net.sf.farrago.catalog.*;
import net.sf.farrago.type.*;

import org.eigenbase.util.*;
import org.eigenbase.rel.*;
import org.eigenbase.rel.rules.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.fun.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.type.*;

import java.util.logging.*;
import net.sf.farrago.trace.*;

/**
 * FirewaterDataWrapper implements the
 * {@link net.sf.farrago.namespace.FarragoMedLocalDataServer}
 * interface for Firewater distributed tables.
 *
 * @author John Sichi
 * @version $Id$
 */
public class FirewaterDataServer
    extends MedJdbcDataServer
    implements FarragoMedLocalDataServer
{
    public static final String PROP_PARTITIONING = "PARTITIONING";
    public static final String DEFAULT_PARTITIONING =
        FirewaterPartitioning.HASH.toString();

    public static final String PROP_PARTITION_COLUMN = "PARTITION_COLUMN";
    public static final String DEFAULT_PARTITION_COLUMN = "";

    private static final Logger tracer
        = FarragoTrace.getClassTracer(FirewaterDataServer.class);

    protected FirewaterDataServer(
        String serverMofId,
        Properties props)
    {
        super(serverMofId, props);
    }

    // override MedJdbcDataServer
    public FarragoMedColumnSet newColumnSet(
        String [] localName,
        Properties tableProps,
        FarragoTypeFactory typeFactory,
        RelDataType rowType,
        Map<String, Properties> columnPropMap)
        throws SQLException
    {
        MedJdbcNameDirectory directory =
            new MedJdbcNameDirectory(this, localName[1]);
        SqlSelect select =
            SqlStdOperatorTable.selectOperator.createCall(
                null,
                new SqlNodeList(
                    Collections.singletonList(
                        new SqlIdentifier("*", SqlParserPos.ZERO)),
                    SqlParserPos.ZERO),
                new SqlIdentifier(localName, SqlParserPos.ZERO),
                null,
                null,
                null,
                null,
                null,
                SqlParserPos.ZERO);
        SqlDialect dialect = SqlDialect.create(getDatabaseMetaData());
        String partitioningString =
            tableProps.getProperty(PROP_PARTITIONING, DEFAULT_PARTITIONING);
        FirewaterPartitioning partitioning = null;
        for (FirewaterPartitioning p : FirewaterPartitioning.values()) {
            if (p.toString().equals(partitioningString)) {
                partitioning = p;
                break;
            }
        }
        if (partitioning == null) {
            throw FirewaterSessionFactory.res.InvalidPartitioning.ex(
                partitioningString);
        }
        String partition_column = tableProps.getProperty(
            PROP_PARTITION_COLUMN, DEFAULT_PARTITION_COLUMN);
        return new FirewaterColumnSet(
            directory,
            localName,
            localName,
            select,
            dialect,
            rowType,
            partitioning,
            partition_column);
    }

    // implement FarragoMedLocalDataServer
    public void setFennelDbHandle(FennelDbHandle fennelDbHandle)
    {
        // ignore
    }

    // implement FarragoMedLocalDataServer
    public void validateTableDefinition(
        FemLocalTable table,
        FemLocalIndex generatedPrimaryKeyIndex)
        throws SQLException
    {
        // no special validation rules yet
    }

    // implement FarragoMedLocalDataServer
    public void validateTableDefinition(
        FemLocalTable table,
        FemLocalIndex generatedPrimaryKeyIndex,
        boolean creation)
        throws SQLException
    {
        validateTableDefinition(table, generatedPrimaryKeyIndex);
    }

    // implement FarragoMedLocalDataServer
    public long createIndex(FemLocalIndex index, FennelTxnContext txnContext)
        throws SQLException
    {
        // no stored data, so dummy root ID
        return 0;
    }

    // implement FarragoMedLocalDataServer
    public void dropIndex(
        FemLocalIndex index,
        long rootPageId,
        boolean truncate,
        FennelTxnContext txnContext)
        throws SQLException
    {
        // ignore
    }

    // implement FarragoMedLocalDataServer
    public FarragoMedLocalIndexStats computeIndexStats(
        FemLocalIndex index,
        long rootPageId,
        boolean estimate,
        FennelTxnContext txnContext)
        throws SQLException
    {
        return new FarragoMedLocalIndexStats(0, -1);
    }

    // implement FarragoMedLocalDataServer
    public RelNode constructIndexBuildPlan(
        RelOptTable table,
        FemLocalIndex index,
        RelOptCluster cluster)
    {
        // Fake out the build plan with a dummy which returns a
        // rowcount of 0.
        RexLiteral zeroLit =
            cluster.getRexBuilder().makeBigintLiteral(new BigDecimal(0));
        List<RexLiteral> tuple = new ArrayList<RexLiteral>();
        tuple.add(zeroLit);
        List<List<RexLiteral>> tuples = new ArrayList<List<RexLiteral>>();
        tuples.add(tuple);
        return new ValuesRel(
            cluster,
            RelOptUtil.createDmlRowType(
                cluster.getTypeFactory()),
            tuples);
    }

    // implement FarragoMedLocalDataServer
    public void versionIndexRoot(
        Long oldRoot,
        Long newRoot,
        FennelTxnContext txnContext)
        throws SQLException
    {
        // ignore
    }

    // implement FarragoMedLocalDataServer
    public boolean supportsAlterTableAddColumn()
    {
        // maybe one day...
        return false;
    }

    // override MedJdbcDataServer
    public void registerRules(RelOptPlanner planner)
    {
        super.registerRules(planner);
        planner.addRule(RemoveTrivialProjectRule.instance);
        // TODO jvs 13-May-2009:  move this to LucidDB planner instead.
        // Also, need special case for grouping on partitioning key
        // (then we can skip top-level agg).
        //
        // ks 6-May-2011: special case added:
        planner.addRule(FirewaterPushDistinctRule.instance);
        planner.addRule(
            PushAggregateThroughUnionRule.instance);
        planner.addRule(
            PushJoinThroughUnionRule.instanceUnionOnLeft);
        planner.addRule(
            PushJoinThroughUnionRule.instanceUnionOnRight);
        // REVIEW jvs 13-May-2009:  Can this be moved up in LucidDB
        // Hep program?
        planner.addRule(
            ReduceAggregatesRule.instance);
        planner.addRule(
            PushProjectPastSetOpRule.instance);
        planner.addRule(
            FirewaterArbitraryReplicaRule.instance);
        planner.addRule(
            FirewaterReplicaJoinRule.instanceReplicaOnLeft);
        planner.addRule(
            FirewaterReplicaJoinRule.instanceReplicaOnRight);
        planner.addRule(MedJdbcProjectionPushDownRule.instance);
        planner.addRule(MedJdbcFilterPushDownRule.instance);
    }

    // override MedJdbcDataServer
    protected boolean isRemoteSqlValid(SqlNode sqlNode)
    {
        return true;
    }

    // override MedJdbcDataServer
    public MedJdbcDataServer testQueryCombination(
        MedJdbcDataServer other)
    {
        // TODO:  if other is something completely different, return null
        return other;
    }

    public static FirewaterPartitioning getPartitioning(
        FarragoRepos repos, FemLocalTable table)
    {
        Properties tableProps =
            FarragoCatalogUtil.getStorageOptionsAsProperties(repos, table);
        String partitioningString =
            tableProps.getProperty(PROP_PARTITIONING);
        if (partitioningString == null) {
            // default
            return FirewaterPartitioning.HASH;
        }
        // assume we've already validated this on creation, so
        // don't bother catching illegal values
        return Enum.valueOf(FirewaterPartitioning.class, partitioningString);
    }
}

// End FirewaterDataServer.java
