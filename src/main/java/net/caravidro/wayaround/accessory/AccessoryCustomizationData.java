package net.caravidro.wayaround.accessory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent workshop parameters stored directly on each customizable head
 * accessory. The same compact values are mirrored while equipped.
 */
public final class AccessoryCustomizationData {

    public static final int EXTRA_GEARS =
            1;

    public static final int EXTRA_CLOCK =
            2;

    public static final int MATERIAL_COUNT =
            8;

    public static final int SIZE_STEPS =
            5;

    public static final int WOOL_COLOR_COUNT =
            16;

    private static final String MATERIAL =
            "WayAroundAccessoryMaterial";

    private static final String SIZE =
            "WayAroundAccessorySize";

    private static final String EXTRAS =
            "WayAroundAccessoryExtras";

    private static final String WOOL_COLOR =
            "WayAroundAccessoryWoolColor";

    private AccessoryCustomizationData() {
    }

    public static boolean supported(
            AccessoryKind kind
    ) {
        return kind == AccessoryKind.ENGINEER_CAP
                || kind == AccessoryKind.CUSTOM_HAT
                || kind == AccessoryKind.SOMBRERO;
    }

    public static Config read(
            ItemStack stack,
            AccessoryKind kind
    ) {
        Config defaults =
                defaults(
                        kind
                );

        if (stack == null
                || stack.isEmpty()
                || !supported(
                kind
        )) {
            return defaults;
        }

        CustomData data =
                stack.getOrDefault(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY
                );

        var tag =
                data.copyTag();

        return new Config(
                tag.contains(MATERIAL)
                        ? clampMaterial(
                        tag.getInt(
                                MATERIAL
                        )
                )
                        : defaults.material(),
                tag.contains(SIZE)
                        ? clampSize(
                        tag.getInt(
                                SIZE
                        )
                )
                        : defaults.size(),
                tag.contains(EXTRAS)
                        ? tag.getInt(
                        EXTRAS
                ) & (EXTRA_GEARS | EXTRA_CLOCK)
                        : defaults.extras(),
                tag.contains(WOOL_COLOR)
                        ? clampWoolColor(
                        tag.getInt(
                                WOOL_COLOR
                        )
                )
                        : defaults.woolColor()
        );
    }

    public static void write(
            ItemStack stack,
            AccessoryKind kind,
            Config config
    ) {
        if (stack == null
                || stack.isEmpty()
                || !supported(
                kind
        )) {
            return;
        }

        Config safe =
                sanitize(
                        kind,
                        config
                );

        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> {
                    tag.putInt(
                            MATERIAL,
                            safe.material()
                    );

                    tag.putInt(
                            SIZE,
                            safe.size()
                    );

                    tag.putInt(
                            EXTRAS,
                            safe.extras()
                    );

                    tag.putInt(
                            WOOL_COLOR,
                            safe.woolColor()
                    );
                }
        );
    }

    public static Config sanitize(
            AccessoryKind kind,
            Config config
    ) {
        Config defaults =
                defaults(
                        kind
                );

        if (config == null
                || !supported(
                kind
        )) {
            return defaults;
        }

        int extras =
                kind == AccessoryKind.ENGINEER_CAP
                        ? config.extras()
                        & (EXTRA_GEARS | EXTRA_CLOCK)
                        : 0;

        return new Config(
                clampMaterial(
                        config.material()
                ),
                clampSize(
                        config.size()
                ),
                extras,
                clampWoolColor(
                        config.woolColor()
                )
        );
    }

    public static Config defaults(
            AccessoryKind kind
    ) {
        if (kind == AccessoryKind.ENGINEER_CAP) {
            return new Config(
                    0,
                    2,
                    EXTRA_GEARS
                            | EXTRA_CLOCK,
                    4
            );
        }

        if (kind == AccessoryKind.SOMBRERO) {
            return new Config(
                    0,
                    2,
                    0,
                    4
            );
        }

        return new Config(
                0,
                2,
                0,
                14
        );
    }

    public static int clampMaterial(
            int value
    ) {
        return Math.floorMod(
                value,
                MATERIAL_COUNT
        );
    }

    public static int clampSize(
            int value
    ) {
        return Math.max(
                0,
                Math.min(
                        SIZE_STEPS - 1,
                        value
                )
        );
    }

    public static int clampWoolColor(
            int value
    ) {
        return Math.floorMod(
                value,
                WOOL_COLOR_COUNT
        );
    }

    public static String materialNameKey(
            int material
    ) {
        return "accessory.workshop.material."
                + clampMaterial(
                material
        );
    }

    public static String woolNameKey(
            int color
    ) {
        return "accessory.workshop.wool."
                + clampWoolColor(
                color
        );
    }

    public static BlockState materialState(
            int material
    ) {
        return switch (clampMaterial(material)) {
            case 1 -> Blocks.BLACK_WOOL.defaultBlockState();
            case 2 -> Blocks.WHITE_WOOL.defaultBlockState();
            case 3 -> Blocks.RED_WOOL.defaultBlockState();
            case 4 -> Blocks.BLUE_WOOL.defaultBlockState();
            case 5 -> Blocks.GREEN_WOOL.defaultBlockState();
            case 6 -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case 7 -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            default -> Blocks.BROWN_WOOL.defaultBlockState();
        };
    }

    public static BlockState woolState(
            int color
    ) {
        return switch (clampWoolColor(color)) {
            case 0 -> Blocks.WHITE_WOOL.defaultBlockState();
            case 1 -> Blocks.ORANGE_WOOL.defaultBlockState();
            case 2 -> Blocks.MAGENTA_WOOL.defaultBlockState();
            case 3 -> Blocks.LIGHT_BLUE_WOOL.defaultBlockState();
            case 4 -> Blocks.YELLOW_WOOL.defaultBlockState();
            case 5 -> Blocks.LIME_WOOL.defaultBlockState();
            case 6 -> Blocks.PINK_WOOL.defaultBlockState();
            case 7 -> Blocks.GRAY_WOOL.defaultBlockState();
            case 8 -> Blocks.LIGHT_GRAY_WOOL.defaultBlockState();
            case 9 -> Blocks.CYAN_WOOL.defaultBlockState();
            case 10 -> Blocks.PURPLE_WOOL.defaultBlockState();
            case 11 -> Blocks.BLUE_WOOL.defaultBlockState();
            case 12 -> Blocks.BROWN_WOOL.defaultBlockState();
            case 13 -> Blocks.GREEN_WOOL.defaultBlockState();
            case 14 -> Blocks.RED_WOOL.defaultBlockState();
            default -> Blocks.BLACK_WOOL.defaultBlockState();
        };
    }

    public record Config(
            int material,
            int size,
            int extras,
            int woolColor
    ) {
        public boolean gears() {
            return (extras
                    & EXTRA_GEARS)
                    != 0;
        }

        public boolean clock() {
            return (extras
                    & EXTRA_CLOCK)
                    != 0;
        }
    }
}
