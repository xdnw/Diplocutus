package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.types.Building;
import link.locutus.discord.api.types.Project;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.binding.annotation.Default;
import link.locutus.discord.commands.manager.v2.binding.annotation.Me;
import link.locutus.discord.commands.manager.v2.binding.annotation.Switch;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.components.NationPrivate;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.MathMan;
import net.dv8tion.jda.api.entities.User;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BuildCommands {

    @Command(desc = "Get a nations build")
    @RolePermission(Roles.MEMBER)
    public String getBuild(@Me GuildDB db, @Me DBNation me, @Me User user, DBNation nation, @Switch("u") boolean update) {
        if (me.getId() != nation.getId() && (!Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) || !db.isAllianceId(nation.getAlliance_id()))) {
            throw new IllegalArgumentException("You can't view another nation's build.");
        }
        double costReduction = 1;
        long now = System.currentTimeMillis() - (update ? 0 : TimeUnit.HOURS.toMillis(1));

        NationPrivate data = nation.getPrivateData();
        Map<Building, Integer> buildings = data.getBuildings(now, false);
        Map<Building, Integer> effectBuildings = data.getEffectBuildings(now);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        StringBuilder response = new StringBuilder();

        int openSlots = data.getOpenSlots(now);
        int totalSlots = data.getTotalSlots(now);
        int numEffectBuildings = effectBuildings.values().stream().mapToInt(Integer::intValue).sum();
        response.append("## " + (totalSlots - openSlots) + "/" + totalSlots + " slots used (+ " + numEffectBuildings + " from effects)\n");

        int employment = Building.getJobs(buildings);
        int effectEmployment = Building.getJobs(effectBuildings);
        response.append("Employment: `" + MathMan.format(employment + effectEmployment) + "(" + MathMan.format(effectEmployment) + " from effects)`\n");

        double cost = 0;
        int num = 0;
        for (Map.Entry<Building, Integer> entry : buildings.entrySet()) {
            cost += entry.getKey().cost(0, entry.getValue(), totalSlots, costReduction);
            num += entry.getValue();
        }
        response.append("Buildings: `" + num + "`\n");
        response.append("Worth: `$" + MathMan.format(cost) + "`\n");

        String realJson = gson.toJson(buildings);
        String effectJson = gson.toJson(effectBuildings);
        response.append("\n### Purchased:\n```json\n" + realJson + "\n```");
        if (!effectBuildings.isEmpty()) {
            response.append("\n### From Effects:\n```json\n" + effectJson + "\n```");
        }
        return response.toString();
    }

    @Command
    public String buildingCost(@Me DBNation me, @Me User user, @Me GuildDB db, Building building, int start_amount, int end_amount, @Default Integer total_slots, @Default DBNation nation, @Default Double buildingCostReduction, @Switch("u") boolean force_update) {
        long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
        if (total_slots == null) {
            if (nation == null) {
                throw new IllegalArgumentException("You must provide either `nation` or `total_slots`.");
            }
            if (me.getId() != nation.getId() && (!Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) || !db.isAllianceId(nation.getAlliance_id()))) {
                throw new IllegalArgumentException("You can't view another nation's build.");
            }
            total_slots = nation.getPrivateData().getTotalSlots(now);
            buildingCostReduction = nation.getPrivateData().getBuildingCostPercent(now);
        }
        if (buildingCostReduction == null) {
            buildingCostReduction = 0d;
        }
        double cost = building.cost(start_amount, end_amount, total_slots, 1 - (buildingCostReduction * 0.01));
        return "Purchasing `" + building.getName() + "` from `" + start_amount + "` to `" + end_amount + "` would cost $" + MathMan.format(cost);
    }

    @Command
    public String projectCost(@Me DBNation me, @Me User user, @Me GuildDB db, Project project, @Default DBNation nation, @Default Integer start_amount, @Default Integer end_amount, @Default Double development, @Default Double land, @Default Double projectCostReduction, @Switch("u") boolean force_update) {
        boolean isInvestment = project.isInvestment();
        boolean canViewPrivate = nation != null && (me.getId() == nation.getId() || (Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) && db.isAllianceId(nation.getAlliance_id())));
        long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
        NationPrivate data = canViewPrivate ? nation.getPrivateData() : null;

        if (start_amount == null) {
            if (data != null && isInvestment) {
                start_amount = data.getProjects(now).getOrDefault(project, 0);
            } else {
                start_amount = 0;
            }
        }
        if (end_amount == null) {
            end_amount = start_amount + 1;
        }
        if (development == null) {
            if (nation != null && isInvestment) {
                development = nation.getInfra();
            } else if (isInvestment) {
                throw new IllegalArgumentException("You must provide either a `nation` or `development` for investment projects.");
            } else {
                development = 0d;
            }
        }
        if (land == null) {
            if (nation != null) {
                land = nation.getLand();
            } else if (isInvestment) {
                throw new IllegalArgumentException("You must provide either a `nation` or `land` for investment projects.");
            } else {
                land = 0d;
            }
        }
        Map<ResourceType, Double> cost;
        if (isInvestment) {
            cost = project.getCost(start_amount, end_amount, development, land);
        } else {
            cost = project.getCost(development, land, 1);
        }
        Map<ResourceType, Double> costReduced = cost;
        double factor = 1;
        if (data != null) {
            factor = 1 - (data.getProjectCostPercent(now) * 0.01);
            if (factor != 1) {
                costReduced = DNS.multiply(new LinkedHashMap<>(cost), factor);
            }
        }
        StringBuilder response = new StringBuilder();
        response.append("Purchasing `" + project.getName() + "` from `" + start_amount + "` to `" + end_amount + "` would cost:\n");
        response.append("```\n" + ResourceType.resourcesToString(costReduced) + "\n``` worth: ~$" + MathMan.format(ResourceType.convertedTotal(costReduced)));
        if (factor != 1) {
            response.append("\nProject cost factor of: `" + (factor * 100) + "%`");
        }
        return response.toString();
    }

    // dev cost
    // land cost
    // project cost
    // bulk build cost
}
