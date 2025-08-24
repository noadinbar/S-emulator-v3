package screens;

public class ExitAction {
    public void run() {
        System.out.println("Bye!");
        System.out.flush();          // לוודא שההודעה נכתבה לקונסולה
        System.exit(0);              // סגירה מיידית של ה־JVM
    }
}
