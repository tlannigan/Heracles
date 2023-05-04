package earth.terrarium.heracles.api.rewards;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

public interface QuestRewardSerializer<T extends QuestReward<T>> {

    ResourceLocation id();

    Codec<T> codec();
}
