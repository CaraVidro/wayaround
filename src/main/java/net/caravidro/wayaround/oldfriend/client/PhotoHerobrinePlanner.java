package net.caravidro.wayaround.oldfriend.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Picks a real world position for photographic Herobrine appearances.
 *
 * Nothing is painted onto the PNG. The planner samples the actual camera
 * frustum, finds visible walkable surfaces, scores discreet positions and asks
 * the server to spawn a short-lived physical Herobrine there before the
 * screenshot is taken.
 */
public final class PhotoHerobrinePlanner {

    private static final int RAY_COLUMNS =
            30;

    private static final int RAY_ROWS =
            18;

    private static final double MIN_DISTANCE =
            9.0;

    private static final double MAX_DISTANCE =
            88.0;

    public record Placement(
            BlockPos feetPos,
            double score,
            int visiblePlayers,
            boolean social
    ) {
    }

    private record Projection(
            double u,
            double v,
            double depth,
            boolean visible
    ) {
    }

    private record VisiblePlayer(
            UUID id,
            Vec3 worldPos,
            double u,
            double v,
            double depth
    ) {
    }

    private record Anchor(
            BlockPos feetPos,
            double u,
            double v,
            double depth,
            boolean nearCover,
            boolean nearEdge
    ) {
    }

    private PhotoHerobrinePlanner() {
    }

    public static Optional<Placement> choose(
            Minecraft minecraft
    ) {
        if (minecraft.level == null
                || minecraft.player == null) {
            return Optional.empty();
        }

        ClientLevel level =
                minecraft.level;

        Vec3 cameraPos =
                minecraft.gameRenderer
                        .getMainCamera()
                        .getPosition();

        Vec3 look =
                minecraft.player
                        .getViewVector(
                                1.0F
                        )
                        .normalize();

        Basis basis =
                basis(
                        look
                );

        int width =
                Math.max(
                        1,
                        minecraft.getWindow()
                                .getWidth()
                );

        int height =
                Math.max(
                        1,
                        minecraft.getWindow()
                                .getHeight()
                );

        double verticalFov =
                Math.toRadians(
                        Math.max(
                                30,
                                Math.min(
                                        110,
                                        minecraft.options
                                                .fov()
                                                .get()
                                )
                        )
                );

        double tanVertical =
                Math.tan(
                        verticalFov * 0.5
                );

        double tanHorizontal =
                tanVertical
                        * (
                        width
                                / (double) height
                );

        List<VisiblePlayer> visiblePlayers =
                collectVisiblePlayers(
                        minecraft,
                        cameraPos,
                        basis,
                        tanHorizontal,
                        tanVertical
                );

        List<Anchor> anchors =
                collectAnchors(
                        minecraft,
                        cameraPos,
                        basis,
                        tanHorizontal,
                        tanVertical
                );

        if (anchors.isEmpty()) {
            return Optional.empty();
        }

        Anchor best =
                anchors.stream()
                        .max(
                                Comparator.comparingDouble(
                                        anchor ->
                                                score(
                                                        anchor,
                                                        visiblePlayers
                                                )
                                )
                        )
                        .orElse(null);

        if (best == null) {
            return Optional.empty();
        }

        double score =
                score(
                        best,
                        visiblePlayers
                );

        boolean social =
                visiblePlayers.stream()
                        .anyMatch(
                                player ->
                                        isSociallyPlaced(
                                                best,
                                                player
                                        )
                        );

        return Optional.of(
                new Placement(
                        best.feetPos(),
                        score,
                        visiblePlayers.size(),
                        social
                )
        );
    }

