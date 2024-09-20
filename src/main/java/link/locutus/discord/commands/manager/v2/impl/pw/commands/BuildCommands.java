package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.types.Building;
import link.locutus.discord.api.types.Project;
import link.locutus.discord.api.types.Technology;
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
import link.locutus.discord.util.math.ArrayUtil;
import net.dv8tion.jda.api.entities.User;

import java.util.*;
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

        String realJson = gson.toJson(ArrayUtil.sortMapKeys(buildings, Comparator.comparing(Building::getName)));
        String effectJson = gson.toJson(ArrayUtil.sortMapKeys(effectBuildings, Comparator.comparing(Building::getName)));
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
        return "Purchasing `" + building.getName() + "` from `" + start_amount + "` to `" + end_amount + "` would cost $" + MathMan.format(cost) + "\n" +
                "(Building Cost Reduction: " + buildingCostReduction + "%)";
    }

    @Command
    public String nextBuildingCost(@Me DBNation me, @Me User user, @Me GuildDB db, DBNation nation, @Default Double buildingCostReduction, @Switch("u") boolean force_update) {
        long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
        int total_slots = nation.getPrivateData().getTotalSlots(now);
        if (buildingCostReduction == null) {
            buildingCostReduction = nation.getPrivateData().getBuildingCostPercent(now);
        }
        Map<Building, Integer> counts = nation.getPrivateData().getBuildings(now, false);

        Map<Building, Double> costPerBuilding = new LinkedHashMap<>();
        Map<Project, Integer> projects = nation.getPrivateData().getProjects(now);
        Map<Technology, Integer> tech = nation.getPrivateData().getTechnology(now);
        Map<Building, String> cantBuild = new HashMap<>();

        for (Building building : Building.values) {
            String cantBuildReason = building.getCantBuildReason(p -> projects.getOrDefault(p, 0), t -> tech.getOrDefault(t, 0));
            if (cantBuildReason != null) {
                cantBuild.put(building, cantBuildReason);
                continue;
            }
            int current = counts.getOrDefault(building, 0);
            double cost = building.cost(current, current + 1, total_slots, 1 - (buildingCostReduction * 0.01));
            costPerBuilding.put(building, cost);
        }
        StringBuilder response = new StringBuilder();
        response.append("### Next Building Costs for " + nation.getMarkdownUrl() + ":\n```\n");
        for (Map.Entry<Building, Double> entry : costPerBuilding.entrySet()) {
            int currentLevel = counts.getOrDefault(entry.getKey(), 0);
            response.append(currentLevel + "x" + entry.getKey().getName() + ": $" + MathMan.format(entry.getValue()) + "\n");
        }
        response.append("```\n");
        if (!cantBuild.isEmpty()) {
            response.append("### Can't Build:\n```\n");
            for (Map.Entry<Building, String> entry : cantBuild.entrySet()) {
                response.append(entry.getKey().getName() + ": " + entry.getValue() + "\n");
            }
            response.append("```\n");
        }
        return response.toString();
    }

    @Command
    public String buildingCostBulk(@Me DBNation me, @Me User user, @Me GuildDB db, Map<Building, Integer> buildTo, @Default Integer total_slots, @Default DBNation nation, @Default Double buildingCostReduction, @Switch("u") boolean force_update) {
        long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
        Map<Building, Integer> counts = nation != null ? nation.getPrivateData().getBuildings(now, false) : Collections.emptyMap();
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

        Map<Building, Double> costPerBuilding = new LinkedHashMap<>();

        for (Map.Entry<Building, Integer> entry : buildTo.entrySet()) {
            int startAmount = counts.getOrDefault(entry.getKey(), 0);
            int endAmount = startAmount + entry.getValue();
            if (endAmount <= startAmount) {
                costPerBuilding.put(entry.getKey(), 0d);
            } else {
                double cost = entry.getKey().cost(startAmount, endAmount, total_slots, 1 - (buildingCostReduction * 0.01));
                costPerBuilding.put(entry.getKey(), cost);
            }
        }
        StringBuilder response = new StringBuilder();
        response.append("### Building Costs for " + nation.getMarkdownUrl() + ":\n```\n");
        for (Map.Entry<Building, Double> entry : costPerBuilding.entrySet()) {
            int startAmount = counts.getOrDefault(entry.getKey(), 0);
            int endAmount = startAmount + buildTo.get(entry.getKey());
            response.append(entry.getKey().getName() + " (" + startAmount + " -> " + endAmount + "): $" + MathMan.format(entry.getValue()) + "\n");
        }
        response.append("```\n");
        return response.toString();
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
            response.append("\n(Project cost reduction: `" + ((1 - factor) * 100) + "%`)");
        }

        return response.toString();
    }

    @Command(desc = "View the projects for a nation")
    public String viewProjects(@Me DBNation me, @Me User user, @Me GuildDB db, DBNation nation, @Switch("u") boolean force_update) {
        if (me.getId() != nation.getId() && (!Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) || !db.isAllianceId(nation.getAlliance_id()))) {
            throw new IllegalArgumentException("You can't view another nation's build.");
        }
        long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
        Map<Project, Integer> projects = nation.getPrivateData().getProjects(now);
        if (projects.isEmpty()) {
            return "No projects.";
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        StringBuilder response = new StringBuilder("### Projects for " + nation.getMarkdownUrl() + ":\n");
        String realJson = gson.toJson(ArrayUtil.sortMapKeys(projects, Comparator.comparing(Project::getName)));
        response.append("```json\n" + realJson + "\n```");
        return response.toString();
    }

    @Command(desc = "View the technologies for a nation")
    public String viewTechnologies(@Me DBNation me, @Me User user, @Me GuildDB db, DBNation nation, @Switch("u") boolean force_update) {
        if (me.getId() != nation.getId() && (!Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) || !db.isAllianceId(nation.getAlliance_id()))) {
            throw new IllegalArgumentException("You can't view another nation's build.");
        }
        long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
        Map<Technology, Integer> technology = nation.getPrivateData().getTechnology(now);
        if (technology.isEmpty()) {
            return "No technologies.";
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        StringBuilder response = new StringBuilder("### Technology for " + nation.getMarkdownUrl() + ":\n");
        String realJson = gson.toJson(ArrayUtil.sortMapKeys(technology, Comparator.comparing(Technology::getName)));
        response.append("```json\n" + realJson + "\n```");
        return response.toString();
    }

    @Command
    public String techCost(@Me User user, @Me GuildDB db, @Me DBNation me, Technology technology,
                           @Default DBNation nation,
                           @Default Integer start_level,
                           @Default Integer end_level,
                           @Default Integer acquired_technologies,
                           @Default Integer sci_level,
                           @Default Integer ai_level,
                           @Default Double techCostReduction, @Switch("u") boolean force_update) {
        if (nation != null && (me.getId() != nation.getId() && (!Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) || !db.isAllianceId(nation.getAlliance_id())))) {
            throw new IllegalArgumentException("You can't view another nation's build.");
        }
        long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
        if (techCostReduction == null) {
            if (nation != null) {
                techCostReduction = nation.getPrivateData().getTechCostPercent(now);
            } else {
                techCostReduction = 0d;
            }
        }
        if (nation == null && (start_level == null || sci_level == null || ai_level == null || acquired_technologies == null)) {
            throw new IllegalArgumentException("You must provide a `nation` or ALL OF `start_level`, `acquired_technologies`, `sci_level`, and `ai_level`.");
        }
        if (nation != null) {
            if (start_level == null) {
                start_level = nation.getPrivateData().getTechnology(now).getOrDefault(technology, 0);
            }
            if (sci_level == null) {
                sci_level = nation.getPrivateData().getTechnology(now).getOrDefault(Technology.SCIENTIFIC_THEORY, 0);
            }
            if (ai_level == null) {
                ai_level = nation.getPrivateData().getTechnology(now).getOrDefault(Technology.ARTIFICIAL_INTELLIGENCE, 0);
            }
            if (acquired_technologies == null) {
                acquired_technologies = nation.getPrivateData().getTechnology(now).values().stream().mapToInt(Integer::intValue).sum();
            }
        }

        if (end_level == null) {
            end_level = start_level + 1;
        }

        double techFactor = 1 - (techCostReduction * 0.01);
        long cost = technology.getCost(techFactor, acquired_technologies, sci_level, ai_level, start_level, end_level);
        return "Purchasing `" + technology.getName() + "` from `" + start_level + "` to `" + end_level + "` would cost `tech:" + MathMan.format(cost) + "`\n" +
                "(Tech Cost Reduction: " + techCostReduction + "%)";
    }

    @Command(desc = "Get cost of purchasing an amount of land")
    public String landCost(@Me User user, @Me GuildDB db, @Me DBNation me, double buy_up_to, @Default DBNation nation, @Default Double current_land, @Default Double land_cost_reduction, @Switch("u") boolean force_update) {
        if (nation == null && (current_land == null)) {
            throw new IllegalArgumentException("You must provide a `nation` or `current_land`.");
        }
        if (nation == null && land_cost_reduction == null) {
            throw new IllegalArgumentException("You must provide a `nation` or `land_cost_reduction`.");
        }
        if (current_land == null) {
            current_land = nation.getLand();
        }
        if (buy_up_to < current_land) {
            throw new IllegalArgumentException("The value for `buy_up_to` (" + MathMan.format(buy_up_to) + ") must be greater than `current_land` (" + MathMan.format(current_land) + ")");
        }
        if (force_update) {
            if (nation == null) throw new IllegalArgumentException("You must provide a `nation` to force update.");
            Locutus.imp().runEventsAsync(events -> Locutus.imp().getNationDB().updateNation(db.getApiOrThrow(), nation, events));
        }
        if (land_cost_reduction == null) {
            land_cost_reduction = 0d;
            if (nation != null) {
                long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
                if (me.getId() != nation.getId() && (!Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) || !db.isAllianceId(nation.getAlliance_id()))) {
                    throw new IllegalArgumentException("You can't view another nation's build.");
                }
                land_cost_reduction = nation.getPrivateData().getLandCostPercent(now);
            }
        }
        double costReductionFactor = 1 - (land_cost_reduction * 0.01);
        double cost = DNS.Land.getCost(costReductionFactor, current_land, buy_up_to - current_land);
        return "Purchasing `" + MathMan.format(buy_up_to) + "` land would cost `$" + MathMan.format(cost) + "`\n" +
                "(Land Cost Reduction: " + land_cost_reduction + "%)";
    }

    @Command
    public String devCost(@Me User user, @Me GuildDB db, @Me DBNation me, double buy_up_to, @Default DBNation nation, @Default Double current_dev, @Default Double current_land, @Default Double dev_cost_reduction, @Switch("u") boolean force_update) {
        if (nation == null && (current_dev == null || current_land == null)) {
            throw new IllegalArgumentException("You must provide a `nation` or BOTH OF `current_dev`, `current_land`.");
        }
        if (nation == null && dev_cost_reduction == null) {
            throw new IllegalArgumentException("You must provide a `nation` or `dev_cost_reduction`.");
        }
        if (current_dev == null) {
            current_dev = nation.getInfra();
        }
        if (current_land == null) {
            current_land = nation.getLand();
        }
        if (buy_up_to < current_dev) {
            throw new IllegalArgumentException("The value for `buy_up_to` (" + MathMan.format(buy_up_to) + ") must be greater than `current_land` (" + MathMan.format(current_dev) + ")");
        }
        if (force_update) {
            if (nation == null) throw new IllegalArgumentException("You must provide a `nation` to force update.");
            Locutus.imp().runEventsAsync(events -> Locutus.imp().getNationDB().updateNation(db.getApiOrThrow(), nation, events));
        }
        if (dev_cost_reduction == null) {
            dev_cost_reduction = 0d;
            if (nation != null) {
                long now = System.currentTimeMillis() - (force_update ? 0 : TimeUnit.HOURS.toMillis(1));
                if (me.getId() != nation.getId() && (!Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) || !db.isAllianceId(nation.getAlliance_id()))) {
                    throw new IllegalArgumentException("You can't view another nation's build.");
                }
                dev_cost_reduction = nation.getPrivateData().getDevelopmentCostPercent(now);
            }
        }
        double costReductionFactor = 1 - (dev_cost_reduction * 0.01);
        double cost = DNS.Development.getCost(costReductionFactor, current_dev, current_land, buy_up_to - current_dev);
        return "Purchasing `" + MathMan.format(buy_up_to) + "` development would cost `$" + MathMan.format(cost) + "`\n" +
                "(Dev Cost Reduction: " + dev_cost_reduction + "%)";
    }

    // Maybe unit cost (minerals, prod) and training cash amounts
    // bulk build cost
    // inventory
    // mmr
}
