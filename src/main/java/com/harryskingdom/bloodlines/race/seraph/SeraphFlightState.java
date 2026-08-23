package com.harryskingdom.bloodlines.race.seraph;

/**
 * Seraph's flight state machine (see SeraphFlightController for the transitions). Both the physics and the wing
 * animation read this - the physics decides it, the animation only ever reacts to it.
 * <pre>
 * GROUNDED -&gt; TAKEOFF -&gt; ACTIVE_FLIGHT/FLAPPING &lt;-&gt; GLIDING -&gt; DESCENDING -&gt; LANDING -&gt; GROUNDED
 * </pre>
 */
public enum SeraphFlightState
{
    /** On the ground, wings folded against the back. */
    GROUNDED,
    /** The first moment of flight: wings spreading and the initial launch impulse playing out. */
    TAKEOFF,
    /** Airborne, thrusting forward under look-directed power, not in the middle of a flap. */
    ACTIVE_FLIGHT,
    /** A flap is actively playing out (the brief window right after a flap event). */
    FLAPPING,
    /** Airborne, coasting on momentum rather than thrusting - wings extended, minimal movement. */
    GLIDING,
    /** Airborne and losing altitude notably, not from a recent flap. */
    DESCENDING,
    /** Just touched down from flight - wings easing from open back toward folded. */
    LANDING
}
