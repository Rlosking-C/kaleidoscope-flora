package com.rlosking.flora;

import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.ModList;

import java.util.Arrays;
import java.util.List;

/**
 * The wood species a flower perch can be built from.
 *
 * <p>Every perch - empty or planted - carries one of these as a blockstate
 * property, so a perch built from cherry keeps its cherry frame after a
 * flower is planted. The frame is <b>referenced from vanilla, never copied
 * into the jar</b>: the Usage Guidelines forbid redistributing Mojang's
 * textures, and a recoloured copy is still their asset ("any derivatives
 * ... remain owned by Mojang").</p>
 *
 * <p>Pale oak arrives with VanillaBackport, which registers it in the
 * {@code minecraft} namespace. Without VB the species is not offered to the
 * player. Its blockstate variant still exists, so a non-VB install logs a
 * missing texture for that one unreachable frame - harmless, and a
 * conditional model is not available.</p>
 */
public enum PerchWood implements StringRepresentable {

    OAK("oak", false),
    SPRUCE("spruce", false),
    BIRCH("birch", false),
    JUNGLE("jungle", false),
    ACACIA("acacia", false),
    DARK_OAK("dark_oak", false),
    MANGROVE("mangrove", false),
    CHERRY("cherry", false),
    PALE_OAK("pale_oak", true);

    /**
     * The species every perch used before the wood variants existed. Kept as
     * the blockstate default so perches placed in older worlds keep the
     * frame they were built with.
     */
    public static final PerchWood DEFAULT = SPRUCE;

    private final String id;
    private final boolean backport;

    PerchWood(String id, boolean backport) {
        this.id = id;
        this.backport = backport;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** True for species that only exist when VanillaBackport is installed. */
    public boolean backport() {
        return backport;
    }

    /** Vanilla bark texture, used on the four side faces of the frame. */
    public String logTexture() {
        return "minecraft:block/" + id + "_log";
    }

    /** Vanilla ring texture, used on the top and bottom faces of the frame. */
    public String logTopTexture() {
        return "minecraft:block/" + id + "_log_top";
    }

    /** The species offered in this install, in catalogue order. */
    public static List<PerchWood> available() {
        boolean backport = ModList.get().isLoaded("vanillabackport");
        return Arrays.stream(values()).filter(wood -> !wood.backport || backport).toList();
    }
}
