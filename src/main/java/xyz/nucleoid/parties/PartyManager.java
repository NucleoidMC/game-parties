package xyz.nucleoid.parties;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.event.GameEvents;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public final class PartyManager {
    private static PartyManager instance;

    private final MinecraftServer server;
    private final Object2ObjectMap<PlayerRef, Party> participantToParty = new Object2ObjectOpenHashMap<>();

    private PartyManager(MinecraftServer server) {
        this.server = server;
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var partyManager = PartyManager.get(server);
            partyManager.onPlayerLogOut(handler.player);
        });

        GameEvents.COLLECT_PLAYERS_FOR_JOIN.register((gameSpace, player, additional) -> {
            var partyManager = PartyManager.get(player.server);

            var members = partyManager.getPartyMembers(player);
            additional.addAll(members);
        });
    }

    public static PartyManager get(MinecraftServer server) {
        if (instance == null || instance.server != server) {
            instance = new PartyManager(server);
        }
        return instance;
    }

    public void onPlayerLogOut(ServerPlayerEntity player) {
        var ref = PlayerRef.of(player);

        var party = this.getParty(ref);
        if (party == null) {
            return;
        }

        var member = party.removeMember(ref);
        if (member != null) {
            if (member.isOwner()) {
                this.onPartyOwnerLogOut(player, party);
            }

            party.getMemberPlayers().sendMessage(PartyTexts.leftGame(player));
        }
    }

    private void onPartyOwnerLogOut(ServerPlayerEntity player, Party party) {
        var members = party.getMembers();

        if (!members.isEmpty()) {
            var nextMember = members.get(0);
            party.putMember(nextMember, PartyMember.Type.OWNER);

            nextMember.ifOnline(this.server, nextPlayer -> {
                nextPlayer.sendMessage(PartyTexts.transferredReceiver(player), false);
            });
        }
    }

    public PartyResult invitePlayer(PlayerRef owner, PlayerRef player) {
        if (owner.equals(player)) {
            return PartyResult.err(PartyError.CANNOT_INVITE_SELF);
        }

        var result = this.getOrCreateOwnParty(owner);

        return result.map(party -> {
            var member = party.getMember(player);
            if (member == null) {
                party.putMember(player, PartyMember.Type.PENDING);
                return PartyResult.ok(party);
            } else {
                return PartyResult.err(PartyError.ALREADY_INVITED);
            }
        });
    }

    public PartyResult kickPlayer(PlayerRef owner, PlayerRef player) {
        if (owner.equals(player)) {
            return PartyResult.err(PartyError.CANNOT_REMOVE_SELF);
        }

        var result = this.getOwnParty(owner);

        return result.map(party -> {
            if (party.removeMember(player) != null) {
                return PartyResult.ok(party);
            }

            return PartyResult.err(PartyError.NOT_IN_PARTY);
        });
    }

    public PartyResult acceptInvite(PlayerRef player, @Nullable Party party) {
        if (this.participantToParty.containsKey(player)) {
            return PartyResult.err(PartyError.ALREADY_IN_PARTY);
        }

        if (party == null) {
            return PartyResult.err(PartyError.DOES_NOT_EXIST);
        }

        if (party.isPending(player)) {
            party.putMember(player, PartyMember.Type.MEMBER);
            return PartyResult.ok(party);
        }

        return PartyResult.err(PartyError.NOT_INVITED);
    }

    public PartyResult leaveParty(PlayerRef player) {
        var party = this.getParty(player);
        if (party == null) {
            return PartyResult.err(PartyError.DOES_NOT_EXIST);
        }

        if (party.isOwner(player)) {
            if (party.getMembers().size() > 1) {
                return PartyResult.err(PartyError.CANNOT_REMOVE_SELF);
            }
            return this.disbandParty(player);
        }

        if (party.removeMember(player) != null) {
            return PartyResult.ok(party);
        } else {
            return PartyResult.err(PartyError.NOT_IN_PARTY);
        }
    }

    public PartyResult transferParty(PlayerRef from, PlayerRef to) {
        var result = this.getOwnParty(from);

        return result.map(party -> {
            if (!party.isParticipant(to)) {
                return PartyResult.err(PartyError.NOT_IN_PARTY);
            }

            party.putMember(from, PartyMember.Type.MEMBER);
            party.putMember(to, PartyMember.Type.OWNER);

            return PartyResult.ok(party);
        });
    }

    public PartyResult disbandParty(PlayerRef owner) {
        var result = this.getOwnParty(owner);

        return result.map(party -> {
            this.disbandParty(party);
            return PartyResult.ok(party);
        });
    }

    public void disbandParty(Party party) {
        for (PlayerRef member : party.getMembers()) {
            this.participantToParty.remove(member, party);
        }
    }

    @Nullable
    public Party getParty(PlayerRef player) {
        return this.participantToParty.get(player);
    }

    @Nullable
    public Party getParty(UUID uuid) {
        for (Party party : this.participantToParty.values()) {
            if (party.getUuid().equals(uuid)) {
                return party;
            }
        }

        return null;
    }

    public PartyResult getOwnParty(PlayerRef owner) {
        var party = this.participantToParty.get(owner);

        if (party == null) {
            return PartyResult.err(PartyError.DOES_NOT_EXIST);
        } else if (!party.isOwner(owner)) {
            PartyResult.err(PartyError.NOT_OWNER);
        }

        return PartyResult.ok(party);
    }

    private PartyResult getOrCreateOwnParty(PlayerRef owner) {
        var party = this.participantToParty.computeIfAbsent(owner, this::createParty);
        if (party.isOwner(owner)) {
            return PartyResult.ok(party);
        }
        return PartyResult.err(PartyError.NOT_OWNER);
    }

    private Party createParty(PlayerRef owner) {
        return new Party(this.server, this.participantToParty, owner);
    }

    public Collection<ServerPlayerEntity> getPartyMembers(ServerPlayerEntity player) {
        var result = this.getOwnParty(PlayerRef.of(player));
        if (result.isOk()) {
            return Lists.newArrayList(result.party().getMemberPlayers());
        } else {
            return Collections.singleton(player);
        }
    }
}
