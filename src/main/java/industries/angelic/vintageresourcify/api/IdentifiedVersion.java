package industries.angelic.vintageresourcify.api;

import industries.angelic.vintageresourcify.services.IVersion;

/** Optional stable remote file identity, independent of expiring URLs and display names. */
public interface IdentifiedVersion extends IVersion {

    String getFileId();
}
