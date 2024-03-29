/*
// $Id$
// Fennel is a library of data storage and processing components.
// Copyright (C) 2006 The Eigenbase Project
// Copyright (C) 2010 SQLstream, Inc.
// Copyright (C) 2006 Dynamo BI Corporation
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

#include "fennel/common/CommonPreamble.h"
#include "fennel/common/FemEnums.h"
#include "fennel/test/ExecStreamUnitTestBase.h"
#include "fennel/lcs/LcsClusterAppendExecStream.h"
#include "fennel/sorter/ExternalSortExecStream.h"
#include "fennel/lbm/LbmGeneratorExecStream.h"
#include "fennel/lbm/LbmSplicerExecStream.h"
#include "fennel/lbm/LbmSearchExecStream.h"
#include "fennel/ldbtest/LbmExecStreamTestBase.h"
#include "fennel/btree/BTreeBuilder.h"
#include "fennel/ftrs/BTreeInsertExecStream.h"
#include "fennel/ftrs/BTreeSearchExecStream.h"
#include "fennel/ftrs/BTreeExecStream.h"
#include "fennel/tuple/StandardTypeDescriptor.h"
#include "fennel/tuple/TupleDescriptor.h"
#include "fennel/exec/MockProducerExecStream.h"
#include "fennel/exec/ValuesExecStream.h"
#include "fennel/exec/ExecStreamGraph.h"
#include "fennel/exec/ExecStreamEmbryo.h"
#include "fennel/exec/SplitterExecStream.h"
#include "fennel/exec/BarrierExecStream.h"
#include "fennel/cache/Cache.h"
#include "fennel/common/SearchEndpoint.h"
#include <stdarg.h>

#include <boost/test/test_tools.hpp>

using namespace fennel;

/**
 * Testcase for scanning a single bitmap index using equality search on
 * all index keys
 */
class LbmSearchTest : public LbmExecStreamTestBase
{
protected:
    TupleAttributeDescriptor attrDesc_char1;
    TupleAttributeDescriptor attrDesc_nullableInt64;

    /**
     * BTrees corresponding to the clusters
     */
    vector<boost::shared_ptr<BTreeDescriptor> > bTreeClusters;

    /**
     * Saved root pageids of btrees corresponding to clusters; used to
     * append to existing table and to read from them
     */
    vector<PageId> savedBTreeClusterRootIds;

    /**
     * BTrees corresponding to the bitmaps
     */
    vector<boost::shared_ptr<BTreeDescriptor> > bTreeBitmaps;

    /**
     * Saved root pageids of btrees corresponding to bitmaps; used to
     * append to existing table and to read from them
     */
    vector<PageId> savedBTreeBitmapRootIds;

    /**
     * Initializes a BTreeExecStreamParam structure
     */
    void initBTreeExecStreamParam(
        BTreeExecStreamParams &param, shared_ptr<BTreeDescriptor> pBTreeDesc);

    /**
     * Initializes BTreeParams structure
     */
    void initBTreeParam(
        BTreeParams &param, shared_ptr<BTreeDescriptor> pBTreeDesc);

    /**
     * Initializes a cluster scan def structure for a LcsRowScanBase exec
     * stream
     */
    void initClusterScanDef(
        LcsRowScanBaseExecStreamParams &generatorParams,
        struct LcsClusterScanDef &clusterScanDef, uint bTreeIndex);

    /**
     * Initializes BTreeExecStreamParam corresponding to a bitmap index
     *
     * @param tupleDesc tuple descriptor corresponding to bitmap index
     * @param keyProj projection corresponding to the bitmap index keys;
     * excludes start rid
     * @param nKeys number of keys in the bitmap index; excludes start
     * rid from key count
     */
    void  initBTreeBitmapDesc(
        TupleDescriptor &tupleDesc, TupleProjection &keyProj, uint nKeys);

    /**
     * Initializes a tuple descriptor corresponding to a bitmap index
     *
     * @param tupleDesc tuple descriptor corresponding to bitmap index
     * @param nKeys number of keys in the bitmap index; excludes start
     * rid from key count
     */
    void initBTreeTupleDesc(TupleDescriptor &tupleDesc, uint nKeys);

    /**
     * Loads a table with nClusters clusters, 1 column per cluster, and nRows
     * rows.
     *
     *<p>

     * Each column has a repeating sequence of values based on the value in the
     * repeatSeqValues vector.  E.g., a repeating sequence of n will have
     * values:
     *
     * (0, 1, 2, ..., n-1, 0, 1, 2, ..., n-1, 0, 1, 2, ...).
     *
     * Then a single bitmap index is created on all columns.
     *
     * @param nRows number of rows to load
     * @param nClusters number of clusters to create
     * @param repeatSeqValues repeating sequence values for each column
     * @param newRoot if true, append to existing table
     */
    void loadTableAndIndex(
        uint nRows, uint nClusters, std::vector<int> const &repeatSeqValues,
        bool newRoot);

