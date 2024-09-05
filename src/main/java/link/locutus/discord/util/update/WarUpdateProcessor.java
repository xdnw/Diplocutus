package link.locutus.discord.util.update;

import link.locutus.discord.Locutus;
import link.locutus.discord.Logg;
import link.locutus.discord.commands.WarCategory;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.db.conflict.ConflictManager;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.GuildHandler;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.war.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.pnw.RegisteredUser;
import link.locutus.discord.util.*;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.task.war.WarCard;
import com.google.common.eventbus.Subscribe;
import link.locutus.discord.db.entities.AllianceMeta;
import link.locutus.discord.db.entities.CounterStat;
import link.locutus.discord.db.entities.CounterType;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.WarStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WarUpdateProcessor {

    public static void processWars(List<Map.Entry<DBWar, DBWar>> wars, Consumer<Event> eventConsumer) {
        if (wars.isEmpty()) return;

        long start = System.currentTimeMillis();
//
        handleAlerts(wars);
        handleAudits(wars); // TODO (from legacy)

        Locutus.imp().getExecutor().submit(() -> handleWarRooms(wars));

        for (Map.Entry<DBWar, DBWar> entry : wars) {
            DBWar previous = entry.getKey();
            DBWar current = entry.getValue();

            if (eventConsumer != null) {
                if (previous == null) {
                    eventConsumer.accept(new WarCreateEvent(current));
                } else {
                    eventConsumer.accept(new WarStatusChangeEvent(previous, current));
                }
            }

            try {
                processLegacy(previous, current);
            }  catch (Throwable e) {
                e.printStackTrace();
            }
        }
        try {
            WarUpdateProcessor.checkActiveConflicts();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        ConflictManager conflictManager = Locutus.imp().getWarDb().getConflicts();
        if (conflictManager != null) {
            try {
                for (Map.Entry<DBWar, DBWar> entry : wars) {
                    DBWar previous = entry.getKey();
                    DBWar current = entry.getValue();
                    conflictManager.updateWar(previous, current, f -> true);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        long diff = System.currentTimeMillis() - start;
        if (diff > 500) {
            Logg.text("Took " + diff + "ms to process " + wars.size() + " wars");
            AlertUtil.error("Took " + diff + "ms to process " + wars.size() + " wars", StringMan.stacktraceToString(new Exception().getStackTrace()));
        }
    }

    @Subscribe
    public void onWarStatus(WarStatusChangeEvent event) {
        DBWar current = event.getCurrent();
        if (event.getPrevious() != null && current != null) {
            WarStatus status1 = event.getPrevious().getStatus();
            WarStatus status2 = current.getStatus();

//            if (status1 != status2) {
//                boolean isPeace1 = status1 == WarStatus.PEACE || status1 == WarStatus.ATTACKER_OFFERED_PEACE || status1 == WarStatus.DEFENDER_OFFERED_PEACE;
//                boolean isPeace2 = status2 == WarStatus.PEACE || status2 == WarStatus.ATTACKER_OFFERED_PEACE || status2 == WarStatus.DEFENDER_OFFERED_PEACE;
//
//                if (isPeace1 || isPeace2) {
//                    new WarPeaceStatusEvent(event.getPrevious(), event.getCurrent()).post();
//                }
//            }
        }
    }


    public static void handleWarRooms(List<Map.Entry<DBWar, DBWar>> wars) {
        try {
            Map<Integer, Set<WarCategory>> warCatsByAA = new HashMap<>();
            Map<Integer, Set<WarCategory>> warCatsByRoom = new HashMap<>();

            for (GuildDB db : Locutus.imp().getGuildDatabases().values()) {
                if (db.isDelegateServer()) continue;
                WarCategory warCat = db.getWarChannel();
                if (warCat != null) {
                    for (int ally : warCat.getTrackedAllianceIds()) {
                        warCatsByAA.computeIfAbsent(ally, f -> new HashSet<>()).add(warCat);
                    }
                    for (WarCategory.WarRoom room : warCat.getWarRoomMap().values()) {
                        if (room.channel != null && room.target != null) {
                            warCatsByRoom.computeIfAbsent(room.target.getId(), f -> new HashSet<>()).add(warCat);
                        }
                    }
                }
            }



            Set<WarCategory> toUpdate = new LinkedHashSet<>();
            for (Map.Entry<DBWar, DBWar> pair : wars) {
                DBWar current = pair.getValue();

                if (!toUpdate.isEmpty()) {
                    toUpdate.clear();
                }
                toUpdate.addAll(warCatsByAA.getOrDefault(current.getAttacker_aa(), Collections.emptySet()));
                toUpdate.addAll(warCatsByAA.getOrDefault(current.getDefender_aa(), Collections.emptySet()));
                toUpdate.addAll(warCatsByRoom.getOrDefault(current.getAttacker_id(), Collections.emptySet()));
                toUpdate.addAll(warCatsByRoom.getOrDefault(current.getDefender_id(), Collections.emptySet()));
                if (!toUpdate.isEmpty()) {
                    if (wars.size() > 25 && RateLimitUtil.getCurrentUsed() > 55) {
                        while (RateLimitUtil.getCurrentUsed(true) > 55) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                    for (WarCategory warCat : toUpdate) {
                        try {
                            warCat.update(pair.getKey(), pair.getValue());
                        } catch (ErrorResponseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void handleAudits(List<Map.Entry<DBWar, DBWar>> wars) {

    }

    public static void handleAlerts(List<Map.Entry<DBWar, DBWar>> wars) {
        List<DBWar> newWars = wars.stream().filter(f -> f.getKey() == null).map(f -> f.getValue()).collect(Collectors.toList());

        Map<Integer, Set<GuildHandler>> defGuildsByAA = new HashMap<>();
        Map<Integer, Set<GuildHandler>> offGuildsByAA = new HashMap<>();

        for (GuildDB db : Locutus.imp().getGuildDatabases().values()) {
            MessageChannel defChan = db.getOrNull(GuildKey.DEFENSE_WAR_CHANNEL, false);
            MessageChannel offChan = db.getOrNull(GuildKey.OFFENSIVE_WAR_CHANNEL, false);

            if (defChan == null && offChan == null) continue;

            if (!db.isValidAlliance()) {
                if (!db.isWhitelisted() || !db.isOwnerActive()) continue;
            }

            GuildHandler handler = db.getHandler();

            if (defChan != null) {
                Set<Integer> tracked = handler.getTrackedWarAlliances(false);
                for (Integer aaId : tracked) {
                    defGuildsByAA.computeIfAbsent(aaId, f -> new LinkedHashSet<>()).add(handler);
                }
            }
            if (offChan != null) {
                Set<Integer> tracked = handler.getTrackedWarAlliances(true);
                for (Integer aaId : tracked) {
                    offGuildsByAA.computeIfAbsent(aaId, f -> new LinkedHashSet<>()).add(handler);
                }
            }
        }

        Map<GuildHandler, List<Map.Entry<DBWar, DBWar>>> defWarsByGuild = new HashMap<>();
        Map<GuildHandler, List<Map.Entry<DBWar, DBWar>>> offWarsByGuild = new HashMap<>();
        Map<GuildHandler, BiFunction<DBWar, DBWar, Boolean>> shouldAlertWar = new HashMap<>();

        int toCreate = 0;

        for (Map.Entry<DBWar, DBWar> entry : wars) {
            DBWar war = entry.getValue();
            if (war.getAttacker_aa() == 0 && war.getDefender_aa() == 0) continue;
            if (war.getDefender_aa() != 0) {
                for (GuildHandler guildHandler : defGuildsByAA.getOrDefault(war.getDefender_aa(), Collections.emptySet())) {
                    boolean shouldAlert = shouldAlertWar.computeIfAbsent(guildHandler, GuildHandler::shouldAlertWar).apply(entry.getKey(), entry.getValue());
                    if (shouldAlert) {
                        toCreate++;
                        defWarsByGuild.computeIfAbsent(guildHandler, f -> new ArrayList<>()).add(entry);
                    }
                }
            }
            if (war.getAttacker_aa() != 0) {
                for (GuildHandler guildHandler : offGuildsByAA.getOrDefault(war.getAttacker_aa(), Collections.emptySet())) {
                    boolean shouldAlert = shouldAlertWar.computeIfAbsent(guildHandler, GuildHandler::shouldAlertWar).apply(entry.getKey(), entry.getValue());
                    if (shouldAlert) {
                        toCreate++;
                        offWarsByGuild.computeIfAbsent(guildHandler, f -> new ArrayList<>()).add(entry);
                    }
                }
            }
        }

        int free = (RateLimitUtil.getLimitPerMinute() - RateLimitUtil.getCurrentUsed());
        boolean rateLimit = toCreate < (free + 10) * 3;

        for (Map.Entry<GuildHandler, List<Map.Entry<DBWar, DBWar>>> entry : defWarsByGuild.entrySet()) {
            GuildHandler handler = entry.getKey();
            boolean limit = rateLimit || (!handler.getDb().hasAlliance() && (toCreate > 50 || toCreate < free));
            handler.onDefensiveWarAlert(entry.getValue(), limit);
        }

        for (Map.Entry<GuildHandler, List<Map.Entry<DBWar, DBWar>>> entry : offWarsByGuild.entrySet()) {
            GuildHandler handler = entry.getKey();
            boolean limit = rateLimit || (!handler.getDb().hasAlliance() && (toCreate > 50 || toCreate < free));
            handler.onOffensiveWarAlert(entry.getValue(), limit);
        }
    }

    public static void processLegacy(DBWar previous, DBWar current) throws IOException {
        try {
            DBNation attacker = Locutus.imp().getNationDB().getNation(current.getAttacker_id());
            DBNation defender = Locutus.imp().getNationDB().getNation(current.getDefender_id());

            if (defender != null && defender.getAlliance_id() == 0 && defender.active_m() > 10000) return;
            if (previous != null) {
                return;
            }

            if (defender == null || defender.getAlliance_id() == 0) return;

            WarCard card = new WarCard(current.getWarId());
            CounterStat stat = card.getCounterStat();

            DBAlliance defAA = defender.getAlliance();
            DBAlliance attAA = attacker == null ? null : attacker.getAlliance();
            if (defAA == null || attAA == null) return;

            ByteBuffer defWarringBuf = defAA.getMeta(AllianceMeta.IS_WARRING);
            if (defWarringBuf != null && defWarringBuf.get() > 0) return;
            ByteBuffer attWarringBuf = attAA.getMeta(AllianceMeta.IS_WARRING);
            if (attWarringBuf != null && attWarringBuf.get() > 0) return;

            if (stat != null && stat.type == CounterType.ESCALATION) {
                AlertUtil.forEachChannel(f -> true, GuildKey.ESCALATION_ALERTS, new BiConsumer<MessageChannel, GuildDB>() {
                    @Override
                    public void accept(MessageChannel channel, GuildDB guildDB) {
                        card.embed(new DiscordChannelIO(channel), false, true);
                    }
                });
            } else if (defender.getOff() > 0 && (stat == null || stat.type != CounterType.UNCONTESTED)) {
                Set<DBWar> wars = defender.getActiveWars();
                Set<DBWar> escalatedWars = null;
                for (DBWar war : wars) {
                    if (war.getAttacker_id() != defender.getNation_id()) continue;

                    DBNation warDef = DBNation.getById(war.getDefender_id());
                    if (warDef == null || warDef.getPosition() < 1) continue;
                    CounterStat stats = war.getCounterStat();
                    if (stats != null && stats.type == CounterType.IS_COUNTER) {
                        if (escalatedWars == null) escalatedWars = new HashSet<>();
                        escalatedWars.add(war);
                    }
                }
                if (escalatedWars != null && escalatedWars.size() > 2) {
                    AlertUtil.forEachChannel(f -> true, GuildKey.ESCALATION_ALERTS, new BiConsumer<MessageChannel, GuildDB>() {
                        @Override
                        public void accept(MessageChannel channel, GuildDB guildDB) {
                            card.embed(new DiscordChannelIO(channel), false, true);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void checkActiveConflicts() {
        Set<DBWar> activeWars = Locutus.imp().getWarDb().getActiveWars();
        Map<Integer, Set<DBWar>> defWarsByAA = new HashMap<>();

        for (DBWar war : activeWars) {
            DBNation attacker = war.getNation(true);
            DBNation defender = war.getNation(false);
            if (attacker == null || attacker.getAlliance_id() == 0 || attacker.isVacation() || attacker.active_m() > 2880) continue;
            if (defender == null || defender.getAlliance_id() <= 0 || defender.isVacation() || defender.active_m() > 2000 || defender.getAlliance_id() != war.getDefender_aa()) continue;
            int aaId = war.getDefender_aa();
            defWarsByAA.computeIfAbsent(aaId, f -> new HashSet<>()).add(war);
        }

        Map<DBAlliance, Double> warRatio = new HashMap<>();
        for (Map.Entry<Integer, Set<DBWar>> entry : defWarsByAA.entrySet()) {
            DBAlliance alliance = DBAlliance.get(entry.getKey());
            if (alliance == null) continue;
            Set<DBWar> wars = entry.getValue();
            if (wars.size() < 6) continue;
            Set<DBNation> nations = alliance.getNations(f -> f.getAlliance_id() > 0 && f.active_m() <= 2000 && f.isVacation() == false);
            int numBeige = 0;
            int numDefending = 0;
            int numDefendingUnprovoked = 0;
            int numC10Plus = 0;
            for (DBNation nation : nations) {
                if (nation.hasProtection()) numBeige++;
                if (nation.getDef() > 0) numDefending++;
                if (nation.getDef() > 0 && nation.getOff() == 0) numDefendingUnprovoked++;
                if (nation.getLand() >= 10000) numC10Plus++;
            }
            if (numDefending == 0) continue;
            if (numDefending > 4) {
                double ratio = (numDefendingUnprovoked * 5d + numDefending) / Math.max(1, (nations.size() - numBeige));
                warRatio.put(alliance, ratio);
            }
        }

        Map<DBAlliance, Boolean> isAtWar = new HashMap<>();
        Map<DBAlliance, String> warInfo = new HashMap<>();

        for (Map.Entry<DBAlliance, Double> entry : warRatio.entrySet()) {
            DBAlliance alliance = entry.getKey();
            Set<DBNation> nations = alliance.getNations(f -> f.getAlliance_id() > 0 && f.active_m() <= 2000 && f.isVacation() == false);

            Set<DBWar> active = alliance.getActiveWars();
            Map<Integer, Integer> notableByAA = new HashMap<>();
            int max = 0;

            Set<Integer> numAttacking = new HashSet<>();

            outer:
            for (DBWar war : active) {
                int otherAA = war.getAttacker_aa();
                if (otherAA == 0) continue;
                if (war.getDefender_aa() != alliance.getAlliance_id()) continue;

                DBNation attacker = war.getNation(true);
                DBNation defender = war.getNation(false);
                if (attacker == null || defender == null) continue;
                if (!nations.contains(defender)) continue;
                if (defender.active_m() > 2000 || defender.getAlliance_id() <= 0) continue;

                int amt = notableByAA.getOrDefault(otherAA, 0) + 1;
                notableByAA.put(otherAA, amt);
                max = Math.max(max, amt);

                numAttacking.add(attacker.getNation_id());
            }
            if (max >= 6 && numAttacking.size() > 1) {
                StringBuilder body = new StringBuilder();
                body.append("#" + alliance.getRank() + " | " + alliance.getMarkdownUrl() + "\n");
                for (Map.Entry<Integer, Integer> warEntry : notableByAA.entrySet()) {
                    body.append("- " + warEntry.getValue() + " unprovoked wars from " + DNS.getMarkdownUrl(warEntry.getKey(), true) + "\n");
                }
//                double planePct = planes * 100d / MilitaryUnit.AIRCRAFT.getMaxMMRCap(cities, f -> false);
//                body.append("\nPlane % of Attackers: " + MathMan.format(planePct) + " (" + numAttacking.size() + " nations)\n");
                warInfo.put(alliance, body.toString());
                isAtWar.put(alliance, true);
            }
        }

        long currentHour = TimeUtil.getHour();
        long warTurnThresheld = 7 * 24;

        Set<DBAlliance> top = new HashSet<>(isAtWar.keySet());
        top.addAll(Locutus.imp().getNationDB().getAlliances(true, true, true, 80));

        for (DBAlliance alliance : top) {
            ByteBuffer previousPctBuf = alliance.getMeta(AllianceMeta.LAST_BLITZ_PCT);
            ByteBuffer warringBuf = alliance.getMeta(AllianceMeta.IS_WARRING);
            ByteBuffer lastWarBuf = alliance.getMeta(AllianceMeta.LAST_AT_WAR_HOUR);

            long lastWarTurn = lastWarBuf == null ? 0 : lastWarBuf.getLong();
            double lastWarRatio = previousPctBuf == null ? 0 : previousPctBuf.getDouble();
            boolean lastWarring = warringBuf == null ? false : warringBuf.get() == 1;

            double currentRatio = warRatio.getOrDefault(alliance, 0d);
            boolean warring = isAtWar.getOrDefault(alliance, false);
            if (lastWarring && lastWarRatio > 0.2) warring = true;

            alliance.setMeta(AllianceMeta.LAST_BLITZ_PCT, currentRatio);
            alliance.setMeta(AllianceMeta.IS_WARRING, (byte) (warring ? 1 : 0));
            if (warring) alliance.setMeta(AllianceMeta.LAST_AT_WAR_HOUR, currentHour);
            String body = warInfo.get(alliance);

            if (body != null && !lastWarring && warring && currentHour - lastWarTurn > warTurnThresheld) {
                String title = alliance.getName() + " is being Attacked";
                AlertUtil.forEachChannel(f -> true, GuildKey.ESCALATION_ALERTS, new BiConsumer<MessageChannel, GuildDB>() {
                    @Override
                    public void accept(MessageChannel channel, GuildDB guildDB) {
                        DiscordUtil.createEmbedCommand(channel, title, body);
                    }
                });
            }
        }
    }
}