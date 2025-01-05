package xyz.nucleoid.parties.test;

import com.mojang.brigadier.Command;
import net.minecraft.SharedConstants;
import net.minecraft.test.TestContext;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CommandAssertion {
    private final TestContext context;

    private final TrackingFakePlayerEntity player;
    private final String command;

    private final Map<TrackingFakePlayerEntity, List<String>> recipientsToExpectedMessages = new HashMap<>();

    private CommandAssertion(TestContext context, TrackingFakePlayerEntity player, Set<TrackingFakePlayerEntity> players, String command) {
        this.context = Objects.requireNonNull(context);
        this.player = Objects.requireNonNull(player);
        this.command = Objects.requireNonNull(command);

        if (!players.contains(player)) {
            throw new IllegalArgumentException("player must be in players set");
        }

        for (var recipient : players) {
            this.recipientsToExpectedMessages.put(recipient, new ArrayList<>());
        }
    }

    public CommandAssertion expectFeedback(String feedback) {
        return this.expectMessage(feedback, this.player);
    }

    public CommandAssertion expectMessage(String message, TrackingFakePlayerEntity... recipients) {
        Objects.requireNonNull(message);

        for (var recipient : recipients) {
            var messages = this.recipientsToExpectedMessages.get(recipient);

            if (messages == null) {
                throw new IllegalArgumentException("recipient must be in players set");
            }

            messages.add(message);
        }

        return this;
    }

    public void executeSuccess() {
        this.execute(Command.SINGLE_SUCCESS);
    }

    public void execute(int expectedSuccessCount) {
        var successCount = new MutableInt();

        var source = player.getCommandSource()
                .withLevel(4)
                .withReturnValueConsumer((successful, successCountx) -> {
                    successCount.setValue(successCountx);
                });

        withDevelopment(() -> {
            this.player.getServer().getCommandManager().executeWithPrefix(source, this.command);
        });

        for (var entry : this.recipientsToExpectedMessages.entrySet()) {
            var recipient = entry.getKey();

            var expectedMessages = entry.getValue();
            var actualMessages = recipient.consumeMessages();

            var name = recipient == this.player ? "feedback" : "messages to " + recipient.getNameForScoreboard();
            this.context.assertEquals(actualMessages, expectedMessages, "'" + command + "' " + name);
        }

        this.context.assertEquals(successCount.intValue(), expectedSuccessCount, "'" + command + "' success count");
    }

    public static CommandAssertion builder(TestContext context, TrackingFakePlayerEntity player, Set<TrackingFakePlayerEntity> players, String command) {
        return new CommandAssertion(context, player, players, command);
    }

    private static void withDevelopment(Runnable runnable) {
        boolean stored = SharedConstants.isDevelopment;
        SharedConstants.isDevelopment = true;

        runnable.run();

        SharedConstants.isDevelopment = stored;
    }
}