    /**
     * Tests equality scan on the table created in loadTableAndIndex, using
     * the entire index key (minus the rid)
     *
     * @param nRows total number of rows in the table
     * @param nKeys number of keys in the index (excluding startrid)
     * @param repeatSeqValues initial repeating sequence values for each column
     * @param useDynamicKeys if true, pass search keys through dynamic
     * parameters
     * @param includeRid if true, include rid >= 0 in the search
     */
    void testScanFullKey(
        uint nRows, uint nKeys, std::vector<int> const &repeatSeqValues,
        bool useDynamicKeys, bool includeRid);

    /**
     * Tests equality scan on the table created in loadTableAndIndex, using
     * the all keys except the rid and the last key
     *
     * @param nRows total number of rows in the table
     * @param nKeys number of keys in the index (excluding startrid)
     * @param repeatSeqValues initial repeating sequence values for each column
     */
    void testScanPartialKey(
        uint nRows, uint nKeys, std::vector<int> const &repeatSeqValues);

    /**
     * Performs an index search using the key values/directives passed in
     *
     * @param totalKeys total number of keys in the index (excluding startrid)
     *
     * @param nKeys number of keys to use in index search; always excludes
     * startrid
     *
     * @param bufSize size of input buffer containing search keys/directive
     *
     * @param inputBuffer buffer containing search key/directive tuples to
     * be passed into the index scan
     *
     * @param expectedNBitmaps expected number of bitmaps in result
     *
     * @param expectedBitmaps buffer containing expected bitmap result
     *
     * @param dynamicRootPageId if true, pass in the btree rootPageId as a
     * dynamic parameter
     *
     * @param useDynamicKeys if true, pass in search key values using dynamic
     * parameters
     *
     * @param includeRid include rid in search
     *
     * @param vals search key values
     */
    void testScanIdx(
        uint totalKeys, uint nKeys, uint bufSize,
        boost::shared_array<FixedBuffer> inputBuffer,
        uint expectedNBitmaps, PBuffer expectedBitmaps,
        bool dynamicRootPageId,
        bool useDynamicKeys,
        bool includeRid,
        const boost::scoped_array<uint64_t> &vals);

    /**
     * Initializes input search key and directives for an equality search
     *
     * @param nKeys number of keys to search on
     * @param nInputTuples number of search ranges to create
     * @param vals values to search on
     * @param lowerDirective lower bound search directive
     * @param upperDirective upper bound search directive
     * @param inputTupleAccessor accessor to marshal/unmarshal search key
     * @param inputTupleData tupledata storing search key
     * @param inputBuffer buffer storing search key
     * @param useDynamicKeys if true, search key values are passed to the
     * search stream using dynamic parameters
     */
    void initEqualSearch(
        uint nKeys, uint nInputTuples, boost::scoped_array<uint64_t> &vals,
        char &lowerDirective, char &upperDirective,
        TupleAccessor &inputTupleAccessor, TupleData &inputTupleData,
        boost::shared_array<FixedBuffer> &inputBuffer,
        bool useDynamicKeys);

    void setSearchKey(
        char lowerDirective, char upperDirective, uint64_t lowerVal,
        uint64_t upperVal, PBuffer inputBuf, uint &offset,
        TupleAccessor &inputTupleAccessor, TupleData &inputTupleData);

public:
    explicit LbmSearchTest()
    {
        FENNEL_UNIT_TEST_CASE(LbmSearchTest, testScanOneLevel);
        FENNEL_UNIT_TEST_CASE(LbmSearchTest, testScanTwoLevel);
        FENNEL_UNIT_TEST_CASE(LbmSearchTest, testMultipleRanges);
    }

    void testCaseSetUp();
    void testCaseTearDown();
    void testScans(uint nRows);

    void testScanOneLevel();
    void testScanTwoLevel();
    void testMultipleRanges();
};

void LbmSearchTest::testScanOneLevel()
{
    // single level btree
    testScans(100);
}

void LbmSearchTest::testScanTwoLevel()
{
    // with a 3-key index, 2000 rows will generate a 2-level btree
    testScans(2000);
}

void LbmSearchTest::testScans(uint nRows)
{
    uint nClusters = 3;
    std::vector<int> repeatSeqValues;

    // load the data
    repeatSeqValues.push_back(1);
    repeatSeqValues.push_back(5);
    repeatSeqValues.push_back(9);
    loadTableAndIndex(nRows, nClusters, repeatSeqValues, true);

    // Scan on all keys, passing the search keys through the input stream.
    // Then do the same, passing the keys via dynamic parameters.  Then,
    // finally, do the search, including a rid search.
    testScanFullKey(nRows, nClusters, repeatSeqValues, false, false);
    testScanFullKey(nRows, nClusters, repeatSeqValues, true, false);
    testScanFullKey(nRows, nClusters, repeatSeqValues, false, true);

    // scan on (nClusters - 1) keys
    testScanPartialKey(nRows, nClusters, repeatSeqValues);
}

