package mod.chiselsandbits.api.measuring;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

import java.util.UUID;

/**
 * Represents a measurement made in a given world by a given player.
 */
public interface IMeasurement
{
    /**
     * The id of the player who made the measurement.
     *
     * @return The id of the owner.
     */
    UUID getOwner();

    /**
     * The lower left front corner of the measurement.
     *
     * @return The start point of the measurement.
     */
    Vector3d getFrom();

    /**
     * The top right back corner of the measurement.
     *
     * @return The end point of the measurement.
     */
    Vector3d getTo();

    /**
     * The color of the measurement.
     *
     * @return The color of the measurement.
     */
    Vector3d getColor();

    /**
     * The id of the world that this measurement was made in.
     *
     * @return The id of the world.
     */
    ResourceLocation getWorldKey();
}
