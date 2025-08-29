package screens;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ExitAction {
    public void run() {
        System.out.println("Thank you for using S-emulator :)");
        System.out.flush();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(4000));
        System.exit(0);
    }
}
