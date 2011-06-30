/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2006 The Eigenbase Project
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
package org.eigenbase.test;

import java.util.*;

import junit.framework.*;

import openjava.mop.*;

import org.eigenbase.oj.util.*;
import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.fun.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.sql.validate.*;
import org.eigenbase.sql2rel.*;
import org.eigenbase.util.*;


/**
 * SqlToRelTestBase is an abstract base for tests which involve conversion from
 * SQL to relational algebra.
 *
 * <p>SQL statements to be translated can use the schema defined in {@link
 * MockCatalogReader}; note that this is slightly different from Farrago's SALES
 * schema. If you get a parser or validator error from your test SQL, look down
 * in the stack until you see "Caused by", which will usually tell you the real
 * error.
 *
 * @author jhyde
 * @version $Id$
 */
public abstract class SqlToRelTestBase
    extends TestCase
{
    //~ Static fields/initializers ---------------------------------------------

    protected static final String NL = System.getProperty("line.separator");

    //~ Instance fields --------------------------------------------------------

    protected final Tester tester = createTester();

    //~ Methods ----------------------------------------------------------------

    public SqlToRelTestBase()
    {
        super();
    }

    public SqlToRelTestBase(String name)
    {
        super(name);
    }

    protected Tester createTester()
    {
        return new TesterImpl(getDiffRepos());
    }

    /**
     * Returns the default diff repository for this test, or null if there is
     * no repository.
     *
     * <p>The default implementation returns null.
     *
     * <p>Sub-classes that want to use a diff repository can override.
     * Sub-sub-classes can override again, inheriting test cases and overriding
     * selected test results.
     *
     * <p>And individual test cases can override by providing a different
     * tester object.
     *
     * @return Diff repository
     */
    protected DiffRepository getDiffRepos()
    {
        return null;
    }

    //~ Inner Interfaces -------------------------------------------------------

    /**
     * Helper class which contains default implementations of methods used for
     * running sql-to-rel conversion tests.
     */
    public static interface Tester
    {
        /**
         * Converts a SQL string to a {@link RelNode} tree.
         *
         * @param sql SQL statement
         *
         * @return Relational expression, never null
         *
         * @pre sql != null
         * @post return != null
         */
        RelNode convertSqlToRel(String sql);

        SqlNode parseQuery(String sql)
            throws Exception;

        /**
         * Factory method to create a {@link SqlValidator}.
         */
        SqlValidator createValidator(
            SqlValidatorCatalogReader catalogReader,
            RelDataTypeFactory typeFactory);

        /**
         * Factory method for a {@link SqlValidatorCatalogReader}.
         */
        SqlValidatorCatalogReader createCatalogReader(
            RelDataTypeFactory typeFactory);

        RelOptPlanner createPlanner();

        /**
         * Returns the {@link SqlOperatorTable} to use.
         */
        SqlOperatorTable getOperatorTable();

        MockRelOptSchema createRelOptSchema(
            SqlValidatorCatalogReader catalogReader,
            RelDataTypeFactory typeFactory);

        /**
         * Returns the SQL dialect to test.
         */
        SqlConformance getConformance();

        /**
         * Checks that a SQL statement converts to a given plan.
         *
         * @param sql SQL query
         * @param plan Expected plan
         */
        void assertConvertsTo(
            String sql,
            String plan);

        /**
         * Checks that a SQL statement converts to a given plan, optionally
         * trimming columns that are not needed.
         *
         * @param sql SQL query
         * @param plan Expected plan
         */
        void assertConvertsTo(
            String sql,
            String plan,
            boolean trim);

        /**
         * Returns the diff repository.
         *
         * @return Diff repository
         */
        DiffRepository getDiffRepos();

        /**
         * Returns the validator.
         *
         * @return Validator
         */
        SqlValidator getValidator();
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Mock implementation of {@link RelOptSchema}.
     */
    protected static class MockRelOptSchema
        implements RelOptSchemaWithSampling
    {
        private final SqlValidatorCatalogReader catalogReader;
        private final RelDataTypeFactory typeFactory;
        protected final List<RelDataTypeField> systemFieldList;

        public MockRelOptSchema(
            SqlValidatorCatalogReader catalogReader,
            RelDataTypeFactory typeFactory,
            List<RelDataTypeField> systemFieldList)
        {
            this.catalogReader = catalogReader;
            this.typeFactory = typeFactory;
            this.systemFieldList = systemFieldList;
            assert systemFieldList != null;
        }

        public RelOptTable getTableForMember(String [] names)
        {
            final SqlValidatorTable table = catalogReader.getTable(names);
            final RelDataType rowType = table.getRowType();
            final List<RelCollation> collationList =
                new ArrayList<RelCollation>();

            // Deduce which fields the table is sorted on.
            int i = -1;
            for (RelDataTypeField field : rowType.getFields()) {
                ++i;
                final SqlMonotonicity monotonicity =
                    table.getMonotonicity(field.getName());
                if (monotonicity != SqlMonotonicity.NotMonotonic) {
                    final RelFieldCollation.Direction direction =
                        monotonicity.isDecreasing()
                        ? RelFieldCollation.Direction.Descending
                        : RelFieldCollation.Direction.Ascending;
                    collationList.add(
                        new RelCollationImpl(
                            Collections.singletonList(
                                new RelFieldCollation(
                                    i,
                                    direction))));
                }
            }
            if (names.length < 3) {
                String [] newNames = { "CATALOG", "SALES", "" };
                System.arraycopy(
                    names,
                    0,
                    newNames,
                    newNames.length - names.length,
                    names.length);
                names = newNames;
            }
            return createColumnSet(table, names, rowType, collationList);
        }

        public RelOptTable getTableForMember(
            String [] names,
            final String datasetName,
            boolean [] usedDataset)
        {
            final RelOptTable table = getTableForMember(names);

            // If they're asking for a sample, just for test purposes,
            // assume there's a table called "<table>:<sample>".
            RelOptTable datasetTable =
                new DelegatingRelOptTable(table) {
                    public String [] getQualifiedName()
                    {
                        final String [] qualifiedName =
                            super.getQualifiedName().clone();
                        qualifiedName[qualifiedName.length - 1] +=
                            ":" + datasetName;
                        return qualifiedName;
                    }
                };
            if (usedDataset != null) {
                assert usedDataset.length == 1;
                usedDataset[0] = true;
            }
            return datasetTable;
        }

        protected MockColumnSet createColumnSet(
            SqlValidatorTable table,
            String [] names,
            final RelDataType rowType,
            final List<RelCollation> collationList)
        {
            return new MockColumnSet(
                names, rowType, collationList, systemFieldList);
        }

        public RelDataTypeFactory getTypeFactory()
        {
            return typeFactory;
        }

        public void registerRules(RelOptPlanner planner)
            throws Exception
        {
        }

        protected class MockColumnSet
            implements RelOptTable
        {
            private final String [] names;
            private final RelDataType rowType;
            private final List<RelDataTypeField> systemFieldList;
            private final List<RelCollation> collationList;

            protected MockColumnSet(
                String[] names,
                RelDataType rowType,
                final List<RelCollation> collationList,
                List<RelDataTypeField> systemFieldList)
            {
                this.names = names;
                this.rowType = rowType;
                this.collationList = collationList;
                this.systemFieldList = systemFieldList;
                assert names != null;
                assert rowType != null;
                assert collationList != null;
                assert systemFieldList != null;
            }

            public String [] getQualifiedName()
            {
                return names;
            }

            public double getRowCount()
            {
                // use something other than 0 to give costing tests
                // some room, and make emps bigger than depts for
                // join asymmetry
                if (names[names.length - 1].equals("EMP")) {
                    return 1000;
                } else {
                    return 100;
                }
            }

            public RelDataType getRowType()
            {
                return rowType;
            }

            public List<RelDataTypeField> getSystemFieldList()
            {
                return systemFieldList;
            }

            public RelOptSchema getRelOptSchema()
            {
                return MockRelOptSchema.this;
            }

            public RelNode toRel(
                RelOptCluster cluster,
                RelOptConnection connection)
            {
                return new TableAccessRel(cluster, this, connection);
            }

            public List<RelCollation> getCollationList()
            {
                return collationList;
            }
        }
    }

    private static class DelegatingRelOptTable
        implements RelOptTable
    {
        private final RelOptTable parent;

        public DelegatingRelOptTable(RelOptTable parent)
        {
            this.parent = parent;
        }

        public String [] getQualifiedName()
        {
            return parent.getQualifiedName();
        }

        public double getRowCount()
        {
            return parent.getRowCount();
        }

        public RelDataType getRowType()
        {
            return parent.getRowType();
        }

        public List<RelDataTypeField> getSystemFieldList()
        {
            return parent.getSystemFieldList();
        }

        public RelOptSchema getRelOptSchema()
        {
            return parent.getRelOptSchema();
        }

        public RelNode toRel(
            RelOptCluster cluster,
            RelOptConnection connection)
        {
            return new TableAccessRel(cluster, this, connection);
        }

        public List<RelCollation> getCollationList()
        {
            return parent.getCollationList();
        }
    }

    /**
     * Mock implementation of {@link RelOptConnection}, contains a {@link
     * MockRelOptSchema}.
     */
    private static class MockRelOptConnection
        implements RelOptConnection
    {
        private final RelOptSchema relOptSchema;

        public MockRelOptConnection(RelOptSchema relOptSchema)
        {
            this.relOptSchema = relOptSchema;
        }

        public RelOptSchema getRelOptSchema()
        {
            return relOptSchema;
        }

        public Object contentsAsArray(
            String qualifier,
            String tableName)
        {
            return null;
        }
    }

    /**
     * Default implementation of {@link Tester}, using mock classes {@link
     * MockRelOptSchema}, {@link MockRelOptConnection} and {@link
     * MockRelOptPlanner}.
     */
    public static class TesterImpl
        implements Tester
    {
        private RelOptPlanner planner;
        private SqlOperatorTable opTab;
        private final DiffRepository diffRepos;
        private RelDataTypeFactory typeFactory;

        /**
         * Creates a TesterImpl.
         *
         * @param diffRepos Diff repository
         */
        protected TesterImpl(DiffRepository diffRepos)
        {
            this.diffRepos = diffRepos;
        }

        public RelNode convertSqlToRel(String sql)
        {
            Util.pre(sql != null, "sql != null");
            final SqlNode sqlQuery;
            try {
                sqlQuery = parseQuery(sql);
            } catch (Exception e) {
                throw Util.newInternal(e); // todo: better handling
            }
            final RelDataTypeFactory typeFactory = getTypeFactory();
            final SqlValidatorCatalogReader catalogReader =
                createCatalogReader(typeFactory);
            final SqlValidator validator =
                createValidator(
                    catalogReader,
                    typeFactory);
            final RelOptSchema relOptSchema =
                createRelOptSchema(catalogReader, typeFactory);
            final RelOptConnection relOptConnection =
                new MockRelOptConnection(relOptSchema);
            final SqlToRelConverter converter =
                createSqlToRelConverter(
                    validator,
                    relOptSchema,
                    relOptConnection,
                    typeFactory);
            converter.setTrimUnusedFields(true);
            final SqlNode validatedQuery = validator.validate(sqlQuery);
            final RelNode rel =
                converter.convertQuery(
                    validatedQuery, false, SqlToRelConverter.QueryContext.TOP);
            Util.post(rel != null, "return != null");
            return rel;
        }

        public MockRelOptSchema createRelOptSchema(
            final SqlValidatorCatalogReader catalogReader,
            final RelDataTypeFactory typeFactory)
        {
            return new MockRelOptSchema(
                catalogReader,
                typeFactory,
                typeFactory.getSystemFieldList());
        }

        protected SqlToRelConverter createSqlToRelConverter(
            final SqlValidator validator,
            final RelOptSchema relOptSchema,
            final RelOptConnection relOptConnection,
            final RelDataTypeFactory typeFactory)
        {
            final SqlToRelConverter converter =
                new SqlToRelConverter(
                    validator,
                    relOptSchema,
                    OJSystem.env,
                    getPlanner(),
                    relOptConnection,
                    new JavaRexBuilder(typeFactory));
            return converter;
        }

        protected final RelDataTypeFactory getTypeFactory()
        {
            if (typeFactory == null) {
                typeFactory = createTypeFactory();
            }
            return typeFactory;
        }

        protected RelDataTypeFactory createTypeFactory()
        {
            return new SqlTypeFactoryImpl();
        }

        protected final RelOptPlanner getPlanner()
        {
            if (planner == null) {
                planner = createPlanner();
            }
            return planner;
        }

        public SqlNode parseQuery(String sql)
            throws Exception
        {
            SqlParser parser = new SqlParser(sql);
            SqlNode sqlNode = parser.parseQuery();
            return sqlNode;
        }

        public SqlConformance getConformance()
        {
            return SqlConformance.Default;
        }

        public SqlValidator createValidator(
            SqlValidatorCatalogReader catalogReader,
            RelDataTypeFactory typeFactory)
        {
            return new FarragoTestValidator(
                getOperatorTable(),
                new MockCatalogReader(typeFactory),
                typeFactory,
                getConformance());
        }

        public final SqlOperatorTable getOperatorTable()
        {
            if (opTab == null) {
                opTab = createOperatorTable();
            }
            return opTab;
        }

        /**
         * Creates an operator table.
         *
         * @return New operator table
         */
        protected SqlOperatorTable createOperatorTable()
        {
            final MockSqlOperatorTable opTab =
                new MockSqlOperatorTable(SqlStdOperatorTable.instance());
            MockSqlOperatorTable.addRamp(opTab);
            return opTab;
        }

        public SqlValidatorCatalogReader createCatalogReader(
            RelDataTypeFactory typeFactory)
        {
            return new MockCatalogReader(typeFactory);
        }

        public RelOptPlanner createPlanner()
        {
            return new MockRelOptPlanner();
        }

        public void assertConvertsTo(
            String sql,
            String plan)
        {
            assertConvertsTo(sql, plan, false);
        }

        public void assertConvertsTo(
            String sql,
            String plan,
            boolean trim)
        {
            String sql2 = getDiffRepos().expand("sql", sql);
            RelNode rel = convertSqlToRel(sql2);

            assertTrue(rel != null);
            assertValid(rel);

            if (trim) {
                final RelFieldTrimmer trimmer = createFieldTrimmer();
                rel = trimmer.trim(rel);
                assertTrue(rel != null);
                assertValid(rel);
            }

            // NOTE jvs 28-Mar-2006:  insert leading newline so
            // that plans come out nicely stacked instead of first
            // line immediately after CDATA start
            String actual = NL + RelOptUtil.toString(rel);
            diffRepos.assertEquals("plan", plan, actual);
        }

        /**
         * Creates a RelFieldTrimmer.
         *
         * @return Field trimmer
         */
        public RelFieldTrimmer createFieldTrimmer()
        {
            return new RelFieldTrimmer(getValidator());
        }

        /**
         * Checks that every node of a relational expression is valid.
         *
         * @param rel Relational expression
         */
        protected void assertValid(RelNode rel)
        {
            SqlToRelConverterTest.RelValidityChecker checker =
                new SqlToRelConverterTest.RelValidityChecker();
            checker.go(rel);
            assertEquals(0, checker.invalidCount);
        }

        public DiffRepository getDiffRepos()
        {
            return diffRepos;
        }

        public SqlValidator getValidator()
        {
            final RelDataTypeFactory typeFactory = getTypeFactory();
            final SqlValidatorCatalogReader catalogReader =
                createCatalogReader(typeFactory);
            return
                createValidator(
                    catalogReader,
                    typeFactory);
        }
    }

    private static class FarragoTestValidator
        extends SqlValidatorImpl
    {
        public FarragoTestValidator(
            SqlOperatorTable opTab,
            SqlValidatorCatalogReader catalogReader,
            RelDataTypeFactory typeFactory,
            SqlConformance conformance)
        {
            super(opTab, catalogReader, typeFactory, conformance);
        }

        // override SqlValidator
        public boolean shouldExpandIdentifiers()
        {
            return true;
        }
    }
}

// End SqlToRelTestBase.java
