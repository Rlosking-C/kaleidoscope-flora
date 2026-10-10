package com.rlosking.flora;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A perch bound to one flower id. The item is resolved lazily from the
 * registry so a VanillaBackport flower absent from the install resolves to
 * null instead of crashing - the perch itself is simply never registered
 * in that case, this is only the belt-and-braces second line of defence.
 */
public class PlantedFlowerPerchBlock extends FlowerPerchBlock {

    private final ResourceLocation flowerId;
    private final int petalColor;
    private final boolean tall;

    public PlantedFlowerPerchBlock(Properties properties, ResourceLocation flowerId, int petalColor, boolean tall) {
        super(properties);
        this.flowerId = flowerId;
        this.petalColor = petalColor;
        this.tall = tall;
    }

    /** Creative pick-block yields the flower (vanilla flower-pot behaviour). */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        Item flower = plantedFlowerItem(state);
        return flower != null ? new ItemStack(flower)
                : new ItemStack(FlowerPerches.emptyPerchItem(state.getValue(FlowerPerchBlock.WOOD)).get());
    }

    @Override
    protected boolean isPlanted(BlockState state) {
        return true;
    }

    @Override
    @Nullable
    protected Item plantedFlowerItem(BlockState state) {
        Item item = BuiltInRegistries.ITEM.get(flowerId);
        return item == Items.AIR ? null : item;
    }

    @Override
    protected ResourceLocation plantedFlowerId() {
        return flowerId;
    }

    @Override
    protected int petalColor() {
        return petalColor;
    }

    @Override
    protected boolean isTallFlower() {
        return tall;
    }
}
