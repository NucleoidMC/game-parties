package xyz.nucleoid.parties.test;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TrackingFakePlayerEntity extends FakePlayer {
    private final List<String> messages = new ArrayList<>();

    protected TrackingFakePlayerEntity(ServerWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public void sendMessageToClient(Text message, boolean overlay) {
        var string = message.getString();

        for (var line : string.split("\n")) {
            this.messages.add(line);
        }
    }

    public List<String> consumeMessages() {
        var result = new ArrayList<>(this.messages);
        this.messages.clear();

        return result;
    }
}
