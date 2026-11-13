plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.2-neoforge"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    replacements.string(current.parsed < "1.21.11") {
        replace("Identifier", "ResourceLocation")
    }

    replacements.string(current.parsed >= "26.3") {
        replace("com.mojang.blaze3d.buffers.GpuBufferSlice", "com.mojang.renderpearl.api.buffers.GpuBufferSlice")
        replace("com.mojang.blaze3d.buffers.GpuBuffer", "com.mojang.renderpearl.api.buffers.GpuBuffer")
        replace("com.mojang.blaze3d.PrimitiveTopology", "com.mojang.renderpearl.api.pipeline.PrimitiveTopology")
        replace("com.mojang.blaze3d.pipeline.RenderPipeline", "com.mojang.renderpearl.api.pipeline.RenderPipeline")
        replace("com.mojang.blaze3d.systems.RenderPass", "com.mojang.renderpearl.api.commands.RenderPass")
        replace("com.mojang.blaze3d.textures.GpuTextureView", "com.mojang.renderpearl.api.textures.GpuTextureView")
        replace("com.mojang.blaze3d.vertex.VertexFormat", "com.mojang.renderpearl.api.vertex.VertexFormat")
    }
}
