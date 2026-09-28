package com.example.render.font;

import net.minecraft.resources.Identifier;

/**
 * Supported MSDF typography font faces.
 */
public enum FontFace {
    SF_REGULAR(
        Identifier.fromNamespaceAndPath("modid", "fonts/sf_pro/sf_regular.png"),
        Identifier.fromNamespaceAndPath("modid", "fonts/sf_pro/sf_regular.json")
    ),
    SF_MEDIUM(
        Identifier.fromNamespaceAndPath("modid", "fonts/sf_pro/sf_medium.png"),
        Identifier.fromNamespaceAndPath("modid", "fonts/sf_pro/sf_medium.json")
    ),
    SF_SEMIBOLD(
        Identifier.fromNamespaceAndPath("modid", "fonts/sf_pro/sf_semibold.png"),
        Identifier.fromNamespaceAndPath("modid", "fonts/sf_pro/sf_semibold.json")
    );

    private final Identifier textureId;
    private final Identifier dataId;

    FontFace(Identifier textureId, Identifier dataId) {
        this.textureId = textureId;
        this.dataId = dataId;
    }

    public Identifier getTextureId() {
        return textureId;
    }

    public Identifier getDataId() {
        return dataId;
    }
}
