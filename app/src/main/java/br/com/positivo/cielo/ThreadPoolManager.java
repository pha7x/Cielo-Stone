package br.com.positivo.cielo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by guangyi.peng on 2017/3/30.
 */
public class ThreadPoolManager {
    private ExecutorService service;

    private ThreadPoolManager() {
        int num = Runtime.getRuntime().availableProcessors() * 2;
        service = Executors.newFixedThreadPool(num);
    }

    private static final ThreadPoolManager manager = new ThreadPoolManager();

    public static ThreadPoolManager getInstance() {
        return manager;
    }

    public void executeTask(Runnable runnable) {
        service.execute(runnable);
    }
}
