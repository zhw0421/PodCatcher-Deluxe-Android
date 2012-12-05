/** Copyright 2012 Kevin Hausmann
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
package net.alliknow.podcatcher.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.adapters.EpisodeListAdapter;
import net.alliknow.podcatcher.listeners.OnSelectEpisodeListener;
import net.alliknow.podcatcher.tasks.Progress;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.views.ProgressView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

/**
 * List fragment to display the list of episodes as part of the
 * podcast activity.
 */
public class EpisodeListFragment extends PodcatcherListFragment {

	/** The list of episode showing */
	private List<Episode> episodeList;
	/** The selected episode */
	private Episode selectedEpisode;
	
	/** The list view */
	private ListView listView;
	/** The empty view */
	private TextView emptyView;
	/** The progress bar */
	private ProgressView progressView;
	
	/** Caches for internal state */
	private boolean showProgress = false;
	private boolean showLoadFailed = false;
	
	/** The activity we are in (listens to user selection) */ 
    private OnSelectEpisodeListener selectedListener;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	setRetainInstance(true);
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.episode_list, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		listView = getListView();
		emptyView = (TextView) getView().findViewById(android.R.id.empty);
		progressView = (ProgressView) getView().findViewById(R.id.episode_list_progress);
		
		if (showProgress) clearAndSpin();
		else if (showLoadFailed) showLoadFailed();
	}
	
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		if (selectedListener != null) selectedListener.onEpisodeSelected(episodeList.get(position));
		else Log.d(getClass().getSimpleName(), "Episode selected, but no listener attached");
	}
	
	/**
	 * Select given episode, iff available in current episode list.
	 * If the episode is not available the list will go to "no-selection-state".
	 * @param selectedEpisode Episode to select.
	 */
	public void selectEpisode(Episode selectedEpisode) {
		this.selectedEpisode = selectedEpisode;
		
		// Episode is available
		if (episodeList != null && episodeList.contains(selectedEpisode)) {
			((EpisodeListAdapter) getListAdapter()).setSelectedPosition(episodeList.indexOf(selectedEpisode));
			scrollListView(episodeList.indexOf(selectedEpisode));
		}
		// Episode is not in the current episode list
		else selectNone();
	}
	
	/**
	 * Check whether there is an episode currently selected in the list.
	 * @return <code>true</code> if so, <code>false</code> otherwise. 
	 */
	public boolean isEpisodeSelected() {
		return selectedEpisode != null;
	}
	
	/**
	 * @param listener Listener to be alerted on episode selection.
	 */
	public void setEpisodeSelectedListener(OnSelectEpisodeListener listener) {
		this.selectedListener = listener;
	}
	
	/**
	 * Set the episode list to display and update the UI accordingly.
	 * @param list List of episodes to display.
	 */
	public void setEpisodeList(List<Episode> list) {
		if (list != null) {
			episodeList = list;
			setListAdapter(new EpisodeListAdapter(getActivity(), episodeList));
			
			processNewEpisodes();
		}
	}
	
	/**
	 * Add the episode list to the currenty displayed episodes
	 * and update the UI accordingly.
	 * @param list List of episode to add.
	 */
	public void addEpisodeList(List<Episode> list) {
		if (episodeList == null) episodeList = new ArrayList<Episode>();
			
		// TODO decide on this: episodeList.addAll(list.subList(0, list.size() > 100 ? 100 : list.size() - 1));
		episodeList.addAll(list);
		Collections.sort(episodeList);
		setListAdapter(new EpisodeListAdapter(getActivity(), episodeList, true));
		
		processNewEpisodes();
	}
	
	private void processNewEpisodes() {
		showProgress = false;
		showLoadFailed = false;
		
		// Reset internal variables
		selectedEpisode = null;
		if (selectedListener != null) selectedListener.onNoEpisodeSelected();
		
		// Update UI 
		if (episodeList.isEmpty()) emptyView.setText(R.string.no_episodes);
		updateUiElementVisibility();
	}
	
	/**
	 * Unselect selected episode (if any).
	 */
	public void selectNone() {
		selectedEpisode = null;
		if (getListAdapter() != null && !showProgress)
			((EpisodeListAdapter) getListAdapter()).setSelectNone();
	}
	
	/**
	 * Reset the UI to initial state.
	 */
	public void reset() {
		showProgress = false;
		showLoadFailed = false;
		
		selectedEpisode = null;
		episodeList = null;
		setListAdapter(null);
		
		emptyView.setText(R.string.no_podcast_selected);
		updateUiElementVisibility();
	}

	/**
	 * Show the UI to be working.
	 */
	public void clearAndSpin() {
		showProgress = true;
		showLoadFailed = false;
		
		progressView.reset();
		updateUiElementVisibility();
	}
	
	/**
	 * Update UI with load progress.
	 * @param progress Amount loaded or flag from load task.
	 */
	public void showProgress(Progress progress) {
		progressView.publishProgress(progress);
	}

	/**
	 * Show error view.
	 */
	public void showLoadFailed() {
		showProgress = false;
		showLoadFailed = true;
		
		progressView.showError(R.string.error_podcast_load);
		updateUiElementVisibility();
	}
	
	private void updateUiElementVisibility() {
		// Progress view is displaying information
		if (showProgress || showLoadFailed) {
			emptyView.setVisibility(GONE);
			listView.setVisibility(GONE);
			progressView.setVisibility(VISIBLE);
		} // Show the episode list or the empty view
		else {
			boolean episodesAvailable = episodeList != null && !episodeList.isEmpty();
			
			emptyView.setVisibility(episodesAvailable ? GONE : VISIBLE);
			listView.setVisibility(episodesAvailable ? VISIBLE : GONE);
			progressView.setVisibility(GONE);
		}
	}
}
