import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ActionHandler {
    public enum AlertText {
        CREATE_DIR_ERR ("Error",      "The directory is not created"),
        DELETE_ERR     ("Error",      "An error occurred during deletion"),
        DELETE_DIR_ERR ("Error",      "The directory is not deleted"),
        DELETE_FILE_ERR("Error",      "The file is not deleted"),
        COPY_ERR       ("Error",      "An error occurred while copying"),
        DELETE         ("Delete",     "Are you sure want to delete?"),
        DISCONNECT     ("Disconnect", "Are you sure want to disconnect?"),
        REWRITE        ("Rewrite",    "Your data will be overwritten. Are you sure?");

        private String title;
        private String headerText;

        public String getTitle() { return title; }
        public String getHeaderText() { return headerText; }

        AlertText(String title, String headerText) {
            this.title = title;
            this.headerText = headerText;
        }
    }

    public static boolean confirmMsg(AlertText alertText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(alertText.getTitle());
        alert.setHeaderText(alertText.getHeaderText());
        alert.setContentText(contentText);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get().getText().equals("OK")) {
            return true;
        }
        return false;
    }

    public static void errorMsg( AlertText alertText, String contentText, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(alertText.getTitle());
        alert.setHeaderText(alertText.getHeaderText());

        if (exception != null) {
            VBox dialogPaneContent = new VBox();
            Label label = new Label("Stack Trace:");

            String stackTrace = getStackTrace(exception);
            TextArea textArea = new TextArea();
            textArea.setText(stackTrace);

            dialogPaneContent.getChildren().addAll(label, textArea);
            alert.getDialogPane().setContent(dialogPaneContent);
        } else {
            alert.setContentText(contentText);
        }
        alert.showAndWait();
    }

    private static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String s = sw.toString();
        return s;
    }

    public static boolean createDirectory(Path path, Exception exception) {
        boolean create = false;
        try {
            if (Files.exists(Files.createDirectory(path))) {
                create = true;
            }
        } catch (IOException e) {
            if (exception != null) exception = e;
        } finally {
            return create;
        }
    }

//При обновлении списка файлов в каталоге занимает и блокирует его. Дальнейшая работа
//с каталогом проблемна - его невозможно удалить, скопировать с тем же именем и т.д.
//    public static List<MyFileDescriptor> getFileList(String path, String root) {
//        List<MyFileDescriptor> data = new ArrayList<>();
//        if (!path.equals(root)) {
//            data.add(new MyFileDescriptor("[ .. ]", 0, false, path));
//        }
//        try {
//            Files.list(Paths.get(path)).filter(Files::isDirectory).forEach(o -> data.add(new MyFileDescriptor(o)));
//            Files.list(Paths.get(path)).filter(Files::isRegularFile).forEach(o -> data.add(new MyFileDescriptor(o)));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return data;
//    }

    public static List<MyFileDescriptor> getFileList(String path, String root) {
        List<MyFileDescriptor> data = new ArrayList<>();
        File dir = new File(path);
        for (File f : dir.listFiles())
                data.add(new MyFileDescriptor(Paths.get(f.getAbsolutePath())));
        data.sort((o1, o2) -> {
            if (o1.isDirectory() && !o2.isDirectory()) {
                return -1;
            } else if (!o1.isDirectory() && o2.isDirectory()) {
                return 1;
            } else {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        });
        if (!path.equals(root)) {
            data.add(0, new MyFileDescriptor("[ .. ]", 0, false, path));
        }
        return data;
    }

    public static void sendFileMsg(AbstractMessage msg, Callback callOnSendMsg) {
        FileMessage fileMessage = (FileMessage) msg;

        String file = fileMessage.getFilename();
        int sizeBuffer = 0;
        try {
            sizeBuffer = (Const.buffer <= Files.size(Paths.get(file))) ? Const.buffer : (int) Files.size(Paths.get(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = new byte[sizeBuffer];

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), sizeBuffer)) {
            int frame;
            boolean first = true;

            while ((frame = in.read(data)) != -1) {
                fileMessage.setParameters(Arrays.copyOf(data, frame), frame, first);
                callOnSendMsg.callback(fileMessage);
                first = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeFileMsg(FileMessage fm, String currentPath) {
        Path path = Paths.get(currentPath, File.separator, ((fm.getPath() != null) ? fm.getPath() + File.separator : ""), fm.getFilename());

        byte[] data = fm.getData();

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(path.toString(), !fm.isFirstPacket()), fm.getSizePacket())) {
            out.write(data);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
