package structure.expand.expansionTree;

import java.util.*;
import structure.instruction.Instruction;

/**
 * ExpansionTree – עץ יוחסין של הרחבות (childId -> parentId) + מיפוי מזהה להוראה (id -> Instruction).
 * משמש להרכבת השרשרת "אבא <<< ילד" עבור פקודה 3.
 */
public final class ExpansionTree {

    private int nextId = 1;
    private final Map<Integer, Integer> parent = new HashMap<>();   // childId -> parentId
    private final Map<Integer, Instruction> node = new HashMap<>(); // id -> Instruction

    /** מוסיף הוראה מקורית (ללא אב) ומחזיר מזהה פנימי שלה. */
    public int addRoot(Instruction ins) {
        int id = nextId++;
        node.put(id, ins);
        return id;
    }

    /** מוסיף ילד עם קישור לאב ומחזיר מזהה פנימי שלו. */
    public int addChild(Instruction child, int parentId) {
        int id = nextId++;
        node.put(id, child);
        parent.put(id, parentId);
        return id;
    }

    /** מחזיר את ההוראה לפי מזהה (או null אם לא קיים). */
    public Instruction getInstruction(int id) {
        return node.get(id);
    }

    /** מחזיר את מזהה האב של id (או null אם שורש / לא קיים). */
    public Integer parentOf(int id) {
        return parent.get(id);
    }

    /** מחזירה את השרשרת מאב קדמון ועד צאצא (לסדר הדפסה: אב <<< ... <<< ילד). */
    public List<Instruction> chain(int id) {
        List<Instruction> list = new ArrayList<>();
        Integer cur = id;
        while (cur != null) {
            list.add(node.get(cur));
            cur = parent.get(cur);
        }
        Collections.reverse(list);
        return list;
    }

    /** איפוס אופציונלי לריצה חדשה. */
    public void clear() {
        parent.clear();
        node.clear();
        nextId = 1;
    }
}
