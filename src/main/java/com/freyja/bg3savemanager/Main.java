package com.freyja.bg3savemanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;

import static javafx.embed.swing.SwingFXUtils.toFXImage;

public class Main extends Application {

	// Larian APPDATA folder containing the save data for the game.
	String saveFolder = System.getenv("LOCALAPPDATA") + "\\Larian Studios\\Baldur's Gate 3\\PlayerProfiles\\Public\\Savegames\\Story";
	// Local folder where the saves will be copied to.
	String backupFolder = System.getProperty("user.dir") + "\\saves\\";

	@FXML
	// ScrollPane containing the save data list.
	ScrollPane sp;

	@FXML
	// ImageView for the save data preview image.
	ImageView imagePreview;

	@FXML
	// currentSavesBtn			= Display list of saves in the Larian APPDATA folder.
	// backedSavesBtn			= Display list of backed up saves.
	// archiveAll				= Backs up all saves in the Larian APPDATA folder.
	// archiveSelected			= Backs up the currently selected save.
	// importSelected			= Copy the selected save data from the backup folder to the actual game save directory.
	// importAll				= Copy all save data from the backup folder to the actual game save directory.
	// deleteSelected			= Remove the currently selected save data from the backup folder.
	Button currentSavesBtn, backedSavesBtn, archiveAll, archiveSelected, importSelected, importAll, deleteSelected;

	@FXML
	// title					= Shows name of the save data.
	// status					= Shows messages about the outcome of functions.
	Label title, status;

	@FXML
	// importing				= HBox containing importSelected, importAll, and deleteSelected.
	// exporting				= HBox containing archiveAll and archiveSelected.
	HBox pane, importing, exporting;

	/*
	 * copyDirectory
	 *
	 * folderName			[String]			Takes in the name of the root folder we are copying.
	 * dest					[Path]				Takes in the path of the destination folder we are copying data to.
	 * target				[Path]				Takes in the path of the target folder we are taking data from.
	 *
	 * The function walks the path provided and copies them all to a local save directory of the same name.
	 *
	 */
	public void copyDirectory(String folderName, Path dest, Path target) throws IOException {
		// New location
		Path dir = Paths.get(dest + "\\" + folderName);
		// Create the save location if not exists.
		if (!Files.exists(dest)) {
			// Create
			Files.createDirectories(dest);
		}
		// The folder + the item we've selected.
		Path pathToWalk = Paths.get(target + "\\" + folderName);

		//Begin walk
		Files.walk(pathToWalk).forEach(source -> {
					// If the path of the file or directory found matches the one we're provided, stop.
					// This is because the program will get stuck trying to add itself.
					if (source != pathToWalk) {
						try {
							// Determine the relative path of the source file to the source directory
							Path relativePath = pathToWalk.relativize(source);

							// Construct the destination file path in the target directory
							Path destination = dir.resolve(relativePath);

							// Ensure the parent directory exists before copying
							Files.createDirectories(destination.getParent());

							// Perform the copy
							Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException e) {
							Platform.runLater(() -> {
								status.setText("One or more items already exist\nand have not been rewritten.");
							});}
					}
				});
	}

	/*
	 * deleteDir
	 *
	 * file					[File]			Takes in the File we are deleting.
	 *
	 * The function traverses the directory provided, calling itself on each new file in order to delete it.
	 * When finally empty, the function deletes the parent folder.
	 *
	 */
	void deleteDir(File file) throws IOException {
		// Get contents of directory passed
		File[] contents = file.listFiles();
		// While not null...
		if (contents != null) {
			// Read...
			for (File f : contents) {
				// Security check that there are no symbolic links since Java would consider
				// those subfolders and could theoretically destroy your computer.
				// Allegedly.
				if (!Files.isSymbolicLink(f.toPath())) {
					// Call
					deleteDir(f);
				}
			}
		}
		// Delete parent.
		Files.delete(file.toPath());
	}

