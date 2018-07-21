package vn.softdreams.springsaml.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

/**
 * Created by chen on 7/20/18.
 */
public class Cacher {
    private static Cacher instance = null;
    private Cache<String, String> sessionCache = null;
    private Cache<String, String> resultCache = null;
    private final int MAX_RECORDS = 10000;
    private final int TIME_OUT_IN_SECONDS = 60;
    private final int DEFAULT_CONCURRENCY_LEVEL = 4;

    public static Cacher getInstance() {
        if (instance == null) {
            instance = new Cacher();
        }
        return instance;
    }

    private Cacher() {
        sessionCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_RECORDS)
                .expireAfterWrite(TIME_OUT_IN_SECONDS, TimeUnit.SECONDS)
                .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
                .recordStats()
                .build();

        resultCache = CacheBuilder.newBuilder()
                .maximumSize(MAX_RECORDS)
                .expireAfterWrite(TIME_OUT_IN_SECONDS, TimeUnit.SECONDS)
                .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
                .recordStats()
                .build();
    }

    public void newRequest(String requestId, String sessionId) {
        sessionCache.put(requestId, sessionId);
    }

    public void addResult(String requestId, String info) {
        String sessionId = sessionCache.getIfPresent(requestId);
        if (!Utils.isNullOrEmpty(sessionId))
            resultCache.put(sessionId, info);
    }

    public String getResult(String sessionId) {
        return resultCache.getIfPresent(sessionId);
    }
}