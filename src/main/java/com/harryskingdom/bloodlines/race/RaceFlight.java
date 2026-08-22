package com.harryskingdom.bloodlines.race;

/**
 * Which races get real flight. The actual flight mechanic (trigger, stamina, boost, loop-de-loop, altitude feel -
 * all of it) is handled entirely by the real Icarus mod now: IcarusIntegration equips Fae/Seraph with an actual
 * Icarus wing item, and Icarus's own code takes it from there natively. This class exists just so the wing-render
 * and wing-item call sites share one definition of "which races fly" instead of repeating the race check.
 */
public final class RaceFlight
{
    private RaceFlight() {}

    public static boolean grantsFlight(Race race)
    {
        return race == Race.FAE || race == Race.SERAPH;
    }
}
