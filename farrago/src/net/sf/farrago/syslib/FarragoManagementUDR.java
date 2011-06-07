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
package net.sf.farrago.syslib;

import java.io.*;

import java.sql.*;

import java.util.*;
import java.util.regex.*;

import javax.jmi.reflect.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.cwm.core.*;
import net.sf.farrago.cwm.relational.*;
import net.sf.farrago.db.*;
import net.sf.farrago.defimpl.*;
import net.sf.farrago.fem.sql2003.*;
import net.sf.farrago.resource.*;
import net.sf.farrago.runtime.*;
import net.sf.farrago.session.*;
import net.sf.farrago.util.*;

import org.eigenbase.jmi.*;
import org.eigenbase.util.*;
import org.eigenbase.util14.*;


/**
 * FarragoManagementUDR is a set of user-defined routines providing access to
 * information about the running state of Farrago, intended for management
 * purposes - such as a list of currently executing statements. The UDRs are
 * used to create views in initsql/createMgmtViews.sql.
 *
 * @author Jason Ouellette
 * @version $Id$
 */
public abstract class FarragoManagementUDR
{
    //~ Static fields/initializers ---------------------------------------------

    static final String STORAGEFACTORY_PROP_NAME =
        "org.netbeans.mdr.storagemodel.StorageFactoryClassName";
    static final String [] STORAGE_PROP_NAMES =
        new String[] {
            "MDRStorageProperty.org.netbeans.mdr.persistence.jdbcimpl.driverClassName",
            "MDRStorageProperty.org.netbeans.mdr.persistence.jdbcimpl.url",
            "MDRStorageProperty.org.netbeans.mdr.persistence.jdbcimpl.userName",
            "MDRStorageProperty.org.netbeans.mdr.persistence.jdbcimpl.password",
            "MDRStorageProperty.org.netbeans.mdr.persistence.jdbcimpl.schemaName"
        };

    // Category, subcategory, and units are not passed in for
    // performance counters, therefore we use this list from the
    // documentation on Eigenpedia as a temporary solution.
    // Structure: {counterName: [category, subcategory, units]}
    private final static Map<String, String[]> perf_counter_info =
        new HashMap<String, String[]>();

