package com.roodie.materialmovies.mvp.presenters;


import android.util.Log;
import android.view.View;

import com.google.common.base.Preconditions;
import com.roodie.materialmovies.R;
import com.roodie.materialmovies.mvp.views.MovieView;
import com.roodie.materialmovies.mvp.views.UiView;
import com.roodie.materialmovies.qualifiers.GeneralPurpose;
import com.roodie.model.entities.BasicWrapper;
import com.roodie.model.entities.ListItem;
import com.roodie.model.entities.MovieWrapper;
import com.roodie.model.entities.PersonWrapper;
import com.roodie.model.entities.ShowWrapper;
import com.roodie.model.state.ApplicationState;
import com.roodie.model.state.MoviesState;
import com.roodie.model.tasks.BaseMovieRunnable;
import com.roodie.model.tasks.FetchSearchMovieRunnable;
import com.roodie.model.tasks.FetchSearchPeopleResult;
import com.roodie.model.tasks.FetchSearchShowRunnable;
import com.roodie.model.util.BackgroundExecutor;
import com.roodie.model.util.Injector;
import com.roodie.model.util.StringFetcher;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by Roodie on 20.08.2015.
 */
public class SearchPresenter extends BasePresenter {

    private SearchView mSearchView;


    protected static final int TMDB_FIRST_PAGE = 1;

    private static final String LOG_TAG = SearchPresenter.class.getSimpleName();

    private final BackgroundExecutor mExecutor;
    private final ApplicationState mState;
    private final Injector mInjector;
    private final StringFetcher mStringFetcher;

    private boolean attached = false;

    @Inject
    public SearchPresenter(@GeneralPurpose BackgroundExecutor mExecutor,
                           ApplicationState mState,
                           Injector mInjector,
                           StringFetcher stringFetcher) {
        this.mExecutor = Preconditions.checkNotNull(mExecutor, "mExecutor cannot be null");
        this.mState = Preconditions.checkNotNull(mState, "mState cannot be null");
        this.mInjector = Preconditions.checkNotNull(mInjector, "mInjector cannot be null");
        mStringFetcher = Preconditions.checkNotNull(stringFetcher, "stringFetcher cannot be null");
    }

    @Subscribe
    public void onSearchResultChanged(MoviesState.SearchResultChangedEvent event) {
        populateUisFromQueryTypes(UiView.MovieQueryType.SEARCH,
                UiView.MovieQueryType.SEARCH_MOVIES,
                UiView.MovieQueryType.SEARCH_SHOWS,
                UiView.MovieQueryType.SEARCH_PEOPLE);
    }

    @Override
    public void initialize() {
        if (mState.getSearchResult() != null) {
            search(mState.getSearchResult().query);
        }
    }

    public void attachView(SearchView view) {
        Preconditions.checkNotNull(view, "View cannot be null");
        this.mSearchView = view;
        attached = true;
    }

    public void detachView(SearchView view) {
        Preconditions.checkArgument(view != null, "view cannot be null");
       // Preconditions.checkState(mSearchView == view, "view is not attached");
        mSearchView = null;
        attached = false;
    }

    @Override
    public void onResume() {
        mState.registerForEvents(this);
    }

    @Override
    public void onPause() {
        mState.unregisterForEvents(this);
    }

    protected int getId(SearchView view) {
        return view.hashCode();
    }

    public void onScrolledToBottom() {

        ApplicationState.SearchResult searchResult;

        Log.d(LOG_TAG, "On scrolled to bottom");
        switch (mSearchView.getQueryType()) {
            case SEARCH_PEOPLE:
                searchResult = mState.getSearchResult();
                if (searchResult != null && canFetchNextPage(searchResult.people)) {
                    fetchPeopleSearchResults(
                            getId(mSearchView),
                            searchResult.query,
                            searchResult.people.page + 1);
                }
                break;
            case SEARCH_MOVIES:
                searchResult = mState.getSearchResult();
                if (searchResult != null && canFetchNextPage(searchResult.movies)) {
                    fetchMovieSearchResults(
                            getId(mSearchView),
                            searchResult.query,
                            searchResult.movies.page + 1);
                }
                break;
        }
    }

    private boolean canFetchNextPage(MoviesState.PaginatedResult<?> paginatedResult) {
        return paginatedResult != null && paginatedResult.page < paginatedResult.totalPages;
    }

    public void search(String query) {
        Log.d(LOG_TAG, "Performing search :" + query);
        mState.setSearchQuery(query);
        switch (mSearchView.getQueryType()) {
            case SEARCH: {
                Log.d(LOG_TAG, "Fetch common search results");
                fetchSearchResults(getId(mSearchView), query);
            }
                break;
            case SEARCH_MOVIES:
                fetchMovieSearchResults(getId(mSearchView), query);
                break;
            case SEARCH_PEOPLE:
                fetchPeopleSearchResults(getId(mSearchView), query);
                break;
            case SEARCH_SHOWS:
                fetchShowsSearchResults(getId(mSearchView), query);
                break;
        }
    }

    public void clearSearch() {
        mState.setSearchResult(null);
    }

