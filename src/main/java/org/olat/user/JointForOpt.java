package org.olat.user;

public enum JointForOpt {
    E("=", null), NE("!=", null), C("like", "exists"), NC("not like", "not exists");
    private final String val;
    private final String ifExist;

    JointForOpt(String val, String ifExist) {
        this.val = val;
        this.ifExist = ifExist;
    }

    public String resolve() {
        return val;
    }

    public String resExist() {
        return ifExist;
    }
}
