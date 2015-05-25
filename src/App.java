import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;


public class App extends Application{

	ObservableList<StupidObject>items;

	@Override
	public void start(Stage stage) throws Exception {
		items = FXCollections.observableArrayList(new StupidObject(), new StupidObject(), new StupidObject(), new StupidObject());
		

		VBox leftBox = new VBox();
		leftBox.getChildren().add(makeFileList());
		
		VBox centerBox = new VBox();
		
		BorderPane pane = new BorderPane();
		pane.getChildren().add(makeMenuBar());
		pane.setCenter(centerBox);
		pane.setLeft(leftBox);
				
		stage.setScene(new Scene(pane, 1024, 480));
		stage.setTitle("Crazy Beautiful Honours Project");
		stage.show();
	}
	
	public MenuBar makeMenuBar() {
		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu("File");
		MenuItem item = new MenuItem("Load Cube");
		item.setOnAction(e -> items.add(new StupidObject()));
		fileMenu.getItems().add(item);
		menuBar.getMenus().add(fileMenu);
		menuBar.setUseSystemMenuBar(true);
		
		return menuBar;
	}
	
	public ListView<StupidObject> makeFileList() {
		ListView<StupidObject>list = new ListView<StupidObject>();
//		items = FXCollections.observableArrayList("Single", "Double", "Suite", "Family App");
		
		list.setItems(items);
		list.setPrefWidth(200);
		list.setPrefHeight(480);
		list.setEditable(true);
		return list;
	}
	
	class StupidObject extends Object {
		public String toString() {
			return "studpp";
		}
	}
	public static void main(String[]args) {
		launch(args);
	}
}
