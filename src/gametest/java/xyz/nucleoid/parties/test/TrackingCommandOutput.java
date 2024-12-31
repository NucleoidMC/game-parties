package xyz.nucleoid.parties.test;

import com.google.common.collect.Lists;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TrackingCommandOutput implements CommandOutput {
    private final TestContext context;
    private final List<String> actualFeedback = new ArrayList<>();

    public TrackingCommandOutput(TestContext context) {
        this.context = context;
    }

    @Override
    public void sendMessage(Text message) {
        var string = message.getString();

        for (var line : string.split("\n")) {
            this.actualFeedback.add(line);
        }
    }

    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return false;
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return true;
    }

    @Override
    public boolean shouldTrackOutput() {
        return true;
    }

    public void check(String command, List<String> expectedFeedback) {
        this.context.assertEquals(this.actualFeedback, Lists.newArrayList(expectedFeedback), "'" + command + "' feedback");
    }
}
