package io.drogue.iot.ditto;

public class Attributes {
    private Attributes() {
    }

    public static boolean isNonEmptyString(final Object value) {
        if (!(value instanceof String)) {
            return false;
        }

        return !((String) value).isBlank();
    }
}
