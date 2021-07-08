package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.datapetrinets.io.DPNIOException;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.AbstractModel;
import proposition.PropositionData;
import task.MonitoringTask;
import utils.AutomatonUtils;
import utils.FileUtils;
import utils.LogUtils;
import utils.ModelUtils;
import utils.enums.MonitoringState;

public class MonitoringViewController {

	@FXML
	private VBox settingsPanel;
	@FXML
	private Label eventLogLabel;
	@FXML
	private TableView<AbstractModel> modelTableView;
	@FXML
	private TableColumn<AbstractModel, String> modelNameColumn;
	@FXML
	private TableColumn<AbstractModel, String> modelTypeColumn;
	@FXML
	private TableColumn<AbstractModel, Number> modelCostColumn;
	@FXML
	private TableColumn<AbstractModel, AbstractModel> modelRemoveColumn;
	@FXML
	private Button startMonitoringButton;
	@FXML
	private ListView<String> tracesListView;
	@FXML
	private ScrollPane resultsPane;

	private Stage stage;
	private int defaultViolationCost = 5;
	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	//Data structures
	PropositionData propositionData = new PropositionData();
	ExecutableAutomaton globalAutomaton;
	Map<State, Map<AbstractModel, MonitoringState>> globalAutomatonColours;
	Map<State, Integer> costCurrMap;
	Map<State, Integer> costBestMap;

	List<VBox> resultsList;

