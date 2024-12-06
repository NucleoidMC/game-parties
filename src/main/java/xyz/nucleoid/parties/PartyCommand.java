package xyz.nucleoid.parties;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class PartyCommand {
    // @formatter:off
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("party")
                .then(literal("invite")
                    .then(argument("player", EntityArgumentType.player())
                    .executes(PartyCommand::invitePlayer)
                ))
                .then(literal("kick")
                    .then(argument("player", GameProfileArgumentType.gameProfile())
                    .executes(PartyCommand::kickPlayer)
                ))
                .then(literal("transfer")
                    .then(argument("player", EntityArgumentType.player())
                    .executes(PartyCommand::transferToPlayer)
                ))
                .then(literal("accept")
                    .then(argument("owner", EntityArgumentType.player())
                        .executes(PartyCommand::acceptInviteByOwner)
                    )
                    .then(argument("party", UuidArgumentType.uuid())
                        .executes(PartyCommand::acceptInviteByUuid)
                    )
                )
                .then(literal("leave").executes(PartyCommand::leave))
                .then(literal("disband").executes(PartyCommand::disband))
        );
    }
    // @formatter:on

    private static int invitePlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var owner = source.getPlayer();

        var player = EntityArgumentType.getPlayer(ctx, "player");

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.invitePlayer(PlayerRef.of(owner), PlayerRef.of(player));
        result.ifSuccessElse(party -> {
            source.sendFeedback(() -> PartyTexts.invitedSender(player).formatted(Formatting.GOLD), false);

            var notification = PartyTexts.invitedReceiver(owner, party.getUuid())
                    .formatted(Formatting.GOLD);

            player.sendMessage(notification, false);
        }, error -> {
            source.sendError(PartyTexts.displayError(error, player));
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int kickPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var server = source.getServer();
        var owner = source.getPlayer();

        var profiles = GameProfileArgumentType.getProfileArgument(ctx, "player");

        for (var profile : profiles) {
            var partyManager = PartyManager.get(source.getServer());
            var ref = PlayerRef.of(profile);
            var result = partyManager.kickPlayer(PlayerRef.of(owner), ref);
            result.ifSuccessElse(party -> {
                MutableText message;
                var player = ref.getEntity(server);

                if (player == null) {
                    message = PartyTexts.kickedSender(Text.literal(profile.getName()));
                } else {
                    message = PartyTexts.kickedSender(player.getDisplayName());
                    player.sendMessage(PartyTexts.kickedReceiver().formatted(Formatting.RED), false);
                }

                party.getMemberPlayers().sendMessage(message.formatted(Formatting.GOLD));
            }, error -> {
                source.sendError(PartyTexts.displayError(error, Text.literal(profile.getName())));
            });
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int transferToPlayer(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var oldOwner = source.getPlayer();
        var newOwner = EntityArgumentType.getPlayer(ctx, "player");

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.transferParty(PlayerRef.of(oldOwner), PlayerRef.of(newOwner));
        result.ifSuccessElse(party -> {
            source.sendFeedback(
                    () -> PartyTexts.transferredSender(newOwner).formatted(Formatting.GOLD),
                    false
            );

            newOwner.sendMessage(
                    PartyTexts.transferredReceiver(oldOwner).formatted(Formatting.GOLD),
                    false
            );
        }, error -> {
            source.sendError(PartyTexts.displayError(error, newOwner));
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int acceptInviteByOwner(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var owner = EntityArgumentType.getPlayer(ctx, "owner");
        var partyManager = PartyManager.get(ctx.getSource().getServer());

        return acceptInvite(ctx, partyManager.getOwnParty(PlayerRef.of(owner), PartyError.NOT_INVITED));
    }

    private static int acceptInviteByUuid(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var uuid = UuidArgumentType.getUuid(ctx, "party");
        var partyManager = PartyManager.get(ctx.getSource().getServer());

        return acceptInvite(ctx, partyManager.getParty(uuid, PartyError.NOT_INVITED));
    }

    private static int acceptInvite(CommandContext<ServerCommandSource> ctx, PartyResult result) throws CommandSyntaxException {
        var source = ctx.getSource();
        var player = source.getPlayer();

        var partyManager = PartyManager.get(source.getServer());
        result
                .map(party -> partyManager.acceptInvite(PlayerRef.of(player), party))
                .ifSuccessElse(party -> {
                    var message = PartyTexts.joinSuccess(player);
                    party.getMemberPlayers().sendMessage(message.formatted(Formatting.GOLD));
                }, error -> {
                    source.sendError(PartyTexts.displayError(error, player));
                });

        return Command.SINGLE_SUCCESS;
    }

    private static int leave(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var player = source.getPlayer();

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.leaveParty(PlayerRef.of(player));
        result.ifSuccessElse(party -> {
            var message = PartyTexts.leaveSuccess(player).formatted(Formatting.GOLD);
            party.getMemberPlayers().sendMessage(message);
            player.sendMessage(message, false);
        }, error -> {
            source.sendError(PartyTexts.displayError(error, player));
        });

        return Command.SINGLE_SUCCESS;
    }

    private static int disband(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var owner = source.getPlayer();

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.disbandParty(PlayerRef.of(owner));
        result.ifSuccessElse(party -> {
            var message = PartyTexts.disbandSuccess();
            party.getMemberPlayers().sendMessage(message.formatted(Formatting.GOLD));
        }, error -> {
            source.sendError(PartyTexts.displayError(error, owner));
        });

        return Command.SINGLE_SUCCESS;
    }
}
