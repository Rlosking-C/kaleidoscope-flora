package com.rlosking.flora;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Raw Flower Cake: thrown like a snowball, pins whatever it hits, and can
 * also be set down as a block.
 *
 * <p><b>Snowball behaviour is mirrored, not inherited - and that is a
 * deliberate trade.</b> The obvious move is to keep extending
 * {@code SnowballItem}, as the first version of this class did, and get the
 * throw for free. But a cake now also has to be placeable, and placement is
 * {@code BlockItem}'s job; a class cannot be both. So this extends
 * {@link PlaceableCakeItem} (a {@code BlockItem}) and <b>reimplements the two
 * things {@code SnowballItem} actually provides</b>, copied from its source so
 * the behaviour is identical rather than approximately identical:</p>
 *
 * <ol>
 *   <li>{@link #use} - the throw: the vanilla snowball sound, a plain
 *       {@code Snowball} carrying this stack, the item-used stat, one consumed.</li>
 *   <li>{@link #asProjectile} - the dispenser path. This is the half worth being
 *       careful about: it is what makes dispensers fire raw cakes at all, and
 *       inheriting {@code BlockItem} alone would have silently dropped it.</li>
 * </ol>
 *
 * <p>Note that the throw does <b>not</b> move onto the sneaking path: sneak +
 * right-click on a block places, plain right-click throws. Both are reachable
 * because {@link PlaceableCakeItem#useOn} passes when not sneaking, which lets
 * the client fall back to {@code use} - the same mechanism by which a vanilla
 * player throws a snowball at a wall.</p>
 *
 * <p><b>The pin is NOT applied here.</b> This class has no hit hook - the
 * projectile is a plain vanilla {@code Snowball}, and its collision is picked
 * up on Neoforge's event bus instead (see
 * {@link FloraEvents#onProjectileImpact}). Doing it that way avoids registering
 * an entity type of our own, which is a permanent cost in every save and every
 * network handshake, for one second of behaviour on a projectile that already
 * exists.</p>
 */
public class RawFlowerCakeItem extends PlaceableCakeItem implements ProjectileItem {

    public RawFlowerCakeItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The throw. Body copied from {@code SnowballItem#use} so it stays exact. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            Snowball snowball = new Snowball(level, player);
            snowball.setItem(itemstack);
            snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(snowball);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        itemstack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    /** The dispenser path. Body copied from {@code SnowballItem#asProjectile}. */
    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        Snowball snowball = new Snowball(level, pos.x(), pos.y(), pos.z());
        snowball.setItem(stack);
        return snowball;
    }
}
