public class Utils {
    public static boolean isIPAddress(String toTest) {
        String[] strSplit = toTest.split("\\.");
        if (strSplit.length != 4) return false;
        for (int i = 0; i < strSplit.length; i++) {
            try {
                Integer.parseInt(strSplit[i]);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNumber(String toTest) {
        try {
            Integer.parseInt(toTest);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
