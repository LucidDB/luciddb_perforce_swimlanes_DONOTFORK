/*
// $Id$
// Fennel is a library of data storage and processing components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 1999 John V. Sichi
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

#ifndef Fennel_SynchObj_Included
#define Fennel_SynchObj_Included

#include <boost/thread/mutex.hpp>
#include <boost/thread/recursive_mutex.hpp>
#include <boost/thread/shared_mutex.hpp>
#include <boost/thread/xtime.hpp>
#include <boost/thread/condition.hpp>

FENNEL_BEGIN_NAMESPACE

typedef boost::mutex StrictMutex;
typedef boost::recursive_mutex RecursiveMutex;
typedef boost::shared_mutex SharedMutex;
typedef boost::mutex::scoped_lock StrictMutexGuard;
typedef boost::recursive_mutex::scoped_lock RecursiveMutexGuard;
typedef boost::shared_lock<boost::shared_mutex> SharedMutexGuard;
typedef boost::unique_lock<boost::shared_mutex> ExclusiveMutexGuard;
typedef boost::condition_variable LocalCondition;

extern void FENNEL_SYNCH_EXPORT convertTimeout(uint iMillis, boost::xtime &);

FENNEL_END_NAMESPACE

#endif

// End SynchObj.h