    private static List<Anchor> collectAnchors(
            Minecraft minecraft,
            Vec3 cameraPos,
            Basis basis,
            double tanHorizontal,
            double tanVertical
    ) {
        ClientLevel level =
                minecraft.level;

        ArrayList<Anchor> result =
                new ArrayList<>();

        Set<BlockPos> seen =
                new HashSet<>();

        for (int row = 0;
             row < RAY_ROWS;
             row++) {
            for (int column = 0;
                 column < RAY_COLUMNS;
                 column++) {

                double u =
                        (
                                column + 0.5
                        ) / RAY_COLUMNS;

                double v =
                        (
                                row + 0.5
                        ) / RAY_ROWS;

                Vec3 direction =
                        rayDirection(
                                basis,
                                tanHorizontal,
                                tanVertical,
                                u,
                                v
                        );

                BlockHitResult hit =
                        level.clip(
                                new ClipContext(
                                        cameraPos,
                                        cameraPos.add(
                                                direction.scale(
                                                        MAX_DISTANCE
                                                )
                                        ),
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        minecraft.player
                                )
                        );

                if (hit.getType()
                        != HitResult.Type.BLOCK) {
                    continue;
                }

                /*
                 * We only trust a surface that the camera ray actually sees
                 * from above. Side-wall hits were the main source of the old
                 * "Herobrine floating next to a block" look.
                 */
                if (hit.getDirection()
                        != Direction.UP) {
                    continue;
                }

                BlockPos support =
                        hit.getBlockPos();

                BlockPos feet =
                        support.above();

                if (!seen.add(
                        feet
                )
                        || !canStand(
                        level,
                        feet
                )) {
                    continue;
                }

                Vec3 feetCenter =
                        Vec3.atBottomCenterOf(
                                feet
                        );

                Projection projection =
                        project(
                                cameraPos,
                                feetCenter.add(
                                        0.0,
                                        0.90,
                                        0.0
                                ),
                                basis,
                                tanHorizontal,
                                tanVertical
                        );

                if (!projection.visible()) {
                    continue;
                }

                double distance =
                        feetCenter.distanceTo(
                                cameraPos
                        );

                if (distance < MIN_DISTANCE
                        || distance > MAX_DISTANCE) {
                    continue;
                }

                result.add(
                        new Anchor(
                                feet.immutable(),
                                projection.u(),
                                projection.v(),
                                projection.depth(),
                                hasNearbyCover(
                                        level,
                                        feet
                                ),
                                nearEdge(
                                        projection.u(),
                                        projection.v()
                                )
                        )
                );
            }
        }

        return result;
    }

    private static List<VisiblePlayer> collectVisiblePlayers(
            Minecraft minecraft,
            Vec3 cameraPos,
            Basis basis,
            double tanHorizontal,
            double tanVertical
    ) {
        ClientLevel level =
                minecraft.level;

        ArrayList<VisiblePlayer> result =
                new ArrayList<>();

        for (AbstractClientPlayer player :
                level.players()) {
            if (player == minecraft.player
                    || player.isSpectator()
                    || !player.isAlive()) {
                continue;
            }

            Vec3 eye =
                    player.getEyePosition();

            Projection projection =
                    project(
                            cameraPos,
                            eye,
                            basis,
                            tanHorizontal,
                            tanVertical
                    );

            if (!projection.visible()
                    || projection.depth()
                    > MAX_DISTANCE) {
                continue;
            }

            BlockHitResult obstruction =
                    level.clip(
                            new ClipContext(
                                    cameraPos,
                                    eye,
                                    ClipContext.Block.COLLIDER,
                                    ClipContext.Fluid.NONE,
                                    minecraft.player
                            )
                    );

            if (obstruction.getType()
                    == HitResult.Type.BLOCK
                    && obstruction.getLocation()
                    .distanceToSqr(
                            cameraPos
                    )
                    + 0.35
                    < eye.distanceToSqr(
                            cameraPos
                    )) {
                continue;
            }

            result.add(
                    new VisiblePlayer(
                            player.getUUID(),
                            player.position(),
                            projection.u(),
                            projection.v(),
                            projection.depth()
                    )
            );
        }

        return result;
    }

    private static double score(
            Anchor anchor,
            List<VisiblePlayer> players
    ) {
        double score =
                Math.min(
                        72.0,
                        anchor.depth()
                )
                        * 0.075;

        if (anchor.nearCover()) {
            score +=
                    3.0;
        }

        if (anchor.nearEdge()) {
            score +=
                    2.4;
        }

        // Avoid the exact centre: discoveries should feel accidental.
        double fromCenter =
                Math.abs(
                        anchor.u()
                                - 0.5
                )
                        + Math.abs(
                        anchor.v()
                                - 0.52
                );

        score +=
                Math.min(
                        1.6,
                        fromCenter * 2.2
                );

        for (VisiblePlayer player :
                players) {
            double du =
                    anchor.u()
                            - player.u();

            double dv =
                    anchor.v()
                            - player.v();

            double screenDistanceSq =
                    du * du
                            + dv * dv;

            /*
             * Strongest social placement: almost the same screen region as a
             * player, but physically farther away. That naturally produces
             * "behind them" or "in the background next to them" photographs.
             */
            if (anchor.depth()
                    > player.depth()
                            + 1.8) {
                score +=
                        10.0
                                * Math.exp(
                                -screenDistanceSq
                                        / 0.018
                        );
            }

            if (screenDistanceSq < 0.045) {
                score +=
                        2.4;
            }
        }

        if (players.size()
                >= 2) {
            for (int i = 0;
                 i < players.size();
                 i++) {
                for (int j = i + 1;
                     j < players.size();
                     j++) {
                    VisiblePlayer a =
                            players.get(
                                    i
                            );

                    VisiblePlayer b =
                            players.get(
                                    j
                            );

                    double midpointU =
                            (
                                    a.u()
                                            + b.u()
                            ) * 0.5;

                    double midpointV =
                            (
                                    a.v()
                                            + b.v()
                            ) * 0.5;

                    double du =
                            anchor.u()
                                    - midpointU;

                    double dv =
                            anchor.v()
                                    - midpointV;

                    double midpointDistance =
                            Math.sqrt(
                                    du * du
                                            + dv * dv
                            );

                    if (anchor.depth()
                            > Math.max(
                            a.depth(),
                            b.depth()
                    )
                            + 1.5) {
                        score +=
                                Math.max(
                                        0.0,
                                        4.5
                                                - midpointDistance
                                                * 18.0
                                );
                    }
                }
            }
        }

        // Stable tiny variation so repeated photos don't always pick one cell.
        score +=
                Math.floorMod(
                        anchor.feetPos()
                                .hashCode(),
                        997
                ) / 9970.0;

        return score;
    }

