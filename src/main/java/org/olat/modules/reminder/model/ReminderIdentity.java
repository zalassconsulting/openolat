package org.olat.modules.reminder.model;

import org.olat.core.id.Identity;
import org.olat.core.id.Persistable;
import org.olat.core.id.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReminderIdentity implements Identity {

    public static final String LECTURE_BLOCK_PROPERTY_KEY = "LECTURE_BLOCK";

    private final Identity wrappedIdentity;
    private final Map<String, Object> reminderProperties = new HashMap<>();

    public ReminderIdentity(Identity wrappedIdentity) {
        this.wrappedIdentity = wrappedIdentity;
    }

    public ReminderIdentity(Identity wrappedIdentity, String reminderPropertyKey, Object reminderPropertyValue) {
        this(wrappedIdentity);
        reminderProperties.put(reminderPropertyKey, reminderPropertyValue);
    }

    @Override
    public String getName() {
        return wrappedIdentity.getName();
    }

    @Override
    public String getExternalId() {
        return wrappedIdentity.getExternalId();
    }

    @Override
    public User getUser() {
        return wrappedIdentity.getUser();
    }

    @Override
    public Date getCreationDate() {
        return wrappedIdentity.getCreationDate();
    }

    @Override
    public Long getKey() {
        return wrappedIdentity.getKey();
    }

    @Override
    public Integer getStatus() {
        return wrappedIdentity.getStatus();
    }

    @Override
    public Date getLastLogin() {
        return wrappedIdentity.getLastLogin();
    }

    @Override
    public Date getPlannedInactivationDate() {
        return wrappedIdentity.getPlannedInactivationDate();
    }

    @Override
    public Date getInactivationDate() {
        return wrappedIdentity.getInactivationDate();
    }

    @Override
    public Date getReactivationDate() {
        return wrappedIdentity.getReactivationDate();
    }

    @Override
    public Date getExpirationDate() {
        return wrappedIdentity.getExpirationDate();
    }

    @Override
    public Date getPlannedDeletionDate() {
        return wrappedIdentity.getPlannedDeletionDate();
    }

    @Override
    public Date getDeletionEmailDate() {
        return wrappedIdentity.getDeletionEmailDate();
    }

    @Override
    public boolean equalsByPersistableKey(Persistable persistable) {
        return wrappedIdentity.equalsByPersistableKey(persistable);
    }

    public final void setReminderProperty(String key, Object value) {
        reminderProperties.put(key, value);
    }

    public final <T> T getReminderProperty(String key, Class<T> type) {
        return type.cast(reminderProperties.get(key));
    }

    public final Identity unwrap() {
    	return wrappedIdentity;
    }

}
