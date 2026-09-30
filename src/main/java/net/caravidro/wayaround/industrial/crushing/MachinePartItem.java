package net.caravidro.wayaround.industrial.crushing;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class MachinePartItem extends Item {
    private final MachinePartSpec spec;
    public MachinePartItem(MachinePartSpec spec, Properties properties) { super(properties); this.spec = spec; }
    public MachinePartSpec spec() { return spec; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.wayaround.machine_part." + spec.role().name().toLowerCase(java.util.Locale.ROOT)));
        tooltip.add(Component.translatable("tooltip.wayaround.machine_part." + spec.id()));
    }
}
