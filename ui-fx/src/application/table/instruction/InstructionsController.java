package application.table.instruction;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import display.Command2DTO;
import display.Command3DTO;
import display.ExpandedInstructionDTO;
import display.InstrKindDTO;
import display.InstrOpDTO;
import display.InstructionBodyDTO;
import display.InstructionDTO;
import types.LabelDTO;
import types.VarRefDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstructionsController {

    @FXML private TableView<InstructionDTO> tblInstructions;

    @FXML private TableColumn<InstructionDTO, Number> colLine;
    @FXML private TableColumn<InstructionDTO, String>  colBS;
    @FXML private TableColumn<InstructionDTO, String>  colLabel;
    @FXML private TableColumn<InstructionDTO, Number> colCycles;
    @FXML private TableColumn<InstructionDTO, String>  colInstruction;

    private final ObservableList<InstructionDTO> items = FXCollections.observableArrayList();

    // מיפוי: מספר שורה -> ExpandedInstructionDTO (בשביל createdByChain)
    private final Map<Integer, ExpandedInstructionDTO> expandedByNumber = new HashMap<>();

    @FXML
    private void initialize() {
        tblInstructions.setItems(items);

        colLine.setCellValueFactory(d -> new ReadOnlyIntegerWrapper(d.getValue().getNumber()));
        colBS.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getKind() == InstrKindDTO.BASIC ? "B" : "S"
        ));
        colLabel.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatLabel(d.getValue().getLabel())));
        colCycles.setCellValueFactory(d -> new ReadOnlyIntegerWrapper(d.getValue().getCycles()));
        colInstruction.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatBody(d.getValue().getBody())));
    }

    // ===== ספירות (לסאמרי אם צריך) =====
    public long countBasic() {
        return items.stream().filter(i -> i.getKind() == InstrKindDTO.BASIC).count();
    }
    public long countSynthetic() {
        return items.stream().filter(i -> i.getKind() == InstrKindDTO.SYNTHETIC).count();
    }

    /** חשיפה לרשימת הפריטים (אם צריך) */
    public ObservableList<InstructionDTO> getItemsView() { return items; }

    /** גישה ל-TableView (ל־setDisable אופציונלי) */
    public TableView<InstructionDTO> getTableView() { return tblInstructions; }

    // ===== ממשק פומבי להצגה =====

    /** מציג את פקודה 2 (ללא expand) */
    public void show(Command2DTO dto) {
        if (dto == null || dto.getInstructions() == null) {
            clear();
            return;
        }
        expandedByNumber.clear();
        show(dto.getInstructions());
    }

    /** מציג הוראות לאחר expand ושומר ExpandedInstructionDTO לכל פקודה */
    public void showExpanded(Command3DTO c3) {
        if (c3 == null || c3.getInstructions() == null) {
            clear();
            return;
        }
        expandedByNumber.clear();
        List<InstructionDTO> flat = new ArrayList<>(c3.getInstructions().size());
        for (ExpandedInstructionDTO ei : c3.getInstructions()) {
            InstructionDTO ins = ei.getInstruction();
            flat.add(ins);
            expandedByNumber.put(ins.getNumber(), ei);
        }
        show(flat);
    }

    /** מציג רשימת הוראות ישירה */
    public void show(List<InstructionDTO> instructions) {
        expandedByNumber.clear(); // הצגה ישירה בלי expand
        setRows(instructions);
    }

    public void setRows(List<InstructionDTO> rows) {
        items.setAll(rows == null ? List.of() : rows);
    }

    public void clear() {
        items.clear();
        expandedByNumber.clear();
    }

    // ===== בחירה ושרשרת יוחסין =====

    public InstructionDTO getSelectedItem() {
        return tblInstructions.getSelectionModel().getSelectedItem();
    }

    public ReadOnlyObjectProperty<InstructionDTO> selectedItemProperty() {
        return tblInstructions.getSelectionModel().selectedItemProperty();
    }

    /** מחזיר את השרשרת כפי שה-Engine סיפק: אב מיידי → ... → מקור (האחרונה) */
    public List<InstructionDTO> getCreatedByChainFor(InstructionDTO instr) {
        if (instr == null) return List.of();
        ExpandedInstructionDTO ei = expandedByNumber.get(instr.getNumber());
        if (ei == null || ei.getCreatedByChain() == null) return List.of();
        return ei.getCreatedByChain();
    }

    // ===== פורמט תצוגה =====

    private String formatLabel(LabelDTO lbl) {
        if (lbl == null) return "";
        if (lbl.isExit()) return "EXIT";
        String n = lbl.getName();
        if (n == null || "EMPTY".equals(n) || n.isBlank()) return "";
        return n;
    }

    private String formatBody(InstructionBodyDTO b) {
        if (b == null) return "";
        InstrOpDTO op = b.getOp();
        switch (op) {
            case INCREASE:
                return String.format("%s <- %s + 1", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case DECREASE:
                return String.format("%s <- %s - 1", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case NEUTRAL:
                return String.format("%s <- %s", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case ASSIGNMENT:
                return String.format("%s <- %s", formatVar(b.getDest()), formatVar(b.getSource()));
            case CONSTANT_ASSIGNMENT:
                return String.format("%s <- %d", formatVar(b.getDest()), b.getConstant()) ;

            case ZERO_VARIABLE:
                return String.format("%s <- 0", formatVar(b.getDest()));
            case JUMP_NOT_ZERO:
                return String.format("IF %s != 0 GOTO %s", formatVar(b.getVariable()), formatLabel(b.getJumpTo()));
            case JUMP_ZERO:
                return String.format("IF %s = 0 GOTO %s", formatVar(b.getVariable()), formatLabel(b.getJumpTo()));
            case JUMP_EQUAL_CONSTANT:
                return String.format("IF %s = %d GOTO %s",
                        formatVar(b.getVariable()), b.getConstant(), formatLabel(b.getJumpTo()));
            case JUMP_EQUAL_VARIABLE:
                return String.format("IF %s = %s GOTO %s",
                        formatVar(b.getCompare()), formatVar(b.getCompareWith()), formatLabel(b.getJumpTo()));
            case GOTO_LABEL:
                return String.format("GOTO %s", formatLabel(b.getJumpTo()));
            default:
                return "?";
        }
    }

    private String formatVar(VarRefDTO v) {
        if (v == null) return "";
        String base = v.getVariable().name(); // x/y/z
        int idx = v.getIndex();
        if ("y".equals(base)) return "y"; // y בלי אינדקס
        if (idx <= 0) return base;
        return base + idx;
    }
}
