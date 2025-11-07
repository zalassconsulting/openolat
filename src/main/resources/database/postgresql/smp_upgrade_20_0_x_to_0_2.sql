alter table o_rem_reminder
    add column r_sms_enabled boolean default false,
    add column r_sms_content varchar(320);