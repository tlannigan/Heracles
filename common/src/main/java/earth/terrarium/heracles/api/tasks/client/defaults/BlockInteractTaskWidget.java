package earth.terrarium.heracles.api.tasks.client.defaults;

import com.teamresourceful.resourcefullib.client.scissor.ScissorBoxStack;
import earth.terrarium.heracles.api.client.DisplayWidget;
import earth.terrarium.heracles.api.client.WidgetUtils;
import earth.terrarium.heracles.api.tasks.QuestTaskDisplayFormatter;
import earth.terrarium.heracles.api.tasks.client.display.TaskTitleFormatter;
import earth.terrarium.heracles.api.tasks.defaults.BlockInteractTask;
import earth.terrarium.heracles.common.handlers.progress.TaskProgress;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record BlockInteractTaskWidget(
    BlockInteractTask task, TaskProgress<ByteTag> progress, List<ItemStack> stacks
) implements DisplayWidget {

    private static final String DESC = "task.heracles.block_interaction.desc.singular";

    public BlockInteractTaskWidget(BlockInteractTask task, TaskProgress<ByteTag> progress) {
        this(task, progress, task.block().getValue().map(
            block -> List.of(block.asItem().getDefaultInstance()),
            tag -> ModUtils.getValue(Registries.BLOCK, tag).stream().map(ItemStack::new).toList()
        ));
    }

    @Override
    public void render(GuiGraphics graphics, ScissorBoxStack scissor, int x, int y, int width, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        Font font = Minecraft.getInstance().font;
        WidgetUtils.drawBackground(graphics, x, y, width, getHeight(width));
        int iconSize = 32;
        this.task.icon().renderOrStack(this.getCurrentItem(), graphics, scissor, x + 5, y + 5, iconSize, mouseX, mouseY);
        graphics.fill(x + iconSize + 9, y + 5, x + iconSize + 10, y + getHeight(width) - 5, 0xFF909090);
        graphics.drawString(
            font,
            task.titleOr(TaskTitleFormatter.create(this.task)), x + iconSize + 16, y + 6, 0xFFFFFFFF,
            false
        );
        graphics.drawString(
            font,
            Component.translatable(DESC, Component.keybind("key.use")), x + iconSize + 16, y + 8 + font.lineHeight, 0xFF808080,
            false
        );
        String progress = QuestTaskDisplayFormatter.create(this.task, this.progress);
        graphics.drawString(
            font,
            progress, x + width - 5 - font.width(progress), y + 6, 0xFFFFFFFF,
            false
        );

        int height = getHeight(width);
        WidgetUtils.drawProgressBar(graphics, x + iconSize + 16, y + height - font.lineHeight - 5, x + width - 5, y + height - 6, this.task, this.progress);
    }

    private ItemStack getCurrentItem() {
        if (this.stacks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = Math.max(0, (int) ((System.currentTimeMillis() / 1000) % this.stacks.size()));
        return this.stacks.get(index);
    }

    @Override
    public int getHeight(int width) {
        return 42;
    }

}
