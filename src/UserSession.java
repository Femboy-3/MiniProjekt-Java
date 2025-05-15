public class UserSession {
    public static boolean isAdmin = true;
    public static int userId = 0;

    public static void clear() {
        isAdmin = false;
        userId = 0;
    }
}
