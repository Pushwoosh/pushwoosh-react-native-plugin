package com.pushwoosh.reactnativeplugin;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.pushwoosh.inapp.InAppFacade;
import com.pushwoosh.internal.utils.Log;

import java.io.Serializable;
import java.util.Map;

public class PostEventActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT = "PW_EVENT";
    private static final String EXTRA_ATTRIBUTES = "PW_ATTRIBUTES";

    public static Intent createIntent(Context context, String event, Map<String, Object> attributes) {
        Intent intent = new Intent(context, PostEventActivity.class);
        intent.putExtra(EXTRA_EVENT, event);
        intent.putExtra(EXTRA_ATTRIBUTES, (Serializable) attributes);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        try {
            String event = intent.getExtras().getString(EXTRA_EVENT);
            Map<String, Object> attributes = (Map<String, Object>) intent.getExtras().getSerializable(EXTRA_ATTRIBUTES);
            InAppFacade.postEvent(this, event, attributes);
        }
        catch (Exception e) {
            Log.exception(e);
        }
        finally {
            finish();
        }
    }
}
