package link.locutus.discord.db.entities;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.generated.WarHistory;
import link.locutus.discord.api.generated.WarType;
import link.locutus.discord.api.types.MilitaryUnit;
import link.locutus.discord.config.Settings;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.war.WarUpdateEvent;
import link.locutus.discord.util.*;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.task.war.WarCard;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class DBWar implements DBEntity<WarHistory, DBWar> {
    private int WarId;
    private int DeclareingNationId;
    private int DefendingNationId;
    private int DeclareingNationAllianceId;
    private int DefendingNationAllianceId;
    // Unique values: Raid War, Conquest War
    private WarType WarType;
    private String WarReason;
    private long StartDate;
    private long EndDate;
    private double DefendingNationVictoryPoints;
    private double DeclareingNationVictoryPoints;

    private double DevastationDeclareingNation;
    private double DevastationDefendingNation;

    private double[] DeclareingNationResourcesStolen;
    private double[] DefendingNationResourcesStolen;
    private double DeclareingNationLandStolen;
    private double DefendingNationLandStolen;

    private int[] DeclareingNationMilitaryUnitsLost;
    private int[] DefendingNationMilitaryUnitsLost;
    public int attInfra;
    public int defInfra;

    public DBWar() {
        WarType = link.locutus.discord.api.generated.WarType.RAID_WAR;
        DeclareingNationResourcesStolen = ResourceType.getBuffer();
        DefendingNationResourcesStolen = ResourceType.getBuffer();
        DeclareingNationMilitaryUnitsLost = MilitaryUnit.getBuffer();
        DefendingNationMilitaryUnitsLost = MilitaryUnit.getBuffer();
    }

    public double getDevastation(boolean isAttacker) {
        return isAttacker ? DevastationDeclareingNation : DevastationDefendingNation;
    }

    public double getLandStolen(boolean isAttacker) {
        return isAttacker ? DeclareingNationLandStolen : DefendingNationLandStolen;
    }

    public double getVictoryPoints(boolean isAttacker) {
        return isAttacker ? DeclareingNationVictoryPoints : DefendingNationVictoryPoints;
    }

    public double[] getResourcesStolen(boolean isAttacker) {
        return isAttacker ? DeclareingNationResourcesStolen : DefendingNationResourcesStolen;
    }

    public int[] getMilitaryUnitsLost(boolean isAttacker) {
        return isAttacker ? DeclareingNationMilitaryUnitsLost : DefendingNationMilitaryUnitsLost;
    }

    @Override
    public String getTableName() {
        return "wars";
    }

    @Override
    public boolean update(WarHistory entity, Consumer<Event> eventConsumer) {
        DBWar copy = null;
        if (entity.WarId != WarId) {
            if (copy == null) copy = copy();
            WarId = entity.WarId;
        }
        if (entity.DeclareingNationId != DeclareingNationId) {
            if (copy == null) copy = copy();
            DeclareingNationId = entity.DeclareingNationId;
        }
        if (entity.DefendingNationId != DefendingNationId) {
            if (copy == null) copy = copy();
            DefendingNationId = entity.DefendingNationId;
        }
        if (attInfra == 0) {
            DBNation att = DBNation.getById(DeclareingNationId);
            if (att != null) {
                attInfra = (int) att.getInfra();
            }
        }
        if (defInfra == 0) {
            DBNation def = DBNation.getById(DefendingNationId);
            if (def != null) {
                defInfra = (int) def.getInfra();
            }
        }
        if (entity.DeclareingNationAllianceId != DeclareingNationAllianceId) {
            if (copy == null) copy = copy();
            DeclareingNationAllianceId = entity.DeclareingNationAllianceId;
        }
        if (entity.DefendingNationAllianceId != DefendingNationAllianceId) {
            if (copy == null) copy = copy();
            DefendingNationAllianceId = entity.DefendingNationAllianceId;
        }
        if (entity.WarType != WarType) {
            if (copy == null) copy = copy();
            WarType = entity.WarType;
        }
        if (!entity.WarReason.equals(WarReason)) {
            if (copy == null) copy = copy();
            WarReason = entity.WarReason;
        }
        if (entity.StartDate.getTime() != StartDate) {
            if (copy == null) copy = copy();
            StartDate = entity.StartDate.getTime();
        }
        if (entity.EndDate.getTime() != EndDate) {
            if (copy == null) copy = copy();
            EndDate = entity.EndDate.getTime();
        }
        if (entity.DefendingNationVictoryPoints != DefendingNationVictoryPoints) {
            if (copy == null) copy = copy();
            DefendingNationVictoryPoints = entity.DefendingNationVictoryPoints;
        }
        if (entity.DeclareingNationVictoryPoints != DeclareingNationVictoryPoints) {
            if (copy == null) copy = copy();
            DeclareingNationVictoryPoints = entity.DeclareingNationVictoryPoints;
        }
        //    public Double DevastationDeclareingNation;
        if (entity.DevastationDeclareingNation != DevastationDeclareingNation) {
            if (copy == null) copy = copy();
            DevastationDeclareingNation = entity.DevastationDeclareingNation;
        }
        //    public Double DevastationDefendingNation;
        if (entity.DevastationDefendingNation != DevastationDefendingNation) {
            if (copy == null) copy = copy();
            DevastationDefendingNation = entity.DevastationDefendingNation;
        }
        //    public Double DeclareingNationCashStolen;
        if (Math.round(entity.DeclareingNationCashStolen * 100) != Math.round(DeclareingNationResourcesStolen[ResourceType.CASH.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DeclareingNationResourcesStolen[ResourceType.CASH.ordinal()] = entity.DeclareingNationCashStolen;
        }
        //    public Double DeclareingNationMineralStolen;
        if (Math.round(entity.DeclareingNationMineralStolen * 100) != Math.round(DeclareingNationResourcesStolen[ResourceType.MINERALS.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DeclareingNationResourcesStolen[ResourceType.MINERALS.ordinal()] = entity.DeclareingNationMineralStolen;
        }
        //    public Double DeclareingNationTechStolen;
        if (Math.round(entity.DeclareingNationTechStolen * 100) != Math.round(DeclareingNationResourcesStolen[ResourceType.TECHNOLOGY.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DeclareingNationResourcesStolen[ResourceType.TECHNOLOGY.ordinal()] = entity.DeclareingNationTechStolen;
        }
        //    public Double DeclareingNationUraniumStolen;
        if (Math.round(entity.DeclareingNationUraniumStolen * 100) != Math.round(DeclareingNationResourcesStolen[ResourceType.URANIUM.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DeclareingNationResourcesStolen[ResourceType.URANIUM.ordinal()] = entity.DeclareingNationUraniumStolen;
        }
        //    public Double DeclareingNationRareMetalStolen;
        if (Math.round(entity.DeclareingNationRareMetalStolen * 100) != Math.round(DeclareingNationResourcesStolen[ResourceType.RARE_METALS.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DeclareingNationResourcesStolen[ResourceType.RARE_METALS.ordinal()] = entity.DeclareingNationRareMetalStolen;
        }
        //    public Double DeclareingNationFuelStolen;
        if (Math.round(entity.DeclareingNationFuelStolen * 100) != Math.round(DeclareingNationResourcesStolen[ResourceType.FUEL.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DeclareingNationResourcesStolen[ResourceType.FUEL.ordinal()] = entity.DeclareingNationFuelStolen;
        }
        //    public Double DefendingNationCashStolen;
        if (Math.round(entity.DefendingNationCashStolen * 100) != Math.round(DefendingNationResourcesStolen[ResourceType.CASH.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DefendingNationResourcesStolen[ResourceType.CASH.ordinal()] = entity.DefendingNationCashStolen;
        }
        //    public Double DefendingNationMineralStolen;
        if (Math.round(entity.DefendingNationMineralStolen * 100) != Math.round(DefendingNationResourcesStolen[ResourceType.MINERALS.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DefendingNationResourcesStolen[ResourceType.MINERALS.ordinal()] = entity.DefendingNationMineralStolen;
        }
        //    public Double DefendingNationTechStolen;
        if (Math.round(entity.DefendingNationTechStolen * 100) != Math.round(DefendingNationResourcesStolen[ResourceType.TECHNOLOGY.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DefendingNationResourcesStolen[ResourceType.TECHNOLOGY.ordinal()] = entity.DefendingNationTechStolen;
        }
        //    public Double DefendingNationUraniumStolen;
        if (Math.round(entity.DefendingNationUraniumStolen * 100) != Math.round(DefendingNationResourcesStolen[ResourceType.URANIUM.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DefendingNationResourcesStolen[ResourceType.URANIUM.ordinal()] = entity.DefendingNationUraniumStolen;
        }
        //    public Double DefendingNationRareMetalStolen;
        if (Math.round(entity.DefendingNationRareMetalStolen * 100) != Math.round(DefendingNationResourcesStolen[ResourceType.RARE_METALS.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DefendingNationResourcesStolen[ResourceType.RARE_METALS.ordinal()] = entity.DefendingNationRareMetalStolen;
        }
        //    public Double DefendingNationFuelStolen;
        if (Math.round(entity.DefendingNationFuelStolen * 100) != Math.round(DefendingNationResourcesStolen[ResourceType.FUEL.ordinal()] * 100)) {
            if (copy == null) copy = copy();
            DefendingNationResourcesStolen[ResourceType.FUEL.ordinal()] = entity.DefendingNationFuelStolen;
        }
        //    public Double DeclareingNationLandStolen;
        if (Math.round(entity.DeclareingNationLandStolen * 100) != Math.round(DeclareingNationLandStolen * 100)) {
            if (copy == null) copy = copy();
            DeclareingNationLandStolen = entity.DeclareingNationLandStolen;
        }
        //    public Double DefendingNationLandStolen;
        if (Math.round(entity.DefendingNationLandStolen * 100) != Math.round(DefendingNationLandStolen * 100)) {
            if (copy == null) copy = copy();
            DefendingNationLandStolen = entity.DefendingNationLandStolen;
        }
        //    public Integer DeclareingNationInfantryTotalLost;
        if (entity.DeclareingNationInfantryTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.INFANTRY.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.INFANTRY.ordinal()] = entity.DeclareingNationInfantryTotalLost;
        }
        //    public Integer DeclareingNationSupportVehiclesTotalLost;
        if (entity.DeclareingNationSupportVehiclesTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.SUPPORT_EQUIPMENT.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.SUPPORT_EQUIPMENT.ordinal()] = entity.DeclareingNationSupportVehiclesTotalLost;
        }
        //    public Integer DeclareingNationArtilleryTotalLost;
        if (entity.DeclareingNationArtilleryTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.ARTILLERY.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.ARTILLERY.ordinal()] = entity.DeclareingNationArtilleryTotalLost;
        }
        //    public Integer DeclareingNationLightTanksTotalLost;
        if (entity.DeclareingNationLightTanksTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.LIGHT_TANKS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.LIGHT_TANKS.ordinal()] = entity.DeclareingNationLightTanksTotalLost;
        }
        //    public Integer DeclareingNationMediumTanksTotalLost;
        if (entity.DeclareingNationMediumTanksTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.MEDIUM_TANKS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.MEDIUM_TANKS.ordinal()] = entity.DeclareingNationMediumTanksTotalLost;
        }
        //    public Integer DeclareingNationHeavyTanksTotalLost;
        if (entity.DeclareingNationHeavyTanksTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.HEAVY_TANKS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.HEAVY_TANKS.ordinal()] = entity.DeclareingNationHeavyTanksTotalLost;
        }
        //    public Integer DeclareingNationLightMechsTotalLost;
        if (entity.DeclareingNationLightMechsTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.LIGHT_MECHS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.LIGHT_MECHS.ordinal()] = entity.DeclareingNationLightMechsTotalLost;
        }
        //    public Integer DeclareingNationHeavyMechsTotalLost;
        if (entity.DeclareingNationHeavyMechsTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.HEAVY_MECHS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.HEAVY_MECHS.ordinal()] = entity.DeclareingNationHeavyMechsTotalLost;
        }
        //    public Integer DeclareingNationPrescusarMechTotalLost;
        if (entity.DeclareingNationPrescusarMechTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.PRECURSOR_MECHS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.PRECURSOR_MECHS.ordinal()] = entity.DeclareingNationPrescusarMechTotalLost;
        }
        //    public Integer DeclareingNationMissileLaunchersTotalLost;
        if (entity.DeclareingNationMissileLaunchersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.MISSILE_LAUNCHERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.MISSILE_LAUNCHERS.ordinal()] = entity.DeclareingNationMissileLaunchersTotalLost;
        }
        //    public Integer DeclareingNationBombersTotalLost;
        if (entity.DeclareingNationBombersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.BOMBERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.BOMBERS.ordinal()] = entity.DeclareingNationBombersTotalLost;
        }
        //    public Integer DeclareingNationFightersTotalLost;
        if (entity.DeclareingNationFightersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.FIGHTERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.FIGHTERS.ordinal()] = entity.DeclareingNationFightersTotalLost;
        }
        //    public Integer DeclareingNationHelicoptersTotalLost;
        if (entity.DeclareingNationHelicoptersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.HELICOPTERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.HELICOPTERS.ordinal()] = entity.DeclareingNationHelicoptersTotalLost;
        }
        //    public Integer DeclareingNationDronesTotalLost;
        if (entity.DeclareingNationDronesTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.DRONES.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.DRONES.ordinal()] = entity.DeclareingNationDronesTotalLost;
        }
        //    public Integer DeclareingNationStealthFightersTotalLost;
        if (entity.DeclareingNationStealthFightersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.STEALTH_FIGHTERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.STEALTH_FIGHTERS.ordinal()] = entity.DeclareingNationStealthFightersTotalLost;
        }
        //    public Integer DeclareingNationStealthBombersTotalLost;
        if (entity.DeclareingNationStealthBombersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.STEALTH_BOMBERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.STEALTH_BOMBERS.ordinal()] = entity.DeclareingNationStealthBombersTotalLost;
        }
        //    public Integer DeclareingNationDestroyersTotalLost;
        if (entity.DeclareingNationDestroyersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.DESTROYERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.DESTROYERS.ordinal()] = entity.DeclareingNationDestroyersTotalLost;
        }
        //    public Integer DeclareingNationSubsTotalLost;
        if (entity.DeclareingNationSubsTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.SUBMARINES.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.SUBMARINES.ordinal()] = entity.DeclareingNationSubsTotalLost;
        }
        //    public Integer DeclareingNationCarriersTotalLost;
        if (entity.DeclareingNationCarriersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.CARRIERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.CARRIERS.ordinal()] = entity.DeclareingNationCarriersTotalLost;
        }
        //    public Integer DeclareingNationCruisersTotalLost;
        if (entity.DeclareingNationCruisersTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.CRUISERS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.CRUISERS.ordinal()] = entity.DeclareingNationCruisersTotalLost;
        }
        //    public Integer DeclareingNationBattleshipsTotalLost;
        if (entity.DeclareingNationBattleshipsTotalLost != DeclareingNationMilitaryUnitsLost[MilitaryUnit.BATTLESHIPS.ordinal()]) {
            if (copy == null) copy = copy();
            DeclareingNationMilitaryUnitsLost[MilitaryUnit.BATTLESHIPS.ordinal()] = entity.DeclareingNationBattleshipsTotalLost;
        }
        //    public Integer DefendingNationInfantryTotalLost;
        if (entity.DefendingNationInfantryTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.INFANTRY.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.INFANTRY.ordinal()] = entity.DefendingNationInfantryTotalLost;
        }
        //    public Integer DefendingNationSupportVehiclesTotalLost;
        if (entity.DefendingNationSupportVehiclesTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.SUPPORT_EQUIPMENT.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.SUPPORT_EQUIPMENT.ordinal()] = entity.DefendingNationSupportVehiclesTotalLost;
        }
        //    public Integer DefendingNationArtilleryTotalLost;
        if (entity.DefendingNationArtilleryTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.ARTILLERY.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.ARTILLERY.ordinal()] = entity.DefendingNationArtilleryTotalLost;
        }
        //    public Integer DefendingNationLightTanksTotalLost;
        if (entity.DefendingNationLightTanksTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.LIGHT_TANKS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.LIGHT_TANKS.ordinal()] = entity.DefendingNationLightTanksTotalLost;
        }
        //    public Integer DefendingNationMediumTanksTotalLost;
        if (entity.DefendingNationMediumTanksTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.MEDIUM_TANKS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.MEDIUM_TANKS.ordinal()] = entity.DefendingNationMediumTanksTotalLost;
        }
        //    public Integer DefendingNationHeavyTanksTotalLost;
        if (entity.DefendingNationHeavyTanksTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.HEAVY_TANKS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.HEAVY_TANKS.ordinal()] = entity.DefendingNationHeavyTanksTotalLost;
        }
        //    public Integer DefendingNationLightMechsTotalLost;
        if (entity.DefendingNationLightMechsTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.LIGHT_MECHS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.LIGHT_MECHS.ordinal()] = entity.DefendingNationLightMechsTotalLost;
        }
        //    public Integer DefendingNationHeavyMechsTotalLost;
        if (entity.DefendingNationHeavyMechsTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.HEAVY_MECHS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.HEAVY_MECHS.ordinal()] = entity.DefendingNationHeavyMechsTotalLost;
        }
        //    public Integer DefendingNationPrescusarMechTotalLost;
        if (entity.DefendingNationPrescusarMechTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.PRECURSOR_MECHS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.PRECURSOR_MECHS.ordinal()] = entity.DefendingNationPrescusarMechTotalLost;
        }
        //    public Integer DefendingNationMissileLaunchersTotalLost;
        if (entity.DefendingNationMissileLaunchersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.MISSILE_LAUNCHERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.MISSILE_LAUNCHERS.ordinal()] = entity.DefendingNationMissileLaunchersTotalLost;
        }
        //    public Integer DefendingNationBombersTotalLost;
        if (entity.DefendingNationBombersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.BOMBERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.BOMBERS.ordinal()] = entity.DefendingNationBombersTotalLost;
        }
        //    public Integer DefendingNationFightersTotalLost;
        if (entity.DefendingNationFightersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.FIGHTERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.FIGHTERS.ordinal()] = entity.DefendingNationFightersTotalLost;
        }
        //    public Integer DefendingNationHelicoptersTotalLost;
        if (entity.DefendingNationHelicoptersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.HELICOPTERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.HELICOPTERS.ordinal()] = entity.DefendingNationHelicoptersTotalLost;
        }
        //    public Integer DefendingNationDronesTotalLost;
        if (entity.DefendingNationDronesTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.DRONES.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.DRONES.ordinal()] = entity.DefendingNationDronesTotalLost;
        }
        //    public Integer DefendingNationStealthFightersTotalLost;
        if (entity.DefendingNationStealthFightersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.STEALTH_FIGHTERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.STEALTH_FIGHTERS.ordinal()] = entity.DefendingNationStealthFightersTotalLost;
        }
        //    public Integer DefendingNationStealthBombersTotalLost;
        if (entity.DefendingNationStealthBombersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.STEALTH_BOMBERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.STEALTH_BOMBERS.ordinal()] = entity.DefendingNationStealthBombersTotalLost;
        }
        //    public Integer DefendingNationDestroyersTotalLost;
        if (entity.DefendingNationDestroyersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.DESTROYERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.DESTROYERS.ordinal()] = entity.DefendingNationDestroyersTotalLost;
        }
        //    public Integer DefendingNationSubsTotalLost;
        if (entity.DefendingNationSubsTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.SUBMARINES.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.SUBMARINES.ordinal()] = entity.DefendingNationSubsTotalLost;
        }
        //    public Integer DefendingNationCarriersTotalLost;
        if (entity.DefendingNationCarriersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.CARRIERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.CARRIERS.ordinal()] = entity.DefendingNationCarriersTotalLost;
        }
        //    public Integer DefendingNationCruisersTotalLost;
        if (entity.DefendingNationCruisersTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.CRUISERS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.CRUISERS.ordinal()] = entity.DefendingNationCruisersTotalLost;
        }
        //    public Integer DefendingNationBattleshipsTotalLost;
        if (entity.DefendingNationBattleshipsTotalLost != DefendingNationMilitaryUnitsLost[MilitaryUnit.BATTLESHIPS.ordinal()]) {
            if (copy == null) copy = copy();
            DefendingNationMilitaryUnitsLost[MilitaryUnit.BATTLESHIPS.ordinal()] = entity.DefendingNationBattleshipsTotalLost;
        }
        if (copy != null && eventConsumer != null) {
            eventConsumer.accept(new WarUpdateEvent(copy, this));
        }
        return copy != null;
    }

    @Override
    public Object[] write() {
        try {
            return new Object[]{
                WarId,
                DeclareingNationId,
                DefendingNationId,
                DeclareingNationAllianceId,
                DefendingNationAllianceId,
                attInfra,
                defInfra,
                WarType.ordinal(),
                WarReason,
                StartDate,
                EndDate,
                DefendingNationVictoryPoints,
                DeclareingNationVictoryPoints,
                DevastationDeclareingNation,
                DevastationDefendingNation,
                ArrayUtil.toByteArray(DeclareingNationResourcesStolen),
                ArrayUtil.toByteArray(DefendingNationResourcesStolen),
                DeclareingNationLandStolen,
                DefendingNationLandStolen,
                ArrayUtil.writeEnumMap(MilitaryUnit.arrayToMap(DeclareingNationMilitaryUnitsLost)),
                ArrayUtil.writeEnumMap(MilitaryUnit.arrayToMap(DefendingNationMilitaryUnitsLost))
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void load(Object[] raw) {
        try {
            WarId = (int) raw[0];
            DeclareingNationId = (int) raw[1];
            DefendingNationId = (int) raw[2];
            DeclareingNationAllianceId = (int) raw[3];
            DefendingNationAllianceId = (int) raw[4];
            attInfra = (int) raw[5];
            defInfra = (int) raw[6];
            WarType = link.locutus.discord.api.generated.WarType.values[(int) raw[7]];
            WarReason = (String) raw[8];
            StartDate = (long) raw[9];
            EndDate = (long) raw[10];
            DefendingNationVictoryPoints = (double) raw[11];
            DeclareingNationVictoryPoints = (double) raw[12];
            DevastationDeclareingNation = (double) raw[13];
            DevastationDefendingNation = (double) raw[14];
            DeclareingNationResourcesStolen = ArrayUtil.toDoubleArray((byte[]) raw[15]);
            DefendingNationResourcesStolen = ArrayUtil.toDoubleArray((byte[]) raw[16]);
            DeclareingNationLandStolen = (double) raw[17];
            DefendingNationLandStolen = (double) raw[18];
            DeclareingNationMilitaryUnitsLost = MilitaryUnit.mapToArray(ArrayUtil.readEnumMap((byte[]) raw[19], MilitaryUnit.class));
            DefendingNationMilitaryUnitsLost = MilitaryUnit.mapToArray(ArrayUtil.readEnumMap((byte[]) raw[20], MilitaryUnit.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put("WarId", int.class);
        map.put("DeclareingNationId", int.class);
        map.put("DefendingNationId", int.class);
        map.put("DeclareingNationAllianceId", int.class);
        map.put("DefendingNationAllianceId", int.class);
        map.put("attInfra", int.class);
        map.put("defInfra", int.class);
        map.put("WarType", int.class);
        map.put("WarReason", String.class);
        map.put("StartDate", long.class);
        map.put("EndDate", long.class);
        map.put("DefendingNationVictoryPoints", double.class);
        map.put("DeclareingNationVictoryPoints", double.class);
        map.put("DevastationDeclareingNation", double.class);
        map.put("DevastationDefendingNation", double.class);
        map.put("DeclareingNationResourcesStolen", byte[].class);
        map.put("DefendingNationResourcesStolen", byte[].class);
        map.put("DeclareingNationLandStolen", double.class);
        map.put("DefendingNationLandStolen", double.class);
        map.put("DeclareingNationMilitaryUnitsLost", byte[].class);
        map.put("DefendingNationMilitaryUnitsLost", byte[].class);
        return map;
    }

    public int getDeclaringInfra(boolean isAttacker) {
        return isAttacker ? attInfra : defInfra;
    }

    @Override
    public DBWar emptyInstance() {
        return new DBWar();
    }

    public int getTier(boolean isAttacker) {
        return DNS.getTier(getDeclaringInfra(isAttacker));
    }

    public boolean equalsDeep(DBWar war) {
        if (war.DeclareingNationId != DeclareingNationId) return false;
        if (war.DefendingNationId != DefendingNationId) return false;
        if (war.DeclareingNationAllianceId != DeclareingNationAllianceId) return false;
        if (war.DefendingNationAllianceId != DefendingNationAllianceId) return false;
        if (war.WarType != WarType) return false;
        if (!war.WarReason.equals(WarReason)) return false;
        if (war.StartDate != StartDate) return false;
        if (war.EndDate != EndDate) return false;
        if ((int) Math.round(100 * war.DefendingNationVictoryPoints) != (int) Math.round(100 * DefendingNationVictoryPoints)) return false;
        if ((int) Math.round(100 * war.DeclareingNationVictoryPoints) != (int) Math.round(100 * DeclareingNationVictoryPoints)) return false;
        if ((int) Math.round(100 * war.DevastationDeclareingNation) != (int) Math.round(100 * DevastationDeclareingNation)) return false;
        if ((int) Math.round(100 * war.DevastationDefendingNation) != (int) Math.round(100 * DevastationDefendingNation)) return false;
        if (!ResourceType.equals(war.DeclareingNationResourcesStolen, DeclareingNationResourcesStolen)) return false;
        if (!ResourceType.equals(war.DefendingNationResourcesStolen, DefendingNationResourcesStolen)) return false;
        if ((int) Math.round(100 * war.DeclareingNationLandStolen) != (int) Math.round(100 * DeclareingNationLandStolen)) return false;
        if ((int) Math.round(100 * war.DefendingNationLandStolen) != (int) Math.round(100 * DefendingNationLandStolen)) return false;
        if (Arrays.equals(war.DeclareingNationMilitaryUnitsLost, DeclareingNationMilitaryUnitsLost)) return false;
        if (Arrays.equals(war.DefendingNationMilitaryUnitsLost, DefendingNationMilitaryUnitsLost)) return false;
        return true;
    }

    public static final class DBWarKey {
        public final int id;
        public DBWarKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return ((DBWar) o).WarId == id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public long getTimeLeft() {
        return Math.max(0, EndDate - System.currentTimeMillis());
    }

    public int getAttacker_id() {
        return DeclareingNationId;
    }

    public int getDefender_id() {
        return DefendingNationId;
    }

    public int getAttacker_aa() {
        return DeclareingNationAllianceId;
    }

    public int getDefender_aa() {
        return DefendingNationAllianceId;
    }

    public WarType getWarType() {
        return WarType;
    }

    public WarStatus getStatus() {
        if (DefendingNationVictoryPoints >= 100) {
            return WarStatus.DEFENDER_VICTORY;
        }
        if (DeclareingNationVictoryPoints >= 100) {
            return WarStatus.ATTACKER_VICTORY;
        }
        if (getTimeLeft() <= 0) {
            return WarStatus.EXPIRED;
        }
        return WarStatus.ACTIVE;
    }

    public String getWarInfoEmbed(boolean isAttacker, boolean loot) {
        return getWarInfoEmbed(isAttacker, loot, true);
    }

    public String getWarInfoEmbed(boolean isAttacker, boolean loot, boolean title) {
        StringBuilder body = new StringBuilder();

        DBNation enemy = getNation(!isAttacker);
        if (enemy == null) return body.toString();
        WarCard card = new WarCard(this, false);

        if (title) {
            String typeStr = isAttacker ? "\uD83D\uDD2A" : "\uD83D\uDEE1";
            body.append(typeStr);
            body.append("`" + enemy.getNation() + "`")
                    .append(" | ").append(enemy.getAllianceName()).append(":");
        }
        // TODO FIXME :||remove loot and military markdown
//        if (loot && isAttacker) {
//            double lootValue = enemy.lootTotal();
//            body.append("$" + MathMan.format((int) lootValue));
//        }
//        body.append(enemy.toCityMilMarkedown());

        String attStr = card.condensedSubInfo(isAttacker);
        String defStr = card.condensedSubInfo(!isAttacker);
        body.append("```" + attStr + "|" + defStr + "``` ");
        body.append(StringMan.repeat("\u2501", 10) + "\n");
        return body.toString();
    }

    /**
     * Resistance
     * @return [attacker, defender]
     */
    public Map.Entry<Double, Double> getResistance() {
        return Map.entry(
        100d - DeclareingNationVictoryPoints,
        100d - DefendingNationVictoryPoints
        );
    }

    public boolean isActive() {
        return getStatus().isActive();
    }

    public int getWarId() {
        return WarId;
    }

    public long getDate() {
        return StartDate;
    }

    public String toUrl() {
        return Settings.INSTANCE.DNS_URL() + "/nation/war/timeline/war=" + WarId;
    }

    @Override
    public String toString() {
        return "{" +
                "warId=" + WarId +
                ", attacker_id=" + getAttacker_id() +
                ", defender_id=" + getDefender_id() +
                ", attacker_aa=" + getAttacker_aa() +
                ", defender_aa=" + getDefender_aa() +
                ", warType=" + getWarType() +
                ", status=" + getStatus() +
                ", date=" + getDate() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof ArrayUtil.IntKey key) {
            return key.key == WarId;
        }
        if (getClass() != o.getClass()) return false;

        DBWar dbWar = (DBWar) o;
        return dbWar.WarId == WarId;
    }

    @Override
    public int hashCode() {
        return WarId;
    }

    public Boolean isAttacker(DBNation nation) {
        if (nation.getNation_id() == getAttacker_id()) return true;
        if (nation.getNation_id() == getDefender_id()) return false;
        return null;
    }

    public DBNation getNation(Boolean attacker) {
        if (attacker == null) return null;
        return Locutus.imp().getNationDB().getNation(attacker ? getAttacker_id() : getDefender_id());
    }

    public CounterStat getCounterStat() {
        return Locutus.imp().getWarDb().getCounterStat(this);
    }

    public String getNationHtmlUrl(boolean attacker) {
        int id = attacker ? getAttacker_id() : getDefender_id();
        return MarkupUtil.htmlUrl(DNS.getName(id, false), DNS.getNationUrl(id));
    }

    public String getAllianceHtmlUrl(boolean attacker) {
        int id = attacker ? getAttacker_aa() : getDefender_aa();
        return MarkupUtil.htmlUrl(DNS.getName(id, true), DNS.getAllianceUrl(id));
    }

    public int getNationId(int allianceId) {
        if (getAttacker_aa() == allianceId) return getAttacker_id();
        if (getDefender_aa() == allianceId) return getDefender_id();
        return 0;
    }
    public boolean isAttacker(int nation_id) {
        return this.getAttacker_id() == nation_id;
    }

    public int getAllianceId(int attacker_nation_id) {
        return attacker_nation_id == this.getAttacker_id() ? this.getAttacker_aa() : (attacker_nation_id == this.getDefender_id() ? this.getDefender_aa() : 0);
    }

    public long possibleEndDate() {
        return EndDate;
    }
}