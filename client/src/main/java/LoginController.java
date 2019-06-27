import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginController {
    @FXML
    TextField login;

    @FXML
    PasswordField password;

    @FXML
    VBox loginWindow;

    public int id;

    public void auth(ActionEvent actionEvent) {
        Network.sendMsg(new Request(Const.Action.AUTHORIZATION, login.getText(), password.getText()));
        loginWindow.getScene().getWindow().hide();
    }
}
