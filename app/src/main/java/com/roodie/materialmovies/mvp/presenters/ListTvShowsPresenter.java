package com.roodie.materialmovies.mvp.presenters;


import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.roodie.materialmovies.MMoviesApp;
import com.roodie.materialmovies.R;
import com.roodie.materialmovies.mvp.views.ListTvShowsView;
import com.roodie.materialmovies.mvp.views.UiView;
import com.roodie.model.entities.ShowWrapper;
import com.roodie.model.state.ApplicationState;
import com.roodie.model.state.BaseState;
import com.roodie.model.state.MoviesState;
import com.roodie.model.tasks.BaseRunnable;
import com.roodie.model.tasks.FetchOnTheAirShowsRunnable;
import com.roodie.model.tasks.FetchPopularShowsRunnable;
import com.roodie.model.tasks.FetchSearchShowRunnable;
import com.roodie.model.util.MoviesCollections;
import com.squareup.otto.Subscribe;

import java.util.List;

/**
 * Created by Roodie on 22.03.2016.
 */
@InjectViewState
public class ListTvShowsPresenter extends MvpPresenter<ListTvShowsView> implements BaseListPresenter<ListTvShowsView> {

    public ListTvShowsPresenter() {
        super();
        MMoviesApp.get().getState().registerForEvents(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MMoviesApp.get().getState().unregisterForEvents(this);
    }

    @Subscribe
    public void onSearchResultChanged(MoviesState.SearchResultChangedEvent event) {
        populateUiFromEvent(event, UiView.MMoviesQueryType.SEARCH_PEOPLE);
    }

    @Subscribe
    public void onPopularChanged(MoviesState.PopularShowsChangedEvent event) {
        populateUiFromEvent(event, UiView.MMoviesQueryType.POPULAR_SHOWS);
    }

    @Subscribe
    public void onTheAirChanged(MoviesState.OnTheAirShowsChangedEvent event) {
        populateUiFromEvent(event, UiView.MMoviesQueryType.ON_THE_AIR_SHOWS);
    }

    @Subscribe
    public void onNetworkError(BaseState.OnErrorEvent event) {
        ListTvShowsView ui = findUi(event.callingId);
        if (ui != null) {
            ui.showError(event.error);
        }
    }

    @Subscribe
    public void onLoadingProgressVisibilityChanged(BaseState.ShowLoadingProgressEvent event) {
        ListTvShowsView ui = findUi(event.callingId);
        if (ui != null) {
            if (!event.secondary) {
                getViewState().showLoadingProgress(event.show);
            }
        }
    }

    public void search(ListTvShowsView view, UiView.MMoviesQueryType queryType, String query) {
        final int callingId = getId(view);
        switch (queryType) {
            case SEARCH_SHOWS:
                fetchTvShowsSearchResults(callingId, query);
                break;
        }
    }

    @Override
    public void onUiAttached(ListTvShowsView view, UiView.MMoviesQueryType queryType, String parameter) {
        String title = null;
        final int callingId = getId(view);
        switch (queryType) {
            case SEARCH_SHOWS:
                title = getUiTitle(queryType);
                view.updateDisplayTitle(title);
                break;
            case POPULAR_SHOWS:
                fetchPopularIfNeeded(callingId);
                break;
            case ON_THE_AIR_SHOWS:
                fetchOnTheAirIfNeeded(callingId);
                break;
        }
        populateUi(view, queryType);
    }

    @Override
    public String getUiTitle(UiView.MMoviesQueryType queryType) {
        switch (queryType) {
            case SEARCH_SHOWS: {
                MoviesState.SearchResult result = MMoviesApp.get().getState().getSearchResult();
                if (result != null) {
                    return result.query;
                } else {
                    return MMoviesApp.get().getStringFetcher().getString(R.string.search_title);
                }
            }
            case POPULAR_SHOWS:
                return MMoviesApp.get().getStringFetcher().getString(R.string.popular_title);
            case ON_THE_AIR_SHOWS:
                return MMoviesApp.get().getStringFetcher().getString(R.string.on_the_air_title);
        }
        return null;
    }

    @Override
    public String getUiSubtitle(UiView.MMoviesQueryType queryType) {
        switch (queryType) {
            case SEARCH_SHOWS:
                return MMoviesApp.get().getStringFetcher().getString(R.string.shows_title);
        }
        return null;
    }

    @Override
    public void populateUi(ListTvShowsView view, UiView.MMoviesQueryType queryType) {

        List<ShowWrapper> items = null;
        switch (queryType) {
            case SEARCH_SHOWS:
                MoviesState.SearchResult searchResult = MMoviesApp.get().getState().getSearchResult();
                if (searchResult != null && searchResult.shows != null) {
                    items = searchResult.shows.items;
                    view.updateDisplaySubtitle(getUiSubtitle(UiView.MMoviesQueryType.SEARCH_SHOWS));
                }
                break;
            case POPULAR_SHOWS:
                ApplicationState.ShowPaginatedResult popular = MMoviesApp.get().getState().getPopularShows();
                if (popular != null) {
                    items = popular.items;
                }
                break;
            case ON_THE_AIR_SHOWS:
                ApplicationState.ShowPaginatedResult onTheAir = MMoviesApp.get().getState().getOnTheAirShows();
                if (onTheAir != null) {
                    items = onTheAir.items;
                }
                break;
        }
        view.setData(items);
    }

    @Override
    public boolean canFetchNextPage(BaseState.PaginatedResult<?> paginatedResult) {
        return paginatedResult != null && paginatedResult.page < paginatedResult.totalPages;
    }

    @Override
    public void refresh(ListTvShowsView view, UiView.MMoviesQueryType queryType) {
        final int callingId = getId(view);

        switch (queryType) {
            case POPULAR_SHOWS:
                fetchPopular(callingId);
                break;
            case ON_THE_AIR_SHOWS:
                fetchOnTheAir(callingId);
                break;
        }
    }

    @Override
    public void onScrolledToBottom(ListTvShowsView view, UiView.MMoviesQueryType queryType) {
        MoviesState.SearchResult searchResult;
        ApplicationState.ShowPaginatedResult result;
        final int callingId = getId(view);
        switch (queryType) {
            case SEARCH_SHOWS:
                searchResult = MMoviesApp.get().getState().getSearchResult();
                if (searchResult != null && canFetchNextPage(searchResult.shows)) {
                    fetchTvShowsSearchResults(
                            getId(view),
                            searchResult.query,
                            searchResult.people.page + 1);
                }
                break;

            case POPULAR_SHOWS:
                result = MMoviesApp.get().getState().getPopularShows();
                if (canFetchNextPage(result)) {
                    fetchPopular(callingId, result.page + 1);
                }
                break;

            case ON_THE_AIR_SHOWS:
                result = MMoviesApp.get().getState().getOnTheAirShows();
                if (canFetchNextPage(result)) {
                    fetchOnTheAir(callingId, result.page + 1);
                }
                break;
        }
    }

    @Override
    public void populateUiFromEvent(BaseState.BaseEvent event, UiView.MMoviesQueryType queryType) {
        final ListTvShowsView ui = findUi(event.callingId);
        if (ui != null) {
            populateUi(ui, queryType);
        }
    }

    @Override
    public ListTvShowsView findUi(int id) {
        for (ListTvShowsView ui: getAttachedViews()) {
            if (getId(ui) == id) {
                return ui;
            }
        }
        return null;
    }

    @Override
    public int getId(ListTvShowsView view) {
        return view.hashCode();
    }

    @Override
    public <BR> void executeNetworkTask(BaseRunnable<BR> task) {
        MMoviesApp.get().inject(task);
        MMoviesApp.get().getBackgroundExecutor().execute(task);
    }

    /**
     * Fetch search task
     */
    private void fetchTvShowsSearchResults(final int callingId, String query) {
        MMoviesApp.get().getState().setSearchResult(callingId, new MoviesState.SearchResult(query));
        fetchTvShowsSearchResults(callingId, query, TMDB_FIRST_PAGE);
    }

    private void fetchTvShowsSearchResults(final int callingId, String query, int page) {
        executeNetworkTask(new FetchSearchShowRunnable(callingId, query, page));
    }

    /**
     * Fetch popular shows task
     */
    private void fetchPopular(final int callingId, final int page) {
        executeNetworkTask(new FetchPopularShowsRunnable(callingId, page));
    }

    private void fetchPopular(final int callingId) {
        MMoviesApp.get().getState().setPopularShows(callingId, null);
        fetchPopular(callingId, TMDB_FIRST_PAGE);
    }

    private void fetchPopularIfNeeded(final int callingId) {
        ApplicationState.ShowPaginatedResult popular = MMoviesApp.get().getState().getPopularShows();
        if (popular == null || MoviesCollections.isEmpty(popular.items)) {
            fetchPopular(callingId, TMDB_FIRST_PAGE);
        } else {
            final ListTvShowsView ui = findUi(callingId);
            if (ui != null) {
                populateUi(ui, UiView.MMoviesQueryType.POPULAR_SHOWS);
            }
        }
    }

    /**
     * Fetch on the air shows task
     */
    private void fetchOnTheAir(final int callingId, final int page) {
        executeNetworkTask(new FetchOnTheAirShowsRunnable(callingId, page));
    }

    private void fetchOnTheAir(final int callingId) {
        MMoviesApp.get().getState().setOnTheAirShows(callingId, null);
        fetchOnTheAir(callingId, TMDB_FIRST_PAGE);
    }

    private void fetchOnTheAirIfNeeded(final int callingId) {
        ApplicationState.ShowPaginatedResult onTheAir = MMoviesApp.get().getState().getOnTheAirShows();
        if (onTheAir == null || MoviesCollections.isEmpty(onTheAir.items)) {
            fetchOnTheAir(callingId, TMDB_FIRST_PAGE);
        } else {
            final ListTvShowsView ui = findUi(callingId);
            if (ui != null) {
                populateUi(ui, UiView.MMoviesQueryType.ON_THE_AIR_SHOWS);
            }
        }
    }
}
