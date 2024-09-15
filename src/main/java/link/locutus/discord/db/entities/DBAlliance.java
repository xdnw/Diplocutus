package link.locutus.discord.db.entities;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.generated.*;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.binding.annotation.Default;
import link.locutus.discord.commands.manager.v2.binding.annotation.NoFormat;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.NationAttributeDouble;
import link.locutus.discord.commands.manager.v2.builder.RankBuilder;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.BankDB;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.NationDB;
import link.locutus.discord.db.SQLUtil;
import link.locutus.discord.db.entities.components.AlliancePrivate;
import link.locutus.discord.db.entities.components.NationPrivate;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.alliance.*;
import link.locutus.discord.event.nation.NationChangePositionEvent;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.pnw.GuildOrAlliance;
import link.locutus.discord.pnw.NationList;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.pnw.SimpleNationList;
import link.locutus.discord.util.FileUtil;
import link.locutus.discord.util.MarkupUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.offshore.Auth;
import link.locutus.discord.util.scheduler.TriConsumer;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.stream.Collectors;

public class DBAlliance implements NationList, NationOrAlliance, GuildOrAlliance, DBEntity<Alliance, DBAlliance> {
    private int AllianceId;
    private String AllianceName;
    private int LeaderId;
    private int MemberCount;
    private long CreationDate;
    private double Score;
    private double AllianceIncome;
    private double AllianceMineralIncome;
    private double AllianceUraniumIncome;
    private double AllianceProductionIncome;
    private double AllianceRareMetalIncome;
    private double TotalLand;
    private double TotalPopulation;
    private int TotalProjects;
    private double AverageWarIndex;
    private double AverageStabilityIndex;
    private double AverageTechIndex;
    private double AverageEducationIndex;
    private double AverageCommerceIndex;
    private double AverageEmploymentIndex;
    private double AveragePowerIndex;
    private int TotalWarVictorys;
    private int TotalWarDefeats;
    private double AverageTransportationIndex;
    private double AllianceFuelIncome;
    private double AllianceTechIncome;

    // cache

    private AlliancePrivate privateData;
    private Int2ObjectOpenHashMap<byte[]> metaCache = null;

    public DBAlliance() {
        this.AllianceName = "";
        CreationDate = System.currentTimeMillis();
    }

    @Override
    public String getTableName() {
        return "alliances";
    }

    @Override
    public boolean update(Alliance alliance, Consumer<Event> eventConsumer) {
        return update(Locutus.imp().getNationDB(), alliance, eventConsumer);
    }

    @Command(desc = "Alliance revenue")
    public Map<ResourceType, Double> getRevenue() {
        Map<ResourceType, Double> revenue = new LinkedHashMap<>();
        revenue.put(ResourceType.CASH, AllianceIncome);
        revenue.put(ResourceType.MINERALS, AllianceMineralIncome);
        revenue.put(ResourceType.URANIUM, AllianceUraniumIncome);
        revenue.put(ResourceType.PRODUCTION, AllianceProductionIncome);
        revenue.put(ResourceType.RARE_METALS, AllianceRareMetalIncome);
        revenue.put(ResourceType.FUEL, AllianceFuelIncome);
        revenue.put(ResourceType.TECHNOLOGY, AllianceTechIncome);
        return revenue;
    }

