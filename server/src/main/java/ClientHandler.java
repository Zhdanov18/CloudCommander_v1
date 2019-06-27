import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler {
    private String nick;
    private String path;
    private List<MyFileDescriptor> data;

    public String getNick() { return nick; }
    public String getPath() { return path; }
    public List<MyFileDescriptor> getData() { return data; }
    public String getClientRoot() {
        return String.join(File.separator, Const.storageRoot, this.nick);
    }

    public ClientHandler(String nick) {
        this.nick = nick;
        this.data = new ArrayList<>();

        File clientRoot = new File(getClientRoot());
        if (!clientRoot.exists()) { clientRoot.mkdir(); }

        this.path = getClientRoot();
        refreshData();
    }

    public void refreshData() {
        data.clear();
        data = ActionHandler.getFileList(path, getClientRoot());
    }

    public String changeDirectory(String directory) {
        refreshData();
        if (directory == null && path.equals(getClientRoot())) {
            return null;
        }
        String newDirectory = (directory != null) ? String.join(File.separator, path, directory) : path.substring(0, path.lastIndexOf(File.separator));

        File tmp = new File(newDirectory);
        if (tmp.exists()) {
            path = newDirectory;
            refreshData();
            return path;
        }
        return null;
    }
}

//Сделано:
//Навигация по каталогам, их создание, копирование в обоих направлениях и удаление, в т.ч. непустых с помощью
//SimpleFileVisitor и callback с запросами подтверждений операций удаления/перезаписи файлов/каталогов
//Копирование больших файлов частями BufferedInputStream/BufferedOutputStream

//скопировать при offline и refresh - ошибка null pointer exception
//Глобальные надо:
//ChunkedFile: передается только содержимое файла, для передачи имени файла необходим протокол -
//т.е. и клиент, и сервер должны использовать netty
//множественное выделение???

//переименование файлов
//докачка файлов
//синхронизация локальное хранилище/облако
//шифрование
