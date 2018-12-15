package gov.nist.p25.issi.utils;

import java.util.concurrent.Executor;

public class ThreadedExecutor implements Executor {

   @Override
    public void execute(Runnable r) {
        new Thread(r).start();
    }
}
