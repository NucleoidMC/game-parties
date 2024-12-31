package xyz.nucleoid.parties.test;

import com.mojang.brigadier.Command;
import net.minecraft.SharedConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandAssertion {
    private final TestContext context;

    private final ServerPlayerEntity player;
    private final String command;

    private final List<String> expectedFeedback = new ArrayList<>();

    private CommandAssertion(TestContext context, ServerPlayerEntity player, String command) {
        this.context = Objects.requireNonNull(context);
        this.player = Objects.requireNonNull(player);
        this.command = Objects.requireNonNull(command);
    }

    public CommandAssertion expectFeedback(String feedback) {
        this.expectedFeedback.add(Objects.requireNonNull(feedback));
        return this;
    }

    public void executeSuccess() {
        this.execute(Command.SINGLE_SUCCESS);
    }

    public void execute(int expectedSuccessCount) {
        var output = new TrackingCommandOutput(this.context);
        var successCount = new MutableInt();

        var source = player.getCommandSource()
                .withOutput(output)
                .withLevel(4)
                .withReturnValueConsumer((successful, successCountx) -> {
                    successCount.setValue(successCountx);
                });

        withDevelopment(() -> {
            this.player.getServer().getCommandManager().executeWithPrefix(source, this.command);
        });

        output.check(this.command, this.expectedFeedback);
        this.context.assertEquals(successCount.intValue(), expectedSuccessCount, "'" + command + "' success count");
    }

    public static CommandAssertion builder(TestContext context, ServerPlayerEntity player, String command) {
        return new CommandAssertion(context, player, command);
    }

    private static void withDevelopment(Runnable runnable) {
        boolean stored = SharedConstants.isDevelopment;
        SharedConstants.isDevelopment = true;

        runnable.run();

        SharedConstants.isDevelopment = stored;
    }
}
