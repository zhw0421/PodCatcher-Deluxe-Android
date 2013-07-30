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

import android.app.FragmentManager.OnBackStackChangedListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment.LogoViewMode;

import java.util.List;

/**
 * Our main activity class. Works as the main controller. Depending on the view
 * state, other activities cooperate.
 */
public class PodcastActivity extends EpisodeListActivity implements OnBackStackChangedListener,
        OnLoadPodcastListListener, OnChangePodcastListListener {

    /** The current podcast list fragment */
    protected PodcastListFragment podcastListFragment;

    /**
     * Flag indicating whether the app should show the add podcast dialog if the
     * list of podcasts is empty.
     */
    private boolean isInitialAppStart = false;
    /**
     * Flag indicating the intent given onCreate contains data we want to use as
     * a podcast URL.
     */
    private boolean hasPodcastToAdd = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable strict mode when on debug
        if (((Podcatcher) getApplication()).isInDebugMode())
            StrictMode.enableDefaults();

        // 1. Register listeners
        // Register as listener to the podcast data manager
        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);

        // 2. Create the UI via XML layouts and fragments
        // Inflate the main content view (depends on view mode)
        setContentView(R.layout.main);
        // Make sure all fragment member handles are properly set
        findFragments();
        // Add extra fragments needed in some view modes
        plugFragments();
        // Make sure the podcast list knows about our theme colors.
        podcastListFragment.setThemeColors(themeColor, lightThemeColor);
        // Make sure the layout matches the preference
        updateLayout();

        // 3. Init/restore the app as needed
        // If we are newly starting up and the podcast list is empty, show add
        // podcast dialog (this is used in onPodcastListLoaded(), since we only
        // know then, whether the list is actually empty). Also do not show it
        // if we are given an URL in the intent, because this will trigger the
        // dialog anyway.
        isInitialAppStart = (savedInstanceState == null);
        hasPodcastToAdd = (getIntent().getData() != null);
        // Check if podcast list is available - if so, set it
        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null) {
            onPodcastListLoaded(podcastList);

            // We only reset our state if the podcast list is available, because
            // otherwise we will not be able to select anything.
            if (getIntent().hasExtra(MODE_KEY))
                onNewIntent(getIntent());
        }

        // Finally we might also be called freshly with a podcast feed to add
        if (getIntent().getData() != null)
            onNewIntent(getIntent());
    }

    @Override
    protected void findFragments() {
        super.findFragments();

        // The podcast list fragment to use
        if (podcastListFragment == null)
            podcastListFragment = (PodcastListFragment) findByTagId(R.string.podcast_list_fragment_tag);
    }

    /**
     * In certain view modes, we need to add some fragments because they are not
     * set in the layout XML files. Member variables will be set if needed.
     */
    private void plugFragments() {
        // On small screens, add the podcast list fragment
        if (view.isSmall() && podcastListFragment == null) {
            podcastListFragment = new PodcastListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, podcastListFragment,
                            getString(R.string.podcast_list_fragment_tag))
                    .commit();
        }
        // On small screens in landscape mode, add the episode list fragment
        if (view.isSmallLandscape() && episodeListFragment == null) {
            episodeListFragment = new EpisodeListFragment();
            episodeListFragment.setThemeColors(themeColor, lightThemeColor);
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.right, episodeListFragment,
                            getString(R.string.episode_list_fragment_tag))
                    .commit();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This is an external call to add a new podcast
        if (intent.getData() != null) {
            Intent addPodcast = new Intent(this, AddPodcastActivity.class);
            addPodcast.setData(intent.getData());

            // We need to cut back the selection here when is small portrait
            // mode to prevent other activities from covering the add podcast
            // dialog
            if (view.isSmallPortrait())
                selection.reset();

            startActivity(addPodcast);
            // Reset data to prevent this intent from fire again on the next
            // configuration change
            intent.setData(null);
        }
        // This is an internal call to update the selection
        else if (intent.hasExtra(MODE_KEY)) {
            selection.setMode((ContentMode) intent.getSerializableExtra(MODE_KEY));
            selection.setPodcast(podcastManager.findPodcastForUrl(
                    intent.getStringExtra(PODCAST_URL_KEY)));
            selection.setEpisode(podcastManager.findEpisodeForUrl(
                    intent.getStringExtra(EPISODE_URL_KEY)));

            // onResume() will be called after this and do the actual selection
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Set podcast logo view mode
        updateLogoViewMode();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Restore UI to match selection:
        // Re-select previously selected podcast(s)
        if (selection.isAll())
            onAllPodcastsSelected();
        else if (selection.isPodcastSet())
            onPodcastSelected(selection.getPodcast());
        else
            onNoPodcastSelected();

        // Re-select previously selected episode
        if (selection.isEpisodeSet())
            onEpisodeSelected(selection.getEpisode());
        else
            onNoEpisodeSelected();

        // Make sure we are alerted on back stack changes. This needs to be
        // added after re-selection of the current content.
        getFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Disable listener (would interfere with resume)
        getFragmentManager().removeOnBackStackChangedListener(this);

        // Make sure we persist the podcast manager state
        podcastManager.saveState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Make sure our http cache is written to disk
        ((Podcatcher) getApplication()).flushHttpCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the listeners
        podcastManager.removeLoadPodcastListListener(this);
        podcastManager.removeChangePodcastListListener(this);
    }

    @Override
    public void onBackStackChanged() {
        // This only needed in small landscape mode and in case
        // we go back to the episode list
        if (view.isSmallLandscape()
                && getFragmentManager().getBackStackEntryCount() == 0) {
            onNoEpisodeSelected();
        }
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Make podcast list show
        podcastListFragment.setPodcastList(podcastList);

        // Make action bar show number of podcasts
        updateActionBar();

        // If podcast list is empty we show dialog on startup
        if (podcastManager.size() == 0 && isInitialAppStart && !hasPodcastToAdd) {
            isInitialAppStart = false;
            startActivity(new Intent(this, AddPodcastActivity.class));
        }
        // If enabled, we run the "select all on start-up" action
        else if (podcastManager.size() > 0 && isInitialAppStart
                && ((Podcatcher) getApplication()).isOnline()
                && preferences.getBoolean(SettingsActivity.KEY_SELECT_ALL_ON_START, false))
            onAllPodcastsSelected();
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // Update podcast list
        podcastListFragment.setPodcastList(podcastManager.getPodcastList());
        // Update UI
        updateActionBar();

        // Set the selection to the new podcast
        if (view.isSmallPortrait())
            selection.reset();
        else {
            selection.setMode(ContentMode.SINGLE_PODCAST);
            selection.setPodcast(podcast);
            if (view.isSmallLandscape())
                selection.resetEpisode();
        }
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        // Update podcast list
        podcastListFragment.setPodcastList(podcastManager.getPodcastList());
        // Update UI
        updateActionBar();

        // Reset selection if deleted
        if (podcast.equals(selection.getPodcast()))
            selection.resetPodcast();
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        super.onPodcastSelected(podcast);

        if (view.isSmallPortrait()) {
            // We need to launch a new activity to display the episode list
            Intent intent = new Intent(this, ShowEpisodeListActivity.class);

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        } else
            // Select in podcast list
            podcastListFragment.select(podcastManager.indexOf(podcast));

        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onAllPodcastsSelected() {
        super.onAllPodcastsSelected();

        // Prepare podcast list fragment
        podcastListFragment.selectAll();

        if (view.isSmallPortrait()) {
            // We need to launch a new activity to display the episode list
            Intent intent = new Intent(this, ShowEpisodeListActivity.class);

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }

        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onNoPodcastSelected() {
        super.onNoPodcastSelected();

        // Reset podcast list fragment
        podcastListFragment.selectNone();
        // Update UI
        updateLogoViewMode();
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        // Only react on progress here, if the activity is visible
        if (!view.isSmallPortrait()) {
            super.onPodcastLoadProgress(podcast, progress);

            // We are in select all mode, show progress in podcast list
            if (selection.isAll())
                podcastListFragment.showProgress(podcastManager.indexOf(podcast), progress);
        }
    }

    @Override
    public void onPodcastLoaded(Podcast podcast) {
        // This will display the number of episodes
        podcastListFragment.refresh();

        // Tell the podcast manager to load podcast logo
        podcastManager.loadLogo(podcast);

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoaded(podcast);
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        podcastListFragment.refresh();

        // Tell the podcast manager to load podcast logo even though the podcast
        // failed to load since the podcast logo might be available offline.
        podcastManager.loadLogo(failedPodcast);

        // In small portrait mode, work is done in separate activity
        if (!view.isSmallPortrait())
            super.onPodcastLoadFailed(failedPodcast);
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        super.onPodcastLogoLoaded(podcast);

        updateLogoViewMode();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        if (key.equals(SettingsActivity.KEY_THEME_COLOR) && podcastListFragment != null) {
            // Make the UI reflect the change
            podcastListFragment.setThemeColors(themeColor, lightThemeColor);
        }
        else if (key.equals(SettingsActivity.KEY_WIDE_EPISODE_LIST)) {
            updateLayout();
        }
    }

    /**
     * Update the layout to match user's preference
     */
    protected void updateLayout() {
        final boolean useWide = preferences
                .getBoolean(SettingsActivity.KEY_WIDE_EPISODE_LIST, false);

        switch (view) {
            case LARGE_PORTRAIT:
                setMainColumnWidthWeight(episodeListFragment.getView(), useWide ? 3.5f : 3f);

                break;
            case LARGE_LANDSCAPE:
                setMainColumnWidthWeight(episodeListFragment.getView(), useWide ? 3.5f : 3f);
                setMainColumnWidthWeight(findViewById(R.id.right_column), useWide ? 3.5f : 4f);

                break;
            default:
                // Nothing to do in small views
                break;
        }
    }

    /**
     * Update the logo view mode according to current app state.
     */
    protected void updateLogoViewMode() {
        LogoViewMode logoViewMode = LogoViewMode.NONE;

        if (view.isLargeLandscape() && !selection.isAll())
            logoViewMode = LogoViewMode.LARGE;
        else if (view.isSmallPortrait())
            logoViewMode = LogoViewMode.SMALL;

        podcastListFragment.setLogoVisibility(logoViewMode);
    }

    @Override
    protected void updateActionBar() {
        // Disable the home button (only used in overlaying activities)
        getActionBar().setHomeButtonEnabled(false);

        if (!view.isSmall() && selection.isAll())
            updateActionBarSubtitleOnMultipleLoad();
        else
            contentSpinner.setSubtitle(null);
    }

    @Override
    protected void updatePlayer() {
        super.updatePlayer();

        if (view.isSmallPortrait() && playerFragment != null) {
            playerFragment.setLoadMenuItemVisibility(false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }

    private void setMainColumnWidthWeight(View view, float weight) {
        view.setLayoutParams(
                new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, weight));
    }
}
