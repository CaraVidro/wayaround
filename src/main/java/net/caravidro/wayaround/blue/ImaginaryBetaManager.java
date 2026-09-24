package net.caravidro.wayaround.blue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.network.BetaTechniqueVisualPayload;
import net.caravidro.wayaround.sounds.WayAroundSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Experimental Otherworld Powers beta:
 *
 * RED is a fast destructive cube fired along the player's crosshair.
 * A RED colliding with that player's stationary BLUE starts a synchronized
 * PURPLE cinematic sequence.
 */
@net.neoforged.fml.common.EventBusSubscriber(
        modid = WayAround.MODID
)
public final class ImaginaryBetaManager {

    private ImaginaryBetaManager() {
    }

    private static final int RED_LIFE_TICKS =
            62;

    private static final double RED_SPEED =
            3.35;

    private static final double RED_RADIUS =
            0.68;

    private static final int RED_SUBSTEPS =
            12;

    private static final int PURPLE_BLAST_TICK =
            92;

    private static final int PURPLE_END_TICK =
            172;

    private static final double VISUAL_RANGE =
            256.0;

    private static final Map<UUID, RedProjectile>
            REDS =
            new HashMap<>();

    private static final Map<UUID, PurpleFusion>
            FUSIONS =
            new HashMap<>();

    public static boolean fireRed(
            ServerPlayer player
    ) {
        if (FUSIONS.containsKey(
                player.getUUID()
        )) {
            return false;
        }

        Vec3 direction =
                player.getLookAngle()
                        .normalize();

        Vec3 position =
                player.getEyePosition()
                        .add(
                                direction.scale(
                                        1.15
                                )
                        );

        REDS.put(
                player.getUUID(),
                new RedProjectile(
                        player.getUUID(),
                        player.serverLevel()
                                .dimension(),
                        position,
                        direction.scale(
                                RED_SPEED
                        ),
                        RED_LIFE_TICKS
                )
        );

        player.swing(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                true
        );

        player.serverLevel()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.FIREWORK_ROCKET_LAUNCH,
                        SoundSource.PLAYERS,
                        0.85F,
                        0.62F
                );

        sendVisual(
                player.serverLevel(),
                player.getUUID(),
                BetaTechniqueVisualPayload.RED,
                position,
                1.0F,
                0.0F
        );

        return true;
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        MinecraftServer server =
                event.getServer();

        tickReds(
                server
        );

