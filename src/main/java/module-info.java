module com.freyja.bg3savemanager {
	requires javafx.controls;
	requires javafx.fxml;

	requires org.controlsfx.controls;
	requires java.desktop;
	requires javafx.swing;

	opens com.freyja.bg3savemanager to javafx.fxml;
	exports com.freyja.bg3savemanager;
}