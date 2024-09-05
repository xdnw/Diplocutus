package link.locutus.discord.commands.manager.v2.impl.pw.binding;

import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.manager.v2.binding.BindingHelper;
import link.locutus.discord.commands.manager.v2.binding.annotation.Binding;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.math.ArrayUtil;

import java.util.Map;

public class PWMath2Type extends BindingHelper {
    @Binding
    public Map<ResourceType, Double> toDoubleArray(ArrayUtil.DoubleArray input) {
        return ResourceType.resourcesToMap(input.toArray());
    }
}
