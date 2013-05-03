/** Copyright 2012, 2013 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package net.alliknow.podcatcher;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.HttpResponseCache;
import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadEpisodeMetadataListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.SuggestionManager;
import net.alliknow.podcatcher.model.tasks.LoadEpisodeMetadataTask;
import net.alliknow.podcatcher.model.tasks.LoadPodcastListTask;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;
import net.alliknow.podcatcher.model.types.Podcast;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Our application subclass. Holds global state and model. The Podcatcher
 * application object is created on application startup and will be alive for
 * all the app's lifetime. Its main purpose is to hold handles to the singleton
 * instances of our model data and data managers. In addition, it provides some
 * generic convenience methods.
 */
public class Podcatcher extends Application implements OnLoadEpisodeMetadataListener,
        OnLoadPodcastListListener {

    /**
     * The amount of dp establishing the border between small and large screen
     * buckets
     */
    public static final int MIN_PIXEL_LARGE = 600;

    /** Characters not allowed in filenames */
    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'#!,&";

    /** The http request header field key for the user agent */
    public static final String USER_AGENT_KEY = "User-Agent";
    /** The user agent string we use to identify us */
    public static final String USER_AGENT_VALUE = "Podcatcher Deluxe";
    /** The HTTP cache size */
    public static final long HTTP_CACHE_SIZE = 8 * 1024 * 1024; // 8 MiB
    /** Static inner thread class to pull flushing the cache off the main thread */
    private static final Thread flushHttpCache = new Thread() {

        @Override
        public void run() {
            final HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache != null)
                cache.flush();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Enabled caching for our HTTP connections
        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            HttpResponseCache.install(httpCacheDir, HTTP_CACHE_SIZE);
        } catch (IOException ioe) {
            Log.i(getClass().getSimpleName(), "HTTP response cache installation failed:" + ioe);
        }

        // This will only run once in the lifetime of the app
        // since the application is an implicit singleton. We create the other
        // singletons here to make sure they know their application instance.
        PodcastManager.getInstance(this);
        // And this one as well
        EpisodeManager.getInstance(this);
        // dito
        SuggestionManager.getInstance(this);

        // Now we will trigger the preparation on start-up, steps include:
        // 1. Load episode metadata from file
        // 2. Load podcast list from file
        // 3. Tell the UI to start

        // This starts step 1
        new LoadEpisodeMetadataTask(this, this).execute((Void) null);
    }

    @Override
    public void onEpisodeMetadataLoaded(Map<URL, EpisodeMetadata> result) {
        // Step 1 finished
        // Make episode manager aware
        EpisodeManager.getInstance().onEpisodeMetadataLoaded(result);

        // This is step 2
        // Load list of podcasts from OPML file on start-up
        new LoadPodcastListTask(this, this).execute((Void) null);
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Step 2 finished
        // Make episode manager aware (it will notify the UI, step 3)
        PodcastManager.getInstance().onPodcastListLoaded(podcastList);
    }

    /**
     * Write http cache data to disk (async).
     */
    public void flushHttpCache() {
        flushHttpCache.start();
    }

    /**
     * Checks whether the device is currently online and can receive data from
     * the internets.
     * 
     * @return <code>true</code> iff we have Internet access.
     */
    public boolean isOnline() {
        final NetworkInfo activeNetwork = getNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Checks whether the device is currently on a fast network (such as wifi)
     * as opposed to a mobile network.
     * 
     * @return <code>true</code> iff we have fast (and potentially free)
     *         Internet access.
     */
    public boolean isOnFastConnection() {
        final NetworkInfo activeNetwork = getNetworkInfo();

        if (activeNetwork == null)
            return false;
        else
            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_ETHERNET:
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_WIMAX:
                    return true;
                default:
                    return false;
            }
    }

    /**
     * Checks whether the app is in debug mode.
     * 
     * @return <code>true</code> iff in debug.
     */
    public boolean isInDebugMode() {
        boolean debug = false;

        PackageManager manager = getApplicationContext().getPackageManager();
        try
        {
            ApplicationInfo info = manager.getApplicationInfo(
                    getApplicationContext().getPackageName(), 0);
            debug = (0 != (info.flags &= ApplicationInfo.FLAG_DEBUGGABLE));
        } catch (Exception e) {
            // pass
        }

        return debug;
    }

    /**
     * Clean up given string to be suitable as a file/directory name. This works
     * by removing all reserved chars.
     * 
     * @param name The String to clean up (not <code>null</code>).
     * @return A cleaned string, might have zero length.
     */
    public static String sanitizeAsFilename(String name) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < name.length(); i++)
            if (RESERVED_CHARS.indexOf(name.charAt(i)) == -1)
                builder.append(name.charAt(i));

        return builder.toString();
    }

    private NetworkInfo getNetworkInfo() {
        ConnectivityManager manager =
                (ConnectivityManager) getApplicationContext()
                        .getSystemService(CONNECTIVITY_SERVICE);

        return manager.getActiveNetworkInfo();
    }
}