	/*
	 * handleButtonClick
	 *
	 * e					[ActionEvent]			Takes in the circumstances for the call.
	 *
	 * This function is called upon clicking any buttons in the program.
	 *
	 * The switch buttons simply swap which HBox is visible, importing or exporting.
	 * The archive buttons will call copyDirectory where dest=backup folder and target=larian.
	 * The import buttons will call copyDirectory where dest=larian folder and target=backup folder.
	 *
	 */
	public void handleButtonClick(ActionEvent e) {
		// If archiveAll...
		if (e.getSource() == archiveAll) {
			// Launch async thread so the program doesn't freeze.
			new Thread(() -> {
				// Try to copy data...
				try {
					// folderName is blank so the parent directory will just be larian
					// dest=backup folder and target=larian
					copyDirectory("", Path.of(backupFolder), Path.of(saveFolder));
					// If it makes it here, status = complete.
					Platform.runLater(() -> status.setText("Complete!"));
				} catch (IOException ex) {
					// createDirectories can cause an IOException in the event that the path is inaccessible.
					Platform.runLater(() -> status.setText("Failed!"));
				}
			}).start();
		} else if (e.getSource() == archiveSelected) {
			new Thread(() -> {
				try {
					// title doubles as a global pass-through variable. How handy.
					// dest=backup folder and target=larian
					copyDirectory(title.getText(), Path.of(backupFolder), Path.of(saveFolder));
					// If it makes it here, status = complete.
					Platform.runLater(() -> status.setText("Complete!"));
				} catch (IOException ex) {
					// createDirectories can cause an IOException in the event that the path is inaccessible.
					Platform.runLater(() -> status.setText("Failed!"));
				}
			}).start();
		} else if (e.getSource() == importSelected) {
			new Thread(() -> {
				try {
					// Same deal as archiveSelected except dest and target params are reversed.
					// Once again, handy dandy global title helps out.
					copyDirectory(title.getText(), Path.of(saveFolder), Path.of(backupFolder));
					// If it makes it here, status = complete.
					Platform.runLater(() -> status.setText("Complete!"));
				} catch (IOException ex) {
					// createDirectories can cause an IOException in the event that the path is inaccessible.
					Platform.runLater(() -> status.setText("Failed!"));
				}
			}).start();
		} else if (e.getSource() == importAll) {
			new Thread(() -> {
				try {
					// Same deal as archiveAll except dest and target params are reversed.
					copyDirectory("", Path.of(saveFolder), Path.of(backupFolder));
					// If it makes it here, status = complete.
					Platform.runLater(() -> status.setText("Complete!"));
				} catch (IOException ex) {
					// createDirectories can cause an IOException in the event that the path is inaccessible.
					Platform.runLater(() -> status.setText("Failed!"));
				}
			}).start();
		} else if (e.getSource() == deleteSelected) {
			new Thread(() -> {
				try {
					deleteDir(new File(backupFolder + "\\" + title.getText()));
					// If it makes it here, status = complete.
					// Also, reload contents of backup folder using loadSaves.
					Platform.runLater(() -> {
						status.setText("Complete!");
						loadSaves(backupFolder);
					});				} catch (IOException ex) {
					// createDirectories can cause an IOException in the event that the path is inaccessible.
					Platform.runLater(() -> status.setText("Failed!"));
				}

			}).start();
		} else if (e.getSource() == currentSavesBtn) {
			// Diplay list of backup saves.
			loadSaves(backupFolder);

			// Switches buttons over to the import options.
			currentSavesBtn.setVisible(false);
			exporting.setVisible(false);
			importing.setVisible(true);
			backedSavesBtn.setVisible(true);

		} else if (e.getSource() == backedSavesBtn) {
			// Diplay list of larian folder saves.
			loadSaves(saveFolder);

			// Switches buttons over to the export options.
			currentSavesBtn.setVisible(true);
			exporting.setVisible(true);
			importing.setVisible(false);
			backedSavesBtn.setVisible(false);

		}
	}

	/*
	 * convertWebPToImage
	 *
	 * path					[String]			Path to webp file
	 *
	 * Converts webp to BufferedImage then converts it to JavaFX Image data type.
	 * This function is provided by the org.sejda.imageio.webp-imageio GitHub and was not written entirely by me.
	 *
	 */
	public Image convertWebPToImage(String path) throws IOException {
		// Use webp-imageio library to read the WebP image
		BufferedImage p = ImageIO.read(new File(path));
		return toFXImage(p, null);
	}

