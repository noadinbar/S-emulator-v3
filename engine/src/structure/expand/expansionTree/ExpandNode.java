package structure.expand.expansionTree;

import structure.instruction.Instruction;

/** עטיפה לצומת בהרחבה: ההוראה + המזהה שלה בעץ + מזהה האב (אם יש). */
public final class ExpandNode {
    public final Instruction inst;
    public final int id;
    public final Integer parentId; // null לשורש

    public ExpandNode(Instruction inst, int id, Integer parentId) {
        this.inst = inst;
        this.id = id;
        this.parentId = parentId;
    }
}
