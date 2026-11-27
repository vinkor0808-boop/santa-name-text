package net.minecraft.world.gen.heightprovider;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.collection.WeightedPool;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.HeightContext;

public class WeightedListHeightProvider extends HeightProvider {
	public static final MapCodec<WeightedListHeightProvider> WEIGHTED_LIST_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
				WeightedPool.createNonEmptyCodec(HeightProvider.CODEC)
					.fieldOf("distribution")
					.forGetter(weightedListHeightProvider -> weightedListHeightProvider.weightedList)
			)
			.apply(instance, WeightedListHeightProvider::new)
	);
	private final WeightedPool<HeightProvider> weightedList;

	public WeightedListHeightProvider(WeightedPool<HeightProvider> weightedList) {
		this.weightedList = weightedList;
	}

	@Override
	public int get(Random random, HeightContext context) {
		return this.weightedList.get(random).get(random, context);
	}

	@Override
	public HeightProviderType<?> getType() {
		return HeightProviderType.WEIGHTED_LIST;
	}
}
