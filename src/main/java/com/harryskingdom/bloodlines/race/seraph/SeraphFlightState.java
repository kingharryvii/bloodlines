package com.harryskingdom.bloodlines.race.seraph;

/**
 * Seraph's flight state machine (see SeraphFlightController for the transitions, SeraphWingsLayer for how each
 * state poses the wings). FOLDED -> TAKEOFF -> ACTIVE_FLIGHT <-> GLIDE -> LANDING -> FOLDED, matching the
 * transition chain the wing animation is built around - no state change should ever visually snap.
 */
public enum SeraphFlightState
{
    /** On the ground, wings folded against the back. */
    GROUNDED,
    /** The first moment of flight: wings spreading and the initial launch impulse playing out. */
    TAKEOFF,
    /** Airborne and recently flapped - wings actively beating. */
    ACTIVE_FLIGHT,
    /** Airborne but coasting - wings extended, only subtle movement. */
    GLIDE,
    /** Just touched down from flight - wings easing from open back toward folded. */
    LANDING
}
