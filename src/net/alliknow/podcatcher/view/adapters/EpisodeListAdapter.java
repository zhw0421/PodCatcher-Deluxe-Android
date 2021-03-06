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

package net.alliknow.podcatcher.view.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.view.Utils;

import java.util.List;

/**
 * Adapter class used for the list of episodes.
 */
public class EpisodeListAdapter extends PodcatcherBaseListAdapter {

    /** The list our data resides in */
    protected final List<Episode> list;
    /** The episode manager handle */
    protected final EpisodeManager episodeManager;
    /** Whether the podcast name should be shown */
    protected boolean showPodcastNames = false;

    /** String to use if no episode publication date available */
    private static final String NO_DATE = "---";
    /** Separator for date and podcast name */
    private static final String SEPARATOR = " • ";

    /**
     * Create new adapter.
     * 
     * @param context The activity.
     * @param episodeList The list of episodes to show in list.
     */
    public EpisodeListAdapter(Context context, List<Episode> episodeList) {
        super(context);

        this.list = episodeList;
        this.episodeManager = EpisodeManager.getInstance();
    }

    /**
     * Set whether the podcast name for the episode should be shown. This will
     * redraw the list and take effect immediately.
     * 
     * @param show Whether to show each episode's podcast name.
     */
    public void setShowPodcastNames(boolean show) {
        this.showPodcastNames = show;

        notifyDataSetChanged();
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
        // Get the return view (possibly recycle a used one)
        View listItemView = findReturnView(convertView, parent, R.layout.episode_list_item);

        // Set list item color background
        setBackgroundColorForPosition(listItemView, position);

        // Find episode to represent
        final Episode episode = list.get(position);

        // Set the text to display for title
        setText(listItemView, R.id.list_item_title, createTitle(episode));
        // Set the text to display as caption
        setText(listItemView, R.id.list_item_caption, createCaption(episode));
        // Update the icons to show for this episode
        updateIcons(listItemView, episode);

        return listItemView;
    }

    private String createTitle(Episode episode) {
        final String episodeName = episode.getName();
        final String redundantPrefix = episode.getPodcast().getName() + " ";
        final String redundantPrefixAlt = episode.getPodcast().getName() + ": ";
        // Remove podcast name from the episode title because it takes to much
        // space and is redundant anyway
        if (episodeName.startsWith(redundantPrefix))
            return episodeName.substring(redundantPrefix.length(), episodeName.length());
        else if (episodeName.startsWith(redundantPrefixAlt))
            return episodeName.substring(redundantPrefixAlt.length(), episodeName.length());
        else
            return episodeName;
    }

    private String createCaption(Episode episode) {
        String result = NO_DATE;

        // Episode has no date, should not happen
        if (episode.getPubDate() == null && showPodcastNames)
            result = episode.getPodcast().getName();
        // This is the interesting case
        else {
            // Get a nice time span string for the age of the episode
            String dateString = Utils.getRelativePubDate(episode);

            // Append podcast name
            if (showPodcastNames)
                result = dateString + SEPARATOR + episode.getPodcast().getName();
            // Omit podcast name
            else
                result = dateString;
        }

        // Also put the playlist position
        final int position = episodeManager.getPlaylistPosition(episode);
        if (position >= 0)
            result = "#" + (position + 1) + SEPARATOR + result;

        return result;
    }

    private void updateIcons(View listItemView, Episode episode) {
        ImageView downloadIconView = (ImageView) listItemView.findViewById(R.id.download_icon);
        ImageView stateIconView = (ImageView) listItemView.findViewById(R.id.state_icon);

        final boolean downloading = episodeManager.isDownloading(episode);
        final boolean downloaded = episodeManager.isDownloaded(episode);
        final boolean isNew = !episodeManager.getState(episode);

        if (downloading)
            downloadIconView.setImageResource(R.drawable.ic_media_downloading);
        else if (downloaded)
            downloadIconView.setImageResource(R.drawable.ic_media_downloaded);

        downloadIconView.setVisibility(downloading || downloaded ? View.VISIBLE : View.GONE);
        stateIconView.setVisibility(isNew ? View.VISIBLE : View.GONE);
    }
}
