package link.locutus.discord.commands.manager.v2.impl.pw.binding;

import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.binding.annotation.Default;
import link.locutus.discord.commands.manager.v2.binding.annotation.Me;
import link.locutus.discord.commands.manager.v2.binding.annotation.TextArea;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.math.ScriptUtil;
import link.locutus.discord.util.TimeUtil;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class StandardPlaceholders {

    @Command
    String js(@Default @Me User author, @TextArea String text) throws ScriptException {
        if (text.contains("[a-zA-Z]+")) {
            if (author == null || !Roles.ADMIN.hasOnRoot(author)) {
                return null;
            }
        }
        return ScriptUtil.evalNumber(text) + "";
    }

    @Command
    public String random(List<String> args) {
        return args.get(ThreadLocalRandom.current().nextInt(args.size()));
    }

    @Command
    public String day() {
        return TimeUtil.getDay() + "";
    }

    @Command
    public String hour() {
        return TimeUtil.getHour() + "";
    }

    @Command(desc = "Get the timestamp")
    public String timestamp() {
        return System.currentTimeMillis() + "";
    }

    @Command
    public String date() {
        return Instant.now().toString();
    }
}