void LbmSearchTest::testMultipleRanges()
{
    uint nRows = 20000;
    uint nClusters = 1;
    std::vector<int> repeatSeqValues;

    // load a table with a single index on a single column
    repeatSeqValues.push_back(100);
    loadTableAndIndex(nRows, nClusters, repeatSeqValues, true);

    // scan on all keys, just to make sure all key values really are there
    testScanFullKey(nRows, nClusters, repeatSeqValues, false, false);

    resetExecStreamTest();

    // Setup the following search keys.  Note that these keys weren't randomly
    // selected.  Some of them correspond to boundary conditions.
    // 1. key < 8
    // 2. key > 10 && key <= 17
    // 3. key >= 44 and key < 60
    // 4. key > 71

    TupleDescriptor inputTupleDesc;
    for (uint i = 0; i < 2; i++) {
        inputTupleDesc.push_back(attrDesc_char1);
        inputTupleDesc.push_back(attrDesc_nullableInt64);
    }
    TupleData inputTupleData(inputTupleDesc);
    TupleAccessor inputTupleAccessor;
    inputTupleAccessor.compute(inputTupleDesc);

    uint nInputTuples = 4;
    boost::shared_array<FixedBuffer> inputBuffer;
    inputBuffer.reset(
        new FixedBuffer[nInputTuples * inputTupleAccessor.getMaxByteCount()]);
    PBuffer inputBuf = inputBuffer.get();
    uint offset = 0;

    setSearchKey(
        '-', ')', 0, 8, inputBuf, offset, inputTupleAccessor, inputTupleData);
    setSearchKey(
        '(', ']', 10, 17, inputBuf, offset, inputTupleAccessor,
        inputTupleData);
    setSearchKey(
        '[', ')', 44, 60, inputBuf, offset, inputTupleAccessor,
        inputTupleData);
    setSearchKey(
        '(', '+', 71, 0, inputBuf, offset, inputTupleAccessor,
        inputTupleData);

    // setup the expected bitmap result values
    boost::scoped_array<FixedBuffer> expectedBitmaps;
    uint bufferSize = ((nRows / repeatSeqValues[0] / 8 + 1) * 60) * 24;
    expectedBitmaps.reset(new FixedBuffer[bufferSize]);
    PBuffer bitmapBuf = expectedBitmaps.get();
    uint expectedNBitmaps = 0;
    uint expectedBufSize = 0;
    // for each range, generate the bitmap values for each key in the desired
    // range
    for (uint i = 0; i < 8; i++) {
        generateBitmaps(
            nRows, i, repeatSeqValues[0], bitmapBuf, expectedBufSize,
            bufferSize, expectedNBitmaps);
    }
    for (uint i = 11; i <= 17; i++) {
        generateBitmaps(
            nRows, i, repeatSeqValues[0], bitmapBuf, expectedBufSize,
            bufferSize, expectedNBitmaps);
    }
    for (uint i = 44; i < 60; i++) {
        generateBitmaps(
            nRows, i, repeatSeqValues[0], bitmapBuf, expectedBufSize,
            bufferSize, expectedNBitmaps);
    }
    for (uint i = 72; i < repeatSeqValues[0]; i++) {
        generateBitmaps(
            nRows, i, repeatSeqValues[0], bitmapBuf, expectedBufSize,
            bufferSize, expectedNBitmaps);
    }

    boost::scoped_array<uint64_t> vals;
    testScanIdx(
        nClusters, nClusters, offset, inputBuffer, expectedNBitmaps,
        bitmapBuf, true, false, false, vals);
}

void LbmSearchTest::setSearchKey(
    char lowerDirective, char upperDirective, uint64_t lowerVal,
    uint64_t upperVal, PBuffer inputBuf, uint &offset,
    TupleAccessor &inputTupleAccessor, TupleData &inputTupleData)
{
    inputTupleData[0].pData = (PConstBuffer) &lowerDirective;
    inputTupleData[2].pData = (PConstBuffer) &upperDirective;
    if (lowerDirective != '-') {
        inputTupleData[1].pData = (PConstBuffer) &lowerVal;
    }
    if (upperDirective != '+') {
        inputTupleData[3].pData = (PConstBuffer) &upperVal;
    }
    inputTupleAccessor.marshal(inputTupleData, inputBuf + offset);
    offset += inputTupleAccessor.getCurrentByteCount();
}

void LbmSearchTest::testScanFullKey(
    uint nRows, uint nKeys, std::vector<int> const &repeatSeqValues,
    bool useDynamicKeys, bool includeRid)
{
    // search for key0 = <val0>, key1 = <val1>, ..., key(n-1) = <val(n-1)>
    uint nInputTuples = 1;
    boost::scoped_array<uint64_t> vals;
    char lowerDirective;
    char upperDirective;
    TupleAccessor inputTupleAccessor;
    TupleData inputTupleData;
    boost::shared_array<FixedBuffer> inputBuffer;

    initEqualSearch(
        nKeys, nInputTuples, vals, lowerDirective, upperDirective,
        inputTupleAccessor, inputTupleData, inputBuffer, useDynamicKeys);

    // do a search on each possible key combo
    uint skipRows = 1;
    for (uint i = 0; i < nKeys; i++) {
        skipRows *= repeatSeqValues[i];
    }
    for (uint i = 0; i < skipRows; i++) {
        // generate input keys for search
        for (uint j = 0; j < nKeys; j++) {
            vals[j] = i % repeatSeqValues[j];
        }
        inputTupleAccessor.marshal(inputTupleData, inputBuffer.get());

        // generate expected bitmap result
        boost::scoped_array<FixedBuffer> expectedBitmaps;
        uint bufferSize = (nRows / skipRows + 1) * 16;
        expectedBitmaps.reset(new FixedBuffer[bufferSize]);
        uint expectedNBitmaps = 0;
        uint expectedBufSize = 0;
        generateBitmaps(
            nRows, i, skipRows, expectedBitmaps.get(), expectedBufSize,
            bufferSize, expectedNBitmaps);

        testScanIdx(
            nKeys, nKeys, inputTupleAccessor.getCurrentByteCount(),
            inputBuffer, expectedNBitmaps, expectedBitmaps.get(), false,
            useDynamicKeys, includeRid, vals);
    }
}

