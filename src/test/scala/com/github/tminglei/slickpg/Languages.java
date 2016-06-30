package com.github.tminglei.slickpg;

/**
 * Used by [[PgEnumSupportSuite]]
 *
 * Created by gwilson on 6/24/16.
 */
public enum Languages {

    SCALA("Scala"),
    JAVA("Java"),
    CLOJURE("Clojure");

    private final String value;

    Languages(final String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }
}
