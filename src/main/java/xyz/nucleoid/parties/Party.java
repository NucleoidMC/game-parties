package xyz.nucleoid.parties;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import xyz.nucleoid.plasmid.api.game.player.MutablePlayerSet;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class Party {
    private final MutablePlayerSet memberPlayers;

    private final Object2ObjectMap<PlayerRef, Party> participantToParty;
    private final Object2ObjectMap<PlayerRef, PartyMember> members = new Object2ObjectOpenHashMap<>();

    private final UUID uuid;

    Party(MinecraftServer server, Object2ObjectMap<PlayerRef, Party> participantToParty, PlayerRef owner) {
        this.memberPlayers = new MutablePlayerSet(server);

        this.participantToParty = participantToParty;
        this.putMember(owner, PartyMember.Type.OWNER);

        this.uuid = UUID.randomUUID();
    }

    PartyMember getMember(PlayerRef player) {
        return this.members.get(player);
    }

    private boolean matches(PlayerRef player, Predicate<PartyMember> predicate) {
        var member = this.members.get(player);
        return member != null && predicate.test(member);
    }

    boolean isPending(PlayerRef player) {
        return this.matches(player, PartyMember::isPending);
    }

    boolean isParticipant(PlayerRef player) {
        return this.matches(player, PartyMember::isParticipant);
    }

    boolean isOwner(PlayerRef player) {
        return this.matches(player, PartyMember::isOwner);
    }

    void putMember(PlayerRef player, PartyMember.Type type) {
        var member = new PartyMember(this, player, type);

        if (member.isParticipant()) {
            var existingParty = this.participantToParty.put(player, this);

            if (existingParty != null && existingParty != this) {
                throw new IllegalStateException("player is already in a party");
            }
        }

        this.members.put(player, member);

        if (member.isParticipant()) {
            this.memberPlayers.add(player);
        }
    }

    PartyMember removeMember(PlayerRef player) {
        var member = this.members.remove(player);
        this.memberPlayers.remove(player);

        if (member != null && member.isParticipant() && !this.participantToParty.remove(player, this)) {
            throw new IllegalStateException("player is not in this party");
        }

        return member;
    }

    public List<PlayerRef> getMembers() {
        return new ObjectArrayList<>(this.members.keySet());
    }

    public MutablePlayerSet getMemberPlayers() {
        return this.memberPlayers;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public String toString() {
        return "Party{members=" + members + ", uuid=" + uuid + "}";
    }
}
