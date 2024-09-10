package link.locutus.discord.util.task.war;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.v2.command.CommandBehavior;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.CounterStat;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.WarStatus;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.util.discord.DiscordUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class WarCard {
    private final int warId;
    public CounterStat counterStat;
    public double attackerResistance;
    public double defenderResistance;

    private DBWar war;
    private String warReason;

    public WarCard(DBWar war, boolean checkCounters) {
        this.warId = war.getWarId();
        update(war, checkCounters);
    }

    public WarCard(int warId) {
        this.warId = warId;
        this.war = Locutus.imp().getWarDb().getWar(warId);
        update(war);
    }

    public String condensedSubInfo(boolean attacker) {
        StringBuilder attStr = new StringBuilder();
        int nation_id = attacker ? this.war.getAttacker_id() : this.war.getDefender_id();
        if (war.getStatus() == (!attacker ? WarStatus.DEFENDER_OFFERED_PEACE : WarStatus.ATTACKER_OFFERED_PEACE)) {
            attStr.append("\u2764");
        }
        attStr.append((attacker ? attackerResistance : defenderResistance) + "%");
        return attStr.toString();
    }

    public void update(DBWar war) {
        update(war, true);
    }

    public void update(DBWar war, boolean checkCounters) {
        this.war = war;
        // TODO
        Map.Entry<Double, Double> res = war.getResistance();
        attackerResistance = res.getKey();
        defenderResistance = res.getValue();
        if (checkCounters) updateCounterStats();
    }

    public String getTitle() {
        String title = String.format("%s > %s- %s- %s",
                DNS.getName(war.getAttacker_id(), false),
                DNS.getName(war.getDefender_id(), false),
                war.getWarType(),
                war.getStatus()
        );
        return title;
    }

    public String getDescription() {
        return getDescription(false);
    }

    public String getDescription(boolean addReactions) {
        StringBuilder description = new StringBuilder();

        if (counterStat != null) {
            switch (counterStat.type) {
                case UNCONTESTED:
                    break;
                case GETS_COUNTERED:
                    description.append("**This has been countered**\n");
                    break;
                case IS_COUNTER:
                    description.append("**This is a counter**\n");
                    break;
                case ESCALATION:
                    description.append("**This is an escalation**\n");
                    break;
            }
        }

        String warUrl = "" + Settings.INSTANCE.DNS_URL() + "/nation/war/timeline/war=" + warId;
        String warReason = this.warReason == null ? "Click here" : this.warReason;
        description.append("Link: [\"" + warReason + "\"\n](" + warUrl + ")");

        description.append(formatNation(true));
        description.append(formatNation(false));

        if (war.isActive()) {
            long end = war.possibleEndDate();
            description.append("End: " + DiscordUtil.timestamp(end, null));
        }

        if (addReactions) {
            description.append("\n\n");
            description.append("Press `" + cmdEmoji + "` to refresh\n");
//            description.append("Press `" + simEmoji + "` to simulate\n");
            description.append("Press `" + counterEmoji + "` to find counters\n");
            description.append("Press `" + spyEmoji + "` to find spyops\n");
        }
        return description.toString();
    }

    public void updateCounterStats() {
        this.counterStat = Locutus.imp().getWarDb().getCounterStat(war);
    }

    public void update() {
        updateCounterStats();
        update(Locutus.imp().getWarDb().getWar(warId));
    }

    public CounterStat getCounterStat() {
        return counterStat;
    }

    private static final String cmdEmoji = "War Info";
    private static final String simEmoji = "Simulate";
    private static final String counterEmoji = "Counter";
    public static final String spyEmoji = "Spies";

    public void embed(IMessageIO channel, boolean addReactions, boolean condense) {
        embed(channel.create(), addReactions, condense, true);
    }

    public IMessageBuilder embed(IMessageBuilder builder, boolean addReactions, boolean condense, boolean send) {
        String warUrl = "" + Settings.INSTANCE.DNS_URL() + "/nation/war/timeline/war=" + warId;
        // TODO FIXME :||remove
//        CommandRef cmd = CM.war.card.cmd.warId(warId + "");
//        CommandRef counter = CM.war.counter.url.cmd.war(warUrl);
//        CommandRef counterSpy = CM.spy.counter.cmd.enemy(war.getAttacker_id() + "").operations("*");
//
//        String pendingEmoji = "Claim";
//        CommandRef pending = CM.embed.update.cmd.desc("{description}\nAssigned to {usermention} in {timediff}").requiredRole(Roles.MILCOM.name());

        IMessageBuilder msg;
        if (addReactions) {
            String desc = getDescription();
            // TODO FIXME :||remove
//            desc += "\n\nPress `" + pendingEmoji + "` to assign";
            msg = builder.embed(getTitle(), desc);// TODO FIXME :||remove
//                    .commandButton(CommandBehavior.DELETE_PRESSED_BUTTON, pending, pendingEmoji)
//                    .commandButton(CommandBehavior.UNPRESS, cmd, cmdEmoji)
//                    .commandButton(CommandBehavior.UNPRESS, counter, counterEmoji)
//                    .commandButton(CommandBehavior.UNPRESS, counterSpy, spyEmoji);
        } else {
            msg = builder.embed(getTitle(), getDescription());
        }
        if (send) {
            if (condense) {
                msg.sendWhenFree();
            } else {
                msg.send();
            }
        }
        return msg;
    }

    private String getSquare(int resistance) {
        if (resistance > 80) {
            return "\uD83D\uDFE9";
        }
        if (resistance > 65) {
            return "\uD83D\uDFE8";
        }
        if (resistance > 30) {
            return "\uD83D\uDFE7";
        }
        return "\uD83D\uDFE5";
    }

    private String formatNation(boolean attacker) {
        String nationFormat = "[%s](%s)- [%s](" + Settings.INSTANCE.DNS_URL() + "/alliance/%s)- %s- %s- %s\n" + // name - alliance - active
                "%s " +
                "%s " +
                "**Resistance**:\n" +
                "%s";

        String peaceSym = "\uD83D\uDD4A";

        int nationId = attacker ? war.getAttacker_id() : war.getDefender_id();
        int otherId = attacker ? war.getDefender_id() : war.getAttacker_id();

        String control = "";
        if (war.getStatus() == (attacker ? WarStatus.ATTACKER_OFFERED_PEACE : WarStatus.DEFENDER_OFFERED_PEACE)) {
            control += peaceSym;
        }

        int resistance = (int) Math.round(attacker ? attackerResistance : defenderResistance);
        String resBar = StringMan.repeat(getSquare(resistance), (resistance + 9) / 10);
        resBar = resBar + ("(" + resistance + "/100)");

        int allianceId = attacker ? war.getAttacker_aa() : war.getDefender_aa();
        String alliance = DNS.getName(allianceId, true);

        DBNation nation = Locutus.imp().getNationDB().getNation(nationId);
        String active_m = "";
        String markdown1 = "";
        String markdown2 = "";
        Rank rank = Rank.NONE;

        if (nation != null) {
            active_m = TimeUtil.secToTime(TimeUnit.MINUTES, nation.active_m());
            markdown1 = nation.toMarkdown(false, true, false);
            markdown2 = nation.toMarkdown(false, false, true);
            rank = nation.getPositionEnum();
        }

        return String.format(nationFormat,
                DNS.getName(nationId, false),
                DNS.getUrl(nationId, false),
                alliance,
                allianceId,
                control,
                active_m,
                rank,
                markdown1,
                markdown2,
                resBar
        );
    }

    public DBWar getWar() {
        return war;
    }

    public boolean isActive() {
        if (war.getStatus() != WarStatus.ACTIVE && war.getStatus() != WarStatus.DEFENDER_OFFERED_PEACE && war.getStatus() != WarStatus.ATTACKER_OFFERED_PEACE) {
            return false;
        }
        if (attackerResistance > 0 && defenderResistance > 0) {
            return System.currentTimeMillis() < war.possibleEndDate();
        }
        return false;
    }
}
