package com.riftlog.entity;

/** Where a match comes from: counted live at the table, typed in afterwards, or imported from another tool. */
public enum MatchSource {
    LIVE,
    MANUAL,
    IMPORTED
}
