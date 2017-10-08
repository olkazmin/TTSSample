package org.kazminov.myttssample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.kazminov.myttssample.org.kazminov.tts.TTSPlayer;

public class MainActivity extends AppCompatActivity {

    private TTSPlayer mTTSPlayer;
    private EditText mUtterance;
    private TextView mStatus;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUtterance = findViewById(R.id.et_utterance);
        mStatus = findViewById(R.id.tv_status);

        mTTSPlayer = new TTSPlayer(this);
        mTTSPlayer.setMyListener(new TTSPlayer.TSSPlayerListener() {
            @Override
            public void onUtterancePlaybackFinished(String utterance) {
                mButton.setEnabled(true);
                mStatus.setText("speak finished");
            }

            @Override
            public void onError() {
                mStatus.setText("error!");
                mButton.setEnabled(true);
            }

            @Override
            public void onInitializing() {
                mStatus.setText("initializing ...");
                mButton.setEnabled(false);
            }

            @Override
            public void onUtterancePlaybackStarted(String utterance) {
                mStatus.setText("speaking ...");
                mButton.setEnabled(false);
            }
        });
        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speek();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mTTSPlayer.destroy();
        super.onDestroy();
    }

    private void speek() {
        String utterance = mUtterance.getText() == null ? "" : mUtterance.getText().toString();
        if (TextUtils.isEmpty(utterance)) {
            return;
        }

        InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mUtterance.getWindowToken(), 0);

        utterance = utterance.trim();
        mButton.setEnabled(false);

        mTTSPlayer.play(
//                "Hello oleg"
                utterance
        );
    }
}
