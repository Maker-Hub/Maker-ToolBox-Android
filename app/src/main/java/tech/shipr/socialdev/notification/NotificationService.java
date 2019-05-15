package tech.shipr.socialdev.notification;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class NotificationService extends Service {

    public static final String service_broadcast = "makertoolbox.intent.action.RestartService";
    public static boolean state = false;
    private static ArrayList<NotificationChannel> notificationChannels = new ArrayList<>();
    private DatabaseReference databaseReference;
    private int channels = 0;
    private Thread runner;

    public static int getIntId(String channelId) {
        for (int i = 0; i < notificationChannels.size(); i++) {
            NotificationChannel channel = notificationChannels.get(i);
            if (channel.getChannel_Id().equals(channelId)) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("chat");
        state = true;
        channels = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot channel : dataSnapshot.getChildren()) {
                    String channel_Id = channel.getKey();
                    NotificationChannel notificationChannel = new NotificationChannel(NotificationService.this, databaseReference, channel_Id, channels++);
                    notificationChannel.startListner();
                    notificationChannels.add(notificationChannel);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        runner = new Thread() {
            @Override
            public void run() {
                while (true) ;
            }
        };
        runner.start();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        runner = null;
        for (NotificationChannel channel : notificationChannels) {
            channel.stopListner();
        }
        notificationChannels.clear();
        channels = 0;
        super.onDestroy();
        if (state) {
            Intent intent = new Intent();
            intent.setAction(service_broadcast);
            getBaseContext().sendBroadcast(intent);
        }
    }
}