void LbmSearchTest::testScanPartialKey(
    uint nRows, uint nKeys, std::vector<int> const &repeatSeqValues)
{
    // search for key0 = 0, key1 = 0, ..., key(n-2) = 0
    uint nInputTuples = 1;
    boost::scoped_array<uint64_t> vals;
    char lowerDirective;
    char upperDirective;
    TupleAccessor inputTupleAccessor;
    TupleData inputTupleData;
    boost::shared_array<FixedBuffer> inputBuffer;

    initEqualSearch(
        nKeys - 1, nInputTuples, vals, lowerDirective, upperDirective,
        inputTupleAccessor, inputTupleData, inputBuffer, false);

    // generate input keys for search
    for (uint j = 0; j < nKeys - 1; j++) {
        vals[j] = 0;
    }
    inputTupleAccessor.marshal(inputTupleData, inputBuffer.get());

    // Generate one set of bitmaps for each key combo that can be combined
    // with the partial key. E.g., if there are 3 keys, generate the
    // bitmaps that would be obtained from searching for (0, 0, 0), (0, 0, 1),
    // ..., (0, 0, repeatSeqValues[nKeys-1] - 1)

    uint skipRows = 1;
    for (uint i = 0; i < nKeys - 1; i++) {
        skipRows *= repeatSeqValues[i];
    }
    boost::scoped_array<FixedBuffer> expectedBitmaps;
    uint bufferSize = (nRows / skipRows / 8 + 1)
        * 12 * repeatSeqValues[nKeys - 1];
    expectedBitmaps.reset(new FixedBuffer[bufferSize]);
    PBuffer bitmapBuf = expectedBitmaps.get();
    uint expectedNBitmaps = 0;
    uint curBufSize = 0;

    for (uint i = 0; i < repeatSeqValues[nKeys - 1]; i++) {
        uint start;
        if (i == 0) {
            start = 0;
        } else {
            // look for the first rid where the last key is equal to "i" and
            // the preceeding keys are all 0
            for (start = i; start < nRows;
                 start += repeatSeqValues[nKeys - 1])
            {
                uint j;
                for (j = 0; j < nKeys - 1; j++) {
                    if (start % repeatSeqValues[j] != 0) {
                        break;
                    }
                }
                if (j == nKeys - 1) {
                    break;
                }
            }
            if (start >= nRows) {
                continue;
            }
        }
        generateBitmaps(
            nRows, start, skipRows * repeatSeqValues[nKeys - 1],
            bitmapBuf, curBufSize, bufferSize, expectedNBitmaps);
    }
    testScanIdx(
        nKeys, nKeys - 1, inputTupleAccessor.getCurrentByteCount(),
        inputBuffer, expectedNBitmaps, bitmapBuf, false, false, false, vals);
}

void LbmSearchTest::initEqualSearch(
    uint nKeys, uint nInputTuples, boost::scoped_array<uint64_t> &vals,
    char &lowerDirective, char &upperDirective,
    TupleAccessor &inputTupleAccessor, TupleData &inputTupleData,
    boost::shared_array<FixedBuffer> &inputBuffer,
    bool useDynamicKeys)
{
    TupleDescriptor inputTupleDesc;
    for (uint i = 0; i < 2; i++) {
        inputTupleDesc.push_back(attrDesc_char1);
        for (uint j = 0; j < nKeys; j++) {
            inputTupleDesc.push_back(attrDesc_nullableInt64);
        }
    }

    inputTupleData.compute(inputTupleDesc);

    vals.reset(new uint64_t[nKeys]);
    lowerDirective = '[';
    inputTupleData[0].pData = (PConstBuffer) &lowerDirective;
    upperDirective = ']';
    inputTupleData[nKeys + 1].pData = (PConstBuffer) &upperDirective;
    for (uint i = 0; i < nKeys; i++) {
        // If keys are being passed through dynamic parameters, set them to
        // NULL in the input row
        if (useDynamicKeys) {
            inputTupleData[i + 1].pData = NULL;
            inputTupleData[i + 1].cbData = 0;
            inputTupleData[nKeys + 1 + i + 1].pData = NULL;
            inputTupleData[nKeys + 1 + i + 1].cbData = 0;
        } else {
            inputTupleData[i + 1].pData = (PConstBuffer) &vals[i];
            inputTupleData[nKeys + 1 + i + 1].pData = (PConstBuffer) &vals[i];
        }
    }

    inputTupleAccessor.compute(inputTupleDesc);

    inputBuffer.reset(
        new FixedBuffer[nInputTuples * inputTupleAccessor.getMaxByteCount()]);
}

