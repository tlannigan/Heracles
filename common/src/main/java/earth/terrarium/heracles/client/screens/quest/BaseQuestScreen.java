package earth.terrarium.heracles.client.screens.quest;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.screens.AbstractQuestScreen;
import earth.terrarium.heracles.client.screens.quest.rewards.RewardListWidget;
import earth.terrarium.heracles.client.widgets.SelectableTabButton;
import earth.terrarium.heracles.client.widgets.base.TemporyWidget;
import earth.terrarium.heracles.common.menus.quest.QuestMenu;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.groups.OpenGroupPacket;
import earth.terrarium.heracles.common.network.packets.rewards.ClaimRewardsPacket;
import earth.terrarium.hermes.api.TagProvider;
import earth.terrarium.hermes.api.themes.DefaultTheme;
import earth.terrarium.hermes.client.DocumentWidget;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseQuestScreen extends AbstractQuestScreen<QuestMenu> {

    private SelectableTabButton overview;
    private SelectableTabButton tasks;
    private SelectableTabButton rewards;
    private Button claimRewards;

    private RewardListWidget rewardList;
    private DocumentWidget description;
    private String descriptionError;

    public BaseQuestScreen(QuestMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, Optionull.mapOrDefault(menu.quest(), quest -> quest.display().title(), component));
        ClientQuests.updateProgress(Map.of(menu.id(), menu.progress()));
    }

    @Override
    protected void init() {
        super.init();
        int sidebarWidth = (int) (this.width * 0.25f) - 2;
        int buttonWidth = sidebarWidth - 10;

        this.overview = addRenderableWidget(new SelectableTabButton(5, 20, buttonWidth, 20, Component.literal("Overview"), () -> {
            clearSelected();
            this.overview.setSelected(true);
        }));

        this.tasks = addRenderableWidget(new SelectableTabButton(5, 45, buttonWidth, 20, Component.literal("Tasks"), () -> {
            clearSelected();
            this.tasks.setSelected(true);
        }));

        this.rewards = addRenderableWidget(new SelectableTabButton(5, 70, buttonWidth, 20, Component.literal("Rewards"), () -> {
            clearSelected();
            this.rewards.setSelected(true);
        }));

        this.claimRewards = addRenderableWidget(Button.builder(Component.literal("Claim Rewards"), button -> {
            NetworkHandler.CHANNEL.sendToServer(new ClaimRewardsPacket(this.menu.id()));
            this.claimRewards.active = false;
        }).bounds(5, this.height - 25, buttonWidth, 20).build());
        this.claimRewards.active = this.menu.progress().isComplete() && this.menu.progress().claimedRewards().size() < this.menu.quest().rewards().size();

        this.overview.setSelected(true);

        int contentX = (int) (this.width * 0.31f);
        int contentY = 30;
        int contentWidth = (int) (this.width * 0.63f);
        int contentHeight = this.height - 45;

        this.rewardList = new RewardListWidget(contentX, contentY, contentWidth, contentHeight);
        this.rewardList.update(this.menu.id(), this.menu.quest());

        try {
            this.descriptionError = null;
            TagProvider provider = new QuestTagProvider();
            String desc = String.join("", this.menu.quest().display().description());
            this.description = new DocumentWidget(contentX, contentY, contentWidth, contentHeight, new DefaultTheme(), provider.parse(desc));
        } catch (Exception e) {
            this.descriptionError = e.getMessage();
        }
    }

    @Override
    protected void goBack() {
        NetworkHandler.CHANNEL.sendToServer(new OpenGroupPacket(
            this.menu.quest().display().group(),
            this instanceof QuestEditScreen
        ));
    }

    private void clearSelected() {
        this.overview.setSelected(false);
        this.tasks.setSelected(false);
        this.rewards.setSelected(false);
    }

    @Override
    protected void renderBg(PoseStack stack, float partialTick, int mouseX, int mouseY) {
        super.renderBg(stack, partialTick, mouseX, mouseY);
        RenderSystem.disableDepthTest();
        if (this.tasks != null && this.tasks.isSelected() && getTaskList() instanceof Renderable renderable) {
            renderable.render(stack, mouseX, mouseY, partialTick);
        }
        if (this.rewards != null && this.rewards.isSelected()) {
            this.rewardList.render(stack, mouseX, mouseY, partialTick);
        }
        if (this.description != null && this.overview.isSelected()) {
            this.description.render(stack, mouseX, mouseY, partialTick);
        }
        if (this.descriptionError != null && this.overview.isSelected()) {
            int contentX = (int) (this.width * 0.31f) + 20;
            int contentY = 30;
            int contentWidth = (int) (this.width * 0.63f) - 40;
            int contentHeight = this.height - 45;
            for (FormattedCharSequence sequence : Minecraft.getInstance().font.split(Component.literal(this.descriptionError), contentWidth)) {
                int textWidth = this.font.width(sequence);
                this.font.draw(stack, sequence, contentX + (contentWidth - textWidth) / 2f, contentY + (contentHeight - this.font.lineHeight) / 2f, 0xFF0000);
                contentY += this.font.lineHeight;
            }
        }
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        List<GuiEventListener> listeners = new ArrayList<>();
        for (TemporyWidget widget : temporaryWidgets) {
            if (widget.isVisible() && widget instanceof GuiEventListener listener) {
                listeners.add(listener);
            }
        }
        if (!listeners.isEmpty()) {
            return listeners;
        }

        List<GuiEventListener> children = new ArrayList<>();
        if (this.tasks != null && this.tasks.isSelected()) {
            children.add(getTaskList());
        }
        if (this.rewards != null && this.rewards.isSelected()) {
            children.add(this.rewardList);
        }
        if (this.description != null && this.overview.isSelected()) {
            children.add(this.description);
        }
        children.addAll(super.children());
        return children;
    }

    public abstract GuiEventListener getTaskList();
}