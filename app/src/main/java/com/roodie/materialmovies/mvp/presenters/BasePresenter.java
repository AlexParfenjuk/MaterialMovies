package com.roodie.materialmovies.mvp.presenters;

import com.roodie.model.Display;

/**
 * Created by Roodie on 25.06.2015.
 */

/**
 * Abstract presenter to work as base for every presenter created in the application. This
 * presenter
 * declares some methods to attach the fragment/activity lifecycle.
 */
abstract class BasePresenter {


    private Display mDisplay;

    /**
     * Called when the presenter is initialized, this method represents the start of the presenter
     * lifecycle.
     */

    public abstract void initialize();

    /**
     * Called when the presenter is resumed. After the initialization and when the presenter comes
     * from a pause state.
     */
    public abstract void onResume();

    /**
     * Called when the presenter is paused.
     */
    public abstract void onPause();

    public Display getDisplay() {
        return mDisplay;
    }

    public void setDisplay(Display mDisplay) {
        this.mDisplay = mDisplay;
    }
}

