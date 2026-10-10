package com.rlosking.flora;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One item per wood species: all nine place the same block, the blockstate
 * carries the species. Keeping the difference in the item avoids nine
 * near-identical block classes, and matches how vanilla exposes "planks"
 * (one block per wood, the wood named in the registry id).
 */
public class FlowerPerchItem extends BlockItem {

    private final PerchWood wood;

    public FlowerPerchItem(Block block, PerchWood wood, Properties properties) {
        super(block, properties);
        this.wood = wood;
    }

    /** Stamps the species onto the state vanilla would have placed. */
    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(FlowerPerchBlock.WOOD, this.wood);
    }

    /**
     * Without this override every item would be named after the block
     * ("Flower Perch"), because BlockItem borrows the block's description id.
     */
    @Override
    public String getDescriptionId() {
        return "item.kaleidoscope_flora.flower_perch_" + this.wood.getSerializedName();
    }

    public PerchWood wood() {
        return this.wood;
    }
}
