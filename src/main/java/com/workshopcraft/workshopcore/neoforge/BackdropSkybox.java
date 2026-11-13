package com.workshopcraft.workshopcore.neoforge;

import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import org.joml.Matrix4fc;

/** NeoForge custom skybox used both via dimension attribute and ExtractLevelRenderStateEvent. */
public final class BackdropSkybox implements CustomSkyboxRenderer {
    public static final BackdropSkybox INSTANCE = new BackdropSkybox();
    public static final Identifier ID = Identifier.fromNamespaceAndPath("workshopcore", "backdrop");

    private BackdropSkybox() {
    }

    @Override
    public boolean renderSky(LevelRenderState levelRenderState, SkyRenderState skyRenderState, Matrix4fc modelViewMatrix, Runnable setupFog) {
        return BackdropClient.render(modelViewMatrix, setupFog);
    }
}
