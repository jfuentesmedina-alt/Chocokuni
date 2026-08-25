package com.jaimechococraft.paper.race;

/**
 * Definicion inmutable de una raza/color de chocobo, tal como se carga desde config.yml.
 */
public final class ChocoboRace {

    private final String id;
    private final String displayName;
    private final String modelAdult;
    private final String modelBaby;
    private final double movementSpeed;
    private final double jumpStrength;
    private final int weight;

    public ChocoboRace(String id, String displayName, String modelAdult, String modelBaby,
                        double movementSpeed, double jumpStrength, int weight) {
        this.id = id;
        this.displayName = displayName;
        this.modelAdult = modelAdult;
        this.modelBaby = modelBaby;
        this.movementSpeed = movementSpeed;
        this.jumpStrength = jumpStrength;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getModelAdult() {
        return modelAdult;
    }

    public String getModelBaby() {
        return modelBaby;
    }

    public String getModel(boolean adult) {
        return adult ? modelAdult : modelBaby;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public double getJumpStrength() {
        return jumpStrength;
    }

    public int getWeight() {
        return weight;
    }
}
