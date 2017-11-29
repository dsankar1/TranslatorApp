package com.sankar.translatorapp;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GestureDetectorCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

public class Chat extends Fragment {

    // chat modes
    private static final int ME = 0;
    private static final int YOU = 1;
    private volatile int mode;

    // chat box view modes
    private static final int MIC_VIEW = 0;
    private static final int SEND_MESSAGE_VIEW = 1;
    private static final int RECEIVE_MESSAGE_VIEW = 2;
    private static final int LOADING_VIEW = 3;
    private volatile int viewMode;

    // UI components
    private LinearLayout chatContainer;
    private FrameLayout chatBoxContainer;
    private FrameLayout chatBoxMessage;
    private FrameLayout chatBoxAudio;
    private EditText chatBoxText;
    private ImageView chatBoxRemoveBtn;
    private FloatingActionButton chatBoxMicBtn;
    private ImageView chatBoxCheckBtn;
    private ProgressBar translationProgress;

    // Information about UI
    private int orientation;
    private int rotation;
    private int screenWidth;
    private int screenHeight;
    private int actionBarHeight;

    private GestureDetectorCompat gestureDetector;
    private OnFragmentInteractionListener mListener;

    public Chat() {}

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        gestureDetector = new GestureDetectorCompat(getActivity(), new GestureListener());
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // UI Components
        chatContainer = (LinearLayout) getView();
        chatBoxContainer = getView().findViewById(R.id.chat_box_container);
        chatBoxMessage = getView().findViewById(R.id.chat_box_message);
        chatBoxAudio = getView().findViewById(R.id.chat_box_audio);
        chatBoxText = getView().findViewById(R.id.chat_box_text);
        chatBoxMicBtn = getView().findViewById(R.id.chat_box_mic_btn);
        chatBoxRemoveBtn = getView().findViewById(R.id.chat_box_remove_btn);
        chatBoxCheckBtn = getView().findViewById(R.id.chat_box_check_btn);
        translationProgress = getView().findViewById(R.id.translation_progress);

        // Helpful Info
        orientation = getActivity().getResources().getConfiguration().orientation;
        rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        actionBarHeight = getActionBarHeight();