void LbmSearchTest::loadTableAndIndex(
    uint nRows, uint nClusters, std::vector<int> const &repeatSeqValues,
    bool newRoot)
{
    // 0. reset member fields.
    for (uint i = 0; i < bTreeClusters.size(); i++) {
        bTreeClusters[i]->segmentAccessor.reset();
    }
    for (uint i = 0; i < bTreeBitmaps.size(); i++) {
        bTreeBitmaps[i]->segmentAccessor.reset();
    }
    bTreeClusters.clear();
    bTreeBitmaps.clear();

    // 1. setup mock input stream

    MockProducerExecStreamParams mockParams;
    for (uint i = 0; i < nClusters; i++) {
        mockParams.outputTupleDesc.push_back(attrDesc_int64);
    }
    mockParams.nRows = nRows;

    vector<boost::shared_ptr<ColumnGenerator<int64_t> > > columnGenerators;
    SharedInt64ColumnGenerator col;
    assert(repeatSeqValues.size() == nClusters);
    for (uint i = 0; i < repeatSeqValues.size(); i++) {
        col =
            SharedInt64ColumnGenerator(
                new RepeatingSeqColumnGenerator(repeatSeqValues[i]));
        columnGenerators.push_back(col);
    }
    mockParams.pGenerator.reset(
        new CompositeExecStreamGenerator(columnGenerators));

    ExecStreamEmbryo mockStreamEmbryo;
    mockStreamEmbryo.init(new MockProducerExecStream(), mockParams);
    mockStreamEmbryo.getStream()->setName("MockProducerExecStream");

    // 2. setup splitter stream for cluster loads

    SplitterExecStreamParams splitterParams;
    ExecStreamEmbryo splitterStreamEmbryo;
    splitterStreamEmbryo.init(new SplitterExecStream(), splitterParams);
    splitterStreamEmbryo.getStream()->setName("ClusterSplitterExecStream");

    // 3. setup loader streams

    vector<ExecStreamEmbryo> lcsAppendEmbryos;
    for (uint i = 0; i < nClusters; i++) {
        LcsClusterAppendExecStreamParams lcsAppendParams;
        boost::shared_ptr<BTreeDescriptor> pBTreeDesc =
            boost::shared_ptr<BTreeDescriptor> (new BTreeDescriptor());
        bTreeClusters.push_back(pBTreeDesc);

        // initialize the btree parameter portion of lcsAppendParams
        // BTree tuple desc has two columns (rid, clusterPageid)
        (lcsAppendParams.tupleDesc).push_back(attrDesc_int64);
        (lcsAppendParams.tupleDesc).push_back(attrDesc_int64);

        // BTree key only has one column which is the first column.
        (lcsAppendParams.keyProj).push_back(0);

        initBTreeExecStreamParam(lcsAppendParams, pBTreeDesc);

        // output two values (rows inserted, starting rid value)
        lcsAppendParams.outputTupleDesc.push_back(attrDesc_int64);
        lcsAppendParams.outputTupleDesc.push_back(attrDesc_int64);

        lcsAppendParams.inputProj.push_back(i);

        // create an empty page to start the btree

        if (newRoot) {
            BTreeBuilder builder(*pBTreeDesc, pRandomSegment);
            builder.createEmptyRoot();
            savedBTreeClusterRootIds.push_back(builder.getRootPageId());
        }
        lcsAppendParams.rootPageId = pBTreeDesc->rootPageId =
            savedBTreeClusterRootIds[i];

        // Now use the above initialized parameter

        ExecStreamEmbryo lcsAppendStreamEmbryo;
        lcsAppendStreamEmbryo.init(
            new LcsClusterAppendExecStream(), lcsAppendParams);
        std::ostringstream oss;
        oss << "LcsClusterAppendExecStream" << "#" << i;
        lcsAppendStreamEmbryo.getStream()->setName(oss.str());
        lcsAppendEmbryos.push_back(lcsAppendStreamEmbryo);
    }

    // 4. setup barrier stream for cluster loads

    BarrierExecStreamParams barrierParams;
    barrierParams.outputTupleDesc.push_back(attrDesc_int64);
    barrierParams.outputTupleDesc.push_back(attrDesc_int64);
    barrierParams.returnMode = BARRIER_RET_ANY_INPUT;

    ExecStreamEmbryo clusterBarrierStreamEmbryo;
    clusterBarrierStreamEmbryo.init(new BarrierExecStream(), barrierParams);
    clusterBarrierStreamEmbryo.getStream()->setName("ClusterBarrierExecStream");

    // create a DAG with the above, but without the final output sink
    prepareDAG(
        mockStreamEmbryo, splitterStreamEmbryo, lcsAppendEmbryos,
        clusterBarrierStreamEmbryo, false);

    // 5. setup splitter stream for create bitmaps

    splitterStreamEmbryo.init(
        new SplitterExecStream(), splitterParams);
    splitterStreamEmbryo.getStream()->setName("BitmapSplitterExecStream");

    // create streams for bitmap generator, sort, and bitmap splicer to
    // build an index on all columns

    std::vector<std::vector<ExecStreamEmbryo> > createBitmapStreamList;
        std::vector<ExecStreamEmbryo> createBitmapStream;

    // 6. setup generator

    LbmGeneratorExecStreamParams generatorParams;
    struct LcsClusterScanDef clusterScanDef;
    clusterScanDef.clusterTupleDesc.push_back(attrDesc_int64);

    for (uint j = 0; j < nClusters; j++) {
        initClusterScanDef(generatorParams, clusterScanDef, j);
    }

    TupleProjection proj;
    for (uint j = 0; j < nClusters; j++) {
        proj.push_back(j);
    }
    generatorParams.outputProj = proj;
    generatorParams.insertRowCountParamId = DynamicParamId(1);
    generatorParams.createIndex = false;

    boost::shared_ptr<BTreeDescriptor> pBTreeDesc =
        boost::shared_ptr<BTreeDescriptor> (new BTreeDescriptor());
    bTreeBitmaps.push_back(pBTreeDesc);

    // BTree tuple desc has the key columns + starting Rid + varbinary
    // field for bit segments/bit descriptors
    uint nKeys = nClusters;
    initBTreeTupleDesc(generatorParams.outputTupleDesc, nKeys);

    initBTreeBitmapDesc(
        generatorParams.tupleDesc, generatorParams.keyProj, nKeys);
    initBTreeExecStreamParam(generatorParams, pBTreeDesc);

    // create an empty page to start the btree

    if (newRoot) {
        BTreeBuilder builder(*pBTreeDesc, pRandomSegment);
        builder.createEmptyRoot();
        savedBTreeBitmapRootIds.push_back(builder.getRootPageId());
    }
    generatorParams.rootPageId = pBTreeDesc->rootPageId =
        savedBTreeBitmapRootIds[0];

    ExecStreamEmbryo generatorStreamEmbryo;
    generatorStreamEmbryo.init(
        new LbmGeneratorExecStream(), generatorParams);
    std::ostringstream oss;
    oss << "LbmGeneratorExecStream" << "#" << 0;
    generatorStreamEmbryo.getStream()->setName(oss.str());
    createBitmapStream.push_back(generatorStreamEmbryo);

    // 7. setup sorter

    ExternalSortExecStreamParams sortParams;
    initBTreeBitmapDesc(
        sortParams.outputTupleDesc, sortParams.keyProj, nKeys);
    sortParams.distinctness = DUP_ALLOW;
    sortParams.pTempSegment = pRandomSegment;
    sortParams.pCacheAccessor = pCache;
    sortParams.scratchAccessor =
        pSegmentFactory->newScratchSegment(pCache, 10);
    sortParams.storeFinalRun = false;
    sortParams.partitionKeyCount = 0;
    sortParams.estimatedNumRows = MAXU;
    sortParams.earlyClose = false;

    ExecStreamEmbryo sortStreamEmbryo;
    sortStreamEmbryo.init(
        ExternalSortExecStream::newExternalSortExecStream(), sortParams);
    sortStreamEmbryo.getStream()->setName("ExternalSortExecStream");
    std::ostringstream oss2;
    oss2 << "ExternalSortExecStream" << "#" << 0;
    sortStreamEmbryo.getStream()->setName(oss2.str());
    createBitmapStream.push_back(sortStreamEmbryo);

    // 8. setup splicer

    LbmSplicerExecStreamParams splicerParams;
    splicerParams.createNewIndex = false;
    splicerParams.scratchAccessor =
        pSegmentFactory->newScratchSegment(pCache, 15);
    splicerParams.pCacheAccessor = pCache;
    BTreeParams bTreeParams;
    initBTreeBitmapDesc(
        bTreeParams.tupleDesc, bTreeParams.keyProj, nKeys);
    initBTreeParam(bTreeParams, pBTreeDesc);
    bTreeParams.rootPageId = pBTreeDesc->rootPageId;
    splicerParams.bTreeParams.push_back(bTreeParams);
    splicerParams.insertRowCountParamId = DynamicParamId(1);
    splicerParams.writeRowCountParamId = DynamicParamId(0);
    splicerParams.outputTupleDesc.push_back(attrDesc_int64);

    ExecStreamEmbryo splicerStreamEmbryo;
    splicerStreamEmbryo.init(new LbmSplicerExecStream(), splicerParams);
    std::ostringstream oss3;
    oss3 << "LbmSplicerExecStream" << "#" << 0;
    splicerStreamEmbryo.getStream()->setName(oss3.str());
    createBitmapStream.push_back(splicerStreamEmbryo);

    // connect the sorter and splicer to generator and then add this
    // newly connected stream to the list of create bitmap stream embryos
    createBitmapStreamList.push_back(createBitmapStream);

    // 9. setup barrier stream for create bitmaps

    barrierParams.outputTupleDesc.clear();
    barrierParams.outputTupleDesc.push_back(attrDesc_int64);

    ExecStreamEmbryo barrierStreamEmbryo;
    barrierStreamEmbryo.init(
        new BarrierExecStream(), barrierParams);
    barrierStreamEmbryo.getStream()->setName("BitmapBarrierExecStream");

    // create the bitmap stream graph, with the load stream graph from
    // above as the source
    SharedExecStream pOutputStream = prepareDAG(
        clusterBarrierStreamEmbryo, splitterStreamEmbryo,
        createBitmapStreamList, barrierStreamEmbryo, true, false);

    // set up a generator which can produce the expected output
    RampExecStreamGenerator expectedResultGenerator(mockParams.nRows);

    verifyOutput(*pOutputStream, 1, expectedResultGenerator);
}

