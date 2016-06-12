package com.mercadopago;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.mercadopago.adapters.InstallmentsAdapter;
import com.mercadopago.callbacks.Callback;
import com.mercadopago.callbacks.FailureRecovery;
import com.mercadopago.core.MercadoPago;
import com.mercadopago.listeners.RecyclerItemClickListener;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.Installment;
import com.mercadopago.model.PayerCost;
import com.mercadopago.model.PaymentPreference;
import com.mercadopago.model.Site;
import com.mercadopago.mptracker.MPTracker;
import com.mercadopago.util.ApiUtil;
import com.mercadopago.util.ErrorUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InstallmentsActivity extends ShowCardActivity {

    //InstallmentsContainer
    private RecyclerView mInstallmentsView;
    private InstallmentsAdapter mInstallmentsAdapter;
    private ProgressBar mProgressBar;
    private View mCardBackground;

    private Activity mActivity;
    protected boolean mActiveActivity;

    //Local vars
    private List<PayerCost> mPayerCosts;
    private PayerCost mSelectedPayerCost;
    private PaymentPreference mPaymentPreference;
    private FailureRecovery mFailureRecovery;
    private Site mSite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivityParameters();
        if(mDecorationPreference != null && mDecorationPreference.hasColors()) {
            setTheme(R.style.Theme_MercadoPagoTheme_NoActionBar);
        }
        setContentView();
        mActivity = this;
        mActiveActivity = true;
        setLayout();
        initializeAdapter();
        initializeToolbar();

        mMercadoPago = new MercadoPago.Builder()
                .setContext(this)
                .setPublicKey(mPublicKey)
                .build();

        if (mCurrentPaymentMethod != null) {
            initializeCard();
        }
        if(mToken != null) {
            initializeFrontFragment();
        }
        else {
            hideCardLayout();
        }
    }

    private void hideCardLayout() {
        mCardBackground.setVisibility(View.GONE);
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

    protected void setContentView() {
        MPTracker.getInstance().trackScreen( "CARD_INSTALLMENTS", "2", mPublicKey, "MLA", "1.0", this);
        setContentView(R.layout.activity_new_installments);
    }

    protected void setLayout() {
        mInstallmentsView = (RecyclerView) findViewById(R.id.mpsdkActivityInstallmentsView);
        mCardContainer = (FrameLayout) findViewById(R.id.mpsdkActivityNewCardContainer);
        mProgressBar = (ProgressBar) findViewById(R.id.mpsdkProgressBar);
        mCardBackground = findViewById(R.id.mpsdkCardBackground);

        if(mDecorationPreference != null && mDecorationPreference.hasColors())
        {
            mCardBackground.setBackgroundColor(mDecorationPreference.getLighterColor());
        }
        mProgressBar.setVisibility(View.GONE);
    }

    protected void initializeAdapter() {
        mInstallmentsAdapter = new InstallmentsAdapter(this, mSite.getCurrencyId());
        initializeAdapterListener(mInstallmentsAdapter, mInstallmentsView);
    }

    protected void onItemSelected(View view, int position) {
        mSelectedPayerCost = mPayerCosts.get(position);
    }

    protected void initializeToolbar() {
        if(mToken != null) {
            super.initializeToolbar("", true);
        } else {
            super.initializeToolbar(getString(R.string.mpsdk_card_installments_title), false);
        }
    }

    @Override
    protected void initializeCard() {
        super.initializeCard();

        if (mPayerCosts == null) {
            getInstallmentsAsync();
        } else {
            resolvePayerCosts(mPayerCosts);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void getActivityParameters() {
        super.getActivityParameters();
        mAmount = new BigDecimal(this.getIntent().getStringExtra("amount"));
        mSite = (Site) this.getIntent().getSerializableExtra("site");
        mPayerCosts = (ArrayList<PayerCost>)getIntent().getSerializableExtra("payerCosts");
        mPaymentPreference = (PaymentPreference) this.getIntent().getSerializableExtra("paymentPreference");
        if (mPaymentPreference == null) {
            mPaymentPreference = new PaymentPreference();
        }
    }

    private void getInstallmentsAsync() {
        mProgressBar.setVisibility(View.VISIBLE);

        Long issuerId = mSelectedIssuer == null ? null : mSelectedIssuer.getId();
        mMercadoPago.getInstallments(mBin, mAmount, issuerId, mCurrentPaymentMethod.getId(),
                new Callback<List<Installment>>() {
                    @Override
                    public void success(List<Installment> installments) {
                        MPTracker.getInstance().trackEvent("CARD_INSTALLMENTS", "GET_INSTALLMENTS_RESPONSE", "SUCCESS", "2",mPublicKey, "MLA", "1.0", mActivity);
                        if (mActiveActivity) {
                            mProgressBar.setVisibility(View.GONE);
                            if (installments.size() == 0) {
                                ErrorUtil.startErrorActivity(mActivity, getString(R.string.mpsdk_standard_error_message), "no installments found for an issuer at InstallmentsActivity", false);
                            } else if (installments.size() == 1) {
                                resolvePayerCosts(installments.get(0).getPayerCosts());
                            } else if (installments.size() > 1) {
                                ErrorUtil.startErrorActivity(mActivity, getString(R.string.mpsdk_standard_error_message), "multiple installments found for an issuer at InstallmentsActivity", false);
                            }
                        }
                    }

                    @Override
                    public void failure(ApiException apiException) {
                        MPTracker.getInstance().trackEvent("CARD_INSTALLMENTS", "GET_INSTALLMENTS_RESPONSE", "FAIL", "2",mPublicKey, "MLA", "1.0", mActivity);
                        if (mActiveActivity) {
                            mProgressBar.setVisibility(View.GONE);
                            mFailureRecovery = new FailureRecovery() {
                                @Override
                                public void recover() {
                                    getInstallmentsAsync();
                                }
                            };
                            ApiUtil.showApiExceptionError(mActivity, apiException);
                        }
                    }
                });
    }

    private void resolvePayerCosts(List<PayerCost> payerCosts) {
        PayerCost defaultPayerCost = mPaymentPreference.getDefaultInstallments(payerCosts);
        mPayerCosts = mPaymentPreference.getInstallmentsBelowMax(payerCosts);

        if (defaultPayerCost != null) {
            mSelectedPayerCost = defaultPayerCost;
            finishWithResult();
        } else if(mPayerCosts.isEmpty()) {
            ErrorUtil.startErrorActivity(mActivity, getString(R.string.mpsdk_standard_error_message), "no payer costs found at InstallmentsActivity" ,false);
        } else if (mPayerCosts.size() == 1) {
            mSelectedPayerCost = payerCosts.get(0);
            finishWithResult();
        } else if (payerCosts.size() > 1) {
            initializeInstallments();
        }
    }

    @Override
    protected void finishWithResult() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("payerCost", mSelectedPayerCost);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        MPTracker.getInstance().trackEvent("CARD_INSTALLMENTS", "BACK_PRESSED", "2", mPublicKey, "MLA", "1.0", this);

        Intent returnIntent = new Intent();
        returnIntent.putExtra("backButtonPressed", true);
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

    protected void initializeAdapterListener(RecyclerView.Adapter adapter, RecyclerView view) {
        view.setAdapter(adapter);
        view.setLayoutManager(new LinearLayoutManager(this));
        view.addOnItemTouchListener(new RecyclerItemClickListener(this,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        onItemSelected(view, position);
                        finishWithResult();
                    }
                }));
    }

    private void initializeInstallments() {
        mInstallmentsAdapter.addResults(mPayerCosts);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ErrorUtil.ERROR_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                recoverFromFailure();
            }
            else {
                setResult(resultCode, data);
                finish();
            }
        }
    }

    private void recoverFromFailure() {
        if(mFailureRecovery != null) {
            mFailureRecovery.recover();
        }
    }

}