    private void fetchSearchResults(final int callingId, String query) {
        mState.setSearchResult(new MoviesState.SearchResult(query));
        fetchMovieSearchResults(callingId, query, TMDB_FIRST_PAGE);
        fetchPeopleSearchResults(callingId, query, TMDB_FIRST_PAGE);
        fetchShowsSearchResults(callingId, query, TMDB_FIRST_PAGE);
    }

    private void fetchMovieSearchResults(final int callingId, String query) {
        mState.setSearchResult(new MoviesState.SearchResult(query));
        fetchMovieSearchResults(callingId, query, TMDB_FIRST_PAGE);
    }

    private void fetchPeopleSearchResults(final int callingId, String query) {
        mState.setSearchResult(new MoviesState.SearchResult(query));
        fetchPeopleSearchResults(callingId, query, TMDB_FIRST_PAGE);
    }

    private void fetchShowsSearchResults(final int callingId, String query) {
        mState.setSearchResult(new MoviesState.SearchResult(query));
        fetchShowsSearchResults(callingId, query, TMDB_FIRST_PAGE);
    }

    private void fetchMovieSearchResults(final int callingId, String query, int page) {
        executeTask(new FetchSearchMovieRunnable(callingId, query, page));
    }

    private void fetchPeopleSearchResults(final int callingId, String query, int page) {
        executeTask(new FetchSearchPeopleResult(callingId, query, page));
    }

    private void fetchShowsSearchResults(final int callingId, String query, int page) {
        executeTask(new FetchSearchShowRunnable(callingId, query, page));
    }

    public void populateUisFromQueryTypes(UiView.MovieQueryType... queryTypes) {
        Log.d(LOG_TAG, "Populate uis from queries");
        final List<UiView.MovieQueryType> list = Arrays.asList(queryTypes);

        for (UiView.MovieQueryType type : list) {
            if (mSearchView.getQueryType().equals(type)) {
                populateUiFromQueryType(type);
                break;
            }
        }
    }

    public void populateUiFromQueryType(UiView.MovieQueryType queryType) {

        if (mSearchView.getQueryType() == queryType) {
            switch (queryType) {
                case SEARCH: {
                    Log.d(LOG_TAG, "Populate search Ui");
                    populateSearchUi();
                }
                    break;
                case SEARCH_MOVIES:
                    populateSearchMovieUi();
                    break;
                case SEARCH_PEOPLE:
                    populateSearchPersonUi();
                    break;
                case SEARCH_SHOWS:
                    populateSearchShowUi();
                    break;
            }
        }


    }

    private void populateSearchUi() {
        mSearchView.setSearchResult(mState.getSearchResult());
    }

    private void populateSearchMovieUi() {
        MoviesState.SearchResult result = mState.getSearchResult();
        mSearchView.updateDisplayTitle(result != null ? result.query : null);

        // Now carry on with list ui population
        List<MovieWrapper> items = null;

        ApplicationState.MoviePaginatedResult popular = mState.getPopularMovies();
        if (popular != null) {
            items = popular.items;
        }

        if (items == null) {
            mSearchView.setItems(null);
        } else  {
            mSearchView.setItems(createListItemList(items));
        }
    }

    private void populateSearchPersonUi() {
        ApplicationState.SearchResult searchResult = mState.getSearchResult();
        mSearchView.updateDisplayTitle(searchResult != null ? searchResult.query : null);

        // Now carry on with list ui population
        if (searchResult != null && searchResult.people != null) {
            mSearchView.setItems(createListItemList(searchResult.people.items));
        }
    }

    private void populateSearchShowUi() {
        ApplicationState.SearchResult result = mState.getSearchResult();
        mSearchView.updateDisplayTitle(result != null ? result.query : null);

        if (result != null && result.shows != null) {
            mSearchView.setItems(createListItemList(result.shows.items));
        }

    }

    public String getUiTitle() {
        return mState.getSearchQuery();
    }

    public String getUiSubTitle() {
        switch (mSearchView.getQueryType()) {
            case SEARCH_MOVIES:
                return mStringFetcher.getString(R.string.movies_title);
            case SEARCH_SHOWS:
                return mStringFetcher.getString(R.string.shows_title);
            case SEARCH_PEOPLE:
                return mStringFetcher.getString(R.string.people_title);
        }
        return null;
    }

    private <T extends ListItem<T>> List<ListItem<T>> createListItemList(final List<T> items) {
        Preconditions.checkNotNull(items, "items cannot be null");
        ArrayList<ListItem<T>> listItems = new ArrayList<>(items.size());
        for (ListItem<T> item : items) {
            listItems.add(item);
        }
        return listItems;
    }


    private void checkViewAlreadySetted() {
        Preconditions.checkState(attached = true, "View not attached");
    }


    private <R> void executeTask(BaseMovieRunnable<R> task) {
        mInjector.inject(task);
        mExecutor.execute(task);
    }

    public interface SearchView<R extends BasicWrapper> extends MovieView {

        void setSearchResult(MoviesState.SearchResult result);

        void setItems(List<ListItem<R>> items);

        void showMovieDetail(MovieWrapper movie, View view);

        void showPersonDetail(PersonWrapper person, View view);

        void showTvShowDialog(ShowWrapper tvShow);

        String getTitle();

        String getSubtitle();


    }

}
