package link.locutus.discord.api.types;

public enum CombatAction {
    GROUND_INVASION(CombatType.GROUND),
    SKIRMISH(CombatType.GROUND),
    RAID(CombatType.GROUND),
    ENTRENCH(CombatType.GROUND),
    ARTILLERY_BOMBARDMENT(CombatType.GROUND),
    MISSILE_STRIKE(CombatType.GROUND),
    GROUND_RESUPPLY(CombatType.GROUND),

    AIR_SUPREMACY(CombatType.AIR),
    AIR_SKIRMISH(CombatType.AIR),
    AIR_DISRUPTION(CombatType.AIR),
    PORT_STRIKE(CombatType.AIR),
    NAVY_STRIKE(CombatType.AIR),
    GROUND_STRIKE(CombatType.AIR),
    BOMB_SUPPLY_LINES(CombatType.AIR),
    BOMB_INFRASTRUCTURE(CombatType.AIR),
    PARADROP(CombatType.AIR),
    RESUPPLY(CombatType.AIR),
    AERIAL_SURVEILLANCE(CombatType.AIR),
    CRUISE_MISSILE_STRIKE(CombatType.AIR),
    NUCLEAR_MISSILE_STRIKE(CombatType.AIR),

    NAVAL_BATTLE(CombatType.NAVAL),
    NAVAL_SKIRMISH(CombatType.NAVAL),
    NAVAL_BLOCKADE(CombatType.NAVAL),
    SUBMARINE_ASSAULT(CombatType.NAVAL),
    SHORE_BOMBARDMENT(CombatType.NAVAL),
    CRUISER_MISSILE_STRIKE(CombatType.NAVAL),
    NAVAL_LANDINGS(CombatType.NAVAL),
    NAVAL_RESUPPLY(CombatType.NAVAL),

    PEACE(CombatType.PEACE)

    ;

    CombatType type;
    CombatAction(CombatType type) {
        this.type = type;
    }
}