    public boolean update(NationDB db, Alliance alliance, Consumer<Event> eventConsumer) {
        DBAlliance copy = null;
        if (alliance.AllianceId != null && this.AllianceId != alliance.AllianceId) {
            copy = this.copy();
            this.AllianceId = alliance.AllianceId;
        }
        if (alliance.CreationDate != null && this.CreationDate != alliance.CreationDate.getTime()) {
            copy = this.copy();
            this.CreationDate = alliance.CreationDate.getTime();
        }
//        public String AllianceName;
        if (alliance.AllianceName != null && !alliance.AllianceName.equals(AllianceName)) {
            copy = this.copy();
            this.AllianceName = alliance.AllianceName;
        }
//        public String LeaderName;
        if (alliance.LeaderName != null) {
            DBNation leader = db.getNationByName(alliance.LeaderName);
            if (leader != null && leader.getId() != LeaderId) {
                copy = this.copy();
                this.LeaderId = leader.getId();
                DBNation nation = this.LeaderId <= 0 ? null : db.getNation(this.LeaderId);
                if (nation != null) {
                    if (nation.getAlliance_id() != AllianceId || nation.getPositionEnum() != Rank.LEADER) {
                        DBNation nationCopy = nation.copy();
                        nation.setAlliance_id(AllianceId);
                        nation.setPosition(Rank.LEADER);
                        if (eventConsumer != null) eventConsumer.accept(new NationChangePositionEvent(nationCopy, nation));
                    }
                }
            }
        }
//        public Integer MemberCount;
        if (alliance.MemberCount != null && this.MemberCount != alliance.MemberCount) {
            copy = this.copy();
            this.MemberCount = alliance.MemberCount;
        }
//        public Double Score;
        if (alliance.Score != null && this.Score != alliance.Score) {
            copy = this.copy();
            this.Score = alliance.Score;
        }
//        public Double AllianceIncome;
        if (alliance.AllianceIncome != null && this.AllianceIncome != alliance.AllianceIncome) {
            copy = this.copy();
            this.AllianceIncome = alliance.AllianceIncome;
        }
//        public Double AllianceMineralIncome;
        if (alliance.AllianceMineralIncome != null && this.AllianceMineralIncome != alliance.AllianceMineralIncome) {
            copy = this.copy();
            this.AllianceMineralIncome = alliance.AllianceMineralIncome;
        }
//        public Double AllianceUraniumIncome;
        if (alliance.AllianceUraniumIncome != null && this.AllianceUraniumIncome != alliance.AllianceUraniumIncome) {
            copy = this.copy();
            this.AllianceUraniumIncome = alliance.AllianceUraniumIncome;
        }
//        public Double AllianceProductionIncome;
        if (alliance.AllianceProductionIncome != null && this.AllianceProductionIncome != alliance.AllianceProductionIncome) {
            copy = this.copy();
            this.AllianceProductionIncome = alliance.AllianceProductionIncome;
        }
//        public Double AllianceRareMetalIncome;
        if (alliance.AllianceRareMetalIncome != null && this.AllianceRareMetalIncome != alliance.AllianceRareMetalIncome) {
            copy = this.copy();
            this.AllianceRareMetalIncome = alliance.AllianceRareMetalIncome;
        }
//        public Double TotalLand;
        if (alliance.TotalLand != null && this.TotalLand != alliance.TotalLand) {
            copy = this.copy();
            this.TotalLand = alliance.TotalLand;
        }
//        public Double TotalPopulation;
        if (alliance.TotalPopulation != null && this.TotalPopulation != alliance.TotalPopulation) {
            copy = this.copy();
            this.TotalPopulation = alliance.TotalPopulation;
        }
//        public Integer TotalProjects;
        if (alliance.TotalProjects != null && this.TotalProjects != alliance.TotalProjects) {
            copy = this.copy();
            this.TotalProjects = alliance.TotalProjects;
        }
//        public Double AverageWarIndex;
        if (alliance.AverageWarIndex != null && this.AverageWarIndex != alliance.AverageWarIndex) {
            copy = this.copy();
            this.AverageWarIndex = alliance.AverageWarIndex;
        }
//        public Double AverageStabilityIndex;
        if (alliance.AverageStabilityIndex != null && this.AverageStabilityIndex != alliance.AverageStabilityIndex) {
            copy = this.copy();
            this.AverageStabilityIndex = alliance.AverageStabilityIndex;
        }
//        public Double AverageTechIndex;
        if (alliance.AverageTechIndex != null && this.AverageTechIndex != alliance.AverageTechIndex) {
            copy = this.copy();
            this.AverageTechIndex = alliance.AverageTechIndex;
        }
//        public Double AverageEducationIndex;
        if (alliance.AverageEducationIndex != null && this.AverageEducationIndex != alliance.AverageEducationIndex) {
            copy = this.copy();
            this.AverageEducationIndex = alliance.AverageEducationIndex;
        }
//        public Double AverageCommerceIndex;
        if (alliance.AverageCommerceIndex != null && this.AverageCommerceIndex != alliance.AverageCommerceIndex) {
            copy = this.copy();
            this.AverageCommerceIndex = alliance.AverageCommerceIndex;
        }
//        public Double AverageEmploymentIndex;
        if (alliance.AverageEmploymentIndex != null && this.AverageEmploymentIndex != alliance.AverageEmploymentIndex) {
            copy = this.copy();
            this.AverageEmploymentIndex = alliance.AverageEmploymentIndex;
        }
//        public Double AveragePowerIndex;
        if (alliance.AveragePowerIndex != null && this.AveragePowerIndex != alliance.AveragePowerIndex) {
            copy = this.copy();
            this.AveragePowerIndex = alliance.AveragePowerIndex;
        }
//        public Integer TotalWarVictorys;
        if (alliance.TotalWarVictorys != null && this.TotalWarVictorys != alliance.TotalWarVictorys) {
            copy = this.copy();
            this.TotalWarVictorys = alliance.TotalWarVictorys;
        }
//        public Integer TotalWarDefeats;
        if (alliance.TotalWarDefeats != null && this.TotalWarDefeats != alliance.TotalWarDefeats) {
            copy = this.copy();
            this.TotalWarDefeats = alliance.TotalWarDefeats;
        }
//        public Double AverageTransportationIndex;
        if (alliance.AverageTransportationIndex != null && this.AverageTransportationIndex != alliance.AverageTransportationIndex) {
            copy = this.copy();
            this.AverageTransportationIndex = alliance.AverageTransportationIndex;
        }
//        public Double AllianceFuelIncome;
        if (alliance.AllianceFuelIncome != null && this.AllianceFuelIncome != alliance.AllianceFuelIncome) {
            copy = this.copy();
            this.AllianceFuelIncome = alliance.AllianceFuelIncome;
        }
//        public Double AllianceTechIncome;
        if (alliance.AllianceTechIncome != null && this.AllianceTechIncome != alliance.AllianceTechIncome) {
            copy = this.copy();
            this.AllianceTechIncome = alliance.AllianceTechIncome;
        }
        if (copy != null && eventConsumer != null) {
            eventConsumer.accept(new AllianceUpdateEvent(copy, this));
        }
        return copy != null;
    }

