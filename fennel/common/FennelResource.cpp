// This class is generated. Do NOT modify it, or
// add it to source control.

/**
 * This class was generated
 * by class mondrian.resource.ResourceGen
 * from /home/jvs/open/dev/fennel/common/FennelResource.xml
 * on Sat Nov 27 23:31:08 PST 2004.
 * It contains a list of messages, and methods to
 * retrieve and format those messages.
 **/

// begin common include specified by /home/jvs/open/dev/fennel/common/FennelResource.xml
#include "CommonPreamble.h"
// end common include specified by /home/jvs/open/dev/fennel/common/FennelResource.xml
#include "FennelResource.h"
#include "ResourceBundle.h"
#include "Locale.h"

#include <map>
#include <string>

namespace fennel {

using namespace std;

#define BASENAME ("FennelResource")

static FennelResourceBundleCache bundleCache;
static string bundleLocation("");

const FennelResource &FennelResource::instance()
{
    return FennelResource::instance(Locale::getDefault());
}

const FennelResource &FennelResource::instance(const Locale &locale)
{
    return *makeInstance<FennelResource, FennelResourceBundleCache, FennelResourceBundleCache::iterator>(bundleCache, locale);
}

void FennelResource::setResourceFileLocation(const string &location)
{
    bundleLocation = location;
}

FennelResource::FennelResource(Locale locale)
    : ResourceBundle(BASENAME, locale, bundleLocation),
      _sysCallFailed(this, "sysCallFailed"),
      _duplicateKeyDetected(this, "duplicateKeyDetected"),
      _internalError(this, "internalError"),
      _executionAborted(this, "executionAborted"),
      _rowTooLong(this, "rowTooLong")
{ }

string FennelResource::sysCallFailed(const std::string &p0) const
{
    return _sysCallFailed.format(p0);
}
string FennelResource::duplicateKeyDetected(const std::string &p0) const
{
    return _duplicateKeyDetected.format(p0);
}
string FennelResource::internalError(const std::string &p0) const
{
    return _internalError.format(p0);
}
string FennelResource::executionAborted() const
{
    return _executionAborted.format();
}
string FennelResource::rowTooLong(int p0, int p1, const std::string &p2) const
{
    return _rowTooLong.format(p0, p1, p2);
}

} // end namespace fennel
