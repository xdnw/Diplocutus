package link.locutus.discord.commands.manager.v2.impl.pw.commands;


import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.manager.v2.binding.annotation.Arg;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.binding.annotation.Switch;
import link.locutus.discord.util.MathMan;

import java.util.Iterator;
import java.util.Map;

public class TradeCommands {
    @Command(desc = "Show the total market value of resource amounts", aliases = {"resourcevalue", "convertedtotal"})
    public String convertedTotal(Map<ResourceType, Double> resources,
                                 @Arg("Remove negative amounts and return the scaled resource amounts of equivalent value")
                                 @Switch("n") boolean normalize,
                                 @Arg("Show total value in a resource instead of money")
                                 @Switch("t") ResourceType convertType) {
        if (normalize) {
            double total = ResourceType.convertedTotal(resources);
            if (total <= 0) {
                return "Total is negative";
            }
            double postiveTotal = ResourceType.convertedTotal(resources);
            double factor = total / postiveTotal;
            for (ResourceType type : ResourceType.values()) {
                Double value = resources.get(type);
                if (value == null || value == 0) continue;

                resources.put(type, value * factor);
            }
        }

        StringBuilder result = new StringBuilder("```" + ResourceType.resourcesToString(resources) + "```");

        double value = ResourceType.convertedTotal(resources);
        result.append("\n" + "Worth: $" + MathMan.format(value));
        if (convertType != null && convertType != ResourceType.CASH) {
            double convertTypeValue = ResourceType.convertedTotal(convertType, 1);
            double amtConvertType = value / convertTypeValue;
            result.append(" OR " + MathMan.format(amtConvertType) + "x " + convertType);
        }
        return result.toString();
    }
}