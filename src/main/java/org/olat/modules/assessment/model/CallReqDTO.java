package org.olat.modules.assessment.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;

@XmlRootElement
public class CallReqDTO implements Validatable {

    @XmlElement public String db;
    @XmlElement public String user;
    @XmlElement public String pass;
    @XmlElement public Integer tid;
    @XmlElement public String org;

    public CallReqDTO(String db, String user, String pass, Integer tid, String org) {
        this.db = db;
        this.user = user;
        this.pass = pass;
        this.tid = tid;
        this.org = org;
    }

    public CallReqDTO () {

    }

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

    public Integer getTid() {
        return tid;
    }

    public void setTid(Integer tid) {
        this.tid = tid;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public void validate() {
        if(StringUtils.isEmpty(db) || StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)
        || StringUtils.isEmpty(org))
            throw new IllegalArgumentException("Empty required param.");
    }
}
