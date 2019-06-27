import javafx.scene.image.Image;

public class Const {
    public enum Node   { SERVER, CLIENT }
    public enum Action { AUTHORIZATION, DISCONNECT, COPY, CREATE, LIST, REMOVE }

    public static final String storageRoot      = "server_storage";
    public static final String userLocalHomeDir = System.getProperty("user.dir");
    public static final Image appIcon           = new Image("img\\cloud.png");

    public static final String host   = "localhost";
    public static final int    port   = 8189;
    public static final int    buffer = 1 * 1024 * 1024;

    public static final int maxObjectSize = 3 * buffer;
}
