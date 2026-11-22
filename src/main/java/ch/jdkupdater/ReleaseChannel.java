package ch.jdkupdater;

/**
 * Selects which Java version counts as the latest one.
 */
public enum ReleaseChannel {

    /** The newest long-term support version, for example 25. */
    LTS,

    /** The newest feature version, long-term support or not, for example 27. */
    NEWEST
}
