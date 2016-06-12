package com.mercadopago;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.mercadopago.callbacks.Callback;
import com.mercadopago.callbacks.FailureRecovery;
import com.mercadopago.core.MercadoPago;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.Instruction;
import com.mercadopago.model.InstructionActionInfo;
import com.mercadopago.model.InstructionReference;
import com.mercadopago.model.Payment;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.mptracker.MPTracker;
import com.mercadopago.util.ApiUtil;
import com.mercadopago.util.CurrenciesUtil;
import com.mercadopago.util.ErrorUtil;
import com.mercadopago.util.LayoutUtil;
import com.mercadopago.util.ScaleUtil;
import com.mercadopago.views.MPButton;
import com.mercadopago.views.MPTextView;

import java.util.List;

public class InstructionsActivity extends AppCompatActivity {

    //Values
    protected MercadoPago mMercadoPago;
    protected FailureRecovery failureRecovery;
    protected Boolean mBackPressedOnce;
    protected boolean mActiveActivity;

    //Controls
    protected LinearLayout mReferencesLayout;
    protected Activity mActivity;
    protected MPTextView mTitle;
    protected MPTextView mPrimaryInfo;
    protected MPTextView mSecondaryInfo;
    protected MPTextView mTertiaryInfo;
    protected MPTextView mAccreditationMessage;
    protected MPButton mActionButton;
    protected MPTextView mExitTextView;

