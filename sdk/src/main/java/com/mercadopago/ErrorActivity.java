package com.mercadopago;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.mercadopago.exceptions.MPException;
import com.mercadopago.util.ApiUtil;

public class ErrorActivity extends AppCompatActivity {

    private MPException mMPException;
    private TextView mErrorMessageTextView;
    private TextView mRetryTextView;
    private TextView mExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        animateErrorScreenLaunch();
        setContentView(R.layout.activity_error);
        getActivityParameters();
        if(validParameters()) {
            initializeControls();
            fillData();
        }
        else {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    private void animateErrorScreenLaunch() {
        overridePendingTransition(R.anim.fade_in_seamless, R.anim.fade_out_seamless);
    }

    private boolean validParameters() {
        return mMPException != null;
    }

    private void getActivityParameters() {
        this.mMPException = (MPException) getIntent().getSerializableExtra("mpException");
    }

    private void initializeControls() {
        this.mErrorMessageTextView = (TextView) findViewById(R.id.mpsdkErrorMessage);
        this.mRetryTextView = (TextView) findViewById(R.id.mpsdkErrorRetry);
        this.mExit = (TextView) findViewById(R.id.mpsdkExit);
        this.mExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void fillData() {
        String message;
        if(mMPException.getApiException() != null) {
            message = ApiUtil.getApiExceptionMessage(this, mMPException.getApiException());
        }
        else {
            message = mMPException.getMessage();
        }

        this.mErrorMessageTextView.setText(message);

        if (mMPException.isRecoverable())
        {
            mRetryTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }
        else {
            mRetryTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("mpException", mMPException);
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
