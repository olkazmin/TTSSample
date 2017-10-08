package org.kazminov.myttssample.org.kazminov.tts;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.MainThread;
import android.util.Log;
import android.widget.Toast;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

/**
 * Created by olkazmin on 24.09.17.
 */

public class TTSPlayer {

    public static final String LOG_TAG = "TTSPlayer";

    private final Context mContext;
    private Handler mHandler = new Handler();

    private TextToSpeech mTTS;

    private final int STATE_DEFAULT = 0;
    private final int STATE_INITIALIZING = 1;
    private final int STATE_READY = 2;
    private final int STATE_PLAYING = 3;

    private final int STATE_ERROR = 4;

    private int mState = STATE_DEFAULT;

    private Deque<String> mQueue = new LinkedList<>();

    private int mStreamType = AudioManager.STREAM_SYSTEM;
    private float mVolume = -1f;

    private TSSPlayerListener myListener;

    public interface TSSPlayerListener {
        void onUtterancePlaybackFinished(String utterance);
        void onError();
        void onInitializing();
        void onUtterancePlaybackStarted(String utterance);
    }

    public TTSPlayer(Context context) {
        mContext = context;
    }

    @MainThread
    public void play(String text) {
        log("play: state=%d, %s", mState, text);

        if (mState == STATE_ERROR) {
            onError();
            return;
        }

        if (mState == STATE_DEFAULT) {
            mQueue.push(text);
            init();
            return;
        }

        if (mState == STATE_INITIALIZING
                || mState == STATE_PLAYING) {
            mQueue.push(text);
            return;
        }

        mQueue.push(text);
        playNext();
    }

    private void onError() {
        if (myListener != null) {
            myListener.onError();
        }
    }

    public void setStreamType(int streamType) {
        this.mStreamType = streamType;
    }

    public void setVolume(float volume) {
        this.mVolume = volume;
    }

    private void init() {
        log("init");
        setState(STATE_INITIALIZING);
        mTTS = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                initLanguage();
            }
        });
    }

    private void initLanguage() {
        Locale locale =
//                    Locale.ENGLISH
                Locale.getDefault()
                ;

        int result = mTTS.setLanguage(locale);
        //int result = mTTS.setLanguage(Locale.getDefault());

        if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED) {

            Toast.makeText(mContext, "TTS is not supported for your language", Toast.LENGTH_SHORT).show();
            mTTS.shutdown();
            mTTS = null;
            setState(STATE_ERROR);
            return;
        }

        onInitFinished();
    }

    private void onInitFinished() {
        log("onInitFinished");

        setState(STATE_READY);

        playNext();
    }

    private void playNext() {
        log("playNext: queue size=%d", mQueue.size());
        if (mQueue.isEmpty()) {
            setState(STATE_READY);
            return;
        }

        setState(STATE_PLAYING);
        String utterance = mQueue.poll();
        log("playNext: %s", utterance);

        /**TextToSpeech.Engine.
         *
         * KEY_PARAM_STREAM
         * KEY_PARAM_UTTERANCE_ID
         * KEY_PARAM_VOLUME
         *
         *
         * AudioManager
         *
         * STREAM_NOTIFICATION
         * STREAM_MUSIC
         */

        mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(final String s) {
                log("onStart: %s", s);
                onPlaybackStarted(s);
            }

            @Override
            public void onDone(final String s) {
                log("onDone: %s", s);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onPlaybackFinished(s);

                        playNext();
                    }
                });
            }

            @Override
            public void onError(String s) {
                log("onError: %s", s);
                TTSPlayer.this.onError();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        playNext();
                    }
                });
            }
        });

        HashMap<String, String> params = new HashMap();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utterance);
        if (mVolume != -1f) {
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, Float.toString(mVolume));
        }

        if (mStreamType != -1) {
            params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, Integer.toString(mStreamType));
        }

        onInitializing();
        mTTS.speak(utterance, TextToSpeech.QUEUE_FLUSH, params);
    }

    private void onPlaybackStarted(String utterance) {
        if (myListener != null) {
            myListener.onUtterancePlaybackStarted(utterance);
        }
    }

    private void onPlaybackFinished(String utterance) {
        if (myListener != null) {
            myListener.onUtterancePlaybackFinished(utterance);
        }
    }

    public void destroy() {
        log("destroy");
        if (mTTS == null) {
            return;
        }

        mTTS.stop();
        mTTS.shutdown();
        mTTS = null;
        setState(STATE_DEFAULT);
    }

    private void setState(int state) {
        log("setState: current=%d, new=%d", mState, state);
        if (state == mState) {
            return;
        }

        mState = state;
        switch (mState) {
            case STATE_INITIALIZING:
                onInitializing();
                break;
            case STATE_ERROR:
                onError();
                break;

        }
    }

    private void onInitializing() {
        if (myListener != null) {
            myListener.onInitializing();
        }
    }

    private void log(String message, Object... args) {
        Log.v(LOG_TAG, String.format(message, args));
    }

    public void setMyListener(TSSPlayerListener myListener) {
        this.myListener = myListener;
    }
}