        tickFusions(
                server
        );
    }

    private static void tickReds(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, RedProjectile>>
                iterator =
                REDS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            RedProjectile red =
                    iterator.next()
                            .getValue();

            ServerPlayer owner =
                    server.getPlayerList()
                            .getPlayer(
                                    red.owner
                            );

            ServerLevel level =
                    server.getLevel(
                            red.dimension
                    );

            if (owner == null
                    || level == null
                    || !owner.isAlive()
                    || owner.serverLevel() != level) {

                iterator.remove();
                continue;
            }

            red.life--;

            if (red.life <= 0) {
                iterator.remove();
                continue;
            }

            Vec3 step =
                    red.velocity.scale(
                            1.0
                                    / RED_SUBSTEPS
                    );

            boolean fused =
                    false;

            for (int sub = 0;
                 sub < RED_SUBSTEPS;
                 sub++) {

                red.position =
                        red.position.add(
                                step
                        );

                BlueManager.FusionSeed blue =
                        BlueManager
                                .consumeHeldBlueForFusion(
                                        owner,
                                        red.position,
                                        RED_RADIUS
                                );

                if (blue != null) {
                    beginFusion(
                            level,
                            owner,
                            blue
                    );

                    fused =
                            true;

                    break;
                }

                destroyRedPath(
                        level,
                        red.position
                );
            }

            if (fused) {
                iterator.remove();
                continue;
            }

            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    red.position.x,
                    red.position.y,
                    red.position.z,
                    4,
                    0.10,
                    0.10,
                    0.10,
                    0.08
            );

            level.sendParticles(
                    ParticleTypes.FLAME,
                    red.position.x,
                    red.position.y,
                    red.position.z,
                    2,
                    0.08,
                    0.08,
                    0.08,
                    0.02
            );

            sendVisual(
                    level,
                    red.owner,
                    BetaTechniqueVisualPayload.RED,
                    red.position,
                    1.0F,
                    1.0F
                            - red.life
                                    / (float) RED_LIFE_TICKS
            );
        }
    }

    private static void destroyRedPath(
            ServerLevel level,
            Vec3 center
    ) {
        int radius =
                1;

        BlockPos origin =
                BlockPos.containing(
                        center
                );

        for (BlockPos sample :
                BlockPos.betweenClosed(
                        origin.offset(
                                -radius,
                                -radius,
                                -radius
                        ),
                        origin.offset(
                                radius,
                                radius,
                                radius
                        )
                )) {

            Vec3 blockCenter =
                    Vec3.atCenterOf(
                            sample
                    );

            if (blockCenter.distanceToSqr(
                    center
            )
                    > RED_RADIUS
                            * RED_RADIUS
                            * 2.25) {

                continue;
            }

            BlockState state =
                    level.getBlockState(
                            sample
                    );

            if (state.isAir()
                    || state.getDestroySpeed(
                            level,
                            sample
                    ) < 0.0F) {

                continue;
            }

            level.removeBlock(
                    sample,
                    false
            );

            level.sendParticles(
                    new BlockParticleOption(
                            ParticleTypes.BLOCK,
                            state
                    ),
                    blockCenter.x,
                    blockCenter.y,
                    blockCenter.z,
                    5,
                    0.34,
                    0.34,
                    0.34,
                    0.26
            );
        }
    }

    private static void beginFusion(
            ServerLevel level,
            ServerPlayer owner,
            BlueManager.FusionSeed blue
    ) {
        /*
         * IMPORTANT: tickReds() is already iterating REDS with an Iterator.
         * Removing from the backing map here invalidates that iterator and
         * caused the PURPLE fusion crash (ConcurrentModificationException).
         * The caller removes the fused RED through iterator.remove().
         */
        PurpleFusion fusion =
                new PurpleFusion(
                        owner.getUUID(),
                        blue.dimension(),
                        blue.center(),
                        Math.max(
                                0.78F,
                                blue.power()
                        ),
                        blue.spinDirection()
                );

        FUSIONS.put(
                owner.getUUID(),
                fusion
        );

        level.playSound(
                null,
                BlockPos.containing(
                        fusion.center
                ),
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.PLAYERS,
                1.55F,
                0.72F
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                110,
                3.2,
                3.2,
                3.2,
                0.44
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                36,
                1.6,
                1.6,
                1.6,
                0.18
        );

        sendVisual(
                level,
                fusion.owner,
                BetaTechniqueVisualPayload.FUSION,
                fusion.center,
                fusion.power,
                0.0F
        );
    }

    private static void tickFusions(
            MinecraftServer server
    ) {
        Iterator<Map.Entry<UUID, PurpleFusion>>
                iterator =
                FUSIONS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            PurpleFusion fusion =
                    iterator.next()
                            .getValue();

            ServerLevel level =
                    server.getLevel(
                            fusion.dimension
                    );

            if (level == null) {
                iterator.remove();
                continue;
            }

            fusion.age++;

            if (fusion.age
                    < PURPLE_BLAST_TICK) {

                float progress =
                        Mth.clamp(
                                fusion.age
                                        / (float) PURPLE_BLAST_TICK,
                                0.0F,
                                1.0F
                        );

                /*
                 * Keep the original BLUE snapshot alive so its spatial theme
                 * does not fade while RED is being fused into it.
                 */
                BlueManager.keepFusionBlueVisible(
                        level,
                        fusion.owner,
                        fusion.center,
                        fusion.power
                );

                if (fusion.age % 2 == 0) {
                    emitFusionArcs(
                            level,
                            fusion,
                            progress
                    );
                }

                sendVisual(
                        level,
                        fusion.owner,
                        BetaTechniqueVisualPayload.FUSION,
                        fusion.center,
                        fusion.power,
                        progress
                );

                continue;
            }

            if (!fusion.blastTriggered) {
                fusion.blastTriggered =
                        true;

                detonatePurple(
                        level,
                        fusion
                );
            }

            float aftermath =
                    Mth.clamp(
                            (
                                    fusion.age
                                            - PURPLE_BLAST_TICK
                            )
                                    / (float) (
                                    PURPLE_END_TICK
                                            - PURPLE_BLAST_TICK
                            ),
                            0.0F,
                            1.0F
                    );

            sendVisual(
                    level,
                    fusion.owner,
                    BetaTechniqueVisualPayload.AFTERMATH,
                    fusion.center,
                    fusion.power,
                    aftermath
            );

            if (fusion.age
                    >= PURPLE_END_TICK) {

                BlueManager.finishFusion(
                        server,
                        fusion.owner
                );

                iterator.remove();
            }
        }
    }

    private static void emitFusionArcs(
            ServerLevel level,
            PurpleFusion fusion,
            float progress
    ) {
        int rays =
                12
                        + Math.round(
                                progress
                                        * 30.0F
                        );

        double radius =
                1.5
                        + progress
                                * 9.2;

        for (int index = 0;
             index < rays;
             index++) {

            double angle =
                    index
                            * (
                            Math.PI
                                    * 2.0
                                    / rays
                    )
                            + fusion.age
                                    * (
                                    0.10
                                            + progress
                                                    * progress
                                                    * 0.66
                            )
                                    * fusion.spinDirection;

            double y =
                    Math.sin(
                            angle * 1.73
                    )
                            * radius
                            * 0.34;

            Vec3 source =
                    fusion.center.add(
                            Math.cos(
                                    angle
                            )
                                    * radius,
                            y,
                            Math.sin(
                                    angle
                            )
                                    * radius
                    );

            Vec3 inward =
                    fusion.center.subtract(
                            source
                    );

            if (inward.lengthSqr()
                    > 0.0001) {

                inward =
                        inward.normalize();
            }

            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    source.x,
                    source.y,
                    source.z,
                    0,
                    inward.x,
                    inward.y,
                    inward.z,
                    0.42
                            + progress
                                    * 0.74
            );
        }
    }

    private static void detonatePurple(
            ServerLevel level,
            PurpleFusion fusion
    ) {
        /*
         * One authoritative explosion. The fire flag is intentional: PURPLE
         * is the rare fusion finisher, not the ordinary BLUE lifecycle.
         */
        level.explode(
                null,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                24.0F,
                true,
                Level.ExplosionInteraction.TNT
        );

        level.playSound(
                null,
                BlockPos.containing(
                        fusion.center
                ),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                4.0F,
                0.62F
        );

        level.sendParticles(
                ParticleTypes.FLASH,
                fusion.center.x,
                fusion.center.y,
                fusion.center.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );

        /*
         * Shockwave particles are emitted with explicit outward velocity
         * instead of lingering at the center.
         */
        for (int index = 0;
             index < 360;
             index++) {

            double y =
                    level.random.nextDouble()
                            * 2.0
                            - 1.0;

            double theta =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double horizontal =
                    Math.sqrt(
                            Math.max(
                                    0.0,
                                    1.0
                                            - y * y
                            )
                    );

            double speed =
                    1.3
                            + level.random.nextDouble()
                                    * 2.6;

            Vec3 velocity =
                    new Vec3(
                            Math.cos(
                                    theta
                            )
                                    * horizontal
                                    * speed,
                            y * speed,
                            Math.sin(
                                    theta
                            )
                                    * horizontal
                                    * speed
                    );

            level.sendParticles(
                    index % 5 == 0
                            ? ParticleTypes.END_ROD
                            : ParticleTypes.ELECTRIC_SPARK,
                    fusion.center.x,
                    fusion.center.y,
                    fusion.center.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0
            );
        }

        igniteAftermath(
                level,
                fusion.center
        );

        sendVisual(
                level,
                fusion.owner,
                BetaTechniqueVisualPayload.BLAST,
                fusion.center,
                fusion.power,
                0.0F
        );
    }

    private static void igniteAftermath(
            ServerLevel level,
            Vec3 center
    ) {
        for (int attempt = 0;
             attempt < 190;
             attempt++) {

            double angle =
                    level.random.nextDouble()
                            * Math.PI
                            * 2.0;

            double radius =
                    4.0
                            + Math.sqrt(
                            level.random.nextDouble()
                    )
                                    * 24.0;

            int x =
                    Mth.floor(
                            center.x
                                    + Math.cos(
                                    angle
                            )
                                    * radius
                    );

            int z =
                    Mth.floor(
                            center.z
                                    + Math.sin(
                                    angle
                            )
                                    * radius
                    );

            int y =
                    Mth.floor(
                            center.y
                                    - 7
                                    + level.random.nextInt(
                                    15
                            )
                    );

            BlockPos pos =
                    new BlockPos(
                            x,
                            y,
                            z
                    );

            if (!level.getBlockState(
                    pos
            )
                    .isAir()
                    || level.getBlockState(
                    pos.below()
            )
                    .isAir()) {

                continue;
            }

            level.setBlockAndUpdate(
                    pos,
                    Blocks.FIRE
                            .defaultBlockState()
            );
        }
    }

    private static void sendVisual(
            ServerLevel level,
            UUID owner,
            byte mode,
            Vec3 position,
            float power,
            float progress
    ) {
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                position.x,
                position.y,
                position.z,
                VISUAL_RANGE,
                new BetaTechniqueVisualPayload(
                        owner,
                        mode,
                        position.x,
                        position.y,
                        position.z,
                        power,
                        progress
                )
        );
    }

    private static final class RedProjectile {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;

        private Vec3 position;
        private final Vec3 velocity;
        private int life;

        private RedProjectile(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 position,
                Vec3 velocity,
                int life
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;

            this.position =
                    position;

            this.velocity =
                    velocity;

            this.life =
                    life;
        }
    }

    private static final class PurpleFusion {

        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<Level>
                dimension;
        private final Vec3 center;
        private final float power;
        private final double spinDirection;

        private int age;
        private boolean blastTriggered;

        private PurpleFusion(
                UUID owner,
                net.minecraft.resources.ResourceKey<Level> dimension,
                Vec3 center,
                float power,
                double spinDirection
        ) {
            this.owner =
                    owner;

            this.dimension =
                    dimension;

            this.center =
                    center;

            this.power =
                    power;

            this.spinDirection =
                    spinDirection;
        }
    }
}
