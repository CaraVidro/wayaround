package net.caravidro.wayaround.ecology;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Persistent death container. Inventory and equipped Way Around accessories
 * move here before vanilla gets a chance to scatter them as item entities.
 */
public final class PlayerCorpseEntity extends Entity {

    private static final net.minecraft.network.syncher.EntityDataAccessor<java.util.Optional<UUID>> OWNER =
            SynchedEntityData.defineId(PlayerCorpseEntity.class, net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_UUID);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> SKELETON =
            SynchedEntityData.defineId(PlayerCorpseEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(PlayerCorpseEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> SKIN_SIGNATURE = SynchedEntityData.defineId(PlayerCorpseEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
    private int lavaTicks;
    private UUID owner;
    private String ownerName =
            "Player";

    private final List<StoredStack> contents =
            new ArrayList<>();

    public PlayerCorpseEntity(
            EntityType<? extends PlayerCorpseEntity> type,
            Level level
    ) {
        super(
                type,
                level
        );

        blocksBuilding =
                true;
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        builder.define(OWNER, java.util.Optional.empty());
        builder.define(SKELETON, false);
        builder.define(SKIN, "");builder.define(SKIN_SIGNATURE, "");
    }

    public String skinTextures() { return entityData.get(SKIN); }
    public String skinSignature() { return entityData.get(SKIN_SIGNATURE); }
    public void copySkin(com.mojang.authlib.GameProfile profile) {
        var texture=profile.getProperties().get("textures").stream().findFirst().orElse(null);
        if(texture!=null && texture.value().length()<=8192) {
            entityData.set(SKIN,texture.value());
            String signature=texture.signature();entityData.set(SKIN_SIGNATURE,signature!=null && signature.length()<=4096?signature:"");
        }
    }
    public boolean isSkeleton() { return entityData.get(SKELETON); }

    public void initialize(
            UUID owner,
            String ownerName,
            List<StoredStack> stacks
    ) {
        this.owner =
                owner;

        this.ownerName =
                ownerName == null
                        || ownerName.isBlank()
                        ? "Player"
                        : ownerName;

        entityData.set(OWNER, java.util.Optional.ofNullable(owner));
        contents.clear();

        for (StoredStack stored :
                stacks) {
            if (stored == null
                    || stored.stack() == null
                    || stored.stack().isEmpty()) {
                continue;
            }

            contents.add(
                    new StoredStack(
                            stored.slot(),
                            stored.stack()
                                    .copy()
                    )
            );
        }

        setCustomName(
                Component.literal(
                        this.ownerName
                                + "'s corpse"
                )
        );

        setCustomNameVisible(
                false
        );
    }

    public UUID owner() {
        return entityData.get(OWNER).orElse(owner);
    }

    public String ownerName() {
        return ownerName;
    }

    public int storedStackCount() {
        return contents.size();
    }

    @Override
    public void tick() {
        super.tick();

        if (isNoGravity()) {
            return;
        }

        Vec3 motion =
                getDeltaMovement();

        if (!level().isClientSide && isInLava() && !isSkeleton()) {
            if (++lavaTicks >= 200) entityData.set(SKELETON, true);
        }
        if (isInWater() || isInLava()) {
            /*
             * A body containing the player's entire inventory must not quietly
             * disappear into a trench. It rises slowly and drifts instead of
             * noclipping/sinking forever.
             */
            motion =
                    motion.multiply(
                            0.84,
                            0.72,
                            0.84
                    )
                            .add(
                                    0.0,
                                    0.018,
                                    0.0
                            );

        } else if (!onGround()) {
            motion =
                    motion.add(
                            0.0,
                            -0.055,
                            0.0
                    );
        }

        move(
                MoverType.SELF,
                motion
        );

        if (onGround()) {
            motion =
                    new Vec3(
                            motion.x * 0.28,
                            0.0,
                            motion.z * 0.28
                    );

        } else if (!isInWater()) {
            motion =
                    motion.multiply(
                            0.91,
                            0.98,
                            0.91
                    );
        }

        setDeltaMovement(
                motion
        );
    }

    @Override
    public InteractionResult interact(
            Player player,
            InteractionHand hand
    ) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (contents.isEmpty()) {
            discard();
            return InteractionResult.SUCCESS;
        }

        Iterator<StoredStack> iterator =
                contents.iterator();

        while (iterator.hasNext()) {
            StoredStack stored =
                    iterator.next();

            ItemStack moving =
                    stored.stack()
                            .copy();

            if (stored.slot() >= 0
                    && stored.slot()
                    < player.getInventory()
                            .getContainerSize()
                    && player.getInventory()
                            .getItem(
                                    stored.slot()
                            )
                            .isEmpty()) {

                player.getInventory()
                        .setItem(
                                stored.slot(),
                                moving
                        );

                iterator.remove();
                continue;
            }

            player.getInventory()
                    .add(
                            moving
                    );

            if (moving.isEmpty()) {
                iterator.remove();

            } else {
                stored.stack()
                        .setCount(
                                moving.getCount()
                        );
            }
        }

        level().playSound(
                null,
                blockPosition(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.65F,
                0.90F
        );

        if (contents.isEmpty()) {
            discard();

        } else {
            player.displayClientMessage(
                    Component.literal(
                            "Corpse ainda possui "
                                    + contents.size()
                                    + " pilha(s); libere espaço no inventário."
                    ),
                    true
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(
            CompoundTag tag
    ) {
        tag.putString("OwnerSkinTextures",skinTextures());tag.putString("OwnerSkinSignature",skinSignature());
        tag.putInt("LavaExposure", lavaTicks);
        tag.putBoolean("Skeleton", isSkeleton());
        if (owner != null) {
            tag.putUUID(
                    "Owner",
                    owner
            );
        }

        tag.putString(
                "OwnerName",
                ownerName
        );

        ListTag list =
                new ListTag();

        for (StoredStack stored :
                contents) {
            if (stored.stack()
                    .isEmpty()) {
                continue;
            }

            CompoundTag entry =
                    new CompoundTag();

            entry.putInt(
                    "Slot",
                    stored.slot()
            );

            entry.put(
                    "Stack",
                    stored.stack()
                            .save(
                                    level()
                                            .registryAccess()
                            )
            );

            list.add(
                    entry
            );
        }

        tag.put(
                "StoredItems",
                list
        );
    }

    @Override
    protected void readAdditionalSaveData(
            CompoundTag tag
    ) {
        String skin=tag.getString("OwnerSkinTextures"),signature=tag.getString("OwnerSkinSignature");
        entityData.set(SKIN,skin.length()<=8192?skin:"");entityData.set(SKIN_SIGNATURE,signature.length()<=4096?signature:"");
        lavaTicks = tag.getInt("LavaExposure");
        entityData.set(SKELETON, tag.getBoolean("Skeleton"));
        owner =
                tag.hasUUID(
                        "Owner"
                )
                        ? tag.getUUID(
                        "Owner"
                )
                        : null;

        ownerName =
                tag.getString(
                        "OwnerName"
                );

        if (ownerName.isBlank()) {
            ownerName =
                    "Player";
        }

        entityData.set(OWNER, java.util.Optional.ofNullable(owner));
        contents.clear();

        ListTag list =
                tag.getList(
                        "StoredItems",
                        Tag.TAG_COMPOUND
                );

        for (int index = 0;
             index < list.size();
             index++) {

            CompoundTag entry =
                    list.getCompound(
                            index
                    );

            ItemStack stack =
                    ItemStack.parseOptional(
                            level()
                                    .registryAccess(),
                            entry.getCompound(
                                    "Stack"
                            )
                    );

            if (!stack.isEmpty()) {
                contents.add(
                        new StoredStack(
                                entry.getInt(
                                        "Slot"
                                ),
                                stack
                        )
                );
            }
        }

        setCustomName(
                Component.literal(
                        ownerName
                                + "'s corpse"
                )
        );
    }

    @Override
    public boolean hurt(
            DamageSource source,
            float amount
    ) {
        /*
         * The corpse is the anti-item-loss container. Fire, explosions and
         * incidental combat may move the scene around, but cannot delete the
         * storage entity itself.
         */
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    public record StoredStack(
            int slot,
            ItemStack stack
    ) {
    }
}
