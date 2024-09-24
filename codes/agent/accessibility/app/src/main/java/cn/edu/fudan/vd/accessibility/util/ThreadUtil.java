package cn.edu.fudan.vd.accessibility.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {

    private static final int CORE_SIZE = 10;
    private static final int MAX_SIZE = 10;
    private static final int ALIVE_TIME = 1;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final Integer CAPACITY = 100;
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(CORE_SIZE, MAX_SIZE, ALIVE_TIME, TIME_UNIT, new LinkedBlockingDeque<>(CAPACITY));

    public static void addTask(Runnable runnable) {
        executor.execute(runnable);
    }

    public static <T> T addTask(Callable<T> callable) {
        Future<T> future = executor.submit(callable);
        try {
            return future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
