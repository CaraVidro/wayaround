package net.caravidro.wayaround.content;

import net.caravidro.wayaround.content.item.VoidSpectrumItem;

import net.caravidro.wayaround.WayAround;
import net.caravidro.wayaround.block.PrioriteBlock;
import net.caravidro.wayaround.content.item.BlueItem;
import net.caravidro.wayaround.content.item.PrioriteBottleItem;
import net.caravidro.wayaround.content.item.PrioriteBucketItem;
import net.caravidro.wayaround.content.item.TukunaFingerItem;
import net.caravidro.wayaround.content.item.TukunaSpectrumItem;
import net.caravidro.wayaround.content.item.JusticeSpectrumItem;
import net.caravidro.wayaround.content.item.JusticeExecutionBladeItem;
import net.caravidro.wayaround.content.item.JujutsuOrbItem;
import net.caravidro.wayaround.cursed.DesmartelarSlashEntity;
import net.caravidro.wayaround.cursed.ImmortalWheelRemnantEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WayAroundContent {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WayAround.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(WayAround.MODID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    WayAround.MODID
            );

    public static final DeferredItem<net.caravidro.wayaround.observation.EntitySpectateItem> ENTITY_SPECTATE=ITEMS.register("entity_spectate",()->new net.caravidro.wayaround.observation.EntitySpectateItem(new Item.Properties().stacksTo(1)));

    /*
     * =========================================================
     * BLOCO
     * =========================================================
     */

    public static final DeferredBlock<net.caravidro.wayaround.war.DebugTurretBlock> DEBUG_TURRET =
            BLOCKS.register("debug_turret", () -> new net.caravidro.wayaround.war.DebugTurretBlock(
                    BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> DEBUG_TURRET_ITEM =
            ITEMS.register("debug_turret", () -> new BlockItem(DEBUG_TURRET.get(), new Item.Properties()));

    public static final DeferredBlock<Block> PRIORITE =
            BLOCKS.register(
                    "priorite",
                    () -> new PrioriteBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_YELLOW)
                                    .strength(100.0F)
                                    .sound(SoundType.HONEY_BLOCK)
                                    .lightLevel(state -> 4)
                                    .randomTicks()
                                    .noOcclusion()
                    )
            );

    /*
     * BlockItem de debug / criativo.
     * Depois, se quiser, pode remover.
     */
    public static final DeferredItem<Item> PRIORITE_BLOCK_ITEM =
            ITEMS.register(
                    "priorite",
                    () -> new BlockItem(
                            PRIORITE.get(),
                            new Item.Properties()
                    )
            );

    /*
     * =========================================================
     * ITENS
     * =========================================================
     */

    public static final DeferredItem<VoidSpectrumItem> GOJO_SPECTRUM =
            ITEMS.register(
                    "gojo_spectrum",
                    () -> new VoidSpectrumItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)
                    )
            );

    public static final DeferredItem<Item> IMMORTAL_WHEEL =
            ITEMS.register(
                    "immortal_wheel",
                    () -> new Item(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .fireResistant()
                                    .rarity(
                                            net.minecraft.world.item.Rarity.EPIC
                                    )
                    )
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<ImmortalWheelRemnantEntity>
            > IMMORTAL_WHEEL_REMNANT =
            ENTITY_TYPES.register(
                    "immortal_wheel_remnant",
                    () -> EntityType.Builder
                            .of(
                                    ImmortalWheelRemnantEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    1.55F,
                                    0.34F
                            )
                            .clientTrackingRange(
                                    12
                            )
                            .updateInterval(
                                    1
                            )
                            .fireImmune()
                            .build(
                                    "wayaround:immortal_wheel_remnant"
                            )
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<DesmartelarSlashEntity>
            > DESMARTELAR_SLASH =
            ENTITY_TYPES.register(
                    "desmartelar_slash",
                    () -> EntityType.Builder
                            .of(
                                    DesmartelarSlashEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.2F,
                                    0.2F
                            )
                            .clientTrackingRange(
                                    12
                            )
                            .updateInterval(
                                    1
                            )
                            .build(
                                    "wayaround:desmartelar_slash"
                            )
            );

    public static final DeferredItem<TukunaSpectrumItem> TUKUNA_SPECTRUM =
            ITEMS.register(
                    "tukuna_spectrum",
                    () -> new TukunaSpectrumItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(
                                            net.minecraft.world.item.Rarity.EPIC
                                    )
                    )
            );

    public static final DeferredItem<TukunaFingerItem> TUKUNA_FINGER =
            ITEMS.register(
                    "tukuna_finger",
                    () -> new TukunaFingerItem(
                            new Item.Properties()
                                    .stacksTo(20)
                                    .fireResistant()
                                    .rarity(
                                            net.minecraft.world.item.Rarity.RARE
                                    )
                    )
            );

    public static final DeferredItem<JujutsuOrbItem> JUJUTSU_ORB =
            ITEMS.register(
                    "jujutsu_orb",
                    () -> new JujutsuOrbItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(net.minecraft.world.item.Rarity.RARE)
                    )
            );

    public static final DeferredItem<JusticeSpectrumItem> JUSTICE_SPECTRUM =
            ITEMS.register(
                    "justice_spectrum",
                    () -> new JusticeSpectrumItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(
                                            net.minecraft.world.item.Rarity.EPIC
                                    )
                    )
            );

    public static final DeferredItem<JusticeExecutionBladeItem> JUSTICE_EXECUTION_BLADE =
            ITEMS.register(
                    "justice_execution_blade",
                    () -> new JusticeExecutionBladeItem(
                            Tiers.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .attributes(
                                            SwordItem.createAttributes(
                                                    Tiers.DIAMOND,
                                                    7,
                                                    -2.2F
                                            )
                                    )
                    )
            );

    public static final DeferredItem<Item> BLUE =
            ITEMS.register(
                    "blue",
                    () -> new BlueItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> PRIORITE_BUCKET =
            ITEMS.register(
                    "priorite_bucket",
                    () -> new PrioriteBucketItem(
                            new Item.Properties()
                                    .stacksTo(1)
                    )
            );

    public static final DeferredItem<Item> PRIORITE_BOTTLE =
            ITEMS.register(
                    "priorite_bottle",
                    () -> new PrioriteBottleItem(
                            new Item.Properties()
                                    .stacksTo(16)
                    )
            );

    private WayAroundContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
    }
}
