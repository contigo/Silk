package com.afollestad.silk.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import com.afollestad.silk.R;
import com.afollestad.silk.TimeUtils;

import java.util.Calendar;

/**
 * A {@link com.afollestad.silk.fragments.SilkCachedFeedFragment} that allows you to show a frame at the top of the list,
 * indicating the last time the fragment refreshed, and allowing the user to invoke a new refresh by pressing a button.
 *
 * @author Aidan Follestad (afollestad)
 */
public abstract class SilkLastUpdatedFragment<T> extends SilkCachedFeedFragment<T> {

    private TextView mLastUpdateLabel;
    private ImageButton mLastUpdateAction;


    /**
     * Sets whether or not the last updated frame is visible.
     */
    public final void setLastUpdatedVisibile(boolean visible) {
        View v = getView();
        if (v == null) return;
        v.findViewById(R.id.lastUpdatedFrame).setVisibility(visible ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.divider).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Gets the last time the fragment was updated (did a full refresh from the web).
     */
    public final Calendar getLastUpdatedTime() {
        SharedPreferences prefs = getActivity().getSharedPreferences("feed_last_update", 0);
        if (prefs.contains(getCacheTitle())) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(prefs.getLong(getCacheTitle(), 0));
            return cal;
        }
        return null;
    }

    /**
     * Gets whether or not right now is a good time to show the last updated frame. Can be overridden to change behavior.
     */
    public boolean getShouldShowLastUpdated() {
        Calendar last = getLastUpdatedTime();
        if (last == null) return false;
        else if (getAdapter().getCount() == 0) return true;
        Calendar now = Calendar.getInstance();
        double lastHours = last.getTimeInMillis() / (1000 * 60 * 60);
        double nowHours = now.getTimeInMillis() / (1000 * 60 * 60);
        // An hour or more difference in last refresh time will return true
        return (nowHours - lastHours) >= 1;
    }

    /**
     * Invalidates the last updated frame. Decides whether or not the last updated frame should be visible based on
     * the return value of {#getShouldShowLastUpdated}, and updates the label if it's shown.
     */
    public final boolean invalidateLastUpdated() {
        boolean shouldShow = getShouldShowLastUpdated();
        setLastUpdatedVisibile(shouldShow);
        if (shouldShow) {
            mLastUpdateLabel.setText(getString(R.string.last_updated).replace("{date}",
                    TimeUtils.getFriendlyTimeLong(getLastUpdatedTime())));
        }
        return shouldShow;
    }


    @Override
    public int getLayout() {
        return R.layout.fragment_list_lastupdated;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mLastUpdateLabel = (TextView) view.findViewById(R.id.ptrLastUpdateLabel);
        mLastUpdateAction = (ImageButton) view.findViewById(R.id.ptrLastUpdateAction);
        super.onViewCreated(view, savedInstanceState);
        invalidateLastUpdated();
        mLastUpdateAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUserRefresh();
            }
        });
    }

    @Override
    public void performRefresh(boolean progress) {
        mLastUpdateAction.setEnabled(false);
        super.performRefresh(progress);
    }

    /**
     * Sets the last update time for the fragment to right now and updates the label.
     */
    public final void setLastUpdatedTime() {
        Calendar now = Calendar.getInstance();
        SharedPreferences prefs = getActivity().getSharedPreferences("feed_last_update", 0);
        prefs.edit().putLong(getCacheTitle(), now.getTimeInMillis()).commit();
        invalidateLastUpdated();
    }

    @Override
    public void setLoadComplete() {
        super.setLoadComplete();
        mLastUpdateAction.setEnabled(true);
        setLastUpdatedTime();
        invalidateLastUpdated();
    }

    @Override
    public void setLoadFromCacheComplete() {
        // Prevent the setLoadComplete() code from this class from being called after a cache load
        super.setLoadComplete();
        invalidateLastUpdated();
    }

    @Override
    public void onCacheEmpty() {
        // Overriding the default behavior of refreshing immediately to show the last updated label
        if (!invalidateLastUpdated()) super.onCacheEmpty();
    }

    /**
     * Called when the user presses the button in the last updated frame; invokes performRefresh() by default.
     */
    public void onUserRefresh() {
        performRefresh(true);
    }
}
