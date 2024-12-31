package xyz.nucleoid.parties.test;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ApiServices;
import net.minecraft.util.Uuids;
import xyz.nucleoid.parties.PartyManager;
import xyz.nucleoid.parties.test.mixin.MinecraftServerAccessor;
import xyz.nucleoid.parties.test.mixin.PlayerManagerAccessor;

import java.net.Proxy;
import java.util.UUID;

public class GamePartiesTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)
    public void test(TestContext context) {
        var server = context.getWorld().getServer();

        var gameDir = FabricLoader.getInstance().getGameDir().toFile();

        var apiServices = ApiServices.create(new YggdrasilAuthenticationService(Proxy.NO_PROXY), gameDir);
        ((MinecraftServerAccessor) (Object) server).setApiServices(apiServices);

        var partyUuid = UUID.fromString("10a05e00-5992-448c-9bed-a14cb2a7a909");

        var player1 = createFakePlayer(context, 1);
        var player2 = createFakePlayer(context, 2);
        createFakePlayer(context, 3);
        var player4 = createFakePlayer(context, 4);
        var player5 = createFakePlayer(context, 5);

        CommandAssertion.builder(context, player1, "/party list")
            .expectFeedback("There are no parties!")
            .execute(0);

        CommandAssertion.builder(context, player1, "/party leave")
            .expectFeedback("You do not control any party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party kick Player2")
            .expectFeedback("You do not control any party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party transfer Player2")
            .expectFeedback("You do not control any party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party invite Player1")
            .expectFeedback("Cannot invite yourself to the party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party invite Player2")
            .expectFeedback("Invited Player2 to the party")
            .executeSuccess();

        var partyManager = PartyManager.get(server);
        partyManager.getAllParties().forEach(party -> party.setUuid(partyUuid));

        CommandAssertion.builder(context, player1, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player2 (pending)")
            .expectFeedback("   - Player1 (owner)")
            .execute(1);

        CommandAssertion.builder(context, player2, "/party accept Player3")
            .expectFeedback("You are not invited to this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, "/party accept Player1")
            // Sends an untested message to all players in the party
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player2")
            .expectFeedback("   - Player1 (owner)")
            .execute(1);

        CommandAssertion.builder(context, player1, "/party invite Player4")
            .expectFeedback("Invited Player4 to the party")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party invite Player5")
            .expectFeedback("Invited Player5 to the party")
            .executeSuccess();

        CommandAssertion.builder(context, player2, "/party invite Player3")
            .expectFeedback("You do not control this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party invite Player1")
            .expectFeedback("Cannot invite yourself to the party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, "/party transfer Player3")
            .expectFeedback("You do not control this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party transfer Player2")
            .expectFeedback("Your party has been transferred to Player2")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player5 (pending)")
            .expectFeedback("   - Player2 (owner)")
            .expectFeedback("   - Player4 (pending)")
            .expectFeedback("   - Player1")
            .execute(1);

        // Selectors are used for game profile arguments to bypass oddities with looking up game profiles from API services

        CommandAssertion.builder(context, player1, "/party kick @a[name=Player3,limit=1]")
            .expectFeedback("You do not control this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, "/party kick @a[name=Player1,limit=1]")
            // Sends an untested message to all players in the party
            .executeSuccess();

        CommandAssertion.builder(context, player2, "/party kick @a[name=Player1,limit=1]")
            .expectFeedback("Player1 is not in this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, "/party kick @a[name=Player2,limit=1]")
            .expectFeedback("Cannot remove yourself from the party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, "/party kick @a[name=Player3,limit=1]")
            .expectFeedback("Player3 is not in this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player4, "/party accept 10a05e00-5992-448c-9bed-a14cb2a7a909")
            // Sends an untested message to all players in the party
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player5 (pending)")
            .expectFeedback("   - Player2 (owner)")
            .expectFeedback("   - Player4")
            .execute(1);

        CommandAssertion.builder(context, player4, "/party leave")
            // Sends an untested message to all players in the party
            .executeSuccess();

        CommandAssertion.builder(context, player5, "/party leave")
            .expectFeedback("You do not control any party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player5 (pending)")
            .expectFeedback("   - Player2 (owner)")
            .execute(1);

        context.complete();
    }

    private static FakePlayer createFakePlayer(TestContext context, int id) {
        var world = context.getWorld();

        var username = "Player" + id;
        var uuid = Uuids.getOfflinePlayerUuid(username);

        var profile = new GameProfile(uuid, username);
        System.out.println("init profile " + profile.getName() + ": " + profile.getId());
        var player = FakePlayer.get(world, profile);

        var playerManager = world.getServer().getPlayerManager();

        playerManager.getPlayerList().add(player);
        ((PlayerManagerAccessor) playerManager).getPlayerMap().put(uuid, player);

        return player;
    }
}
