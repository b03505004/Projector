package com.example.ky.projector;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import com.transitionseverywhere.Fade;
import com.transitionseverywhere.Recolor;
import com.transitionseverywhere.TransitionManager;
import com.transitionseverywhere.TransitionSet;
import com.transitionseverywhere.extra.Scale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity{
    private static  String TAG = "MainActivity";
    Button startFocus;
    Animation welcome_fade_out, icon_fade;
    ConstraintLayout start_layout;
    ImageView icon;
    private static Socket socket;
    private static PrintWriter printWriter;
    private static String ip = "192.168.4.1";
    private static String startCode = "$S%\n";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        /**Start animation**/
        //final ViewGroup transitionsContainer = (ViewGroup) findViewById(R.id.transitions_container);
        start_layout = (ConstraintLayout) findViewById(R.id.start_animation_layout);
        icon = (ImageView) findViewById(R.id.autoFocusIcon);
        /*TransitionSet set = new TransitionSet()
                .addTransition(new Scale(0.7f))
                .addTransition(new Fade())
                .setInterpolator(new LinearOutSlowInInterpolator());
        TransitionManager.beginDelayedTransition(transitionsContainer, set);
        start_layout.setVisibility(View.VISIBLE);
        start_layout.setVisibility(View.INVISIBLE);*/

        welcome_fade_out = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.welcome_fade_out);
        icon_fade = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_right);

        start_layout = (ConstraintLayout) findViewById(R.id.start_animation_layout);
        icon = (ImageView) findViewById(R.id.autoFocusIcon);
        welcome_fade_out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                icon.startAnimation(icon_fade);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                start_layout.setVisibility(View.GONE);
                start_layout.setClickable(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        start_layout.startAnimation(welcome_fade_out);


        startFocus = (Button) findViewById(R.id.focusButton);
        startFocus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTask startTask = new startTask();
                startTask.execute();
                Intent i = new Intent(MainActivity.this, FocusingActivity.class);
                //Intent i = new Intent(MainActivity.this, DoneActivity.class);
                startActivity(i);
            }
        });
    }
    class startTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try{
                socket = new Socket(ip, 66);
                socket.setSoTimeout(10000);
                Log.i(TAG, "Socket connected: "+socket.isConnected());
                printWriter = new PrintWriter(socket.getOutputStream());
                printWriter.write(startCode);
                Log.i(TAG, "code sent: "+startCode);
                printWriter.flush();
                socket.close();
                printWriter.close();
            }catch (IOException ioe){
                //Log.i(TAG, "Socket connected: "+socket.isConnected());
                Log.e(TAG, "IOE: "+ioe);
            }
            return null;
        }
    }
}
