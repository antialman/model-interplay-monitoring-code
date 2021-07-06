package controller;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import model.AbstractModel;

public class ModelCell extends ListCell<AbstractModel> {

	@FXML
	private HBox rootRegion;
	@FXML
	private Label modelNameLabel;
	@FXML
	private Label modelTypeLabel;
	@FXML
	private TextField violationCostField;
	
	private FXMLLoader loader;
	
	@Override
	protected void updateItem(AbstractModel item, boolean empty) {
		//https://openjfx.io/javadoc/11/javafx.controls/javafx/scene/control/Cell.html#updateItem(T,boolean)
		super.updateItem(item, empty);
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
		} else {
			if (loadFxml()) {
				modelNameLabel.setText(item.getModelName());
				modelTypeLabel.setText(item.getModelType().toString());
				violationCostField.setText(Integer.toString(item.getViolationCost()));

				setText(null);
				setGraphic(rootRegion);
				//logger.debug("Updated event cell to item: " + item.toString());
			}
		}
	}
	
	private boolean loadFxml() {
		if (loader == null) {
			//Load ActionCell contents if not already loaded
			loader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/ModelCell.fxml"));
			loader.setController(this);
			try {
				loader.load();
				return true;
			} catch (IOException | IllegalStateException e) {
				return false;
			}
		} else {
			return true;
		}
	}
	
}
