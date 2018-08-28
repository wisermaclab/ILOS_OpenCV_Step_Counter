package com.ilos.wiser.dottracker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class Info extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        TextView textView = findViewById(R.id.displayText);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("App Information");
        String finalOutput = "";
        finalOutput+="This application was created in the summer of 2018 by Mitchell Cooke as a research project under Dr.Rong Zheng of McMaster University's Department of Computing and software. ";
        finalOutput+="\n\nThis application is a visual step counter that allows location tagging of accelerometer, magentic, and grycoscope sensors. This allows better analysis of human walking cycle. Steps are detected based on the position of cyan and magenta dots on the user's feet.";
        finalOutput+= "\n\nFor more information contact Mitchell Cooke at cookem4@mcmaster.ca";
        textView.setText(finalOutput);

    }
}
