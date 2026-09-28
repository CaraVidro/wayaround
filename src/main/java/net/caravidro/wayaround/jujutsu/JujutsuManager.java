package net.caravidro.wayaround.jujutsu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.advancement.WayAroundAdvancements;
import net.caravidro.wayaround.network.EnergyVisionS2CPayload;
import net.caravidro.wayaround.spectrum.SpectrumAccess;
import net.caravidro.wayaround.spectrum.SpectrumType;
import net.caravidro.wayaround.worldconfig.WorldFeature;
import net.caravidro.wayaround.worldconfig.WorldFeatureRuntime;
import net.caravidro.wayaround.worldstate.WorldEventTypes;
import net.caravidro.wayaround.worldstate.WorldStateService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Per-server Jujutsu identity + the generic runtime that interprets modular
 * techniques. The identity is generated before awakening and kept in the
 * player's persistent data, so the Orb can never be used as a reroll button.
 */
@EventBusSubscriber(modid = WayAround.MODID)
public final class JujutsuManager {
    private static final String ROOT = "WayAroundJujutsu_";
    private static final String ASSIGNED = ROOT + "Assigned";
    private static final String AWAKENED = ROOT + "Awakened";
    private static final String KIND = ROOT + "Kind";
    private static final String TECHNIQUE = ROOT + "Technique";
    private static final String SPECTRUM = ROOT + "Spectrum";

    private static final String NORMAL = "normal";
    private static final String SPECTRUM_KIND = "spectrum";

    // Overall Spectrum rate: ~0.02015%. Tukuna keeps the design-document
    // example of 0.00015%; Void and Justice are each 0.01%.
    private static final double TUKUNA_CHANCE = 0.0000015D;
    private static final double VOID_CHANCE = 0.0001000D;
    private static final double JUSTICE_CHANCE = 0.0001000D;

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();
    private static final List<ActiveTechnique> ACTIVE = new ArrayList<>();
    private static final Set<UUID> ENERGY_VISION = new HashSet<>();

    private JujutsuManager() {}

    public static void ensureAssigned(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(ASSIGNED)) return;

        SpectrumType migrated = SpectrumAccess.firstOwned(player).orElse(null);
        if (migrated != null) {
            writeSpectrum(player, migrated, true);
            return;
        }