	/*
	 * loadSaves
	 *
	 * saveDir					[String]			Path to the folder we're reading.
	 *
	 * This functions simply reads every subfolder in a dir to a ScrollPane as Button objects.
	 *
	 */
	public void loadSaves(String saveDir) {
		// Define the VBox to hold this data
		// Later added to the ScrollPane
		VBox content = new VBox();
		// Pixel spacing...
		content.setSpacing(5);
		// Set width to equal the ScrollPane that the Vbox will be inside.
		content.prefWidthProperty().bind(sp.widthProperty());
		// Set height to equal the ScrollPane that the Vbox will be inside.
		content.prefHeightProperty().bind(sp.heightProperty());

		// Using FileFilter in order to only grab directories, files are ignored.
		FileFilter directoryFileFilter = File::isDirectory;
		// Save dir names to File array.
		File[] saves = new File(saveDir).listFiles(directoryFileFilter);
		// Sort by modification date.
		Arrays.sort(saves, Comparator.comparingLong(File::lastModified));

		// For each dir found...
		for (File save : saves) {
			// Define button with the name of the save...
			Button saveBtn = new Button(save.getName() + " ");
			// Set button width equal to that of the ScrollPane
			saveBtn.setMinWidth(320);
			// Center...
			saveBtn.setAlignment(Pos.CENTER_LEFT);
			// Setting button click action
			saveBtn.setOnAction(e -> {
				// Change title placeholder to the selected items name.
				// This is used to pass the selected save name between multiple functions.
				title.setText(save.getName());
				// On click we reset the status message.
				status.setText("");
				// Try to display the associated image file.
				String folderName = save.getName().replaceAll("^.*?__", "");
				if (!folderName.isEmpty()) {
					String imagePath = save.getAbsolutePath() + "\\" + folderName + ".WebP";
					try {
						imagePreview.setImage(convertWebPToImage(imagePath));
					} catch (IOException ex) {
						status.setText("Image does not exist.");
					}
				} else {
					status.setText("Invalid folder name for image preview.");
				}
			});
			// Add to the VBox
			content.getChildren().add(saveBtn);
		}
		// Add completed VBox to the ScrollPane
		sp.setContent(content);
	}

	@FXML
	// Built in protected JavaFX scene init function
	protected void initialize() throws IOException {
		// Checks if the save folder has been created yet
		if (!Files.exists(Paths.get(backupFolder))) {
			// If not, make it.
			Files.createDirectories(Paths.get(backupFolder));
		}
		// Checks if the save folder has been created yet
		if (!Files.exists(Paths.get(saveFolder))) {
			// If not, make it.
			Files.createDirectories(Paths.get(saveFolder));
		}
		// Prettification of stuff
		status.setAlignment(Pos.CENTER);
		status.setTextAlignment(TextAlignment.CENTER);
		title.setAlignment(Pos.CENTER);
		title.setFont(Font.font("", FontWeight.BOLD, 16));

		// Binds the spacing property to the nodes visibility.
		// This makes it so currentSavesBtn, backedSavesBtn, exporting, and importing will not take up space in the window when they are not visible.
		currentSavesBtn.managedProperty().bind(currentSavesBtn.visibleProperty());
		backedSavesBtn.managedProperty().bind(backedSavesBtn.visibleProperty());
		exporting.managedProperty().bind(exporting.visibleProperty());
		importing.managedProperty().bind(importing.visibleProperty());

		// Display the larian saves folder in sp
		loadSaves(saveFolder);
	}

	@Override
	public void start(Stage stage) throws IOException {
		System.out.println(saveFolder);
		FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main.fxml"));
		Scene scene = new Scene(fxmlLoader.load());
		stage.setTitle("Baldurs Gate 3 Save Manager");
		stage.getIcons().add(new Image(Main.class.getResourceAsStream("revivify.png")));
		scene.getStylesheets().add(String.valueOf(Main.class.getResource("style.css")));
		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
		launch();
	}
}