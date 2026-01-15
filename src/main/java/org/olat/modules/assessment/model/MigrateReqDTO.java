package org.olat.modules.assessment.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;

@XmlRootElement(name = "migrate")
public class MigrateReqDTO implements Validatable {

    @XmlElement public String db;
    @XmlElement public String user;
    @XmlElement public String pass;
    @XmlElement public Long uid;

    public MigrateReqDTO(String db, String user, String pass, Long uid) {
        this.uid = uid;
        this.db = db;
        this.user = user;
        this.pass = pass;
    }

    public MigrateReqDTO() {}

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public void validate() {
        if(StringUtils.isEmpty(db) || StringUtils.isEmpty(user) || StringUtils.isEmpty(pass) || uid == null)
            throw new IllegalArgumentException("Empty required param.");
    }
}
