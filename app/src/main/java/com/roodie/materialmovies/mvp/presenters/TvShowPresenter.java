package com.roodie.materialmovies.mvp.presenters;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.google.common.base.Preconditions;
import com.roodie.materialmovies.MMoviesApp;
import com.roodie.materialmovies.mvp.views.TvShowWatchedView;
import com.roodie.model.entities.ShowWrapper;
import com.roodie.model.state.MoviesState;
import com.roodie.model.tasks.DatabaseBackgroundRunnable;
import com.roodie.model.tasks.MarkEntitySeenRunnable;
import com.roodie.model.tasks.MarkEntityUnseenRunnable;
import com.squareup.otto.Subscribe;

/**
 * Created by Roodie on 16.04.2016.
 */

@InjectViewState
public class TvShowPresenter extends MvpPresenter<TvShowWatchedView> {

    public static final String TAG = "TvShowPresenter";

    public TvShowPresenter() {
        super();
        MMoviesApp.get().getState().registerForEvents(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MMoviesApp.get().getState().unregisterForEvents(this);
    }

    public <BR> void executeBackgroundTask(DatabaseBackgroundRunnable<BR> task) {
        MMoviesApp.get().inject(task);
        MMoviesApp.get().getBackgroundExecutor().execute(task);
    }

    public int getId(TvShowWatchedView view) {
        return view.hashCode();
    }

    public void toggleShowWatched(ShowWrapper item, int position) {
        Preconditions.checkNotNull(item, "show cannot be null");
        if (item.isWatched()) {
            markShowUnseen(position, item);
        } else {
            markShowSeen(position, item);
        }
    }

    private void markShowSeen(int callingId, ShowWrapper item) {
        executeBackgroundTask(new MarkEntitySeenRunnable(callingId, item));
    }

    private void markShowUnseen(int callingId, ShowWrapper item) {
        executeBackgroundTask(new MarkEntityUnseenRunnable(callingId, item));

    }

    @Subscribe
    public void onShowWatchedChanged(MoviesState.ShowFlagUpdateEvent event) {
       // updateShowWatched(event.item, event.callingId);
    }


    public void updateShowWatched(ShowWrapper show, int position) {
        Preconditions.checkNotNull(show, "show cannot be null");
            getViewState().updateShowWatched(show, position);
    }




}
