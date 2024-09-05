package link.locutus.discord.util;

import java.util.Map;

public enum AutoAuditType {
    INACTIVE("Please remember to login every day to deter raiders and collect the login bonus"),

        ;


    public final String message;

    AutoAuditType(String msg) {
        this.message = msg;
    }

    public Map.Entry<AutoAuditType, String> toPair() {
        return Map.entry(this, this.message);
    }
}
