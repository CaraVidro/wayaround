package net.caravidro.wayaround.industrial.power;

import javax.annotation.Nullable;

import net.caravidro.wayaround.industrial.mechanical.IRotationalPower;
import net.caravidro.wayaround.industrial.mechanical.MechanicalTransmission;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SawmillBlockEntity extends BlockEntity {
    private boolean bladeInstalled;
    private boolean shaftInstalled;
    private ItemStack input = ItemStack.EMPTY;

    private float progress;
    private float rpm;
    private float bladeAngle;

    public SawmillBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.SAWMILL_ENTITY.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SawmillBlockEntity sawmill
    ) {
        sawmill.tickMachine();
    }

    private void tickMachine() {
        IRotationalPower source = findBestSource();

        float targetRpm = source == null ? 0.0F : source.rpm();
        float granted = 0.0F;

        if (source != null && bladeInstalled && shaftInstalled) {
            float idleDraw = input.isEmpty() ? 0.10F : 1.8F + Math.min(1.4F, Math.abs(targetRpm) * 0.025F);
            granted = source.consumePower(idleDraw);
        }

        boolean spinning = source != null
                && bladeInstalled
                && shaftInstalled
                && granted > 0.04F
                && Math.abs(targetRpm) > 0.5F;

        float effectiveTarget = spinning ? targetRpm : 0.0F;
        rpm += (effectiveTarget - rpm) * 0.30F;
        if (Math.abs(rpm) < 0.01F && Math.abs(effectiveTarget) < 0.01F) rpm = 0.0F;

        bladeAngle = wrap(bladeAngle + rpm * 0.30F);

        if (spinning && !input.isEmpty()) {
            float speed = Mth.clamp(Math.abs(rpm), 0.0F, 60.0F);
            progress += 0.0045F + speed * 0.00042F;

            if (level instanceof ServerLevel server
                    && Math.floorMod(server.getGameTime() + worldPosition.asLong(), 13) == 0) {
                server.playSound(
                        null,
                        worldPosition,
                        SoundEvents.GRINDSTONE_USE,
                        SoundSource.BLOCKS,
                        0.32F,
                        0.82F + Math.min(0.35F, speed / 180.0F)
                );
            }

            if (progress >= 1.0F) {
                finishCut();
            }
        }

        if (level != null
                && Math.floorMod(level.getGameTime() + worldPosition.asLong(), 4) == 0
                && (Math.abs(rpm) > 0.01F || progress > 0.0F)) {
            sync();
        }
    }

    @Nullable
    private IRotationalPower findBestSource() {
        if (level == null) return null;

        IRotationalPower best = null;
        float bestPower = -1.0F;

        for (Direction direction : Direction.values()) {
            IRotationalPower source = MechanicalTransmission.findSource(level, worldPosition, direction);
            if (source != null && source.power() > bestPower) {
                bestPower = source.power();
                best = source;
            }
        }

        return best;
    }

    public void installBlade(Player player, ItemStack stack) {
        if (bladeInstalled) {
            player.displayClientMessage(Component.translatable("message.wayaround.sawmill.blade_present"), true);
            return;
        }

        bladeInstalled = true;
        if (!player.getAbilities().instabuild) stack.consume(1, player);
        playAssemblySound(SoundEvents.ANVIL_PLACE);
        sync();

        player.displayClientMessage(
                Component.translatable("message.wayaround.sawmill.blade_installed"),
                true
        );
    }

    public void installShaft(Player player, ItemStack stack) {
        if (!bladeInstalled) {
            player.displayClientMessage(Component.translatable("message.wayaround.sawmill.need_blade"), true);
            return;
        }
        if (shaftInstalled) {
            player.displayClientMessage(Component.translatable("message.wayaround.sawmill.shaft_present"), true);
            return;
        }

        shaftInstalled = true;
        if (!player.getAbilities().instabuild) stack.consume(1, player);
        playAssemblySound(SoundEvents.ANVIL_PLACE);
        sync();

        player.displayClientMessage(
                Component.translatable("message.wayaround.sawmill.shaft_installed"),
                true
        );
    }

    public void insertLog(Player player, ItemStack stack) {
        if (!bladeInstalled || !shaftInstalled) {
            player.displayClientMessage(Component.translatable("message.wayaround.sawmill.incomplete"), true);
            return;
        }

        if (!input.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.wayaround.sawmill.busy"), true);
            return;
        }

        if (!stack.is(ItemTags.LOGS)) return;

        input = stack.copyWithCount(1);
        progress = 0.0F;

        if (!player.getAbilities().instabuild) stack.consume(1, player);

        sync();
        player.displayClientMessage(
                Component.translatable("message.wayaround.sawmill.log_inserted"),
                true
        );
    }

    public void removeLast(Player player) {
        if (!input.isEmpty()) {
            giveOrDrop(player, input.copy());
            input = ItemStack.EMPTY;
            progress = 0.0F;
            sync();
            return;
        }

        if (shaftInstalled) {
            shaftInstalled = false;
            giveOrDrop(player, new ItemStack(PowerContent.MECHANICAL_SHAFT_ITEM.get()));
            sync();
            return;
        }

        if (bladeInstalled) {
            bladeInstalled = false;
            giveOrDrop(player, new ItemStack(PowerContent.SAW_BLADE.get()));
            sync();
            return;
        }

        describe(player);
    }

    public void describe(Player player) {
        String stage;
        if (!bladeInstalled) stage = Component.translatable("message.wayaround.sawmill.stage_body").getString();
        else if (!shaftInstalled) stage = Component.translatable("message.wayaround.sawmill.stage_blade").getString();
        else stage = Component.translatable("message.wayaround.sawmill.stage_ready").getString();

        player.displayClientMessage(
                Component.translatable(
                        "message.wayaround.sawmill.status",
                        stage,
                        String.format(java.util.Locale.ROOT, "%.1f", rpm),
                        Math.round(progress * 100.0F)
                ),
                true
        );
    }

    private void finishCut() {
        if (!(level instanceof ServerLevel server) || input.isEmpty()) return;

        Item outputItem = matchingPlanks(input);
        int count = outputItem == Items.OAK_PLANKS && matchingPath(input) == null ? 4 : 6;

        Block.popResource(
                server,
                worldPosition.above(),
                new ItemStack(outputItem, count)
        );

        server.playSound(
                null,
                worldPosition,
                SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS,
                0.65F,
                1.15F
        );

        input = ItemStack.EMPTY;
        progress = 0.0F;
        sync();
    }

    private Item matchingPlanks(ItemStack stack) {
        String path = matchingPath(stack);
        if (path == null) return Items.OAK_PLANKS;

        ResourceLocation source = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(source.getNamespace(), path);
        Item item = BuiltInRegistries.ITEM.get(id);

        return item == Items.AIR ? Items.OAK_PLANKS : item;
    }

    @Nullable
    private String matchingPath(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return null;

        String path = id.getPath();
        if (path.endsWith("_log")) return path.substring(0, path.length() - 4) + "_planks";
        if (path.endsWith("_wood")) return path.substring(0, path.length() - 5) + "_planks";
        if (path.endsWith("_stem")) return path.substring(0, path.length() - 5) + "_planks";
        if (path.endsWith("_hyphae")) return path.substring(0, path.length() - 7) + "_planks";
        return null;
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack) && level != null) {
            Block.popResource(level, worldPosition, stack);
        }
    }

    private void playAssemblySound(net.minecraft.sounds.SoundEvent sound) {
        if (level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.7F, 1.0F);
        }
    }

    public void dropContents() {
        if (level == null) return;

        if (!input.isEmpty()) Block.popResource(level, worldPosition, input.copy());
        if (shaftInstalled) Block.popResource(level, worldPosition, new ItemStack(PowerContent.MECHANICAL_SHAFT_ITEM.get()));
        if (bladeInstalled) Block.popResource(level, worldPosition, new ItemStack(PowerContent.SAW_BLADE.get()));

        input = ItemStack.EMPTY;
        shaftInstalled = false;
        bladeInstalled = false;
    }

    public boolean bladeInstalled() {
        return bladeInstalled;
    }

    public boolean shaftInstalled() {
        return shaftInstalled;
    }

    public boolean hasInput() {
        return !input.isEmpty();
    }

    public float progress() {
        return progress;
    }

    public float rpm() {
        return rpm;
    }

    public float bladeAngle() {
        return bladeAngle;
    }

    private static float wrap(float value) {
        value %= 360.0F;
        if (value < 0.0F) value += 360.0F;
        return value;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("BladeInstalled", bladeInstalled);
        tag.putBoolean("ShaftInstalled", shaftInstalled);
        tag.putFloat("Progress", progress);
        tag.putFloat("Rpm", rpm);
        tag.putFloat("BladeAngle", bladeAngle);
        if (!input.isEmpty()) tag.put("Input", input.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bladeInstalled = tag.getBoolean("BladeInstalled");
        shaftInstalled = tag.getBoolean("ShaftInstalled");
        progress = Mth.clamp(tag.getFloat("Progress"), 0.0F, 1.0F);
        rpm = tag.getFloat("Rpm");
        bladeAngle = tag.getFloat("BladeAngle");
        input = tag.contains("Input", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound("Input"))
                : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
