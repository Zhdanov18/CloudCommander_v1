import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Paths;

public class CreateController {
    public int id;

    private Const.Node node;
    private String path;
    private String folderName;
    private Exception exception;
    private boolean result;
    private boolean choiceYes;

    @FXML
    TextField folder;

    @FXML
    VBox createWindow;

    public String getPath() { return path; }
    public String getFolder() { return folderName; }
    public Exception getException() { return exception; }
    public boolean getResult() { return result; }
    public boolean isChoiceYes() { return choiceYes; }

    public void create(ActionEvent actionEvent) {
        folderName = folder.getText().trim();
        choiceYes = true;
        switch (node) {
            case CLIENT:
                this.exception = new Exception();

                if (ActionHandler.createDirectory(Paths.get(path, File.separator, folderName), exception)) {
                    this.result = true;
                }
                this.path = String.join(File.separator, path, folderName);
                break;
            case SERVER:
                Network.sendMsg(new Request(Const.Action.CREATE, null, folderName));
                break;
        }
        createWindow.getScene().getWindow().hide();
    }

    public void cancel(ActionEvent actionEvent) {
        createWindow.getScene().getWindow().hide();
    }

    public void receiveData(Const.Node node, String path) {
        this.node = node;
        this.path = path;
        this.result = false;
        this.choiceYes = false;
    }
}
