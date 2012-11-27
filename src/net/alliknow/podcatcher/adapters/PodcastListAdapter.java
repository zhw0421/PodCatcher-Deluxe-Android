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
package net.alliknow.podcatcher.adapters;

import net.alliknow.podcatcher.PodcastList;
import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.views.HorizontalProgressView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Adapter class used for the list of podcasts.
 */
public class PodcastListAdapter extends PodcatcherBaseListAdapter {

	/** The list our data resides in */
	protected PodcastList list;

	/** String resources used */
	protected final String oneEpisode;
	protected final String episodes;
	
	/**
	 * Create new adapter.
	 * 
	 * @param context The current context.
	 * @param podcastList List of podcasts to wrap (not <code>null</code>).
	 */
	public PodcastListAdapter(Context context, PodcastList podcastList) {
		super(context);
		
		this.list = podcastList;
		oneEpisode = context.getResources().getString(R.string.one_episode);
		episodes = context.getResources().getString(R.string.episodes);
	}
	
	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return list.get(position).hashCode();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = findReturnView(convertView, parent, R.layout.list_item);
		
		int numberOfEpisodes = list.get(position).getEpisodes().size();
		setTextAndState(convertView, R.id.list_item_title, list.get(position).getName(), position);
		setTextAndState(convertView, R.id.list_item_caption, createCaption(numberOfEpisodes), position);
		
		// Show progress on select all podcasts?
		HorizontalProgressView progressView = (HorizontalProgressView)convertView.findViewById(R.id.list_item_progress);
		progressView.setVisibility(numberOfEpisodes == 0 && selectAll ? View.VISIBLE : View.GONE);
		// Not if episodes are already available...
		View episodeNumberView = convertView.findViewById(R.id.list_item_caption);
		episodeNumberView.setVisibility(numberOfEpisodes != 0 ? View.VISIBLE : View.GONE);
		
		return convertView;
	}

	private String createCaption(int numberOfEpisodes) {
		return numberOfEpisodes == 1 ? oneEpisode :	numberOfEpisodes + " " + episodes;
	}
}
