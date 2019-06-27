import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private enum Direction { PARENT, CHILD, CREATE, REMOVE, NONE}

    private String nick;
    private Const.Node node;

    private String serverPath;
    private String clientPath;
    private int serverIndex;
    private int clientIndex;
    private MyFileDescriptor serverItem;
    private MyFileDescriptor clientItem;
    private Direction direction;

    private Image folderIcon  = new Image("img\\folder16.png");
    private Image arrowIcon   = new Image("img\\arrow.png");
    private Image onlineIcon  = new Image("img\\online.png");
    private Image offlineIcon = new Image("img\\offline.png");

    @FXML
    ImageView btnLoginImage;

    @FXML
    ListView<MyFileDescriptor> serverList;

    @FXML
    ListView<MyFileDescriptor> clientList;

    @FXML
    Label lblClientPath;

    @FXML
    Label lblServerPath;

    @FXML
    Label lblClientFileInfo;

    @FXML
    Label lblServerFileInfo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.clientPath = Const.userLocalHomeDir;
        this.node       = Const.Node.CLIENT;

        Network.start();

        serverList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        serverList.focusedProperty().addListener( (obs, oldValue, newValue) -> { node = Const.Node.SERVER; });
        serverList.setCellFactory(param -> new ListCell<MyFileDescriptor>() {
            private ImageView imageView = new ImageView();
            @Override
            public void updateItem(MyFileDescriptor myFileDescriptor, boolean empty) {
                super.updateItem(myFileDescriptor, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (myFileDescriptor.isDirectory()) {
                        imageView.setImage(folderIcon);
                        setGraphic(imageView);
                    } else if (myFileDescriptor.isParent()) {
                        imageView.setImage(arrowIcon);
                        setGraphic(imageView);
                    } else {
                        setGraphic(null);
                    }
                    setText(myFileDescriptor.getName());
                }
            }
        });
        serverList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent click) {
                updateParameters();
                refreshLabels();
                if (click.getClickCount() == 2) {
                    if (getItem(serverList).isParent()) {
                        setDirection(Direction.PARENT);
                        Network.sendMsg(new Request(Const.Action.LIST, nick, null));
                    } else if (getItem(serverList).isDirectory()) {
                        setDirection(Direction.CHILD);
                        Network.sendMsg(new Request(Const.Action.LIST, nick, getItem(serverList).getName()));
                    }
                }
            }
        });

        clientList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        clientList.focusedProperty().addListener( (obs, oldValue, newValue) -> { node = Const.Node.CLIENT; });
        clientList.setCellFactory(serverList.getCellFactory());
        clientList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent click) {
                updateParameters();
                refreshLabels();
                if (click.getClickCount() == 2) {
                    if (getItem(clientList).isParent()) {
                        clientPath = Paths.get(clientPath).getParent().toString();
                        setDirection(Direction.PARENT);
                        refreshClientList();
                    } else {
                        Path filename = Paths.get(clientPath, File.separator, getItem(clientList).getName());
                        if (getItem(clientList).isDirectory() && Files.exists(filename)) {
                            clientPath = filename.toString();
                            setDirection(Direction.CHILD);
                            refreshClientList();
                        }
                    }
                }
            }
        });
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        ActionHandler.writeFileMsg((FileMessage) am, clientPath);
                        setDirection(Direction.NONE);
                        refreshClientList();
                    }
                    if (am instanceof AuthMessage) {
                        AuthMessage authMessage = (AuthMessage) am;
                        String name = authMessage.getName();
                        if (name != null) {
                            this.nick = name;
                            btnLoginImage.setImage(onlineIcon);
                            setDirection(Direction.CHILD);
                            Network.sendMsg(new Request(Const.Action.LIST, null, null));
                        }
                    }
                    if (am instanceof FolderMessage) {
                        FolderMessage fm = (FolderMessage) am;
                        refreshServerList(fm);
                        serverPath = fm.getPath();
                    }
                    if (am instanceof ErrorMessage) {
                        ErrorMessage em = (ErrorMessage) am;
                        updateUI(() -> {
                            ActionHandler.errorMsg(em.getAlertText(), getRemotePathForDisplay() + File.separator + em.getContentText(), em.getException());
                        });
                    }
                    if (am instanceof Request) {
                        Request request = (Request) am;
                        switch (request.getAction()) {
                            case CREATE:
                                Path path = Paths.get(clientPath, File.separator, ((request.getArg(0) != null) ? request.getArg(0) + File.separator : ""), request.getArg(1));
                                if (ActionHandler.createDirectory(path, null)) {
                                    setDirection(Direction.CREATE);
                                    refreshClientList();
                                }
                                break;
                        }
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();
        setDirection(Direction.CHILD);
        refreshClientList();
    }

    public void btnLogin(ActionEvent actionEvent) {
        if (nick == null) {
            try {
                Stage stage = new Stage();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
                Parent root = loader.load();
                LoginController lc = (LoginController) loader.getController();
                lc.id = 100;
                stage.setTitle("Authorization");
                stage.setScene(new Scene(root, 400, 200));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.getIcons().add(Const.appIcon);
                stage.setResizable(false);
                stage.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (ActionHandler.confirmMsg(ActionHandler.AlertText.DISCONNECT, nick)) {
                Network.sendMsg(new Request(Const.Action.DISCONNECT, this.nick, null));
                btnLoginImage.setImage(offlineIcon);
                this.nick = null;
                serverList.getItems().clear();
                refreshLabels();
            }
        }
    }

    public void btnCopy(ActionEvent actionEvent) {
        direction = Direction.NONE;
        switch (node) {
            case SERVER:
                if (nick == null || getItem(serverList) == null || getItem(serverList).isParent()) { return; }
                if (confirmOverwrite()) {
                    Network.sendMsg(new Request(Const.Action.COPY, null, getItem(serverList).getName()));
                }
                break;
            case CLIENT:
                if (getItem(clientList) == null || getItem(clientList).isParent()) { return; }
                Path path = Paths.get(clientPath, File.separator, getItem(clientList).getName());
                try {
                    if (confirmOverwrite()) {
                        Files.walkFileTree(path, new MyFileVisitor(Const.Action.COPY, path,
                                args -> Network.sendMsg((AbstractMessage) args[0])));
                    }
                } catch (IOException e) {
                    ActionHandler.errorMsg(ActionHandler.AlertText.COPY_ERR, path.toString(), e);
                }
                break;
        }
    }

    public void btnCreate(ActionEvent actionEvent) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("create.fxml"));
            Parent root = loader.load();
            CreateController cc = (CreateController) loader.getController();
            cc.id = 100;
            cc.receiveData(node, (node == Const.Node.CLIENT) ? clientPath : serverPath);
            stage.setTitle("Create directory");
            stage.setScene(new Scene(root, 400, 200));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(Const.appIcon);
            stage.setResizable(false);
            stage.showAndWait();
            if (node == Const.Node.CLIENT) {
                if (cc.getResult()) {
                    clientItem.setName(cc.getFolder());
                    setDirection(Direction.CREATE);
                    refreshClientList();
                } else if (cc.isChoiceYes()){
                    ActionHandler.errorMsg(ActionHandler.AlertText.CREATE_DIR_ERR, cc.getPath(), null);
                }
            } else {
                serverItem.setName(cc.getFolder());
                setDirection(Direction.CREATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnRemove(ActionEvent actionEvent) {
        switch (node) {
            case SERVER:
                if (nick == null || getItem(serverList) == null || getItem(serverList).isParent()) { return; }
                if (ActionHandler.confirmMsg(ActionHandler.AlertText.DELETE, String.join(File.separator, getRemotePathForDisplay(), getItem(serverList).getName()))) {
                    setDirection(Direction.REMOVE);
                    Network.sendMsg(new Request(Const.Action.REMOVE, null, getItem(serverList).getName()));
                }
                break;
            case CLIENT:
                if (getItem(clientList) == null || getItem(clientList).isParent()) { return; }
                Path path = Paths.get(String.join(File.separator, clientPath, getItem(clientList).getName()));
                if (ActionHandler.confirmMsg(ActionHandler.AlertText.DELETE, String.join(File.separator, clientPath, getItem(clientList).getName()))) {
                    try {
                        Files.walkFileTree(path, new MyFileVisitor(Const.Action.REMOVE, path, null));
                    } catch (IOException e) {
                        ActionHandler.errorMsg(ActionHandler.AlertText.DELETE_ERR, path.toString(), e);
                    }
                    setDirection(Direction.REMOVE);
                    refreshClientList();
                }
                break;
        }
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void refreshClientList() {
        updateUI(() -> {
            updateParameters();
            clientList.getItems().clear();
            clientList.getItems().addAll(ActionHandler.getFileList(clientPath, Paths.get(clientPath).getRoot().toString().trim()));
            selectItem(clientList, clientIndex, clientItem);
            updateParameters();
            refreshLabels();
            setFocus();
        });
    }

    public void refreshServerList(FolderMessage msg) {
        updateUI(() -> {
            serverList.getItems().clear();
            serverList.getItems().addAll(msg.getData());
            selectItem(serverList, serverIndex, serverItem);
            updateParameters();
            refreshLabels();
            setFocus();
        });
    }

    public void refreshList() {
        if (this.nick == null || node == Const.Node.CLIENT) {
            refreshClientList();
            return;
        }
        Network.sendMsg(new Request(Const.Action.LIST, this.nick, serverPath));
    }

    private void setFocus() {
        switch (node) {
            case CLIENT:
                clientList.requestFocus();
                break;
            case SERVER:
                serverList.requestFocus();
                break;
        }
    }

    private MyFileDescriptor getItem(ListView<MyFileDescriptor> listView) {
        return listView.getSelectionModel().getSelectedItem();
    }

    private void selectItem(ListView<MyFileDescriptor> list, int index, MyFileDescriptor item) {
        switch (direction) {
            case PARENT:
                findItem(list, item.getParent().substring(item.getParent().lastIndexOf(File.separator) + 1));
                break;
            case CHILD:
                list.getSelectionModel().selectFirst();
                break;
            case REMOVE:
                if (index >= list.getItems().size()) {
                    list.getSelectionModel().selectLast();
                } else {
                    list.getSelectionModel().select(index);
                }
                break;
            case CREATE:
                findItem(list, item.getName());
                break;
            case NONE:
                break;
            default:
                break;
        }
        list.scrollTo(list.getSelectionModel().getSelectedIndex());
    }

    private void findItem(ListView<MyFileDescriptor> list, String item) {
        for (MyFileDescriptor d : list.getItems())
            if (d.getName().equals(item))
                list.getSelectionModel().select(list.getItems().indexOf(d));
    }

    private void updateParameters() {
        switch (node) {
            case CLIENT:
                clientIndex = clientList.getSelectionModel().getSelectedIndex();
                clientItem = clientList.getSelectionModel().getSelectedItem();
                break;
            case SERVER:
                serverIndex = serverList.getSelectionModel().getSelectedIndex();
                serverItem = serverList.getSelectionModel().getSelectedItem();
                break;
        }
    }

    private void refreshLabels() {
        lblClientPath.setText(clientPath);
        if (clientItem.isParent() || clientItem.isDirectory()) {
            lblClientFileInfo.setText("");
        } else {
            lblClientFileInfo.setText(String.format("%,d", clientItem.getSize()));
        }
        if (nick != null) {
            lblServerPath.setText(nick + ":" + getRemotePathForDisplay());
            if (serverItem != null) {
                if (serverItem.isParent() || serverItem.isDirectory()) {
                    lblServerFileInfo.setText("");
                } else {
                    lblServerFileInfo.setText(String.format("%,d", serverItem.getSize()));
                }
            }
        } else {
            lblServerPath.setText("");
            lblServerFileInfo.setText("");
        }
    }

    private String getRemotePathForDisplay() {
        if (serverPath.equals(String.join(File.separator, Const.storageRoot, this.nick))) { return ""; }
        return serverPath.substring(serverPath.indexOf(File.separator, Const.storageRoot.length() + nick.length()));
    }

    private boolean confirmOverwrite() {
        switch (node) {
            case SERVER:
                if (!clientList.getItems().contains(getItem(serverList))) { return true; }
                if (ActionHandler.confirmMsg(ActionHandler.AlertText.REWRITE, getItem(serverList).getName())) { return true; }
                break;
            case CLIENT:
                if (!serverList.getItems().contains(getItem(clientList))) { return true; }
                if (ActionHandler.confirmMsg(ActionHandler.AlertText.REWRITE, getItem(clientList).getName())) { return true; }
                break;
        }
        return false;
    }

    private void setDirection(Direction direction) { this.direction = direction; }
}