    static {
        perf_counter_info.put(
            "JvmMemoryUnused", new String[]{"JVM Memory", null, "bytes"});
        perf_counter_info.put(
            "JvmMemoryAllocationLimit",
            new String[]{"JVM Memory", null, "bytes"});
        perf_counter_info.put(
            "JvmMemoryAllocated",
            new String[]{"JVM Memory", null, "bytes"});
        perf_counter_info.put(
            "ExpectedConcurrentStatements",
            new String[]{"Parameter Settings", null, null});
        perf_counter_info.put(
            "CacheReservePercentage",
            new String[]{"Parameter Settings", null, null});
        perf_counter_info.put(
            "CachePagesAllocated",
            new String[]{"Parameter Settings", null, "pages"});
        perf_counter_info.put(
            "CachePagesGoverned",
            new String[]{"Parameter Settings", null, "pages"});
        perf_counter_info.put(
            "CachePagesAllocationLimit",
            new String[]{"Parameter Settings", null, "pages"});
        perf_counter_info.put(
            "DatabaseCheckpoints", new String[]{"Execution", null, null});
        perf_counter_info.put(
            "DatabaseCheckpointsSinceInit",
            new String[]{"Execution", null, null});
        perf_counter_info.put(
            "DatabasePagesAllocated",
            new String[]{"Storage", null, "pages"});
        perf_counter_info.put(
            "DatabasePagesOccupiedHighWaterSinceInit",
            new String[]{"Storage", null, "pages"});
        perf_counter_info.put(
            "DatabasePagesExtendedSinceInit",
            new String[]{"Storage", null, "pages"});
        perf_counter_info.put(
            "TempPagesAllocated", new String[]{"Storage", null, "pages"});
        perf_counter_info.put(
            "TempPagesOccupiedHighWaterSinceInit",
            new String[]{"Storage", null, "pages"});
        perf_counter_info.put(
            "TempPagesExtendedSinceInit",
            new String[]{"Storage", null, "pages"});
        perf_counter_info.put(
            "CachePagesReserved",
            new String[]{"Buffer Pool Statistics", "General Usage", "pages"});
        perf_counter_info.put(
            "CachePagesUnused",
            new String[]{"Buffer Pool Statistics", "General Usage", "pages"});
        perf_counter_info.put(
            "CacheDirtyPages",
            new String[]{"Buffer Pool Statistics", "General Usage", "pages"});
        perf_counter_info.put(
            "CacheRequests",
            new String[]{"Buffer Pool Statistics", "General Usage", null});
        perf_counter_info.put(
            "CacheRequestsSinceInit",
            new String[]{"Buffer Pool Statistics", "General Usage", null});
        perf_counter_info.put(
            "CacheHits",
            new String[]{"Buffer Pool Statistics", "General Usage", null});
        perf_counter_info.put(
            "CacheHitsSinceInit",
            new String[]{"Buffer Pool Statistics", "General Usage", null});
        perf_counter_info.put(
            "CacheVictimizations",
            new String[]{"Buffer Pool Statistics", "General Usage", null});
        perf_counter_info.put(
            "CacheVictimizationsSinceInit",
            new String[]{"Buffer Pool Statistics", "General Usage", null});
        perf_counter_info.put(
            "CachePageIoRetries",
            new String[]{"Buffer Pool Statistics", "General Usage", "pages"});
        perf_counter_info.put(
            "CachePageIoRetriesSinceInit",
            new String[]{"Buffer Pool Statistics", "General Usage", "pages"});
        perf_counter_info.put(
            "CachePagesRead",
            new String[]{"Buffer Pool Statistics", "Read-Specific", "pages"});
        perf_counter_info.put(
            "CachePagesReadSinceInit",
            new String[]{"Buffer Pool Statistics", "Read-Specific", "pages"});
        perf_counter_info.put(
            "CachePagesPrefetched",
            new String[]{"Buffer Pool Statistics", "Read-Specific", "pages"});
        perf_counter_info.put(
            "CachePagesPrefetchedSinceInit",
            new String[]{"Buffer Pool Statistics", "Read-Specific", "pages"});
        perf_counter_info.put(
            "CachePagePrefetchesRejected",
            new String[]{"Buffer Pool Statistics", "Read-Specific", "pages"});
        perf_counter_info.put(
            "CachePagePrefetchesRejectedSinceInit",
            new String[]{"Buffer Pool Statistics", "Read-Specific", "pages"});
        perf_counter_info.put(
            "CachePagesWritten",
            new String[]{"Buffer Pool Statistics", "Write-Specific", "pages"});
        perf_counter_info.put(
            "CachePagesWrittenSinceInit",
            new String[]{"Buffer Pool Statistics", "Write-Specific", "pages"});
        perf_counter_info.put(
            "CacheVictimizationWrites",
            new String[]{"Buffer Pool Statistics", "Write-Specific", "pages"});
        perf_counter_info.put(
            "CacheVictimizationWritesSinceInit",
            new String[]{"Buffer Pool Statistics", "Write-Specific", "pages"});
        perf_counter_info.put(
            "CacheLazyWriteCalls",
            new String[]{"Buffer Pool Statistics", "Write-Specific", null});
        perf_counter_info.put(
            "CacheLazyWriteCallsSinceInit",
            new String[]{"Buffer Pool Statistics", "Write-Specific", null});
        perf_counter_info.put(
            "CacheLazyWrites",
            new String[]{"Buffer Pool Statistics", "Write-Specific", "pages"});
        perf_counter_info.put(
            "CacheLazyWritesSinceInit",
            new String[]{"Buffer Pool Statistics", "Write-Specific", "pages"});
        perf_counter_info.put(
            "CacheCheckpointWrites",
            new String[]{"Buffer Pool Statistics", "Write-Specific", "pages"});
        perf_counter_info.put(
            "CacheCheckpointWritesSinceInit",
            new String[]{"Buffer Pool Statistics", "Write-Specific", "pages"});
        perf_counter_info.put(
            "JvmNanoTime", new String[]{"Miscellaneous", null, "ns"});
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Populates a table of information on currently executing statements.
     */
    public static void statements(PreparedStatement resultInserter)
        throws SQLException
    {
        FarragoSession callerSession = FarragoUdrRuntime.getSession();
        FarragoDatabase db = ((FarragoDbSession) callerSession).getDatabase();
        List<FarragoSession> sessions = db.getSessions();
        for (FarragoSession s : sessions) {
            FarragoSessionInfo info = s.getSessionInfo();
            List<Long> ids = info.getExecutingStmtIds();
            for (long id : ids) {
                FarragoSessionExecutingStmtInfo stmtInfo =
                    info.getExecutingStmtInfo(id);
                if (stmtInfo != null) {
                    int i = 0;
                    resultInserter.setLong(++i, id);
                    resultInserter.setLong(
                        ++i,
                        info.getId());
                    resultInserter.setString(
                        ++i,
                        stmtInfo.getSql());
                    resultInserter.setTimestamp(
                        ++i,
                        new Timestamp(stmtInfo.getStartTime()));
                    resultInserter.setString(
                        ++i,
                        Arrays.asList(stmtInfo.getParameters()).toString());
                    resultInserter.executeUpdate();
                }
            }
        }
    }

    /**
     * Populates a table of catalog objects in use by active statements.
     *
     * @param resultInserter
     *
     * @throws SQLException
     */
    public static void objectsInUse(PreparedStatement resultInserter)
        throws SQLException
    {
        FarragoSession callerSession = FarragoUdrRuntime.getSession();
        FarragoDatabase db = ((FarragoDbSession) callerSession).getDatabase();
        List<FarragoSession> sessions = db.getSessions();
        for (FarragoSession s : sessions) {
            FarragoSessionInfo info = s.getSessionInfo();
            List<Long> ids = info.getExecutingStmtIds();
            for (long id : ids) {
                FarragoSessionExecutingStmtInfo stmtInfo =
                    info.getExecutingStmtInfo(id);
                if (stmtInfo != null) {
                    List<String> mofIds = stmtInfo.getObjectsInUse();
                    for (String mofId : mofIds) {
                        int i = 0;
                        resultInserter.setLong(
                            ++i,
                            info.getId());
                        resultInserter.setLong(++i, id);
                        resultInserter.setString(++i, mofId);
                        resultInserter.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Populates a table of currently active sessions.
     *
     * @param resultInserter
     *
     * @throws SQLException
     */
    public static void sessions(PreparedStatement resultInserter)
        throws SQLException
    {
        FarragoSession callerSession = FarragoUdrRuntime.getSession();
        FarragoDatabase db = ((FarragoDbSession) callerSession).getDatabase();
        List<FarragoSession> sessions = db.getSessions();
        for (FarragoSession s : sessions) {
            int i = 0;
            FarragoSessionVariables v = s.getSessionVariables();
            FarragoSessionInfo info = s.getSessionInfo();
            resultInserter.setLong(
                ++i,
                info.getId());
            resultInserter.setString(
                ++i,
                s.getUrl());
            resultInserter.setString(++i, v.currentUserName);
            resultInserter.setString(++i, v.currentRoleName);
            resultInserter.setString(++i, v.sessionUserName);
            resultInserter.setString(++i, v.systemUserName);
            resultInserter.setString(++i, v.systemUserFullName);
            resultInserter.setString(++i, v.sessionName);
            resultInserter.setString(++i, v.programName);
            resultInserter.setLong(++i, v.processId);
            resultInserter.setString(++i, v.catalogName);
            resultInserter.setString(++i, v.schemaName);
            resultInserter.setBoolean(
                ++i,
                s.isClosed());
            resultInserter.setBoolean(
                ++i,
                s.isAutoCommit());
            resultInserter.setBoolean(
                ++i,
                s.isTxnInProgress());
            resultInserter.setString(
                ++i,
                v.get(FarragoDefaultSessionPersonality.LABEL));
            resultInserter.executeUpdate();
        }
    }

    /**
     * Populates a list of session parameters
     */
    public static void sessionParameters(PreparedStatement resultInserter)
        throws SQLException
    {
        FarragoSessionVariables variables =
            FarragoUdrRuntime.getSession().getSessionVariables();
        Map<String, String> readMap = variables.getMap();
        for (String paramName : readMap.keySet()) {
            int i = 0;
            resultInserter.setString(++i, paramName);
            resultInserter.setString(++i, readMap.get(paramName));
            resultInserter.executeUpdate();
        }
    }

    /**
     * Sleeps for a given number of milliseconds (checking for query
     * cancellation every second).
     *
     * @param millis number of milliseconds to sleep
     *
     * @return 0 (instead of void, so that this can be used as a UDF)
     */
    public static int sleep(long millis)
    {
        try {
            while (millis != 0) {
                long delta = Math.min(1000, millis);
                Thread.sleep(delta);
                millis -= delta;
                FarragoUdrRuntime.checkCancel();
            }
        } catch (InterruptedException ex) {
            // should not happen
            throw Util.newInternal(ex);
        }
        return 0;
    }

    /**
     * Discards all entries from the global code cache.
     */
    public static void flushCodeCache()
        throws SQLException
    {
        Connection conn =
            DriverManager.getConnection(
                "jdbc:default:connection");
        Statement stmt = conn.createStatement();

        // First, retrieve current setting.
        ResultSet rs =
            stmt.executeQuery(
                "select \"codeCacheMaxBytes\" from "
                + "sys_fem.\"Config\".\"FarragoConfig\"");
        rs.next();
        long savedSetting = rs.getLong(1);
        rs.close();

        // Discard
        stmt.executeUpdate(
            "alter system set \"codeCacheMaxBytes\" = min");

        // Restore saved setting
        stmt.executeUpdate(
            "alter system set \"codeCacheMaxBytes\" = "
            + ((savedSetting == -1) ? "max" : Long.toString(savedSetting)));
    }

    /**
     * Exports the catalog repository contents as an XMI file.
     *
     * @param xmiFile name of file to create
     */
    public static void exportCatalog(String xmiFile)
        throws Exception
    {
        xmiFile = FarragoProperties.instance().expandProperties(xmiFile);
        File file = new File(xmiFile);
        FarragoModelLoader modelLoader = getModelLoader();
        FarragoReposUtil.exportExtent(
            modelLoader.getMdrRepos(),
            file,
            "FarragoCatalog");
    }

    public static FarragoModelLoader getModelLoader()
    {
        FarragoSession callerSession = FarragoUdrRuntime.getSession();
        FarragoDatabase db = ((FarragoDbSession) callerSession).getDatabase();
        return db.getSystemRepos().getModelLoader();
    }

    /**
     * Retrieves a list of repository integrity violations. The result has two
     * string columns; the first is the error description, the second is the
     * MOFID of the object on which the error was detected, or null if unknown.
     */
    public static void repositoryIntegrityViolations(
        PreparedStatement resultInserter)
        throws SQLException
    {
        FarragoSession session = FarragoUdrRuntime.getSession();
        FarragoRepos repos = session.getRepos();
        FarragoReposTxnContext reposTxnContext =
            new FarragoReposTxnContext(repos, true);
        reposTxnContext.beginReadTxn();

        try {
            List<FarragoReposIntegrityErr> errs = repos.verifyIntegrity(null);
            for (FarragoReposIntegrityErr err : errs) {
                resultInserter.setString(1, err.getDescription());
                if (err.getRefObject() != null) {
                    resultInserter.setString(2, err.getRefObject().refMofId());
                } else {
                    resultInserter.setString(2, null);
                }
                resultInserter.executeUpdate();
            }
        } finally {
            reposTxnContext.commit();
        }
    }

    /**
     * Populates a table of properties of the current repository connection.
     *
     * @param resultInserter
     *
     * @throws SQLException
     */
    public static void repositoryProperties(PreparedStatement resultInserter)
        throws SQLException
    {
        FarragoModelLoader loader = getModelLoader();
        if (loader != null) {
            Properties props = loader.getStorageProperties();
            int i = 0;
            resultInserter.setString(++i, STORAGEFACTORY_PROP_NAME);
            resultInserter.setString(
                ++i,
                loader.getStorageFactoryClassName());
            resultInserter.executeUpdate();

            for (String propName : STORAGE_PROP_NAMES) {
                i = 0;
                resultInserter.setString(++i, propName);
                resultInserter.setString(
                    ++i,
                    props.getProperty(propName));
                resultInserter.executeUpdate();
            }
        }
    }

    /**
     * Populates a table of all threads running in the JVM.
     *
     * @param resultInserter
     *
     * @throws SQLException
     */
    public static void threadList(PreparedStatement resultInserter)
        throws Exception
    {
        // TODO jvs 17-Sept-2006:  Inside of Fennel, require all threads
        // to register with the JVM so that we can get a complete
        // picture here.

        Map<Thread, StackTraceElement[]> stackTraces =
            Thread.getAllStackTraces();

        for (
            Map.Entry<Thread, StackTraceElement[]> entry
            : stackTraces.entrySet())
        {
            Thread thread = entry.getKey();

            int i = 0;

            resultInserter.setLong(++i, thread.getId());
            final ThreadGroup threadGroup = thread.getThreadGroup();
            resultInserter.setString(
                ++i,
                (threadGroup == null) ? "null" : threadGroup.getName());
            resultInserter.setString(++i, thread.getName());
            resultInserter.setInt(++i, thread.getPriority());
            resultInserter.setString(++i, thread.getState().toString());
            resultInserter.setBoolean(++i, thread.isAlive());
            resultInserter.setBoolean(++i, thread.isDaemon());
            resultInserter.setBoolean(++i, thread.isInterrupted());
            resultInserter.executeUpdate();
        }
    }

    /**
     * Populates a table of stack entries for all threads running in the JVM.
     *
     * @param resultInserter
     *
     * @throws SQLException
     */
    public static void threadStackEntries(PreparedStatement resultInserter)
        throws Exception
    {
        Map<Thread, StackTraceElement[]> stackTraces =
            Thread.getAllStackTraces();

        for (
            Map.Entry<Thread, StackTraceElement[]> entry
            : stackTraces.entrySet())
        {
            Thread thread = entry.getKey();
            StackTraceElement [] stackArray = entry.getValue();

            int j = 0;

            for (StackTraceElement element : stackArray) {
                int i = 0;

                resultInserter.setLong(++i, thread.getId());
                resultInserter.setLong(++i, j++);
                resultInserter.setString(++i, element.toString());
                resultInserter.setString(++i, element.getClassName());
                resultInserter.setString(++i, element.getMethodName());
                resultInserter.setString(++i, element.getFileName());
                resultInserter.setInt(++i, element.getLineNumber());
                resultInserter.setBoolean(++i, element.isNativeMethod());
                resultInserter.executeUpdate();
            }
        }
    }


    /**
     * Populates a table of performance counters.
     *
     * @param resultInserter
     */
    public static void performanceCounters(PreparedStatement resultInserter)
        throws Exception
    {
        String JVM_SRC = "JVM";
        Runtime runtime = Runtime.getRuntime();

        // Read values from System and Runtime
        addSysInfo(
            resultInserter,
            perf_counter_info.get("JvmMemoryUnused")[0],
            perf_counter_info.get("JvmMemoryUnused")[1],
            JVM_SRC,
            "JvmMemoryUnused",
            Long.toString(runtime.freeMemory()),
            "bytes");
        addSysInfo(
            resultInserter,
            perf_counter_info.get("JvmMemoryAllocationLimit")[0],
            perf_counter_info.get("JvmMemoryAllocationLimit")[1],
            JVM_SRC,
            "JvmMemoryAllocationLimit",
            Long.toString(runtime.maxMemory()),
            "bytes");
        addSysInfo(
            resultInserter,
            perf_counter_info.get("JvmMemoryAllocated")[0],
            perf_counter_info.get("JvmMemoryAllocated")[1],
            JVM_SRC,
            "JvmMemoryAllocated",
            Long.toString(runtime.totalMemory()),
            "bytes");
        addSysInfo(
            resultInserter,
            perf_counter_info.get("JvmNanoTime")[0],
            perf_counter_info.get("JvmNanoTime")[1],
            JVM_SRC,
            "JvmNanoTime",
            Long.toString(System.nanoTime()),
            "ns");

        // Read values from Fennel
        Map<String, String> perfCounters =
            NativeTrace.instance().getPerfCounters();
        for (Map.Entry<String, String> entry : perfCounters.entrySet()) {
            String[] info = perf_counter_info.get(entry.getKey());
            if (info == null) {
                info = new String[]{null, null, null};
            }
            addSysInfo(
                resultInserter,
                info[0],
                info[1],
                "Fennel",
                entry.getKey(),
                entry.getValue(),
                info[2]);
        }
    }

    /**
     * Populates a table of global information about the running system.
     *
     * @param resultInserter
     */
    public static void systemInfo(PreparedStatement resultInserter)
        throws Exception
    {
        String JVM_SRC = "java.lang.System";
        Runtime runtime = Runtime.getRuntime();

        // Read values from System and Runtime
        addSysInfo(
            resultInserter,
            null,
            null,
            JVM_SRC,
            "currentTimeMillis",
            Long.toString(System.currentTimeMillis()),
            "ns");
        addSysInfo(
            resultInserter,
            null,
            null,
            JVM_SRC,
            "availableProcessors",
            Integer.toString(runtime.availableProcessors()),
            "cpus");

        // Read environment variables
        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            addSysInfo(
                resultInserter,
                null,
                null,
                "System.getenv",
                entry.getKey(),
                entry.getValue(),
                null);
        }

        // Read system properties
        for (
            Map.Entry<Object, Object> entry
            : System.getProperties().entrySet())
        {
            addSysInfo(
                resultInserter,
                null,
                null,
                "System.getProperties",
                entry.getKey().toString(),
                entry.getValue().toString(),
                null);
        }

        // If we're running on Linux, we can try to get a lotta info
        // from the /proc virtual filesystem; if not, just omit it
        FileReader fileReader = null;
        try {
            String src = "/proc/meminfo";
            fileReader = new FileReader(src);
            readLinuxMeminfo(resultInserter, fileReader, src);
            src = "/proc/cpuinfo";
            fileReader.close();
            fileReader = new FileReader(src);
            readLinuxCpuinfo(resultInserter, fileReader, src);
        } catch (Throwable ex) {
            // ignore in case we're not running on Linux or don't have access
        } finally {
            Util.squelchReader(fileReader);
        }
    }

    private static void addSysInfo(
        PreparedStatement resultInserter,
        String category,
        String subcategory,
        String source,
        String property,
        String value,
        String units)
        throws Exception
    {
        int i = 0;
        resultInserter.setString(++i, category);
        resultInserter.setString(++i, subcategory);
        resultInserter.setString(++i, source);
        resultInserter.setString(++i, property);
        resultInserter.setString(++i, units);
        resultInserter.setString(++i, value);
        resultInserter.executeUpdate();
    }

    private static void readLinuxMeminfo(
        PreparedStatement resultInserter,
        FileReader fileReader,
        String src)
        throws Exception
    {
        LineNumberReader lineReader = new LineNumberReader(fileReader);
        for (;;) {
            String line = lineReader.readLine();
            if (line == null) {
                break;
            }
            StringTokenizer st = new StringTokenizer(line, ": ");
            String itemName = st.nextToken();
            String itemValue = st.nextToken();
            String itemUnits = st.nextToken();
            addSysInfo(
                resultInserter, null, null, src, itemName, itemValue,
                itemUnits);
        }
    }

    private static void readLinuxCpuinfo(
        PreparedStatement resultInserter,
        FileReader fileReader,
        String src)
        throws Exception
    {
        LineNumberReader lineReader = new LineNumberReader(fileReader);
        for (;;) {
            String line = lineReader.readLine();
            if (line == null) {
                break;
            }
            StringTokenizer st = new StringTokenizer(line, ":\t");
            String itemName = st.nextToken().trim();
            String itemValue = st.nextToken().trim();
            String itemUnits = null;
            addSysInfo(
                resultInserter, null, null, src, itemName, itemValue,
                itemUnits);
        }
    }

    /**
     * Retrieves a long catalog string attribute in chunks.
     *
     * @param mofId MOFID of a repository object
     * @param attributeName name of attribute to retrieve
     * @param resultInserter
     */
    public static void lobText(
        String mofId,
        String attributeName,
        PreparedStatement resultInserter)
        throws Exception
    {
        FarragoSession session = FarragoUdrRuntime.getSession();
        FarragoRepos repos = session.getRepos();
        FarragoReposTxnContext reposTxnContext =
            new FarragoReposTxnContext(repos, true);
        reposTxnContext.beginReadTxn();
        String text;
        try {
            RefObject refObj =
                (RefObject) repos.getMdrRepos().getByMofId(mofId);
            if (refObj == null) {
                throw FarragoResource.instance().ValidatorUnknownObject.ex(
                    "MOFID " + mofId);
            }

            Object expr = refObj.refGetValue(attributeName);

            if (expr == null) {
                text = null;
            } else if (expr instanceof CwmExpression) {
                text = ((CwmExpression) expr).getBody();
            } else {
                text = expr.toString();
            }
        } finally {
            reposTxnContext.commit();
        }

        // special case for null
        if (text == null) {
            // emit a single null value
            resultInserter.setInt(1, -1);
            resultInserter.setString(2, null);
            resultInserter.executeUpdate();
            return;
        }

        // special case for empty string
        int textLength = text.length();
        if (textLength == 0) {
            resultInserter.setInt(1, 0);
            resultInserter.setString(2, "");
            resultInserter.executeUpdate();
            return;
        }

        // break up text into chunks of maximum size 1024 characters
        int begin = 0;
        do {
            int end = begin + 1024;
            if (end > textLength) {
                end = textLength;
            }
            String chunk = text.substring(begin, end);
            resultInserter.setInt(1, begin);
            resultInserter.setString(2, chunk);
            resultInserter.executeUpdate();
            begin = end;
        } while (begin < textLength);
    }

    /**
     * Implements a UDX for filtering out metadata descriptor rows
     * for objects to which the current user does not have at least
     * one privilege granted (either directly or indirectly via some
     * role applicable to that user).  This method can deal
     * with variably-typed objects.
     *
     * @param inputSet input rows
     *
     * @param idColumnNames array containing two string elements (name
     * of input column with MOFID, name of input column with
     * MOF class name)
     *
     * @param resultInserter
     */
    public static void filterUserVisibleObjects(
        ResultSet inputSet, List<String> idColumnNames,
        PreparedStatement resultInserter)
        throws SQLException
    {
        FarragoVisibilityFilter filter = new FarragoVisibilityFilter(
            inputSet, null, idColumnNames, resultInserter);
        filter.execute();
    }

    /**
     * Implements a UDX for filtering out metadata descriptor rows
     * for objects to which the current user does not have at least
     * one privilege granted (either directly or indirectly via some
     * role applicable to that user).  This method requires
     * all objects to have the same concrete type.
     *
     * @param inputSet input rows
     *
     * @param className MOF class name of concrete type
     *
     * @param idColumnNames array containing one string element (name
     * of input column with MOFID)
     *
     * @param resultInserter
     */
    public static void filterUserVisibleObjectsTyped(
        ResultSet inputSet,
        String className,
        List<String> idColumnNames,
        PreparedStatement resultInserter)
        throws SQLException
    {
        FarragoVisibilityFilter filter = new FarragoVisibilityFilter(
            inputSet, className, idColumnNames, resultInserter);
        filter.execute();
    }

    /**
     * Sets a filter on the optimizer rules to be used in the current session.
     *
     * @param regex regular expression for rule names to be excluded
     */
    public static void setOptRuleDescExclusionFilter(String regex)
    {
        FarragoSession sess = FarragoUdrRuntime.getSession();
        if (regex == null) {
            sess.setOptRuleDescExclusionFilter(null);
        } else {
            sess.setOptRuleDescExclusionFilter(Pattern.compile(regex));
        }
    }

    /**
     * Creates a directory, including any parent directories.
     *
     * @param path directory path to be created
     */
    public static void createDirectory(String path)
        throws Exception
    {
        new File(path).mkdirs();
    }

    /**
     * Deletes a file or directory. Attempts to delete a non-empty directory
     * will fail.
     */
    public static void deleteFileOrDirectory(String path)
        throws Exception
    {
        if (!(new File(path).delete())) {
            throw FarragoResource.instance().FileDeletionFailed.ex(path);
        }
    }

    /**
     * Backs up the database, but without checking that there's enough space to
     * perform the backup.
     *
     * @param archiveDirectory the pathname of the directory where the backup
     * files will be created
     * @param backupType string value indicating whether the backup is a FULL,
     * INCREMENTAL, or DIFFERENTIAL backup
     * @param compressionMode string value indicating whether the backup is
     * COMPRESSED or UNCOMPRESSED
     */
    public static void backupDatabaseWithoutSpaceCheck(
        String archiveDirectory,
        String backupType,
        String compressionMode)
        throws Exception
    {
        FarragoSystemBackup backup =
            new FarragoSystemBackup(
                archiveDirectory,
                backupType,
                compressionMode,
                false,
                0);
        backup.backupDatabase();
    }

    /**
     * Backs up the database, checking that there's enough space to perform the
     * backup.
     *
     * @param archiveDirectory the pathname of the directory where the backup
     * files will be created
     * @param backupType string value indicating whether the backup is a FULL,
     * INCREMENTAL, or DIFFERENTIAL backup
     * @param compressionMode string value indicating whether the backup is
     * COMPRESSED or UNCOMPRESSED
     * @param padding number of bytes of additional space required on top of
     * what's estimated based on the number of data pages allocated
     */
    public static void backupDatabaseWithSpaceCheck(
        String archiveDirectory,
        String backupType,
        String compressionMode,
        long padding)
        throws Exception
    {
        FarragoSystemBackup backup =
            new FarragoSystemBackup(
                archiveDirectory,
                backupType,
                compressionMode,
                true,
                padding);
        backup.backupDatabase();
    }

    /**
     * Restores a database from backup.
     *
     * @param archiveDirectory the directory containing the backup
     */
    public static void restoreDatabase(String archiveDirectory)
        throws Exception
    {
        FarragoSystemRestore restore =
            new FarragoSystemRestore(archiveDirectory);
        restore.restoreDatabase(true);
    }

    /**
     * Restores a database from backup, excluding the catalog data.
     *
     * @param archiveDirectory the directory containing the backup
     */
    public static void restoreDatabaseWithoutCatalog(String archiveDirectory)
        throws Exception
    {
        FarragoSystemRestore restore =
            new FarragoSystemRestore(archiveDirectory);
        restore.restoreDatabase(false);
    }

    /**
     * Sets Unicode as the default character set.
     */
    public static void setUnicodeAsDefault()
        throws Exception
    {
        FarragoSession session = FarragoUdrRuntime.getSession();
        FarragoRepos repos = session.getRepos();
        FarragoReposTxnContext reposTxnContext =
            new FarragoReposTxnContext(repos, true);
        reposTxnContext.beginWriteTxn();
        try {
            Collection c =
                repos.getMedPackage().getFemLocalTable().refAllOfType();
            if (!c.isEmpty()) {
                throw FarragoResource.instance().ChangeToUnicodeFailed.ex();
            }
            String characterSetName = ConversionUtil.NATIVE_UTF16_CHARSET_NAME;
            String collationName =
                ConversionUtil.NATIVE_UTF16_CHARSET_NAME + "$en_US";
            c = repos.getRelationalPackage().getCwmCatalog().refAllOfType();
            for (Object obj : c) {
                CwmCatalog catalog = (CwmCatalog) obj;
                catalog.setDefaultCharacterSetName(characterSetName);
                catalog.setDefaultCollationName(collationName);
            }
            c = repos.getRelationalPackage().getCwmColumn().refAllOfType();
            for (Object obj : c) {
                FemAbstractTypedElement column = (FemAbstractTypedElement) obj;
                setElementToUnicode(
                    FarragoCatalogUtil.toFemSqltypedElement(column),
                    characterSetName,
                    collationName);
            }
            c = repos.getSql2003Package().getFemRoutineParameter()
                .refAllOfType();
            for (Object obj : c) {
                FemRoutineParameter param = (FemRoutineParameter) obj;
                setElementToUnicode(
                    FarragoCatalogUtil.toFemSqltypedElement(param),
                    characterSetName,
                    collationName);
            }
            reposTxnContext.commit();
        } finally {
            reposTxnContext.rollback();
        }
    }

    private static void setElementToUnicode(
        FemSqltypedElement element,
        String characterSetName,
        String collationName)
    {
        // TODO jvs 4-Mar-2009:  deal with the nested attributes for
        // UDT's and collection types
        if (!JmiObjUtil.isBlank(element.getCharacterSetName())) {
            element.setCharacterSetName(characterSetName);
            element.setCollationName(collationName);
        }
    }

    /**
     * @return the ID for the current session
     */
    public static long getCurrentSessionId()
    {
        return FarragoUdrRuntime.getSession().getSessionInfo().getId();
    }

    /**
     * Shuts down the DBMS engine (and optionally the JVM).
     */
    public static void shutdownDatabase(
        boolean killSessions, long jvmShutdownDelayInMillis)
        throws Exception
    {
        FarragoDbSession dbSession =
            (FarragoDbSession) FarragoUdrRuntime.getSession();
        if (!killSessions) {
            FarragoDatabase db = dbSession.getDatabase();
            List<FarragoSession> activeSessions = db.getSessions();
            if (activeSessions.size() > 1) {
                throw FarragoResource.instance()
                    .ShutdownPreventedBySessions.ex();
            }
        }
        dbSession.setShutdownRequest(true);
        if (jvmShutdownDelayInMillis != -1) {
            Timer timer = new Timer();
            TimerTask task = new TimerTask()
                {
                    public void run()
                    {
                        System.exit(0);
                    }
                };
            timer.schedule(task, jvmShutdownDelayInMillis);
        }
    }
}

// End FarragoManagementUDR.java
