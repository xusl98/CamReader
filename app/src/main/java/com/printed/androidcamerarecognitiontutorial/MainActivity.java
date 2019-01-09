package com.printed.androidcamerarecognitiontutorial;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    SurfaceView cameraView;
    TextView textView;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;

    private ImageButton btn_play;
    private ImageButton btn_languages;
    private ImageButton btn_translate;

    private TextToSpeech mTTS;
    private Locale loc;

    private String tempString;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case RequestCameraPermissionID:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraView = (SurfaceView) findViewById(R.id.surface_view);
        textView = (TextView) findViewById(R.id.text_view);

        btn_play = (ImageButton) findViewById(R.id.btn_play);
        btn_languages = (ImageButton) findViewById(R.id.btn_languages);
        btn_translate = (ImageButton) findViewById(R.id.btn_translate);


        //CLICKLISTENER FOR TRANSLATION
        btn_translate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent translateIntent = new Intent(MainActivity.this, TranslateActivity.class);
                translateIntent.putExtra("text", tempString);
                startActivity(translateIntent);
            }
        });


        //CLICKLISTENER FOR LANGUAGES BUTTON
        btn_languages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loc = new Locale("en", "US");
//                mTTS.setLanguage(loc);
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, btn_languages);
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        //GETS THE ITEM U CLICKED AND CHANGED LANGUAGE
                        switch (item.getItemId()){
                            case R.id.english:
                                loc = new Locale("en", "US");
                                mTTS.setLanguage(loc);
                                break;
                            case R.id.spanish:
                                loc = new Locale("es", "ES");
                                mTTS.setLanguage(loc);
                                break;
                            case R.id.french:
                                loc = new Locale("fr", "FR");
                                mTTS.setLanguage(loc);
                        }

                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    loc = new Locale("es", "ES");
                    int result = mTTS.setLanguage(loc);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("TTS", "Language not supported");
                    } else {
                        btn_play.setEnabled(true);
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });



        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer).setFacing(CameraSource.CAMERA_FACING_BACK).setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f).setAutoFocusEnabled(true).build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0){
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < items.size(); ++i){
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                textView.setText(stringBuilder.toString());
                                tempString = stringBuilder.toString();
                            }
                        });
                    }

                }
            });
        }

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });

    }

    private void speak() {
        String text = tempString;

        mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);

    }

    @Override
    protected void onDestroy(){
        if (mTTS != null){
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }

}

