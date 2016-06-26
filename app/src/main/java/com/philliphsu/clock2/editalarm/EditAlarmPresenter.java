package com.philliphsu.clock2.editalarm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.philliphsu.clock2.Alarm;
import com.philliphsu.clock2.R;
import com.philliphsu.clock2.SharedPreferencesHelper;
import com.philliphsu.clock2.model.Repository;

import java.util.Date;

import static com.philliphsu.clock2.DaysOfWeek.SATURDAY;
import static com.philliphsu.clock2.DaysOfWeek.SUNDAY;
import static com.philliphsu.clock2.util.Preconditions.checkNotNull;

/**
 * Created by Phillip Hsu on 6/3/2016.
 */
@Deprecated
public class EditAlarmPresenter implements EditAlarmContract.Presenter {
    private static final String TAG = "EditAlarmPresenter";

    @NonNull private final EditAlarmContract.View mView;
    @NonNull private final Repository<Alarm> mRepository;
    @NonNull private final AlarmUtilsHelper mAlarmUtilsHelper;
    @NonNull private final SharedPreferencesHelper mSharedPreferencesHelper;
    @Nullable private Alarm mAlarm;

    @Deprecated
    public EditAlarmPresenter(@NonNull EditAlarmContract.View view,
                              @NonNull Repository<Alarm> repository,
                              @NonNull AlarmUtilsHelper helper,
                              @NonNull SharedPreferencesHelper sharedPreferencesHelper) {
        mView = view;
        mRepository = repository;
        mAlarmUtilsHelper = helper;
        mSharedPreferencesHelper = sharedPreferencesHelper;
    }

    @Deprecated
    @Override
    public void loadAlarm(long alarmId) {
        // Can't load alarm in ctor because showDetails() calls
        // showTime(), which calls setTime() on the numpad, which
        // fires onNumberInput() events, which routes to the presenter,
        // which would not be initialized yet because we still haven't
        // returned from the ctor.
        mAlarm = alarmId > -1 ? mRepository.getItem(alarmId) : null;
        showDetails();
    }

    @Deprecated
    @Override
    public void save() {
        int hour;
        int minutes;
        try {
            hour = mView.getHour();
            minutes = mView.getMinutes();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        Alarm a = Alarm.builder()
                .hour(hour)
                .minutes(minutes)
                .ringtone(mView.getRingtone())
                .label(mView.getLabel())
                .vibrates(mView.vibrates())
                .build();
        a.setEnabled(mView.isEnabled());
        for (int i = SUNDAY; i <= SATURDAY; i++) {
            a.setRecurring(i, mView.isRecurringDay(i));
        }

        if (mAlarm != null) {
            if (mAlarm.isEnabled()) {
                Log.d(TAG, "Cancelling old alarm first");
                mAlarmUtilsHelper.cancelAlarm(mAlarm, false);
            }
            mRepository.updateItem(mAlarm, a);
        } else {
            mRepository.addItem(a);
        }

        if (a.isEnabled()) {
            mAlarmUtilsHelper.scheduleAlarm(a);
        }

        mView.showEditorClosed();
    }

    @Deprecated
    @Override
    public void delete() {
        if (mAlarm != null) {
            if (mAlarm.isEnabled()) {
                mAlarmUtilsHelper.cancelAlarm(mAlarm, false);
            }
            mRepository.deleteItem(mAlarm);
        }
        mView.showEditorClosed();
    }

    @Deprecated
    @Override
    public void dismissNow() {
        mAlarmUtilsHelper.cancelAlarm(checkNotNull(mAlarm), true);
        // cancelAlarm() should have turned off this alarm if appropriate
        mView.showEnabled(mAlarm.isEnabled());
    }

    @Deprecated
    @Override
    public void stopSnoozing() {
        dismissNow(); // MUST be first, see AlarmUtils.notifyUpcomingAlarmIntent()
        // AlarmUtils.cancelAlarm() does this for you if snoozed
        /*
        mAlarm.stopSnoozing(); // TOneverDO: before dismissNow()
        mRepository.saveItems();
        */
    }

    @Deprecated
    @Override
    public void showNumpad() {
        mView.showNumpad(true);
    }

    @Deprecated
    @Override
    public void hideNumpad() {
        mView.showNumpad(false);
    }

    @Deprecated
    @Override
    public void onBackspace(String newStr) {
        mView.showTimeTextPostBackspace(newStr);
    }

    @Deprecated
    @Override
    public void acceptNumpadChanges() {
        mView.showNumpad(false);
        mView.showEnabled(true);
    }

    @Deprecated
    @Override
    public void onPrepareOptionsMenu() {
        if (mAlarm != null && mAlarm.isEnabled()) {
            int hoursBeforeUpcoming = mSharedPreferencesHelper.getInt(R.string.key_notify_me_of_upcoming_alarms, 2);
            // TODO: Schedule task with handler to show the menu item when it is time. Handler is fine because
            // the task only needs to be done if the activity is being viewed. (I think) if the process of this
            // app is killed, then the handler is also killed.
            if ((mAlarm.ringsWithinHours(hoursBeforeUpcoming))) {
                mView.showCanDismissNow();
            } else if (mAlarm.isSnoozed()) {
                mView.showSnoozed(new Date(mAlarm.snoozingUntil()));
            }
        }
    }

    @Deprecated
    @Override
    public void openRingtonePickerDialog() {
        mView.showRingtonePickerDialog();
    }

    @Deprecated
    @Override
    public void setTimeTextHint() {
        mView.setTimeTextHint();
    }

    @Deprecated
    @Override
    public void onNumberInput(String formattedInput) {
        mView.showTimeText(formattedInput);
    }

    @Deprecated
    @Override
    public void focusTimeText() {
        mView.showTimeTextFocused(true);
    }

    @Deprecated
    private void showDetails() {
        if (mAlarm != null) {
            mView.showTime(mAlarm.hour(), mAlarm.minutes());
            mView.showEnabled(mAlarm.isEnabled());
            for (int i = SUNDAY; i <= SATURDAY; i++) {
                mView.showRecurringDays(i, mAlarm.isRecurring(i));
            }
            mView.showLabel(mAlarm.label());
            mView.showRingtone(mAlarm.ringtone());
            mView.showVibrates(mAlarm.vibrates());
            // Editing so don't show
            mView.showNumpad(false);
            mView.showTimeTextFocused(false);
        } else {
            // TODO default values
            mView.showTimeTextFocused(true);
            mView.showRingtone(""); // gets default ringtone
            mView.showNumpad(true);
        }
    }
}
