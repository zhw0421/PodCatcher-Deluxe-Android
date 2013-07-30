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

package net.alliknow.podcatcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.types.Episode;

/**
 * A list item view to represent an episode.
 */
public class EpisodeListItemView extends LinearLayout {

    /** String to use if no episode publication date available */
    private static final String NO_DATE = "---";
    /** Separator for date and podcast name */
    private static final String SEPARATOR = " • ";

    /** The title text view */
    private TextView titleTextView;
    /** The caption text view */
    private TextView captionTextView;

    /**
     * Create an episode item list view.
     * 
     * @param context Context for the view to live in.
     * @param attrs View attributes.
     */
    public EpisodeListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        titleTextView = (TextView) findViewById(R.id.list_item_title);
        captionTextView = (TextView) findViewById(R.id.list_item_caption);
    }

    /**
     * Make the view update all its child to represent input given.
     * 
     * @param episode Episode to represent.
     * @param showPodcastName Whether the podcast name should show.
     */
    public void show(final Episode episode, boolean showPodcastName) {
        // 1. Set episode title
        titleTextView.setText(createTitle(episode));

        // 2. Set caption
        captionTextView.setText(createCaption(episode, showPodcastName));
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

    private String createCaption(Episode episode, boolean showPodcastName) {
        // This should not happen (but we cover it)
        if (episode.getPubDate() == null && !showPodcastName)
            return NO_DATE;
        // Episode has no date, should not happen
        else if (episode.getPubDate() == null && showPodcastName)
            return episode.getPodcast().getName();
        // This is the interesting case
        else {
            // Get a nice time span string for the age of the episode
            String dateString = Utils.getRelativePubDate(episode);

            // Append podcast name
            if (showPodcastName)
                return dateString + SEPARATOR + episode.getPodcast().getName();
            // Omit podcast name
            else
                return dateString;
        }
    }
}
