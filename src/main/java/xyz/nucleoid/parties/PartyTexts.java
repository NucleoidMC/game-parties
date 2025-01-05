package xyz.nucleoid.parties;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.game.GameTexts;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public final class PartyTexts {
    public static MutableText displayError(PartyError error, ServerPlayerEntity player) {
        return displayError(error, player.getDisplayName());
    }

    public static MutableText displayError(PartyError error, Text player) {
        return switch (error) {
            case DOES_NOT_EXIST -> Text.translatable("text.game_parties.party.error.does_not_exist");
            case ALREADY_INVITED -> Text.translatable("text.game_parties.party.error.already_invited", player);
            case ALREADY_IN_PARTY -> Text.translatable("text.game_parties.party.error.already_in_party");
            case CANNOT_INVITE_SELF -> Text.translatable("text.game_parties.party.error.cannot_invite_self");
            case CANNOT_REMOVE_SELF -> Text.translatable("text.game_parties.party.error.cannot_remove_self");
            case NOT_IN_PARTY -> Text.translatable("text.game_parties.party.error.not_in_party", player);
            case NOT_INVITED -> Text.translatable("text.game_parties.party.error.not_invited");
            case NOT_OWNER -> Text.translatable("text.game_parties.party.error.not_owner");
        };
    }

    public static MutableText joinSuccess(ServerPlayerEntity player) {
        return Text.translatable("text.game_parties.party.join.success", player.getDisplayName());
    }

    public static MutableText leaveSuccess(ServerPlayerEntity player) {
        return Text.translatable("text.game_parties.party.leave.success", player.getDisplayName());
    }

    public static MutableText disbandSuccess() {
        return Text.translatable("text.game_parties.party.disband.success");
    }

    public static MutableText addSuccess(ServerPlayerEntity player) {
        return Text.translatable("text.game_parties.party.add.success", player.getDisplayName());
    }

    public static MutableText removeSuccess(ServerPlayerEntity player) {
        return Text.translatable("text.game_parties.party.remove.success", player.getDisplayName());
    }

    public static MutableText transferredSender(ServerPlayerEntity transferredTo) {
        return Text.translatable("text.game_parties.party.transferred.sender", transferredTo.getDisplayName());
    }

    public static MutableText transferredReceiver(ServerPlayerEntity transferredFrom) {
        return Text.translatable("text.game_parties.party.transferred.receiver", transferredFrom.getDisplayName());
    }

    public static MutableText kickedSender(Text playerName) {
        return Text.translatable("text.game_parties.party.kicked.sender", playerName);
    }

    public static MutableText kickedReceiver() {
        return Text.translatable("text.game_parties.party.kicked.receiver");
    }

    public static MutableText invitedSender(ServerPlayerEntity player) {
        return Text.translatable("text.game_parties.party.invited.sender", player.getDisplayName());
    }

    public static MutableText invitedReceiver(ServerPlayerEntity owner, UUID uuid) {
        return Text.translatable("text.game_parties.party.invited.receiver", owner.getDisplayName())
                .append(PartyTexts.inviteNotificationLink(owner, uuid));
    }

    public static MutableText inviteNotificationLink(ServerPlayerEntity owner, UUID uuid) {
        return Text.translatable("text.game_parties.party.invited.receiver.click")
                .setStyle(GameTexts.commandLinkStyle(
                        "/party accept " + uuid,
                        Text.translatable("text.game_parties.party.invited.receiver.hover", owner.getDisplayName())
                ));
    }

    public static MutableText leftGame(ServerPlayerEntity player) {
        return Text.translatable("text.game_parties.party.left_game", player.getDisplayName());
    }

    public static MutableText noParties() {
        return Text.translatable("text.game_parties.party.list.none");
    }

    public static MutableText listEntry(UUID uuid) {
        return Text.translatable("text.game_parties.party.list.entry", Texts.bracketedCopyable(uuid.toString()));
    }

    public static MutableText listMemberEntry(PlayerRef member, MinecraftServer server) {
        return Text.translatable("text.game_parties.party.list.member.entry", name(member, server));
    }

    public static MutableText listMemberEntryType(PlayerRef member, MinecraftServer server, Text type) {
        return Text.translatable("text.game_parties.party.list.member.entry.type", name(member, server), type);
    }

    public static MutableText listMemberTypeOwner() {
        return Text.translatable("text.game_parties.party.list.member.type.owner");
    }

    public static MutableText listMemberTypePending() {
        return Text.translatable("text.game_parties.party.list.member.type.pending");
    }

    private static Text name(PlayerRef ref, MinecraftServer server) {
        var player = ref.getEntity(server);
        if (player == null) {
            Text id = Text.literal(ref.id().toString());
            return Texts.bracketed(id).formatted(Formatting.GRAY);
        }

        return player.getDisplayName();
    }
}
