package application.table.instruction;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import display.DisplayDTO;
import display.ExpandDTO;
import display.ExpandedInstructionDTO;
import display.InstrKindDTO;
import display.InstrOpDTO;
import display.InstructionBodyDTO;
import display.InstructionDTO;

import types.LabelDTO;
import types.VarRefDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstructionsController {

    @FXML private TableView<InstructionDTO> tblInstructions;
    @FXML private TableColumn<InstructionDTO, Number> colLine;
    @FXML private TableColumn<InstructionDTO, String> colBS;
    @FXML private TableColumn<InstructionDTO, String> colLabel;
    @FXML private TableColumn<InstructionDTO, Number> colCycles;
    @FXML private TableColumn<InstructionDTO, String> colInstruction;

    private final ObservableList<InstructionDTO> items = FXCollections.observableArrayList();
    private final Map<Integer, ExpandedInstructionDTO> expandedByNumber = new HashMap<>();

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

        colInstruction.setCellValueFactory(d ->
                new ReadOnlyStringWrapper(formatBody(d.getValue().getBody())));
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

    public void show(DisplayDTO dto) {
        if (dto == null || dto.getInstructions() == null) {
            clear();
            return;
        }
        expandedByNumber.clear();
        show(dto.getInstructions());
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
    }

    public void setRows(List<InstructionDTO> rows) {
        items.setAll(rows == null ? List.of() : rows);
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
                    // ואז האבות שלו
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
}
