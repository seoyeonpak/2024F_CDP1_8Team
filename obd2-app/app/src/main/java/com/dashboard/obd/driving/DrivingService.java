/*
 * Copyright (c) 2021. Anthony Fryberg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dashboard.obd.driving;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.dashboard.obd.R;
import com.dashboard.obd.receiver.BringToFrontReceiver;
import org.apache.log4j.Logger;

import java.util.Objects;

public class DrivingService extends Service {

    static final String TAG = "DrivingService";
    final Logger logger = Logger.getLogger(TAG);

    public static final String ACTION_GOTO_EXIT
            = "com.dashboard.obd.action.GOTO_EXIT";

    public static final String ACTION_GOTO_HOME
            = "com.dashboard.obd.action.GOTO_HOME";

    public static final String ACTION_GOTO_PAUSE
            = "com.dashboard.obd.action.GOTO_PAUSE";

    public static final String ACTION_READY_TO_EXIT
            = "com.dashboard.obd.action.READY_TO_EXIT";

    public static final String ACTION_ERS_TARGET_CHANGED
            = "com.dashboard.obd.action.ERS_TARGET_CHANGED";

    public static final String ACTION_DIALOG_DISMISSED
            = "com.dashboard.obd.action.DIALOG_DISMISSED";
    public static final String EXTRA_DIALOG_TYPE
            = "com.dashboard.obd.extra.DIALOG_TYPE";

    public static final String ACTION_SHOW_DIALOG_DIAGNOSTIC = "com.dashboard.obd.action.SHOW_DIALOG_DIAGNOSTIC";

    public static final int DIALOG_EXIT = 1;
    public static final int DIALOG_DRIVING_OFF = 2;
    public static final int DIALOG_WAIT_FOR_EXIT = 3;
    public static final int DIALOG_ERS = 4;
    public static final int DIALOG_OBD_RECOVERY = 5;

    private final int mForegroundId = 1000;


    private LocalBroadcastManager mBroadcastManager;

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            registerBluetoothStateReceiver();
            mBroadcastManager = LocalBroadcastManager.getInstance(this);
        } catch (Exception e) {
            Log.e("DrivingService","onCreate");
        }
    }

    @Override
    public void onDestroy() {
        try {
            unregisterBluetoothStateReceiver();
            dismissExitDialog();
            dismissDrivingOffDialog();
            dismissWaitForExitDialog();
            dismissErsDialog();
            dismissObdRecoveryDialog();
            super.onDestroy();
        } catch (Exception e) {
            Log.e("DrivingService","onDestroy");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            return START_NOT_STICKY;
        } catch (Exception e) {
            Log.e("DrivingService","onStartCommand");
            return START_NOT_STICKY;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        try {
            return new DrivingServiceBinder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pauseDriving() {
        startForeground(mForegroundId, buildForegroundNotification(
                getString(R.string.noti_driving_content_off_driving)));
    }

    public void resumeDriving() {
        startForeground(mForegroundId, buildForegroundNotification(
                getString(R.string.noti_driving_content_on_driving)));
    }

    public void stopDriving() {
        stopForeground(true);
    }

    private Notification buildForegroundNotification(String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, String.valueOf(1))
                .setSmallIcon(R.drawable.icno_notice_n)
                .setContentTitle(getString(R.string.noti_driving_content_title))
                .setChannelId("1")
                .setContentText(contentText);

        Intent i = new Intent(BringToFrontReceiver.ACTION_BRING_TO_FRONT);

        @SuppressLint("LaunchActivityFromNotification")
        PendingIntent pi = PendingIntent.getBroadcast(this, mForegroundId, i, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(pi);

        return builder.build();
    }

    private void registerBluetoothStateReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBluetoothReceiver, filter);
        } catch (Exception e) {
            Log.e("DrivingService","registerBluetoothStateReceiver");
        }
    }

    private void unregisterBluetoothStateReceiver() {
        try {
            unregisterReceiver(mBluetoothReceiver);
        } catch (Exception e) {
            Log.e("DrivingService","unregisterBluetoothStateReceiver");
        }
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Logger.getLogger(TAG).info("ACTION_STATE_CHANGED#" + state + "@bluetooth@service");
                if (state == BluetoothAdapter.STATE_OFF) {
                    logger.info("TurnOn!!@bluetooth@service");
                    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((Activity) context,
                                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                        } else {
                            btAdapter.enable();
                        }
                    } else {
                        btAdapter.enable();
                    }
                }
            }
        }
    };



    public AlertDialog createAlertDialog() {
        try {
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_System).create();
            toSystemWindow(Objects.requireNonNull(dialog.getWindow()));
            return dialog;
        } catch (Exception e) {
            Log.e("DrivingService","createAlertDialog");
            return null;
        }
    }

    public ProgressDialog createProgressDialog() {
        try {
            ProgressDialog dialog = new ProgressDialog(this, R.style.AppTheme_Dialog_System);
            toSystemWindow(dialog.getWindow());
            return dialog;
        } catch (Exception e) {
            Log.e("DrivingService","createProgressDialog");
            return null;
        }
    }

    private void toSystemWindow(Window window) {
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        window.setAttributes(lp);
    }

    public class DrivingServiceBinder extends Binder {
        public DrivingService getService() {
            return DrivingService.this;
        }
    }

    private ExitDialog mExitDialog;

    public void showExitDialog() {
        if (mExitDialog == null) {
            mExitDialog = new ExitDialog(this);
            mExitDialog.show();
        }
    }

    public void dismissExitDialog() {
        if (mExitDialog != null) {
            mExitDialog.dismiss();
        }
    }

    public boolean isExitDialogShowing() {
        if (mExitDialog != null) {
            return mExitDialog.isShowing();
        } else {
            return false;
        }
    }

    public void onExitDialogExit() {
        Intent data = new Intent(ACTION_GOTO_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    public void onExitDialogHome() {
        Intent data = new Intent(ACTION_GOTO_HOME);
        mBroadcastManager.sendBroadcast(data);
    }

    private void onExitDialogDismissed() {
        mExitDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    private void onShowDiagnosticDialog() {
        Intent data = new Intent(ACTION_SHOW_DIALOG_DIAGNOSTIC);
        mBroadcastManager.sendBroadcast(data);
    }

    public class ExitDialog {

        private static final int TARGET_TIMEOUT = 3000; // 5 seconds
        private static final int COUNTDOWN_INTERVAL = 100;

        private final Context mContext;
        private AlertDialog mDialog;
        private CountDownTask mCountDownTask;

        public ExitDialog(Context context) {
            mContext = context;

            initDialog();
        }

        public void show() {
            mDialog.show();

            // Start count down
            mCountDownTask = new CountDownTask();
            mCountDownTask.execute();
        }

        public void dismiss() {
            mDialog.dismiss();
        }

        public boolean isShowing() {
            return mDialog.isShowing();
        }

        private void initDialog() {
            mDialog = createAlertDialog();
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(false);

            mDialog.setTitle(mContext.getString(R.string.dialog_title_driving_exit));
            mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_exit, TARGET_TIMEOUT / 1000));
            mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.btn_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            mDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.btn_exit),
                    (dialog, which) -> {
                        try {
                            onExitDialogExit();
                        } catch (Exception e) {
                            Log.e("DrivingService", "setButton");
                        }
                    });
            mDialog.setOnDismissListener(dialog -> {
                mCountDownTask.cancel(true);
                onExitDialogDismissed();
            });
        }

        @SuppressLint("StaticFieldLeak")
        private class CountDownTask extends AsyncTask<Void, Long, Void> {

            @Override
            protected Void doInBackground(Void... args) {
                long now = SystemClock.elapsedRealtime();
                long target = now + TARGET_TIMEOUT;
                int interval = COUNTDOWN_INTERVAL;

                while (now < target && !isCancelled()) {
                    now += interval;
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        break;
                    }
                    publishProgress(target - now);
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Long... values) {
                mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_exit,
                        1 + (values[0] / 1000)));
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!isCancelled()) {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                }
            }
        }
    }

    private DrivingOffDialog mDrivingOffDialog;

    public void showDrivingOffDialog() {
        if (mDrivingOffDialog == null) {
            mDrivingOffDialog = new DrivingOffDialog(this);
            mDrivingOffDialog.show();
        }
    }

    public void dismissDrivingOffDialog() {
        if (mDrivingOffDialog != null) {
            mDrivingOffDialog.dismiss();
        }
    }

    public boolean isDrivingOffDialogShowing() {
        if (mDrivingOffDialog != null) {
            return mDrivingOffDialog.isShowing();
        } else {
            return false;
        }
    }

    public void onDrivingOffDialogExit() {
        Intent data = new Intent(ACTION_GOTO_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    public void onDrivingOffDialogPause() {
        Intent data = new Intent(ACTION_GOTO_PAUSE);
        mBroadcastManager.sendBroadcast(data);
    }

    public void onDrivingOffDialogDismissed() {
        mDrivingOffDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_DRIVING_OFF);
        mBroadcastManager.sendBroadcast(data);
    }

    public class DrivingOffDialog {

        private static final int TARGET_TIMEOUT = 3000; // 5 seconds
        private static final int COUNTDOWN_INTERVAL = 100;

        private final Context mContext;
        private AlertDialog mDialog;
        private CountDownTask mCountDownTask;

        public DrivingOffDialog(Context context) {
            mContext = context;

            initDialog();
        }

        public void show() {
            mDialog.show();

            // Start count down
            mCountDownTask = new CountDownTask();
            mCountDownTask.execute();
        }

        public void dismiss() {
            mDialog.dismiss();
        }

        public boolean isShowing() {
            return mDialog.isShowing();
        }

        private void initDialog() {
            mDialog = createAlertDialog();
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(false);

            mDialog.setTitle(mContext.getString(R.string.dialog_title_driving_off));
            mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_off, TARGET_TIMEOUT / 1000));
            mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.btn_cancel),
                    (dialog, which) -> {
                    });
            mDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.btn_exit),
                    (dialog, which) -> onDrivingOffDialogExit());

            mDialog.setOnDismissListener(dialog -> {
                mCountDownTask.cancel(true);
                onDrivingOffDialogDismissed();
            });
        }

        private class CountDownTask extends AsyncTask<Void, Long, Void> {

            @Override
            protected Void doInBackground(Void... args) {
                try {
                    long now = SystemClock.elapsedRealtime();
                    long target = now + TARGET_TIMEOUT;
                    int interval = COUNTDOWN_INTERVAL;

                    while (now < target && !isCancelled()) {
                        now += interval;
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                            break;
                        }
                        publishProgress(target - now);
                    }
                } catch (Exception e) {
                    Log.e("DrivingService","CountDownTask");
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Long... values) {
                try {
                    mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_off,
                            1 + (values[0] / 1000)));
                } catch (Exception e) {
                    Log.e("DrivingService","onProgressUpdate");
                }
            }

            @Override
            protected void onPostExecute(Void result) {
                try {
                    if (!isCancelled()) {
                        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    }
                } catch (Exception e) {
                    Log.e("DrivingService","onPostExecute");
                }
            }

        }

    }

    private WaitForExitDialog mWaitForExitDialog;

    public void showWaitForExitDialog() {
        if (mWaitForExitDialog == null) {
            mWaitForExitDialog = new WaitForExitDialog(this);
            mWaitForExitDialog.show();
        }
    }

    public void dismissWaitForExitDialog() {
        if (mWaitForExitDialog != null) {
            mWaitForExitDialog.dismiss();
        }
    }

    public boolean isWaitForExitDialogShowing() {
        return (mWaitForExitDialog != null) && mWaitForExitDialog.isShowing();
    }

    public void setReadyToExit(boolean isReady) {
        if (mWaitForExitDialog != null) {
            mWaitForExitDialog.setReadyToExit(isReady);
        }
    }

    public void onWaitForDialogReadyToExit() {
        Intent data = new Intent(ACTION_READY_TO_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    public void onWaitForExitDialogDismissed() {
        mWaitForExitDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_WAIT_FOR_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    public class WaitForExitDialog {

        private final Context mContext;
        private ProgressDialog mDialog;
        private CountDownTask mCountDownTask;
        private boolean mReadyToExit = true;

        public WaitForExitDialog(Context context) {
            mContext = context;

            initDialog();
        }

        public void show() {
            mDialog.show();

            // Start count down
            mCountDownTask = new CountDownTask();
            mCountDownTask.execute();
        }

        public void dismiss() {
            mDialog.dismiss();
        }

        public boolean isShowing() {
            return mDialog.isShowing();
        }

        public void setReadyToExit(boolean isReady) {
            mReadyToExit = isReady;
        }

        private void initDialog() {
            mDialog = createProgressDialog();

            mDialog.setMessage(mContext.getString(R.string.dialog_message_wait_for));
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(false);

            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    mCountDownTask.cancel(true);
                    onWaitForExitDialogDismissed();
                }
            });
        }

        @SuppressLint("StaticFieldLeak")
        private class CountDownTask extends AsyncTask<Void, Long, Void> {

            @Override
            protected Void doInBackground(Void... args) {
                int sleepCount = 0;
                while (!mReadyToExit) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    if (++sleepCount >= 50) {
                        break;
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!isCancelled()) {
                    mCountDownTask.cancel(true);
                    mDialog.dismiss();

                    onWaitForDialogReadyToExit();
                }
            }

        }

    }

    private ErsDialog mErsDialog;

    public void showErsDialog(boolean isBlackboxRunning) {
        if (mErsDialog == null) {
            mErsDialog = new ErsDialog(this, isBlackboxRunning);
            mErsDialog.show();
        }
    }

    public void dismissErsDialog() {
        if (mErsDialog != null) {
            mErsDialog.dismiss();
        }
    }

    public boolean isErsDialogShowing() {
        return (mErsDialog != null) && mErsDialog.isShowing();
    }



    private void onErsDialogDismissed() {
        mErsDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_ERS);
        mBroadcastManager.sendBroadcast(data);
    }

    public class ErsDialog implements OnClickListener {

        private static final int TARGET_TIMEOUT = 10000; // 10 seconds
        private static final int COUNTDOWN_INTERVAL = 100;

        private Button mCallBtn;
        private Button mSmsBtn;
        private Button mYoutubeBtn;
        private View mCancelBtn;
        private TextView mMessageText;

        private final Context mContext;
        private final boolean mIsBlackboxRunning;

        private AlertDialog mDialog;
        private View mContentView;
        private CountDownTask mCountDownTask;


        public ErsDialog(Context context, boolean isBlackboxRunning) {
            mContext = context;




            mIsBlackboxRunning = isBlackboxRunning;

            // Blackbox is not running but default ERS target is YOUTUBE


            initDialog();
        }

        public void show() {
            mDialog.show();

            // setContentView() method should be invoked after showing
            mDialog.setContentView(mContentView);

            // Start count down
            mCountDownTask = new CountDownTask();
            mCountDownTask.execute();

            // Callback default target
           //onErsTargetChanged(mTarget, false);
        }

        public void dismiss() {
            mDialog.dismiss();
        }

        public boolean isShowing() {
            return mDialog.isShowing();
        }

        private void initDialog() {
            mDialog = new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog_Transparent)
                    .create();
            toSystemWindow(mDialog.getWindow());

            initContentView();

            mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    mCountDownTask.cancel(true);
                }
            });
            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    // Cancel timer
                    mCountDownTask.cancel(true);

                    onErsDialogDismissed();
                }
            });
        }

        private void initContentView() {

        }

        @Override
        public void onClick(View v) {
            int id = v.getId();

            // Cancel timer first
            mCountDownTask.cancel(true);


            mDialog.dismiss();
        }


        private class CountDownTask extends AsyncTask<Void, Long, Void> {

            @Override
            protected Void doInBackground(Void... args) {
                long now = SystemClock.elapsedRealtime();
                long target = now + TARGET_TIMEOUT;
                int interval = COUNTDOWN_INTERVAL;

                while (now < target && !isCancelled()) {
                    now += interval;
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        break;
                    }
                    publishProgress(target - now);
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Long... values) {
                mMessageText.setText(mContext.getString(R.string.dialog_mesage_ers,
                        1 + (values[0] / 1000)));
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!isCancelled()) {


                    mDialog.dismiss();
                }
            }

        }

    }

    private ObdRecoveryDialog mObdRecoveryDialog;

    public void showObdRecoveryDialog() {
        if (mObdRecoveryDialog == null) {
            mObdRecoveryDialog = new ObdRecoveryDialog(this);
            mObdRecoveryDialog.show();
        }
    }

    public void dismissObdRecoveryDialog() {
        if (mObdRecoveryDialog != null) {
            mObdRecoveryDialog.dismiss();
        }
    }

    public void onObdRecoveryDialogDismissed() {
        mObdRecoveryDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_OBD_RECOVERY);
        mBroadcastManager.sendBroadcast(data);
    }

    public class ObdRecoveryDialog {

        private final Context mContext;
        private AlertDialog mDialog;

        public ObdRecoveryDialog(Context context) {
            mContext = context;

            initDialog();
        }

        public void show() {
            mDialog.show();
        }

        public void dismiss() {
            mDialog.dismiss();
        }

        public boolean isShowing() {
            return mDialog.isShowing();
        }

        private void initDialog() {
            mDialog = createAlertDialog();

            mDialog.setMessage(mContext.getString(R.string.dialog_message_obd_recovery));
            mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.btn_cancel),
                    (dialog, which) -> {
                        // Do nothing, just dismiss the dialog
                    });
            mDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.btn_yes),
                    (dialog, which) -> {

                        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions((Activity) mContext,
                                        new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                            } else {
                                btAdapter.disable();
                            }
                        } else {
                            btAdapter.disable();
                        }
                    });
            mDialog.setOnDismissListener(dialog -> onObdRecoveryDialogDismissed());
        }
    }
}
