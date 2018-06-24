package com.apryshchepa.taskdiff.controller;

import com.apryshchepa.taskdiff.model.Task;
import com.apryshchepa.taskdiff.service.*;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TaskLoaderController {

    private static final int PERIOD = 2000;

    @FXML
    private TableView<Task> liveView;
    @FXML
    private TableColumn<Task, String> imageNameColumn;
    @FXML
    private TableColumn<Task, String> pidColumn;
    @FXML
    private TableColumn<Task, String> sessionNameColumn;
    @FXML
    private TableColumn<Task, String> sessionIdColumn;
    @FXML
    private TableColumn<Task, String> memUsageColumn;
    @FXML
    private TableView<Task> snapshotView;
    @FXML
    private TableColumn<Task, String> snapImageNameColumn;
    @FXML
    private TableColumn<Task, String> snapPidColumn;
    @FXML
    private TableColumn<Task, String> snapSessionNameColumn;
    @FXML
    private TableColumn<Task, String> snapSessionIdColumn;
    @FXML
    private TableColumn<Task, String> snapMemUsageColumn;

    private final TaskLoader taskLoader;

    private final DifferenceService differenceService;

    private final ScheduleService scheduleService;

    private List<Task> snapshotList = Collections.emptyList();

    private List<Task> liveList = Collections.emptyList();

    private Map<Integer, Status> taskStatuses = Collections.emptyMap();

    public TaskLoaderController() {
        //TODO: whether check if this is Window or delegate creation
        this(new ScheduleService(), new WindowsTaskLoader(), new DifferenceService());
    }

    private TaskLoaderController(ScheduleService scheduleService, TaskLoader taskLoader, DifferenceService differenceService) {
        this.scheduleService = scheduleService;
        this.taskLoader = taskLoader;
        this.differenceService = differenceService;
    }

    @FXML
    public void initialize() {
        initLiveTableColumns();
        initSnapshotTableColumns();
        scheduleLiveView();
        initRowFactory();
    }

    private void initRowFactory() {
        this.liveView.setRowFactory(tableView -> {
            TableRow<Task> row = new TableRow<>();
            ObjectBinding<Status> contains = Bindings.createObjectBinding(() -> {
                if (row.getItem() != null) {
                    Integer pid = row.getItem().getPid();
                    return this.taskStatuses.getOrDefault(pid, Status.NEW);
                }
                return Status.NEW;
            }, row.itemProperty());
            row.styleProperty().bind(Bindings.when(contains.isEqualTo(Status.CHANGED))
                    .then("-fx-background-color: grey;")
                    .otherwise(Bindings.when(contains.isEqualTo(Status.NEW))
                            .then("-fx-background-color: green;")
                            .otherwise("")));
            return row;
        });
    }

    private void scheduleLiveView() {
        this.scheduleService.start(() -> {
            this.liveList = this.taskLoader.load();
            this.taskStatuses = this.differenceService.compare(this.liveList, this.snapshotList);
            this.liveView.setItems(FXCollections.observableArrayList(this.liveList));
        }, PERIOD);
    }

    private void initSnapshotTableColumns() {
        this.snapImageNameColumn.setCellValueFactory(new PropertyValueFactory<>("imageName"));
        this.snapPidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        this.snapSessionNameColumn.setCellValueFactory(new PropertyValueFactory<>("sessionName"));
        this.snapSessionIdColumn.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        this.snapMemUsageColumn.setCellValueFactory(new PropertyValueFactory<>("memUsage"));
    }

    private void initLiveTableColumns() {
        this.imageNameColumn.setCellValueFactory(new PropertyValueFactory<>("imageName"));
        this.pidColumn.setCellValueFactory(new PropertyValueFactory<>("pid"));
        this.sessionNameColumn.setCellValueFactory(new PropertyValueFactory<>("sessionName"));
        this.sessionIdColumn.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        this.memUsageColumn.setCellValueFactory(new PropertyValueFactory<>("memUsage"));
    }

    public void snapshot() {
        this.snapshotList = this.taskLoader.load();
        this.taskStatuses = this.differenceService.compare(this.liveList, this.snapshotList);
        this.snapshotView.setItems(FXCollections.observableArrayList(this.snapshotList));
        this.liveView.refresh();
    }
}
