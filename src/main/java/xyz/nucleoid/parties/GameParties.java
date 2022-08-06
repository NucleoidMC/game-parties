package xyz.nucleoid.parties;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class GameParties implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PartyCommand.register(dispatcher);
        });

        PartyManager.register();
    }
}
