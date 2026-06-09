package ru.truhot.rdang.data;

import org.bukkit.block.Biome;

import java.util.List;
import java.util.Objects;

public class DangData {

    private final String fileName;
    private final String world;
    private final List<Biome> biome;

    public DangData(String fileName, String world, List<Biome> biome) {
        this.fileName = fileName;
        this.world = world;
        this.biome = biome;
    }

    public String getFileName() {
        return fileName;
    }

    public String getWorld() {
        return world;
    }

    public List<Biome> getBiome() {
        return biome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DangData dangData = (DangData) o;
        return Objects.equals(fileName, dangData.fileName) && Objects.equals(world, dangData.world) && Objects.equals(biome, dangData.biome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, world, biome);
    }
}
