/*
// $Id$
// Fennel is a library of data storage and processing components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 2004 John V. Sichi
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
#include "fennel/exec/MockProducerExecStream.h"
#include "fennel/tuple/TupleAccessor.h"
#include "fennel/tuple/TuplePrinter.h"
#include "fennel/tuple/StandardTypeDescriptor.h"
#include "fennel/exec/ExecStreamBufAccessor.h"
#include <boost/scoped_array.hpp>


FENNEL_BEGIN_CPPFILE("$Id$");

MockProducerExecStream::MockProducerExecStream()
{
    cbTuple = 0;
    nRowsProduced = nRowsMax = 0;
    saveTuples = false;
    echoTuples = 0;
}

SharedMockProducerExecStreamGenerator MockProducerExecStream::getGenerator()
{
    return pGenerator;
}

void MockProducerExecStream::prepare(MockProducerExecStreamParams const &params)
{
    SingleOutputExecStream::prepare(params);
    pGenerator = params.pGenerator;
    for (uint i = 0; i < params.outputTupleDesc.size(); i++) {
        assert(!params.outputTupleDesc[i].isNullable);
        StandardTypeDescriptorOrdinal ordinal =
            StandardTypeDescriptorOrdinal(
                params.outputTupleDesc[i].pTypeDescriptor->getOrdinal());
        assert(StandardTypeDescriptor::isIntegralNative(ordinal));
        if (pGenerator) {
            assert(ordinal == STANDARD_TYPE_INT_64);
        }
    }
    outputData.compute(params.outputTupleDesc);
    TupleAccessor &tupleAccessor = pOutAccessor->getScratchTupleAccessor();
    assert(tupleAccessor.isFixedWidth());
    cbTuple = tupleAccessor.getMaxByteCount();
    nRowsMax = params.nRows;
    saveTuples = params.saveTuples;
    echoTuples = params.echoTuples;
    if (saveTuples || echoTuples) {
        assert(pGenerator);
    }
}

void MockProducerExecStream::open(bool restart)
{
    SingleOutputExecStream::open(restart);
    nRowsProduced = 0;
    savedTuples.clear();
    if (saveTuples) {
        // assume it's not too big
        savedTuples.reserve(nRowsMax);
    }
}

ExecStreamResult MockProducerExecStream::innerExecute(
    ExecStreamQuantum const &quantum)
{
    TuplePrinter tuplePrinter;
    uint nTuples = 0;
    boost::scoped_array<int64_t> values(new int64_t[outputData.size()]);
    for (int col = 0; col < outputData.size(); ++col) {
        outputData[col].pData = reinterpret_cast<PConstBuffer>(
            &(values.get()[col]));
    }

    while (nRowsProduced < nRowsMax) {
        if (pGenerator->endsBatch(nRowsProduced)) {
            return onEndOfBatch(nRowsProduced);
        }
        if (pOutAccessor->getProductionAvailable() < cbTuple) {
            return EXECRC_BUF_OVERFLOW;
        }

        for (int col = 0; col < outputData.size(); ++col) {
            values.get()[col] =
                pGenerator->generateValue(nRowsProduced, col);
        }
        bool rc = pOutAccessor->produceTuple(outputData);
        assert(rc);

        if (echoTuples) {
            tuplePrinter.print(
                *echoTuples,
                pOutAccessor->getTupleDesc(), outputData);
        }
        if (saveTuples) {
            std::ostringstream oss;
            tuplePrinter.print(
                oss, pOutAccessor->getTupleDesc(), outputData);
            savedTuples.push_back(oss.str());
        }
        ++nRowsProduced;
        if (++nTuples >= quantum.nTuplesMax) {
            return EXECRC_QUANTUM_EXPIRED;
        }
    }
    pOutAccessor->markEOS();
    return EXECRC_EOS;
}

ExecStreamResult MockProducerExecStream::onEndOfBatch(uint)
{
    return EXECRC_QUANTUM_EXPIRED;
}

ExecStreamResult MockProducerExecStream::execute(
    ExecStreamQuantum const &quantum)
{
    if (pGenerator) {
        uint64_t oldRowCount = nRowsProduced;
        ExecStreamResult rc = innerExecute(quantum);
        FENNEL_TRACE(
            TRACE_FINE,
            "wrote " << (nRowsProduced - oldRowCount)
            << " rows, total rows written is " << nRowsProduced);
        return rc;
    }

    // NOTE: implementation below is kept lean and mean
    // intentionally so that it can be used to drive other streams with minimal
    // overhead during profiling

    uint cb = pOutAccessor->getProductionAvailable();
    uint nRows = std::min<uint64_t>(nRowsMax - nRowsProduced, cb / cbTuple);
    uint cbBatch = nRows * cbTuple;

    // TODO:  pOutAccessor->validateTupleSize(?);
    if (cbBatch) {
        cb -= cbBatch;
        nRowsProduced += nRows;
        PBuffer pBuffer = pOutAccessor->getProductionStart();
        memset(pBuffer, 0, cbBatch);
        pOutAccessor->produceData(pBuffer + cbBatch);
        pOutAccessor->requestConsumption();
        FENNEL_TRACE(
            TRACE_FINE, "wrote " << nRows
            << " rows, total rows written is " << nRowsProduced);
    }
    if (nRowsProduced == nRowsMax) {
        pOutAccessor->markEOS();
        return EXECRC_EOS;
    } else {
        return EXECRC_BUF_OVERFLOW;
    }
}

uint64_t MockProducerExecStream::getGeneratedRowCount()
{
    return nRowsProduced;
}

uint64_t MockProducerExecStream::getProducedRowCount()
{
    uint waitingRowCount = pOutAccessor->getConsumptionTuplesAvailable();
    return nRowsProduced - waitingRowCount;
}

MockProducerExecStreamGenerator::MockProducerExecStreamGenerator()
{
}

MockProducerExecStreamGenerator::~MockProducerExecStreamGenerator()
{
}

void MockProducerExecStreamGenerator::setEndsBatchPredicate(
    SharedRowPredicate f)
{
    endsBatchPredicate = f;
}

bool MockProducerExecStreamGenerator::endsBatch(uint iRow)
{
    if (endsBatchPredicate) {
        return (*endsBatchPredicate)(iRow);
    } else {
        return false;
    }
}

MockProducerExecStreamGenerator::RowPredicate::RowPredicate(bool val)
    : defaultValue(val)
{
}

MockProducerExecStreamGenerator::RowPredicate::~RowPredicate()
{
}

bool MockProducerExecStreamGenerator::RowPredicate::operator()(uint)
{
    return defaultValue;
}

FENNEL_END_CPPFILE("$Id$");
// End MockProducerExecStream.cpp
