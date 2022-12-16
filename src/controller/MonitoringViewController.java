package controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.datapetrinets.io.DPNIOException;
import org.processmining.ltl2automaton.plugins.automaton.State;
import org.processmining.plugins.declareminer.ExecutableAutomaton;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import model.AbstractModel;
import model.ModelType;
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
	private TableView<ScopeSelection> activityScopeTableView;
	@FXML
	private TableColumn<ScopeSelection, String> activityNameColumn;
	@FXML
	private TableColumn<ScopeSelection, Number> activityOverlapsColumn;
	@FXML
	private TableColumn<ScopeSelection, Boolean> activityScopeColumn;
	@FXML
	private TableView<ScopeSelection> attributeScopeTableView;
	@FXML
	private TableColumn<ScopeSelection, String> attributeNameColumn;
	@FXML
	private TableColumn<ScopeSelection, Number> attributeOverlapsColumn;
	@FXML
	private TableColumn<ScopeSelection, Boolean> attributeScopeColumn;
	@FXML
	private Button startMonitoringButton;
	@FXML
	private ListView<String> tracesListView;
	@FXML
	private ScrollPane resultsPane;

	private Stage stage;
	private int defaultViolationCost = 5;
	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	//Data structures to keep track of attribute overlaps and selected scopes (could probably be done better)
	private Map<String, ScopeSelection> attributeNameToScopeSelection = new HashMap<String, ScopeSelection>();
	private Map<String, ScopeSelection> activityNameToScopeSelection = new HashMap<String, ScopeSelection>();
	private ObservableList<ScopeSelection> attributeScopeSelections = FXCollections.observableArrayList();
	private ObservableList<ScopeSelection> activityScopeSelections = FXCollections.observableArrayList();

	//Data structures for monitoring
	PropositionData propositionData;
	ExecutableAutomaton globalAutomaton;
	Map<State, Map<AbstractModel, MonitoringState>> globalAutomatonColours;
	Map<State, Integer> costCurrMap;
	Map<State, Integer> costBestMap;

	//statistics
	long monitoringAutomatonTime;
	List<Long> eventProcessingTimes = new ArrayList<Long>();

	List<VBox> resultsList;

	private XLog xlog;

	public void setStage(Stage stage) {
		this.stage = stage;
	}

	@FXML
	private void initialize() {

		//Setting up the process specifications table
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
						event -> {
							if (item.getModelType() == ModelType.DPN) {
								for (String activityName : item.getActivityNames()) {
									if (activityNameToScopeSelection.get(activityName).getOverlapsCount() == 1) {
										activityScopeSelections.remove(activityNameToScopeSelection.get(activityName));
										activityNameToScopeSelection.remove(activityName);
									} else {
										activityNameToScopeSelection.get(activityName).setOverlapsCount(activityNameToScopeSelection.get(activityName).getOverlapsCount()-1);
									}
								}
								for (String attributeName : item.getVariableTypeMap().keySet()) {
									if (attributeNameToScopeSelection.get(attributeName).getOverlapsCount() == 1) {
										attributeScopeSelections.remove(attributeNameToScopeSelection.get(attributeName));
										attributeNameToScopeSelection.remove(attributeName);
									} else {
										attributeNameToScopeSelection.get(attributeName).setOverlapsCount(attributeNameToScopeSelection.get(attributeName).getOverlapsCount()-1);
									}
								}
							}
							getTableView().getItems().remove(item);
							activityScopeTableView.sort();
							attributeScopeTableView.sort();
						});
			}
		});

		//Setting up DPN activity scope table
		activityScopeTableView.setPlaceholder(new Label("No DPN activity overlaps detected"));
		activityScopeTableView.setItems(activityScopeSelections);
		activityScopeTableView.getSortOrder().add(activityOverlapsColumn);
		activityNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		activityNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getItemName()));
		activityOverlapsColumn.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
		activityOverlapsColumn.setCellValueFactory(data -> data.getValue().verlapsCountProperty());
		activityScopeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(activityScopeColumn));
		activityScopeColumn.setCellValueFactory(data -> data.getValue().isGlobalScopeProperty());
		
		//Setting up DPN attribute scope table
		attributeScopeTableView.setPlaceholder(new Label("No DPN attribute overlaps detected"));
		attributeScopeTableView.setItems(attributeScopeSelections);
		attributeScopeTableView.getSortOrder().add(attributeOverlapsColumn);
		attributeNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		attributeNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getItemName()));
		attributeOverlapsColumn.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
		attributeOverlapsColumn.setCellValueFactory(data -> data.getValue().verlapsCountProperty());
		attributeScopeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(attributeScopeColumn));
		attributeScopeColumn.setCellValueFactory(data -> data.getValue().isGlobalScopeProperty());


		//Setting up the trace selector for displaying the results
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
						AbstractModel abstractModel = ModelUtils.loadDpnModel(modelFile.toPath(), modelName, defaultViolationCost);
						abstractModels.add(abstractModel);
						for (String activityName : abstractModel.getActivityNames()) {
							if (!activityNameToScopeSelection.containsKey(activityName)) {
								ScopeSelection attributeScopeSelection = new ScopeSelection(activityName);
								activityNameToScopeSelection.put(activityName, attributeScopeSelection);
								activityScopeSelections.add(attributeScopeSelection);
							}
							activityNameToScopeSelection.get(activityName).setOverlapsCount(activityNameToScopeSelection.get(activityName).getOverlapsCount()+1);
						}
						for (String attributeName : abstractModel.getVariableTypeMap().keySet()) {
							if (!attributeNameToScopeSelection.containsKey(attributeName)) {
								ScopeSelection attributeScopeSelection = new ScopeSelection(attributeName);
								attributeNameToScopeSelection.put(attributeName, attributeScopeSelection);
								attributeScopeSelections.add(attributeScopeSelection);
							}
							attributeNameToScopeSelection.get(attributeName).setOverlapsCount(attributeNameToScopeSelection.get(attributeName).getOverlapsCount()+1);
						}
						activityScopeTableView.sort();
						attributeScopeTableView.sort();
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
		//settingsPanel.setDisable(true);

		long startTime = System.nanoTime();
		createMonitoringDataStructures();
		monitoringAutomatonTime = System.nanoTime() - startTime;

		//To make memory usage more predictable for testing
		//		System.gc();
		//		Alert alert = new Alert(AlertType.INFORMATION);
		//		alert.setContentText("Monitoring data structures created");
		//		alert.showAndWait();

		tracesListView.getItems().clear();
		resultsList = new ArrayList<VBox>();

		monitorNextTrace();
	}

	private void monitorNextTrace() {
		if (resultsList.size() < xlog.size()) {
			MonitoringTask monitoringTask = new MonitoringTask(xlog.get(resultsList.size()), modelTableView.getItems(), propositionData, globalAutomaton, globalAutomatonColours, costCurrMap, costBestMap, eventProcessingTimes);

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

			System.out.println("\n\n\n");
			System.out.println("===========================================");
			System.out.println("Statistics");
			System.out.println("===========================================");
			double autTime = TimeUnit.MILLISECONDS.convert(monitoringAutomatonTime, TimeUnit.NANOSECONDS)/1000.0;
			System.out.println("Monitoring automaton creation time (s): " + autTime);
			int autStates = globalAutomaton.stateCount();
			System.out.println("Monitoring automaton number of states: " + autStates);

			double minEvTime = TimeUnit.MICROSECONDS.convert(Collections.min(eventProcessingTimes), TimeUnit.NANOSECONDS)/1000.0;
			System.out.println("Min event processing time (ms): " + minEvTime);
			double maxEvTime = TimeUnit.MICROSECONDS.convert(Collections.max(eventProcessingTimes), TimeUnit.NANOSECONDS)/1000.0;
			System.out.println("Max event processing time (ms): " + maxEvTime);
			long sum = 0;
			for(int i = 0; i < eventProcessingTimes.size(); i++) {
				sum += eventProcessingTimes.get(i);
			}
			long avg = sum / eventProcessingTimes.size();
			double meanEvTime = TimeUnit.MICROSECONDS.convert(avg, TimeUnit.NANOSECONDS)/1000.0;
			System.out.println("Avg event processing time (ms): " + meanEvTime);


			System.out.println();
			System.out.println("For easier copying (Automaton)");
			System.out.println("AutTime\tAutStates");
			System.out.println(autTime + "\t" + autStates);

			System.out.println("For easier copying (Monitoring)");
			System.out.println("EvMin\tEvMax\tEvMean");
			System.out.println(minEvTime + "\t" + maxEvTime + "\t" + meanEvTime);

			System.out.println("For easier copying (full)");
			System.out.println("AutTime\tAutStates\tMemory\tEvMin\tEvMax\tEvMean");
			System.out.println(autTime + "\t" + autStates + "\t\t" + minEvTime + "\t" + maxEvTime + "\t" + meanEvTime);

			//To make memory usage more predictable for testing
			//			System.gc();
			//			Alert alert2 = new Alert(AlertType.INFORMATION);
			//			alert2.setContentText("Monitoring done");
			//			alert2.showAndWait();

		}
	}

	private void createMonitoringDataStructures() {
		
		System.out.println("Start: Populating propositionalization data structure");
		propositionData = new PropositionData();
		propositionData.setModels(modelTableView.getItems());
		
		propositionData.setDpnGlobalActivities(activityScopeSelections);
		propositionData.setDpnGlobalVariables(attributeScopeSelections);
		
		propositionData.setDpnLocalActivities(modelTableView.getItems());
		propositionData.setDpnLocalVariables(modelTableView.getItems());
		
		propositionData.setConstraintActivities(modelTableView.getItems());
		propositionData.setConstraintVariables(modelTableView.getItems());
		
		propositionData.setVariableConnectionsAndPropositions(modelTableView.getItems());
		System.out.println("Done: Populating propositionalization data structure\n");

		System.out.println("Start: Creating a propositionalized automaton of each Declare and LTL model");
		for (AbstractModel processModel : modelTableView.getItems()) {
			processModel.initializeAutomaton(propositionData);
		}
		
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