void LbmSearchTest::initBTreeExecStreamParam(
    BTreeExecStreamParams &param, shared_ptr<BTreeDescriptor> pBTreeDesc)
{
    param.scratchAccessor = pSegmentFactory->newScratchSegment(pCache, 15);
    param.pCacheAccessor = pCache;
    initBTreeParam(param, pBTreeDesc);
}

void LbmSearchTest::initBTreeParam(
    BTreeParams &param, shared_ptr<BTreeDescriptor> pBTreeDesc)
{
    param.pSegment = pRandomSegment;
    param.pRootMap = 0;
    param.rootPageIdParamId = DynamicParamId(0);

    pBTreeDesc->segmentAccessor.pSegment = param.pSegment;
    pBTreeDesc->segmentAccessor.pCacheAccessor = pCache;
    pBTreeDesc->tupleDescriptor = param.tupleDesc;
    pBTreeDesc->keyProjection = param.keyProj;
    param.pageOwnerId = pBTreeDesc->pageOwnerId;
    param.segmentId = pBTreeDesc->segmentId;
}

void LbmSearchTest::initClusterScanDef(
    LcsRowScanBaseExecStreamParams &rowScanParams,
    struct LcsClusterScanDef &clusterScanDef,
    uint bTreeIndex)
{
    clusterScanDef.pSegment =
        bTreeClusters[bTreeIndex]->segmentAccessor.pSegment;
    clusterScanDef.pCacheAccessor =
        bTreeClusters[bTreeIndex]->segmentAccessor.pCacheAccessor;
    clusterScanDef.tupleDesc = bTreeClusters[bTreeIndex]->tupleDescriptor;
    clusterScanDef.keyProj = bTreeClusters[bTreeIndex]->keyProjection;
    clusterScanDef.rootPageId = bTreeClusters[bTreeIndex]->rootPageId;
    clusterScanDef.pageOwnerId = bTreeClusters[bTreeIndex]->pageOwnerId;
    clusterScanDef.segmentId = bTreeClusters[bTreeIndex]->segmentId;
    clusterScanDef.pRootMap = 0;
    clusterScanDef.rootPageIdParamId = DynamicParamId(0);
    rowScanParams.lcsClusterScanDefs.push_back(clusterScanDef);
}