        autoSetChatBoxSize(orientation);
        setMode(ME);
        micView();
        setButtonClickListeners();
        setChatBoxEventListeners();
        setChatFragmentEventListeners();
        chatBoxText.setShowSoftInputOnFocus(false);
    }

    // Below methods change the chat box view mode(send message, receive message, mic, loading)
    private void loadingView() {
        chatBoxAudio.setVisibility(View.GONE);
        chatBoxMessage.setVisibility(View.GONE);
        showProgress();
        viewMode = LOADING_VIEW;
    }

    private void receiveMessageView() {
        chatBoxAudio.setVisibility(View.GONE);
        chatBoxMessage.setVisibility(View.VISIBLE);
        chatBoxCheckBtn.setVisibility(View.VISIBLE);
        chatBoxRemoveBtn.setVisibility(View.GONE);
        chatBoxText.setEnabled(false);
        hideProgress();
        viewMode = RECEIVE_MESSAGE_VIEW;
    }

    private void sendMessageView() {
        chatBoxAudio.setVisibility(View.GONE);
        chatBoxMessage.setVisibility(View.VISIBLE);
        chatBoxCheckBtn.setVisibility(View.GONE);
        chatBoxRemoveBtn.setVisibility(View.VISIBLE);
        chatBoxText.setEnabled(true);
        hideProgress();
        viewMode = SEND_MESSAGE_VIEW;
    }

    private void micView() {
        chatBoxAudio.setVisibility(View.VISIBLE);
        chatBoxMessage.setVisibility(View.GONE);
        hideProgress();
        viewMode = MIC_VIEW;
    }

    // Below methods set event listeners for all UI components
    private void setButtonClickListeners() {
        chatBoxMicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatBoxAudio.startAnimation(AnimationUtils
                        .loadAnimation(getActivity(), R.anim.fade_out));
                String defaultMessage = mode == ME ? "Hello World" : "Hola Mundo";
                chatBoxText.setText(defaultMessage);
                sendMessageView();
            }
        });

        chatBoxRemoveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatBoxAudio.startAnimation(AnimationUtils
                        .loadAnimation(getActivity(), R.anim.fade_in));
                micView();
            }
        });

        chatBoxCheckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chatBoxCheckBtn.startAnimation(AnimationUtils
                        .loadAnimation(getActivity(), R.anim.fade_out));
                chatBoxRemoveBtn.startAnimation(AnimationUtils
                        .loadAnimation(getActivity(), R.anim.fade_in));
                sendMessageView();
            }
        });
    }

    private void setChatBoxEventListeners() {
        chatBoxContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (viewMode == LOADING_VIEW) return true;
                gestureDetector.onTouchEvent(motionEvent);
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    Log.i("Motion Up", "Motion Up");
                    LinearLayout.LayoutParams params =
                            (LinearLayout.LayoutParams) chatBoxContainer.getLayoutParams();
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        params.setMargins(0, 16, 0, 16);
                    }
                    else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        params.setMargins(16, 0, 16, 0);
                    }
                    chatBoxContainer.setLayoutParams(params);
                }
                return true;
            }
        });
    }

    private void setChatFragmentEventListeners() {
        chatContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (viewMode == LOADING_VIEW) return true;
                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();

                switch (rotation) {
                    case Surface.ROTATION_90:
                        if (x > (screenWidth / 2)) {
                            if (mode == ME) return false;
                            makeInvisible(chatBoxContainer);
                            setMode(ME);
                            micView();
                            makeVisible(chatBoxContainer);
                        } else {
                            if (mode == YOU) return false;
                            makeInvisible(chatBoxContainer);
                            setMode(YOU);
                            micView();
                            makeVisible(chatBoxContainer);
                        }
                        break;
                    case Surface.ROTATION_270:
                        if (x < (screenWidth / 2)) {
                            if (mode == ME) return false;
                            makeInvisible(chatBoxContainer);
                            setMode(ME);
                            micView();
                            makeVisible(chatBoxContainer);
                        } else {
                            if (mode == YOU) return false;
                            makeInvisible(chatBoxContainer);
                            setMode(YOU);
                            micView();
                            makeVisible(chatBoxContainer);
                        }
                        break;
                    default:
                        if (y > ((screenHeight - actionBarHeight) / 2)) {
                            if (mode == ME) return false;
                            makeInvisible(chatBoxContainer);
                            setMode(ME);
                            micView();
                            makeVisible(chatBoxContainer);
                        } else {
                            if (mode == YOU) return false;
                            makeInvisible(chatBoxContainer);
                            setMode(YOU);
                            micView();
                            makeVisible(chatBoxContainer);
                        }
                        break;
                }
                return true;
            }
        });
    }

    // Below are methods to make development easier
    private void setMode(int mode) {
        switch (rotation) {
            case Surface.ROTATION_90:
                if (mode == ME) {
                    chatContainer.setGravity(Gravity.RIGHT);
                    chatBoxContainer.setRotation(0);
                    this.mode = mode;
                }
                else if (mode == YOU) {
                    chatContainer.setGravity(Gravity.LEFT);
                    chatBoxContainer.setRotation(0);
                    this.mode = mode;
                }
                break;
            case Surface.ROTATION_270:
                if (mode == ME) {
                    chatContainer.setGravity(Gravity.LEFT);
                    chatBoxContainer.setRotation(0);
                    this.mode = mode;
                }
                else if (mode == YOU) {
                    chatContainer.setGravity(Gravity.RIGHT);
                    chatBoxContainer.setRotation(0);
                    this.mode = mode;
                }
                break;
            default:
                if (mode == ME) {
                    chatContainer.setGravity(Gravity.BOTTOM);
                    chatBoxContainer.setRotation(0);
                    this.mode = mode;
                }
                else if (mode == YOU) {
                    chatContainer.setGravity(Gravity.TOP);
                    chatBoxContainer.setRotation(180);
                    this.mode = mode;
                }
                break;
        }
    }

    private int getActionBarHeight() {
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize
                    (tv.data, getResources().getDisplayMetrics());
        }
        return 0;
    }

    private void autoSetChatBoxSize(int orientation) {
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) chatBoxContainer.getLayoutParams();
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                params.width = screenWidth * 4 / 9;
                params.leftMargin = 16;
                params.rightMargin = 16;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                params.height = (screenHeight - actionBarHeight) * 4 / 9;
                params.topMargin = 16;
                params.bottomMargin = 16;
                break;
            default:
                break;
        }
        chatBoxContainer.setLayoutParams(params);
    }

    // Below are methods related to animating events such as scrolling and fading
    private void makeVisible(FrameLayout layout) {
        layout.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
        layout.setVisibility(View.VISIBLE);
    }

    private void makeInvisible(FrameLayout layout) {
        layout.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out));
        layout.setVisibility(View.GONE);
    }

    private void slideChatBoxUp() {
        chatBoxContainer.startAnimation(AnimationUtils
                .loadAnimation(getActivity(), R.anim.slide_up));
        chatContainer.setGravity(Gravity.TOP);
        chatBoxContainer.setRotation(180);
        //if (viewMode == SEND_MESSAGE_VIEW) receiveMessageView();
    }

    private void slideChatBoxDown() {
        chatBoxContainer.startAnimation(AnimationUtils
                .loadAnimation(getActivity(), R.anim.slide_down));
        chatContainer.setGravity(Gravity.BOTTOM);
        chatBoxContainer.setRotation(0);
    }

    private void slideChatBoxLeft() {
        chatBoxContainer.startAnimation(AnimationUtils
                .loadAnimation(getActivity(), R.anim.slide_left));
        chatContainer.setGravity(Gravity.LEFT);
    }

    private void slideChatBoxRight() {
        chatBoxContainer.startAnimation(AnimationUtils
                .loadAnimation(getActivity(), R.anim.slide_right));
        chatContainer.setGravity(Gravity.RIGHT);
    }

    private void showProgress() {
        translationProgress.startAnimation
                (AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
        translationProgress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        translationProgress.startAnimation
                (AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out));
        translationProgress.setVisibility(View.GONE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int y = (int)(e1.getRawY() - e2.getRawY());
            int x = (int)(e1.getRawX() - e2.getRawX());
            Log.i("Scroll", Integer.toString(y));
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) chatBoxContainer.getLayoutParams();
            switch (rotation) {
                case Surface.ROTATION_90:
                    if (mode == ME) {
                        if (x < 16) x = 16;
                        if (((float)x / (float)screenWidth) > .2) {
                            slideChatBoxLeft();
                            if (viewMode != MIC_VIEW) new TranslateTask("es").execute();
                            setMode(YOU);
                            x = 16;
                        }
                        params.rightMargin = x;
                    }else if (mode == YOU) {
                        x = -x;
                        if (x < 16) x = 16;
                        if (((float)x / (float)screenWidth) > .2) {
                            slideChatBoxRight();
                            if (viewMode != MIC_VIEW) new TranslateTask("en").execute();
                            setMode(ME);
                            x = 16;
                        }
                        params.leftMargin = x;
                    }
                    break;
                case Surface.ROTATION_270:
                    if (mode == YOU) {
                        if (x < 16) x = 16;
                        if (((float)x / (float)screenWidth) > .2) {
                            slideChatBoxLeft();
                            if (viewMode != MIC_VIEW) new TranslateTask("en").execute();
                            setMode(ME);
                            x = 16;
                        }
                        params.rightMargin = x;
                    }else if (mode == ME) {
                        x = -x;
                        if (x < 16) x = 16;
                        if (((float)x / (float)screenWidth) > .2) {
                            slideChatBoxRight();
                            if (viewMode != MIC_VIEW) new TranslateTask("es").execute();
                            setMode(YOU);
                            x = 16;
                        }
                        params.leftMargin = x;
                    }
                    break;
                default:
                    if (mode == ME) {
                        if (y < 16) y = 16;
                        if (((float)y / (float)screenHeight) > .2) {
                            slideChatBoxUp();
                            if (viewMode != MIC_VIEW) new TranslateTask("es").execute();
                            setMode(YOU);
                            y = 16;
                        }
                        params.bottomMargin = y;
                    }else if (mode == YOU) {
                        y = -y;
                        if (y < 16) y = 16;
                        if (((float)y / (float)screenHeight) > .2) {
                            slideChatBoxDown();
                            if (viewMode != MIC_VIEW) new TranslateTask("en").execute();
                            setMode(ME);
                            y = 16;
                        }
                        params.topMargin = y;
                    }
                    break;
            }

            chatBoxContainer.setLayoutParams(params);
            return true;
        }

    }

    private class TranslateTask extends AsyncTask<Void, Void, String> {

        private String language;
        public TranslateTask(String language) {
            this.language = language;
        }

        @Override
        protected  void onPreExecute() {
            loadingView();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String text = chatBoxText.getText().toString();
            try {
                // Bad line of code below I know
                TranslateOptions options = TranslateOptions.newBuilder().setApiKey("AIzaSyDhW6TDvQXd9FNPjPLU5lM2Z9sBmElknRM")
                        .build();
                Translate translate = options.getService();
                final Translation translation = translate.translate(text, Translate.TranslateOption.targetLanguage(language));
                return translation.getTranslatedText();
            } catch (Exception e){
                Log.i("ERROR", e.toString());
                return "";
            }
        }

        @Override
        protected void onPostExecute(final String translated) {
            chatBoxText.setText(translated);
            receiveMessageView();
            Log.i("Translated", translated);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

}
