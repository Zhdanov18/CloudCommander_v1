import java.nio.file.Path;
import java.util.Arrays;

public class FileMessage extends AbstractMessage {
    private String  path;
    private String  filename;
    private byte[]  data;
    private int     sizePacket;
    private boolean firstPacket;

    public String  getPath()       { return path; }
    public String  getFilename()   { return filename; }
    public byte[]  getData()       { return Arrays.copyOf(data, sizePacket); }
    public int     getSizePacket() { return sizePacket; }
    public boolean isFirstPacket() { return firstPacket;  }

    public void setParameters(byte[] data, int sizePacket, boolean firstPacket) {
        this.data = data;
        this.sizePacket = sizePacket;
        this.firstPacket = firstPacket;
    }

    public FileMessage(String path, Path filename) {
        this.path = path;
        this.filename = filename.getFileName().toString();
    }

    public FileMessage(String path, Path filename, byte[] data, int sizePacket, boolean firstPacket) {
        this.path = path;
        this.filename = filename.getFileName().toString();
        this.data = data;
        this.sizePacket = sizePacket;
        this.firstPacket = firstPacket;
    }
}