	private XLog xlog;

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@FXML
	private void initialize() {
		modelTableView.setPlaceholder(new Label("No process specifications selected"));
		modelNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		modelNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelName()));
		modelTypeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		modelTypeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getModelType().toString()));
		modelCostColumn.setCellFactory(col -> new IntegerEditingCell());
		modelCostColumn.setCellValueFactory(data -> data.getValue().violationCostProperty());
		modelRemoveColumn.setCellValueFactory(
				param -> new ReadOnlyObjectWrapper<AbstractModel>(param.getValue())
				);
		modelRemoveColumn.setCellFactory(param -> new TableCell<AbstractModel, AbstractModel>() {
			private final Button removeButton = new Button("Remove");

			@Override
			protected void updateItem(AbstractModel item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null) {
					setGraphic(null);
					return;
				}

				setGraphic(removeButton);
				removeButton.setOnAction(
						event -> getTableView().getItems().remove(item)
						);
			}
		});

		tracesListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
			if (newIndex.intValue() >= 0) {
				resultsPane.setContent(resultsList.get(newIndex.intValue()));
			} else {
				resultsPane.setContent(null);
			}
		});
	}

	@FXML
	private void selectLog() {
		File logFile = FileUtils.showLogOpenDialog(stage);
		if (logFile != null) {
			eventLogLabel.setText(logFile.getAbsolutePath());
			xlog = LogUtils.convertToXlog(logFile.getAbsolutePath());
		}
	}

	@FXML
	private void addModel() {
		List<File> modelFiles = FileUtils.showModelOpenDialog(stage);
		if (modelFiles != null) {
			List<AbstractModel> abstractModels = new ArrayList<AbstractModel>();
			for (File modelFile : modelFiles) {
				String modelName = modelFile.getName();
				try {
					String modelExtension = modelName.substring(modelName.lastIndexOf(".")+1);
					if ("decl".equalsIgnoreCase(modelExtension)) {
						abstractModels.add(ModelUtils.loadDeclareModel(modelFile.toPath(), modelName, defaultViolationCost));
					} else if ("ltl".equalsIgnoreCase(modelExtension)) {
						abstractModels.add(ModelUtils.loadLtlModel(modelFile.toPath(), modelName, defaultViolationCost));
					} else if ("pnml".equalsIgnoreCase(modelExtension)) {
						abstractModels.add(ModelUtils.loadDpnModel(modelFile.toPath(), modelName, defaultViolationCost));
					} else {
						System.err.println("Skipping model of unknown type: " + modelExtension);
					}
				} catch (DPNIOException | IOException | IndexOutOfBoundsException e) {
					System.err.println("Unable to load model: " + modelFile.getAbsolutePath());
					e.printStackTrace();
				}
			}
			modelTableView.getItems().addAll(abstractModels);
		}
	}

	@FXML
	private void startMonitoring() {
		settingsPanel.setDisable(true);
		createMonitoringDataStructures();

		tracesListView.getItems().clear();
		resultsList = new ArrayList<VBox>();

		monitorNextTrace();
	}

	private void monitorNextTrace() {
		if (resultsList.size() < xlog.size()) {
			MonitoringTask monitoringTask = new MonitoringTask(xlog.get(resultsList.size()), modelTableView.getItems(), propositionData, globalAutomaton, globalAutomatonColours, costCurrMap, costBestMap);

			monitoringTask.setOnSucceeded(event -> {
				resultsList.add(monitoringTask.getValue());
				tracesListView.getItems().add(XConceptExtension.instance().extractName(xlog.get(resultsList.size()-1)));
				monitorNextTrace();
			});

			monitoringTask.setOnFailed(event -> {
				resultsList.add(new VBox(new Label("Error getting trace results")));
				tracesListView.getItems().add(XConceptExtension.instance().extractName(xlog.get(resultsList.size()-1)));
				monitorNextTrace();
			});
			executorService.execute(monitoringTask);

		} else {
			settingsPanel.setDisable(false);
		}
	}

	private void createMonitoringDataStructures() {
		//TODO: Avoid code duplication (following code is duplicated in MainCmd.java)
		System.out.println("Start: Populating propositionalization data structure");
		for (AbstractModel processModel : modelTableView.getItems()) {
			propositionData.addActivities(processModel);
			propositionData.addAttributePropositions(processModel);
		}
		System.out.println("Done: Populating propositionalization data structure\n");


		//Creating a propositionalized automaton of each input model
		System.out.println("Start: Creating a propositionalized automaton of each input model");
		for (AbstractModel processModel : modelTableView.getItems()) {
			processModel.initializeAutomaton(propositionData);
		}
		System.out.println("Done: Creating a propositionalized automaton of each input model\n");


		System.out.println("Start: Creating the global automaton");
		globalAutomaton = AutomatonUtils.createGlobalAutomaton(modelTableView.getItems());
		System.out.println("Done: Creating the global automaton\n");

		System.out.println("Start: Calculating colors for each state of the global automaton");
		globalAutomatonColours = AutomatonUtils.getGlobalAutomatonColours(modelTableView.getItems(), globalAutomaton);
		System.out.println("Done: Calculating colors for each state of the global automaton\n");

		System.out.println("Start: Calculating cost_curr and cost_best values for each state of the global automaton");
		costCurrMap = AutomatonUtils.getCostCurrMap(modelTableView.getItems(), globalAutomaton, globalAutomatonColours);
		costBestMap = AutomatonUtils.getCostBestMap(globalAutomaton, costCurrMap);
		System.out.println("Done: Calculating cost_curr and cost_best values for each state of the global automaton\n");
	}


	private class IntegerEditingCell extends TableCell<AbstractModel, Number> {
		//https://stackoverflow.com/questions/27900344/how-to-make-a-table-column-with-integer-datatype-editable-without-changing-it-to
		private final TextField textField = new TextField();
		private final Pattern intPattern = Pattern.compile("\\d+");

		public IntegerEditingCell() {
			textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
				if (! isNowFocused) {
					processEdit();
				}
			});
			textField.setOnAction(event -> processEdit());
		}

		private void processEdit() {
			String text = textField.getText();
			if (intPattern.matcher(text).matches()) {
				commitEdit(Integer.parseInt(text));
			} else {
				cancelEdit();
			}
		}

		@Override
		public void updateItem(Number value, boolean empty) {
			super.updateItem(value, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else if (isEditing()) {
				setText(null);
				textField.setText(value.toString());
				setGraphic(textField);
			} else {
				setText(value.toString());
				setGraphic(null);
			}
		}

		@Override
		public void startEdit() {
			super.startEdit();
			Number value = getItem();
			if (value != null) {
				textField.setText(value.toString());
				setGraphic(textField);
				setText(null);
			}
		}

		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setText(getItem().toString());
			setGraphic(null);
		}

		// This seems necessary to persist the edit on loss of focus; not sure why:
		@Override
		public void commitEdit(Number value) {
			super.commitEdit(value);
			((AbstractModel)this.getTableRow().getItem()).setViolationCost(value.intValue());
		}
	}
}
