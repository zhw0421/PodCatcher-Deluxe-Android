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

package net.alliknow.podcatcher.adapters;

import android.content.Context;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.model.types.Genre;
import net.alliknow.podcatcher.view.fragments.SuggestionFragment;

/**
 * Adapter for the genre spinner in the suggestion dialog.
 */
public class GenreSpinnerAdapter extends SuggestionFilterSpinnerAdapter {

    /**
     * Create new adapter.
     * 
     * @param context The current context.
     */
    public GenreSpinnerAdapter(Context context) {
        super(context);

        // Put all genres into the value map where they are sorted by language
        // because we are using the corresponding resources as keys
        for (int index = 0; index < Genre.values().length; index++) {
            final String key = resources.getStringArray(R.array.genres)[index];
            values.put(key, Genre.values()[index]);
        }
    }

    @Override
    public Object getItem(int position) {
        if (position == 0)
            return SuggestionFragment.FILTER_WILDCARD;
        else
            return values.values().toArray()[position - 1];
    }

    @Override
    public int getCount() {
        return Genre.values().length + 1;
    }
}
