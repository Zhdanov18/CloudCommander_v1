import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainClient extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("CloudCommander");
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
//        primaryStage.setResizable(false);
        primaryStage.getIcons().add(Const.appIcon);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
