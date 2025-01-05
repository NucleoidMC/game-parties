package xyz.nucleoid.parties;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public record PartyMember(Party party, PlayerRef player, Type type) implements Comparable<PartyMember> {
    public boolean isPending() {
        return this.type == Type.PENDING;
    }

    public boolean isParticipant() {
        return !this.isPending();
    }

    public boolean isOwner() {
        return this.type == Type.OWNER;
    }

    public Text getListEntry(MinecraftServer server) {
        if (this.isOwner()) {
            return PartyTexts.listMemberEntryType(this.player, server, PartyTexts.listMemberTypeOwner().formatted(Formatting.LIGHT_PURPLE));
        } else if (this.isParticipant()) {
            return PartyTexts.listMemberEntry(this.player, server);
        } else {
            return PartyTexts.listMemberEntryType(this.player, server, PartyTexts.listMemberTypePending().formatted(Formatting.GRAY));
        }
    }

    @Override
    public int compareTo(PartyMember o) {
        int result = o.type.compareTo(this.type);
        return result != 0 ? result : this.player.id().compareTo(o.player.id());
    }

    @Override
    public String toString() {
        return this.player.id() + " (" + this.type + ")";
    }

    enum Type {
        PENDING,
        MEMBER,
        OWNER;
    }
}
