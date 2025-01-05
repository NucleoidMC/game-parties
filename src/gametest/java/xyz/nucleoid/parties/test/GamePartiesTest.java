package xyz.nucleoid.parties.test;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
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
import java.util.Set;
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
        var player3 = createFakePlayer(context, 3);
        var player4 = createFakePlayer(context, 4);
        var player5 = createFakePlayer(context, 5);

        var players = Set.of(player1, player2, player3, player4, player5);

        CommandAssertion.builder(context, player1, players, "/party list")
            .expectFeedback("There are no parties!")
            .execute(0);

        CommandAssertion.builder(context, player1, players, "/party leave")
            .expectFeedback("You do not control any party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party kick Player2")
            .expectFeedback("You do not control any party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party transfer Player2")
            .expectFeedback("You do not control any party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party invite Player1")
            .expectFeedback("Cannot invite yourself to the party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party invite Player2")
            .expectFeedback("Invited Player2 to the party")
            .expectMessage("You have been invited to join Player1's party! Click here to join", player2)
            .executeSuccess();

        var partyManager = PartyManager.get(server);
        partyManager.getAllParties().forEach(party -> party.setUuid(partyUuid));

        CommandAssertion.builder(context, player1, players, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player1 (owner)")
            .expectFeedback("   - Player2 (pending)")
            .execute(1);

        CommandAssertion.builder(context, player2, players, "/party accept Player3")
            .expectFeedback("You are not invited to this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, players, "/party accept Player1")
            .expectMessage("Player2 has joined the party!", player1, player2)
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player1 (owner)")
            .expectFeedback("   - Player2")
            .execute(1);

        CommandAssertion.builder(context, player1, players, "/party invite Player4")
            .expectFeedback("Invited Player4 to the party")
            .expectMessage("You have been invited to join Player1's party! Click here to join", player4)
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party invite Player5")
            .expectFeedback("Invited Player5 to the party")
            .expectMessage("You have been invited to join Player1's party! Click here to join", player5)
            .executeSuccess();

        CommandAssertion.builder(context, player2, players, "/party invite Player3")
            .expectFeedback("You do not control this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party invite Player1")
            .expectFeedback("Cannot invite yourself to the party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, players, "/party transfer Player3")
            .expectFeedback("You do not control this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party transfer Player2")
            .expectFeedback("Your party has been transferred to Player2")
            .expectMessage("Player1's party has been transferred to you", player2)
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player2 (owner)")
            .expectFeedback("   - Player1")
            .expectFeedback("   - Player5 (pending)")
            .expectFeedback("   - Player4 (pending)")
            .execute(1);

        // Selectors are used for game profile arguments to bypass oddities with looking up game profiles from API services

        CommandAssertion.builder(context, player1, players, "/party kick @a[name=Player3,limit=1]")
            .expectFeedback("You do not control this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, players, "/party kick @a[name=Player1,limit=1]")
            .expectFeedback("Player1 has been kicked from the party")
            .expectMessage("You have been kicked from the party", player1)
            .executeSuccess();

        CommandAssertion.builder(context, player2, players, "/party kick @a[name=Player1,limit=1]")
            .expectFeedback("Player1 is not in this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, players, "/party kick @a[name=Player2,limit=1]")
            .expectFeedback("Cannot remove yourself from the party!")
            .executeSuccess();

        CommandAssertion.builder(context, player2, players, "/party kick @a[name=Player3,limit=1]")
            .expectFeedback("Player3 is not in this party!")
            .executeSuccess();

        CommandAssertion.builder(context, player4, players, "/party accept 10a05e00-5992-448c-9bed-a14cb2a7a909")
            .expectMessage("Player4 has joined the party!", player2, player4)
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player2 (owner)")
            .expectFeedback("   - Player4")
            .expectFeedback("   - Player5 (pending)")
            .execute(1);

        CommandAssertion.builder(context, player4, players, "/party leave")
            .expectMessage("Player4 has left the party!", player2, player4)
            .executeSuccess();

        CommandAssertion.builder(context, player5, players, "/party leave")
            .expectFeedback("You do not control any party!")
            .executeSuccess();

        CommandAssertion.builder(context, player1, players, "/party list")
            .expectFeedback(" - Party [10a05e00-5992-448c-9bed-a14cb2a7a909]")
            .expectFeedback("   - Player2 (owner)")
            .expectFeedback("   - Player5 (pending)")
            .execute(1);

        context.complete();
    }

    private static TrackingFakePlayerEntity createFakePlayer(TestContext context, int id) {
        var world = context.getWorld();

        var username = "Player" + id;
        var uuid = Uuids.getOfflinePlayerUuid(username);

        var profile = new GameProfile(uuid, username);
        var player = new TrackingFakePlayerEntity(world, profile);

        var playerManager = world.getServer().getPlayerManager();

        playerManager.getPlayerList().add(player);
        ((PlayerManagerAccessor) playerManager).getPlayerMap().put(uuid, player);

        return player;
    }
}
