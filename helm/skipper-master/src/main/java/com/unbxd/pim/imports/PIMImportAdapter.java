package com.unbxd.pim.imports;

import com.unbxd.pim.exception.PIMException;
import com.unbxd.pim.imports.model.ImportProperties;

public interface PIMImportAdapter {

     ImportProperties importProperties(String cookie, String orgId, String importId,
                                       long page, long count) throws PIMException;
}