    @Override
    public Object[] write() {
        return new Object[] {
            AllianceId,
            AllianceName,
            LeaderId,
            MemberCount,
            CreationDate,
            Score,
            AllianceIncome,
            AllianceMineralIncome,
            AllianceUraniumIncome,
            AllianceProductionIncome,
            AllianceRareMetalIncome,
            TotalLand,
            TotalPopulation,
            TotalProjects,
            AverageWarIndex,
            AverageStabilityIndex,
            AverageTechIndex,
            AverageEducationIndex,
            AverageCommerceIndex,
            AverageEmploymentIndex,
            AveragePowerIndex,
            TotalWarVictorys,
            TotalWarDefeats,
            AverageTransportationIndex,
            AllianceFuelIncome,
            AllianceTechIncome
        };
    }

    @Override
    public void load(Object[] raw) {
        AllianceId = (int) raw[0];
        AllianceName = (String) raw[1];
        LeaderId = (int) raw[2];
        MemberCount = (int) raw[3];
        CreationDate = SQLUtil.castLong(raw[4]);
        Score = (double) raw[5];
        AllianceIncome = (double) raw[6];
        AllianceMineralIncome = (double) raw[7];
        AllianceUraniumIncome = (double) raw[8];
        AllianceProductionIncome = (double) raw[9];
        AllianceRareMetalIncome = (double) raw[10];
        TotalLand = (double) raw[11];
        TotalPopulation = (double) raw[12];
        TotalProjects = (int) raw[13];
        AverageWarIndex = (double) raw[14];
        AverageStabilityIndex = (double) raw[15];
        AverageTechIndex = (double) raw[16];
        AverageEducationIndex = (double) raw[17];
        AverageCommerceIndex = (double) raw[18];
        AverageEmploymentIndex = (double) raw[19];
        AveragePowerIndex = (double) raw[20];
        TotalWarVictorys = (int) raw[21];
        TotalWarDefeats = (int) raw[22];
        AverageTransportationIndex = (double) raw[23];
        AllianceFuelIncome = (double) raw[24];
        AllianceTechIncome = (double) raw[25];
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        types.put("AllianceId", Integer.class);
        types.put("AllianceName", String.class);
        types.put("LeaderId", Integer.class);
        types.put("MemberCount", Integer.class);
        types.put("CreationDate", Long.class);
        types.put("Score", Double.class);
        types.put("AllianceIncome", Double.class);
        types.put("AllianceMineralIncome", Double.class);
        types.put("AllianceUraniumIncome", Double.class);
        types.put("AllianceProductionIncome", Double.class);
        types.put("AllianceRareMetalIncome", Double.class);
        types.put("TotalLand", Double.class);
        types.put("TotalPopulation", Double.class);
        types.put("TotalProjects", Integer.class);
        types.put("AverageWarIndex", Double.class);
        types.put("AverageStabilityIndex", Double.class);
        types.put("AverageTechIndex", Double.class);
        types.put("AverageEducationIndex", Double.class);
        types.put("AverageCommerceIndex", Double.class);
        types.put("AverageEmploymentIndex", Double.class);
        types.put("AveragePowerIndex", Double.class);
        types.put("TotalWarVictorys", Integer.class);
        types.put("TotalWarDefeats", Integer.class);
        types.put("AverageTransportationIndex", Double.class);
        types.put("AllianceFuelIncome", Double.class);
        types.put("AllianceTechIncome", Double.class);
        return types;
    }

    @Override
    public DBAlliance emptyInstance() {
        return new DBAlliance();
    }

    // legacy junk

    @Override
    public boolean isValid() {
        return get(AllianceId) != null;
    }

    @Override
    public AllianceList toAllianceList() {
        return new AllianceList(AllianceId);
    }

    @Override
    public String getFilter() {
        return getTypePrefix() + ":" + AllianceId;
    }

    @Override
    public boolean test(DBNation dbNation) {
        return dbNation.getAlliance_id() == AllianceId;
    }

    public static DBAlliance parse(String arg, boolean throwError) {
        Integer id = DNS.parseAllianceId(arg);
        if (id == null) {
            if (throwError) throw new IllegalArgumentException("Invalid alliance id: `" + arg + "`");
            return null;
        }
        DBAlliance alliance = DBAlliance.get(id);
        if (alliance == null) {
            if (throwError) throw new IllegalArgumentException("No alliance found for id: `" + id + "`");
        }
        return alliance;
    }

    @Command
    public long getDateCreated() {
        return CreationDate;
    }

    public static DBAlliance get(int aaId) {
        return Locutus.imp().getNationDB().getAlliance(aaId);
    }

    public static DBAlliance getOrCreate(int aaId) {
        if (aaId == 0) return new DBAlliance();
        return Locutus.imp().getNationDB().getOrCreateAlliance(aaId);
    }

    @Command(desc = "Number of offensive and defensive wars since date")
    public int getNumWarsSince(long date) {
        return Locutus.imp().getWarDb().countWarsByAlliance(AllianceId, date);
    }

