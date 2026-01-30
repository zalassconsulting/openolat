package org.olat.commons.memberlist.manager;

import org.olat.basesecurity.IdentityRef;
import org.olat.basesecurity.OrganisationRoles;
import org.olat.basesecurity.OrganisationService;
import org.olat.commons.memberlist.model.OrganisationInfo;
import org.olat.core.id.Identity;
import org.olat.core.id.Organisation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrganisationQueryManager {

    @Autowired
    OrganisationService organisationService;

    public Map<Long, OrganisationInfo> getOrganisationInfos(Collection<? extends IdentityRef> identities) {
        Map<Long, OrganisationInfo> result = new LinkedHashMap<>();

        for (IdentityRef identity : identities) {
            organisationService.getOrganisations(identity, OrganisationRoles.user).stream()
                    .findFirst()
                    .ifPresent(o -> result.put(identity.getKey(), new OrganisationInfo(o.getDisplayName())));
        }

        return result.isEmpty() ? null : result;
    }
}