    private static boolean isSociallyPlaced(
            Anchor anchor,
            VisiblePlayer player
    ) {
        if (anchor.depth()
                <= player.depth()
                        + 1.5) {
            return false;
        }

        double du =
                anchor.u()
                        - player.u();

        double dv =
                anchor.v()
                        - player.v();

        return du * du
                + dv * dv
                < 0.055;
    }

    private static boolean canStand(
            ClientLevel level,
            BlockPos feet
    ) {
        BlockPos support =
                feet.below();

        BlockState supportState =
                level.getBlockState(
                        support
                );

        return supportState.isFaceSturdy(
                level,
                support,
                Direction.UP
        )
                && level.getBlockState(
                feet
        ).isAir()
                && level.getBlockState(
                feet.above()
        ).isAir()
                && level.getFluidState(
                feet
        ).isEmpty()
                && level.getFluidState(
                feet.above()
        ).isEmpty();
    }

    private static boolean hasNearbyCover(
            ClientLevel level,
            BlockPos feet
    ) {
        for (BlockPos pos :
                BlockPos.betweenClosed(
                        feet.offset(
                                -2,
                                -1,
                                -2
                        ),
                        feet.offset(
                                2,
                                2,
                                2
                        )
                )) {
            if (pos.equals(
                    feet
            )
                    || pos.equals(
                    feet.above()
            )
                    || pos.equals(
                    feet.below()
            )) {
                continue;
            }

            BlockState state =
                    level.getBlockState(
                            pos
                    );

            if (state.is(
                    BlockTags.LOGS
            )
                    || (
                    !state.isAir()
                            && state.isCollisionShapeFullBlock(
                            level,
                            pos
                    )
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean nearEdge(
            double u,
            double v
    ) {
        return u < 0.23
                || u > 0.77
                || v < 0.18
                || v > 0.82;
    }

    private static Projection project(
            Vec3 camera,
            Vec3 point,
            Basis basis,
            double tanHorizontal,
            double tanVertical
    ) {
        Vec3 relative =
                point.subtract(
                        camera
                );

        double depth =
                relative.dot(
                        basis.forward()
                );

        if (depth <= 0.15) {
            return new Projection(
                    0.0,
                    0.0,
                    depth,
                    false
            );
        }

        double ndcX =
                relative.dot(
                        basis.right()
                )
                        / (
                        depth
                                * tanHorizontal
                );

        double ndcY =
                relative.dot(
                        basis.up()
                )
                        / (
                        depth
                                * tanVertical
                );

        double u =
                0.5
                        + ndcX * 0.5;

        double v =
                0.5
                        - ndcY * 0.5;

        return new Projection(
                u,
                v,
                depth,
                u >= 0.0
                        && u <= 1.0
                        && v >= 0.0
                        && v <= 1.0
        );
    }

    private static Vec3 rayDirection(
            Basis basis,
            double tanHorizontal,
            double tanVertical,
            double u,
            double v
    ) {
        double ndcX =
                u * 2.0
                        - 1.0;

        double ndcY =
                1.0
                        - v * 2.0;

        return basis.forward()
                .add(
                        basis.right()
                                .scale(
                                        ndcX
                                                * tanHorizontal
                                )
                )
                .add(
                        basis.up()
                                .scale(
                                        ndcY
                                                * tanVertical
                                )
                )
                .normalize();
    }

    private static Basis basis(
            Vec3 forward
    ) {
        Vec3 right =
                new Vec3(
                        -forward.z,
                        0.0,
                        forward.x
                );

        if (right.lengthSqr()
                < 0.0001) {
            right =
                    new Vec3(
                            1.0,
                            0.0,
                            0.0
                    );
        } else {
            right =
                    right.normalize();
        }

        Vec3 up =
                right.cross(
                        forward
                )
                        .normalize();

        return new Basis(
                forward,
                right,
                up
        );
    }

    private record Basis(
            Vec3 forward,
            Vec3 right,
            Vec3 up
    ) {
    }
}
