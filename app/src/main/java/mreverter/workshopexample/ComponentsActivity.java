package mreverter.workshopexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mercadopago.callbacks.Callback;
import com.mercadopago.core.MercadoPago;
import com.mercadopago.core.MerchantServer;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.DecorationPreference;
import com.mercadopago.model.Issuer;
import com.mercadopago.model.Item;
import com.mercadopago.model.MerchantPayment;
import com.mercadopago.model.PayerCost;
import com.mercadopago.model.Payment;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.model.PaymentPreference;
import com.mercadopago.model.Sites;
import com.mercadopago.model.Token;

import java.math.BigDecimal;
import java.util.ArrayList;

public class ComponentsActivity extends AppCompatActivity {

    private CoordinatorLayout mCoordinatorLayout;

    private Activity mActivity;

    private PaymentMethod mSelectedPaymentMethod;
    private Issuer mSelectedIssuer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_components);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mActivity = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void startPaymentMethodsComponent(View view) {
        //TODO START PAYMENT METHODS COMPONENT
    }

    public void startIssuersComponent(View view) {
        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(ExampleUtils.PUBLIC_KEY)
                .setPaymentMethod(mSelectedPaymentMethod)
                .startIssuersActivity();
    }

    public void startInstallmentsComponent(View view) {
        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(ExampleUtils.PUBLIC_KEY)
                .setSite(Sites.ARGENTINA)
                .setAmount(ExampleUtils.AMOUT)
                .setIssuer(mSelectedIssuer)
                .setPaymentMethod(mSelectedPaymentMethod)
                .startInstallmentsActivity();
    }

    public void startCardWithInstallmentsComponent(View view) {
        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(ExampleUtils.PUBLIC_KEY)
                .setAmount(ExampleUtils.AMOUT)
                .setSite(Sites.ARGENTINA)
                .startCardVaultActivity();
    }

    public void startPaymentVaultComponent(View view) {

        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(ExampleUtils.PUBLIC_KEY)
                .setAmount(ExampleUtils.AMOUT)
                .setSite(Sites.ARGENTINA)
                .startPaymentVaultActivity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        //TODO PAYMENT METHODS COMPONENT RESULT

        if(requestCode == MercadoPago.ISSUERS_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                mSelectedIssuer = (Issuer) data.getSerializableExtra("issuer");
                showResult(mSelectedPaymentMethod, mSelectedIssuer, null, null);
            }
        }
        if(requestCode == MercadoPago.INSTALLMENTS_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                PayerCost payerCost = (PayerCost) data.getSerializableExtra("payerCost");
                showResult(mSelectedPaymentMethod, mSelectedIssuer, payerCost, null);
            }
        }
        if(requestCode == MercadoPago.CARD_VAULT_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                PaymentMethod paymentMethod = (PaymentMethod) data.getSerializableExtra("paymentMethod");
                Issuer issuer = (Issuer) data.getSerializableExtra("issuer");
                Token token = (Token) data.getSerializableExtra("token");
                PayerCost payerCost = (PayerCost) data.getSerializableExtra("payerCost");

                showResult(paymentMethod, issuer, payerCost, token);
            }
        }
        if(requestCode == MercadoPago.PAYMENT_VAULT_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                PaymentMethod paymentMethod = (PaymentMethod) data.getSerializableExtra("paymentMethod");
                Issuer issuer = (Issuer) data.getSerializableExtra("issuer");
                Token token = (Token) data.getSerializableExtra("token");
                PayerCost payerCost = (PayerCost) data.getSerializableExtra("payerCost");

                showResult(paymentMethod, issuer, payerCost, token);

                //TODO CREATE PAYMENT
            }
        }
    }

    private void showResult(PaymentMethod paymentMethod, Issuer issuer, PayerCost payerCost, Token token) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Se seleccionó: ");
        stringBuilder.append(paymentMethod.getName());
        if(issuer != null) {
            stringBuilder.append(" emitida por " + issuer.getName());
        }
        if(payerCost != null) {
            stringBuilder.append(" con  " + payerCost.getRecommendedMessage());
        }
        if(token != null) {
            stringBuilder.append(" y token id: " + token.getId());
        }
        showText(stringBuilder.toString());
    }

    private void showText(String text) {
        final Snackbar snackbar = Snackbar.make(mCoordinatorLayout, text, Snackbar.LENGTH_INDEFINITE);
        TextView textView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(5);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        snackbar.show();
    }
}
