package link.locutus.discord.db.entities.conflict;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.generated.WarType;
import link.locutus.discord.api.types.MilitaryUnit;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.WarStatus;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static link.locutus.discord.db.entities.conflict.ConflictColumn.header;
import static link.locutus.discord.db.entities.conflict.ConflictColumn.ranking;

public class DamageStatGroup {
    public char totalWars;
    public char activeWars;
    public char warsWon;
    public char warsLost;
    public char warsExpired;
    public char warsPeaced;
    public final char[] warTypes = new char[WarType.values.length];

    public final double[] totalCost = ResourceType.getBuffer();
    public final double[] loot = ResourceType.getBuffer();
    public final int[] units = new int[MilitaryUnit.values.length];
    private long landStolenCents = 0;
    private long devastationCents = 0;

    public static Map<ConflictColumn, Function<DamageStatGroup, Object>> createRanking() {
        Map<ConflictColumn, Function<DamageStatGroup, Object>> header = createHeader();
        header.entrySet().removeIf(e -> !e.getKey().isRanking());
        return header;
    }

    public void clear() {
        totalWars = 0;
        activeWars = 0;
        warsWon = 0;
        warsLost = 0;
        warsExpired = 0;
        warsPeaced = 0;
        Arrays.fill(warTypes, (char) 0);
        Arrays.fill(totalCost, 0);
        Arrays.fill(loot, 0);
        Arrays.fill(units, 0);
        landStolenCents = 0;
        devastationCents = 0;
    }
    public static Map<ConflictColumn, Function<DamageStatGroup, Object>> createHeader() {
        Map<ConflictColumn, Function<DamageStatGroup, Object>> map = new Object2ObjectLinkedOpenHashMap<>();
        map.put(ranking("loss_value", ColumnType.STANDARD, "Total market value of damage", false), p -> (long) ResourceType.convertedTotal(p.totalCost));
        for (ResourceType type : ResourceType.values) {
            map.put(header("loss_" + type.name().toLowerCase(), ColumnType.RESOURCE, "Total " + type.name().toLowerCase(Locale.ROOT) + " damage", false), p -> (long) p.totalCost[type.ordinal()]);
        }
        map.put(ranking("loot_value", ColumnType.STANDARD, "Total market value of looted resources", false), p -> (long) ResourceType.convertedTotal(p.loot));
        for (MilitaryUnit unit : MilitaryUnit.values) {
            String name = unit.name().toLowerCase() + "_loss";
            String desc = "Total number of destroyed " + unit.name().toLowerCase();
            ConflictColumn col = ranking(name, ColumnType.UNIT, desc, false);
            map.put(col, p -> p.units[unit.ordinal()]);
        }
        map.put(ranking("land_stolen", ColumnType.STANDARD, "Value of destroyed infrastructure (not including project or policy discounts)", false), p -> (long) (p.landStolenCents * 0.01));
        map.put(ranking("devastation", ColumnType.STANDARD, "Value of land devastated", false), p -> (long) (p.devastationCents * 0.01));

        map.put(ranking("wars", ColumnType.WARS, "Number of wars", true), p -> (int) p.totalWars);
        map.put(header("wars_active", ColumnType.WARS, "Number of active wars", true), p -> (int) p.activeWars);
        map.put(ranking("wars_won", ColumnType.WARS,"Number of wars won", true), p -> (int) p.warsWon);
        map.put(ranking("wars_lost", ColumnType.WARS,"Number of wars lost", true), p -> (int) p.warsLost);
        map.put(ranking("wars_expired", ColumnType.WARS,"Number of wars expired", true), p -> (int) p.warsExpired);
        map.put(ranking("wars_peaced", ColumnType.WARS,"Number of wars peaced", true), p -> (int) p.warsPeaced);
        for (WarType type : WarType.values) {
            map.put(ranking(type.name().toLowerCase() + "_wars", ColumnType.WARS, "Number of wars declared as " + type.toString().toLowerCase(), true), p -> (int) p.warTypes[type.ordinal()]);
        }

        return map;
    }

    public void newWar(DBWar war, boolean isAttacker) {
        totalWars++;
        if (war.isActive()) activeWars++;
        else {
            addWarStatus(war.getStatus(), isAttacker);
        }
        warTypes[war.getWarType().ordinal()]++;
        addStats(war, isAttacker);
    }

    private void addWarStatus(WarStatus status, boolean isAttacker) {
        switch (status) {
            case DEFENDER_VICTORY -> {
                if (isAttacker) warsLost++;
                else warsWon++;
            }
            case ATTACKER_VICTORY -> {
                if (isAttacker) warsWon++;
                else warsLost++;
            }
            case PEACE -> {
                warsPeaced++;
            }
            case EXPIRED -> {
                warsExpired++;
            }
        }
    }

    public void addStats(DBWar war, boolean isAttacker) {
        ResourceType.add(totalCost, war.getResourcesStolen(isAttacker));
        ResourceType.add(loot, war.getResourcesStolen(isAttacker));
        int[] unitLoss = war.getMilitaryUnitsLost(isAttacker);
        for (int i = 0; i < units.length; i++) {
            this.units[i] += unitLoss[i];
        }
        landStolenCents += Math.round(war.getLandStolen(isAttacker) * 100);
        devastationCents += Math.round(war.getDevastation(isAttacker) * 100);
    }

    public void subtractStats(DBWar war, boolean isAttacker) {
        ResourceType.subtract(totalCost, war.getResourcesStolen(isAttacker));
        ResourceType.subtract(loot, war.getResourcesStolen(isAttacker));
        int[] unitLoss = war.getMilitaryUnitsLost(isAttacker);
        for (int i = 0; i < units.length; i++) {
            this.units[i] -= unitLoss[i];
        }
        landStolenCents -= Math.round(war.getLandStolen(isAttacker) * 100);
        devastationCents -= Math.round(war.getDevastation(isAttacker) * 100);
    }

    public void updateWar(DBWar previous, DBWar current, boolean isAttacker) {
        addWarStatus(current.getStatus(), isAttacker);
        if (previous != null && current != null) {
            subtractStats(previous, isAttacker);
            addStats(current, isAttacker);
        }
        if (previous.isActive() && !current.isActive()) {
            activeWars--;
        }
    }

    public double getTotalConverted() {
        return ResourceType.convertedTotal(totalCost);
    }


}