    //Params
    protected Payment mPayment;
    protected String mMerchantPublicKey;
    protected PaymentMethod mPaymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);
        getActivityParameters();
        initializeControls();
        mBackPressedOnce = false;
        mActivity = this;
        mActiveActivity = true;
        mMercadoPago = new MercadoPago.Builder()
                .setContext(this)
                .setPublicKey(mMerchantPublicKey)
                .build();
        getInstructionsAsync();
    }

    protected void getInstructionsAsync() {

        LayoutUtil.showProgressLayout(this);
        mMercadoPago.getInstructions(mPayment.getId(), mPaymentMethod.getPaymentTypeId(), new Callback<Instruction>() {
            @Override
            public void success(Instruction instruction) {
                MPTracker.getInstance().trackEvent( "INSTRUCTIONS", "GET_INSTRUCTION_RESPONSE", "SUCCESS", "2", mMerchantPublicKey, "MLA", "1.0", mActivity);
                showInstructions(instruction);
                LayoutUtil.showRegularLayout(mActivity);
            }

            @Override
            public void failure(ApiException apiException) {
                MPTracker.getInstance().trackEvent( "INSTRUCTIONS", "GET_INSTRUCTION_RESPONSE", "FAIL", "2", mMerchantPublicKey, "MLA", "1.0", mActivity);
                if (mActiveActivity) {
                    ApiUtil.showApiExceptionError(mActivity, apiException);
                    failureRecovery = new FailureRecovery() {
                        @Override
                        public void recover() {
                            getInstructionsAsync();
                        }
                    };
                }
            }
        });
    }

    protected void showInstructions(Instruction instruction) {
        MPTracker.getInstance().trackScreen( "INSTRUCTIONS", "2", mMerchantPublicKey, "MLA", "1.0", this);

        setTitle(instruction.getTitle());
        setInformationMessages(instruction);
        setReferencesInformation(instruction);
        mAccreditationMessage.setText(instruction.getAcreditationMessage());
        setActions(instruction);
    }

    protected void setTitle(String title) {
        Spanned formattedTitle = CurrenciesUtil.formatCurrencyInText(mPayment.getTransactionAmount(), mPayment.getCurrencyId(), title, true, true);
        mTitle.setText(formattedTitle);
    }

    protected void setActions(Instruction instruction) {
        if(instruction.getActions() != null && !instruction.getActions().isEmpty()) {
            final InstructionActionInfo actionInfo = instruction.getActions().get(0);
            if(actionInfo.getUrl() != null && !actionInfo.getUrl().isEmpty()) {
                mActionButton.setVisibility(View.VISIBLE);
                mActionButton.setText(actionInfo.getLabel());
                mActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(actionInfo.getUrl()));
                        startActivity(browserIntent);
                    }
                });
            }
        }
    }

    protected void setReferencesInformation(Instruction instruction) {
        LinearLayout.LayoutParams marginParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginTop = ScaleUtil.getPxFromDp(3, this);
        int marginBottom = ScaleUtil.getPxFromDp(15, this);
        for(InstructionReference reference : instruction.getReferences()) {
            MPTextView currentTitleTextView = new MPTextView(this);
            MPTextView currentValueTextView = new MPTextView(this);

            if(reference.hasValue()) {

                if (reference.hasLabel()) {
                    currentTitleTextView.setText(reference.getLabel().toUpperCase());
                    currentTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.mpsdk_smaller_text));
                    mReferencesLayout.addView(currentTitleTextView);
                }

                String formattedReference = reference.getFormattedReference();
                int referenceSize = getTextSizeForReference();

                currentValueTextView.setText(formattedReference);
                currentValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, referenceSize);

                marginParams.setMargins(0, marginTop, 0, marginBottom);
                currentValueTextView.setLayoutParams(marginParams);
                mReferencesLayout.addView(currentValueTextView);
            }
        }
    }

    private int getTextSizeForReference() {
        return getResources().getDimensionPixelSize(R.dimen.mpsdk_large_text);
    }

    private void setInformationMessages(Instruction instruction) {
        if(instruction.getInfo() != null && !instruction.getInfo().isEmpty()) {
            mPrimaryInfo.setText(Html.fromHtml(getInfoHtmlText(instruction.getInfo())));
        }
        else {
            mPrimaryInfo.setVisibility(View.GONE);
        }
        if(instruction.getSecondaryInfo() != null && !instruction.getSecondaryInfo().isEmpty()) {
            mSecondaryInfo.setText(Html.fromHtml(getInfoHtmlText(instruction.getSecondaryInfo())));
        }
        else {
            mSecondaryInfo.setVisibility(View.GONE);
        }
        if(instruction.getTertiaryInfo() != null && !instruction.getTertiaryInfo().isEmpty()) {
            mTertiaryInfo.setText(Html.fromHtml(getInfoHtmlText(instruction.getTertiaryInfo())));
        }
        else {
            mTertiaryInfo.setVisibility(View.GONE);
        }
    }

    protected String getInfoHtmlText(List<String> info) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String line : info) {
            stringBuilder.append(line);
            if(!line.equals(info.get(info.size() - 1))) {
                stringBuilder.append("<br/>");
            }
        }
        return stringBuilder.toString();
    }

    protected void getActivityParameters() {
        mPayment = (Payment) getIntent().getSerializableExtra("payment");
        mMerchantPublicKey = this.getIntent().getStringExtra("merchantPublicKey");
        mPaymentMethod = (PaymentMethod) getIntent().getSerializableExtra("paymentMethod");
    }

    protected void initializeControls() {
        mReferencesLayout = (LinearLayout) findViewById(R.id.mpsdkReferencesLayout);
        mTitle = (MPTextView) findViewById(R.id.mpsdkTitle);
        mPrimaryInfo = (MPTextView) findViewById(R.id.mpsdkPrimaryInfo);
        mSecondaryInfo = (MPTextView) findViewById(R.id.mpsdkSecondaryInfo);
        mTertiaryInfo = (MPTextView) findViewById(R.id.mpsdkTertiaryInfo);
        mAccreditationMessage = (MPTextView) findViewById(R.id.mpsdkAccreditationMessage);
        mActionButton = (MPButton) findViewById(R.id.mpsdkActionButton);
        mExitTextView = (MPTextView) findViewById(R.id.mpsdkExitTextView);
        mExitTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                animateOut();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ErrorUtil.ERROR_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                recoverFromFailure();
            }
            else {
                setResult(RESULT_CANCELED, data);
                finish();
            }
        }
    }

    private void animateOut() {
        overridePendingTransition(R.anim.slide_right_to_left_in, R.anim.slide_right_to_left_out);
    }

    private void recoverFromFailure() {
        if(failureRecovery != null) {
            failureRecovery.recover();
        }
    }

    @Override
    protected void onResume() {
        mActiveActivity = true;
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mActiveActivity = false;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mActiveActivity = false;
        super.onPause();
    }

    @Override
    protected void onStop() {
        mActiveActivity = false;
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if(mBackPressedOnce) {
            super.onBackPressed();
        }
        else {
            MPTracker.getInstance().trackEvent("INSTRUCTION", "BACK_PRESSED", "2", mMerchantPublicKey, "MLA", "1.0", this);
            Snackbar.make(mTertiaryInfo, getString(R.string.mpsdk_press_again_to_leave), Snackbar.LENGTH_LONG).show();
            mBackPressedOnce = true;
            resetBackPressedOnceIn(4000);
        }
    }

    private void resetBackPressedOnceIn(final int mills) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(mills);
                    mBackPressedOnce = false;
                } catch (InterruptedException e) {
                    //Do nothing
                }
            }
        }).start();
    }
}
