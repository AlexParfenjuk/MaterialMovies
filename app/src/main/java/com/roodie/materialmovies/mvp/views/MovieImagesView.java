package com.roodie.materialmovies.mvp.views;

import com.arellomobile.mvp.GenerateViewState;
import com.roodie.model.entities.MovieWrapper;

import java.util.List;

/**
 * Created by Roodie on 13.02.2016.
 */

@GenerateViewState
public interface MovieImagesView extends MvpLceView<List<MovieWrapper.BackdropImage>> {
}