void LbmSearchTest::initBTreeBitmapDesc(
    TupleDescriptor &tupleDesc, TupleProjection &keyProj, uint nKeys)
{
    initBTreeTupleDesc(tupleDesc, nKeys);

    // btree key consists of the key columns and the start rid
    for (uint j = 0; j < nKeys + 1; j++) {
        keyProj.push_back(j);
    }
}

void LbmSearchTest::initBTreeTupleDesc(
    TupleDescriptor &tupleDesc, uint nKeys)
{
    for (uint i = 0; i < nKeys; i++) {
        tupleDesc.push_back(attrDesc_int64);
    }
    // add on the rid and bitmaps
    tupleDesc.push_back(bitmapTupleDesc[0]);
    tupleDesc.push_back(bitmapTupleDesc[1]);
    tupleDesc.push_back(bitmapTupleDesc[2]);
}

void LbmSearchTest::testCaseSetUp()
{
    LbmExecStreamTestBase::testCaseSetUp();

    attrDesc_char1 = TupleAttributeDescriptor(
        stdTypeFactory.newDataType(STANDARD_TYPE_CHAR), false, 1);
    attrDesc_nullableInt64 = TupleAttributeDescriptor(
        stdTypeFactory.newDataType(STANDARD_TYPE_INT_64),
        true, sizeof(uint64_t));
}

void LbmSearchTest::testCaseTearDown()
{
    for (uint i = 0; i < bTreeClusters.size(); i++) {
        bTreeClusters[i]->segmentAccessor.reset();
    }
    for (uint i = 0; i < bTreeBitmaps.size(); i++) {
        bTreeBitmaps[i]->segmentAccessor.reset();
    }
    bTreeClusters.clear();
    bTreeBitmaps.clear();
    savedBTreeClusterRootIds.clear();
    savedBTreeBitmapRootIds.clear();

    LbmExecStreamTestBase::testCaseTearDown();
}

