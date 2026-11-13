package com.workshopcraft.workshopcore.screwdriver;

/** Result of a screwdriver use. PASS lets the next handler or vanilla try. */
public enum ScrewdriverResult {
    PASS,
    SUCCESS,
    FAIL
}
