package mreverter.workshopexample;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.mercadopago.core.MercadoPago;
import com.mercadopago.model.Issuer;
import com.mercadopago.model.PayerCost;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.model.Sites;

import java.math.BigDecimal;

public class ComponentsActivity extends AppCompatActivity {

    private CoordinatorLayout mCoordinatorLayout;
    private Button mIssuersButton;
    private Button mInstallmentsButton;

    private String mMyPublicKey;
    private BigDecimal mAmount;

    private PaymentMethod mSelectedPaymentMethod;
    private Issuer mSelectedIssuer;
    private PayerCost mSelectedPayerCost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_components);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mIssuersButton = (Button) findViewById(R.id.issuersButton);
        mInstallmentsButton = (Button) findViewById(R.id.installmentsButton);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAmount = new BigDecimal(100);
        mMyPublicKey = "TEST-ad365c37-8012-4014-84f5-6c895b3f8e0a";
    }

    public void startPaymentMethodsComponent(View view) {
        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(mMyPublicKey)
                .startPaymentMethodsActivity();
    }

    public void startIssuersComponent(View view) {

        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(mMyPublicKey)
                .setPaymentMethod(mSelectedPaymentMethod)
                .startIssuersActivity();
    }

    public void startInstallmentsComponent(View view) {
        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(mMyPublicKey)
                .setPaymentMethod(mSelectedPaymentMethod)
                .setIssuer(mSelectedIssuer)
                .setSite(Sites.ARGENTINA)
                .setAmount(mAmount)
                .startInstallmentsActivity();
    }

    public void startCardWithoutInstallmentsComponent(View view) {
        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(mMyPublicKey)
                .startGuessingCardActivity();
    }

    public void startCardWithInstallmentsComponent(View view) {
        new MercadoPago.StartActivityBuilder()
                .setActivity(this)
                .setPublicKey(mMyPublicKey)
                .setAmount(mAmount)
                .setSite(Sites.ARGENTINA)
                .startCardVaultActivity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MercadoPago.PAYMENT_METHODS_REQUEST_CODE) {
            resolvePaymentMethodsResult(resultCode, data);
        }
        else if(requestCode == MercadoPago.ISSUERS_REQUEST_CODE) {
            resolveIssuersResult(resultCode, data);
        }
        else if(requestCode == MercadoPago.INSTALLMENTS_REQUEST_CODE) {
            resolveInstallmentsResult(resultCode, data);
        }
        else if(requestCode == MercadoPago.GUESSING_CARD_REQUEST_CODE) {
            resolveCardWithoutInstallmentsResult(resultCode, data);
        }
        else if(requestCode == MercadoPago.CARD_VAULT_REQUEST_CODE) {
            resolveCardWithInstallmentsResult(resultCode, data);
        }
    }

    private void resolvePaymentMethodsResult(int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            mSelectedPaymentMethod = (PaymentMethod) data.getSerializableExtra("paymentMethod");

            mIssuersButton.setEnabled(true);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Medio de pago: ");
            stringBuilder.append(mSelectedPaymentMethod.getName());
            showText(stringBuilder.toString());
        }
    }

    private void resolveIssuersResult(int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            mSelectedIssuer = (Issuer) data.getSerializableExtra("issuer");
            mInstallmentsButton.setEnabled(true);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Banco: ");
            stringBuilder.append(mSelectedIssuer.getName());
            showText(stringBuilder.toString());
        }
    }

    private void resolveInstallmentsResult(int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            mSelectedPayerCost = (PayerCost) data.getSerializableExtra("payerCost");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(mSelectedPayerCost.getRecommendedMessage());
            showText(stringBuilder.toString());
        }
    }

    private void resolveCardWithoutInstallmentsResult(int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            mSelectedPaymentMethod = (PaymentMethod) data.getSerializableExtra("paymentMethod");
            mSelectedIssuer = (Issuer) data.getSerializableExtra("issuer");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Se seleccionó: " + mSelectedPaymentMethod.getName() + " - " + mSelectedIssuer.getName());
            showText(stringBuilder.toString());
        }
    }

    private void resolveCardWithInstallmentsResult(int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            mSelectedPaymentMethod = (PaymentMethod) data.getSerializableExtra("paymentMethod");
            mSelectedIssuer = (Issuer) data.getSerializableExtra("issuer");
            mSelectedPayerCost = (PayerCost) data.getSerializableExtra("payerCost");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Se seleccionó: " + mSelectedPaymentMethod.getName()
                    + " - " + mSelectedIssuer.getName() + " con " + mSelectedPayerCost.getRecommendedMessage());
            showText(stringBuilder.toString());
        }
    }

    private void showText(String text) {
        final Snackbar snackbar = Snackbar.make(mCoordinatorLayout, text, Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }

    public void restartExample(View view) {
        mSelectedPaymentMethod = null;
        mSelectedIssuer = null;
        mSelectedPayerCost = null;
        mIssuersButton.setEnabled(false);
        mInstallmentsButton.setEnabled(false);
    }
}