void LbmSearchTest::testScanIdx(
    uint totalKeys, uint nKeys, uint bufSize,
    boost::shared_array<FixedBuffer> inputBuffer,
    uint expectedNBitmaps, PBuffer expectedBitmaps,
    bool dynamicRootPageId,
    bool useDynamicKeys,
    bool includeRid,
    const boost::scoped_array<uint64_t> &vals)
{
    resetExecStreamTest();

    // setup input into index scan; values stream will read tuples from
    // inputBuffer

    ValuesExecStreamParams valuesParams;
    for (uint i = 0; i < 2; i++) {
        valuesParams.outputTupleDesc.push_back(attrDesc_char1);
        for (uint j = 0; j < nKeys; j++) {
            valuesParams.outputTupleDesc.push_back(attrDesc_nullableInt64);
        }
    }
    valuesParams.pTupleBuffer = inputBuffer;
    valuesParams.bufSize = bufSize;

    ExecStreamEmbryo valuesStreamEmbryo;
    valuesStreamEmbryo.init(new ValuesExecStream(), valuesParams);
    valuesStreamEmbryo.getStream()->setName("ValuesExecStream");

    // setup index scan stream

    LbmSearchExecStreamParams indexScanParams;

    // initialize parameters specific to indexScan
    indexScanParams.rowLimitParamId = DynamicParamId(0);
    if (includeRid) {
        indexScanParams.startRidParamId = DynamicParamId(1);
        SharedDynamicParamManager pDynamicParamManager =
            pGraph->getDynamicParamManager();
        pDynamicParamManager->createParam(DynamicParamId(1), attrDesc_int64);
        TupleDatum ridDatum;
        LcsRid rid = LcsRid(0);
        ridDatum.pData = (PConstBuffer) &rid;
        ridDatum.cbData = sizeof(LcsRid);
        pDynamicParamManager->writeParam(DynamicParamId(1), ridDatum);
    } else {
        indexScanParams.startRidParamId = DynamicParamId(0);
    }

    // initialize parameters for btree read
    initBTreeBitmapDesc(
        indexScanParams.tupleDesc, indexScanParams.keyProj, totalKeys);
    initBTreeExecStreamParam(indexScanParams, bTreeBitmaps[0]);
    bTreeBitmaps[0]->rootPageId = savedBTreeBitmapRootIds[0];

    if (!dynamicRootPageId) {
        indexScanParams.rootPageId = savedBTreeBitmapRootIds[0];
    } else {
        indexScanParams.rootPageId = NULL_PAGE_ID;
        indexScanParams.rootPageIdParamId = DynamicParamId(2);
        SharedDynamicParamManager pDynamicParamManager =
            pGraph->getDynamicParamManager();
        pDynamicParamManager->createParam(DynamicParamId(2), attrDesc_int64);
        TupleDatum rootPageIdDatum;
        rootPageIdDatum.pData = (PConstBuffer) &(savedBTreeBitmapRootIds[0]);
        rootPageIdDatum.cbData = sizeof(PageId);
        pDynamicParamManager->writeParam(DynamicParamId(2), rootPageIdDatum);
    }

    TupleProjection outputProj;
    for (uint i = totalKeys; i < totalKeys + 3; i++) {
        outputProj.push_back(i);
    }
    indexScanParams.outputProj = outputProj;

    // initialize parameters for btree search
    indexScanParams.outerJoin = false;
    TupleProjection inputKeyProj;
    for (uint i = 0; i < 2; i++) {
        for (uint j = 0; j < nKeys; j++) {
            inputKeyProj.push_back(i * (nKeys + 1) + j + 1);
        }
    }
    indexScanParams.inputKeyProj = inputKeyProj;
    indexScanParams.inputDirectiveProj.push_back(0);
    indexScanParams.inputDirectiveProj.push_back(nKeys + 1);

    // output is bitmap btree tuple without the key values, but with the rid
    indexScanParams.outputTupleDesc = bitmapTupleDesc;

    if (useDynamicKeys) {
        SharedDynamicParamManager pDynamicParamManager =
            pGraph->getDynamicParamManager();
        for (uint i = 3; i < nKeys * 2 + 3; i++) {
            indexScanParams.searchKeyParams.push_back(
                BTreeSearchKeyParameter(
                    DynamicParamId(i),
                    i - 3));
            pDynamicParamManager->createParam(
                DynamicParamId(i), attrDesc_int64);
            TupleDatum keyValDatum;
            keyValDatum.pData = (PConstBuffer) &(vals[(i - 3) % nKeys]);
            keyValDatum.cbData = sizeof(uint64_t);
            pDynamicParamManager->writeParam(DynamicParamId(i), keyValDatum);
        }
    }

    ExecStreamEmbryo indexScanStreamEmbryo;
    indexScanStreamEmbryo.init(new LbmSearchExecStream(), indexScanParams);
    indexScanStreamEmbryo.getStream()->setName("IndexScanStream");

    SharedExecStream pOutputStream = prepareTransformGraph(
        valuesStreamEmbryo, indexScanStreamEmbryo);

    bitmapTupleAccessor.setCurrentTupleBuf(expectedBitmaps);
    verifyBufferedOutput(
        *pOutputStream, bitmapTupleDesc, expectedNBitmaps, expectedBitmaps);
}

FENNEL_UNIT_TEST_SUITE(LbmSearchTest);

// End LbmSearchTest.cpp
