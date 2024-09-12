package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import link.locutus.discord.api.types.Building;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.binding.annotation.Me;
import link.locutus.discord.commands.manager.v2.binding.annotation.Switch;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.components.NationPrivate;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.MathMan;
import net.dv8tion.jda.api.entities.User;

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
}
