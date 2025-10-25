package application.execution.table.instruction;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;

import display.ExpandDTO;
import display.ExpandedInstructionDTO;
import display.InstrKindDTO;
import display.InstrOpDTO;
import display.InstructionBodyDTO;
import display.InstructionDTO;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.util.Duration;
import types.LabelDTO;
import types.VarRefDTO;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class InstructionsController {
    @FXML private TableView<InstructionDTO> tblInstructions;
    @FXML private TableColumn<InstructionDTO, Number> colLine;
    @FXML private TableColumn<InstructionDTO, String> colBS;
    @FXML private TableColumn<InstructionDTO, String> colLabel;
    @FXML private TableColumn<InstructionDTO, Number> colCycles;
    @FXML private TableColumn<InstructionDTO, String> colInstruction;
    @FXML private TableColumn<InstructionDTO, String> colGeneration;

    private final ObservableList<InstructionDTO> items = FXCollections.observableArrayList();
    private final Map<Integer, ExpandedInstructionDTO> expandedByNumber = new HashMap<>();
    private static final String HILITE_CLASS = "hilite";
    private Predicate<InstructionDTO> highlightPredicate = i -> false;

    @FXML
    private void initialize() {
        tblInstructions.setItems(items);

        colLine.setCellValueFactory(d ->
                new ReadOnlyIntegerWrapper(d.getValue().getNumber()));

        colBS.setCellValueFactory(d ->
                new ReadOnlyStringWrapper(
                        d.getValue().getKind() == InstrKindDTO.BASIC ? "B" : "S"));

        colLabel.setCellValueFactory(d ->
                new ReadOnlyStringWrapper(formatLabel(d.getValue().getLabel())));

        colCycles.setCellValueFactory(d ->
                new ReadOnlyIntegerWrapper(d.getValue().getCycles()));

        colCycles.setCellFactory(column -> new TableCell<InstructionDTO, Number>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                InstructionDTO row = getTableRow() != null ? getTableRow().getItem() : null;
                if (row == null || row.getBody() == null) {
                    setText(String.valueOf(value.intValue()));
                    return;
                }
                InstrOpDTO op = row.getBody().getOp();
                if (op == InstrOpDTO.QUOTE) {
                    setText("5+");
                } else if (op == InstrOpDTO.JUMP_EQUAL_FUNCTION) {
                    setText("6+");
                } else {
                    setText(String.valueOf(value.intValue()));
                }
            }
        });

        colInstruction.setCellValueFactory(d ->
                new ReadOnlyStringWrapper(formatBody(d.getValue().getBody())));

        colGeneration.setCellValueFactory(new PropertyValueFactory<>("generation"));

        URL css = getClass().getResource("/application/execution/table/instruction/instructions.css");
        if (css != null && tblInstructions != null) {
            String url = css.toExternalForm();
            if (!tblInstructions.getStylesheets().contains(url)) {
                tblInstructions.getStylesheets().add(url);
            }
        }

        // Row highlight: apply CSS class "hilite" when highlightPredicate(item) == true.
        tblInstructions.setRowFactory(new Callback<TableView<InstructionDTO>, TableRow<InstructionDTO>>() {
            @Override
            public TableRow<InstructionDTO> call(TableView<InstructionDTO> tv) {
                return new TableRow<InstructionDTO>() {
                    @Override
                    protected void updateItem(InstructionDTO item, boolean empty) {
                        super.updateItem(item, empty);
                        // Always remove previous highlight before re-evaluating
                        getStyleClass().remove(InstructionsController.HILITE_CLASS);

                        if (empty || item == null) {
                            return;
                        }

                        // If predicate says "highlight this row", add the CSS class
                        if (highlightPredicate != null && highlightPredicate.test(item)) {
                            if (!getStyleClass().contains(InstructionsController.HILITE_CLASS)) {
                                getStyleClass().add(InstructionsController.HILITE_CLASS);
                            }
                        }
                    }
                };
            }
        });

    }

    public String getMaxGenerationValue() {
        if (tblInstructions == null || tblInstructions.getItems() == null || tblInstructions.getItems().isEmpty()) {
            return "I";
        }
        String max = "I";
        for (InstructionDTO dto : tblInstructions.getItems()) {
            String g = (dto != null && dto.getGeneration() != null) ? dto.getGeneration().trim().toUpperCase() : null;
            if (g != null && g.compareTo(max) > 0) {
                max = g;
            }
        }
        return max;
    }

    public long countBasic() {
        return items.stream().filter(i -> i.getKind() == InstrKindDTO.BASIC).count();
    }

    public long countSynthetic() {
        return items.stream().filter(i -> i.getKind() == InstrKindDTO.SYNTHETIC).count();
    }

    public ObservableList<InstructionDTO> getItemsView() {
        return items;
    }

    public TableView<InstructionDTO> getTableView() {
        return tblInstructions;
    }

    public void showExpanded(ExpandDTO dto) {
        if (dto == null) {
            clear();
            return;
        }
        expandedByNumber.clear();

        List<InstructionDTO> flat = new ArrayList<>(dto.getInstructions().size());
        for (ExpandedInstructionDTO d : dto.getInstructions()) {
            InstructionDTO instruction = d.getInstruction();
            flat.add(instruction);
            expandedByNumber.put(instruction.getNumber(), d);
        }
        show(flat);
    }

    public void show(List<InstructionDTO> instructions) {
        items.setAll(instructions == null ? List.of() : instructions);
        tblInstructions.layout();
    }

    public void setRows(List<InstructionDTO> rows) {
        items.setAll(rows == null ? List.of() : rows);
        tblInstructions.layout();
    }

    public void clear() {
        items.clear();
        expandedByNumber.clear();
    }

    public void hideLineColumn() {
        if (colLine != null) {
            colLine.setVisible(false);
        }
    }

    public void showLineColumn() {
        if (colLine != null) {
            colLine.setVisible(true);
        }
    }

    public InstructionDTO getSelectedItem() {
        return tblInstructions.getSelectionModel().getSelectedItem();
    }

    public ReadOnlyObjectProperty<InstructionDTO> selectedItemProperty() {
        return tblInstructions.getSelectionModel().selectedItemProperty();
    }

    public List<InstructionDTO> getCreatedByChainFor(InstructionDTO instr) {
        if (instr == null) {
            return List.of();
        }
        ExpandedInstructionDTO ei = expandedByNumber.get(instr.getNumber());
        if (ei == null) {
            return List.of();
        }
        List<InstructionDTO> chain = ei.getCreatedByChain();
        if (!chain.isEmpty()) {
            return chain;
        }
        return buildChainDepthFirst(ei, new LinkedHashSet<>());
    }

    private List<InstructionDTO> buildChainDepthFirst(ExpandedInstructionDTO node, Set<Integer> seen) {
        List<InstructionDTO> out = new ArrayList<>();
        if (node == null) {
            return out;
        }

        for (InstructionDTO parent : node.getCreatedByChain()) {
            if (parent == null) {
                continue;
            }
            int num = parent.getNumber();
            if (seen.add(num)) {
                out.add(parent);
                ExpandedInstructionDTO parentEi = expandedByNumber.get(num);
                if (parentEi != null) {
                    out.addAll(buildChainDepthFirst(parentEi, seen));
                }
            }
        }
        return out;
    }

    private String formatLabel(LabelDTO lbl) {
        if (lbl == null) {
            return "";
        }
        if (lbl.isExit()) {
            return "EXIT";
        }
        String n = lbl.getName();
        if (n == null || "EMPTY".equals(n) || n.isBlank()) {
            return "";
        }
        return n;
    }

    private String formatBody(InstructionBodyDTO b) {
        if (b == null) {
            return "";
        }
        InstrOpDTO op = b.getOp();

        return switch (op) {
            case INCREASE -> String.format("%s <- %s + 1",
                    formatVar(b.getVariable()), formatVar(b.getVariable()));
            case DECREASE -> String.format("%s <- %s - 1",
                    formatVar(b.getVariable()), formatVar(b.getVariable()));
            case NEUTRAL -> String.format("%s <- %s",
                    formatVar(b.getVariable()), formatVar(b.getVariable()));
            case ASSIGNMENT -> String.format("%s <- %s",
                    formatVar(b.getDest()), formatVar(b.getSource()));
            case CONSTANT_ASSIGNMENT -> String.format("%s <- %d",
                    formatVar(b.getDest()), b.getConstant());
            case ZERO_VARIABLE -> String.format("%s <- 0", formatVar(b.getDest()));
            case JUMP_NOT_ZERO -> String.format("IF %s != 0 GOTO %s",
                    formatVar(b.getVariable()), formatLabel(b.getJumpTo()));
            case JUMP_ZERO -> String.format("IF %s = 0 GOTO %s",
                    formatVar(b.getVariable()), formatLabel(b.getJumpTo()));
            case JUMP_EQUAL_CONSTANT -> String.format("IF %s = %d GOTO %s",
                    formatVar(b.getVariable()), b.getConstant(), formatLabel(b.getJumpTo()));
            case JUMP_EQUAL_VARIABLE -> String.format("IF %s = %s GOTO %s",
                    formatVar(b.getCompare()), formatVar(b.getCompareWith()), formatLabel(b.getJumpTo()));
            case GOTO_LABEL -> String.format("GOTO %s", formatLabel(b.getJumpTo()));
            case QUOTE ->
                    String.format("%s <- (%s,%s)", formatVar(b.getVariable()), b.getUserString(), b.getFunctionArgs());
            case JUMP_EQUAL_FUNCTION ->
                    String.format("IF %s = (%s,%s) GOTO %s", formatVar(b.getVariable()), b.getUserString(), b.getFunctionArgs(), formatLabel(b.getJumpTo()));
        };
    }

    private String formatVar(VarRefDTO v) {
        if (v == null) {
            return "";
        }
        String base = v.getVariable().name();
        int idx = v.getIndex();

        if ("y".equals(base)) {
            return "y";
        }
        if (idx <= 0) {
            return base;
        }
        return base + idx;
    }


    public void setHighlightPredicate(Predicate<InstructionDTO> pred) {
        this.highlightPredicate = (pred != null) ? pred : i -> false;
        if (tblInstructions != null) tblInstructions.refresh();
    }
    //== BONUS==
}
