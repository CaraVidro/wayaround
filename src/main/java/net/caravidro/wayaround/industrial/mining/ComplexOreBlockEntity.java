package net.caravidro.wayaround.industrial.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ComplexOreBlockEntity extends BlockEntity {
    private int remaining = 64;
    private int initial = 64;

    public ComplexOreBlockEntity(BlockPos pos, BlockState state) {
        super(MiningContent.COMPLEX_ENTITY.get(), pos, state);
    }

    public ComplexOreKind kind() {
        return getBlockState().getBlock() instanceof ComplexOreBlock complex
                ? complex.kind()
                : ComplexOreKind.IRON;
    }

    public int remaining() { return remaining; }
    public int initial() { return initial; }

    public void initializeReserve(int reserve) {
        int bounded = Mth.clamp(reserve, 12, 256);
        remaining = bounded;
        initial = bounded;
        sync();
    }

    public ItemStack extract(RandomSource random, boolean mechanical) {
        if (remaining <= 0) return ItemStack.EMPTY;

        int chunk = mechanical
                ? 1 + (random.nextFloat() < 0.22F ? 1 : 0)
                : 1;

        chunk = Math.min(chunk, remaining);
        remaining -= chunk;

        int output = chunk;
        if (kind() == ComplexOreKind.COPPER && random.nextFloat() < 0.40F) {
            output++;
        }

        ItemStack result = new ItemStack(kind().drop(), output);

        if (remaining <= 0 && level instanceof ServerLevel server) {
            server.setBlock(
                    worldPosition,
                    kind().exhaustedState(worldPosition.getY()),
                    3
            );
        } else {
            sync();
        }

        return result;
    }

    public void extractByPlayer(Player player) {
        if (!(level instanceof ServerLevel server)) return;
        ItemStack mined = extract(server.random, false);
        if (mined.isEmpty()) return;

        if (!player.getInventory().add(mined)) {
            player.drop(mined, false);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Remaining", remaining);
        tag.putInt("Initial", initial);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        remaining = Math.max(0, tag.contains("Remaining") ? tag.getInt("Remaining") : 64);
        initial = Math.max(remaining, tag.contains("Initial") ? tag.getInt("Initial") : remaining);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Remaining", remaining);
        tag.putInt("Initial", initial);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
