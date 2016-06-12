package com.mercadopago;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;

import com.mercadopago.core.MercadoPago;
import com.mercadopago.fragments.CardFrontFragment;
import com.mercadopago.model.Cardholder;
import com.mercadopago.model.DecorationPreference;
import com.mercadopago.model.Issuer;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.model.Setting;
import com.mercadopago.model.Token;
import com.mercadopago.util.JsonUtil;
import com.mercadopago.views.MPTextView;

import java.math.BigDecimal;

public abstract class ShowCardActivity extends FrontCardActivity {

    public static Integer LAST_DIGITIS_LENGTH = 4;

    protected MercadoPago mMercadoPago;

    //Card container
    protected FrameLayout mCardContainer;
    protected CardFrontFragment mFrontFragment;

    //Card data
    protected String mBin;
    protected BigDecimal mAmount;
    protected String mPublicKey;
    protected Token mToken;
    protected String mSecurityCodeLocation;
    protected Cardholder mCardholder;
    protected int mCardNumberLength;

    //Local vars
    protected Issuer mSelectedIssuer;
    protected MPTextView mToolbarTitle;
    protected DecorationPreference mDecorationPreference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected abstract void finishWithResult();

    protected void getActivityParameters() {
        mCurrentPaymentMethod = JsonUtil.getInstance().fromJson(
                this.getIntent().getStringExtra("paymentMethod"), PaymentMethod.class);
        mPublicKey = getIntent().getStringExtra("publicKey");
        mToken = JsonUtil.getInstance().fromJson(
                this.getIntent().getStringExtra("token"), Token.class);


        if(this.getIntent().getSerializableExtra("issuer") != null) {
            mSelectedIssuer = (Issuer) this.getIntent().getSerializableExtra("issuer");
        }

        if(this.getIntent().getSerializableExtra("decorationPreference") != null) {
            mDecorationPreference = (DecorationPreference) this.getIntent().getSerializableExtra("decorationPreference");
        }

        if(mToken != null) {
            setCardInfo();
        }
    }

    private void setCardInfo() {
        mBin = mToken.getFirstSixDigits();
        mCardholder = mToken.getCardholder();
        Setting setting = Setting.getSettingByBin(mCurrentPaymentMethod.getSettings(),
                mToken.getFirstSixDigits());

        if (setting != null) {
            mCardNumberLength = setting.getCardNumber().getLength();
        } else {
            mCardNumberLength = CARD_NUMBER_MAX_LENGTH;
        }

        mSecurityCodeLocation = setting.getSecurityCode().getCardLocation();
    }

    protected void initializeToolbar(String title, boolean transparent) {
        Toolbar toolbar;
        if(transparent) {
            toolbar = (Toolbar) findViewById(R.id.mpsdkToolbar);
        }
        else {
            toolbar = (Toolbar) findViewById(R.id.mpsdkRegularToolbar);
            toolbar.setVisibility(View.VISIBLE);
        }
        mToolbarTitle = (MPTextView) findViewById(R.id.mpsdkTitle);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        mToolbarTitle.setText(title);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        if(mDecorationPreference != null) {
            if(mDecorationPreference.hasColors()) {
                if(toolbar != null) {
                    decorateToolbar(toolbar);
                }
            }
        }
    }

    private void decorateToolbar(Toolbar toolbar) {
        if(mDecorationPreference.isDarkFontEnabled()) {
            Drawable upArrow = toolbar.getNavigationIcon();
            if(upArrow != null) {
                upArrow.setColorFilter(mDecorationPreference.getDarkFontColor(this), PorterDuff.Mode.SRC_ATOP);
            }
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
            mToolbarTitle.setTextColor(mDecorationPreference.getDarkFontColor(this));
        }
        toolbar.setBackgroundColor(mDecorationPreference.getLighterColor());
    }

    protected void initializeCard() {
        if (mCurrentPaymentMethod == null || mToken == null || mCardholder == null) {
            return;
        }
        saveCardNumber(getCardNumberHidden());
        saveCardName(mCardholder.getName());
        saveCardExpiryMonth(String.valueOf(mToken.getExpirationMonth()));
        saveCardExpiryYear(String.valueOf(mToken.getExpirationYear()).substring(2,4));
        if (mCurrentPaymentMethod.isSecurityCodeRequired(mBin)
                && mSecurityCodeLocation.equals(CARD_SIDE_FRONT)) {
            saveCardSecurityCode(getSecurityCodeHidden());
        }
    }

    protected void initializeFrontFragment() {
        saveErrorState(NORMAL_STATE);
        if (mFrontFragment == null) {
            mFrontFragment = new CardFrontFragment();
            mFrontFragment.disableAnimate();
            mFrontFragment.setDecorationPreference(mDecorationPreference);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.mpsdkActivityNewCardContainer, mFrontFragment)
                .commit();
    }

    private String getCardNumberHidden() {
        StringBuilder sb = new StringBuilder();
        int length = mToken.getCardNumberLength();
        for (int i = 0; i < length - LAST_DIGITIS_LENGTH; i++) {
            sb.append("X");
        }
        sb.append(mToken.getLastFourDigits());
        return sb.toString();
    }

    private String getSecurityCodeHidden() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mToken.getSecurityCodeLength(); i++) {
            sb.append("X");
        }
        return sb.toString();
    }

    @Override
    public int getCardNumberLength() {
        return mCardNumberLength;
    }

    @Override
    public int getSecurityCodeLength() {
        if (mToken == null) {
            return CARD_DEFAULT_SECURITY_CODE_LENGTH;
        }
        return mToken.getSecurityCodeLength();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in_seamless, R.anim.fade_out_seamless);
    }

    @Override
    public boolean isSecurityCodeRequired() {
        return mCurrentPaymentMethod == null || mCurrentPaymentMethod.isSecurityCodeRequired(mBin);
    }

    @Override
    public String getSecurityCodeLocation() {
        if (mCurrentPaymentMethod == null || mBin == null) {
            return CARD_SIDE_BACK;
        }
        Setting setting = Setting.getSettingByBin(mCurrentPaymentMethod.getSettings(), mBin);
        return setting.getSecurityCode().getCardLocation();
    }
}
