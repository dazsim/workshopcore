package net.minecraft.resources;

import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;

/** Vanilla added this in 1.20.2; Fabric 1.20.1 needs the same helper for datapack scans. */
public class FileToIdConverter {
    private final String prefix;
    private final String extension;

    public FileToIdConverter(String prefix, String extension) {
        this.prefix = prefix;
        this.extension = extension;
    }

    public static FileToIdConverter json(String prefix) {
        return new FileToIdConverter(prefix, ".json");
    }

    public ResourceLocation idToFile(ResourceLocation id) {
        return new ResourceLocation(id.getNamespace(), this.prefix + "/" + id.getPath() + this.extension);
    }

    public ResourceLocation fileToId(ResourceLocation file) {
        String path = file.getPath();
        return new ResourceLocation(file.getNamespace(), path.substring(this.prefix.length() + 1, path.length() - this.extension.length()));
    }

    public Map<ResourceLocation, Resource> listMatchingResources(ResourceManager manager) {
        return manager.listResources(this.prefix, location -> location.getPath().endsWith(this.extension));
    }

    public Map<ResourceLocation, java.util.List<Resource>> listMatchingResourceStacks(ResourceManager manager) {
        return manager.listResourceStacks(this.prefix, location -> location.getPath().endsWith(this.extension));
    }
}
