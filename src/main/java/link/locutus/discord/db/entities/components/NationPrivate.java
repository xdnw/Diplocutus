package link.locutus.discord.db.entities.components;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.generated.*;
import link.locutus.discord.api.types.*;
import link.locutus.discord.db.SQLUtil;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.event.Event;
import link.locutus.discord.util.IOUtil;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.math.ArrayUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public class NationPrivate implements DBEntity<Void, NationPrivate> {
    private int parentId;

    public NationPrivate() {
        this(0);
    }

    public NationPrivate(int parentId) {
        this.parentId = parentId;
    }

    public DBNation getNation() {
        return DBNation.getOrCreate(parentId);
    }

    private DBAlliance getAlliance() {
        return getNation().getAlliance();
    }

    private DnsApi getAllianceApi() {
        DBAlliance alliance = getAlliance();
        return alliance == null ? null : alliance.getApi(false);
    }

    private final Map<Project, Integer> projects = new ConcurrentHashMap<>();
    private final Map<MilitaryUnit, Integer> military = new ConcurrentHashMap<>();
    private final Map<MilitaryUnitType, Integer> militaryCapacity = new ConcurrentHashMap<>();
    private final Map<MilitaryUnit, Double> militaryQuality = new ConcurrentHashMap<>();
    private final Map<Technology, Integer> technology = new ConcurrentHashMap<>();
    private final Map<Building, Integer> building = new ConcurrentHashMap<>();
    private final Map<ResourceType, Double> stockpile = new ConcurrentHashMap<>();
    private final Map<Policy, Long> policyLastRan = new ConcurrentHashMap<>();
    private final List<AllianceMemberInventory> memberInventory = new ArrayList<>();

    private final AtomicLong outdatedProjects = new AtomicLong(-1);
    private final AtomicLong outdatedMilitary = new AtomicLong(-1);
    private final AtomicLong outdatedTechnology = new AtomicLong(-1);
    private final AtomicLong outdatedBuilding = new AtomicLong(-1);
    private final AtomicLong outdatedStockpile = new AtomicLong(-1);
    private final AtomicLong outdatedPolicyLastRan = new AtomicLong(-1);
    private final AtomicLong outdatedInventory = new AtomicLong(-1);

    public AtomicLong getOutdatedProjects() {
        return outdatedProjects;
    }

    public AtomicLong getOutdatedMilitary() {
        return outdatedMilitary;
    }

    public AtomicLong getOutdatedTechnology() {
        return outdatedTechnology;
    }

    public AtomicLong getOutdatedBuilding() {
        return outdatedBuilding;
    }

    public AtomicLong getOutdatedStockpile() {
        return outdatedStockpile;
    }

    public AtomicLong getOutdatedPolicyLastRan() {
        return outdatedPolicyLastRan;
    }

    public AtomicLong getOutdatedInventory() {
        return outdatedInventory;
    }

    @Override
    public String getTableName() {
        return "nation_private";
    }

    @Override
    public boolean update(Void entity, Consumer<Event> eventConsumer) {
        return false;
    }

    @Override
    public Object[] write() {
        try {
            return new Object[]{
                    parentId,
                    ArrayUtil.writeEnumMap(projects),
                    ArrayUtil.writeEnumMap(military),
                    ArrayUtil.writeEnumMap(technology),
                    ArrayUtil.writeEnumMap(building),
                    ArrayUtil.toByteArray(ResourceType.resourcesToArray(stockpile)),
                    ArrayUtil.writeEnumMapLong(policyLastRan),
                    outdatedProjects.get(),
                    outdatedMilitary.get(),
                    outdatedTechnology.get(),
                    outdatedBuilding.get(),
                    outdatedStockpile.get(),
                    outdatedPolicyLastRan.get(),
                    inventoryToBytes(memberInventory),
                    outdatedInventory,
                    ArrayUtil.writeEnumMap(militaryCapacity),
                    ArrayUtil.writeEnumMapDouble(militaryQuality)
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] inventoryToBytes(List<AllianceMemberInventory> equipment) throws IOException {
        synchronized (memberInventory) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (AllianceMemberInventory inventory : equipment) {
                out.write(inventory.type.ordinal());
                IOUtil.writeVarInt(out, inventory.quality);
                IOUtil.writeVarInt(out, inventory.quantity);
            }
            return out.toByteArray();
        }
    }

    private List<AllianceMemberInventory> readMemberInventory(byte[] raw) throws IOException {
        synchronized (memberInventory) {
            memberInventory.clear();
            ByteArrayInputStream in = new ByteArrayInputStream(raw);
            while (in.available() > 0) {
                InventoryType type = InventoryType.values[in.read()];
                int quality = IOUtil.readVarInt(in);
                int quantity = IOUtil.readVarInt(in);
                AllianceMemberInventory record = new AllianceMemberInventory();
                record.type = type;
                record.quality = quality;
                record.quantity = quantity;
                memberInventory.add(record);
            }
            return memberInventory;
        }
    }

    @Override
    public void load(Object[] raw) {
        try {
            projects.clear();
            military.clear();
            technology.clear();
            building.clear();
            stockpile.clear();
            policyLastRan.clear();
            memberInventory.clear();
            militaryCapacity.clear();
            militaryQuality.clear();

            parentId = (int) raw[0];
            if (raw[1] != null) projects.putAll(ArrayUtil.readEnumMap((byte[]) raw[1], Project.class));
            if (raw[2] != null) military.putAll(ArrayUtil.readEnumMap((byte[]) raw[2], MilitaryUnit.class));
            if (raw[3] != null) technology.putAll(ArrayUtil.readEnumMap((byte[]) raw[3], Technology.class));
            if (raw[4] != null) building.putAll(ArrayUtil.readEnumMap((byte[]) raw[4], Building.class));
            if (raw[5] != null) stockpile.putAll(ResourceType.resourcesToMap(ArrayUtil.toDoubleArray((byte[]) raw[5])));
            if (raw[6] != null) policyLastRan.putAll(ArrayUtil.readEnumMapLong((byte[]) raw[6], Policy.class));

            outdatedProjects.set(SQLUtil.castLong(raw[7]));
            outdatedMilitary.set(SQLUtil.castLong(raw[8]));
            outdatedTechnology.set(SQLUtil.castLong(raw[9]));
            outdatedBuilding.set(SQLUtil.castLong(raw[10]));
            outdatedStockpile.set(SQLUtil.castLong(raw[11]));
            outdatedPolicyLastRan.set(SQLUtil.castLong(raw[12]));

            if (raw[13] != null) readMemberInventory((byte[]) raw[13]);
            if (raw[14] != null) outdatedInventory.set(SQLUtil.castLong(raw[14]));
            if (raw[15] != null) militaryCapacity.putAll(ArrayUtil.readEnumMap((byte[]) raw[15], MilitaryUnitType.class));
            if (raw[16] != null) militaryQuality.putAll(ArrayUtil.readEnumMapDouble((byte[]) raw[16], MilitaryUnit.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        result.put("nation_id", int.class);
        result.put("projects", byte[].class);
        result.put("military", byte[].class);
        result.put("technology", byte[].class);
        result.put("building", byte[].class);
        result.put("stockpile", byte[].class);
        result.put("policyLastRan", byte[].class);
        result.put("outdatedProjects", long.class);
        result.put("outdatedMilitary", long.class);
        result.put("outdatedTechnology", long.class);
        result.put("outdatedBuilding", long.class);
        result.put("outdatedStockpile", long.class);
        result.put("outdatedPolicyLastRan", long.class);
        result.put("memberInventory", byte[].class);
        result.put("outdatedInventory", long.class);
        result.put("militaryCapacity", byte[].class);
        result.put("militaryQuality", byte[].class);
        return result;
    }

    @Override
    public NationPrivate emptyInstance() {
        return new NationPrivate();
    }

    public NationPrivate copy() {
        NationPrivate result = new NationPrivate(parentId);
        result.projects.putAll(projects);
        result.military.putAll(military);
        result.technology.putAll(technology);
        result.building.putAll(building);
        result.stockpile.putAll(stockpile);
        result.policyLastRan.putAll(policyLastRan);
        
        result.outdatedProjects.set(outdatedProjects.get());
        result.outdatedMilitary.set(outdatedMilitary.get());
        result.outdatedTechnology.set(outdatedTechnology.get());
        result.outdatedBuilding.set(outdatedBuilding.get());
        result.outdatedStockpile.set(outdatedStockpile.get());
        result.outdatedPolicyLastRan.set(outdatedPolicyLastRan.get());
        result.memberInventory.addAll(memberInventory);
        result.outdatedInventory.set(outdatedInventory.get());
        result.militaryCapacity.putAll(militaryCapacity);
        result.militaryQuality.putAll(militaryQuality);
        return result;
    }

    public Map<Project, Integer> getProjects(long timestamp) {
        return withApi(outdatedProjects, timestamp, projects, () -> {
            DnsApi api = getAllianceApi();
            if (api != null) {
                long now = System.currentTimeMillis();
                List<NationProjects> result = api.nationProjects(parentId).call();
                if (result != null && !result.isEmpty()) {
                    update(result.get(0), now);
                }
                return true;
            }
            return false;
        });
    }
    // private final Map<MilitaryUnit, Integer> military = new ConcurrentHashMap<>();
    public Map<MilitaryUnit, Integer> getMilitary(long timestamp) {
        return withApi(outdatedMilitary, timestamp, military, () -> {
            DBAlliance aa = getAlliance();
            AllianceMilitary result = aa.updateMilitaryOfNation(parentId);
            if (result != null) {
                long now = System.currentTimeMillis();
                update(result, now);
                return true;
            }
            return false;
        });
    }

    public Map<MilitaryUnitType, Integer> getMilitaryCapacity(long timestamp) {
        return withApi(outdatedMilitary, timestamp, militaryCapacity, () -> {
            DBAlliance aa = getAlliance();
            AllianceMilitary result = aa.updateMilitaryOfNation(parentId);
            if (result != null) {
                long now = System.currentTimeMillis();
                update(result, now);
                return true;
            }
            return false;
        });
    }

    public Map<MilitaryUnit, Double> getMilitaryQuality(long timestamp) {
        return withApi(outdatedMilitary, timestamp, militaryQuality, () -> {
            DBAlliance aa = getAlliance();
            AllianceMilitary result = aa.updateMilitaryOfNation(parentId);
            if (result != null) {
                long now = System.currentTimeMillis();
                update(result, now);
                return true;
            }
            return false;
        });
    }

    //    private final Map<Technology, Integer> technology = new ConcurrentHashMap<>();
    public Map<Technology, Integer> getTechnology(long timestamp) {
        return withApi(outdatedTechnology, timestamp, technology, () -> {
            DBAlliance aa = getAlliance();
            AllianceTech result = aa.updateTechOfNation(parentId);
            if (result != null) {
                long now = System.currentTimeMillis();
                update(result, now);
                return true;
            }
            return false;
        });
    }
    //    private final Map<Building, Integer> building = new ConcurrentHashMap<>();
    public Map<Building, Integer> getBuildings(long timestamp) {
        return localCall(timestamp, outdatedBuilding, building, this::getAllianceApi, () -> parentId, DnsApi::nationBuildings, this::update);
    }

    //    private final Map<ResourceType, Double> stockpile = new ConcurrentHashMap<>();
    public Map<ResourceType, Double> getStockpile(long timestamp) {
        return withApi(outdatedStockpile, timestamp, stockpile, () -> {
            DBAlliance aa = getAlliance();
            long now = System.currentTimeMillis();
            AllianceMemberFunds result = aa.updateStockpileOfNation(parentId);
            if (result != null) {
                update(result, now);
                return true;
            }
            return false;
        });
    }
    //    private final Map<Policy, Long> policyLastRan = new ConcurrentHashMap<>();
    public Map<Policy, Long> getPolicyLastRan(long timestamp) {
        return localCall(timestamp, outdatedPolicyLastRan, policyLastRan, this::getAllianceApi, () -> parentId, DnsApi::nationPolicyLastRan, this::update);
    }

    public List<AllianceMemberInventory> getInventory(long timestamp) {
        return localCallList(timestamp, outdatedInventory, memberInventory, this::getAllianceApi, () -> parentId, DnsApi::allianceMemberInventory, this::update);
    }

    public void update(NationProjects projects, long timestamp) {
        boolean dirty = false;
        for (Project project : Project.values) {
            int existing = this.projects.getOrDefault(project, 0);
            int newValue = project.get(projects);
            if (newValue == existing) continue;
            this.projects.put(project, newValue);
            dirty = true;
        }
        this.outdatedProjects.set(timestamp);
        if (dirty) Locutus.imp().getNationDB().saveNationPrivate(this);
    }

    public void update(AllianceMilitary military, long timestamp) {
        boolean dirty = false;
        for (MilitaryUnit unit : MilitaryUnit.values) {
            int existing = this.military.getOrDefault(unit, 0);
            int newValue = unit.getAmount(military);
            if (newValue != existing) {
                this.military.put(unit, newValue);
                dirty = true;
            }

            double existingQuality = this.militaryQuality.getOrDefault(unit, 0d);
            double newQuality = unit.getQuality(military);
            if (newQuality != existingQuality) {
                this.militaryQuality.put(unit, newQuality);
                dirty = true;
            }
        }
        for (MilitaryUnitType type : MilitaryUnitType.values) {
            int existing = this.militaryCapacity.getOrDefault(type, 0);
            int newValue = type.getCapacity(military);
            if (newValue != existing) {
                this.militaryCapacity.put(type, newValue);
                dirty = true;
            }
        }
        this.outdatedMilitary.set(timestamp);
        if (dirty) Locutus.imp().getNationDB().saveNationPrivate(this);
    }

    public void update(AllianceTech technology, long timestamp) {
        boolean dirty = false;
        for (Technology tech : Technology.values) {
            int existing = this.technology.getOrDefault(tech, 0);
            int newValue = tech.get(technology);
            if (newValue == existing) continue;
            this.technology.put(tech, newValue);
            dirty = true;
        }
        this.outdatedTechnology.set(timestamp);
        if (dirty) Locutus.imp().getNationDB().saveNationPrivate(this);
    }

    public void update(List<AllianceMemberInventory> equipment, long timestamp) {
        boolean dirty = false;
        synchronized (this.memberInventory) {
            Map<InventoryType, Map<Integer, Integer>> inventoryToQualityToQuantity = new HashMap<>();
            for (AllianceMemberInventory inventory : this.memberInventory) {
                inventoryToQualityToQuantity.computeIfAbsent(inventory.type, k -> new HashMap<>()).put(inventory.quality, inventory.quantity);
            }
            for (AllianceMemberInventory inventory : equipment) {
                InventoryType type = inventory.type;
                int existingQuantity = inventoryToQualityToQuantity.getOrDefault(type, Collections.emptyMap()).getOrDefault(inventory.quality, 0);
                if (existingQuantity == inventory.quantity) continue;
                dirty = true;
            }
            if (dirty) {
                this.memberInventory.clear();
                this.memberInventory.addAll(equipment);
            }
        }
        this.outdatedInventory.set(timestamp);
        if (dirty) Locutus.imp().getNationDB().saveNationPrivate(this);
    }

    public void update(NationBuildings buildings, long timestamp) {
        boolean dirty = false;
        for (Building building : Building.values) {
            int existing = this.building.getOrDefault(building, 0);
            int newValue = building.get(buildings);
            if (newValue == existing) continue;
            this.building.put(building, newValue);
            dirty = true;
        }
        this.outdatedBuilding.set(timestamp);
        if (dirty) Locutus.imp().getNationDB().saveNationPrivate(this);
    }

    // stockpile
    public void update(AllianceMemberFunds stockpile, long timestamp) {
        boolean dirty = false;
        for (ResourceType resource : ResourceType.values) {
            double existing = this.stockpile.getOrDefault(resource, 0.0);
            double newValue = resource.getNation(stockpile);
            if (newValue == existing) continue;
            this.stockpile.put(resource, newValue);
            dirty = true;
        }
        this.outdatedStockpile.set(timestamp);
        if (dirty) Locutus.imp().getNationDB().saveNationPrivate(this);
    }

    public void update(NationPolicyLastRan policies, long timestamp) {
        boolean dirty = false;
        for (Policy policy : Policy.values) {
            long existing = this.policyLastRan.getOrDefault(policy, 0L);
            long newValue = policy.get(policies);
            if (newValue == existing) continue;
            this.policyLastRan.put(policy, newValue);
            dirty = true;
        }
        this.outdatedPolicyLastRan.set(timestamp);
        if (dirty) Locutus.imp().getNationDB().saveNationPrivate(this);
    }

    public int getNationId() {
        return parentId;
    }
}
