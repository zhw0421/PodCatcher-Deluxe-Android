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

package net.alliknow.podcatcher.model;

import net.alliknow.podcatcher.Podcatcher;
import net.alliknow.podcatcher.listeners.OnChangePlaylistListener;
import net.alliknow.podcatcher.listeners.OnLoadPlaylistListener;
import net.alliknow.podcatcher.model.tasks.LoadPlaylistTask;
import net.alliknow.podcatcher.model.types.Episode;
import net.alliknow.podcatcher.model.types.EpisodeMetadata;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Episode manager in the episode manager stack that cares for the playlist.
 * 
 * @see EpisodeManager
 */
public abstract class EpisodePlaylistManager extends EpisodeDownloadManager {

    /** Helper to make playlist methods more efficient */
    private int playlistSize = -1;

    /** The call-back set for the playlist listeners */
    private Set<OnChangePlaylistListener> playlistListeners = new HashSet<OnChangePlaylistListener>();

    /**
     * Init the episode playlist manager.
     * 
     * @param app The podcatcher application object (also a singleton).
     */
    protected EpisodePlaylistManager(Podcatcher app) {
        super(app);
    }

    /**
     * @return The current playlist. Might be empty but not <code>null</code>.
     *         Only call this if you are sure the metadata is already available,
     *         if in doubt use {@link LoadPlaylistTask}.
     * @see LoadPlaylistTask
     * @see OnLoadPlaylistListener
     */
    public List<Episode> getPlaylist() {
        // The resulting playlist
        TreeMap<Integer, Episode> playlist = new TreeMap<Integer, Episode>();

        // This is only possible if the metadata is available
        if (metadata != null) {
            // Find playlist entries from metadata
            Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<URL, EpisodeMetadata> entry = iterator.next();

                // Find records for playlist entries
                if (entry.getValue().playlistPosition != null) {
                    // Create and add the downloaded episode
                    Episode playlistEntry = entry.getValue().marshalEpisode(entry.getKey());
                    playlist.put(entry.getValue().playlistPosition, playlistEntry);
                }
            }

            // Since we have the playlist here, we could just as well set this
            // and make the other methods return faster
            this.playlistSize = playlist.size();
        }

        return new ArrayList<Episode>(playlist.values());
    }

    /**
     * @return The number of episodes in the playlist.
     */
    public int getPlaylistSize() {
        if (playlistSize == -1 && metadata != null)
            initPlaylistCounter();

        return playlistSize == -1 ? 0 : playlistSize;
    }

    /**
     * @return Whether the current playlist has any entries.
     */
    public boolean isPlaylistEmpty() {
        if (playlistSize == -1 && metadata != null)
            initPlaylistCounter();

        return playlistSize <= 0;
    }

    /**
     * Check whether a specific episode already exists in the playlist.
     * 
     * @param episode Episode to check for.
     * @return <code>true</code> iff present in playlist.
     */
    public boolean isInPlaylist(Episode episode) {
        return getPlaylistPosition(episode) != -1;
    }

    /**
     * Find the position of the given episode in the playlist.
     * 
     * @param episode Episode to find.
     * @return The position of the episode (staring at 0) or -1 if not present.
     */
    public int getPlaylistPosition(Episode episode) {
        int result = -1;

        if (episode != null && metadata != null) {
            // Find metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null && meta.playlistPosition != null)
                result = meta.playlistPosition;
        }

        return result;
    }

    /**
     * Add an episode to the playlist. The episode will be appended to the end
     * of the list.
     * 
     * @param episode The episode to add.
     */
    public void appendToPlaylist(Episode episode) {
        if (episode != null && metadata != null) {
            // Only append the episode if it is not already part of the playlist
            final List<Episode> playlist = getPlaylist();
            if (!playlist.contains(episode)) {
                final int position = playlist.size();

                // Find or create the metadata information holder
                EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
                if (meta == null) {
                    meta = new EpisodeMetadata();
                    metadata.put(episode.getMediaUrl(), meta);
                }

                // Put metadata information
                meta.playlistPosition = position;
                putAdditionalEpisodeInformation(episode, meta);

                // Increment counter
                if (playlistSize != -1)
                    playlistSize++;

                // Alert listeners
                for (OnChangePlaylistListener listener : playlistListeners)
                    listener.onPlaylistChanged();

                // Mark metadata record as dirty
                metadataChanged = true;
            }
        }
    }

    /**
     * Delete given episode off the playlist.
     * 
     * @param episode Episode to pop.
     */
    public void removeFromPlaylist(Episode episode) {
        if (episode != null && metadata != null) {
            // Find the metadata information holder
            EpisodeMetadata meta = metadata.get(episode.getMediaUrl());
            if (meta != null && meta.playlistPosition != null) {
                // Update the playlist positions for all entries beyond the one
                // we are removing
                Iterator<Entry<URL, EpisodeMetadata>> iterator = metadata.entrySet().iterator();
                while (iterator.hasNext()) {
                    EpisodeMetadata other = iterator.next().getValue();

                    // Find records for playlist entries
                    if (other.playlistPosition != null
                            && other.playlistPosition > meta.playlistPosition)
                        other.playlistPosition--;
                }

                // Reset the playlist position for given episode
                meta.playlistPosition = null;

                // Decrement counter
                if (playlistSize != -1)
                    playlistSize--;

                // Alert listeners
                for (OnChangePlaylistListener listener : playlistListeners)
                    listener.onPlaylistChanged();

                // Mark metadata record as dirty
                metadataChanged = true;
            }
        }
    }

    /**
     * Add a playlist listener.
     * 
     * @param listener Listener to add.
     * @see OnChangePlaylistListener
     */
    public void addPlaylistListener(OnChangePlaylistListener listener) {
        playlistListeners.add(listener);
    }

    /**
     * Remove a playlist listener.
     * 
     * @param listener Listener to remove.
     * @see OnChangePlaylistListener
     */
    public void removePlaylistListener(OnChangePlaylistListener listener) {
        playlistListeners.remove(listener);
    }

    private void initPlaylistCounter() {
        this.playlistSize = 0;

        for (EpisodeMetadata meta : metadata.values())
            if (meta.playlistPosition != null)
                playlistSize++;
    }
}
