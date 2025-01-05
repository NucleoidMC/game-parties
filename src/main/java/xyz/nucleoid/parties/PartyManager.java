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
import java.util.HashSet;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

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

            var members = partyManager.getPartyMembers(player, true);
            
            additional.addAll(members);
        });

        GameEvents.TEAM_SELECTION_LOBBY_FINALIZE.register((gameSpace, allocator, players) -> {
            var partyManager = PartyManager.get(gameSpace.getServer());

            var ungroupedPlayers = players.stream().collect(Collectors.toCollection(HashSet::new));

            for (ServerPlayerEntity player : players) {
                if (ungroupedPlayers.contains(player)) {
                    var members = partyManager.getPartyMembers(player, false);

                    allocator.group(members);
                    ungroupedPlayers.removeAll(members);
                }
            }
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
            party.putMember(nextMember.player(), PartyMember.Type.OWNER);

            nextMember.player().ifOnline(this.server, nextPlayer -> {
                nextPlayer.sendMessage(PartyTexts.transferredReceiver(player), false);
            });
        }
    }

    public PartyResult invitePlayer(PlayerRef owner, PlayerRef player) {
        if (owner.equals(player)) {
            return new PartyResult.Error(PartyError.CANNOT_INVITE_SELF);
        }

        var result = this.getOrCreateOwnParty(owner);

        return result.map(party -> {
            var member = party.getMember(player);
            if (member == null) {
                party.putMember(player, PartyMember.Type.PENDING);
                return new PartyResult.Success(party);
            } else {
                return new PartyResult.Error(PartyError.ALREADY_INVITED);
            }
        });
    }

    public PartyResult kickPlayer(PlayerRef owner, PlayerRef player) {
        if (owner.equals(player)) {
            return new PartyResult.Error(PartyError.CANNOT_REMOVE_SELF);
        }

        var result = this.getOwnParty(owner, null);

        return result.map(party -> {
            if (party.removeMember(player) != null) {
                return new PartyResult.Success(party);
            }

            return new PartyResult.Error(PartyError.NOT_IN_PARTY);
        });
    }

    public PartyResult acceptInvite(PlayerRef player, Party party) {
        if (this.participantToParty.containsKey(player)) {
            return new PartyResult.Error(PartyError.ALREADY_IN_PARTY);
        }

        if (party.isPending(player)) {
            party.putMember(player, PartyMember.Type.MEMBER);
            return new PartyResult.Success(party);
        }

        return new PartyResult.Error(PartyError.NOT_INVITED);
    }

    public PartyResult leaveParty(PlayerRef player) {
        var party = this.getParty(player);
        if (party == null) {
            return new PartyResult.Error(PartyError.DOES_NOT_EXIST);
        }

        if (party.isOwner(player)) {
            if (party.getMembers().size() > 1) {
                return new PartyResult.Error(PartyError.CANNOT_REMOVE_SELF);
            }
            return this.disbandParty(player);
        }

        if (party.removeMember(player) != null) {
            return new PartyResult.Success(party);
        } else {
            return new PartyResult.Error(PartyError.NOT_IN_PARTY);
        }
    }

    public PartyResult transferParty(PlayerRef from, PlayerRef to) {
        var result = this.getOwnParty(from, null);

        return result.map(party -> {
            if (!party.isParticipant(to)) {
                return new PartyResult.Error(PartyError.NOT_IN_PARTY);
            }

            party.putMember(from, PartyMember.Type.MEMBER);
            party.putMember(to, PartyMember.Type.OWNER);

            return new PartyResult.Success(party);
        });
    }

    public PartyResult disbandParty(PlayerRef owner) {
        var result = this.getOwnParty(owner, null);

        return result.map(party -> {
            this.disbandParty(party);
            return new PartyResult.Success(party);
        });
    }

    public void disbandParty(Party party) {
        for (var member : party.getMembers()) {
            this.participantToParty.remove(member, party);
        }
    }

    public PartyResult addPlayer(PlayerRef player, @Nullable Party party) {
        if (party == null) {
            return new PartyResult.Error(PartyError.DOES_NOT_EXIST);
        }

        var oldParty = this.getParty(player);
        if (party == oldParty) {
            return new PartyResult.Error(PartyError.ALREADY_IN_PARTY);
        } else if (oldParty != null) {
            if (oldParty.isOwner(player)) {
                this.disbandParty(player);
            } else {
                oldParty.removeMember(player);
            }
        }

        party.putMember(player, PartyMember.Type.MEMBER);

        return new PartyResult.Success(party);
    }

    public PartyResult removePlayer(PlayerRef player) {
        var party = this.getParty(player);
        if (party == null) {
            return new PartyResult.Error(PartyError.NOT_IN_PARTY);
        }

        if (party.isOwner(player)) {
            this.disbandParty(player);
        } else {
            party.removeMember(player);
        }

        return new PartyResult.Success(party);
    }

    @Nullable
    public Party getParty(PlayerRef player) {
        return this.participantToParty.get(player);
    }

    public PartyResult getParty(UUID uuid, PartyError error) {
        for (Party party : this.participantToParty.values()) {
            if (party.getUuid().equals(uuid)) {
                return new PartyResult.Success(party);
            }
        }

        return new PartyResult.Error(error);
    }

    public PartyResult getOwnParty(PlayerRef owner, @Nullable PartyError error) {
        var party = this.participantToParty.get(owner);

        if (party == null) {
            return new PartyResult.Error(error == null ? PartyError.DOES_NOT_EXIST : error);
        } else if (!party.isOwner(owner)) {
            return new PartyResult.Error(error == null ? PartyError.NOT_OWNER : error);
        }

        return new PartyResult.Success(party);
    }

    public PartyResult getOrCreateOwnParty(PlayerRef owner) {
        var party = this.participantToParty.computeIfAbsent(owner, this::createParty);
        if (party.isOwner(owner)) {
            return new PartyResult.Success(party);
        }
        return new PartyResult.Error(PartyError.NOT_OWNER);
    }

    private Party createParty(PlayerRef owner) {
        return new Party(this.server, this.participantToParty, owner);
    }

    public Collection<ServerPlayerEntity> getPartyMembers(ServerPlayerEntity player, boolean own) {
        var ref = PlayerRef.of(player);
        var result = own ? this.getOwnParty(PlayerRef.of(player), null) : this.getParty(ref);

        if (result instanceof PartyResult.Success success) {
            return Lists.newArrayList(success.party().getMemberPlayers());
        } else {
            return Collections.singleton(player);
        }
    }

    public Collection<Party> getAllParties() {
        return new HashSet<>(this.participantToParty.values());
    }
}
