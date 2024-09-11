package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.types.DepositType;
import link.locutus.discord.api.types.FlowType;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.command.ICommand;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.CommandManager2;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.ImageUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.discord.DiscordUtil;
import org.json.JSONObject;

import java.util.*;

public class TestCommands {
    @Command(desc = "Dummy command. No output")
    public String dummy() {
        return null;
    }

    @Command(desc = "Create a discord modal for a bot command\n" +
            "This will make a popup prompting for the command arguments you specify and submit any defaults you provide\n" +
            "Note: This is intended to be used in conjuction with the card command")
    public String modal(@Me IMessageIO io, ICommand command,
                        @Arg("A comma separated list of the command arguments to prompt for") String arguments,
                        @Arg("The default arguments and values you want to submit to the command\n" +
                                "Example: `myarg1:myvalue1 myarg2:myvalue2`")
                        @Default String defaults) {
        Map<String, String> args;
        if (defaults == null) {
            args = new HashMap<>();
        } else if (defaults.startsWith("{") && defaults.endsWith("}")) {
            args = DNS.parseMap(defaults);
        } else {
            args = CommandManager2.parseArguments(command.getUserParameterMap().keySet(), defaults, true);
        }
        io.modal().create(command, args, StringMan.split(arguments, ',')).send();
        return null;
    }

    @Command(desc = "Get the text from a discord image\n" +
            "It is recommended to crop the image first")
    public String ocr(String discordImageUrl) {
        String text = ImageUtil.getText(discordImageUrl);
        return "```\n" +text + "\n```\n";
    }

    @Command(desc = "Shift the transfer note notegory flows for a nation.\n" +
            "For adjusting whether amounts are internal, withdrawn or deposited.\n" +
            "Does not change overall or note balance unless it is shifted to `#ignore`")
    @RolePermission(value = Roles.ECON)
    public String shiftFlow(@Me GuildDB db, @Me DBNation me, @Me IMessageIO io, @Me JSONObject command,
                            DBNation nation,
                            DepositType noteFrom,
                            FlowType flowType,
                            Map<ResourceType, Double> amount,
                            @Default DepositType noteTo,
                            @Switch("a") DBAlliance alliance,
                            @Switch("f") boolean force) {
        if (noteTo == null) noteTo = DepositType.DEPOSIT;
        if (noteFrom == noteTo) {
            return "Cannot shift flow from `" + noteFrom.name() + "` to `" + noteTo.name() + "` please specify another `noteTo`";
        }
        long date = System.currentTimeMillis();
        String fromStr = "#" + noteFrom.name().toLowerCase(Locale.ROOT);
        String toStr = "#" + noteTo.name().toLowerCase(Locale.ROOT);
        long fromId;
        int fromType;
        String fromUrl;
        Set<Integer> ids = db.getAllianceIds();
        if (alliance != null && !ids.contains(alliance.getId())) {
            throw new IllegalArgumentException("Alliance " + alliance.getName() + " is not registered to this guild: " + CM.settings_default.registerAlliance.cmd.toSlashMention());
        }
        if (ids.isEmpty()) {
            fromId = db.getIdLong();
            fromUrl = DiscordUtil.getGuildName(db.getIdLong());
            fromType = db.getReceiverType();
        } else {
            fromType = 2;
            if (alliance != null) {
                fromId = alliance.getId();
            } else if (ids.contains(nation.getAlliance_id())) {
                fromId = nation.getAlliance_id();
            } else {
                fromId = ids.iterator().next();
            }
            fromUrl = DNS.getMarkdownUrl((int) fromId, true);
        }

        double[] amtNeg = ResourceType.builder().subtract(amount).build();
        double[] amtPos = ResourceType.builder().add(amount).build();

        List<Runnable> tasks = new ArrayList<>();
        List<String> messages = new ArrayList<>();


        switch (flowType) {
            case INTERNAL -> {
                tasks.add(() -> db.addBalance(date, nation, me.getId(), fromStr, amtPos));
                tasks.add(() -> db.addBalance(date, nation, me.getId(), toStr, amtNeg));
                messages.add(flowType + " transfer " + ResourceType.toString(amtNeg) + " note: `" + fromStr + "`");
                messages.add(flowType + " transfer " + ResourceType.toString(amtPos) + " note: `" + toStr + "`");
            }
            case WITHDRAWAL -> {
                tasks.add(() -> db.addTransfer(date, fromId, fromType, nation, me.getId(), fromStr, amtPos));
                tasks.add(() -> db.addTransfer(date, fromId, fromType, nation, me.getId(), toStr, amtNeg));
                messages.add(flowType + " transfer " + ResourceType.toString(amtNeg) + " note: `" + fromStr + "` sender: " + fromUrl);
                messages.add(flowType + " transfer " + ResourceType.toString(amtPos) + " note: `" + toStr + "` sender: " + fromUrl);
            }
            case DEPOSIT -> {
                tasks.add(() -> db.addTransfer(date, nation, fromId, fromType, me.getId(), fromStr, amtPos));
                tasks.add(() -> db.addTransfer(date, nation, fromId, fromType, me.getId(), toStr, amtNeg));
                messages.add(flowType + " transfer " + ResourceType.toString(amtNeg) + " note: `" + fromStr + "` receiver: " + fromUrl);
                messages.add(flowType + " transfer " + ResourceType.toString(amtPos) + " note: `" + toStr + "` receiver: " + fromUrl);
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + flowType);
        }

        if (!force) {
            String title = "Add Transfer Flow: " + nation.getName();
            StringBuilder body = new StringBuilder();
            body.append(nation.getNationUrlMarkup(true) + " | " + nation.getAllianceUrlMarkup(true)).append("\n");
            body.append("Worth: `$" + MathMan.format(ResourceType.convertedTotal(amount)) + "`\n- ");
            body.append(StringMan.join(messages, "\n- "));
            io.create().confirmation(title, body.toString(), command).send();
            return null;
        }

        for (Runnable task : tasks) {
            task.run();
        }

        return "Done!\n- " + StringMan.join(messages, "\n- ");
    }

