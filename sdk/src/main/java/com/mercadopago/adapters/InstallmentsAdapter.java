package com.mercadopago.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mercadopago.R;
import com.mercadopago.model.PayerCost;
import com.mercadopago.util.CurrenciesUtil;
import com.mercadopago.views.MPTextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InstallmentsAdapter extends  RecyclerView.Adapter<InstallmentsAdapter.ViewHolder> {


    private Context mContext;
    private List<PayerCost> mInstallmentsList;
    private String mCurrencyId;

    public InstallmentsAdapter(Context context, String currency) {
        this.mContext = context;
        this.mCurrencyId = currency;
        this.mInstallmentsList = new ArrayList<>();
    }

    public void addResults(List<PayerCost> list) {
        mInstallmentsList.addAll(list);
        notifyDataSetChanged();
    }

    public void clear() {
        mInstallmentsList.clear();
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View adapterView = inflater.inflate(R.layout.row_payer_cost_edit, parent, false);
        ViewHolder viewHolder = new ViewHolder(adapterView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PayerCost payerCost = mInstallmentsList.get(position);

        holder.mInstallmentsTextView.setText(getInstallmentsText(payerCost));

        if (payerCost.getInstallmentRate().equals(BigDecimal.ZERO)) {
            holder.mRateTextView.setVisibility(View.GONE);
            if (payerCost.getInstallments() != 1) {
                holder.mZeroRateTextView.setVisibility(View.VISIBLE);
            } else {
                holder.mZeroRateTextView.setVisibility(View.GONE);
            }
        } else {
            holder.mZeroRateTextView.setVisibility(View.GONE);
            holder.mRateTextView.setText(getInstallmentsRateText(payerCost));
            holder.mRateTextView.setVisibility(View.VISIBLE);
        }

    }

    private Spanned getInstallmentsText(PayerCost payerCost) {
        StringBuffer sb = new StringBuffer();
        sb.append(payerCost.getInstallments());
        sb.append(" ");
        sb.append(mContext.getString(R.string.mpsdk_installments_of));
        sb.append(" ");

        sb.append(CurrenciesUtil.formatNumber(payerCost.getInstallmentAmount(), mCurrencyId));
        return CurrenciesUtil.formatCurrencyInText(payerCost.getInstallmentAmount(),
                mCurrencyId, sb.toString(), true, true);
    }

    private Spanned getInstallmentsRateText(PayerCost payerCost) {
        StringBuffer sb = new StringBuffer();
        sb.append("( ");
        sb.append(CurrenciesUtil.formatNumber(payerCost.getTotalAmount(), mCurrencyId));
        sb.append(" )");
        return CurrenciesUtil.formatCurrencyInText(payerCost.getTotalAmount(),
                mCurrencyId, sb.toString(), true, true);
    }

    public PayerCost getItem(int position) {
        return mInstallmentsList.get(position);
    }

    @Override
    public int getItemCount() {
        return mInstallmentsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public MPTextView mInstallmentsTextView;
        public MPTextView mZeroRateTextView;
        public MPTextView mRateTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mInstallmentsTextView = (MPTextView) itemView.findViewById(R.id.mpsdkInstallmentsText);
            mZeroRateTextView = (MPTextView) itemView.findViewById(R.id.mpsdkInstallmentsZeroRate);
            mRateTextView = (MPTextView) itemView.findViewById(R.id.mpsdkInstallmentsWithRate);
        }
    }


}
