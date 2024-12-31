package xyz.nucleoid.parties.test.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ApiServices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor
    @Mutable
    void setApiServices(ApiServices apiServices);
}
