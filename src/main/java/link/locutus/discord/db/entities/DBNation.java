package link.locutus.discord.db.entities;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.generated.Nation;
import link.locutus.discord.api.types.*;
import link.locutus.discord.api.types.tx.BankTransfer;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.binding.annotation.Timestamp;
import link.locutus.discord.commands.manager.v2.binding.bindings.PlaceholderCache;
import link.locutus.discord.commands.manager.v2.binding.bindings.ScopedPlaceholderCache;
import link.locutus.discord.commands.manager.v2.command.*;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.BankDB;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.NationDB;
import link.locutus.discord.db.ReportManager;
import link.locutus.discord.db.entities.components.NationPrivate;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.nation.NationChangeEvent2;
import link.locutus.discord.event.nation.NationRegisterEvent;
import link.locutus.discord.pnw.*;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.*;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.offshore.Auth;
import link.locutus.discord.util.scheduler.ThrowingSupplier;
import link.locutus.discord.util.sheet.SheetUtil;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.util.task.ia.IACheckup;
import link.locutus.discord.util.task.roles.AutoRoleInfo;
import link.locutus.discord.web.CommandResult;
import link.locutus.discord.api.generated.ResourceType;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DBNation implements NationOrAlliance, DBEntity<Nation, DBNation> {
    private int NationId;
    private int AllianceId;
    private String NationName;
    private String LeaderName;
    private String DiscordName;
    private Rank rank;
    private long DateOfJoining;
    private double Infra;
    private double Land;
    private double Pop;
    private double StabilityIndex;
    private double PowerIndex;
    private double EducationIndex;
    private double CommerceIndex;
    private double TransportationIndex;
    private double EmploymentIndex;
    private double TechIndex;
    private double WarIndex;
    //
    private double TaxIncome;
    private double OtherIncome;
    private double CorporationIncome;
    //
    private double MineralOutput;
    private double ProductionOutput;
    private double FuelOutput;
    private double CashOutput;
    //
    private double Score;
    private int GolfRateing;
    private int NumberOfProjects;
    private int TotalLawsPassed;
    private int AverageIndex;
    private double Devastation;
    private int DefWars;
    private int OffWars;
    private long LastOnline;
    private long ProtectionTime;
    private int TotalSlots;
    private double PoliticalPowerOutput;
    private double RareMetalOutput;
    private double UraniumOutput;
    private double NonCoreLand;

    //

    public DBNation() {
        NationName = "";
        LeaderName = "";
        DiscordName = "";
        rank = Rank.NONE;
    }

    private NationPrivate privateData;
    private Int2ObjectOpenHashMap<byte[]> metaCache = null;

    @Override
    public String getTableName() {
        return "nations";
    }

    @Override
    public DBNation copy() {
        DBNation result = DBEntity.super.copy();
        if (this.metaCache != null) {
            result.metaCache = metaCache;
        }
        if (this.privateData != null) {
            result.privateData = privateData;
        }
        return result;
    }

    @Override
    public boolean update(Nation entity, Consumer<Event> eventConsumer) {
        return update(Locutus.imp().getNationDB(), entity, eventConsumer);
    }

    public boolean update(NationDB db, Nation entity, Consumer<Event> eventConsumer) {
        DBNation copy = null;
        if (entity.NationId != NationId) {
            if (copy == null) copy = copy();
            NationId = entity.NationId;
        }
        if (entity.AllianceId != AllianceId) {
            if (copy == null) copy = copy();
            AllianceId = entity.AllianceId;
        }
        if (!entity.NationName.equals(NationName)) {
            if (copy == null) copy = copy();
            NationName = entity.NationName;
        }
        if (!entity.LeaderName.equals(LeaderName)) {
            if (copy == null) copy = copy();
            LeaderName = entity.LeaderName;
        }
        if (!entity.DiscordName.equals(DiscordName)) {
            if (copy == null) copy = copy();
            DiscordName = entity.DiscordName;
        }
        Rank newRank = getRank(db, entity);
        if (newRank != rank) {
            if (copy == null) copy = copy();
            rank = newRank;
        }
        if (entity.DateOfJoining.getTime() != DateOfJoining) {
            if (copy == null) copy = copy();
            DateOfJoining = entity.DateOfJoining.getTime();
        }
        if (entity.Infra != Infra) {
            if (copy == null) copy = copy();
            Infra = entity.Infra;
        }
        if (entity.Land != Land) {
            if (copy == null) copy = copy();
            Land = entity.Land;
        }
        if (entity.Pop != Pop) {
            if (copy == null) copy = copy();
            Pop = entity.Pop;
        }
        if (entity.StabilityIndex != StabilityIndex) {
            if (copy == null) copy = copy();
            StabilityIndex = entity.StabilityIndex;
        }
        if (entity.PowerIndex != PowerIndex) {
            if (copy == null) copy = copy();
            PowerIndex = entity.PowerIndex;
        }
        if (entity.EducationIndex != EducationIndex) {
            if (copy == null) copy = copy();
            EducationIndex = entity.EducationIndex;
        }
        if (entity.CommerceIndex != CommerceIndex) {
            if (copy == null) copy = copy();
            CommerceIndex = entity.CommerceIndex;
        }
        if (entity.TransportationIndex != TransportationIndex) {
            if (copy == null) copy = copy();
            TransportationIndex = entity.TransportationIndex;
        }
        if (entity.EmploymentIndex != EmploymentIndex) {
            if (copy == null) copy = copy();
            EmploymentIndex = entity.EmploymentIndex;
        }
        if (entity.TechIndex != TechIndex) {
            if (copy == null) copy = copy();
            TechIndex = entity.TechIndex;
        }
        if (entity.WarIndex != WarIndex) {
            if (copy == null) copy = copy();
            WarIndex = entity.WarIndex;
        }
        if (entity.TaxIncome != TaxIncome) {
            if (copy == null) copy = copy();
            TaxIncome = entity.TaxIncome;
        }
        if (entity.OtherIncome != OtherIncome) {
            if (copy == null) copy = copy();
            OtherIncome = entity.OtherIncome;
        }
        if (entity.CorporationIncome != CorporationIncome) {
            if (copy == null) copy = copy();
            CorporationIncome = entity.CorporationIncome;
        }
        if (entity.MineralOutput != MineralOutput) {
            if (copy == null) copy = copy();
            MineralOutput = entity.MineralOutput;
        }
        if (entity.ProductionOutput != ProductionOutput) {
            if (copy == null) copy = copy();
            ProductionOutput = entity.ProductionOutput;
        }
        if (entity.FuelOutput != FuelOutput) {
            if (copy == null) copy = copy();
            FuelOutput = entity.FuelOutput;
        }
        if (entity.CashOutput != CashOutput) {
            if (copy == null) copy = copy();
            CashOutput = entity.CashOutput;
        }
        if (entity.Score != Score) {
            if (copy == null) copy = copy();
            Score = entity.Score;
        }
        if (entity.GolfRateing != GolfRateing) {
            if (copy == null) copy = copy();
            GolfRateing = entity.GolfRateing;
        }
        if (entity.NumberOfProjects != NumberOfProjects) {
            if (copy == null) copy = copy();
            NumberOfProjects = entity.NumberOfProjects;
        }
        if (entity.TotalLawsPassed != TotalLawsPassed) {
            if (copy == null) copy = copy();
            TotalLawsPassed = entity.TotalLawsPassed;
        }
        if (entity.AverageIndex != AverageIndex) {
            if (copy == null) copy = copy();
            AverageIndex = entity.AverageIndex;
        }
        if (entity.Devastation != Devastation) {
            if (copy == null) copy = copy();
            Devastation = entity.Devastation;
        }
        if (entity.DefWars != DefWars) {
            if (copy == null) copy = copy();
            DefWars = entity.DefWars;
        }
        if (entity.OffWars != OffWars) {
            if (copy == null) copy = copy();
            OffWars = entity.OffWars;
        }
        if (entity.LastOnline.getTime() != LastOnline) {
            if (copy == null) copy = copy();
            LastOnline = entity.LastOnline.getTime();
        }
        if (entity.ProtectionTime.getTime() != ProtectionTime) {
            if (copy == null) copy = copy();
            ProtectionTime = entity.ProtectionTime.getTime();
        }
        if (entity.TotalSlots != TotalSlots) {
            if (copy == null) copy = copy();
            TotalSlots = entity.TotalSlots;
        }
        if (entity.PoliticalPowerOutput != PoliticalPowerOutput) {
            if (copy == null) copy = copy();
            PoliticalPowerOutput = entity.PoliticalPowerOutput;
        }
        if (entity.RareMetalOutput != RareMetalOutput) {
            if (copy == null) copy = copy();
            RareMetalOutput = entity.RareMetalOutput;
        }
        if (entity.UraniumOutput != UraniumOutput) {
            if (copy == null) copy = copy();
            UraniumOutput = entity.UraniumOutput;
        }
        if (entity.NonCoreLand != NonCoreLand) {
            if (copy == null) copy = copy();
            NonCoreLand = entity.NonCoreLand;
        }
        if (copy != null && eventConsumer != null) {
            eventConsumer.accept(new NationChangeEvent2(this, copy));
        }
        return copy != null;
    }

    private Rank getRank(NationDB db, Nation nation) {
        if (nation.Alliance.endsWith(" Applicant")) {
            return Rank.APPLICANT;
        }
        if (nation.AllianceId == 0) {
            return Rank.NONE;
        }
        DBAlliance aa = db.getAlliance(nation.AllianceId);
        if (aa != null && aa.getLeaderId() == nation.NationId) {
            return Rank.LEADER;
        }
        return Rank.MEMBER;
    }

    @Override
    public Object[] write() {
        return new Object[] {
                NationId,
                AllianceId,
                NationName,
                LeaderName,
                DiscordName,
                rank.ordinal(),
                DateOfJoining,
                Infra,
                Land,
                Pop,
                StabilityIndex,
                PowerIndex,
                EducationIndex,
                CommerceIndex,
                TransportationIndex,
                EmploymentIndex,
                TechIndex,
                WarIndex,
                TaxIncome,
                OtherIncome,
                CorporationIncome,
                MineralOutput,
                ProductionOutput,
                FuelOutput,
                CashOutput,
                Score,
                GolfRateing,
                NumberOfProjects,
                TotalLawsPassed,
                AverageIndex,
                Devastation,
                DefWars,
                OffWars,
                LastOnline,
                ProtectionTime,
                TotalSlots,
                PoliticalPowerOutput,
                RareMetalOutput,
                UraniumOutput,
                NonCoreLand
        };
    }

    @Override
    public void load(Object[] raw) {
        this.NationId = (int) raw[0];
        this.AllianceId = (int) raw[1];
        this.NationName = (String) raw[2];
        this.LeaderName = (String) raw[3];
        this.DiscordName = (String) raw[4];
        this.rank = Rank.values[((int) raw[5])];
        this.DateOfJoining = (long) raw[6];
        this.Infra = (double) raw[7];
        this.Land = (double) raw[8];
        this.Pop = (double) raw[9];
        this.StabilityIndex = (double) raw[10];
        this.PowerIndex = (double) raw[11];
        this.EducationIndex = (double) raw[12];
        this.CommerceIndex = (double) raw[13];
        this.TransportationIndex = (double) raw[14];
        this.EmploymentIndex = (double) raw[15];
        this.TechIndex = (double) raw[16];
        this.WarIndex = (double) raw[17];
        this.TaxIncome = (double) raw[18];
        this.OtherIncome = (double) raw[19];
        this.CorporationIncome = (double) raw[20];
        this.MineralOutput = (double) raw[21];
        this.ProductionOutput = (double) raw[22];
        this.FuelOutput = (double) raw[23];
        this.CashOutput = (double) raw[24];
        this.Score = (double) raw[25];
        this.GolfRateing = (int) raw[26];
        this.NumberOfProjects = (int) raw[27];
        this.TotalLawsPassed = (int) raw[28];
        this.AverageIndex = (int) raw[29];
        this.Devastation = (double) raw[30];
        this.DefWars = (int) raw[31];
        this.OffWars = (int) raw[32];
        this.LastOnline = (long) raw[33];
        this.ProtectionTime = (long) raw[34];
        this.TotalSlots = (int) raw[35];
        this.PoliticalPowerOutput = (double) raw[36];
        this.RareMetalOutput = (double) raw[37];
        this.UraniumOutput = (double) raw[38];
        this.NonCoreLand = (double) raw[39];
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> types = new LinkedHashMap<>();
        types.put("NationId", Integer.class);
        types.put("AllianceId", Integer.class);
        types.put("NationName", String.class);
        types.put("LeaderName", String.class);
        types.put("DiscordName", String.class);
        types.put("rank", Integer.class);
        types.put("DateOfJoining", Long.class);
        types.put("Infra", Double.class);
        types.put("Land", Double.class);
        types.put("Pop", Double.class);
        types.put("StabilityIndex", Double.class);
        types.put("PowerIndex", Double.class);
        types.put("EducationIndex", Double.class);
        types.put("CommerceIndex", Double.class);
        types.put("TransportationIndex", Double.class);
        types.put("EmploymentIndex", Double.class);
        types.put("TechIndex", Double.class);
        types.put("WarIndex", Double.class);
        types.put("TaxIncome", Double.class);
        types.put("OtherIncome", Double.class);
        types.put("CorporationIncome", Double.class);
        types.put("MineralOutput", Double.class);
        types.put("ProductionOutput", Double.class);
        types.put("FuelOutput", Double.class);
        types.put("CashOutput", Double.class);
        types.put("Score", Double.class);
        types.put("GolfRateing", Integer.class);
        types.put("NumberOfProjects", Integer.class);
        types.put("TotalLawsPassed", Integer.class);
        types.put("AverageIndex", Integer.class);
        types.put("Devastation", Double.class);
        types.put("DefWars", Integer.class);
        types.put("OffWars", Integer.class);
        types.put("LastOnline", Long.class);
        types.put("ProtectionTime", Long.class);
        types.put("TotalSlots", Integer.class);
        types.put("PoliticalPowerOutput", Double.class);
        types.put("RareMetalOutput", Double.class);
        types.put("UraniumOutput", Double.class);
        types.put("NonCoreLand", Double.class);
        return types;
    }

    @Override
    public DBNation emptyInstance() {
        return new DBNation();
    }

    public static DBNation getByUser(User user) {
        return DiscordUtil.getNation(user);
    }

    public static DBNation getById(int nationId) {
        return Locutus.imp().getNationDB().getNation(nationId);
    }

    public static DBNation getOrCreate(int nationId) {
        DBNation existing = getById(nationId);
        if (existing == null) {
            existing = new DBNation();
            existing.NationId = nationId;
        }
        return existing;
    }

    public String register(User user, GuildDB db, boolean isNewRegistration) {
        if (NationId == Settings.INSTANCE.NATION_ID) {
            if (Settings.INSTANCE.ADMIN_USER_ID != user.getIdLong()) {
                if (Settings.INSTANCE.ADMIN_USER_ID > 0) {
                    throw new IllegalArgumentException("Invalid admin user id in `config.yaml`. Tried to register `" + user.getIdLong() + "` but config has `" + Settings.INSTANCE.ADMIN_USER_ID + "`");
                }
                Settings.INSTANCE.ADMIN_USER_ID = user.getIdLong();
                Settings.INSTANCE.save(Settings.INSTANCE.getDefaultFile());
            }
        }
        new NationRegisterEvent(NationId, db, user, isNewRegistration).post();

        StringBuilder output = new StringBuilder();
        output.append("Registration successful. See:\n");
        output.append("- " + MarkupUtil.markdownUrl("Wiki Pages", "<https://github.com/xdnw/diplocutus/wiki>") + "\n");
        output.append("- " + MarkupUtil.markdownUrl("Initial Setup", "<https://github.com/xdnw/diplocutus/wiki/initial_setup>") + "\n");
        output.append("- " + MarkupUtil.markdownUrl("Commands", "<https://github.com/xdnw/diplocutus/wiki/commands>") + "\n\n");
        output.append("Join the Support Server \n");
        output.append("""
                - Help using or configuring the bot
                - Public banking/offshoring
                - Requesting a feature
                - General inquiries/feedback
                <https://discord.gg/cUuskPDrB7>""")
                .append("\n\nRunning auto role task...");
        if (db != null) {
            try {
                Role role = Roles.REGISTERED.toRole(db);
                if (role != null) {
                    try {
                        Member member = db.getGuild().getMember(user);
                        if (member == null) {
                            member = db.getGuild().retrieveMember(user).complete();
                        }
                        if (member != null) {
                            RateLimitUtil.complete(db.getGuild().addRoleToMember(user, role));
                            output.append("You have been assigned the role: " + role.getName());
                            AutoRoleInfo task = db.getAutoRoleTask().autoRole(member, this);
                            task.execute();
                            output.append("\n" + task.getChangesAndErrorMessage());
                        } else {
                            output.append("Member " + DiscordUtil.getFullUsername(user) + " not found in guild: " + db.getGuild());
                        }
                    } catch (InsufficientPermissionException e) {
                        output.append(e.getMessage() + "\n");
                    }
                } else {
                    if (Roles.ADMIN.has(user, db.getGuild())) {
                        output.append("No REGISTERED role mapping found.");
                        output.append("\nCreate a role mapping with " + CM.role.setAlias.cmd.toSlashMention() + "");
                    }
                }
            } catch (HierarchyException e) {
                output.append("\nCannot add role (Make sure the Bot's role is high enough and has add role perms)\n- " + e.getMessage());
            }
        }
        return output.toString();
    }

    public NationPrivate getPrivateData() {
        if (privateData == null) {
            privateData = new NationPrivate(getId());
        }
        return privateData;
    }

    public int getProject(Project project, long timestamp) {
        return getPrivateData().getProjects(timestamp).getOrDefault(project, 0);
    }

    private Map.Entry<Object, String> getAuditRaw(ValueStore store, @Me GuildDB db, IACheckup.AuditType audit) throws IOException, ExecutionException, InterruptedException {
        ScopedPlaceholderCache<DBNation> scoped = PlaceholderCache.getScoped(store, DBNation.class, "getAudit");
        List<DBNation> nations = scoped.getList(this);
        AllianceList aaList = db.getAllianceList().subList(nations);
        IACheckup checkup = scoped.getGlobal((ThrowingSupplier<IACheckup>)
                () -> new IACheckup(db, aaList, true));
        if (!aaList.isInAlliance(this)) {
            throw new IllegalArgumentException("Nation " + NationId + " not in alliance: " + checkup.getAlliance().getIds());
        }
        Map<IACheckup.AuditType, Map.Entry<Object, String>> result =
                checkup.checkup(this, new IACheckup.AuditType[]{audit}, true, true);
        return result.get(audit);
    }

    @RolePermission(Roles.INTERNAL_AFFAIRS_STAFF)
    @Command(desc = "If the nation passes an audit")
    public boolean passesAudit(ValueStore store, @Me GuildDB db, IACheckup.AuditType audit) throws IOException, ExecutionException, InterruptedException {
        Map.Entry<Object, String> result = getAuditRaw(store, db, audit);
        return result == null || result.getKey() == null;
    }

    @RolePermission(Roles.INTERNAL_AFFAIRS_STAFF)
    @Command(desc = "Get the Audit result raw value")
    public Object getAuditResult(ValueStore store, @Me GuildDB db, IACheckup.AuditType audit) throws IOException, ExecutionException, InterruptedException {
        Map.Entry<Object, String> result = getAuditRaw(store, db, audit);
        return result == null ? null : result.getKey();
    }

    @RolePermission(Roles.INTERNAL_AFFAIRS_STAFF)
    @Command(desc = "Get the Audit result message")
    public String getAuditResultString(ValueStore store, @Me GuildDB db, IACheckup.AuditType audit) throws IOException, ExecutionException, InterruptedException {
        Map.Entry<Object, String> result = getAuditRaw(store, db, audit);
        return result == null ? null : result.getValue();
    }

    @Override
    public boolean isValid() {
        return getById(NationId) != null;
    }

    @Command(desc = "Number of nations")
    // Mock
    public int getNations() {
        return 1;
    }

//    /**
//     * Entry( value, has data )
//     * @return
//     */
//    public Map.Entry<Double, Boolean> getIntelOpValue() {
//        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14);
//        return getIntelOpValue(cutoff);
//    }

//    public Map.Entry<Double, Boolean> getIntelOpValue(long cutoff) {
//        if (active_m() < 4320) return null;
//        if (isVacation()) return null;
//        if (active_m() > 385920) return null;
////        if (!isGray()) return null;
//        if (getDef() == 3) return null;
//        long currentDate = System.currentTimeMillis();
//
//        LootEntry loot = Locutus.imp().getNationDB().getLoot(getNation_id());
//        if (loot != null && loot.getDate() > cutoff) return null;
//
//        long lastLootDate = 0;
//        if (loot != null) lastLootDate = Math.max(lastLootDate, loot.getDate());
//        if (currentDate - active_m() * 60L * 1000L < lastLootDate) return null;
//
//        long checkBankCutoff = currentDate - TimeUnit.DAYS.toMillis(60);
//        if (cities > 10 && lastLootDate < checkBankCutoff) {
//            List<Transaction2> transactions = getTransactions(Long.MAX_VALUE, true);
//            long recent = 0;
//            for (Transaction2 transaction : transactions) {
//                if (transaction.receiver_id != getNation_id()) {
//                    recent = Math.max(recent, transaction.tx_datetime);
//                }
//            }
//            if (recent > 0) {
//                lastLootDate = Math.max(lastLootDate, recent);
//            }
//        }
//        double cityCost = DNS.City.nextCityCost(cities, true, hasProject(Projects.URBAN_PLANNING), hasProject(Projects.ADVANCED_URBAN_PLANNING), hasProject(Projects.METROPOLITAN_PLANNING), hasProject(Projects.GOVERNMENT_SUPPORT_AGENCY));
//        double maxStockpile = cityCost * 2;
//        double daysToMax = maxStockpile / (getInfra() * 300);
//        if (lastLootDate == 0) {
//            lastLootDate = currentDate - TimeUnit.DAYS.toMillis((int) daysToMax);
//        }
//
//        long diffMin = TimeUnit.MILLISECONDS.toMinutes(currentDate - lastLootDate);
//
//        if (active_m() < 12000) {
//            diffMin /= 8;
//            DBWar lastWar = Locutus.imp().getWarDb().getLastDefensiveWar(nation_id);
//            if (lastWar != null) {
//                long warDiff = currentDate - TimeUnit.DAYS.toMillis(240);
//                if (lastWar.getDate() > warDiff) {
//                    double ratio = TimeUnit.MILLISECONDS.toDays(currentDate - lastWar.getDate()) / 240d;
//                    if (lastWar.getStatus() == WarStatus.PEACE || lastWar.getStatus() == WarStatus.DEFENDER_VICTORY) {
//                        diffMin *= ratio;
//                    }
//                }
//            }
//        }
//
//        double value = getAvg_infra() * (diffMin + active_m()) * getCities();
//
//        if (loot == null && cities < 12) {
//            long finalLastLootDate = lastLootDate;
//            Map<Integer, DBWar> wars = Locutus.imp().getWarDb().getWarsForNationOrAlliance(f -> f == nation_id, null, f -> f.getDate() > finalLastLootDate);
//            if (!wars.isEmpty()) {
//                WarParser cost = WarParser.of(wars.values(), f -> f.getAttacker_id() == nation_id);
//                double total = cost.toWarCost(false, false, false, false, false).convertedTotal(true);
//                value -= total;
//            }
//        }
//
//        // value for weak military
//        double soldierPct = (double) getSoldiers() / (Buildings.BARRACKS.getUnitCap() * Buildings.BARRACKS.cap(this::hasProject) * getCities());
//        double tankPct = (double) getTanks() / (Buildings.FACTORY.getUnitCap() * Buildings.FACTORY.cap(this::hasProject) * getCities());
//        value = value + value * (2 - soldierPct - tankPct);
//
//        return new AbstractMap.SimpleEntry<>(value, loot != null);
//    }

    public @Nullable Long getSnapshot() {
        return null;
    }

    @Command(desc = "Days since joining the alliance")
    public double allianceSeniority() {
        long result = allianceSeniorityMs();
        if (result == 0 || result == Long.MAX_VALUE) return result;
        return result / (double) TimeUnit.DAYS.toMillis(1);
    }

    @Command(desc = "Days since joining the alliance")
        public double allianceSeniorityApplicant() {
        long result = allianceSeniorityApplicantMs();
        if (result == 0 || result == Long.MAX_VALUE) return result;
        return result / (double) TimeUnit.DAYS.toMillis(1);
    }

    @Command
    public long allianceSeniorityNoneMs() {
        if (AllianceId != 0) return 0;
        long timestamp = Locutus.imp().getNationDB().getAllianceMemberSeniorityTimestamp(this, getSnapshot());
        long now = getSnapshot() == null ? System.currentTimeMillis() : getSnapshot();
        if (timestamp > now) return 0;
        return now - timestamp;
    }

    @Command(desc = "Milliseconds since joining the alliance")
    public long allianceSeniorityApplicantMs() {
        if (AllianceId == 0) return 0;
        long timestamp = Locutus.imp().getNationDB().getAllianceApplicantSeniorityTimestamp(this, getSnapshot());
        long now = getSnapshot() == null ? System.currentTimeMillis() : getSnapshot();
        if (timestamp > now) return 0;
        return (now - timestamp);
    }

    @Command(desc = "Milliseconds since joining the alliance")
    public long allianceSeniorityMs() {
        if (AllianceId == 0) return 0;
        long timestamp = Locutus.imp().getNationDB().getAllianceMemberSeniorityTimestamp(this, getSnapshot());
        long now = getSnapshot() == null ? System.currentTimeMillis() : getSnapshot();
        if (timestamp > now) return 0;
        return now - timestamp;
    }

    @Command(desc="War Index")
    public double getWarIndex() {
        return this.WarIndex;
    }

    @Command(desc = "Estimated combined strength of the enemies its fighting")
    public double getEnemyStrength() {
        Set<DBWar> wars = getActiveWars();
        if (wars.isEmpty()) {
            return 0;
        }
        double totalStr = 0;
        int numWars = 0;
        for (DBWar war : wars) {
            DBNation other = war.getNation(!war.isAttacker(this));
            if (other == null) continue;
            numWars++;
            totalStr += Math.pow(other.getWarIndex(), 3);
        }
        totalStr = Math.pow(totalStr / numWars, 1 / 3d);

        return totalStr;
    }

    @Command
    public double[] getRevenue() {
        double[] result = ResourceType.getBuffer();
        result[ResourceType.CASH.ordinal()] = TaxIncome + OtherIncome + CorporationIncome;
        result[ResourceType.MINERALS.ordinal()] = MineralOutput;
        result[ResourceType.PRODUCTION.ordinal()] = ProductionOutput;
        result[ResourceType.FUEL.ordinal()] = FuelOutput;
        // TODO tech
        // TODO uranium
        // TODO rare metal
        // TODO political
        return result;
    }

    @Command
    public double getRevenueConverted() {
        return ResourceType.convertedTotal(getRevenue());
    }

    @Command(desc="Minimum resistance of self in current active wars")
    public double minWarResistance() { // Placeholders.PlaceholderCache<DBNation> cache
        if (getNumWars() == 0) return 100;
        double min = 100;
        for (DBWar war : getActiveWars()) {
            Map.Entry<Double, Double> warRes = war.getResistance();
            double myRes = war.isAttacker(this) ? warRes.getKey() : warRes.getValue();
            if (myRes < min) min = myRes;
        }
        return min;
    }

    @Command(desc = "Relative strength compared to enemies its fighting (1 = equal)")
    public double getRelativeStrength() {
        return getRelativeStrength(true);
    }

    public double getRelativeStrength(boolean inactiveIsLoss) {
        if (active_m() > 7200 && inactiveIsLoss) return 0;

        double myStr = getWarIndex();
        double enemyStr = getEnemyStrength();

        return myStr / enemyStr;
    }

    public Auth auth = null;

    public Auth getAuth() {
        return getAuth(true);
    }

    @Command(desc = "Effective strength of the strongest nation this nation is attacking (offensive war)")
    public double getStrongestOffEnemyOfScore(double minScore, double maxScore) {
        Set<DBWar> wars = getActiveOffensiveWars();
        double strongest = -1;
        for (DBWar war : wars) {
            DBNation other = war.getNation(!war.isAttacker(this));
            if (other == null || other.active_m() > 2440 || other.isVacation()) continue;
//            if (filter.test(other.getScore())) {
            if (other.getScore() >= minScore && other.getScore() <= maxScore) {
                strongest = Math.max(strongest, other.getWarIndex());
            }
        }
        return strongest;
    }

    @Command(desc = "Effective strength of the strongest nation this nation is fighting")
    public double getStrongestEnemy() {
        double val = getStrongestEnemyOfScore(0, Double.MAX_VALUE);
        return val == -1 ? 0 : val;
    }

    @Command(desc = "Relative strength of the strongest nation this nation is fighting (1 = equal)")
    public double getStrongestEnemyRelative() {
        double enemyStr = getStrongestEnemy();
        double myStrength = getWarIndex();
        return myStrength == 0 ? 0 : enemyStr / myStrength;
    }

    @Command(desc = "Get the effective military strength of the strongegst nation within the provided score range")
    public double getStrongestEnemyOfScore(double minScore, double maxScore) {
        Set<DBWar> wars = getActiveWars();
        double strongest = -1;
        for (DBWar war : wars) {
            DBNation other = war.getNation(!war.isAttacker(this));
            if (other == null || other.active_m() > 2440 || other.isVacation()) continue;
            if (other.getScore() >= minScore && other.getScore() <= maxScore) {
                strongest = Math.max(strongest, other.getWarIndex());
            }
        }
        return strongest;
    }

    @Command(desc = "If this nation has an offensive war against an enemy in the provided score range")
    public boolean isAttackingEnemyOfScore(double minScore, double maxScore) {
        return getStrongestOffEnemyOfScore(minScore, maxScore) != -1;
    }

    @Command(desc = "If this nation has a war with an enemy in the provided score range")
    public boolean isFightingEnemyOfScore(double minScore, double maxScore) {
        return getStrongestEnemyOfScore(minScore, maxScore) != -1;
    }

    public Auth getAuth(boolean throwError) {
        if (this.auth != null && !this.auth.isValid()) this.auth = null;
        if (this.auth != null) return auth;
        synchronized (this) {
            if (auth == null) {
                if (this.NationId == Locutus.loader().getNationId()) {
                    if (!Settings.INSTANCE.USERNAME.isEmpty() && !Settings.INSTANCE.PASSWORD.isEmpty()) {
                        return auth = new Auth(NationId, Settings.INSTANCE.USERNAME, Settings.INSTANCE.PASSWORD);
                    }
                }
                Map.Entry<String, String> pass = Locutus.imp().getDiscordDB().getUserPass2(NationId);
                if (pass == null) {
                    RegisteredUser dbUser = getDBUser();
                    if (dbUser != null) {
                        pass = Locutus.imp().getDiscordDB().getUserPass2(dbUser.getDiscordId());
                        if (pass != null) {
                            Locutus.imp().getDiscordDB().addUserPass2(NationId, pass.getKey(), pass.getValue());
                            Locutus.imp().getDiscordDB().logout(dbUser.getDiscordId());
                        }
                    }
                }
                if (pass != null) {
                    auth = new Auth(NationId, pass.getKey(), pass.getValue());
                }
            }
        }
        if (auth == null && throwError) {
            throw new IllegalArgumentException("Please authenticate using " + CM.credentials.login.cmd.toSlashMention() + "");
        }
        return auth;
    }


    @Command(desc = "Raw positional value (0 = remove, 1 = applicant, 2 = member, 3 = leader)")
    public int getPosition() {
        return rank.id;
    }

    @Command(desc = "Alliance position enum id\n" +
            "0 = None or Removed\n" +
            "1 = Applicant\n" +
            "2 = Member\n" +
            "3 = Leader")
    public Rank getPositionEnum() {
        return rank;
    }

    public void setPosition(Rank rank) {
        this.rank = rank;
    }

    public void setMeta(NationMeta key, byte value) {
        setMeta(key, new byte[] {value});
    }

    public void setMeta(NationMeta key, int value) {
        setMeta(key, ByteBuffer.allocate(4).putInt(value).array());
    }

    public void setMeta(NationMeta key, long value) {
        setMeta(key, ByteBuffer.allocate(8).putLong(value).array());
    }

    public void setMeta(NationMeta key, double value) {
        setMeta(key, ByteBuffer.allocate(8).putDouble(value).array());
    }

    public void setMeta(NationMeta key, String value) {
        setMeta(key, value.getBytes(StandardCharsets.ISO_8859_1));
    }

    public boolean setMetaRaw(int id, byte[] value) {
        Int2ObjectOpenHashMap<byte[]> metaCache = (this.metaCache == null ? (this.metaCache = new Int2ObjectOpenHashMap<>()) : this.metaCache);
        boolean changed = false;
        byte[] existing = metaCache.get(id);
        changed = existing == null || !Arrays.equals(existing, value);
        if (changed) {
            metaCache.put(id, value);
            return true;
        }
        return false;
    }

    public void setMeta(NationMeta key, byte... value) {
        if (setMetaRaw(key.ordinal(), value)) {
            Locutus.imp().getNationDB().setMeta(getNation_id(), key, value);
        }
    }

    public ByteBuffer getMeta(NationMeta key) {
        if (metaCache == null) {
            return null;
        }
        byte[] result = metaCache.get(key.ordinal());
        return result == null ? null : ByteBuffer.wrap(result);
    }

    public void deleteMeta(NationMeta key) {
        if (this.metaCache != null) {
            if (this.metaCache.remove(key.ordinal()) != null) {
                Locutus.imp().getNationDB().deleteMeta(NationId, key);
            }
        }
    }

    public double[] getNetDeposits(GuildDB db) throws IOException {
        return getNetDeposits(db, 0L);
    }

    public double[] getNetDeposits(GuildDB db, boolean includeGrants) throws IOException {
        return getNetDeposits(db, null, true, includeGrants, 0L, 0L);
    }

    public double[] getNetDeposits(GuildDB db, boolean includeGrants, long updateThreshold) throws IOException {
        return getNetDeposits(db, null, true, includeGrants, updateThreshold, 0L);
    }

    public double[] getNetDeposits(GuildDB db, long updateThreshold) throws IOException {
        return getNetDeposits(db, null, true, updateThreshold, 0L);
    }

    public double[] getNetDeposits(GuildDB db, Set<Long> tracked, boolean offset, long updateThreshold, long cutOff) throws IOException {
        return getNetDeposits(db, tracked, offset, true, updateThreshold, cutOff);
    }

    public double[] getNetDeposits(GuildDB db, Set<Long> tracked, boolean offset, boolean includeGrants, long updateThreshold, long cutOff) throws IOException {
        Map<DepositType, double[]> result = getDeposits(db, tracked, offset, updateThreshold, cutOff);
        double[] total = new double[ResourceType.values.length];
        for (Map.Entry<DepositType, double[]> entry : result.entrySet()) {
            if (includeGrants || entry.getKey() != DepositType.GRANT) {
                double[] value = entry.getValue();
                for (int i = 0; i < value.length; i++) total[i] += value[i];
            }
        }
        return total;
    }

    public List<BankTransfer> updateTransactions() {
        BankDB db = Locutus.imp().getBankDB();
        db.updateBankTransfers(getAlliance());
        return db.getTransactionsByNation(NationId);
    }

    @Command(desc = "Days since the last three consecutive daily logins")
    public long daysSince3ConsecutiveLogins() {
        return daysSinceConsecutiveLogins(1200, 3);
    }

    @Command(desc = "Days since the last four consecutive daily logins")
    public long daysSince4ConsecutiveLogins() {
        return daysSinceConsecutiveLogins(1200, 4);
    }

    @Command(desc = "Days since the last five consecutive daily logins")
    public long daysSince5ConsecutiveLogins() {
        return daysSinceConsecutiveLogins(1200, 5);
    }

    @Command(desc = "Days since the last six consecutive daily logins")
    public long daysSince6ConsecutiveLogins() {
        return daysSinceConsecutiveLogins(1200, 6);
    }

    @Command(desc = "Days since the last seven consecutive daily logins")
    public long daysSince7ConsecutiveLogins() {
        return daysSinceConsecutiveLogins(1200, 7);
    }

    @Command(desc = "Days since last specified consecutive daily logins")
    public long daysSinceConsecutiveLogins(long checkPastXDays, int sequentialDays) {
        long currentDay = TimeUtil.getDay();
        long hours = checkPastXDays * 24 + 23;
        List<Long> logins = new ArrayList<>(Locutus.imp().getNationDB().getActivityByDay(NationId, TimeUtil.getHour(getSnapshot()) - hours));
        Collections.reverse(logins);
        int tally = 0;
        long last = 0;
        for (long day : logins) {
            if (day == last - 1) {
                tally++;
            } else {
                tally = 0;
            }
            if (tally >= sequentialDays) return currentDay - day;
            last = day;
        }
        return Long.MAX_VALUE;
    }

    @Command(desc = "Number of times since this nation's creation that they have been inactive for a specified number of days")
    public int inactivity_streak(int daysInactive, long checkPastXDays) {
        long turns = checkPastXDays * 24 + 23;
        List<Long> logins = new ArrayList<>(Locutus.imp().getNationDB().getActivityByDay(NationId, TimeUtil.getHour(getSnapshot()) - turns));
        Collections.reverse(logins);

        int inactivityCount = 0;
        long last = 0;

        for (long day : logins) {
            long diff = last - day;
            if (diff > daysInactive) {
                inactivityCount++;
            }
            last = day;
        }

        return inactivityCount;
    }

    @Command(desc = "Days since last bank deposit")
    public double daysSinceLastBankDeposit() {
        return (System.currentTimeMillis() - lastBankDeposit()) / (double) TimeUnit.DAYS.toMillis(1);
    }

    @Command(desc = "Unix timestamp when last deposited in a bank")
    public long lastBankDeposit() {
        if (getPositionEnum().id <= Rank.APPLICANT.id) return 0;
        List<BankTransfer> transactions = getTransactions(Long.MAX_VALUE);
        long max = 0;
        for (Transaction2 transaction : transactions) {
            if (transaction.receiver_id == AllianceId && transaction.isReceiverAA()) max = Math.max(max, transaction.tx_datetime);
        }
        return max;
    }

    @Command(desc = "Days since they last withdrew from their own deposits")
    public double daysSinceLastSelfWithdrawal() {
        return (System.currentTimeMillis() - lastSelfWithdrawal()) / (double) TimeUnit.DAYS.toMillis(1);
    }

    @Command(desc = "Unix timestamp when they last withdrew from their own deposits")
    public long lastSelfWithdrawal() {
        if (getPositionEnum().id <= Rank.APPLICANT.id) return 0;
        List<BankTransfer> transactions = getTransactions(Long.MAX_VALUE);
        long max = 0;
        for (Transaction2 transaction : transactions) {
            if (transaction.isSelfWithdrawal(this)) {
                max = Math.max(max, transaction.tx_datetime);
            }
        }
        return max;
    }

    /**
     * @return
     */
    public List<BankTransfer> getTransactions(long updateMs) {
        ByteBuffer updated = getMeta(NationMeta.BANK_TRANSFER_LAST_UPDATED);
        long updateDate = updated == null ? 0 : updated.getLong();
        if (updateDate < updateMs) {
            return updateTransactions();
        }
        return Locutus.imp().getBankDB().getTransactionsByNation(NationId);
    }

    public Map<Long, Long> getLoginNotifyMap() {
        ByteBuffer existing = getMeta(NationMeta.LOGIN_NOTIFY);
        Map<Long, Long> existingMap = new LinkedHashMap<>();
        if (existing != null) {
            while (existing.hasRemaining()) {
                existingMap.put(existing.getLong(), existing.getLong());
            }
        } else {
            return null;
        }
        existingMap.entrySet().removeIf(e -> e.getValue() < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5));
        return existingMap;
    }

    public void setLoginNotifyMap(Map<Long, Long> map) {
        ByteBuffer buffer = ByteBuffer.allocate(map.size() * 16);
        map.forEach((k, v) -> buffer.putLong(k).putLong(v));
        setMeta(NationMeta.LOGIN_NOTIFY, buffer.array());
    }

    public Map<ResourceType, Double> getStockpile(long updateThreshold) {
        ApiKeyPool pool;
        ApiKeyPool.ApiKey myKey = getApiKey(false);

        DBAlliance alliance = getAlliance();
        if (myKey != null) {
            pool  = ApiKeyPool.create(myKey);
        } else if (getPositionEnum().id <= Rank.APPLICANT.id || alliance == null) {
            throw new IllegalArgumentException("Nation " + NationName + " is not member in an alliance");
        } else {
            pool = alliance.getApiKeys(false);
            if (pool == null) {
                throw new IllegalArgumentException("No api key found. Please use" + CM.credentials.addApiKey.cmd.toSlashMention() + "");
            }
        }
        return getPrivateData().getStockpile(updateThreshold);
    }

    @Command(desc = "Get nation deposits")
    @RolePermission(Roles.ECON)
    public Map<ResourceType, Double> getDeposits(ValueStore store, @Me GuildDB db, @Default @Timestamp Long start, @Default @Timestamp Long end,
                                                 @Arg("Do NOT include any manual deposit offesets") @Switch("o") boolean ignoreOffsets,
                                                 @Switch("e") boolean includeExpired,
                                                 @Switch("i") boolean includeIgnored,
                                                 @Switch("d") Set<DepositType> excludeTypes
    ) {
        if (start == null) start = 0L;
        if (end == null) end = Long.MAX_VALUE;
        ScopedPlaceholderCache<DBNation> scoped = PlaceholderCache.getScoped(store, DBNation.class, "getDeposits");
        Set<Long> tracked = scoped.getGlobal(db::getTrackedBanks);
        List<Map.Entry<Integer, Transaction2>> transactions = getTransactions(db, tracked, !ignoreOffsets, !ignoreOffsets, -1L, start);
//        if (filter != null) {
//            transactions.removeIf(f -> !filter.test(f.getValue()));
//        }
        Map<DepositType, double[]> sum = DNS.sumNationTransactions(db, null, transactions, includeExpired, includeIgnored, null);
        double[] total = ResourceType.getBuffer();
        for (Map.Entry<DepositType, double[]> entry : sum.entrySet()) {
            if (excludeTypes != null && excludeTypes.contains(entry.getKey())) continue;
            total = ResourceType.add(total, entry.getValue());
        }
        return ResourceType.resourcesToMap(total);
    }

    public ApiKeyPool.ApiKey getApiKey(boolean dummy) {
        return Locutus.imp().getDiscordDB().getApiKey(NationId);
    }

    /**
     * process needs to do the following
     *  - check that the receiver / sender is a tracked bank
     *  - process tax base rates
     * @param db
     * @param tracked null if using alliance defaults
     * @param updateThreshold use 0l for force update
     * @return
     */
    public Map<DepositType, double[]> getDeposits(GuildDB db, Set<Long> tracked, boolean offset, long updateThreshold, long cutOff) {
        return getDeposits(db, tracked, offset, updateThreshold, cutOff, false, false, f -> true);
    }
    public Map<DepositType, double[]> getDeposits(GuildDB db, Set<Long> tracked, boolean offset, long updateThreshold, long cutOff, boolean forceIncludeExpired, boolean forceIncludeIgnored, Predicate<Transaction2> filter) {
        List<Map.Entry<Integer, Transaction2>> transactions = getTransactions(db, tracked, offset, updateThreshold, cutOff);
        Map<DepositType, double[]> sum = DNS.sumNationTransactions(db, tracked, transactions, forceIncludeExpired, forceIncludeIgnored, filter);
        return sum;
    }

    public List<Map.Entry<Integer, Transaction2>> getTransactions(GuildDB db, Set<Long> tracked, boolean offset, long updateThreshold, long cutOff) {
        return getTransactions(db, tracked, true, offset, updateThreshold, cutOff);
    }

    public List<Map.Entry<Integer, Transaction2>> getTransactions(GuildDB db, Set<Long> tracked, boolean useTaxBase, boolean offset, long updateThreshold, long cutOff) {
        if (tracked == null) {
            tracked = db.getTrackedBanks();
//        } else {
//            tracked = DNS.expandCoalition(tracked);
        }
        List<Transaction2> transactions = new ArrayList<>();
        if (offset) {
            List<BankTransfer> offsets = db.getDepositOffsetTransactions(getNation_id());
            transactions.addAll(offsets);
        }

        List<BankTransfer> records = getTransactions(updateThreshold);
        transactions.addAll(records);

        List<Map.Entry<Integer, Transaction2>> result = new ArrayList<>();

        outer:
        for (Transaction2 record : transactions) {
            if (record.tx_datetime < cutOff) continue;

            Long otherId = null;

            if (((record.isSenderGuild() || record.isSenderAA()) && tracked.contains(record.sender_id))
                    || (record.sender_type == 0 && record.sender_id == 0 && record.tx_id == -1)) {
                otherId = record.sender_id;
            } else if (((record.isReceiverGuild() || record.isReceiverAA()) && tracked.contains(record.receiver_id))
                    || (record.receiver_type == 0 && record.receiver_id == 0 && record.tx_id == -1)) {
                otherId = record.receiver_id;
            }

            if (otherId == null) continue;

            int sign;
            if (record.sender_id == NationId && record.sender_type == 1) {
                sign = 1;
            } else {
                sign = -1;
            }

            result.add(new AbstractMap.SimpleEntry<>(sign, record));
        }

        return result;
    }

    @Command(desc = "Nation ID")
    public int getNation_id() {
        return NationId;
    }

    public void setNation_id(int nation_id) {
        this.NationId = nation_id;
    }

    @Command(desc = "Nation Name")
    public String getNation() {
        return NationName;
    }

    public void setNation(String nation) {
        this.NationName = nation;
    }

    @Command(desc = "Leader name")
    public String getLeader() {
        return LeaderName;
    }

    public void setLeader(String leader) {
        this.LeaderName = leader;
    }

    @Command(desc = "Alliance ID")
    public int getAlliance_id() {
        return AllianceId;
    }

    public void setAlliance_id(int alliance_id) {
        this.AllianceId = alliance_id;
    }

    @Command(desc = "Alliance Name")
    public String getAllianceName() {
        if (AllianceId == 0) return "AA:0";
        return Locutus.imp().getNationDB().getAllianceName(AllianceId);
    }

    @Command(desc = "The alliance class")
    public DBAlliance getAlliance() {
        return getAlliance(true);
    }

    public DBAlliance getAlliance(boolean createIfNotExist) {
        if (AllianceId == 0) return null;
        if (createIfNotExist) {
            return Locutus.imp().getNationDB().getOrCreateAlliance(AllianceId);
        } else {
            return Locutus.imp().getNationDB().getAlliance(AllianceId);
        }
    }

    public void setActive_m(int active_m) {
        this.LastOnline = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(active_m);
    }

    @Command(desc = "Nation Score (ns)")
    public double getScore() {
        return Score;
    }

    public void setScore(double score) {
        this.Score = score;
    }

    @Command(desc = "Total infra in all cities")
    public double getInfra() {
        return Infra;
    }


    @Command(desc = "Total population in all cities")
    public double getPopulation() {
        return Pop;
    }

    @Command(desc = "Number of turns in Vacation Mode (VM)")
    public boolean isVacation() {
        return System.currentTimeMillis() - this.LastOnline > TimeUnit.DAYS.toMillis(30);
    }

    @Command(desc = "Number of active offensive wars")
    public int getOff() {
        return OffWars;
    }

    @Command(desc = "All time offensive wars involved in")
    public int getAllTimeOffensiveWars() {
        return (int) getWars().stream().filter(f -> f.getAttacker_id() == NationId).count();
    }

    @Command(desc = "All time defensive wars involved in")
    public int getAllTimeDefensiveWars() {
        return (int) getWars().stream().filter(f -> f.getDefender_id() == NationId).count();
    }

    @Command
    public Map.Entry<Integer, Integer> getAllTimeOffDefWars() {
        Set<DBWar> wars = getWars();
        int off = (int) wars.stream().filter(f -> f.getAttacker_id() == NationId).count();
        int def = (int) wars.stream().filter(f -> f.getDefender_id() == NationId).count();
        return new AbstractMap.SimpleEntry<>(off, def);
    }

    @Command(desc = "All time wars involved in")
    public int getAllTimeWars() {
        return getWars().size();
    }

    @Command(desc = "All time wars against active nations")
    public int getNumWarsAgainstActives() {
        if (getNumWars() == 0) return 0;
        int total = 0;
        for (DBWar war : getActiveWars()) {
            DBNation other = war.getNation(!war.isAttacker(this));
            if (other != null && other.active_m() < 4880) total++;
        }
        return total;
    }

    @Command(desc = "Number of active offensive and defensive wars")
    public int getNumWars() {
        return getActiveWars().size();
    }

    @Command(desc = "Number of offensive and defensive wars since date")
    public int getNumWarsSince(long date) {
        return Locutus.imp().getWarDb().countWarsByNation(NationId, date, getSnapshot());
    }

    @Command(desc = "Number of offensive wars since date")
    public int getNumOffWarsSince(long date) {
        return Locutus.imp().getWarDb().countOffWarsByNation(NationId, date, getSnapshot() == null ? Long.MAX_VALUE : getSnapshot());
    }

    @Command(desc = "Number of defensive wars since date")
    public int getNumDefWarsSince(long date) {
        return Locutus.imp().getWarDb().countDefWarsByNation(NationId, date, getSnapshot() == null ? Long.MAX_VALUE : getSnapshot());
    }

    @Command(desc = "Number of active defensive wars")
    public int getDef() {
        return (int) getActiveWars().stream().filter(f -> f.getDefender_id() == NationId).count();
    }

    @Command(desc = "Unix timestamp of date created")
    public long getDate() {
        return DateOfJoining;
    }

    public void setDate(long date) {
        this.DateOfJoining = date;
    }

    @Command(desc = "Set of nation ids fighting this nation")
    public Set<Integer> getEnemies() {
        return getActiveWars().stream()
                .map(dbWar -> dbWar.getAttacker_id() == getNation_id() ? dbWar.getDefender_id() : dbWar.getAttacker_id())
                .collect(Collectors.toSet());
    }

    @Command(desc = "If a specified nation is within this nations espionage range")
    public boolean isInSpyRange(DBNation other) {
        return DNS.isInScoreRange(getScore(), other.getScore(), false, isInactiveForWar());
    }

    @Command(desc = "Number of milliseconds inactive")
    public long getInactiveMs() {
        return System.currentTimeMillis() - LastOnline;
    }

    @Command(desc = "If inactive for war range period")
    public boolean isInactiveForWar() {
        return getInactiveMs() > TimeUnit.DAYS.toMillis(14);
    }

    public int active_m() {
        long now = getSnapshot() == null ? System.currentTimeMillis() : getSnapshot();
        return (int) TimeUnit.MILLISECONDS.toMinutes(now - lastActiveMs());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Integer i) {
            return i.equals(NationId);
        }
        if (o instanceof ArrayUtil.IntKey key) {
            return key.key == NationId;
        }
        if (o == null || getClass() != o.getClass()) return false;

        DBNation nation = (DBNation) o;

        return NationId == nation.NationId;
    }

    @Override
    public int hashCode() {
        return NationId;
    }

    public String toMarkdown() {
        return toMarkdown(false);
    }

    public String toMarkdown(boolean war) {
        return toMarkdown(war, true, true, true, true);
    }
    public String toMarkdown(boolean war, boolean showOff, boolean showSpies, boolean showInfra, boolean spies) {
        StringBuilder response = new StringBuilder();
        if (war) {
            response.append("<" + Settings.INSTANCE.DNS_URL() + "/nation/" + getNation_id() + ">");
        } else {
            response.append("<" + Settings.INSTANCE.DNS_URL() + "/nation/" + getNation_id() + ">");
        }
        // TODO FIXME :||remove marktdown who !!important
//        String beigeStr = null;
//        if (color == NationColor.BEIGE) {
//            int turns = getBeigeTurns();
//            long diff = TimeUnit.MILLISECONDS.toMinutes(TimeUtil.getTimeFromTurn(TimeUtil.getTurn() + turns) - System.currentTimeMillis());
//            beigeStr = TimeUtil.secToTime(TimeUnit.MINUTES, diff);
//        }
//        int vm = getVm_turns();
//
//        response.append(" | " + String.format("%16s", getNation()))
//                .append(" | " + String.format("%16s", getAllianceName()))
//                .append(alliance_id != 0 && getPositionEnum() == Rank.APPLICANT ? " applicant" : "")
//                .append(color == NationColor.BEIGE ? " beige:" + beigeStr : "")
//                .append(vm > 0 ? " vm=" + TimeUtil.secToTime(TimeUnit.HOURS, vm) : "")
//                .append("\n```")
//                .append(String.format("%5s", (int) getScore())).append(" ns").append(" | ")
//                .append(String.format("%10s", TimeUtil.secToTime(TimeUnit.MINUTES, active_m()))).append(" \uD83D\uDD52").append(" | ")
//                .append(String.format("%2s", getCities())).append(" \uD83C\uDFD9").append(" | ");
//                if (showInfra) response.append(String.format("%5s", (int) getAvg_infra())).append(" \uD83C\uDFD7").append(" | ");
//                response.append(String.format("%6s", getSoldiers())).append(" \uD83D\uDC82").append(" | ")
//                .append(String.format("%5s", getTanks())).append(" \u2699").append(" | ")
//                .append(String.format("%5s", getAircraft())).append(" \u2708").append(" | ")
//                .append(String.format("%4s", getShips())).append(" \u26F5");
//                if (showOff) response.append(" | ").append(String.format("%1s", getOff())).append(" \uD83D\uDDE1");
//                response.append(" | ").append(String.format("%1s", getDef())).append(" \uD83D\uDEE1");
//                if (showSpies) response.append(" | ").append(String.format("%2s", getSpies())).append(" \uD83D\uDD0D");
//                response.append("```");
        return response.toString();
    }

    public String toMarkdown(boolean title, boolean general, boolean military) {
        return toMarkdown(true, false, title, general, military, true);
    }

    @Command(desc = "Days since creation")
    public int getAgeDays() {
        if (getDate() == 0) return 0;
        return (int) TimeUnit.MILLISECONDS.toDays((getSnapshot() != null ? getSnapshot() : System.currentTimeMillis()) - DateOfJoining);
    }

    @Command(desc = "Decimal pct of days they login")
    public double avg_daily_login() {
        int turns = Math.min(24 * 14, Math.max(1, (getAgeDays() - 1) * 24));;
        Activity activity = getActivity(turns);
        double[] arr = activity.getByDay();
        double total = 0;
        for (double v : arr) total += v;
        return total / arr.length;
    }

    @Command(desc = "Decimal pct of days they login in the past week")
    public double avg_daily_login_week() {
        int turns = Math.min(24 * 7, Math.max(1, (getAgeDays() - 1) * 24));
        Activity activity = getActivity(turns);
        double[] arr = activity.getByDay();
        double total = 0;
        for (double v : arr) total += v;
        return total / arr.length;
    }

    @Command(desc = "Decimal pct of turns they login")
    public double avg_daily_login_turns() {
        Activity activity = getActivity(24 * 14);
        double[] arr = activity.getByDayTurn();
        double total = 0;
        for (double v : arr) total += v;
        return total / arr.length;
    }

    @Command(desc = "Decimal pct of times they login during UTC daychange")
    public double login_daychange() {
        Activity activity = getActivity(24 * 14);
        double[] arr = activity.getByDayTurn();
        return (arr[0] + arr[arr.length - 1]);
    }

    @Command(desc = "The alliance tax rate necessary to break even when distributing raw resources")
    @RolePermission(value = Roles.ECON)
    public double equilibriumTaxRate() {
        double[] revenue = getRevenue();
        double consumeCost = 0;
        double taxable = 0;
        for (ResourceType type : ResourceType.values) {
            double value = revenue[type.ordinal()];
            if (value < 0) {
                consumeCost += ResourceType.convertedTotal(type, -value);
            } else {
                taxable += -ResourceType.convertedTotal(type, -value);
            }
        }
        if (taxable > consumeCost) {
            return 100 * consumeCost / taxable;
        }
        return Double.NaN;
    }

    public String getUsername() throws IOException {
        return DiscordName;
    }

    @Command(desc = "Get the number of wars with nations matching a filter")
    public int getActiveWarsWith(@NoFormat NationFilter filter) {
        int count = 0;
        for (DBWar war : getActiveWars()) {
            DBNation other = war.getNation(!war.isAttacker(this));
            if (other != null && filter.test(other)) count++;
        }
        return count;
    }

    public RegisteredUser getDBUser() {
        return Locutus.imp().getDiscordDB().getUserFromNationId(NationId);
    }

    @Command(desc = "If registered with this Bot")
    public boolean isVerified() {
        return getDBUser() != null;
    }

    @Command(desc = "If in the discord guild for their alliance")
    public boolean isInAllianceGuild() {
        GuildDB db = Locutus.imp().getGuildDBByAA(AllianceId);
        if (db != null) {
            User user = getUser();
            if (user != null) {
                return db.getGuild().getMember(user) != null;
            }
        }
        return false;
    }

    @Command(desc = "If in the milcom discord guild for their alliance")
    public boolean isInMilcomGuild() {
        GuildDB db = Locutus.imp().getGuildDBByAA(AllianceId);
        if (db != null) {
            Guild warGuild = db.getOrNull(GuildKey.WAR_SERVER);
            if (warGuild == null) warGuild = db.getGuild();
            User user = getUser();
            if (user != null) {
                return warGuild.getMember(user) != null;
            }
        }
        return false;
    }

    @Command(desc = "The registered discord username and user discriminator")
    public String getUserDiscriminator() {
        User user = getUser();
        if (user == null) return null;
        return DiscordUtil.getFullUsername(user);
    }

    @Command(desc = "The registered discord user id")
    public Long getUserId() {
        RegisteredUser dbUser = getDBUser();
        if (dbUser == null) return null;
        return dbUser.getDiscordId();
    }

    @Command(desc = "The registered discord user mention (or null)")
    public String getUserMention() {
        RegisteredUser dbUser = getDBUser();
        if (dbUser == null) return null;
        return "<@" + dbUser.getDiscordId() + ">";
    }

    @Command(desc = "Age of discord account in milliseconds")
    public long getUserAgeMs() {
        User user = getUser();
        return user == null ? 0 : (System.currentTimeMillis() - user.getTimeCreated().toEpochSecond() * 1000L);
    }

    @Command(desc = "Age of discord account in days")
    public double getUserAgeDays() {
        return getUserAgeMs() / (double) TimeUnit.DAYS.toMillis(1);
    }

    public User getUser() {
        RegisteredUser dbUser = getDBUser();
        return dbUser != null ? dbUser.getUser() : null;
    }

    @Command(desc = "Get the discord user object")
    public UserWrapper getDiscordUser(@Me Guild guild) {
        User user = getUser();
        if (user == null || guild == null) return null;
        Member member = guild.getMember(user);
        return member == null ? null : new UserWrapper(member);
    }

    public String toEmbedString() {
        StringBuilder response = new StringBuilder();
        RegisteredUser user = Locutus.imp().getDiscordDB().getUserFromNationId(getNation_id());
        if (user != null) {
            response.append(user.getDiscordName() + " / <@" + user.getDiscordId() + "> | ");
        }
        response.append(toMarkdown(true, false, true, true, false, false));
        response.append(toMarkdown(true, false, false, false, true, true));

        // TODO FIXME :||remove markdown !!important


        response.append(" ```")
                .append(String.format("%6s", isVacation())).append(" \uD83C\uDFD6\ufe0f").append(" | ");

//        if (color == NationColor.BEIGE) {
//            int turns = getBeigeTurns();
//            long diff = TimeUnit.MILLISECONDS.toMinutes(TimeUtil.getTimeFromTurn(TimeUtil.getTurn() + turns) - System.currentTimeMillis());
//            String beigeStr = TimeUtil.secToTime(TimeUnit.MINUTES, diff);
//            response.append(" beige:" + beigeStr);
//        } else {
//            response.append(String.format("%6s", getColor()));
//        }
//        response.append(" | ")
//                .append(String.format("%4s", getAgeDays())).append("day").append(" | ")
//                .append(String.format("%6s", getContinent()))
//                .append("```");


//        if (nation_id == Settings.INSTANCE.NATION_ID) {
//            String imageUrl = "https://cdn.discordapp.com/attachments/694201462837739563/710292110494138418/borg.jpg";
//            GuildMessageChannel channel = Locutus.imp().getDiscordApi().getGuildChannelById(channelId);
//            DiscordUtil.createEmbedCommand(channel, new Consumer<EmbedBuilder>() {
//                @Override
//                public void accept(EmbedBuilder embed) {
//                    embed.setThumbnail(imageUrl);
//                    embed.setTitle(title);
//                    embed.setDescription(response.toString());
//                }
//            }, counterEmoji, counterCmd, simEmoji, simCommand);
//        } else
        return response.toString();
    }

    @Command(desc = "Sheet lookup")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS})
    public Object cellLookup(SpreadSheet sheet, String tabName, String columnSearch, String columnOutput, String search) {
        List<List<Object>> values = sheet.loadValues(tabName, false);
        if (values == null) return null;
        int searchIndex = SheetUtil.getIndex(columnSearch) - 1;
        int outputIndex = SheetUtil.getIndex(columnOutput) - 1;

        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() > searchIndex && row.size() > outputIndex) {
                Object cellSearch = row.get(searchIndex);
                if (cellSearch == null) continue;
                cellSearch = cellSearch.toString();
                if (search.equals(cellSearch)) {
                    return row.get(outputIndex);
                }
            }
        }
        return null;
    }

    @Command(desc = "Game url for nation")
    public String getUrl() {
        return "" + Settings.INSTANCE.DNS_URL() + "/nation/" + getNation_id();
    }

    @Command(desc = "Game url for alliance")
    public String getAllianceUrl() {
        return "" + Settings.INSTANCE.DNS_URL() + "/alliance/" + getAlliance_id();
    }

    public String getMarkdownUrl() {
        return getNationUrlMarkup(true);
    }

    public String getNationUrlMarkup(boolean embed) {
        String nationUrl = getUrl();
        nationUrl = MarkupUtil.markdownUrl(NationName, "<" + nationUrl + ">");
        return nationUrl;
    }

    public String getAllianceUrlMarkup(boolean embed) {
        String allianceUrl = getAllianceUrl();
        allianceUrl = MarkupUtil.markdownUrl(getAllianceName(), "<" + allianceUrl + ">");
        return allianceUrl;
    }

    @Command
    public int getNumReports() {
        ReportManager reportManager = Locutus.imp().getNationDB().getReportManager();
        List<ReportManager.Report> reports = reportManager.loadReports(getId(), getUserId(), null, null);
        if (getSnapshot() != null) {
            reports.removeIf(report -> report.date < getSnapshot());
        }
        return reports.size();
    }

    public DnsApi getApi(boolean throwError) {
        ApiKeyPool.ApiKey apiKey = this.getApiKey(true);
        if (apiKey == null) {
            if (throwError) throw new IllegalStateException("No api key found for `" + NationName + "` Please set one: " + CM.credentials.addApiKey.cmd.toSlashMention());
            return null;
        }
        return new DnsApi(ApiKeyPool.create(apiKey));
    }

    public String toFullMarkdown() {
        StringBuilder body = new StringBuilder();
        //Nation | Leader name | timestamp(DATE_CREATED) `tax_id=1`
        body.append(getNationUrlMarkup(true)).append(" | ");
        body.append(LeaderName).append(" | ");
        // DiscordUtil.timestamp

        body.append(DiscordUtil.timestamp(DateOfJoining, null)).append("\n");
        //Alliance | PositionOrEnum(id=0,enum=0) | timestamp(Seniority)
        if (AllianceId == 0) {
            body.append("`AA:0`");
        } else {
            body.append(getAllianceUrlMarkup(true));
            Rank position = rank;
            String posStr = getPositionEnum().name();
            long dateJoined = System.currentTimeMillis() - allianceSeniorityMs();
            body.append(" | `").append(posStr).append("` | ").append(DiscordUtil.timestamp(dateJoined, null));
        }
        body.append("\n");
        User user = getUser();
        int len = body.length();
        {
            String prefix = "";
            if (user != null) {
                long created = user.getTimeCreated().toEpochSecond() * 1000L;
                body.append(user.getAsMention() + " | " + MarkupUtil.markdownUrl(DiscordUtil.getFullUsername(user), DiscordUtil.userUrl(user.getIdLong(), false)) + " | " + DiscordUtil.timestamp(created, null));
                prefix = " | ";
            }
            int reports = getNumReports();
            if (reports > 0) {
                body.append(prefix).append(reports + " reports");
            }
            if (len != body.length()) {
                body.append("\n\n");
            }
        }
        {
            double infra = getInfra();
            double pop = getPopulation();
            double land = getLand();
            body.append("I:`" + MathMan.format(infra)).append("` ");
            body.append("P:`" + MathMan.format(pop)).append("` ");
            body.append("L:`" + MathMan.format(land)).append("` ");

            body.append(" | O:").append("`").append(getOff()).append("/").append(getMaxOff()).append("` \uD83D\uDDE1\uFE0F | ");
            body.append("D:`").append(getDef()).append("/").append(2).append("` \uD83D\uDEE1\uFE0F").append(" | `").append(MathMan.format(Score) + "ns`\n");
        }
        //Domestic/War policy | beige turns | score
        long diff = ProtectionTime - System.currentTimeMillis();
        if (diff > 0) {
            body.append("Protected: ").append(TimeUtil.secToTime(TimeUnit.MILLISECONDS, diff)).append("\n");
        }
        body.append(DiscordUtil.timestamp(lastActiveMs(), null)).append(" \u23F0\n");
        //MMR[Building]: 1/2/3 | MMR[Unit]: 5/6/7
        body.append("\n");
        //VM: Timestamp(Started) - Timestamp(ends) (5 turns)
        if (isVacation()) {
            body.append("**VM**");
        }
        //
        //Attack Range: War= | Spy=
        boolean isActive = !isInactiveForWar();
        {
            double offWarMin = DNS.getAttackRange(true, true, true, isActive, Score);
            double offWarMax = DNS.getAttackRange(true, true, false, isActive, Score);
            double offSpyMin = DNS.getAttackRange(true, false, true, isActive, Score);
            double offSpyMax = DNS.getAttackRange(true, false, false, isActive, Score);
            // use MathMan.format to format doubles
            body.append("**Attack Range**: War=`").append(MathMan.format(offWarMin)).append("-").append(MathMan.format(offWarMax)).append("` | Spy=`").append(MathMan.format(offSpyMin)).append("-").append(MathMan.format(offSpyMax)).append("`\n");
        }
        //Defense Range: War= | Spy=
        {
            double defWarMin = DNS.getAttackRange(false, true, true, isActive, Score);
            double defWarMax = DNS.getAttackRange(false, true, false, isActive, Score);
            double defSpyMin = DNS.getAttackRange(false, false, true, isActive, Score);
            double defSpyMax = DNS.getAttackRange(false, false, false, isActive, Score);
            // use MathMan.format to format doubles
            body.append("**Defense Range**: War=`").append(MathMan.format(defWarMin)).append("-").append(MathMan.format(defWarMax)).append("` | Spy=`").append(MathMan.format(defSpyMin)).append("-").append(MathMan.format(defSpyMax)).append("`\n");
        }
        return body.toString();
    }

    public String toMarkdown(boolean embed, boolean war, boolean title, boolean general, boolean military, boolean spies) {
        StringBuilder response = new StringBuilder();
        if (title) {
            String nationUrl;
            if (war) {
                String url = Settings.INSTANCE.DNS_URL() + "/nation/" + getNation_id();
                nationUrl = embed ? MarkupUtil.markdownUrl(getName(), url) : "<" + url + ">";
            } else {
                nationUrl = getNationUrlMarkup(embed);
            }
            String allianceUrl = getAllianceUrlMarkup(embed);
            response
                    .append(nationUrl)
                    .append(" | ")
                    .append(allianceUrl);

            if (embed && getPositionEnum() == Rank.APPLICANT && AllianceId != 0) response.append(" (applicant)");

            if (isVacation()) {
                response.append(" | VM");
            }

            response.append('\n');
        }
        if (general || military) {
            response.append("```");
            if (general) {
                int active = active_m();
                active = active - active % (60);
                String time = active <= 1 ? "Online" : TimeUtil.secToTime(TimeUnit.MINUTES, active);
                response
                        .append(String.format("%5s", (int) getScore())).append(" ns").append(" | ")
                        .append(String.format("%6s", time)).append(" | ")
                        .append(String.format("%2s", getLand())).append(" land").append(" | ")
                        .append(String.format("%5s", getInfra())).append(" infra").append(" | ");
            }
            if (military) {
                response
                        .append(String.format("%6s", getWarIndex())).append(" warIndex").append(" | ");
            }
            if (general) {
                response
                        .append(String.format("%1s", getOff())).append("\uD83D\uDDE1").append(" | ")
                        .append(String.format("%1s", getDef())).append("\uD83D\uDEE1").append(" | ");
            }
            String str = response.toString();
            if (str.endsWith(" | ")) response = new StringBuilder(str.substring(0, str.length() - 3));
            response.append("```");
        }
        return response.toString();
    }


    @Command(desc = "Game turns left on the beige color bloc")
    public long getProtectionEnds() {
        return this.ProtectionTime;
    }

    @Command(desc = "Returns self nation ID if the nation is a reroll, otherwise 0")
    public int isReroll() {
        return isReroll(false);
    }

    public int isReroll(boolean fetchUid) {
        Map<Integer, DBNation> nations = Locutus.imp().getNationDB().getNations();
        for (Map.Entry<Integer, DBNation> entry : nations.entrySet()) {
            int otherId = entry.getKey();
            DBNation otherNation = entry.getValue();
            if (otherNation.getDate() == 0) continue;

            if (otherId > NationId && Math.abs(otherNation.getDate()  - getDate()) > TimeUnit.DAYS.toMillis(14)) {
                return NationId;
            }
        }
        return 0;
    }

    public NationMeta.ProtectionAlertMode getBeigeAlertMode(NationMeta.ProtectionAlertMode def) {
        ByteBuffer value = getMeta(NationMeta.PROTECTION_ALERT_MODE);
        if (value == null) {
            return def;
        }
        return NationMeta.ProtectionAlertMode.values()[value.get()];
    }

    public double getBeigeAlertRequiredLoot() {
        double requiredLoot = 0;
        ByteBuffer requiredLootBuf = getMeta(NationMeta.PROTECTION_ALERT_REQUIRED_LOOT);
        if (requiredLootBuf != null) {
            requiredLoot = requiredLootBuf.getDouble();
        }
        return requiredLoot;
    }

    public NationMeta.BeigeAlertRequiredStatus getBeigeRequiredStatus(NationMeta.BeigeAlertRequiredStatus def) {
        ByteBuffer value = getMeta(NationMeta.PROTECTION_ALERT_REQUIRED_STATUS);
        if (value == null) {
            return def;
        }
        return NationMeta.BeigeAlertRequiredStatus.values()[value.get()];
    }

    @Command(desc = "If this nation is not daily active and lost their most recent war")
    public boolean lostInactiveWar() {
        if (active_m() < 2880) return false;
        DBWar lastWar = Locutus.imp().getWarDb().getLastOffensiveWar(NationId, getSnapshot());
        if (lastWar != null && lastWar.getDefender_id() == NationId && lastWar.getStatus() == WarStatus.ATTACKER_VICTORY) {
            long now = getSnapshot() == null ? System.currentTimeMillis() : getSnapshot();
            long lastActiveCutoff = now - TimeUnit.MINUTES.toMillis(Math.max(active_m() + 1220, 7200));
            if (lastWar.getDate() > lastActiveCutoff) return true;
        }
        return false;
    }

    @Command(desc = "Alliance rank by score")
    public int getAllianceRank(@NoFormat @Default NationFilter filter) {
        if (AllianceId == 0) return Integer.MAX_VALUE;
        return getAlliance().getRank(filter);
    }

    @Command(desc = "If on the correct MMR for their alliance (if one is set)")
    @RolePermission(Roles.MEMBER)
    public boolean correctAllianceMMR(@Me GuildDB db) {
        if (!db.isAllianceId(AllianceId)) throw new IllegalArgumentException("Not in this guilds alliance");
        if (getPosition() <= 1 || isVacation()) return true;
        long updateMs = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(3);
        return db.hasRequiredMMR(this, updateMs);
    }

    public Activity getActivity() {
        return new Activity(getNation_id());
    }

    public Activity getActivity(long turns) {
        long now = TimeUtil.getHour();
        return new Activity(getNation_id(), now - turns, Long.MAX_VALUE);
    }

    public Activity getActivity(long turnStart, long turnEnd) {
        return new Activity(getNation_id(), turnStart, turnEnd);
    }

    @Command(desc = "If fighting a war against another active nation")
    public boolean isFightingActive() {
        if (getDef() > 0) return true;
        if (getOff() > 0) {
            for (DBWar activeWar : this.getActiveWars()) {
                DBNation other = activeWar.getNation(!activeWar.isAttacker(this));
                if (other != null && other.active_m() < 1440 && other.isVacation() == false) return true;
            }
        }
        return false;
    }

    @Command(desc = "Discord online status")
    public OnlineStatus getOnlineStatus() {
        User user = getUser();
        if (user != null) {
            for (Guild guild : user.getMutualGuilds()) {
                Member member = guild.getMember(user);
                if (member != null) {
                    return member.getOnlineStatus();
                }
            }
        }
        return OnlineStatus.OFFLINE;
    }

    @Command(desc = "If online ingame or discord")
    public boolean isOnline() {
        if (active_m() < 60) return true;
        OnlineStatus status = getOnlineStatus();
        return status == OnlineStatus.ONLINE || status == OnlineStatus.DO_NOT_DISTURB;
    }

    public Set<DBWar> getActiveWars() {
        if (getSnapshot() != null) {
            long end = getSnapshot();
            long start = end - TimeUnit.DAYS.toMillis(4);
            Set<DBWar> wars = Locutus.imp().getWarDb().getWarsByNationMatching(NationId, f -> f.getDate() > start && f.getDate() < end);
            return wars;
        }
        return Locutus.imp().getWarDb().getActiveWars(NationId);
    }

    public Set<DBWar> getActiveOffensiveWars() {
        Set<DBWar> myWars = getActiveWars();
        if (myWars.isEmpty()) return Collections.emptySet();
        Set<DBWar> result = new ObjectOpenHashSet<>(myWars);
        result.removeIf(f -> f.getAttacker_id() != NationId);
        return result;
    }

    public Set<DBWar> getActiveDefensiveWars() {
        Set<DBWar> myWars = getActiveWars();
        if (myWars.isEmpty()) return Collections.emptySet();
        Set<DBWar> result = new ObjectOpenHashSet<>(myWars);
        result.removeIf(f -> f.getDefender_id() != NationId);
        return result;
    }

    public Set<DBWar> getWars() {
        if (getSnapshot() != null) {
            return Locutus.imp().getWarDb().getWarsByNationMatching(NationId, f -> f.getDate() < getSnapshot());
        }
        return Locutus.imp().getWarDb().getWarsByNation(NationId);
    }

    public List<AllianceChange> getAllianceHistory(Long date) {
        return Locutus.imp().getNationDB().getRemovesByNation(getNation_id(), date);
    }

    public AllianceChange getPreviousAlliance(boolean ignoreApplicant, Long date) {
        return Locutus.imp().getNationDB().getPreviousAlliance(NationId, AllianceId);
    }

    public String getWarInfoEmbed() {
        return getWarInfoEmbed(false);
    }

    public String getWarInfoEmbed(DBWar war, boolean loot) {
        return war.getWarInfoEmbed(war.isAttacker(this), loot);
    }

    public String getWarInfoEmbed(boolean loot) {
        StringBuilder body = new StringBuilder();
        Set<DBWar> wars = this.getActiveWars();

        for (DBWar war : wars) {
            body.append(getWarInfoEmbed(war, loot));
        }
        body.append(this.getNationUrlMarkup(true));
//        body.append("\n").append(this.toCityMilMarkdown());
        // TODO FIXME :||remove war info !!important
        return body.toString().replaceAll(" \\| ","|");
    }

    @Command(desc = "Get the number of active wars with a list of nations")
    public int getFighting(@NoFormat Set<DBNation> nations) {
        if (nations == null) return getNumWars();
        int count = 0;
        for (DBWar war : getActiveWars()) {
            DBNation other = war.getNation(!war.isAttacker(this));
            if (nations.contains(other)) {
                count++;
            }
        }
        return count;
    }

    @Command(desc = "Get the number of active offensive wars with a list of nations")
    public int getAttacking(@NoFormat Set<DBNation> nations) {
        if (nations == null) return getNumWars();
        int count = 0;
        for (DBWar war : getActiveOffensiveWars()) {
            DBNation other = war.getNation(false);
            if (nations.contains(other)) {
                count++;
            }
        }
        return count;
    }

    @Command(desc = "Get the number of active defensive wars with a list of nations")
        public int getDefending(@NoFormat Set<DBNation> nations) {
        if (nations == null) return getNumWars();
        int count = 0;
        for (DBWar war : getActiveDefensiveWars()) {
            DBNation other = war.getNation(true);
            if (nations.contains(other)) {
                count++;
            }
        }
        return count;
    }

    @Command(desc = "Can perform a spy attack against a nation of score")
    public boolean canSpyOnScore(double score) {
        double min = DNS.getAttackRange(true, false, true, true, this.Score);
        double max = DNS.getAttackRange(true, false, false, true, this.Score);
        return score >= min && score <= max;
    }

    @Command(desc = "Can be spied by a nation of score")
    public boolean canBeSpiedByScore(double score) {
        double min = DNS.getAttackRange(false, false, true, true, this.Score);
        double max = DNS.getAttackRange(false, false, false, true, this.Score);
        return score >= min && score <= max;
    }

    @Command(desc = "Can declare war on a nation of score")
        public boolean canDeclareOnScore(double score) {
        double min = DNS.getAttackRange(true, true, true, true, this.Score);
        double max = DNS.getAttackRange(true, true, false, true, this.Score);
        return score >= min && score <= max;
    }

    @Command(desc = "Can be declared on by a nation of score")
    public boolean canBeDeclaredOnByScore(double score) {
        double min = DNS.getAttackRange(false, true, true, true, this.Score);
        double max = DNS.getAttackRange(false, true, false, true, this.Score);
        return score >= min && score <= max;
    }

    @Command(desc = "If this nation is in a nation list")
    public boolean isIn(@NoFormat Set<DBNation> nations) {
        return nations.contains(this);
    }

    @Command(desc = "If this nation is in the enemies coalition")
    public boolean isEnemy(@Me GuildDB db) {
        if (AllianceId == 0) return false;
        return db.getCoalition(Coalition.ENEMIES).contains(AllianceId);
    }

    public long getRerollDate() {
        List<DBNation> nations = new ArrayList<>(Locutus.imp().getNationDB().getNations().values());
        int previousNationId = -1;
        for (DBNation nation : nations) {
            if (nation.getNation_id() < NationId) {
                previousNationId = Math.max(previousNationId, nation.getNation_id());
            }
        }
        int finalPreviousNationId = previousNationId;
        nations.removeIf(f -> f.getNation_id() < finalPreviousNationId);
        // sort nations by nation_id
        nations.sort(Comparator.comparingInt(DBNation::getNation_id));

        long minDate = Long.MAX_VALUE;
        for (int i = 1; i < nations.size() - 1; i++) {
            DBNation nation = nations.get(i);
            if (nation.NationId <= NationId) continue;
            if (nation.DateOfJoining < DateOfJoining) {
                DBNation previous = nations.get(i - 1);
                if (previous.DateOfJoining < nation.DateOfJoining) {
                    // valid
                    minDate = Math.min(nation.DateOfJoining, minDate);
                }
            }
        }
        if (Math.abs(DateOfJoining - minDate) > TimeUnit.DAYS.toMillis(10)) {
            return Long.MAX_VALUE;
        }
        return minDate;
    }

    public boolean sendDM(String msg) {
        return sendDM(msg, null);
    }

    public boolean sendDM(String msg, Consumer<String> errors) {
        User user = getUser();
        if (user == null) return false;

        try {
            RateLimitUtil.queue(RateLimitUtil.complete(user.openPrivateChannel()).sendMessage(msg));
        } catch (Throwable e) {
            if (errors != null) {
                errors.accept(e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Command(desc = "Number of wars matching a filter")
    public int countWars(@NoFormat Predicate<DBWar> warFilter) {
        return (int) getWars().stream().filter(warFilter).count();
    }

    @Command(desc = "Days since their last offensive war")
    public double daysSinceLastOffensive() {
        if (getOff() > 0) return 0;
        DBWar last = Locutus.imp().getWarDb().getLastOffensiveWar(NationId, getSnapshot());
        if (last != null) {
            long now = getSnapshot() == null ? System.currentTimeMillis() : getSnapshot();
            long diff = now - last.getDate();
            return ((double) diff) / TimeUnit.DAYS.toMillis(1);
        }
        return Integer.MAX_VALUE;
    }

    @Command(desc = "Days since last defensive war")
    public double daysSinceLastDefensiveWarLoss() {
        long maxDate = 0;
        for (DBWar war : getWars()) {
            if (war.getDefender_id() == NationId && war.getStatus() == WarStatus.ATTACKER_VICTORY) {
                maxDate = Math.max(maxDate, war.getDate());
            }
        }
        long now = getSnapshot() == null ? System.currentTimeMillis() : getSnapshot();
        return maxDate == 0 ? Integer.MAX_VALUE : ((double) (now - maxDate)) / TimeUnit.DAYS.toMillis(1);
    }

    @Command(desc = "Days since last war")
    public double daysSinceLastWar() {
        if (getNumWars() > 0) return 0;
        DBWar last = Locutus.imp().getWarDb().getLastWar(NationId, getSnapshot());
        if (last != null) {
            long now = getSnapshot() == null ? System.currentTimeMillis() : getSnapshot();
            long diff = now - last.getDate();
            return ((double) diff) / TimeUnit.DAYS.toMillis(1);
        }
        return Integer.MAX_VALUE;
    }

    @Override
    @Command(desc = "The nation id")
    public int getId() {
        return NationId;
    }

    @Override
    public boolean isAlliance() {
        return false;
    }

    @Override
    @Command(desc = "The nation name")
    public String getName() {
        return NationName;
    }

    @Command(desc = "Total land in their cities")
    public double getLand() {
        return Land;
    }

    @Command(desc = "Free offensive war slots")
    public int getFreeOffensiveSlots() {
        return getMaxOff() - getOff();
    }

    @Command(desc = "Maximum offensive war slots")
    public int getMaxOff() {
        return 5;
    }

    public GuildDB getGuildDB() {
        if (AllianceId == 0) return null;
        return Locutus.imp().getGuildDBByAA(AllianceId);
    }

    public Map.Entry<CommandResult, List<StringMessageBuilder>> runCommandInternally(Guild guild, User user, String command) {
        if (user == null) return new AbstractMap.SimpleEntry<>(CommandResult.ERROR, StringMessageBuilder.list(null, "No user for: " + getMarkdownUrl()));

        StringMessageIO output = new StringMessageIO(user, guild);
        CommandResult type;
        String result;
        try {
            Locutus.imp().getCommandManager().run(guild, output, user, command, false, true);
            type = CommandResult.SUCCESS;
            return new AbstractMap.SimpleEntry<>(type, output.getMessages());
        } catch (Throwable e) {
            result = StringMan.stripApiKey(e.getMessage());
            type = CommandResult.ERROR;
            return new AbstractMap.SimpleEntry<>(type, StringMessageBuilder.list(user, result));
        }
    }

    public long lastActiveMs() {
        return LastOnline;
    }

    public void setLastActive(long epoch) {
        this.LastOnline = epoch;
    }

    @Command(desc = "If this nation is in war declare range of the current attacking nation")
    public boolean isInWarRange(@Default @Me DBNation target) {
        return target.getScore() > getScore() * 0.75 && target.getScore() < getScore() * 1.25;
    }

    public Member getMember(GuildDB db) {
        if (db != null) {
            User user = getUser();
            if (user != null) {
                return db.getGuild().getMember(user);
            }
        }
        return null;
    }

    @Command(desc = "Maximum number of defensive war slots")
    public int getMaxDef() {
        return 2;
    }

    @Command(desc = "Has protection")
    public boolean hasProtection() {
        return ProtectionTime > System.currentTimeMillis();
    }

    @Command
    public long getProtectionRemainingMs() {
        long now = System.currentTimeMillis();
        return Math.max(0, ProtectionTime - now);
    }

    @Command(desc = "Non core land")
    public double getNonCoreLand() {
        return NonCoreLand;
    }

    @Command(desc = "Tax income")
    public double getTaxIncome() {
        return TaxIncome;
    }

    // getOtherIncome
    @Command(desc = "Other income")
    public double getOtherIncome() {
        return OtherIncome;
    }
    // getCorporateIncome
    @Command(desc = "Corporate income")
    public double getCorporationIncome() {
        return CorporationIncome;
    }

    // getMineralIncome
    @Command(desc = "Mineral income")
    public double getMineralOutput() {
        return MineralOutput;
    }
    // getFueldIncome
    @Command(desc = "Fuel income")
    public double getFuelOutput() {
        return FuelOutput;
    }

    @Command(desc = "Development tier (every 10k)")
    public int getTier() {
        return DNS.getTier(getInfra());
    }

    @Command(desc = "Education index")
    public double getEducationIndex() {
        return EducationIndex;
    }
    //@Default Double PowerIndex,
    @Command(desc = "Power index")
    public double getPowerIndex() {
        return PowerIndex;
    }
    //                        @Default Double EmploymentIndex,
    @Command(desc = "Employment index")
    public double getEmploymentIndex() {
        return EmploymentIndex;
    }
    //                        @Default Double TransportationIndex,
    @Command(desc = "Transportation index")
    public double getTransportationIndex() {
        return TransportationIndex;
    }
    //                        @Default Double StabilityIndex,
    @Command(desc = "Stability index")
    public double getStabilityIndex() {
        return StabilityIndex;
    }
    //                        @Default Double CommerceIndex,
    @Command(desc = "Commerce index")
    public double getCommerceIndex() {
        return CommerceIndex;
    }
    //                        @Default Double Development,
    //                        @Default Double Land,
    //                        @Default Double Devastation,
    @Command(desc = "Devastation")
    public double getDevastation() {
        return Devastation;
    }
    //                        @Default Double WarIndex,
    //                        @Default Double TechIndex

    @Command(desc = "Tech index")
    public double getTechIndex() {
        return TechIndex;
    }

    public double estimateUnfilledJobsPenalty() {
        return DNS.estimateUnfilledJobsPenalty(
                Score,
        EducationIndex,
        PowerIndex,
        EmploymentIndex,
        TransportationIndex,
        StabilityIndex,
        CommerceIndex,
        Infra,
        Land,
        Devastation,
        WarIndex,
        TechIndex
        );
    }

    public void setNationPrivate(NationPrivate np) {
        this.privateData = np;
    }
}
