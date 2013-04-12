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

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;

import net.alliknow.podcatcher.listeners.OnChangePodcastListListener;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.listeners.OnSelectPodcastListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.view.fragments.EpisodeListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment;
import net.alliknow.podcatcher.view.fragments.PodcastListFragment.LogoViewMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Our main activity class. Works as the main controller. Depending on the view
 * state, other activities cooperate.
 */
public class PodcastActivity extends EpisodeListActivity implements
        OnLoadPodcastListListener, OnChangePodcastListListener, OnSelectPodcastListener {

    /** The current podcast list fragment */
    protected PodcastListFragment podcastListFragment;

    /**
     * Flag indicating whether the podcast list changed while the activity was
     * paused or stopped.
     */
    private boolean podcastListChanged = false;

    /**
     * Flag indicating whether the app should show the add podcast dialog if the
     * list of podcasts is empty.
     */
    private boolean showAddPodcastOnEmptyPodcastList = false;

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

        // 3. Init/restore the app as needed
        // If we are newly starting up and the podcast list is empty, show add
        // podcast dialog (this is used in onPodcastListLoaded(), since we only
        // know then, whether the list is actually empty.
        showAddPodcastOnEmptyPodcastList = savedInstanceState == null;
        // Check if podcast list is available - if so, set it
        List<Podcast> podcastList = podcastManager.getPodcastList();
        if (podcastList != null) {
            onPodcastListLoaded(podcastList);

            // We only reset our state if the podcast list is available, because
            // otherwise we will not be able to select anything.
            // There are two cases to cover here:
            // 1. We come back from a configuration change and restore from the
            // bundle saved at onSaveInstanceState()
            if (savedInstanceState != null)
                restoreState(savedInstanceState);
            // 2. We are (re)started and the intent might contain some
            // information we need to parse (this also works if it doesn't)
            else
                onNewIntent(getIntent());
        }
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
        if ((viewMode == SMALL_PORTRAIT_VIEW || viewMode == SMALL_LANDSCAPE_VIEW)
                && podcastListFragment == null) {
            podcastListFragment = new PodcastListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, podcastListFragment,
                            getString(R.string.podcast_list_fragment_tag))
                    .commit();
        }
        // On small screens in landscape mode, add the episode list fragment
        if (viewMode == SMALL_LANDSCAPE_VIEW && episodeListFragment == null) {
            episodeListFragment = new EpisodeListFragment();
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.right, episodeListFragment,
                            getString(R.string.episode_list_fragment_tag))
                    .commit();
        }
    }

    /**
     * Restore members and selection state from bundle.
     * 
     * @param savedInstanceState Restore information.
     */
    private void restoreState(Bundle savedInstanceState) {
        // Recover members
        multiplePodcastsMode = savedInstanceState.getBoolean(MODE_KEY);
        currentPodcast = podcastManager.findPodcastForUrl(
                savedInstanceState.getString(PODCAST_URL_KEY));
        currentEpisode = podcastManager.findEpisodeForUrl(
                savedInstanceState.getString(EPISODE_URL_KEY));

        restoreSelection();
    }

    /**
     * Restore selection to match member variables
     */
    private void restoreSelection() {
        // Re-select previously selected podcast(s)
        if (multiplePodcastsMode)
            onAllPodcastsSelected();
        else if (currentPodcast != null)
            onPodcastSelected(currentPodcast);
        else
            onNoPodcastSelected();

        // Re-select previously selected episode
        if (currentEpisode != null)
            onEpisodeSelected(currentEpisode);
        else
            onNoEpisodeSelected();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Recover members
        multiplePodcastsMode = intent.getBooleanExtra(MODE_KEY, false);
        currentPodcast = podcastManager.findPodcastForUrl(
                intent.getStringExtra(PODCAST_URL_KEY));
        currentEpisode = podcastManager.findEpisodeForUrl(
                intent.getStringExtra(EPISODE_URL_KEY));

        restoreSelection();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Load all podcasts? TODO Make this a preference

        // Set podcast logo view mode
        updateLogoViewMode();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reset podcast list fragment in small portrait mode
        if (viewMode == SMALL_PORTRAIT_VIEW && multiplePodcastsMode)
            podcastListFragment.selectNone();

        // Podcast list has been changed while we were stopped
        if (podcastListChanged) {
            // Reset flag
            podcastListChanged = false;

            // Update podcast list
            podcastListFragment.setPodcastList(podcastManager.getPodcastList());

            // Update UI
            updateActionBar();

            // Only act if we are not in select all mode
            if (!multiplePodcastsMode) {
                // Selected podcast was deleted
                if (currentPodcast == null)
                    onNoPodcastSelected();
                // Show the last podcast added if not in small portrait mode
                else if (viewMode != SMALL_PORTRAIT_VIEW)
                    onPodcastSelected(currentPodcast);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Make sure we persist the podcast manager state
        podcastManager.saveState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(MODE_KEY, multiplePodcastsMode);
        if (currentPodcast != null)
            outState.putString(PODCAST_URL_KEY, currentPodcast.getUrl().toString());
        if (currentEpisode != null)
            outState.putString(EPISODE_URL_KEY, currentEpisode.getMediaUrl().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the listeners
        podcastManager.removeLoadPodcastListListener(this);
        podcastManager.removeChangePodcastListListener(this);
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Make podcast list show
        podcastListFragment.setPodcastList(podcastList);

        // Make action bar show number of podcasts
        updateActionBar();

        // If podcast list is empty we show dialog on startup
        if (podcastManager.size() == 0 && showAddPodcastOnEmptyPodcastList) {
            showAddPodcastOnEmptyPodcastList = false;
            startActivity(new Intent(this, AddPodcastActivity.class));
        }
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        // Pick up the change in onRestart()
        podcastListChanged = true;

        // Set the member
        currentPodcast = podcast;
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        // Pick up the change in onRestart()
        podcastListChanged = true;

        // Reset member if deleted
        if (podcast.equals(currentPodcast))
            currentPodcast = null;
    }

    @Override
    public void onPodcastSelected(Podcast podcast) {
        this.currentPodcast = podcast;
        this.currentEpisodeList = null;
        this.multiplePodcastsMode = false;

        switch (viewMode) {
            case SMALL_LANDSCAPE_VIEW:
                // This will go back to the list view in case we are showing
                // episode details
                getFragmentManager().popBackStackImmediate();
                // There is no break here on purpose, we need to run the code
                // below as well
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                // List fragment is visible, make it show progress UI
                episodeListFragment.resetAndSpin();
                // Update other UI
                updateLogoViewMode();
                updateDivider();

                // Load podcast
                podcastManager.load(podcast);
                // Select in podcast list
                podcastListFragment.select(podcastManager.indexOf(podcast));

                break;
            case SMALL_PORTRAIT_VIEW:
                // We need to launch a new activity to display the episode list
                Intent intent = new Intent(this, ShowEpisodeListActivity.class);
                intent.putExtra(EpisodeListActivity.PODCAST_URL_KEY,
                        podcast.getUrl().toString());
                intent.putExtra(MODE_KEY, false);

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    @Override
    public void onAllPodcastsSelected() {
        this.currentPodcast = null;
        this.currentEpisodeList = new ArrayList<Episode>();
        this.multiplePodcastsMode = true;

        // Prepare podcast list fragment
        podcastListFragment.selectAll();

        switch (viewMode) {
            case SMALL_LANDSCAPE_VIEW:
                // This will go back to the list view in case we are showing
                // episode details
                getFragmentManager().popBackStackImmediate();
                // There is no break here on purpose, we need to run the code
                // below as well
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                // List fragment is visible, make it show progress UI
                episodeListFragment.resetAndSpin();
                episodeListFragment.setShowPodcastNames(true);
                // Update other UI
                updateLogoViewMode();
                updateDivider();

                for (Podcast podcast : podcastManager.getPodcastList())
                    podcastManager.load(podcast);

                break;
            case SMALL_PORTRAIT_VIEW:
                // We need to launch a new activity to display the episode list
                Intent intent = new Intent(this, ShowEpisodeListActivity.class);
                intent.putExtra(MODE_KEY, true);

                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        }
    }

    @Override
    public void onNoPodcastSelected() {
        this.currentPodcast = null;
        this.currentEpisodeList = null;
        this.multiplePodcastsMode = false;

        // Reset podcast list fragment
        podcastListFragment.selectNone();

        switch (viewMode) {
            case SMALL_LANDSCAPE_VIEW:
            case LARGE_PORTRAIT_VIEW:
            case LARGE_LANDSCAPE_VIEW:
                // If there is an episode list visible, reset it
                episodeListFragment.selectNone();
                episodeListFragment.resetUi();

                // Update other UI
                updateLogoViewMode();
                updateDivider();
                break;
        }
    }

    @Override
    public void onPodcastLoadProgress(Podcast podcast, Progress progress) {
        // Only react on progress here, if the activity is visible
        if (viewMode != SMALL_PORTRAIT_VIEW) {
            super.onPodcastLoadProgress(podcast, progress);

            // We are in select all mode, show progress in podcast list
            if (multiplePodcastsMode)
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
        if (viewMode != SMALL_PORTRAIT_VIEW)
            super.onPodcastLoaded(podcast);
    }

    @Override
    public void onPodcastLoadFailed(Podcast failedPodcast) {
        podcastListFragment.refresh();

        // In small portrait mode, work is done in separate activity
        if (viewMode != SMALL_PORTRAIT_VIEW)
            super.onPodcastLoadFailed(failedPodcast);
    }

    @Override
    public void onPodcastLogoLoaded(Podcast podcast) {
        super.onPodcastLogoLoaded(podcast);

        updateLogoViewMode();
    }

    /**
     * Update the logo view mode according to current app state.
     */
    protected void updateLogoViewMode() {
        LogoViewMode logoViewMode = LogoViewMode.NONE;

        if (viewMode == LARGE_LANDSCAPE_VIEW && !multiplePodcastsMode)
            logoViewMode = LogoViewMode.LARGE;
        else if (viewMode == SMALL_PORTRAIT_VIEW)
            logoViewMode = LogoViewMode.SMALL;

        podcastListFragment.setLogoVisibility(logoViewMode);
    }

    @Override
    protected void updateActionBar() {
        getActionBar().setTitle(R.string.app_name);

        // Disable the home button (only used in overlaying activities)
        getActionBar().setHomeButtonEnabled(false);

        if (podcastManager.getPodcastList() != null) {
            int podcastCount = podcastManager.size();
            getActionBar().setSubtitle(
                    podcastCount == 1 ? getString(R.string.one_podcast_selected) : podcastCount
                            + " " + getString(R.string.podcasts_selected));
        } else {
            getActionBar().setSubtitle(null);
        }
    }

    @Override
    protected void updatePlayer() {
        super.updatePlayer();

        if (viewMode == SMALL_PORTRAIT_VIEW && playerFragment != null) {
            playerFragment.setLoadMenuItemVisibility(false, false);
            playerFragment.setPlayerTitleVisibility(true);
        }
    }
}
