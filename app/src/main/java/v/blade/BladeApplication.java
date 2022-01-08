package v.blade;

import android.app.Application;
import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import v.blade.library.Library;
import v.blade.sources.Source;

public class BladeApplication extends Application
{
    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 4, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private static final ExecutorService executorService = threadPoolExecutor;
    public static Context appContext;

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);

        //Provide static access to application context (eg. for 'Local' source, needing ContentProvider)
        appContext = base;

        //Load sources
        executorService.execute(() ->
        {
            Source.loadSourcesFromSave();
            Source.initSources();

            Library.loadFromCache();
        });
    }

    public static ExecutorService obtainExecutorService()
    {
        return executorService;
    }
}