package link.locutus.discord.util.task.roles;

import link.locutus.discord.db.entities.DBNation;
import net.dv8tion.jda.api.entities.Member;

import java.util.Map;
import java.util.function.Function;

public class DelegateAutoRoleTask implements IAutoRoleTask {
    @Override
    public AutoRoleInfo autoRoleConditions(Member member, DBNation nation) {
        return task.autoRoleConditions(member, nation);
    }

    @Override
    public AutoRoleInfo autoRoleMember(Member member, DBNation nation) {
        return task.autoRoleMember(member, nation);
    }

    @Override
    public AutoRoleInfo autoRoleAll() {
        return task.autoRoleAll();
    }

    @Override
    public AutoRoleInfo autoRole(Member member, DBNation nation) {
        return task.autoRole(member, nation);
    }

    @Override
    public Function<Integer, Boolean> getAllowedAlliances() {
        return task.getAllowedAlliances();
    }

    @Override
    public String syncDB() {
        return task.syncDB();
    }

    private final IAutoRoleTask task;

    public DelegateAutoRoleTask(IAutoRoleTask task) {
        this.task = task;
    }
}
