package net.caravidro.wayaround.industrial.ship;

import net.caravidro.wayaround.WayAround;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WayAround.MODID)
@PrefixGameTestTemplate(false)
public final class ShipGameTests {
    @GameTest(template = "ship_test", timeoutTicks = 120)
    public static void workbenchAndBerth(GameTestHelper helper) {
        GreatShipEntity ship = helper.spawn(CoalShipContent.GREAT_SHIP_ENTITY.get(), 8, 3, 8);
        ship.setNoGravity(true);
        var player = helper.makeMockServerPlayerInLevel();
        // The mock client has no mod handshake; suppress outgoing packets in this server-side test.
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                helper.getLevel().getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {}
        };
        player.setPos(ship.position());
        ship.toggleAnchor(player);
        ship.openWorkbench(player);
        helper.assertTrue(player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu,
                "Onboard workbench must open the normal crafting menu");
        player.containerMenu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
        helper.assertTrue(player.containerMenu.getSlot(0).getItem().is(Items.OAK_PLANKS),
                "Onboard workbench must produce recipe results");
        helper.assertTrue(player.containerMenu.stillValid(player), "Workbench must remain valid aboard");
        helper.getLevel().setDayTime(14000);
        helper.getLevel().updateSkyBrightness();
        ship.sleep(player);
        helper.assertTrue(player.isSleeping(), "Onboard bed must start real sleep");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(player.isSleeping(), "Sleep must persist without a world bed block");
            ship.toggleAnchor(player);
            helper.assertTrue(ship.isAnchored(), "Anchor must stay lowered while bed is occupied");
            player.stopSleepInBed(true, true);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(player.getVehicle() == ship, "Waking player must return aboard");
                helper.assertTrue(!ship.hasSleeper(), "Bed must be free after waking");
                helper.getLevel().getServer().getPlayerList().remove(player);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "ship_test", timeoutTicks = 120)
    public static void seatsCargoAndSave(GameTestHelper helper) {
        GreatShipEntity ship = helper.spawn(CoalShipContent.GREAT_SHIP_ENTITY.get(), 8, 3, 8);
        Player[] passengers = new Player[6];
        for (int i = 0; i < 6; i++) {
            passengers[i] = helper.makeMockPlayer(GameType.SURVIVAL);
            passengers[i].setPos(ship.position());
            ship.board(passengers[i], i);
        }
        helper.assertTrue(ship.getPassengers().size() == 6, "Six passengers must board");
        helper.assertTrue(ship.getControllingPassenger() == passengers[0], "Only helm seat controls");
        passengers[0].stopRiding();
        helper.assertTrue(ship.getControllingPassenger() == null, "Other passengers must not become pilot");
        ship.board(passengers[0], 0);
        ship.setItem(53, new ItemStack(Items.DIAMOND, 17));
        ship.toggleAnchor(passengers[0]);
        CompoundTag saved = new CompoundTag();
        ship.saveWithoutId(saved);
        GreatShipEntity restored = new GreatShipEntity(CoalShipContent.GREAT_SHIP_ENTITY.get(), helper.getLevel());
        restored.load(saved);
        helper.assertTrue(restored.getContainerSize() == 54 && restored.getItem(53).getCount() == 17,
                "Last cargo slot must survive saving");
        helper.assertTrue(restored.isAnchored(), "Anchor must survive saving");
        ship.hurt(helper.getLevel().damageSources().generic(), 10);
        helper.assertTrue(ship.isAlive() && Math.abs(ship.getDamage() - 8) < .001, "Hull must reduce damage by 92 percent");
        ship.setDeltaMovement(new Vec3(1, .2, 1));
        helper.assertTrue(ship.getDeltaMovement().horizontalDistance() <= .100001
                && ship.getDeltaMovement().y == .2, "Speed limit must preserve vertical buoyancy");
        helper.succeed();
    }

    @GameTest(template = "ship_test", timeoutTicks = 160)
    public static void waterDriftAndAnchor(GameTestHelper helper) {
        for (int x = 1; x <= 36; x++) for (int z = 1; z <= 13; z++) {
            helper.setBlock(x, 0, z, Blocks.STONE);
            for (int y = 1; y <= 3; y++) helper.setBlock(x, y, z, Blocks.WATER);
        }
        SailingShipEntity[] ships = {
            helper.spawn(CoalShipContent.COAL_SHIP_ENTITY.get(), 6, 4, 7),
            helper.spawn(CoalShipContent.CARAVEL_ENTITY.get(), 18, 4, 7),
            helper.spawn(CoalShipContent.GREAT_SHIP_ENTITY.get(), 30, 4, 7)
        };
        helper.runAtTickTime(40, () -> {
            Vec3[] positions = java.util.Arrays.stream(ships).map(SailingShipEntity::position).toArray(Vec3[]::new);
            helper.runAfterDelay(30, () -> {
                for (int i = 0; i < ships.length; i++) {
                    SailingShipEntity ship = ships[i];
                    helper.assertTrue(ship.position().subtract(positions[i]).horizontalDistance() > .01,
                            "Unoccupied ship must drift: " + i);
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    ship.toggleAnchor(player);
                    positions[i] = ship.position();
                    ship.setDeltaMovement(new Vec3(.09, 0, .09));
                }
                helper.runAfterDelay(25, () -> {
                    for (int i = 0; i < ships.length; i++) {
                        helper.assertTrue(ships[i].position().subtract(positions[i]).horizontalDistance() < .0001,
                                "Anchor must stop drift: " + i);
                        helper.assertTrue(!ships[i].isUnderWater(), "Hull must remain afloat: " + i);
                    }
                    helper.succeed();
                });
            });
        });
    }
}
