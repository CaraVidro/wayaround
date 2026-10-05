package net.caravidro.wayaround.weaponry.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.caravidro.wayaround.client.cinematic.PlayerAnimationController;
import net.caravidro.wayaround.weaponry.WayWeaponItem;
import net.caravidro.wayaround.weaponry.WeaponFamily;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class WeaponAnimationClient {

    private record SwingState(
            boolean active,
            int variant
    ) {
    }

    private static final Map<UUID, SwingState> SWINGS =
            new HashMap<>();

    private WeaponAnimationClient() {
    }

    public static void apply(
            AbstractClientPlayer player,
            PlayerModel<?> model
    ) {
        if (PlayerAnimationController.hasActiveAnimation(
                player.getUUID()
        )) {
            return;
        }

        if (!(player.getMainHandItem()
                .getItem()
                instanceof WayWeaponItem weapon)) {
            SWINGS.remove(
                    player.getUUID()
            );

            return;
        }

        float attack =
                Mth.clamp(
                        model.attackTime,
                        0.0F,
                        1.0F
                );

        boolean active =
                attack > 0.001F;

        SwingState old =
                SWINGS.getOrDefault(
                        player.getUUID(),
                        new SwingState(
                                false,
                                -1
                        )
                );

        int variant =
                old.variant();

        if (active
                && !old.active()) {
            variant =
                    Math.floorMod(
                            variant + 1,
                            3
                    );
        }

        SWINGS.put(
                player.getUUID(),
                new SwingState(
                        active,
                        variant
                )
        );

        if (!active) {
            return;
        }

        float phase =
                (float) Math.sin(
                        attack
                                * Math.PI
                );

        float snap =
                ease(
                        Math.min(
                                1.0F,
                                attack
                                        * 2.6F
                        )
                );

        boolean leftMain =
                player.getMainArm()
                        == HumanoidArm.LEFT;

        boolean dualDaggers =
                weapon.family()
                        == WeaponFamily.DAGGER
                        && player.getOffhandItem()
                        .getItem()
                        instanceof WayWeaponItem offhand
                        && offhand.family()
                        == WeaponFamily.DAGGER;

        switch (weapon.family()) {
            case DAGGER ->
                    dagger(
                            model,
                            variant,
                            phase,
                            snap,
                            leftMain,
                            dualDaggers
                    );

            case KATANA ->
                    katana(
                            model,
                            variant,
                            phase,
                            snap,
                            leftMain
                    );

            case SCYTHE ->
                    scythe(
                            model,
                            variant,
                            phase,
                            snap,
                            leftMain
                    );
        }
    }

    private static void dagger(
            PlayerModel<?> model,
            int variant,
            float phase,
            float snap,
            boolean leftMain,
            boolean dual
    ) {
        var main =
                leftMain
                        ? model.leftArm
                        : model.rightArm;

        var off =
                leftMain
                        ? model.rightArm
                        : model.leftArm;

        float side =
                leftMain
                        ? -1.0F
                        : 1.0F;

        model.body.yRot +=
                side
                        * (
                        dual
                                ? 0.08F
                                : 0.14F
                )
                        * phase;

        if (dual) {
            switch (variant) {
                case 1 -> {
                    main.xRot =
                            -1.45F
                                    - 0.55F
                                            * phase;

                    off.xRot =
                            -1.15F
                                    - 0.72F
                                            * phase;

                    main.yRot =
                            -side
                                    * 0.42F;

                    off.yRot =
                            side
                                    * 0.56F;

                    main.zRot =
                            -side
                                    * 0.18F;

                    off.zRot =
                            side
                                    * 0.24F;
                }

                case 2 -> {
                    main.xRot =
                            -0.72F
                                    - 1.05F
                                            * phase;

                    off.xRot =
                            -1.95F
                                    + 0.64F
                                            * phase;

                    main.yRot =
                            side
                                    * 0.58F;

                    off.yRot =
                            -side
                                    * 0.44F;

                    main.zRot =
                            side
                                    * 0.34F;

                    off.zRot =
                            -side
                                    * 0.30F;
                }

                default -> {
                    main.xRot =
                            -1.58F
                                    - 0.48F
                                            * snap;

                    off.xRot =
                            -1.58F
                                    - 0.48F
                                            * (
                                            1.0F
                                                    - snap
                                    );

                    main.yRot =
                            -side
                                    * 0.34F;

                    off.yRot =
                            side
                                    * 0.34F;

                    main.zRot =
                            side
                                    * 0.16F;

                    off.zRot =
                            -side
                                    * 0.16F;
                }
            }

            return;
        }

        switch (variant) {
            case 1 -> {
                main.xRot =
                        -1.18F
                                - 0.55F
                                        * phase;

                main.yRot =
                        side
                                * (
                                0.92F
                                        - 1.28F
                                                * snap
                        );

                main.zRot =
                        side
                                * 0.28F
                                * phase;
            }

            case 2 -> {
                main.xRot =
                        -2.18F
                                + 1.34F
                                        * snap;

                main.yRot =
                        -side
                                * 0.48F
                                * phase;

                main.zRot =
                        side
                                * (
                                0.72F
                                        - 0.92F
                                                * snap
                        );
            }

            default -> {
                main.xRot =
                        -1.28F
                                - 0.78F
                                        * phase;

                main.yRot =
                        -side
                                * 0.16F;

                main.zRot =
                        side
                                * 0.08F;
            }
        }
    }

    private static void katana(
            PlayerModel<?> model,
            int variant,
            float phase,
            float snap,
            boolean leftMain
    ) {
        var main =
                leftMain
                        ? model.leftArm
                        : model.rightArm;

        var off =
                leftMain
                        ? model.rightArm
                        : model.leftArm;

        float side =
                leftMain
                        ? -1.0F
                        : 1.0F;

        switch (variant) {
            case 1 -> {
                // Fast horizontal waist-height sweep.
                model.body.yRot +=
                        side
                                * (
                                0.72F
                                        - 1.18F
                                                * snap
                        );

                main.xRot =
                        -1.14F;

                main.yRot =
                        side
                                * (
                                1.05F
                                        - 1.72F
                                                * snap
                        );

                main.zRot =
                        side
                                * 0.08F;

                off.xRot =
                        -0.48F;

                off.yRot =
                        -side
                                * 0.34F;
            }

            case 2 -> {
                // Reverse rising cut.
                model.body.yRot -=
                        side
                                * 0.34F
                                * phase;

                main.xRot =
                        -0.28F
                                - 1.58F
                                        * snap;

                main.yRot =
                        -side
                                * (
                                0.64F
                                        - 0.72F
                                                * snap
                        );

                main.zRot =
                        side
                                * (
                                0.72F
                                        - 0.96F
                                                * snap
                        );

                off.xRot =
                        -0.62F;

                off.yRot =
                        side
                                * 0.18F;
            }

            default -> {
                // Diagonal shoulder-to-hip cut.
                model.body.yRot +=
                        side
                                * 0.32F
                                * phase;

                model.body.xRot +=
                        0.10F
                                * phase;

                main.xRot =
                        -2.24F
                                + 1.35F
                                        * snap;

                main.yRot =
                        side
                                * (
                                0.48F
                                        - 0.82F
                                                * snap
                        );

                main.zRot =
                        -side
                                * (
                                0.52F
                                        - 0.86F
                                                * snap
                        );

                off.xRot =
                        -0.82F;

                off.yRot =
                        -side
                                * 0.20F;
            }
        }
    }

    private static void scythe(
            PlayerModel<?> model,
            int variant,
            float phase,
            float snap,
            boolean leftMain
    ) {
        var main =
                leftMain
                        ? model.leftArm
                        : model.rightArm;

        var off =
                leftMain
                        ? model.rightArm
                        : model.leftArm;

        float side =
                leftMain
                        ? -1.0F
                        : 1.0F;

        switch (variant) {
            case 1 -> {
                // Heavy overhead hook.
                model.body.xRot +=
                        0.18F
                                * phase;

                main.xRot =
                        -2.82F
                                + 1.66F
                                        * snap;

                off.xRot =
                        -2.48F
                                + 1.42F
                                        * snap;

                main.yRot =
                        -side
                                * 0.22F;

                off.yRot =
                        side
                                * 0.36F;

                main.zRot =
                        side
                                * 0.24F;

                off.zRot =
                        -side
                                * 0.18F;
            }

            case 2 -> {
                // Low reverse reap.
                model.body.yRot -=
                        side
                                * (
                                0.58F
                                        - 1.02F
                                                * snap
                        );

                main.xRot =
                        -0.72F
                                - 0.56F
                                        * phase;

                off.xRot =
                        -0.96F
                                - 0.42F
                                        * phase;

                main.yRot =
                        -side
                                * (
                                0.92F
                                        - 1.45F
                                                * snap
                        );

                off.yRot =
                        -side
                                * (
                                0.48F
                                        - 0.92F
                                                * snap
                        );
            }

            default -> {
                // Broad two-handed horizontal harvesting arc.
                model.body.yRot +=
                        side
                                * (
                                0.84F
                                        - 1.38F
                                                * snap
                        );

                main.xRot =
                        -1.22F;

                off.xRot =
                        -1.34F;

                main.yRot =
                        side
                                * (
                                0.96F
                                        - 1.54F
                                                * snap
                        );

                off.yRot =
                        side
                                * (
                                0.52F
                                        - 1.18F
                                                * snap
                        );

                main.zRot =
                        side
                                * 0.16F;

                off.zRot =
                        -side
                                * 0.10F;
            }
        }
    }

    private static float ease(
            float value
    ) {
        float t =
                Mth.clamp(
                        value,
                        0.0F,
                        1.0F
                );

        return t
                * t
                * (
                3.0F
                        - 2.0F
                                * t
        );
    }
}
