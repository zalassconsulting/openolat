package org.olat.commons.memberlist.model;

import java.io.Serializable;

public class OrganisationInfo implements Serializable {

    private final String displayName;

    public OrganisationInfo(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
