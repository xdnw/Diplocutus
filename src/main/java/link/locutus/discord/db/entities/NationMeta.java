package link.locutus.discord.db.entities;

import link.locutus.discord.api.types.Rank;
import link.locutus.discord.util.MathMan;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Predicate;

public enum NationMeta {
    INTERVIEW_DEPOSITS,

    INTERVIEW_WAR_ROOM,
    INTERVIEW_CHECKUP,

    INTERVIEW_INDEX,

    SPY_OPS_DAY(ByteBuffer::getLong),
    SPY_OPS_AMOUNT_DAY,
    SPY_OPS_AMOUNT_TOTAL,

    DISCORD_APPLICANT,

    BORGMAS,

    INTERVIEW_TRANSFER_SELF,

    BANKER_WITHDRAW_LIMIT,

    REFERRER,

    INCENTIVE_REFERRER,

    INCENTIVE_INTERVIEWER,

    IA_CATEGORY_MAX_STAGE,

    INCENTIVE_MENTOR,

    CURRENT_MENTOR,

    PROTECTION_ALERT_MODE,

    PROTECTION_ALERT_REQUIRED_STATUS,

    PROTECTION_ALERT_REQUIRED_LOOT,

    PROTECTION_ALERT_SCORE_LEEWAY,

    RECRUIT_AD_COUNT,

    LOGIN_NOTIFY,

    GPT_PROVIDER,

    GPT_OPTIONS,

    REPORT_BAN,

    GPT_MODERATED,

    GPT_SOURCES,

    BANK_TRANSFER_REQUIRED_AMOUNT,

    BANK_TRANSFER_LAST_UPDATED,

    ;

    public static NationMeta[] values = values();

    private final Function<ByteBuffer, Object> parse;

    NationMeta(Function<ByteBuffer, Object> parse) {
        this.parse = parse;
    }

    NationMeta() {
        this(null);
    }

    public String toString(ByteBuffer buf) {
        if (buf == null) return "";
        if (parse != null) return "" + parse.apply(buf);

        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        buf = ByteBuffer.wrap(arr);
        switch (arr.length) {
            case 0:
                return "" + (buf.get() & 0xFF);
            case 4:
                return "" + (buf.getInt());
            case 8:
                ByteBuffer buf2 = ByteBuffer.wrap(arr);
                return buf.getLong() + "/" + MathMan.format(buf2.getDouble());
            default:
                return new String(arr, StandardCharsets.ISO_8859_1);
        }
    }

    public enum BeigeAlertRequiredStatus {
        ONLINE(f -> f.getOnlineStatus() == OnlineStatus.ONLINE),
        ONLINE_AWAY(f -> {
            OnlineStatus status = f.getOnlineStatus();
            return status == OnlineStatus.ONLINE || status == OnlineStatus.IDLE;
        }),
        ONLINE_AWAY_DND(f -> {
            OnlineStatus status = f.getOnlineStatus();
            return status == OnlineStatus.ONLINE || status == OnlineStatus.IDLE || status == OnlineStatus.DO_NOT_DISTURB;
        }),
        ANY(f -> true)
        ;

        private final Predicate<Member> applies;

        BeigeAlertRequiredStatus(Predicate<Member> applies) {
            this.applies = applies;
        }

        public Predicate<Member> getApplies() {
            return applies;
        }
    }

    public enum ProtectionAlertMode {
        NO_ALERTS(f -> false),
        INACTIVE_NONES(f -> f.active_m() > 10000 && f.getAlliance_id() == 0),
        NONES(f -> f.getAlliance_id() == 0),
        NONES_INACTIVE_APPS(f -> (f.getAlliance_id() == 0 || (f.active_m() > 10000 && f.getPosition() <= Rank.APPLICANT.id))),
        ANYONE_NOT_BLACKLISTED(f -> true)

        ;

        private final Predicate<DBNation> isAllowed;

        ProtectionAlertMode(Predicate<DBNation> isAllowed) {
            this.isAllowed = isAllowed;
        }

        public Predicate<DBNation> getIsAllowed() {
            return isAllowed;
        }
    }

}
