package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import link.locutus.discord.commands.WarCategory;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.discord.DiscordUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.*;

public class CounterGenerator {
    public static List<DBNation> generateCounters(GuildDB db, DBNation enemy, boolean requireOnDiscord, boolean allowAttackersWithMaxOffensives) {
        Set<DBNation> nations = new HashSet<>();
        Guild guild = db.getGuild();
        Role role = Roles.MEMBER.toRole(guild);

        Set<Integer> allies = db.getAllies();
        AllianceList alliance = db.getAllianceList();

        if (requireOnDiscord || alliance == null || alliance.isEmpty() || allies.isEmpty()) {
            if (role == null) throw new IllegalArgumentException("No member role setup");
            List<Member> members = guild.getMembersWithRoles(role);
            for (Member member : members) {
                DBNation nation = DiscordUtil.getNation(member.getUser());
                if (nation != null && (allies == null || allies.contains(nation))) {
                    nations.add(nation);
                }
            }
        } else {
            nations.addAll(alliance.getNations(true, 4880, true));
        }

        return generateCounters(db, enemy, new ArrayList<>(nations), allowAttackersWithMaxOffensives);
    }

    public static List<DBNation> generateCounters(GuildDB db, DBNation enemy, List<DBNation> attackersSorted, boolean allowAttackersWithMaxOffensives) {
        return generateCounters(db, enemy, attackersSorted, allowAttackersWithMaxOffensives, true);
    }

    public static List<DBNation> generateCounters(GuildDB db, DBNation enemy, List<DBNation> attackersSorted, boolean allowAttackersWithMaxOffensives, boolean filter) {
        double totalStr = 0;
        int numWars = 0;
        for (DBWar war : enemy.getWars()) {
            DBNation other = war.getNation(!war.isAttacker(enemy));
            if (other != null) {
                totalStr += Math.pow(other.getWarIndex(), 3);
                numWars++;
                attackersSorted.remove(other);
            }
        }

        WarCategory warCat = db.getWarChannel();

        if (filter) {
            attackersSorted.removeIf(f -> f.getNumWars() > 0 && f.getRelativeStrength() < 1);
            attackersSorted.removeIf(f -> f.getWarIndex() * 1.25 < enemy.getWarIndex());
            attackersSorted.removeIf(f -> {
                double minModifier = f.isInactiveForWar() ? DNS.WAR_RANGE_MIN_MODIFIER_INACTIVE : DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE;
                double maxModifier = f.isInactiveForWar() ? DNS.WAR_RANGE_MAX_MODIFIER_INACTIVE : DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE;
                return f.getScore() * maxModifier < enemy.getScore() || f.getScore() * minModifier > enemy.getScore();
            });
            attackersSorted.removeIf(f -> f.active_m() > 4880);
            attackersSorted.removeIf(f -> f.isVacation());
            attackersSorted.removeIf(f -> f.getPosition() <= 1);
        }

        if (!allowAttackersWithMaxOffensives) attackersSorted.removeIf(f -> f.getOff() >= f.getMaxOff());
        Map<DBNation, Double> factors = new HashMap<>();

        for (DBNation att : attackersSorted) {
            double activeFactor = att.isOnline() ? 1.2 : att.active_m() < 1440 ? 1 : (1 - (att.active_m() - 1440d) / 4880);
            double warFactor = Math.max(0.5, Math.min(3, att.getWarIndex() / enemy.getWarIndex()));

            double maxOffFactor = 1;
            if (allowAttackersWithMaxOffensives && att.getOff() >= att.getMaxOff()) {
                int minReminingTurns = Integer.MAX_VALUE;
                for (DBWar war : att.getActiveWars()) {
                    DBNation defender = war.getNation(false);
                    if (defender == null) {
                        continue;
                    }
                    if (defender.active_m() < 10000 && defender.getWarIndex() > att.getWarIndex() * 0.3) {
                        maxOffFactor = 0;
                        break;
                    }
                    Map.Entry<Double, Double> resistance = war.getResistance();
                    int remainingHours = (int) Math.max(0, ((resistance.getValue()) / 10) * 3);

                    minReminingTurns = Math.min(minReminingTurns, remainingHours);
                }

                if (maxOffFactor == 1 && minReminingTurns != Integer.MAX_VALUE) {
                    maxOffFactor = (24 - minReminingTurns) / 24d;
                }
            }

            double roomFactor = 1;
            if (warCat != null) {
                Set<Integer> inWarRoom = new HashSet<>();
                for (WarCategory.WarRoom room : warCat.getWarRoomMap().values()) {
                    if (room.isParticipant(att, false) && room.target.active_m() < 2880) {
                        inWarRoom.add(room.target.getNation_id());
                    }
                }

                if (!inWarRoom.isEmpty()) {
                    for (DBWar war : att.getActiveWars()) {
                        inWarRoom.remove(war.getDefender_id());
                    }

                    if (!inWarRoom.isEmpty()) {
                        roomFactor = roomFactor * 1 * Math.pow(0.8, inWarRoom.size());
                    }
                }
            }

            factors.put(att, activeFactor * warFactor * att.getWarIndex() * maxOffFactor * roomFactor);
        }

        attackersSorted.sort((o1, o2) -> Double.compare(factors.get(o2), factors.get(o1)));

        if (attackersSorted.size() > 0 && filter) {
            for (int i = 0; i < Math.min(3, attackersSorted.size()); i++) {
                DBNation att = attackersSorted.get(i);
                totalStr += Math.pow(att.getWarIndex(), 3);
                numWars++;
            }
            totalStr = Math.pow(totalStr / numWars, 1 / 3d);
            if (totalStr < enemy.getWarIndex()) {
                return Collections.emptyList();
            }
        }

        return attackersSorted;
    }
}
