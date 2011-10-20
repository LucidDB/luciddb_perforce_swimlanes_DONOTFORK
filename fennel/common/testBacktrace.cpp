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

// a primitive, manual, stand-alone test of Backtrace

#include "fennel/common/CommonPreamble.h"
#include "fennel/common/Backtrace.h"
#include "fennel/common/ConfigMap.h"
#include <ostream>
#include <cassert>
#include <stdexcept>

FENNEL_BEGIN_CPPFILE("$Id$");

class foo {
    void g(int);
    void h(int);
    void k(int);
public:
    foo() {}
    ~foo() {}
    void f(int);
};

void foo::f(int n)
{
    g(n);
}

void foo::g(int n)
{
    h(n);
    k(n);
}

void foo::h(int n)
{
    std::cerr << "\ntesting Backtrace(" << n << ")\n";
    Backtrace bt(n);
    std::cerr << bt;
}

void foo::k(int n)
{
    void *addrs[32];
    assert(n < 32);
    std::cerr << "\ntesting Backtrace(" << n << ", addrs)\n";
    Backtrace bt(n, addrs);
    std::cerr << bt;
}

class goo : public AutoBacktrace::Extra {
    // implement AutoBacktrace::Extra
    virtual void printBacktrace(std::ostream& os);
    virtual void finishBacktrace();
};

void goo::finishBacktrace()
{
    std::cerr << "goo has finished" << std::endl;
}

void goo::printBacktrace(std::ostream& os)
{
    os << "Hi, my name is goo" << std::endl;
}


FENNEL_END_CPPFILE("$Id");

// testBacktrace tests Backtrace()
// testBacktrace a tests AutoBacktrace of assert()
// testBacktrace e tests AutoBacktrace of an uncaught exception
// testBacktrace s tests AutoBacktrace of a SIGSEVG
// testBacktrace x tests AutoBacktrace of assert(), with an Extra
int main(int argc, char **argv)
{
    if (argc == 1) {
        fennel::foo o;
        o.f(16);
        o.f(2);
    } else {
        fennel::AutoBacktrace::install();
        switch (argv[1][0]) {
        case 'a':
            assert(false);
            std::cerr << "not reached\n";
            break;
        case 'p':
            permAssert(false);
            std::cerr << "not reached\n";
            break;
        case 'x':
        {
            fennel::goo o;
            fennel::AutoBacktrace::removeExtra(&o);
            fennel::AutoBacktrace::addExtra(&o);
            fennel::AutoBacktrace::removeExtra(&o);
            fennel::AutoBacktrace::addExtra(&o);
            assert(false);
            std::cerr << "not reached\n";
        }
            break;
        case 'e':
            std::cerr
                << "throw new std::runtime_error(\"testing AutoBacktrace\")\n";
            throw new std::runtime_error("testing AutoBacktrace");
            std::cerr << "not reached\n";
            break;
        case 's':
            {
                fennel::ConfigMap configMap;
                configMap.readParams(*(std::istream *) NULL);
            }
            break;
        default:
            ;
        }
    }
    exit(0);
}

// End testBacktrace.cpp