    @Command(desc = "Check the flow for a specific transaction note, showing the net by internal addbalance, withdrawals, and deposits")
    @RolePermission(value = Roles.ECON_STAFF)
    public String viewFlow(@Me GuildDB db, DBNation nation, DepositType note) {

        // public List<Map.Entry<Integer, Transaction2>> getTransactions(GuildDB db, Set<Long> tracked, boolean useTaxBase, boolean offset, long updateThreshold, long cutOff, boolean priority) {
        List<Map.Entry<Integer, Transaction2>> transfers = nation.getTransactions(db, null, false,  0, 0);

        if (note != null) {
            String noteStr = "#" + note.name().toLowerCase(Locale.ROOT);
            transfers.removeIf(f -> !DNS.parseTransferHashNotes(f.getValue().note).containsKey(noteStr));
        }
        double[] manual = FlowType.INTERNAL.getTotal(transfers, nation.getNation_id());
//      - Amount withdrawn via a # note
        double[] withdrawn = FlowType.WITHDRAWAL.getTotal(transfers, nation.getNation_id());
//      - Amount deposit via a # note
        double[] deposited = FlowType.DEPOSIT.getTotal(transfers, nation.getNation_id());

        StringBuilder response = new StringBuilder();
        response.append("**" + FlowType.INTERNAL + "**: worth `$" + MathMan.format(ResourceType.convertedTotal(manual)) + "`\n");
        response.append("```json\n" + ResourceType.toString(manual) + "\n```\n");
//        response.append("Withrawal:\n```json\n" + ResourceType.toString(withdrawn) + "\n```\n");
        response.append("**" + FlowType.WITHDRAWAL + "**: worth `$" + MathMan.format(ResourceType.convertedTotal(withdrawn)) + "`\n");
        response.append("```json\n" + ResourceType.toString(withdrawn) + "\n```\n");
//        response.append("Deposits:\n```json\n" + ResourceType.toString(deposited) + "\n```\n");
        response.append("**" + FlowType.DEPOSIT + "**: worth `$" + MathMan.format(ResourceType.convertedTotal(deposited)) + "`\n");
        response.append("```json\n" + ResourceType.toString(deposited) + "\n```\n");
        return response.toString();
    }
}