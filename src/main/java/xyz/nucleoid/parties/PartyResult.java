package xyz.nucleoid.parties;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class PartyResult {
    private final Party party;
    private final PartyError error;

    private PartyResult(Party party, PartyError error) {
        this.party = party;
        this.error = error;
    }

    public static PartyResult ok(Party party) {
        return new PartyResult(party, null);
    }

    public static PartyResult err(PartyError error) {
        return new PartyResult(null, error);
    }

    public boolean isOk() {
        return this.error == null;
    }

    public boolean isErr() {
        return this.error != null;
    }

    public PartyResult map(Function<Party, PartyResult> mapper) {
        if (this.party != null) {
            return mapper.apply(this.party);
        } else {
            return this;
        }
    }

    @Nullable
    public Party party() {
        return this.party;
    }

    @Nullable
    public PartyError error() {
        return this.error;
    }
}
