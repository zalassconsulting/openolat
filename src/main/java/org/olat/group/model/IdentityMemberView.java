package org.olat.group.model;

import org.olat.basesecurity.IdentityRef;
import org.olat.core.id.Identity;
import org.olat.user.propertyhandlers.UserPropertyHandler;

import java.util.List;
import java.util.Locale;

public class IdentityMemberView extends MemberView {

    private final IdentityRef identityRef;

    public IdentityMemberView(Identity identity, List<UserPropertyHandler> userPropertyHandlers, Locale locale) {
        super(identity, userPropertyHandlers, locale);
        this.identityRef = identity;
    }

    public IdentityRef getIdentityRef() {
        return identityRef;
    }

}
