package com.pixelplex.qtum.ui.fragment.SendBaseFragment;

import android.content.Context;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;
import com.pixelplex.qtum.dataprovider.RestAPI.QtumService;
import com.pixelplex.qtum.dataprovider.RestAPI.gsonmodels.SendRawTransactionRequest;
import com.pixelplex.qtum.dataprovider.RestAPI.gsonmodels.SendRawTransactionResponse;
import com.pixelplex.qtum.dataprovider.RestAPI.gsonmodels.UnspentOutput;
import com.pixelplex.qtum.datastorage.HistoryList;
import com.pixelplex.qtum.datastorage.KeyStorage;
import com.pixelplex.qtum.datastorage.QtumSharedPreference;
import com.pixelplex.qtum.utils.CurrentNetParams;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


class SendBaseFragmentInteractorImpl implements SendBaseFragmentInteractor {

    private Context mContext;

    SendBaseFragmentInteractorImpl(Context context) {
        mContext = context;
    }

    @Override
    public void getUnspentOutputs(final GetUnspentListCallBack callBack) {
        QtumService.newInstance().getUnspentOutputsForSeveralAddresses(KeyStorage.getInstance().getAddresses())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<UnspentOutput>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                    @Override
                    public void onNext(List<UnspentOutput> unspentOutputs) {

                        for(Iterator<UnspentOutput> iterator = unspentOutputs.iterator();iterator.hasNext();){
                            UnspentOutput unspentOutput = iterator.next();
                            if(unspentOutput.getConfirmations()==0){
                                iterator.remove();
                            }
                        }
                        Collections.sort(unspentOutputs, new Comparator<UnspentOutput>() {
                            @Override
                            public int compare(UnspentOutput unspentOutput, UnspentOutput t1) {
                                return unspentOutput.getAmount().doubleValue() > t1.getAmount().doubleValue() ? 1 : unspentOutput.getAmount().doubleValue() < t1.getAmount().doubleValue() ? -1 : 0;
                            }
                        });
                        callBack.onSuccess(unspentOutputs);
                    }
                });
    }

    @Override
    public void createTx(final String address, final String amountString, final CreateTxCallBack callBack) {
        getUnspentOutputs(new GetUnspentListCallBack() {
            @Override
            public void onSuccess(List<UnspentOutput> unspentOutputs) {
                Transaction transaction = new Transaction(CurrentNetParams.getNetParams());
                Address addressToSend;
                BigDecimal bitcoin = new BigDecimal(100000000);
                try {
                    addressToSend = Address.fromBase58(CurrentNetParams.getNetParams(), address);
                } catch (AddressFormatException a) {
                    callBack.onError("Incorrect Address");
                    return;
                }
                ECKey myKey = KeyStorage.getInstance().getCurrentKey();
                BigDecimal amount = new BigDecimal(amountString);
                BigDecimal fee = new BigDecimal("0.1");

                BigDecimal amountFromOutput = new BigDecimal("0.0");
                BigDecimal overFlow = new BigDecimal("0.0");
                transaction.addOutput(Coin.valueOf((long)(amount.multiply(bitcoin).doubleValue())), addressToSend);

                amount = amount.add(fee);

                for (UnspentOutput unspentOutput : unspentOutputs) {
                    overFlow = overFlow.add(unspentOutput.getAmount());
                    if (overFlow.doubleValue() >= amount.doubleValue()) {
                        break;
                    }
                }
                if (overFlow.doubleValue() < amount.doubleValue()) {
                    //TODO: throw exception
                    callBack.onError("Not enough money");
                    return;
                }
                BigDecimal delivery = overFlow.subtract(amount);
                if (delivery.doubleValue() != 0.0) {
                    transaction.addOutput(Coin.valueOf((long)(delivery.multiply(bitcoin).doubleValue())), myKey.toAddress(CurrentNetParams.getNetParams()));
                }



                for (UnspentOutput unspentOutput : unspentOutputs) {
                    if(unspentOutput.getAmount().doubleValue() != 0.0)
                    for (DeterministicKey deterministicKey : KeyStorage.getInstance().getKeyList(100)) {
                        if (Hex.toHexString(deterministicKey.getPubKeyHash()).equals(unspentOutput.getPubkeyHash())) {
                            Sha256Hash sha256Hash = new Sha256Hash(Utils.parseAsHexOrBase58(unspentOutput.getTxHash()));
                            TransactionOutPoint outPoint = new TransactionOutPoint(CurrentNetParams.getNetParams(), unspentOutput.getVout(), sha256Hash);
                            Script script = new Script(Utils.parseAsHexOrBase58(unspentOutput.getTxoutScriptPubKey()));
                            transaction.addSignedInput(outPoint, script, deterministicKey, Transaction.SigHash.ALL, true);
                            amountFromOutput = amountFromOutput.add(unspentOutput.getAmount());
                            break;
                        }
                    }
                    if (amountFromOutput.doubleValue() >= amount.doubleValue()) {
                        break;
                    }
                }


                transaction.getConfidence().setSource(TransactionConfidence.Source.SELF);
                transaction.setPurpose(Transaction.Purpose.USER_PAYMENT);

                byte[] bytes = transaction.unsafeBitcoinSerialize();

                String transactionHex = Hex.toHexString(bytes);

                Date date = new Date();
                long l = date.getTime() / 1000;
                int i3 = (int) l;
                byte[] bytesData = ByteBuffer.allocate(4).putInt(i3).array();
                byte tmp1 = bytesData[3];
                byte tmp2 = bytesData[2];
                byte tmp3 = bytesData[1];
                byte tmp4 = bytesData[0];
                bytesData[0] = tmp1;
                bytesData[1] = tmp2;
                bytesData[2] = tmp3;
                bytesData[3] = tmp4;

                transactionHex += Hex.toHexString(bytesData);
                callBack.onSuccess(transactionHex);
            }
        });
    }

    @Override
    public void sendTx(String address, String amount, final SendTxCallBack callBack) {
        createTx(address, amount, new CreateTxCallBack() {
            @Override
            public void onSuccess(String txHex) {
                QtumService.newInstance().sendRawTransaction(new SendRawTransactionRequest(txHex, 1))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<SendRawTransactionResponse>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(SendRawTransactionResponse sendRawTransactionResponse) {
                                callBack.onSuccess();
                            }
                        });
            }

            @Override
            public void onError(String error) {
                callBack.onError(error);
            }
        });
    }

    interface GetUnspentListCallBack {
        void onSuccess(List<UnspentOutput> unspentOutputs);
    }

    interface CreateTxCallBack {
        void onSuccess(String txHex);

        void onError(String error);
    }

    interface SendTxCallBack {
        void onSuccess();

        void onError(String error);
    }

    @Override
    public int getPassword() {
        return QtumSharedPreference.getInstance().getWalletPassword(mContext);
    }

    @Override
    public String getBalance() {
        return HistoryList.getInstance().getBalance();
    }

    @Override
    public List<String> getAddresses() {
        return KeyStorage.getInstance().getAddresses();
    }
}