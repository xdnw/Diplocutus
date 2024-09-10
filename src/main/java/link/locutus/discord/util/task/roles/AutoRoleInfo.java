package link.locutus.discord.util.task.roles;

import link.locutus.discord.db.GuildDB;
import link.locutus.discord.util.RateLimitUtil;
import link.locutus.discord.util.math.CIEDE2000;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AutoRoleInfo {

    private final Map<String, RoleOrCreate> createMap;
    private final Map<Member, Set<RoleAdd>> addRoles;
    private final Map<Member, Set<Role>> removeRoles;
    private final Map<Member, String> nickSet;
    private final GuildDB db;
    private final Map<Member, List<String>> errors;
    private final Map<Member, List<String>> success;
    private final String syncDbResult;

    public AutoRoleInfo(GuildDB db, String syncDbResult) {
        this.db = db;
        this.syncDbResult = syncDbResult;
        this.createMap = new LinkedHashMap<>();
        this.addRoles = new LinkedHashMap<>();
        this.removeRoles = new LinkedHashMap<>();
        this.nickSet = new LinkedHashMap<>();
        this.errors = new LinkedHashMap<>();
        this.success = new LinkedHashMap<>();
    }

    public String getSyncDbResult() {
        return syncDbResult;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (!createMap.isEmpty()) {
            result.append("Create Roles:\n");
            for (RoleOrCreate create : createMap.values()) {
                result.append("- " + create.name).append("\n");
            }
        }
        if (!addRoles.isEmpty()) {
            result.append("Add Roles:\n");
            for (Map.Entry<Member, Set<RoleAdd>> entry : addRoles.entrySet()) {
                String roleNames = entry.getValue().stream().map(roleAdd -> roleAdd.role.name).collect(Collectors.joining(", "));
                result.append("- ").append(entry.getKey().getEffectiveName()).append(" -> ").append(roleNames).append("\n");
            }
        }
        if (!removeRoles.isEmpty()) {
            result.append("Remove Roles:\n");
            for (Map.Entry<Member, Set<Role>> entry : removeRoles.entrySet()) {
                String roleNames = entry.getValue().stream().map(Role::getName).collect(Collectors.joining(", "));
                result.append("- ").append(entry.getKey().getEffectiveName()).append(" -> ").append(roleNames).append("\n");
            }
        }
        if (!nickSet.isEmpty()) {
            result.append("Set Nicknames:\n");
            for (Map.Entry<Member, String> entry : nickSet.entrySet()) {
                result.append("- ").append(entry.getKey().getEffectiveName()).append(" -> ").append(entry.getValue()).append("\n");
            }
        }
        return result.toString();
    }

    public String getChangesAndErrorMessage() {
        StringBuilder response = new StringBuilder();
        if (!errors.isEmpty()) {
            response.append("Errors:\n");
            for (Map.Entry<Member, List<String>> entry : errors.entrySet()) {
                response.append("- ").append(entry.getKey().getEffectiveName()).append(" -> ").append(String.join(", ", entry.getValue())).append("\n");
            }
        }
        if (!success.isEmpty()) {
            response.append("Success:\n");
            for (Map.Entry<Member, List<String>> entry : success.entrySet()) {
                response.append("- ").append(entry.getKey().getEffectiveName()).append(" -> ").append(String.join(", ", entry.getValue())).append("\n");
            }
        }
        if (response.isEmpty()) {
            response.append("No changes");
        }
        return response.toString();
    }

    public RoleOrCreate createRole(Role role, String roleName, int position, Supplier<Color> color) {
        RoleOrCreate create;
        if (role != null) {
            create = new RoleOrCreate(role, roleName, position, color);
        } else {
            create = createMap.computeIfAbsent(roleName, k -> new RoleOrCreate(role, roleName, position, color));
        }
        return create;
    }

    public Map<Member, List<String>> getErrors() {
        return errors;
    }

    public Map<Member, List<String>> getSuccess() {
        return success;
    }

    /**
     * Execute the changes
     * @return a list of error messages grouped by member
     */
    public void execute() {
        errors.clear();
        success.clear();

        for (Map.Entry<Member, Set<RoleAdd>> entry : addRoles.entrySet()) {
            for (RoleAdd roleAdd : entry.getValue()) {
                roleAdd.submit(db.getGuild());
            }
        }

        List<CompletableFuture> tasks = new ArrayList<>();
        // removeRoles
        for (Map.Entry<Member, Set<Role>> entry : removeRoles.entrySet()) {
            Member member = entry.getKey();
            for (Role role : entry.getValue()) {
                if (!member.getRoles().contains(role)) {
                    errors.computeIfAbsent(member, k -> new ArrayList<>()).add("Failed to remove role `" + role.getName() + "`: Member does not have role");
                    continue;
                }
                try {
                    tasks.add(RateLimitUtil.queue(db.getGuild().removeRoleFromMember(member, role)).thenAccept(v -> {
                        success.computeIfAbsent(member, k -> new ArrayList<>()).add("Removed role `" + role.getName() + "` from " + member.getEffectiveName());
                    }));
                } catch (PermissionException e) {
                    errors.computeIfAbsent(member, k -> new ArrayList<>()).add("Failed to remove role `" + role.getName() + "`: " + e.getMessage());
                }
            }
        }

        for (Map.Entry<Member, String> entry : nickSet.entrySet()) {
            Member member = entry.getKey();
            String nick = entry.getValue();
            if (nick == null) {
                // remove nick
                if (member.getNickname() == null) {
                    errors.computeIfAbsent(member, k -> new ArrayList<>()).add("Failed to remove nickname: Member does not have nickname");
                    continue;
                }
                try {
                    tasks.add(RateLimitUtil.queue(db.getGuild().modifyNickname(member, null)).thenAccept(v -> {
                        success.computeIfAbsent(member, k -> new ArrayList<>()).add("Removed nickname from " + member.getEffectiveName());
                    }));
                } catch (PermissionException e) {
                    errors.computeIfAbsent(member, k -> new ArrayList<>()).add("Failed to remove nickname: " + e.getMessage());
                }
            } else {
                // set nick
                if (member.getNickname() != null && member.getNickname().equals(nick)) {
                    errors.computeIfAbsent(member, k -> new ArrayList<>()).add("Failed to set nickname: Member already has nickname");
                    continue;
                }
                try {
                    tasks.add(RateLimitUtil.queue(db.getGuild().modifyNickname(member, nick)).thenAccept(v -> {
                        success.computeIfAbsent(member, k -> new ArrayList<>()).add("Set nickname of " + member.getEffectiveName() + " to `" + nick + "`");
                    }));
                } catch (PermissionException e) {
                    errors.computeIfAbsent(member, k -> new ArrayList<>()).add("Failed to set nickname: " + e.getMessage());
                }
            }
        }

        // addRoles
        for (Map.Entry<Member, Set<RoleAdd>> entry : addRoles.entrySet()) {
            Member member = entry.getKey();
            for (RoleAdd roleAdd : entry.getValue()) {
                if (!roleAdd.get()) {
                    errors.computeIfAbsent(member, k -> new ArrayList<>()).add(roleAdd.failedMessage);
                } else {
                    success.computeIfAbsent(member, k -> new ArrayList<>()).add("Added role `" + roleAdd.role.name + "` to " + member.getEffectiveName());
                }
            }
        }

        // poll tasks
        for (CompletableFuture task : tasks) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        this.createMap.clear();
        this.addRoles.clear();
        this.removeRoles.clear();
        this.nickSet.clear();
    }

    public void logError(Member member, String error) {
        errors.computeIfAbsent(member, k -> new ArrayList<>()).add(error);
    }

    public void addRoleToMember(Member member, RoleOrCreate create) {
        RoleAdd roleAdd = new RoleAdd(member, create);
        if (create.getCachedOrNull() != null && member.getRoles().contains(create.getCachedOrNull())) {
            return;
        }
        addRoles.computeIfAbsent(member, k -> new HashSet<>()).add(roleAdd);
    }

    public void addRoleToMember(Member member, Role role) {
        RoleOrCreate create = createRole(role, role.getName(), -1, () -> role.getColor());
        addRoleToMember(member, create);
    }

    public void removeRoleFromMember(Member member, Role role) {
        removeRoles.computeIfAbsent(member, k -> new HashSet<>()).add(role);
    }

    public void modifyNickname(Member member, String name) {
        nickSet.put(member, name);
    }

    public static class RoleAdd {
        private final Member member;
        private final RoleOrCreate role;

        private boolean success;
        private String failedMessage;
        private CompletableFuture<Boolean> future;

        public RoleAdd(Member member, RoleOrCreate role) {
            this.member = member;
            this.role = role;
        }

        public CompletableFuture<Boolean> submit(Guild guild) {
            if (future != null) return future;
            this.future = this.role.submit(guild).thenApply(new Function<Role, Boolean>() {
                @Override
                public Boolean apply(Role role) {
                    if (role == null) {
                        success = false;
                        failedMessage = "Failed to create role `" + RoleAdd.this.role.name + "`: " + RoleAdd.this.role.failedCreateMessage;
                        return false;
                    }
                    if (member.getRoles().contains(role)) {
                        success = false;
                        failedMessage = "Member already has role `" + role.getName() + "`";
                        return false;
                    }
                    RateLimitUtil.queue(guild.addRoleToMember(member, role)).thenAccept(new Consumer<Void>() {
                        @Override
                        public void accept(Void aVoid) {
                            success = true;
                        }
                    }).exceptionally(new Function<Throwable, Void>() {
                        @Override
                        public Void apply(Throwable throwable) {
                            success = false;
                            failedMessage = "Failed to add role `" + role.getName() + "`: " + throwable.getMessage();
                            return null;
                        }
                    }).join();
                    return success;
                }
            });
            return future;
        }

        public boolean get() {
            if (future != null) {
                try {
                    future.join();
                } catch (RuntimeException e) {
                    failedMessage = e.getMessage();
                }
                future = null;
            }
            return success;
        }
    }

    public static class RoleOrCreate {
        private Role role;
        private final String name;
        private final Supplier<Color> hasColor;
        private final int position;
        private boolean fetched = false;

        private CompletableFuture<Role> future;

        private String failedCreateMessage = null;

        public RoleOrCreate(Role roleOrNull, String name, int position, Supplier<Color> hasColor) {
            this.role = roleOrNull;
            this.name = name;
            this.hasColor = hasColor;
            this.position = position;
        }

        public CompletableFuture<Role> submit(Guild guild) {
            if (future != null) return future;
            if (role == null && !fetched) {
                Color color = hasColor.get();
                RoleAction create = guild.createRole().setName(name);
                create = create.setMentionable(false).setHoisted(true);
                if (color != null) {
                    create = create.setColor(color);
                }
                try {
                    fetched = true;
                    future = RateLimitUtil.queue(create).thenApply(r -> {
                        this.role = r;
                        if (position >= 0) {
                            RateLimitUtil.queue(guild.modifyRolePositions().selectPosition(role).moveTo(position));
                        }
                        return r;
                    }).exceptionally(e -> {
                        this.failedCreateMessage = "Failed to create role `" + name + "`: " + e.getMessage();
                        return role;
                    });
                } catch (PermissionException e) {
                    failedCreateMessage = "Failed to create role `" + name + "`: " + e.getMessage();
                    future = CompletableFuture.failedFuture(e);
                }
            }
            if (future != null) {
                return future;
            } else {
                return CompletableFuture.completedFuture(role);
            }
        }

        public Role getCachedOrNull() {
            return role;
        }

        public Role get() {
            if (role == null && future != null) {
                try {
                    role = future.join();
                } catch (RuntimeException e) {
                    failedCreateMessage = "Failed to create role `" + name + "`: " + e.getMessage();
                }
                future = null;
            }
            return role;
        }
    }

    private static Color BG = Color.decode("#36393E");

    private Set<Color> existingColors = new HashSet<>();

    public Supplier<Color> supplyColor(int allianceId, Collection<Role> allianceRoles) {
        return new Supplier<Color>() {
            private Color color;
            @Override
            public Color get() {
                if (color != null) return color;

                if (existingColors == null) {
                    existingColors = new HashSet<>();
                    allianceRoles.forEach(r -> {
                        if (r.getColor() != null) existingColors.add(r.getColor());
                    });
                }

                Random random = new Random(allianceId);
                double maxDiff = 0;
                for (int i = 0; i < 100; i++) {
                    int nextInt = random.nextInt(0xffffff + 1);
                    String colorCode = String.format("#%06x", nextInt);
                    Color nextColor = Color.decode(colorCode);

                    if (CIEDE2000.calculateDeltaE(BG, nextColor) < 12) continue;

                    double minDiff = Double.MAX_VALUE;
                    for (Color otherColor : existingColors) {
                        if (otherColor != null) {
                            minDiff = Math.min(minDiff, CIEDE2000.calculateDeltaE(nextColor, otherColor));
                        }
                    }
                    if (minDiff > maxDiff) {
                        maxDiff = minDiff;
                        color = nextColor;
                    }
                    if (minDiff > 12) break;
                }
                if (color != null) {
                    existingColors.add(color);
                }
                return color;
            }
        };
    }
}
