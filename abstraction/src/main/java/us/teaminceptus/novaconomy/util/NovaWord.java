package us.teaminceptus.novaconomy.util;

import org.jetbrains.annotations.NotNull;

public class NovaWord {

    public static String capitalize(@NotNull String str) {
        if (str.isEmpty()) return str;

        final char[] buffer = str.toLowerCase().toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
    }

}
