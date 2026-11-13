package com.workshopcraft.workshopcore;

public final class WorkshopCore {
    public static final String MOD_ID = "workshopcore";
    public static final String API_VERSION = "0.3.0";

    private WorkshopCore() {
    }

    public static boolean isAtLeast(String version) {
        return compare(API_VERSION, version) >= 0;
    }

    private static int compare(String a, String b) {
        int[] left = parse(a);
        int[] right = parse(b);
        for (int i = 0; i < 3; i++) {
            int c = Integer.compare(left[i], right[i]);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    private static int[] parse(String version) {
        int[] parts = new int[3];
        if (version == null || version.isEmpty()) {
            return parts;
        }
        String core = version;
        int cut = version.indexOf('-');
        if (cut >= 0) {
            core = version.substring(0, cut);
        }
        int i = 0;
        int start = 0;
        while (i < 3) {
            int dot = core.indexOf('.', start);
            String piece = dot < 0 ? core.substring(start) : core.substring(start, dot);
            parts[i] = parseInt(piece);
            i++;
            if (dot < 0) {
                break;
            }
            start = dot + 1;
        }
        return parts;
    }

    private static int parseInt(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                break;
            }
            n = n * 10 + (c - '0');
        }
        return n;
    }
}
