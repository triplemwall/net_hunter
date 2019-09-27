package com.offsec.nethunter.service;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.offsec.nethunter.AppNavHomeActivity;
import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.ChrootManagerFragment;
import com.offsec.nethunter.KaliServicesFragment;
import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.CheckForRoot;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class RunAtBootService extends IntentService {

    private static final String CHROOT_INSTALLED_TAG = "CHROOT_INSTALLED_TAG";
    private static final String TAG = "Nethunter: Startup";
    private static final int JOB_ID = 1;
    private final ShellExecuter x = new ShellExecuter();
    private String doing_action = "";
    boolean isAllFine = true;
    private NotificationCompat.Builder n = null;
    private HashMap<String, String> hashMap = new HashMap<>();
    private NhPaths nhPaths;

    public RunAtBootService(){
        super("RunAtBootService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create notification channel
        createNotificationChannel();
        nhPaths = NhPaths.getInstance(getApplicationContext());

    }

    private void doNotification(String contents) {
        if (n == null) {
            n = new NotificationCompat.Builder(getApplicationContext(), AppNavHomeActivity.BOOT_CHANNEL_ID);
        }
        n.setStyle(new NotificationCompat.BigTextStyle().bigText(contents))
                .setContentTitle(RunAtBootService.TAG)
                .setSmallIcon(R.drawable.ic_stat_ic_nh_notificaiton)
                .setAutoCancel(true);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(999, n.build());
        }
    }

    @Override
    protected void onHandleIntent(@NonNull Intent intent) {
        doNotification("Doing boot checks...");

        hashMap.put("ROOT", "No root access is granted.");
        hashMap.put("BUSYBOX", "No busybox is found.");
        hashMap.put("CHROOT", "Chroot is not yet installed.");

        if (CheckForRoot.isRoot()) {
            hashMap.put("ROOT", "OK.");
        }

        if (CheckForRoot.isBusyboxInstalled()) {
            hashMap.put("BUSYBOX", "OK.");
        }

        if (new File((NhPaths.CHROOT_PATH + NhPaths.CHROOT_EXEC)).canExecute()){
            new ShellExecuter().RunAsRootOutput("rm -rf " + NhPaths.CHROOT_PATH + "/tmp/.X1*"); // remove posible vnc locks (if the phone is rebooted with the vnc server running)
            hashMap.put("CHROOT", "OK.");
        }

        String resultMsg = "All should work fine now.";
        for(Map.Entry<String, String> entry: hashMap.entrySet()){
            if (!entry.getValue().equals("OK")){
                isAllFine = false;
                resultMsg = "Make sure the above requirements are met.";
                break;
            }
        }

        String result = "Root: " + hashMap.get("ROOT") + "\n" +
                        "Busybox: " + hashMap.get("BUSYBOX") + "\n" +
                        "Chroot: " + hashMap.get("CHROOT") + "\n" +
                        resultMsg;

        doNotification(result);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (nhPaths != null) {
            nhPaths.onDestroy();
        }
    }

    /*private boolean userinit(Boolean ShouldRun) {
            if (!ShouldRun) {
                return false;
            }
            doing_action = "RUNNING BOOT SERVICES";
            // doNotification(TAG, "RUNNING BOOT SERVICES");
            // this duplicates the functionality of the userinit service, formerly in init.rc
            // These scripts will start up after the system is booted.
            // Put scripts in fileDir/scripts/etc/init.d/ and set execute permission.  Scripts should
            // start with a number and include a hashbang such as #!/system/bin/sh as the first line.
            ShellExecuter exe = new ShellExecuter();
            if (!NhPaths.BUSYBOX.equals("")) {
                exe.RunAsRootOutput("rm -rf " + NhPaths.CHROOT_PATH + "/tmp/.X1*"); // remove posible vnc locks (if the phone is rebooted with the vnc server running)
                // init.d
                String[] runner = {NhPaths.BUSYBOX + " run-parts " + NhPaths.APP_INITD_PATH};
                exe.RunAsRoot(runner);
    //            Toast.makeText(getBaseContext(), getString(R.string.autorunningscripts), Toast.LENGTH_SHORT).show();
                return true;
            }
    //        Toast.makeText(getBaseContext(), getString(R.string.toastForNoBusybox), Toast.LENGTH_SHORT).show();
            doNotification(getString(R.string.toastForNoBusybox));
            return false;
        }*/

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    AppNavHomeActivity.BOOT_CHANNEL_ID,
                    "Boot Check Service",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }
}