        long seed = player.server.overworld().getSeed()
                ^ player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 23)
                ^ 0x4A554A555453554CL;
        SplittableRandom random = new SplittableRandom(seed);
        double spectrumRoll = random.nextDouble();

        if (spectrumRoll < TUKUNA_CHANCE) {
            writeSpectrum(player, SpectrumType.TUKUNA, false);
            return;
        }
        if (spectrumRoll < TUKUNA_CHANCE + VOID_CHANCE) {
            writeSpectrum(player, SpectrumType.VOID, false);
            return;
        }
        if (spectrumRoll < TUKUNA_CHANCE + VOID_CHANCE + JUSTICE_CHANCE) {
            writeSpectrum(player, SpectrumType.JUSTICE, false);
            return;
        }

        List<JujutsuTechnique> catalog = JujutsuTechnique.catalog();
        JujutsuTechnique technique = catalog.get(random.nextInt(catalog.size()));
        writeNormal(player, technique, false);
    }

    public static boolean awaken(ServerPlayer player) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.SPECTRUMS)) {
            player.displayClientMessage(
                    Component.literal("Jujutsu e Spectrums estão desativados neste mundo.")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    true
            );
            return false;
        }

        ensureAssigned(player);
        if (isAwakened(player)) return false;

        player.getPersistentData().putBoolean(AWAKENED, true);
        if (isSpectrumIdentity(player)) {
            SpectrumType type = hiddenSpectrum(player);
            if (type == null) return false;
            SpectrumAccess.replace(player, type);
            player.sendSystemMessage(Component.literal("Seu Jujutsu despertou.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            player.sendSystemMessage(Component.literal("SPECTRUM: " + spectrumDisplay(type))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            JujutsuTechnique technique = technique(player);
            if (technique == null) return false;
            player.sendSystemMessage(Component.literal("Seu Jujutsu despertou:")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            player.sendSystemMessage(Component.literal(technique.displayName())
                    .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("Use Shift + T e pressione 1 para usar sua técnica.")
                    .withStyle(ChatFormatting.GRAY));
        }

        recordAwakening(player);
        return true;
    }

    private static void recordAwakening(ServerPlayer player) {
        /*
         * Deliberately generic. A Spectrum awakening gets the exact same public
         * emblem as an ordinary Jujutsu, so advancements never spoil the roll.
         */
        WayAroundAdvancements.jujutsuAwakened(
                player
        );

        CompoundTag history = new CompoundTag();

        history.putString(
                "playerName",
                player.getGameProfile().getName()
        );

        if (isSpectrumIdentity(player)) {
            SpectrumType type = hiddenSpectrum(player);

            history.putString(
                    "kind",
                    SPECTRUM_KIND
            );

            history.putString(
                    "spectrum",
                    type == null
                            ? "unknown"
                            : type.path()
            );
        } else {
            JujutsuTechnique technique = technique(player);

            history.putString(
                    "kind",
                    NORMAL
            );

            if (technique != null) {
                history.putString(
                        "technique",
                        technique.id()
                );

                history.putString(
                        "displayName",
                        technique.displayName()
                );
            }
        }

        WorldStateService.record(
                player.serverLevel(),
                WorldEventTypes.JUJUTSU_AWAKENED,
                player.blockPosition(),
                player.getUUID(),
                history
        );
    }

    public static void forceTechnique(ServerPlayer player, JujutsuTechnique technique) {
        cleanupOwnedRuntime(player.getUUID());
        ENERGY_VISION.remove(player.getUUID());
        SpectrumAccess.clearAll(player);
        writeNormal(player, technique, true);
        player.sendSystemMessage(Component.literal("Jujutsu de teste aplicado: " + technique.displayName())
                .withStyle(ChatFormatting.AQUA));
    }

    public static boolean attuneSpectrumFromItem(
            ServerPlayer player,
            SpectrumType type
    ) {
        if (isAwakened(
                player
        )) {
            return false;
        }

        cleanupOwnedRuntime(
                player.getUUID()
        );

        ENERGY_VISION.remove(
                player.getUUID()
        );

        writeSpectrum(
                player,
                type,
                true
        );

        SpectrumAccess.replace(
                player,
                type
        );

        player.sendSystemMessage(
                Component.literal(
                        "O Spectrum reagiu a você antes da Orb."
                ).withStyle(
                        ChatFormatting.DARK_PURPLE
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "SPECTRUM: "
                                + spectrumDisplay(
                                type
                        )
                ).withStyle(
                        ChatFormatting.LIGHT_PURPLE
                )
        );

        recordAwakening(
                player
        );

        return true;
    }

    public static void forceSpectrum(ServerPlayer player, SpectrumType type) {
        cleanupOwnedRuntime(player.getUUID());
        ENERGY_VISION.remove(player.getUUID());
        writeSpectrum(player, type, true);
        SpectrumAccess.replace(player, type);
        player.sendSystemMessage(Component.literal("Spectrum de teste aplicado: " + spectrumDisplay(type))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static boolean isAwakened(Player player) {
        return player.getPersistentData().getBoolean(AWAKENED);
    }

    public static boolean isAssigned(Player player) {
        return player.getPersistentData().getBoolean(ASSIGNED);
    }

    public static boolean isSpectrumIdentity(Player player) {
        return SPECTRUM_KIND.equals(player.getPersistentData().getString(KIND));
    }

    public static JujutsuTechnique technique(Player player) {
        if (!NORMAL.equals(player.getPersistentData().getString(KIND))) return null;
        return JujutsuTechnique.find(player.getPersistentData().getString(TECHNIQUE)).orElse(null);
    }

    public static SpectrumType hiddenSpectrum(Player player) {
        if (!isSpectrumIdentity(player)) return null;
        String id = player.getPersistentData().getString(SPECTRUM);
        for (SpectrumType type : SpectrumType.values()) {
            if (type.path().equals(id)) return type;
        }
        return null;
    }

    public static String publicStatus(ServerPlayer player) {
        ensureAssigned(player);
        if (!isAwakened(player)) return "Jujutsu adormecido.";
        if (isSpectrumIdentity(player)) {
            SpectrumType type = hiddenSpectrum(player);
            return type == null ? "Spectrum desconhecido." : "Spectrum: " + spectrumDisplay(type);
        }
        JujutsuTechnique technique = technique(player);
        return technique == null ? "Jujutsu inválido." : technique.displayName() + " [" + technique.id() + "]";
    }

    public static void cast(ServerPlayer player) {
        if (!WorldFeatureRuntime.serverEnabled(WorldFeature.SPECTRUMS)) return;
        ensureAssigned(player);

        if (!isAwakened(player)) {
            player.displayClientMessage(
                    Component.literal("Alguma coisa está adormecida.")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    true
            );
            return;
        }
        if (isSpectrumIdentity(player)) {
            player.displayClientMessage(
                    Component.literal("Seu poder é um Spectrum. Use o menu de Spectrum.")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true
            );
            return;
        }

        JujutsuTechnique technique = technique(player);
        if (technique == null) return;

        long now = player.server.getTickCount();
        if (now < COOLDOWN_UNTIL.getOrDefault(player.getUUID(), 0L)) return;
        COOLDOWN_UNTIL.put(player.getUUID(), now + 26L);

        ServerLevel level = player.serverLevel();
        Vec3 origin = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 target = origin.add(look.scale(6.0D));

        switch (technique.form()) {
            case SUMMON -> summon(player, technique, target, now);
            case PROJECTILE -> {
                ActiveTechnique active = new ActiveTechnique(
                        player.getUUID(), level, technique,
                        origin.add(look.scale(1.0D)),
                        look.scale(0.72D), now, now + 120L
                );
                ACTIVE.add(active);
                burst(level, active.position, technique, 18);
            }
            case FIELD -> {
                ActiveTechnique active = new ActiveTechnique(
                        player.getUUID(), level, technique,
                        target, Vec3.ZERO, now, now + 120L
                );
                ACTIVE.add(active);
                burst(level, target, technique, 32);
            }
            case AURA -> {
                ActiveTechnique active = new ActiveTechnique(
                        player.getUUID(), level, technique,
                        player.position().add(0, 1.0D, 0), Vec3.ZERO, now, now + 100L
                );
                ACTIVE.add(active);
                burst(level, active.position, technique, 24);
            }
            case BEAM -> castBeam(player, technique, origin, look);
        }

        player.displayClientMessage(
                Component.literal(technique.displayName()).withStyle(ChatFormatting.AQUA),
                true
        );
    }

    public static void toggleEnergyVision(ServerPlayer player) {
        ensureAssigned(player);
        if (!isAwakened(player) || !SpectrumAccess.hasAny(player)) return;

        if (ENERGY_VISION.remove(player.getUUID())) {
            PacketDistributor.sendToPlayer(player, new EnergyVisionS2CPayload(false, List.of()));
            player.displayClientMessage(Component.literal("Visão de energia: encerrada.")
                    .withStyle(ChatFormatting.DARK_GRAY), true);
        } else {
            ENERGY_VISION.add(player.getUUID());
            sendEnergySnapshot(player);
            player.displayClientMessage(Component.literal("Visão de energia.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    private static void sendEnergySnapshot(ServerPlayer viewer) {
        List<EnergyVisionS2CPayload.Entry> signatures = new ArrayList<>();
        for (ServerPlayer target : viewer.server.getPlayerList().getPlayers()) {
            ensureAssigned(target);
            byte kind = isSpectrumIdentity(target)
                    ? EnergyVisionS2CPayload.SPECTRUM
                    : EnergyVisionS2CPayload.NORMAL_JUJUTSU;
            signatures.add(new EnergyVisionS2CPayload.Entry(target.getUUID(), kind));
        }
        PacketDistributor.sendToPlayer(viewer, new EnergyVisionS2CPayload(true, List.copyOf(signatures)));
    }

    private static void summon(
            ServerPlayer owner,
            JujutsuTechnique technique,
            Vec3 target,
            long now
    ) {
        ServerLevel level = owner.serverLevel();
        BlockPos center = BlockPos.containing(target);
        if (!level.getBlockState(center).canBeReplaced()) center = center.above();

        ActiveTechnique active = new ActiveTechnique(
                owner.getUUID(), level, technique,
                Vec3.atCenterOf(center), Vec3.ZERO, now, now + 140L
        );

        BlockPos[] positions = {
                center,
                center.offset(1, 0, 0),
                center.offset(-1, 0, 1)
        };

        BlockState summoned = summonedBlock(technique.element());
        for (BlockPos pos : positions) {
            BlockState old = level.getBlockState(pos);
            if (!old.canBeReplaced()) continue;
            if (level.setBlock(pos, summoned, 3)) {
                active.blocks.add(new SummonedBlock(pos.immutable(), old, summoned));
            }
        }

        ACTIVE.add(active);
        burst(level, active.position, technique, 30);
    }

    private static void castBeam(
            ServerPlayer owner,
            JujutsuTechnique technique,
            Vec3 origin,
            Vec3 look
    ) {
        ServerLevel level = owner.serverLevel();
        Vec3 end = origin.add(look.scale(20.0D));
        LivingEntity hit = null;
        Vec3 hitPos = end;

        for (double distance = 0.8D; distance <= 20.0D; distance += 0.45D) {
            Vec3 point = origin.add(look.scale(distance));
            level.sendParticles(particle(technique.element()),
                    point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.002);

            if (!level.getBlockState(BlockPos.containing(point)).isAir()) {
                hitPos = point;
                break;
            }

            List<LivingEntity> nearby = entities(level, point, 0.55D, owner.getUUID());
            if (!nearby.isEmpty()) {
                hit = nearby.getFirst();
                hitPos = hit.position().add(0, hit.getBbHeight() * 0.5D, 0);
                break;
            }
        }

        applyEffect(owner, technique, hitPos, hit == null ? List.of() : List.of(hit));
    }

    private static void tickActive(long now) {
        ACTIVE.removeIf(active -> {
            ServerPlayer owner = active.level.getServer().getPlayerList().getPlayer(active.owner);
            if (owner == null || !owner.isAlive() || active.level != owner.serverLevel()) {
                cleanup(active);
                return true;
            }

            if (active.technique.form() == JujutsuTechnique.Form.PROJECTILE) {
                active.position = active.position.add(active.velocity);
                BlockPos block = BlockPos.containing(active.position);
                if (!active.level.getBlockState(block).isAir()) {
                    trigger(active, owner);
                    return true;
                }
            } else if (active.technique.form() == JujutsuTechnique.Form.AURA) {
                active.position = owner.position().add(0, 1.0D, 0);
            }

            trail(active);

            boolean shouldTrigger = switch (active.technique.trigger()) {
                case TOUCH -> !touching(active, owner).isEmpty();
                case PROXIMITY -> !entities(active.level, active.position, 3.4D, owner.getUUID()).isEmpty();
                case DELAY -> now - active.created >= 30L;
                case IMPACT -> false;
            };

            if (shouldTrigger) {
                trigger(active, owner);
                return true;
            }

            if (now >= active.expires) {
                cleanup(active);
                return true;
            }
            return false;
        });
    }

    private static List<LivingEntity> touching(ActiveTechnique active, ServerPlayer owner) {
        if (active.blocks.isEmpty()) {
            return entities(active.level, active.position, 1.15D, owner.getUUID());
        }

        List<LivingEntity> result = new ArrayList<>();
        for (SummonedBlock block : active.blocks) {
            Vec3 center = Vec3.atCenterOf(block.pos);
            for (LivingEntity entity : entities(active.level, center, 1.1D, owner.getUUID())) {
                if (!result.contains(entity)) result.add(entity);
            }
        }
        return result;
    }

    private static void trigger(ActiveTechnique active, ServerPlayer owner) {
        List<LivingEntity> targets = entities(active.level, active.position, 4.5D, owner.getUUID());
        applyEffect(owner, active.technique, active.position, targets);
        cleanup(active);
    }

    private static void applyEffect(
            ServerPlayer owner,
            JujutsuTechnique technique,
            Vec3 position,
            List<LivingEntity> suppliedTargets
    ) {
        ServerLevel level = owner.serverLevel();
        List<LivingEntity> targets = suppliedTargets.isEmpty()
                ? entities(level, position, 4.5D, owner.getUUID())
                : suppliedTargets;

        switch (technique.effect()) {
            case EXPLOSION -> level.explode(
                    owner,
                    position.x, position.y, position.z,
                    3.4F,
                    Level.ExplosionInteraction.TNT
            );
            case CUT -> targets.forEach(target ->
                    target.hurt(level.damageSources().playerAttack(owner), 9.0F));
            case PUSH -> targets.forEach(target -> {
                Vec3 away = target.position().subtract(position);
                if (away.lengthSqr() < 0.001D) away = new Vec3(0, 1, 0);
                target.setDeltaMovement(target.getDeltaMovement()
                        .add(away.normalize().scale(1.35D))
                        .add(0, 0.28D, 0));
                target.hurtMarked = true;
            });
            case PULL -> targets.forEach(target -> {
                Vec3 toward = position.subtract(target.position());
                if (toward.lengthSqr() < 0.001D) return;
                target.setDeltaMovement(target.getDeltaMovement()
                        .add(toward.normalize().scale(1.15D)));
                target.hurtMarked = true;
            });
            case BIND -> targets.forEach(target ->
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3)));
        }

        for (LivingEntity target : targets) {
            applyElement(owner, technique.element(), target, position);
        }

        burst(level, position, technique, technique.effect() == JujutsuTechnique.Effect.EXPLOSION ? 55 : 28);
    }

    private static void applyElement(
            ServerPlayer owner,
            JujutsuTechnique.Element element,
            LivingEntity target,
            Vec3 source
    ) {
        ServerLevel level = owner.serverLevel();
        switch (element) {
            case ICE -> {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), 160));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 1));
            }
            case FIRE -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
            case LIGHTNING -> target.hurt(level.damageSources().playerAttack(owner), 4.0F);
            case STONE -> target.hurt(level.damageSources().playerAttack(owner), 2.0F);
            case WIND -> {
                Vec3 away = target.position().subtract(source);
                if (away.lengthSqr() > 0.001D) {
                    target.setDeltaMovement(target.getDeltaMovement()
                            .add(away.normalize().scale(0.55D))
                            .add(0, 0.22D, 0));
                    target.hurtMarked = true;
                }
            }
            case SHADOW -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 55, 0));
        }
    }

    private static List<LivingEntity> entities(
            ServerLevel level,
            Vec3 center,
            double radius,
            UUID excluded
    ) {
        AABB box = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );
        return level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive() && !entity.getUUID().equals(excluded)
        );
    }

    private static void trail(ActiveTechnique active) {
        active.level.sendParticles(
                particle(active.technique.element()),
                active.position.x, active.position.y, active.position.z,
                active.technique.form() == JujutsuTechnique.Form.PROJECTILE ? 5 : 2,
                0.18, 0.18, 0.18, 0.015
        );
    }

    private static void burst(
            ServerLevel level,
            Vec3 position,
            JujutsuTechnique technique,
            int count
    ) {
        level.sendParticles(
                particle(technique.element()),
                position.x, position.y, position.z,
                count, 0.55, 0.55, 0.55, 0.045
        );
    }

    private static ParticleOptions particle(JujutsuTechnique.Element element) {
        return switch (element) {
            case ICE -> ParticleTypes.SNOWFLAKE;
            case FIRE -> ParticleTypes.FLAME;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case STONE -> ParticleTypes.ASH;
            case WIND -> ParticleTypes.CLOUD;
            case SHADOW -> ParticleTypes.PORTAL;
        };
    }

    private static BlockState summonedBlock(JujutsuTechnique.Element element) {
        return switch (element) {
            case ICE -> Blocks.PACKED_ICE.defaultBlockState();
            case FIRE -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case LIGHTNING -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            case STONE -> Blocks.STONE.defaultBlockState();
            case WIND -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            case SHADOW -> Blocks.BLACK_CONCRETE.defaultBlockState();
        };
    }

    private static void cleanup(ActiveTechnique active) {
        for (SummonedBlock block : active.blocks) {
            if (active.level.getBlockState(block.pos).equals(block.summoned)) {
                active.level.setBlock(block.pos, block.previous, 3);
            }
        }
        active.blocks.clear();
    }

    private static void cleanupOwnedRuntime(UUID owner) {
        ACTIVE.removeIf(active -> {
            if (!active.owner.equals(owner)) return false;
            cleanup(active);
            return true;
        });
        COOLDOWN_UNTIL.remove(owner);
    }

    private static void writeNormal(
            ServerPlayer player,
            JujutsuTechnique technique,
            boolean awakened
    ) {
        player.getPersistentData().putBoolean(ASSIGNED, true);
        player.getPersistentData().putBoolean(AWAKENED, awakened);
        player.getPersistentData().putString(KIND, NORMAL);
        player.getPersistentData().putString(TECHNIQUE, technique.id());
        player.getPersistentData().remove(SPECTRUM);
    }

    private static void writeSpectrum(
            ServerPlayer player,
            SpectrumType type,
            boolean awakened
    ) {
        player.getPersistentData().putBoolean(ASSIGNED, true);
        player.getPersistentData().putBoolean(AWAKENED, awakened);
        player.getPersistentData().putString(KIND, SPECTRUM_KIND);
        player.getPersistentData().putString(SPECTRUM, type.path());
        player.getPersistentData().remove(TECHNIQUE);
    }

    private static void copyIdentity(ServerPlayer original, ServerPlayer replacement) {
        if (!original.getPersistentData().getBoolean(ASSIGNED)) return;
        replacement.getPersistentData().putBoolean(ASSIGNED, true);
        replacement.getPersistentData().putBoolean(
                AWAKENED,
                original.getPersistentData().getBoolean(AWAKENED)
        );
        replacement.getPersistentData().putString(
                KIND,
                original.getPersistentData().getString(KIND)
        );

        if (original.getPersistentData().contains(TECHNIQUE)) {
            replacement.getPersistentData().putString(
                    TECHNIQUE,
                    original.getPersistentData().getString(TECHNIQUE)
            );
        }
        if (original.getPersistentData().contains(SPECTRUM)) {
            replacement.getPersistentData().putString(
                    SPECTRUM,
                    original.getPersistentData().getString(SPECTRUM)
            );
        }
    }

    private static String spectrumDisplay(SpectrumType type) {
        return switch (type) {
            case VOID -> "Vazio";
            case TUKUNA -> "Tukuna";
            case JUSTICE -> "Justiça";
        };
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ensureAssigned(player);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer replacement) {
            copyIdentity(original, replacement);
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        tickActive(now);

        if (now % 20L == 0L) {
            ENERGY_VISION.removeIf(id -> event.getServer().getPlayerList().getPlayer(id) == null);
            for (UUID id : ENERGY_VISION) {
                ServerPlayer viewer = event.getServer().getPlayerList().getPlayer(id);
                if (viewer != null && SpectrumAccess.hasAny(viewer)) sendEnergySnapshot(viewer);
            }
        }

        if (now % 200L == 0L) {
            COOLDOWN_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        for (ActiveTechnique active : ACTIVE) cleanup(active);
        ACTIVE.clear();
        COOLDOWN_UNTIL.clear();
        ENERGY_VISION.clear();
    }

    private static final class ActiveTechnique {
        final UUID owner;
        final ServerLevel level;
        final JujutsuTechnique technique;
        final Vec3 velocity;
        final long created;
        final long expires;
        final List<SummonedBlock> blocks = new ArrayList<>();
        Vec3 position;

        ActiveTechnique(
                UUID owner,
                ServerLevel level,
                JujutsuTechnique technique,
                Vec3 position,
                Vec3 velocity,
                long created,
                long expires
        ) {
            this.owner = owner;
            this.level = level;
            this.technique = technique;
            this.position = position;
            this.velocity = velocity;
            this.created = created;
            this.expires = expires;
        }
    }

    private record SummonedBlock(BlockPos pos, BlockState previous, BlockState summoned) {}
}
