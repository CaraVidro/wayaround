package net.caravidro.wayaround.industrial.ship;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public final class ShipMenu
        extends AbstractContainerMenu {

    private final SailingShipEntity ship;

    private final DataSlot large =
            DataSlot.standalone();

    private final DataSlot anchored =
            DataSlot.standalone();

    private final DataSlot hull =
            DataSlot.standalone();

    private final DataSlot flooding =
            DataSlot.standalone();

    private final DataSlot trim =
            DataSlot.standalone();

    private final DataSlot sails =
            DataSlot.standalone();

    private final DataSlot wind =
            DataSlot.standalone();

    private final DataSlot heading =
            DataSlot.standalone();

    private final DataSlot sea =
            DataSlot.standalone();

    public ShipMenu(
            int id,
            Inventory inventory
    ) {
        this(
                id,
                inventory,
                null
        );
    }

    public ShipMenu(
            int id,
            Inventory inventory,
            SailingShipEntity ship
    ) {
        super(
                CoalShipContent.SHIP_MENU.get(),
                id
        );

        this.ship =
                ship;

        addDataSlot(
                large
        );
        addDataSlot(
                anchored
        );
        addDataSlot(
                hull
        );
        addDataSlot(
                flooding
        );
        addDataSlot(
                trim
        );
        addDataSlot(
                sails
        );
        addDataSlot(
                wind
        );
        addDataSlot(
                heading
        );
        addDataSlot(
                sea
        );

        updateState();
    }

    private void updateState() {
        if (ship == null) {
            return;
        }

        boolean isNau =
                ship
                        instanceof GreatShipEntity;

        large.set(
                isNau
                        ? 1
                        : 0
        );

        anchored.set(
                ship.isAnchored()
                        ? 1
                        : 0
        );

        if (ship
                instanceof GreatShipEntity nau) {
            hull.set(
                    Math.round(
                            nau.hullIntegrity()
                    )
            );

            flooding.set(
                    Math.round(
                            nau.flooding()
                    )
            );

            trim.set(
                    nau.sailTrim()
            );

            sails.set(
                    nau.sailsRaised()
                            ? 1
                            : 0
            );

            wind.set(
                    nau.windEfficiencyPercent()
            );

            heading.set(
                    nau.headingDegrees()
            );

            sea.set(
                    nau.seaSeverityPercent()
            );
        }
    }

    public boolean isLarge() {
        return large.get()
                != 0;
    }

    public boolean isAnchored() {
        return anchored.get()
                != 0;
    }

    public int hullPercent() {
        return hull.get();
    }

    public int floodingPercent() {
        return flooding.get();
    }

    public int sailTrim() {
        return trim.get();
    }

    public boolean sailsRaised() {
        return sails.get()
                != 0;
    }

    public int windEfficiency() {
        return wind.get();
    }

    public int heading() {
        return heading.get();
    }

    public int seaSeverity() {
        return sea.get();
    }

    @Override
    public void broadcastChanges() {
        updateState();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        return ship == null
                || ship.canUse(
                player
        );
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(
            Player player,
            int action
    ) {
        if (ship == null
                || !ship.canUse(
                player
        )) {
            return false;
        }

        if (action == 0) {
            ship.toggleAnchor(
                    player
            );

            updateState();
            return true;
        }

        if (action == 1) {
            player.openMenu(
                    ship
            );

            return true;
        }

        if (ship
                instanceof GreatShipEntity nau) {

            if (action == 2) {
                nau.openWorkbench(
                        player
                );

                return true;
            }

            if (action == 3) {
                nau.sleep(
                        player
                );

                return true;
            }

            if (action == 4) {
                nau.adjustSailTrim(
                        player,
                        -15
                );

                updateState();
                return true;
            }

            if (action == 5) {
                nau.adjustSailTrim(
                        player,
                        15
                );

                updateState();
                return true;
            }

            if (action == 6) {
                nau.toggleSails(
                        player
                );

                updateState();
                return true;
            }

            if (action == 7) {
                nau.pumpWater(
                        player
                );

                updateState();
                return true;
            }

            if (action >= 10
                    && action < 16) {
                nau.board(
                        player,
                        action - 10
                );

                return true;
            }
        } else if (action == 10
                && !player.isPassenger()) {
            player.closeContainer();
            player.startRiding(
                    ship
            );

            return true;
        }

        return false;
    }
}
