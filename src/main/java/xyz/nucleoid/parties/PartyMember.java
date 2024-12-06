package xyz.nucleoid.parties;

import xyz.nucleoid.plasmid.api.util.PlayerRef;

public record PartyMember(Party party, PlayerRef player, Type type) {
    public boolean isPending() {
        return this.type == Type.PENDING;
    }

    public boolean isParticipant() {
        return !this.isPending();
    }

    public boolean isOwner() {
        return this.type == Type.OWNER;
    }

    enum Type {
        PENDING,
        MEMBER,
        OWNER;
    }
}
