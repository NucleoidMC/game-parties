package xyz.nucleoid.parties;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.ChatFormatting;
import xyz.nucleoid.plasmid.api.game.GameTexts;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public final class PartyTexts {
    public static MutableComponent displayError(PartyError error, ServerPlayer player) {
        return displayError(error, player.getGameProfile().name());
    }

    public static MutableComponent displayError(PartyError error, String player) {
        return switch (error) {
            case DOES_NOT_EXIST -> Component.translatable("text.game_parties.party.error.does_not_exist");
            case ALREADY_INVITED -> Component.translatable("text.game_parties.party.error.already_invited", player);
            case ALREADY_IN_PARTY -> Component.translatable("text.game_parties.party.error.already_in_party");
            case CANNOT_REMOVE_SELF -> Component.translatable("text.game_parties.party.error.cannot_remove_self");
            case NOT_IN_PARTY -> Component.translatable("text.game_parties.party.error.not_in_party", player);
            case NOT_INVITED -> Component.translatable("text.game_parties.party.error.not_invited");
        };
    }

    public static MutableComponent joinSuccess(ServerPlayer player) {
        return Component.translatable("text.game_parties.party.join.success", player.getDisplayName());
    }

    public static MutableComponent leaveSuccess(ServerPlayer player) {
        return Component.translatable("text.game_parties.party.leave.success", player.getDisplayName());
    }

    public static MutableComponent disbandSuccess() {
        return Component.translatable("text.game_parties.party.disband.success");
    }

    public static MutableComponent addSuccess(ServerPlayer player) {
        return Component.translatable("text.game_parties.party.add.success", player.getDisplayName());
    }

    public static MutableComponent removeSuccess(ServerPlayer player) {
        return Component.translatable("text.game_parties.party.remove.success", player.getDisplayName());
    }

    public static MutableComponent transferredSender(ServerPlayer transferredTo) {
        return Component.translatable("text.game_parties.party.transferred.sender", transferredTo.getDisplayName());
    }

    public static MutableComponent transferredReceiver(ServerPlayer transferredFrom) {
        return Component.translatable("text.game_parties.party.transferred.receiver", transferredFrom.getDisplayName());
    }

    public static MutableComponent kickedSender(ServerPlayer player) {
        return Component.translatable("text.game_parties.party.kicked.sender", player.getDisplayName());
    }

    public static MutableComponent kickedReceiver() {
        return Component.translatable("text.game_parties.party.kicked.receiver");
    }

    public static MutableComponent invitedSender(ServerPlayer player) {
        return Component.translatable("text.game_parties.party.invited.sender", player.getDisplayName());
    }

    public static MutableComponent invitedReceiver(ServerPlayer owner, UUID uuid) {
        return Component.translatable("text.game_parties.party.invited.receiver", owner.getDisplayName())
                .append(PartyTexts.inviteNotificationLink(owner, uuid));
    }

    public static MutableComponent inviteNotificationLink(ServerPlayer owner, UUID uuid) {
        return Component.translatable("text.game_parties.party.invited.receiver.click")
                .setStyle(GameTexts.commandLinkStyle(
                        "/party accept " + uuid,
                        Component.translatable("text.game_parties.party.invited.receiver.hover", owner.getDisplayName())
                ));
    }

    public static MutableComponent leftGame(ServerPlayer player) {
        return Component.translatable("text.game_parties.party.left_game", player.getDisplayName());
    }

    public static MutableComponent noParties() {
        return Component.translatable("text.game_parties.party.list.none");
    }

    public static MutableComponent listEntry(UUID uuid) {
        return Component.translatable("text.game_parties.party.list.entry", ComponentUtils.copyOnClickText(uuid.toString()));
    }

    public static MutableComponent listMemberEntry(PlayerRef member, MinecraftServer server) {
        return Component.translatable("text.game_parties.party.list.member.entry", name(member, server));
    }

    public static MutableComponent listMemberEntryType(PlayerRef member, MinecraftServer server, Component type) {
        return Component.translatable("text.game_parties.party.list.member.entry.type", name(member, server), type);
    }

    public static MutableComponent listMemberTypeOwner() {
        return Component.translatable("text.game_parties.party.list.member.type.owner");
    }

    public static MutableComponent listMemberTypePending() {
        return Component.translatable("text.game_parties.party.list.member.type.pending");
    }

    private static Component name(PlayerRef ref, MinecraftServer server) {
        var player = ref.getEntity(server);
        if (player == null) {
            Component id = Component.literal(ref.id().toString());
            return ComponentUtils.wrapInSquareBrackets(id).withStyle(ChatFormatting.GRAY);
        }

        return player.getDisplayName();
    }
}
