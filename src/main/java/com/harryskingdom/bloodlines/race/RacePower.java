package com.harryskingdom.bloodlines.race;

/**
 * One line item in a race's power list on the selection screen (see BloodlineSelectScreen) - a title, a short
 * description, and whether it reads as a strength, a weakness, or neither. Purely descriptive/UI-facing: the
 * actual mechanical effect lives in RaceStats/RaceAbility, this just narrates it for the player.
 */
public record RacePower(String title, String description, Category category)
{
    public enum Category
    {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }
}
