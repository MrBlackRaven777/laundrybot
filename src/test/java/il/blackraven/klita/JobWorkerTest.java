package il.blackraven.klita;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JobWorkerTest {
    private static final long INIT_DELAY = 1;
    private static final long JOB_WORKER_RATE = 15;
    @Test
    public void jobWorkerRunTest() throws InterruptedException {

//        ScheduledExecutorService jobWorker = Executors.newSingleThreadScheduledExecutor();
//        jobWorker.scheduleAtFixedRate(new JobWorker(),
//                INIT_DELAY,
//                JOB_WORKER_RATE,
//                TimeUnit.SECONDS);
//
//        while (true);
    }
}
