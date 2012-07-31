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
package net.alliknow.podcatcher;


import net.alliknow.podcatcher.EpisodeListFragment.OnEpisodeSelectedListener;
import net.alliknow.podcatcher.PodcastListFragment.OnPodcastSelectedListener;
import net.alliknow.podcatcher.tasks.LoadPodcastLogoTask;
import net.alliknow.podcatcher.tasks.LoadPodcastTask;
import net.alliknow.podcatcher.types.Episode;
import net.alliknow.podcatcher.types.Podcast;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

public class PodcastActivity extends Activity implements OnPodcastSelectedListener, OnEpisodeSelectedListener {

	/** The current podcast load task */
	private LoadPodcastTask loadPodcastTask;
	/** The current podcast logo load task */
	private LoadPodcastLogoTask loadPodcastLogoTask;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.main);
	}

	@Override
	public void onPodcastSelected(Podcast podcast) {
		// Stopp loading previous tasks
		if (this.loadPodcastTask != null) this.loadPodcastTask.cancel(true);
		if (this.loadPodcastLogoTask != null) this.loadPodcastLogoTask.cancel(true);
		
		// Download podcast RSS feed (async)
		this.loadPodcastTask = new LoadPodcastTask(this);
		this.loadPodcastTask.execute(podcast);
	}
	
	/**
	 * Notified by async RSS file loader on completion.
	 * Updates UI to display the podcast's episodes.
	 * @param podcast Podcast RSS feed loaded for
	 */
	public void onPodcastLoaded(Podcast podcast) {
		this.loadPodcastTask = null;
		
		EpisodeListFragment elf = (EpisodeListFragment) getFragmentManager().findFragmentById(R.id.episode_list);
		elf.setPodcast(podcast);
		
		// Download podcast logo
		if (podcast.getLogoUrl() != null) {
			this.loadPodcastLogoTask = new LoadPodcastLogoTask(this);
			this.loadPodcastLogoTask.execute(podcast);
		} else Log.d("Logo", "No logo for podcast " + podcast);
	}
	
	public void onPodcastLogoLoaded(Bitmap logo) {
		this.loadPodcastLogoTask = null;
		
		PodcastListFragment plf = (PodcastListFragment) getFragmentManager().findFragmentById(R.id.podcast_list);
		plf.setPodcastLogo(logo);
	}
	
	/**
	 * Notified by the async RSS loader on failure.
	 */
	public void onPodcastLoadFailed() {
		this.loadPodcastTask = null;
	}

	@Override
	public void onEpisodeSelected(Episode selectedEpisode) {
		EpisodeFragment ef = (EpisodeFragment) getFragmentManager().findFragmentById(R.id.episode);
		ef.setEpisode(selectedEpisode);
	}
}