    public static Set<DBAlliance> getTopX(int topX, boolean checkTreaty) {
        Set<DBAlliance> results = new LinkedHashSet<>();
        Map<Integer, Double> aas = new RankBuilder<>(Locutus.imp().getNationDB().getNations().values()).group(DBNation::getAlliance_id).sumValues(DBNation::getScore).sort().get();
        for (Map.Entry<Integer, Double> entry : aas.entrySet()) {
            if (entry.getKey() == 0) continue;
            if (topX-- <= 0) break;
            int allianceId = entry.getKey();
            results.add(DBAlliance.getOrCreate(allianceId));
            if (checkTreaty) {
                Map<Integer, Treaty> treaties = Locutus.imp().getNationDB().getTreaties(allianceId);
                for (Map.Entry<Integer, Treaty> aaTreatyEntry : treaties.entrySet()) {
                    if (aaTreatyEntry.getValue().getType().isMandatoryDefensive()) {
                        results.add(DBAlliance.getOrCreate(aaTreatyEntry.getKey()));
                    }
                }
            }
        }
        return results;
    }

    public String toMarkdown() {
        StringBuilder body = new StringBuilder();
        // `#id` | Alliance urlMakrup / acronym (linked)
        body.append("`AA:").append(AllianceId).append("` | ").append(getMarkdownUrl());
        body.append(" | `#").append(getRank()).append("`").append("\n");

        String prefix = "";
        if (!prefix.isEmpty()) {
            body.append("\n");
        }
        body.append("```\n");
        // Number of members / applicants (active past day)
        Set<DBNation> nations = getNations();
        Set<DBNation> members = nations.stream().filter(n -> n.getPosition() > Rank.APPLICANT.id && !n.isVacation()).collect(Collectors.toSet());
        Set<DBNation> activeMembers = members.stream().filter(n -> n.active_m() < 10080).collect(Collectors.toSet());
        Set<DBNation> applicants = nations.stream().filter(n -> n.getPosition() == Rank.APPLICANT.id && !n.isVacation()).collect(Collectors.toSet());
        Set<DBNation> activeApplicants = applicants.stream().filter(n -> n.active_m() < 10080).collect(Collectors.toSet());
        // 5 members (3 active/2 taxable) | 2 applicants (1 active)
        body.append(members.size()).append(" members (").append(activeMembers.size()).append(" active 1w)");
        if (!applicants.isEmpty()) {
            body.append(" | ").append(applicants.size()).append(" applicants (").append(activeApplicants.size()).append(" active)");
        }
        body.append("\n");
        // Off, Def, Cities (total/average), Score, Color
        int off = nations.stream().mapToInt(DBNation::getOff).sum();
        int def = nations.stream().mapToInt(DBNation::getDef).sum();
        long dev = members.stream().mapToLong(f -> (long) f.getInfra()).sum();
        long land = members.stream().mapToLong(f -> (long) f.getLand()).sum();
        double avgDev = dev / (double) members.size();
        double avgLand = land / (double) members.size();
        double score = members.stream().mapToDouble(DBNation::getScore).sum();
        // TODO FIXME :||remove !!important
        // revenue
        // devastation
        // laws
        // population
        Map<String, Integer> indexes = new LinkedHashMap<>();

        body.append(off).append("\uD83D\uDDE1 | ")
                .append(def).append("\uD83D\uDEE1 | ")
                .append("dev:" + MathMan.format(dev)).append(" (avg:").append(MathMan.format(avgDev)).append(") | ")
                .append("land:" + MathMan.format(land)).append(" (avg:").append(MathMan.format(avgLand)).append(") | ")
                .append(MathMan.format(score)).append("ns ")
                .append("\n```\n");


        Map<DBAlliance, Integer> warsByAlliance = new HashMap<>();
        for (DBWar war : getActiveWars()) {
            DBNation attacker = war.getNation(true);
            DBNation defender = war.getNation(false);
            if (attacker == null || attacker.active_m() > 7200) continue;
            if (defender == null || defender.active_m() > 7200) continue;
            int otherAAId = war.getAttacker_aa() == AllianceId ? war.getDefender_aa() : war.getAttacker_aa();
            if (otherAAId > 0) {
                DBAlliance otherAA = DBAlliance.getOrCreate(otherAAId);
                warsByAlliance.put(otherAA, warsByAlliance.getOrDefault(otherAA, 0) + 1);
            }
        }
        if (!warsByAlliance.isEmpty()) {
            List<Map.Entry<DBAlliance, Integer>> sorted = warsByAlliance.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();
            body.append("\n**Alliance Wars:**\n");
            String cappedMsg = null;
            if (sorted.size() > 20) {
                cappedMsg = "- +" + (sorted.size() - 20) + " more";
                sorted = sorted.stream().limit(20).collect(Collectors.toList());
            }
            for (Map.Entry<DBAlliance, Integer> entry : sorted) {
                body.append("- ").append(DNS.getMarkdownUrl(entry.getKey().getId(), true))
                        .append(": ").append(entry.getValue()).append(" wars\n");
            }
            if (cappedMsg != null) {
                body.append(cappedMsg).append("\n");
            }
        }
        Map<Integer, Treaty> treaties = this.getTreaties();
        if (treaties.isEmpty()) {
            body.append("`No treaties`\n");
        } else {
            body.append("\n**Treaties:**\n");
            String cappedMsg = null;
            if (treaties.size() > 20) {
                cappedMsg = "- +" + (treaties.size() - 20) + " more";
                treaties = treaties.entrySet().stream().limit(20).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            for (Treaty treaty : treaties.values()) {
                int otherId = treaty.getToId() == AllianceId ? treaty.getFromId() : treaty.getToId();
                body.append("- ").append(treaty.getType())
                        .append(": ").append(DNS.getMarkdownUrl(otherId, true))
                        .append(" (").append(DiscordUtil.timestamp(treaty.getEndTime(), null))
                        .append(")\n");
            }
            if (cappedMsg != null) {
                body.append(cappedMsg).append("\n");
            }
        }
        // Revenue
        Map<ResourceType, Double> revenue = getRevenue();
        body.append("\n**Revenue:**");
        body.append("`").append(ResourceType.resourcesToString(revenue)).append("`\n");
        body.append("- worth: `$" + MathMan.formatSig(ResourceType.convertedTotal(revenue)) + "`\n");
        return body.toString();
    }

    @Command(desc = "Sum of nation attribute for specific nations in alliance")
    public double getTotal(@NoFormat NationAttributeDouble attribute, @NoFormat @Default NationFilter filter) {
        Set<DBNation> nations = filter == null ? getNations() : getNations(filter.toCached(Long.MAX_VALUE));
        return nations.stream().mapToDouble(attribute::apply).sum();
    }

    @Command(desc = "Average of nation attribute for specific nations in alliance")
    public double getAverage(@NoFormat NationAttributeDouble attribute, @NoFormat @Default NationFilter filter) {
        Set<DBNation> nations = filter == null ? getNations() : getNations(filter.toCached(Long.MAX_VALUE));
        return nations.stream().mapToDouble(attribute::apply).average().orElse(0);
    }

    @Command(desc = "Returns the average value of the given attribute per another attribute (such as cities)")
    public double getAveragePer(@NoFormat NationAttributeDouble attribute, @NoFormat NationAttributeDouble per, @Default NationFilter filter) {
        double total = 0;
        double perTotal = 0;
        for (DBNation nation : getNations(filter.toCached(Long.MAX_VALUE))) {
            total += attribute.apply(nation);
            perTotal += per.apply(nation);
        }
        return total / perTotal;
    }


    @Command(desc = "Count of nations in alliance matching a filter")
    public int countNations(@NoFormat @Default NationFilter filter) {
        if (filter == null) return getNations().size();
        return getNations(filter.toCached(Long.MAX_VALUE)).size();
    }

    @Command(desc = "Is allied with another alliance")
    public boolean hasDefensiveTreaty(@NoFormat Set<DBAlliance> alliances) {
        for (DBAlliance alliance : alliances) {
            Treaty treaty = getDefenseTreaties().get(alliance.getId());
            if (treaty != null) return true;
        }
        return false;
    }

    @Command(desc = "Get the treaty type with another alliance")
    public TreatyType getTreatyType(DBAlliance alliance) {
        Treaty treaty = getTreaties().get(alliance.getId());
        return treaty == null ? null : treaty.getType();
    }

    @Command(desc = "Get the treaty level number with another alliance"
    )
    public int getTreatyOrdinal(DBAlliance alliance) {
        Treaty treaty = getTreaties().get(alliance.getId());
        return treaty == null ? 0 : treaty.getType().getStrength();
    }

    public String getMarkdownUrl() {
        return DNS.getMarkdownUrl(AllianceId, true);
    }

    @Override
    @Command
    public int getId() {
        return AllianceId;
    }

    @Override
    public boolean isAlliance() {
        return true;
    }

    @Command
    public String getName() {
        return AllianceName;
    }

    @Override
    public boolean isGuild() {
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }

    public Set<DBNation> getNations(boolean removeVM, int removeInactiveM, boolean removeApps) {
        Set<DBNation> nations = getNations();
        if (removeVM) nations.removeIf(f -> f.isVacation());
        if (removeInactiveM > 0) nations.removeIf(f -> f.active_m() > removeInactiveM);
        if (removeApps) nations.removeIf(f -> f.getPosition() <= 1);
        return nations;
    }

    public Set<DBNation> getNations() {
        return Locutus.imp().getNationDB().getNations(Collections.singleton(AllianceId));
    }

    @Command
    public Set<DBAlliance> getTreatiedAllies() {
        return getTreatiedAllies(TreatyType::isDefensive);
    }

    public Set<DBAlliance> getTreatiedAllies(Predicate<TreatyType> allowedType) {
        Set<DBAlliance> allies = new HashSet<>();
        for (Map.Entry<Integer, Treaty> treatyEntry : getTreaties(allowedType).entrySet()) {
            Treaty treaty = treatyEntry.getValue();
            int other = treaty.getFromId() == AllianceId ? treaty.getToId() : treaty.getFromId();
            allies.add(DBAlliance.getOrCreate(other));
        }
        return allies;
    }

    public Map<Integer, Treaty> getDefenseTreaties() {
        HashMap<Integer, Treaty> defTreaties = new HashMap<>(getTreaties());
        defTreaties.entrySet().removeIf(f -> !f.getValue().getType().isDefensive());
        return defTreaties;
    }

    public Map<Integer, Treaty> getTreaties() {
        return getTreaties(null);
    }

    public Map<Integer, Treaty> getTreaties(Predicate<TreatyType> allowedType) {
        Map<Integer, Treaty> result = Locutus.imp().getNationDB().getTreaties(AllianceId);
        if (allowedType != null) {
            result = result.entrySet().stream().filter(f -> allowedType.test(f.getValue().getType())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return result;
    }

    public Set<DBAlliance> getSphere() {
        return getSphereCached(new HashMap<>());
    }

    public Set<DBAlliance> getSphereCached(Map<Integer, DBAlliance> aaCache) {
        return getTreaties(this, new HashMap<>(), aaCache);
    }

    public List<DBAlliance> getSphereRankedCached(Map<Integer, DBAlliance> aaCache) {
        ArrayList<DBAlliance> list = new ArrayList<>(getSphereCached(aaCache));
        Collections.sort(list, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));
        return list;
    }

    public List<DBAlliance> getSphereRanked() {
        return getSphereRankedCached( new HashMap<>());
    }

    double scoreCached = -1;

    @Override
    public double getScore() {
        return getScore(null);
    }

    @Command
    public double getScore(@NoFormat @Default NationFilter filter) {
        if (filter != null) {
            return new SimpleNationList(getNations(filter.toCached(Long.MAX_VALUE))).getScore();
        }
        if (scoreCached == -1) {
            scoreCached = new SimpleNationList(getNations(true, 0, true)).getScore();
        }
        return scoreCached;
    }

    private Integer rank;

    public int getRank() {
        return getRank(null);
    }

    @Command(desc = "Rank by score")
    public int getRank(@NoFormat @Default NationFilter filter) {
        if (filter != null) {
            Map<Integer, List<DBNation>> byScore = Locutus.imp().getNationDB().getNationsByAlliance(filter.toCached(Long.MAX_VALUE), true);
            int rankTmp = 0;
            for (Map.Entry<Integer, List<DBNation>> entry : byScore.entrySet()) {
                rankTmp++;
                if (entry.getKey() == AllianceId) return rankTmp;
            }
            return Integer.MAX_VALUE;
        }
        if (rank == null) {
            Map<Integer, List<DBNation>> byScore = Locutus.imp().getNationDB().getNationsByAlliance( false, true, true, true);
            rank = 0;
            for (Map.Entry<Integer, List<DBNation>> entry : byScore.entrySet()) {
                rank++;
                if (entry.getKey() == AllianceId) return rank;
            }
            return rank = Integer.MAX_VALUE;
        }
        return rank;
    }


    @Command
    public int getAlliance_id() {
        return AllianceId;
    }

    private static Set<DBAlliance> getTreaties(DBAlliance currentAA, Map<DBAlliance, Double> currentWeb, Map<Integer, DBAlliance> aaCache) {
        if (!currentWeb.containsKey(currentAA)) currentWeb.put(currentAA, currentAA.getScore());
        aaCache.put(currentAA.AllianceId, currentAA);

        Map<Integer, Treaty> treaties = currentAA.getTreaties();

        DBAlliance protector = null;
        double protectorScore = 0;
        for (Map.Entry<Integer, Treaty> entry : treaties.entrySet()) {
            int otherId = entry.getKey();
            DBAlliance otherAA = aaCache.computeIfAbsent(otherId, f -> DBAlliance.getOrCreate(otherId));
            if (currentWeb.containsKey(otherAA)) continue;
            TreatyType type = entry.getValue().getType();
            if (type == TreatyType.PROTECTORATE) {
                double score = otherAA.getScore();
                double currentScore = 0;
                for (double value : currentWeb.values()) currentScore = Math.max(currentScore, value);
                if (score > currentScore && score > protectorScore) {
                    protectorScore = score;
                    protector = otherAA;
                }
                continue;
            }
            if (type.isMandatoryDefensive()) {
                if (protector != null) continue;
                getTreaties(otherAA, currentWeb, aaCache);
                continue;
            }
        }
        if (protector != null) {
            return getTreaties(protector, currentWeb, aaCache);
        }

        return new HashSet<>(currentWeb.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            if (o instanceof Number) {
                return ((Number) o).intValue() == AllianceId;
            }
            return false;
        }

        DBAlliance alliance = (DBAlliance) o;

        return AllianceId == alliance.AllianceId;
    }

    @Override
    public int hashCode() {
        return AllianceId;
    }

    @Command(desc = "Get the alliance's in-game link")
    public String getUrl() {
        return "" + Settings.INSTANCE.DNS_URL() + "/alliance/" + getAlliance_id();
    }

    public boolean exists() {
        return Locutus.imp().getNationDB().getAlliance(AllianceId) != null;
    }

    public GuildDB getGuildDB() {
        return Locutus.imp().getGuildDBByAA(AllianceId);
    }

    public List<AllianceChange> getRankChanges() {
        return Locutus.imp().getNationDB().getRemovesByAlliance(AllianceId);
    }

    public List<AllianceChange> getRankChanges(long timeStart) {
        return Locutus.imp().getNationDB().getRemovesByAlliance(AllianceId, timeStart);
    }

    public Set<DBWar> getActiveWars() {
        return Locutus.imp().getWarDb().getActiveWars(Collections.singleton(AllianceId), WarStatus.ACTIVE);
    }


    public void deleteMeta(AllianceMeta key) {
        if (metaCache != null && metaCache.remove(key.ordinal()) != null) {
            Locutus.imp().getNationDB().deleteMeta(-AllianceId, key.ordinal());
        }
    }

    public boolean setMetaRaw(int id, byte[] value) {
        if (metaCache == null) {
            synchronized (this) {
                if (metaCache == null) {
                    metaCache = new Int2ObjectOpenHashMap<>();
                }
            }
        }
        byte[] existing = metaCache.isEmpty() ? null : metaCache.get(id);
        if (existing == null || !Arrays.equals(existing, value)) {
            metaCache.put(id, value);
            return true;
        }
        return false;
    }

    public void setMeta(AllianceMeta key, byte... value) {
        if (setMetaRaw(key.ordinal(), value)) {
            Locutus.imp().getNationDB().setMeta(-AllianceId, key.ordinal(), value);
        }
    }

    public ByteBuffer getMeta(AllianceMeta key) {
        if (metaCache == null) {
            return null;
        }
        byte[] result = metaCache.get(key.ordinal());
        return result == null ? null : ByteBuffer.wrap(result);
    }

    public void setMeta(AllianceMeta key, byte value) {
        setMeta(key, new byte[] {value});
    }

    public void setMeta(AllianceMeta key, int value) {
        setMeta(key, ByteBuffer.allocate(4).putInt(value).array());
    }

    public void setMeta(AllianceMeta key, long value) {
        setMeta(key, ByteBuffer.allocate(8).putLong(value).array());
    }

    public void setMeta(AllianceMeta key, double value) {
        setMeta(key, ByteBuffer.allocate(8).putDouble(value).array());
    }

    public void setMeta(AllianceMeta key, String value) {
        setMeta(key, value.getBytes(StandardCharsets.ISO_8859_1));
    }

    public DBNation getLeader() {
        return LeaderId == 0 ? null : DBNation.getById(LeaderId);
    }

    public ApiKeyPool getApiKeys(boolean onlyLeader) {
        {
            DBNation leader = getLeader();
            if (leader != null) {
                try {
                    ApiKeyPool.ApiKey key = leader.getApiKey(false);
                    if (key != null) return new ApiKeyPool.SimpleBuilder().addKey(key).build();
                } catch (IllegalArgumentException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
        GuildDB db = getGuildDB();
        if (db != null) {
            List<ApiKeyPool.ApiKey> apiKeys = db.getOrNull(GuildKey.API_KEY);
            if (apiKeys != null && !apiKeys.isEmpty()) {
                ApiKeyPool.SimpleBuilder builder = new ApiKeyPool.SimpleBuilder();
                int keys = 0;
                for (ApiKeyPool.ApiKey apiKey : apiKeys) {
                    DBNation nation = apiKey.getNation();
                    if (nation == null || nation.getAlliance_id() != AllianceId) continue;
                    builder.addKey(apiKey);
                    keys++;
                }
                if (keys > 0) {
                    return builder.build();
                }
            }
        }
        ApiKeyPool.SimpleBuilder builder = new ApiKeyPool.SimpleBuilder();
        Set<DBNation> nations = getNations();
        for (DBNation gov : nations) {
            if (gov.getPositionEnum() == Rank.APPLICANT) continue;
            try {
                ApiKeyPool.ApiKey key = gov.getApiKey(false);
                if (key == null) continue;
                builder.addKey(key);
            } catch (IllegalArgumentException ignore) {
                ignore.printStackTrace();
            }
        }
        if (!builder.isEmpty()) {
            return builder.build();
        }
        return null;
    }

    public Map<DBNation, Map<ResourceType, Double>> getMemberStockpile() throws IOException {
        return getMemberStockpile(f -> true);
    }

    public Map<DBNation, Map<ResourceType, Double>> getMemberStockpile(Predicate<DBNation> fetchNations) throws IOException {
        Set<DBNation> nations = getNations().stream()
                .filter(f -> !f.isVacation() && f.getPositionEnum().id > Rank.APPLICANT.id && fetchNations.test(f)).collect(Collectors.toSet());
        if (nations.isEmpty()) return Collections.emptyMap();

        DnsApi api = getApiOrThrow(true);
        long now = System.currentTimeMillis();
        Map<DBNation, Map<ResourceType, Double>> result = new HashMap<>();
        List<AllianceMemberFunds> funds = api.allianceMemberFunds().call();
        for (AllianceMemberFunds fund : funds) {
            DBNation nation = DBNation.getById(fund.NationId);
            if (nation != null) {
                NationPrivate natPriv = nation.getPrivateData();
                natPriv.update(fund, now);
                result.put(nation, natPriv.getStockpile(Long.MIN_VALUE));
            }
        }
        for (DBNation nation : getNations()) {
            NationPrivate nationPrivate = nation.getPrivateData();
            nationPrivate.getOutdatedStockpile().set(now);
        }
        return result;
    }

    public DnsApi getApiOrThrow(boolean preferKeyStore, boolean requireLeader) {
        if (preferKeyStore) {
            GuildDB db = getGuildDB();
            if (db != null) {
                ApiKeyPool key = db.getApiKey(AllianceId, requireLeader);
                if (key != null) return new DnsApi(key);
            }
        }
        return getApiOrThrow(requireLeader);
    }

    public DnsApi getApiOrThrow(boolean requireLeader) {
        DnsApi api = getApi(requireLeader);
        if (api == null) {
            String msg = "No api key found for " + getMarkdownUrl() + ". Please use" + CM.settings_default.registerApiKey.cmd.toSlashMention() + "\n" +
                    "Api key can be found on <https://diplomacyandstrife.com/account/>";
            if (requireLeader) msg += " and ensure your in-game position grants: " + "LEADER";
            throw new IllegalArgumentException(msg);
        }
        return api;
    }

    public DnsApi getApi(boolean requireLeader) {
        ApiKeyPool pool = getApiKeys(requireLeader);
        if (pool == null) return null;
        return new DnsApi(pool);
    }

    public AlliancePrivate getPrivateData() {
        if (privateData == null) {
            privateData = new AlliancePrivate(getId());
        }
        return privateData;
    }

    public Map<ResourceType, Double> getStockpile(long timestamp) {
        return getStockpile(timestamp, false);
    }

    public Map<ResourceType, Double> getStockpile(long timestamp, boolean throwError) {
        Map<ResourceType, Double> stockpile = getPrivateData().getStockpile(timestamp);
        if (stockpile == null && throwError) {
            getApiOrThrow(true);
            throw new IllegalArgumentException("No stockpile found for " + getMarkdownUrl());
        }
        return stockpile;
    }

    public Set<DBNation> getNations(Predicate<DBNation> filter) {
        Set<DBNation> nations = new HashSet<>();
        for (DBNation nation : getNations()) {
            if (filter.test(nation)) nations.add(nation);
        }
        return nations;
    }

    public int getLeaderId() {
        return LeaderId;
    }

    private <T> T withApi(int nationId, Function<NationPrivate, AtomicLong> getCacheMsFlag, Function<DnsApi, DnsQuery<T>> call, Function<T, Integer> getNationId, TriConsumer<NationPrivate, T, Long> update) {
        DBNation nation = DBNation.getById(nationId);
        if (nation == null || nation.isVacation() || nation.getPositionEnum().id <= Rank.APPLICANT.id || nation.getAlliance_id() != getAlliance_id()) return null;
        DnsApi api = getApiOrThrow(true);
        long now = System.currentTimeMillis();
        List<T> entities = call.apply(api).call();
        T result = null;
        for (T entity : entities) {
            int id = getNationId.apply(entity);
            DBNation other = DBNation.getById(id);
            if (other != null) {
                update.accept(other.getPrivateData(), entity, now);
            }
            if (id == nationId) {
                result = entity;
            }
        }
        for (DBNation other : getNations()) {
            getCacheMsFlag.apply(other.getPrivateData()).set(now);
        }
        return result;
    }

    public AllianceMilitary updateMilitaryOfNation(int nationId) {
        return withApi(nationId, NationPrivate::getOutdatedMilitary, DnsApi::allianceMilitary, f -> f.NationId, NationPrivate::update);
    }

    // AllianceEffects result = aa.updateEffectsOfNation(parentId);
    public NationsEffectsSummary updateEffectsOfNation(int nationId) {
        return withApi(nationId, NationPrivate::getOutdatedEffects, DnsApi::nationsEffectsSummary, f -> f.nationID, NationPrivate::update);
    }

    public AllianceTech updateTechOfNation(int nationId) {
        return withApi(nationId, NationPrivate::getOutdatedTechnology, DnsApi::allianceTech, f -> f.nationID, NationPrivate::update);
    }

    public AllianceMemberFunds updateStockpileOfNation(int nationId) {
        return withApi(nationId, NationPrivate::getOutdatedStockpile, DnsApi::allianceMemberFunds, f -> f.NationId, NationPrivate::update);
    }

    public Auth getAuth(boolean requireLeader) {
        GuildDB db = getGuildDB();
        if (db != null) {
            List<ApiKeyPool.ApiKey> apiKeys = db.getOrNull(GuildKey.API_KEY);
            if (apiKeys != null) {
                for (ApiKeyPool.ApiKey apiKey : apiKeys) {
                    DBNation gov = apiKey.getNation();
                    if (gov == null || !db.isAllianceId(gov.getAlliance_id()) || gov.isVacation()) continue;
                    if (requireLeader && gov.getPositionEnum() != Rank.LEADER) continue;
                    Auth auth = gov.getAuth(false);
                    if (auth != null && auth.isValid()) return auth;
                }
            }
        }
        Set<DBNation> nations = getNations();
        for (DBNation gov : nations) {
            if (gov.isVacation() || gov.getPositionEnum().id <= Rank.APPLICANT.id) continue;
            if (requireLeader && gov.getPositionEnum() != Rank.LEADER) continue;
            Auth auth = gov.getAuth(false);
            if (auth != null && auth.getAllianceId() == getAlliance_id() && auth.isValid()) {
                return auth;
            }
        }
        return null;
    }

    public void setAlliancePrivate(AlliancePrivate ap) {
        this.privateData = ap;
    }

    @Command(desc = "Offensive wars")
    public int getOff() {
        return getNations().stream().mapToInt(DBNation::getOff).sum();
    }

    @Command(desc = "Defensive wars")
    public int getDef() {
        return getNations().stream().mapToInt(DBNation::getDef).sum();
    }

    @Command(desc = "Total number of active wars")
    public int getNumWars() {
        return getNations().stream().mapToInt(DBNation::getNumWars).sum();
    }
}
