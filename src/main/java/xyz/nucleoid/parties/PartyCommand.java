package xyz.nucleoid.parties;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.ArrayList;
import java.util.Comparator;

import static net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class PartyCommand {
    // @formatter:off
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("party")
                .then(literal("list")
                    .requires(source -> source.permissions().hasPermission(COMMANDS_GAMEMASTER))
                    .executes(PartyCommand::listParties)
                )
                .then(literal("invite")
                    .then(argument("player", EntityArgument.player())
                    .executes(PartyCommand::invitePlayer)
                ))
                .then(literal("kick")
                    .then(argument("player", GameProfileArgument.gameProfile())
                    .executes(PartyCommand::kickPlayer)
                ))
                .then(literal("transfer")
                    .then(argument("player", EntityArgument.player())
                    .executes(PartyCommand::transferToPlayer)
                ))
                .then(literal("accept")
                    .then(argument("owner", EntityArgument.player())
                        .executes(PartyCommand::acceptInviteByOwner)
                    )
                    .then(argument("party", UuidArgument.uuid())
                        .executes(PartyCommand::acceptInviteByUuid)
                    )
                )
                .then(literal("leave").executes(PartyCommand::leave))
                .then(literal("disband").executes(PartyCommand::disband))
                .then(literal("add")
                    .requires(source -> source.permissions().hasPermission(COMMANDS_GAMEMASTER))
                    .then(argument("player", EntityArgument.player())
                        .then(argument("owner", EntityArgument.player())
                            .executes(PartyCommand::addPlayerByOwner)
                        )
                        .then(argument("party", UuidArgument.uuid())
                            .executes(PartyCommand::addPlayerByUuid)
                        )
                    )
                )
                .then(literal("remove")
                    .requires(source -> source.permissions().hasPermission(COMMANDS_GAMEMASTER))
                    .then(argument("player", EntityArgument.player())
                    .executes(PartyCommand::removePlayer)
                ))
        );
    }
    // @formatter:on

    private static int listParties(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var server = source.getServer();

        var partyManager = PartyManager.get(server);
        var parties = new ArrayList<>(partyManager.getAllParties());

        if (parties.isEmpty()) {
            source.sendFailure(PartyTexts.noParties());
            return 0;
        }

        parties.sort(Comparator.comparing(Party::getUuid));

        source.sendSuccess(() -> {
            var text = Component.empty();
            boolean first = true;

            for (var party : parties) {
                if (first) {
                    first = false;
                } else {
                    text.append(CommonComponents.NEW_LINE);
                }

                text.append(PartyTexts.listEntry(party.getUuid()));

                var members = new ArrayList<>(party.getMembers());
                members.sort(Comparator.comparing(PlayerRef::id));

                for (var member : members) {
                    text.append(CommonComponents.NEW_LINE);

                    if (party.isOwner(member)) {
                        text.append(PartyTexts.listMemberEntryType(member, server, PartyTexts.listMemberTypeOwner().withStyle(ChatFormatting.LIGHT_PURPLE)));
                    } else if (party.contains(member)) {
                        text.append(PartyTexts.listMemberEntry(member, server));
                    } else {
                        text.append(PartyTexts.listMemberEntryType(member, server, PartyTexts.listMemberTypePending().withStyle(ChatFormatting.GRAY)));
                    }
                }
            }

            return text;
        }, false);

        return parties.size();
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var owner = source.getPlayer();

        var player = EntityArgument.getPlayer(ctx, "player");

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.invitePlayer(PlayerRef.of(owner), PlayerRef.of(player));
        if (result.isOk()) {
            source.sendSuccess(() -> PartyTexts.invitedSender(player).withStyle(ChatFormatting.GOLD), false);

            var notification = PartyTexts.invitedReceiver(owner, result.party().getUuid())
                    .withStyle(ChatFormatting.GOLD);

            player.displayClientMessage(notification, false);
        } else {
            var error = result.error();
            source.sendFailure(PartyTexts.displayError(error, player));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int kickPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var server = source.getServer();
        var owner = source.getPlayer();

        var profiles = GameProfileArgument.getGameProfiles(ctx, "player");

        for (var profile : profiles) {
            var targetPlayer = server.getPlayerList().getPlayer(profile.id());

            var partyManager = PartyManager.get(source.getServer());
            var result = partyManager.kickPlayer(PlayerRef.of(owner), PlayerRef.of(targetPlayer));
            if (result.isOk()) {
                var party = result.party();

                var message = PartyTexts.kickedSender(owner);
                party.getMemberPlayers().sendMessage(message.withStyle(ChatFormatting.GOLD));

                PlayerRef.of(targetPlayer).ifOnline(server, player -> {
                    player.displayClientMessage(PartyTexts.kickedReceiver().withStyle(ChatFormatting.RED), false);
                });
            } else {
                var error = result.error();
                source.sendFailure(PartyTexts.displayError(error, profile.name()));
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int transferToPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var oldOwner = source.getPlayer();
        var newOwner = EntityArgument.getPlayer(ctx, "player");

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.transferParty(PlayerRef.of(oldOwner), PlayerRef.of(newOwner));
        if (result.isOk()) {
            source.sendSuccess(
                    () -> PartyTexts.transferredSender(newOwner).withStyle(ChatFormatting.GOLD),
                    false
            );

            newOwner.displayClientMessage(
                    PartyTexts.transferredReceiver(oldOwner).withStyle(ChatFormatting.GOLD),
                    false
            );
        } else {
            var error = result.error();
            source.sendFailure(PartyTexts.displayError(error, newOwner));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int acceptInviteByOwner(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var owner = EntityArgument.getPlayer(ctx, "owner");
        var partyManager = PartyManager.get(ctx.getSource().getServer());

        return acceptInvite(ctx, partyManager.getOwnParty(PlayerRef.of(owner)));
    }

    private static int acceptInviteByUuid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var uuid = UuidArgument.getUuid(ctx, "party");
        var partyManager = PartyManager.get(ctx.getSource().getServer());

        return acceptInvite(ctx, partyManager.getParty(uuid));
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> ctx, Party party) throws CommandSyntaxException {
        var source = ctx.getSource();
        var player = source.getPlayer();

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.acceptInvite(PlayerRef.of(player), party);
        if (result.isOk()) {
            var message = PartyTexts.joinSuccess(player);
            party.getMemberPlayers().sendMessage(message.withStyle(ChatFormatting.GOLD));
        } else {
            var error = result.error();
            source.sendFailure(PartyTexts.displayError(error, player));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var player = source.getPlayer();

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.leaveParty(PlayerRef.of(player));
        if (result.isOk()) {
            var party = result.party();

            var message = PartyTexts.leaveSuccess(player);
            party.getMemberPlayers().sendMessage(message.withStyle(ChatFormatting.GOLD));
        } else {
            var error = result.error();
            source.sendFailure(PartyTexts.displayError(error, player));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int disband(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var owner = source.getPlayer();

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.disbandParty(PlayerRef.of(owner));
        if (result.isOk()) {
            var party = result.party();

            var message = PartyTexts.disbandSuccess();
            party.getMemberPlayers().sendMessage(message.withStyle(ChatFormatting.GOLD));
        } else {
            var error = result.error();
            source.sendFailure(PartyTexts.displayError(error, owner));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int addPlayerByOwner(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var owner = EntityArgument.getPlayer(ctx, "owner");
        var partyManager = PartyManager.get(ctx.getSource().getServer());

        return addPlayer(ctx, partyManager.getOrCreateOwnParty(PlayerRef.of(owner)));
    }

    private static int addPlayerByUuid(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var uuid = UuidArgument.getUuid(ctx, "party");
        var partyManager = PartyManager.get(ctx.getSource().getServer());

        return addPlayer(ctx, partyManager.getParty(uuid));
    }

    private static int addPlayer(CommandContext<CommandSourceStack> ctx, Party party) throws CommandSyntaxException {
        var source = ctx.getSource();
        var player = EntityArgument.getPlayer(ctx, "player");

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.addPlayer(PlayerRef.of(player), party);
        if (result.isOk()) {
            var message = PartyTexts.addSuccess(player);
            party.getMemberPlayers().sendMessage(message.withStyle(ChatFormatting.GOLD));
        } else {
            var error = result.error();
            source.sendFailure(PartyTexts.displayError(error, player));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int removePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var source = ctx.getSource();
        var player = EntityArgument.getPlayer(ctx, "player");

        var partyManager = PartyManager.get(source.getServer());
        var result = partyManager.removePlayer(PlayerRef.of(player));
        if (result.isOk()) {
            var message = PartyTexts.removeSuccess(player);
            result.party().getMemberPlayers().sendMessage(message.withStyle(ChatFormatting.GOLD));
        } else {
            var error = result.error();
            source.sendFailure(PartyTexts.displayError(error, player));
        }

        return Command.SINGLE_SUCCESS;
    }
}
