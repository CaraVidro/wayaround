package net.caravidro.wayaround.media.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;

import net.caravidro.wayaround.media.MediaContent;
import net.caravidro.wayaround.media.MediaInventory;
import net.caravidro.wayaround.network.HerobrinePhotoSpawnC2SPayload;
import net.caravidro.wayaround.network.PhotoTakenC2SPayload;
import net.caravidro.wayaround.oldfriend.client.HerobrinePhotoState;
import net.caravidro.wayaround.oldfriend.client.PhotoHerobrinePlanner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PhotoCapture {

    private PhotoCapture() {
    }

    private static boolean pending;

    private static boolean apparitionPrepared;

    private static int apparitionWarmupFrames;

    public static boolean isPending() {
        return pending;
    }

    public static void request() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {

            return;
        }

        if (MediaInventory.count(
                minecraft.player,
                MediaContent.PHOTO_PAPER.get()
        ) <= 0
                && !minecraft.player
                .getAbilities()
                .instabuild) {

            minecraft.player
                    .displayClientMessage(
                            Component.translatable(
                                    "message.wayaround.media.need_photo_paper"
                            ),
                            true
                    );

            return;
        }

        pending =
                true;

        apparitionPrepared =
                false;

        apparitionWarmupFrames =
                0;
    }

    public static void captureIfPending() {
        if (!pending) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.level == null) {

            pending =
                    false;

            return;
        }

        /*
         * In Herobrine photo mode the apparition is not composited into the
         * PNG. Pick a real visible floor position, ask the server to spawn a
         * physical Herobrine there, then let a few rendered frames pass so the
         * normal entity renderer writes him into the framebuffer.
         */
        if (HerobrinePhotoState.enabled()
                && !apparitionPrepared) {
            PhotoHerobrinePlanner.Placement placement =
                    PhotoHerobrinePlanner.choose(
                                    minecraft
                            )
                            .orElse(
                                    null
                            );

            apparitionPrepared =
                    true;

            if (placement != null) {
                PacketDistributor.sendToServer(
                        new HerobrinePhotoSpawnC2SPayload(
                                placement.feetPos()
                                        .getX(),
                                placement.feetPos()
                                        .getY(),
                                placement.feetPos()
                                        .getZ()
                        )
                );

                apparitionWarmupFrames =
                        7;

                return;
            }
        }

        if (apparitionWarmupFrames > 0) {
            apparitionWarmupFrames--;
            return;
        }

        pending =
                false;

        apparitionPrepared =
                false;

        String id =
                UUID.randomUUID()
                        .toString();

        long takenAt =
                System.currentTimeMillis();

        Vec3 position =
                minecraft.gameRenderer
                        .getMainCamera()
                        .getPosition();

        try {
            Path directory =
                    minecraft.gameDirectory
                            .toPath()
                            .resolve(
                                    "wayaround-photos"
                            );

            Files.createDirectories(
                    directory
            );

            try (
                    NativeImage image =
                            Screenshot.takeScreenshot(
                                    minecraft
                                            .getMainRenderTarget()
                            )
            ) {
                image.writeToFile(
                        directory.resolve(
                                id + ".png"
                        )
                );
            }

            PacketDistributor.sendToServer(
                    new PhotoTakenC2SPayload(
                            id,
                            takenAt,
                            (int) Math.floor(
                                    position.x
                            ),
                            (int) Math.floor(
                                    position.y
                            ),
                            (int) Math.floor(
                                    position.z
                            )
                    )
            );

            minecraft.player
                    .displayClientMessage(
                            Component.translatable(
                                            "message.wayaround.media.photo_taken"
                                    )
                                    .withStyle(
                                            ChatFormatting.GREEN
                                    ),
                            true
                    );

        } catch (Exception exception) {
            minecraft.player
                    .displayClientMessage(
                            Component.translatable(
                                            "message.wayaround.media.photo_failed",
                                            exception.getMessage()
                                    )
                                    .withStyle(
                                            ChatFormatting.RED
                                    ),
                            true
                    );
        }
    }
}
