package org.olat.modules.assessment;

import org.olat.core.id.Identity;

public interface MigrationService {

    void callMe(String sourceDB, String user, String pass, String org, Identity idn);

    void callMe(String sourceDB, String user, String pass, String org, Integer tid, Identity idn);

    void migrateMe(String sourceDB, String user, String pass, Long uid, Identity idn);
}
