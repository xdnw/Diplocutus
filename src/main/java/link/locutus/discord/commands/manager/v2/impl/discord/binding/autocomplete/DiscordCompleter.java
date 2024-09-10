package link.locutus.discord.commands.manager.v2.impl.discord.binding.autocomplete;

import link.locutus.discord.api.types.Project;
import link.locutus.discord.commands.manager.v2.binding.BindingHelper;
import link.locutus.discord.commands.manager.v2.binding.FunctionConsumerParser;
import link.locutus.discord.commands.manager.v2.binding.Key;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;
import link.locutus.discord.commands.manager.v2.binding.annotation.Autocomplete;
import link.locutus.discord.commands.manager.v2.binding.annotation.Binding;
import link.locutus.discord.commands.manager.v2.binding.annotation.Me;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.DiscordBindings;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.StringMan;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class DiscordCompleter extends BindingHelper {
    public final Set<Role> ROLES_KEY = null;
    public final Set<Member> MEMBERS_KEY = null;
    public final Set<Project> PROJECTS_KEY = null;

    {
        try {
            {
                Type type = getClass().getDeclaredField("MEMBERS_KEY").getGenericType();
                Key key = Key.of(type, Autocomplete.class);
                addBinding(store -> store.addParser(key, new FunctionConsumerParser(key, (BiFunction<ValueStore, Object, Object>) (valueStore, input) -> {
                    Guild guild = (Guild) valueStore.getProvided(Key.of(Guild.class, Me.class));

                    List<Member> options = guild.getMembers();
                    return StringMan.autocompleteComma(input.toString(), options, s -> DiscordBindings.member(guild, null, s), Member::getEffectiveName, IMentionable::getAsMention, OptionData.MAX_CHOICES);
                })));
            }
            {
                Type type = getClass().getDeclaredField("ROLES_KEY").getGenericType();
                Key key = Key.of(type, Autocomplete.class);
                addBinding(store -> store.addParser(key, new FunctionConsumerParser(key, (BiFunction<ValueStore, Object, Object>) (valueStore, input) -> {
                    Guild guild = (Guild) valueStore.getProvided(Key.of(Guild.class, Me.class));

                    List<Role> options = guild.getRoles();
                    return StringMan.autocompleteComma(input.toString(), options, s -> DiscordBindings.role(guild, s), Role::getName, IMentionable::getAsMention, OptionData.MAX_CHOICES);
                })));
            }
            {
                Type type = getClass().getDeclaredField("PROJECTS_KEY").getGenericType();
                Key key = Key.of(type, Autocomplete.class);
                addBinding(store -> store.addParser(key, new FunctionConsumerParser(key, (BiFunction<ValueStore, Object, Object>) (valueStore, input) -> {

                    List<Project> options = Arrays.asList(Project.values);
                    return StringMan.autocompleteComma(input.toString(), options, Project::parse, Project::name, Project::name, OptionData.MAX_CHOICES);
                })));
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Autocomplete
    @Binding(types = {Category.class})
    public List<String> category(@Me Member member, @Me Guild guild, String input) {
        List<Category> categories = new ArrayList<>(guild.getCategories());
        categories.removeIf(f -> !f.getMembers().contains(member));
        if (!input.isEmpty()) {
            categories = StringMan.getClosest(input, categories, Channel::getName, OptionData.MAX_CHOICES, true);
        }

        return categories.stream().map(Channel::getAsMention).collect(Collectors.toList());
    }

    @Autocomplete
    @Binding(types = {Guild.class})
    public List<Map.Entry<String, String>> Guild(@Me User user, String input) {
        List<Guild> options = user.getMutualGuilds();
        options = StringMan.getClosest(input, options, Guild::getName, OptionData.MAX_CHOICES, true, true);
        return options.stream().map(f -> new AbstractMap.SimpleEntry<>(f.getName(), f.getIdLong() + "")).collect(Collectors.toList());
    }

    @Autocomplete
    @Binding(types = {Roles.class})
    public List<String> Roles(String input) {
        return StringMan.completeEnum(input, Roles.class);
    }

    @Autocomplete
    @Binding(types = {Permission.class})
    public List<String> Permission(String input) {
        return StringMan.completeEnum(input, Permission.class);
    }

    @Autocomplete
    @Binding(types = {OnlineStatus.class})
    public List<String> onlineStatus(String input) {
        return StringMan.completeEnum(input, OnlineStatus.class);
    }

    @Autocomplete
    @Binding(types = {Role.class})
    public List<Map.Entry<String, String>> role(@Me Guild guild, String input) {
        List<Role> options = guild.getRoles();
        List<Role> closest = StringMan.getClosest(input, options, true);
        return StringMan.autocompletePairs(closest, Role::getName, IMentionable::getAsMention);
    }
    //// TODO start here
//
//    @Binding
//    public Set<Member> members(@Me Guild guild, String input) {
//        Set<Member> members = new LinkedHashSet<>();
//        for (String arg : input.split("[|]+")) {
//            if (arg.equalsIgnoreCase("*")) {
//                members.addAll(guild.getMembers());
//            } else if (arg.equalsIgnoreCase("*,#verified=0") || arg.equalsIgnoreCase("#verified=0,*")) {
//                for (Member member : guild.getMembers()) {
//                    if (DiscordUtil.getNation(member.getUser()) == null) {
//                        members.add(member);
//                    }
//                }
//            } else {
//                Set<DBNation> nations = DiscordUtil.parseNations(guild, arg);
//                for (Member member : guild.getMembers()) {
//                    DBNation nation = DiscordUtil.getNation(member.getUser());
//                    if (nation != null && nations.contains(nation)) {
//                        members.add(member);
//                    }
//                }
//            }
//        }
//        return members;
//    }
//
//    @Binding
//    public Guild guild(long guildId) {
//        Guild guild = Locutus.imp().getDiscordApi().getGuildById(guildId);
//        if (guild == null) throw new IllegalArgumentException("No guild found for: " + guildId);
//        return guild;
//    }
//
//    @Binding
//    public MessageChannel channel(Guild guild, String channel) {
//        MessageChannel GuildMessageChannel = DiscordUtil.getChannel(guild, channel);
//        if (GuildMessageChannel == null) throw new IllegalArgumentException("No channel found for " + channel);
//        return GuildMessageChannel;
//    }
//
}
