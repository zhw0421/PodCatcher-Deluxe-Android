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

package net.alliknow.podcatcher.view.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.listeners.EpisodeListContextListener;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.listeners.OnToggleFilterListener;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.adapters.EpisodeListAdapter;

import java.util.List;

/**
 * List fragment to display the list of episodes.
 */
public class EpisodeListFragment extends PodcatcherListFragment {

    /** The list of episodes we are currently showing. */
    private List<Episode> currentEpisodeList;

    /** The activity we are in (listens to user selection) */
    private OnSelectEpisodeListener selectionListener;
    /** The activity we are in (listens to filter toggles) */
    private OnToggleFilterListener filterListener;

    /** Flag for show filter menu item state */
    private boolean showFilterMenuItem = false;
    /** Flag for the state of the filter menu item */
    private boolean filterMenuItemState = false;
    /** Flag to indicate whether podcast names should be shown for episodes */
    private boolean showPodcastNames = false;

    /** Status flag indicating that our view is created */
    private boolean viewCreated = false;

    /** The download episode menu bar item */
    private MenuItem filterMenuItem;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.selectionListener = (OnSelectEpisodeListener) activity;
            this.filterListener = (OnToggleFilterListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectEpisodeListener and OnFilterToggleListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(R.layout.episode_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewCreated = true;

        // Set list choice listener (context action mode)
        getListView().setMultiChoiceModeListener(new EpisodeListContextListener(this));

        // This will make sure we show the right information once the view
        // controls are established (the list might have been set earlier)
        if (currentEpisodeList != null)
            setEpisodeList(currentEpisodeList);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.episode_list, menu);

        filterMenuItem = menu.findItem(R.id.filter_menuitem);
        setFilterMenuItemVisibility(showFilterMenuItem, filterMenuItemState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_menuitem:
                // Tell activity to toggle the filter
                filterListener.onToggleFilter();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        // Find selected episode and alert listener
        Episode selectedEpisode = (Episode) adapter.getItem(position);
        selectionListener.onEpisodeSelected(selectedEpisode);
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set the list of episodes to show in this fragment. You can call this any
     * time and the view will catch up as soon as it is created.
     * 
     * @param episodeList List of episodes to show.
     */
    public void setEpisodeList(List<Episode> episodeList) {
        this.currentEpisodeList = episodeList;

        showProgress = false;
        showLoadFailed = false;

        // Update UI
        if (viewCreated) {
            // Update the list
            EpisodeListAdapter adapter = new EpisodeListAdapter(getActivity(), episodeList);
            adapter.setShowPodcastNames(showPodcastNames);

            setListAdapter(adapter);

            // Update other UI elements
            if (episodeList.isEmpty())
                emptyView.setText(R.string.no_episodes);

            // Make sure to match selection state
            if (selectAll)
                selectAll();
            else if (selectedPosition >= 0 && selectedPosition < episodeList.size())
                select(selectedPosition);
            else
                selectNone();
        }
    }

    /**
     * Set whether the fragment should show the filter icon. You can call this
     * any time and can expect it to happen on fragment resume at the latest.
     * You also have to set the filter icon state, <code>true</code> for
     * "new only" and <code>false</code> for "show all".
     * 
     * @param show Whether to show the filter menu item.
     * @param filter State of the filter menu item (new / all)
     */
    public void setFilterMenuItemVisibility(boolean show, boolean filter) {
        this.showFilterMenuItem = show;
        this.filterMenuItemState = filter;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (filterMenuItem != null) {
            filterMenuItem.setVisible(showFilterMenuItem);
            filterMenuItem.setTitle(filterMenuItemState ? R.string.all : R.string.new_only);
            filterMenuItem.setIcon(filterMenuItemState ?
                    R.drawable.ic_menu_filter_back : R.drawable.ic_menu_filter);
        }
    }

    /**
     * Set whether the fragment should show the podcast name for each episode
     * item. Change will be reflected upon next call of
     * {@link #setEpisodeList(List)}
     * 
     * @param show Whether to show the podcast names.
     */
    public void setShowPodcastNames(boolean show) {
        this.showPodcastNames = show;
    }

    @Override
    protected void reset() {
        if (viewCreated)
            emptyView.setText(R.string.no_podcast_selected);

        showPodcastNames = false;

        super.reset();
    }

    /**
     * Show error view.
     */
    @Override
    public void showLoadFailed() {
        if (viewCreated)
            progressView.showError(R.string.error_podcast_load);

        super.showLoadFailed();
    }
}
