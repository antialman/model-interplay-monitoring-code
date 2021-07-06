package controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.AbstractModel;
import utils.enums.MonitoringState;

public class TraceVisualisationController {

	@FXML
	private HBox costCurrHbox;
	@FXML
	private HBox costBestHbox;
	@FXML
	private HBox globalStateHbox;
	@FXML
	private VBox modelsMonitoringVbox;
	@FXML
	private HBox eventLabelHbox;
	@FXML
	private VBox debugVbox;

	private MonitoringState globalState;
	private Map<AbstractModel, MonitoringState> modelStates = new HashMap<AbstractModel, MonitoringState>();
	private Map<AbstractModel, HBox> modelVisualisations = new HashMap<AbstractModel, HBox>();

	@FXML
	private void initialize() {
		costCurrHbox.getChildren().clear();
		costBestHbox.getChildren().clear();
		globalStateHbox.getChildren().clear();
		modelsMonitoringVbox.getChildren().clear();
		eventLabelHbox.getChildren().clear();
		debugVbox.getChildren().clear();
	}
	
	public void setModels(List<AbstractModel> processModels) {
		for (AbstractModel processModel : processModels) {
			Label modelNameLabel = new Label(processModel.getModelName());
			modelNameLabel.getStyleClass().add("monitor-label");
			modelsMonitoringVbox.getChildren().add(modelNameLabel);
			modelStates.put(processModel, null);
			HBox modelStateHbox = new HBox();
			modelsMonitoringVbox.getChildren().add(modelStateHbox);
			modelVisualisations.put(processModel, modelStateHbox);
		}
	}
	
	public void addModelState(AbstractModel processModel, MonitoringState modelState) {
		Label monitoringTile = new Label();
		monitoringTile.getStyleClass().add("monitor-tile");
		switch (modelState) {
		case POSS_SAT:
			monitoringTile.getStyleClass().add("monitor-tile__poss-sat");
			break;
		case POSS_VIOL:
			monitoringTile.getStyleClass().add("monitor-tile__poss-viol");
			break;
		case SAT:
			monitoringTile.getStyleClass().add("monitor-tile__perm-sat");
			break;
		case VIOL:
			monitoringTile.getStyleClass().add("monitor-tile__perm-viol");
			break;
		default:
			break;
		}

		if (modelState != modelStates.get(processModel)) {
			monitoringTile.setText(modelState.getMobuconltlName());
			modelStates.put(processModel, modelState);
		}
		modelVisualisations.get(processModel).getChildren().add(monitoringTile);
	}

	public void addCostCurrValue(Integer costCurrValue) {
		Label costCurrLabel = new Label(costCurrValue.toString());
		costCurrLabel.getStyleClass().add("monitor-cost");
		costCurrHbox.getChildren().add(costCurrLabel);
	}

	public void addCostBestValue(Integer costBestValue) {
		Label costCurrLabel = new Label(costBestValue.toString());
		costCurrLabel.getStyleClass().add("monitor-cost");
		costBestHbox.getChildren().add(costCurrLabel);
	}

	public void addGlobalState(MonitoringState globalState) {
		Label monitoringTile = new Label();
		
		monitoringTile.getStyleClass().add("monitor-tile");
		switch (globalState) {
		case POSS_SAT:
			monitoringTile.getStyleClass().add("monitor-tile__poss-sat");
			break;
		case POSS_VIOL:
			monitoringTile.getStyleClass().add("monitor-tile__poss-viol");
			break;
		case SAT:
			monitoringTile.getStyleClass().add("monitor-tile__perm-sat");
			break;
		case VIOL:
			monitoringTile.getStyleClass().add("monitor-tile__perm-viol");
			break;
		default:
			break;
		}

		if (globalState != this.globalState) {
			monitoringTile.setText(globalState.getMobuconltlName());
			this.globalState = globalState;
		}
		globalStateHbox.getChildren().add(monitoringTile);
	}
	
	public void addEventLabel(String eventString) {
		Label eventLabel = new Label(eventString);
		eventLabel.getStyleClass().add("monitor-event-name");
		eventLabelHbox.getChildren().add(new Group(eventLabel));
	}

	public void addDebugMessage (String message) {
		debugVbox.getChildren().add(new Label(message));
	}
}
