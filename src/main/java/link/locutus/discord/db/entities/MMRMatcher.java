package link.locutus.discord.db.entities;

import java.util.function.Predicate;

public class MMRMatcher implements Predicate<String> {
    private final String required;

    public MMRMatcher(String required) {
        this.required = required.trim().toLowerCase().replace("x", ".");
        String regex = "[0-9xX.]/[0-9xX.]/[0-9xX.]";
        if (!required.matches(regex)) throw new IllegalArgumentException("MMR must be 3 numbers or X separated by `/`. Provided value: `" + required + "`");
    }

    @Override
    public String toString() {
        return required;
    }

    public String getRequired() {
        return required.replace('.', 'X');
    }

    @Override
    public boolean test(String mmr) {
        return mmr.matches(required);
    }
}
