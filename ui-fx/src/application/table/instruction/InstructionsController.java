package application.table.instruction;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import display.Command2DTO;
import display.InstructionDTO;
import display.InstructionBodyDTO;
import display.InstrKindDTO;
import display.InstrOpDTO;
import types.LabelDTO;
import types.VarRefDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InstructionsController {

    @FXML private TableView<InstructionRow> tblInstructions;

    @FXML private TableColumn<InstructionRow, Number> colLine;
    @FXML private TableColumn<InstructionRow, String>  colBS;
    @FXML private TableColumn<InstructionRow, String>  colLabel;
    @FXML private TableColumn<InstructionRow, Number> colCycles;
    @FXML private TableColumn<InstructionRow, String>  colInstruction;

    private final ObservableList<InstructionRow> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // חיבור מודל לטבלה
        tblInstructions.setItems(items);

        // מיפוי עמודות -> שדות בשורה
        colLine.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getLine()));
        colBS.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBs()));
        colLabel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLabel()));
        colCycles.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCycles()));
        colInstruction.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInstruction()));
    }

    // === ממשק פומבי להצגה ===

    /** מציג את פקודה 2 כפי שמוחזרת מהמנוע (תרגיל 1). */
    public void show(Command2DTO dto) {
        if (dto == null || dto.getInstructions() == null) {
            clear();
            return;
        }
        show(dto.getInstructions());
    }

    /** מציג רשימת הוראות ישירה. */
    public void show(List<InstructionDTO> instructions) {
        setRows(toRows(instructions));
    }

    public void setRows(List<InstructionRow> rows) {
        items.setAll(rows == null ? List.of() : rows);
    }

    public void addRow(InstructionRow row) {
        if (row != null) items.add(row);
    }

    public void clear() {
        items.clear();
    }

    // === המרות מ-DTO לשורות טבלה ===

    private List<InstructionRow> toRows(List<InstructionDTO> ins) {
        List<InstructionRow> rows = new ArrayList<>();
        if (ins == null) return rows;
        for (InstructionDTO i : ins) {
            int number = i.getNumber();
            String bs = (i.getKind() == InstrKindDTO.BASIC) ? "B" : "S";
            String label = formatLabel(i.getLabel());
            String body = formatBody(i.getBody());
            int cycles = i.getCycles();
            rows.add(new InstructionRow(number, bs, label, cycles, body));
        }
        return rows;
    }

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
        switch (op){
            case INCREASE:
                // "%s <- %s + 1"
                return String.format("%s <- %s + 1", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case DECREASE:
                // "%s <- %s - 1"
                return String.format("%s <- %s - 1", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case NEUTRAL:
                // "%s <- %s"
                return String.format("%s <- %s", formatVar(b.getVariable()), formatVar(b.getVariable()));
            case ASSIGNMENT:
                // "%s <- %s"
                return String.format("%s <- %s", formatVar(b.getDest()), formatVar(b.getSource()));
            case CONSTANT_ASSIGNMENT:
                // "%s <- <const>"
                return String.format("%s <- %d", formatVar(b.getDest()), b.getConstant());
            case ZERO_VARIABLE:
                // "%s <- 0"
                return String.format("%s <- 0", formatVar(b.getDest()));
            case JUMP_NOT_ZERO:
                // "IF %s != 0 GOTO %s"
                return String.format("IF %s != 0 GOTO %s", formatVar(b.getVariable()), formatLabel(b.getJumpTo()));
            case JUMP_ZERO:
                // "IF %s = 0 GOTO %s"
                return String.format("IF %s = 0 GOTO %s", formatVar(b.getVariable()), formatLabel(b.getJumpTo()));
            case JUMP_EQUAL_CONSTANT:
                // "IF %s = %s GOTO %s"
                return String.format("IF %s = %d GOTO %s",
                        formatVar(b.getVariable()), b.getConstant(), formatLabel(b.getJumpTo()));
            case JUMP_EQUAL_VARIABLE:
                // "IF %s = %s GOTO %s"
                return String.format("IF %s = %s GOTO %s",
                        formatVar(b.getCompare()), formatVar(b.getCompareWith()), formatLabel(b.getJumpTo()));
            case GOTO_LABEL:
                // "GOTO %s"
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

    private String jump(InstructionBodyDTO b) {
        LabelDTO j = b.getJumpTo();
        if (j == null) return "";
        if (j.isExit()) return "EXIT";
        return j.getName() == null ? "" : j.getName();
    }

    private static String fmt(String f, Object... args) {
        return String.format(f, args);
    }

    // === מודל שורה לטבלה ===
    public static class InstructionRow {
        private final int line;
        private final String bs;
        private final String label;
        private final int cycles;
        private final String instruction;

        public InstructionRow(int line, String bs, String label, int cycles, String instruction) {
            this.line = line;
            this.bs = Objects.requireNonNullElse(bs, "");
            this.label = Objects.requireNonNullElse(label, "");
            this.cycles = cycles;
            this.instruction = Objects.requireNonNullElse(instruction, "");
        }

        public int getLine() { return line; }
        public String getBs() { return bs; }
        public String getLabel() { return label; }
        public int getCycles() { return cycles; }
        public String getInstruction() { return instruction; }
    }
}
