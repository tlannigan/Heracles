package earth.terrarium.heracles.client.screens.quest;

import com.mojang.blaze3d.platform.InputConstants;
import earth.terrarium.heracles.api.client.settings.SettingInitializer;
import earth.terrarium.heracles.api.client.settings.Settings;
import earth.terrarium.heracles.api.rewards.QuestReward;
import earth.terrarium.heracles.api.rewards.QuestRewardType;
import earth.terrarium.heracles.api.rewards.QuestRewards;
import earth.terrarium.heracles.api.tasks.QuestTask;
import earth.terrarium.heracles.api.tasks.QuestTaskType;
import earth.terrarium.heracles.api.tasks.QuestTasks;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.screens.quest.editing.QuestTextEditor;
import earth.terrarium.heracles.client.screens.quest.rewards.RewardListWidget;
import earth.terrarium.heracles.client.screens.quest.tasks.TaskListWidget;
import earth.terrarium.heracles.client.widgets.modals.CreateObjectModal;
import earth.terrarium.heracles.client.widgets.modals.EditObjectModal;
import earth.terrarium.heracles.client.widgets.modals.ItemModal;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.handlers.quests.QuestHandler;
import earth.terrarium.heracles.common.menus.quest.QuestContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.quests.OpenQuestPacket;
import earth.terrarium.heracles.common.network.packets.quests.data.NetworkQuestData;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class QuestEditScreen extends BaseQuestScreen {

    private TaskListWidget taskList;
    private RewardListWidget rewardList;
    private QuestTextEditor descriptionBox;

    private CreateObjectModal createModal;
    private ItemModal itemModal;

    public QuestEditScreen(QuestContent content) {
        super(content);
    }

    @Override
    public void updateProgress(@Nullable QuestProgress newProgress) {
        super.updateProgress(newProgress);
        this.taskList.update(this.quest().tasks().values());
        this.rewardList.update(this.content.fromGroup(), this.content.id(), this.quest());
    }

    @Override
    protected void rebuildWidgets() {
        var oldBox = this.descriptionBox;
        ClientQuests.updateQuest(
            entry(),
            quest -> NetworkQuestData.builder().description(new ArrayList<>(this.descriptionBox.lines())),
            false
        );
        super.rebuildWidgets();
        this.descriptionBox.update(oldBox);
    }

    @Override
    protected void init() {
        super.init();

        this.createModal = addTemporary(new CreateObjectModal(this.width, this.height));

        int contentWidth = (int) (this.width * 0.63f);
        int contentHeight = this.height - 15;
        int contentX = (int) (this.width * 0.31f);
        int contentY = 15;

        this.taskList = new TaskListWidget(contentX, contentY, contentWidth, contentHeight, 5.0D, 5.0D,
            this.content.id(), this.entry(), this.content.progress(), this.content.quests(), (task, isRemoving) -> {
            if (isRemoving) {
                ClientQuests.updateQuest(this.entry(), quest -> {
                    quest.tasks().remove(task.id());
                    return NetworkQuestData.builder().tasks(quest.tasks());
                });
                this.taskList.update(this.quest().tasks().values());
                return;
            }
            taskPopup(ModUtils.cast(task.type()), task.id(), ModUtils.cast(task), this.taskList::updateTask);
        }, () -> {
            BiConsumer<String, QuestTaskType<?>> creator = (id, type) ->
                taskPopup(ModUtils.cast(type), id, null, newTask -> {
                    ClientQuests.updateQuest(this.entry(), quest -> {
                        quest.tasks().put(id, newTask);
                        return NetworkQuestData.builder().tasks(quest.tasks());
                    });
                    this.taskList.update(this.quest().tasks().values());
                });

            this.createModal.setVisible(true);
            this.createModal.update(
                "task",
                (type, id) -> creator.accept(id, QuestTasks.get(type)),
                (type, id) -> !this.quest().tasks().containsKey(id) && QuestTasks.types().containsKey(type),
                ConstantComponents.Tasks.CREATE,
                QuestTasks.types().values()
                    .stream()
                    .filter(questTaskType -> Settings.getFactory(questTaskType) != null)
                    .map(QuestTaskType::id)
                    .toList()
            );
        });

        this.rewardList = new RewardListWidget(
            contentX, contentY, contentWidth, contentHeight, 5.0D, 5.0D, this.entry(),
            this.content.progress(), (reward, isRemoving) -> {
                if (isRemoving) {
                    ClientQuests.updateQuest(this.entry(), quest -> {
                        quest.rewards().remove(reward.id());
                        return NetworkQuestData.builder().rewards(quest.rewards());
                    });
                    this.rewardList.update(this.content.fromGroup(), this.content.id(), this.quest());
                    return;
                }
                rewardPopup(ModUtils.cast(reward.type()), reward.id(), ModUtils.cast(reward), this.rewardList::updateReward);
            }, () -> {
            BiConsumer<String, QuestRewardType<?>> creator = (id, type) ->
                rewardPopup(ModUtils.cast(type), id, null, newReward -> {
                    ClientQuests.updateQuest(this.entry(), quest -> {
                        quest.rewards().put(id, newReward);
                        return NetworkQuestData.builder().rewards(quest.rewards());
                    });
                    this.rewardList.update(this.content.fromGroup(), this.content.id(), this.quest());
                });

            this.createModal.setVisible(true);
            this.createModal.update(
                "reward",
                (type, id) -> creator.accept(id, QuestRewards.get(type)),
                (type, id) -> !this.quest().rewards().containsKey(id) && QuestRewards.types().containsKey(type),
                ConstantComponents.Rewards.CREATE,
                QuestRewards.types().values()
                    .stream()
                    .filter(questRewardType -> Settings.getFactory(questRewardType) != null)
                    .map(QuestRewardType::id)
                    .toList()
            );
        }
        );

        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2)) {
            addRenderableWidget(new ImageButton(this.width - 24, 1, 11, 11, 33, 15, 11, HEADING, 256, 256, (button) ->
                NetworkHandler.CHANNEL.sendToServer(new OpenQuestPacket(this.content.fromGroup(), this.content.id(), false))
            )).setTooltip(Tooltip.create(ConstantComponents.TOGGLE_EDIT));
        }

        this.descriptionBox = new QuestTextEditor(contentX, contentY, contentWidth, contentHeight);
        this.descriptionBox.setContent(String.join("\n", this.quest().display().description()).replace("§", "&&"));


        if (Minecraft.getInstance().isLocalServer()) {
            addRenderableWidget(new ImageButton(this.width - 36, 1, 11, 11, 33, 59, 11, HEADING, 256, 256, (button) -> {
                Path path = QuestHandler.getQuestPath(this.quest(), this.getQuestId());
                if (path.toFile().isFile() && path.toFile().exists()) {
                    Util.getPlatform().openFile(path.toFile());
                }
            })).setTooltip(Tooltip.create(ConstantComponents.OPEN_QUEST_FILE));
        }

        this.itemModal = addTemporary(new ItemModal(this.width, this.height));
        updateProgress(null);
    }

    private <T extends QuestTask<?, ?, T>> void taskPopup(QuestTaskType<T> type, String id, @Nullable T task, Consumer<T> consumer) {
        SettingInitializer<?> setting = Settings.getFactory(type);
        if (setting == null) return;

        SettingInitializer.CreationData data = setting.create(ModUtils.cast(task));
        if (data.isEmpty()) {
            var newTask = setting.create(id, ModUtils.cast(task), new SettingInitializer.Data());
            if (newTask == null) return;
            consumer.accept(ModUtils.cast(newTask));
        } else {
            EditObjectModal widget = findOrCreateEditWidget();
            widget.init(type.id(), data, savedData -> {
                var newTask = setting.create(id, ModUtils.cast(task), savedData);
                if (newTask == null) return;
                consumer.accept(ModUtils.cast(newTask));
            });
            widget.setTitle(ConstantComponents.Tasks.EDIT);
        }
    }

    private <T extends QuestReward<T>> void rewardPopup(QuestRewardType<T> type, String id, @Nullable T reward, Consumer<T> consumer) {
        SettingInitializer<?> setting = Settings.getFactory(type);
        if (setting == null) return;
        SettingInitializer.CreationData data = setting.create(ModUtils.cast(reward));
        if (data.isEmpty()) {
            var newReward = setting.create(id, ModUtils.cast(reward), new SettingInitializer.Data());
            if (newReward == null) return;
            consumer.accept(ModUtils.cast(newReward));
        } else {
            EditObjectModal widget = findOrCreateEditWidget();
            widget.init(type.id(), data, savedData -> {
                var newReward = setting.create(id, ModUtils.cast(reward), savedData);
                if (newReward == null) return;
                consumer.accept(ModUtils.cast(newReward));
            });
            widget.setTitle(ConstantComponents.Rewards.EDIT);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Screen.hasControlDown() && keyCode == InputConstants.KEY_S) {
            ClientQuests.updateQuest(
                entry(),
                quest -> NetworkQuestData.builder().description(new ArrayList<>(this.descriptionBox.lines())),
                false
            );
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        ClientQuests.updateQuest(entry(), quest ->
            NetworkQuestData.builder().description(new ArrayList<>(this.descriptionBox.lines()))
        );
    }

    @Override
    public GuiEventListener getTaskList() {
        return this.taskList;
    }

    @Override
    public GuiEventListener getRewardList() {
        return this.rewardList;
    }

    @Override
    public GuiEventListener getDescriptionWidget() {
        return this.descriptionBox;
    }

    @Override
    public String getDescriptionError() {
        return null;
    }

    @Override
    public @NotNull Component getTitle() {
        return super.getTitle().copy()
            .append(" - [")
            .append(Component.literal(this.getQuestId() + ".json").withStyle(ChatFormatting.BLACK))
            .append("]");
    }

    public ItemModal itemModal() {
        return this.itemModal;
    }
}
