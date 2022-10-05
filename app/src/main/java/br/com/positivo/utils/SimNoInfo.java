package br.com.positivo.utils;

import java.lang.reflect.Method;
import java.util.List;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.telephony.TelephonyManagerEx;

public final class SimNoInfo
{
    TelephonyManager    _telephonyManager;
    TelephonyManagerEx  _mtkTelephonyManager;
    SubscriptionManager _simManager;

    public SimNoInfo(Context context)
    {
        _telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (Build.VERSION.SDK_INT < 23)
        {
            try
            {
                _mtkTelephonyManager = new TelephonyManagerEx(context);
            }
            catch (Exception ex)
            {
                _mtkTelephonyManager = null;
            }
        }
        else
            _simManager = (SubscriptionManager) context.getSystemService(
                    Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    @Nullable
    public String getDeviceId(int cardNo)
    {
        if (_mtkTelephonyManager != null)
            return _mtkTelephonyManager.getDeviceId(cardNo);

        String IMEI = null;
        try
        {
            IMEI = getDeviceIdBySlot("getDeviceId", cardNo);
        }
        catch (GeminiMethodNotFoundException e)
        {
            try
            {
                IMEI = getDeviceIdBySlot("getDeviceIdGemini", cardNo);
            }
            catch (GeminiMethodNotFoundException e1)
            {
                //Call here for next manufacturer's predicted method name if you wish
            }
        }

        if (IMEI == null)
            IMEI = cardNo == 0 ? _telephonyManager.getDeviceId() : null;

        return IMEI;
    }

    public int getSimState(int cardNo)
    {
        if (_mtkTelephonyManager != null)
            return _mtkTelephonyManager.getSimState(cardNo);

        try
        {
            return getSIMStateBySlot("getSimState", cardNo);
        }
        catch (GeminiMethodNotFoundException e)
        {
            try
            {
                return getSIMStateBySlot("getSimStateGemini", cardNo);
            }
            catch (GeminiMethodNotFoundException e1)
            {
                //Call here for next manufacturer's predicted method name if you wish
            }
        }

        return cardNo == 0 ? _telephonyManager.getSimState() : TelephonyManager.SIM_STATE_UNKNOWN;
    }

    public boolean hasIccCard(int cardNo)
    {
        if (_mtkTelephonyManager != null &&_mtkTelephonyManager.hasIccCard(cardNo))
            return true;

        try
        {
            return Boolean.parseBoolean(callMethodReturnsStringReceivesInt("hasIccCard", cardNo));
        }
        catch (GeminiMethodNotFoundException e1)
        {
            try
            {
                return Boolean.parseBoolean(callMethodReturnsStringReceivesLong("hasIccCard", (long) cardNo));
            }
            catch(GeminiMethodNotFoundException e2)
            {
                try
                {
                    return Boolean.parseBoolean(callMethodReturnsStringReceivesInt("hasIccCardGemini", cardNo));
                }
                catch (GeminiMethodNotFoundException e3)
                {
                    //Call here for next manufacturer's predicted method name if you wish
                }
            }
        }

        return cardNo == 0 ? _telephonyManager.hasIccCard() : false;
    }

    @Nullable
    public String getSimOperatorName(int cardNo)
    {
        if (_simManager != null)
        {
            final List<SubscriptionInfo> sims = _simManager.getActiveSubscriptionInfoList();
            if (sims == null || cardNo >= sims.size()) return null;

            SubscriptionInfo simInfo = sims.get(cardNo);
            return simInfo.getDisplayName().toString();
        }

        if (_mtkTelephonyManager != null)
            return _mtkTelephonyManager.getSimOperatorName(cardNo);

        String simOperator = null;
        try
        {
            simOperator = callMethodReturnsStringReceivesLong("getSimOperatorName", (long) cardNo);
        }
        catch (GeminiMethodNotFoundException e)
        {
            try
            {
                simOperator = callMethodReturnsStringReceivesInt("getSimOperatorNameGemini", cardNo);
            }
            catch (GeminiMethodNotFoundException e1)
            {
                //Call here for next manufacturer's predicted method name if you wish
            }
        }

        if (simOperator == null)
            simOperator = cardNo == 0 ? _telephonyManager.getSimOperatorName() : null;

        return simOperator;
    }

    @Nullable
    private String callMethodReturnsStringReceivesLong(String predictedMethodName, long slotID) throws GeminiMethodNotFoundException
    {
        String result = null;

        try
        {
            final Class<?> telephonyClass = /*Class.forName(*/_telephonyManager.getClass()/*.getName())*/;
            final Class<?>[] parameter = new Class[1];
            parameter[0] = long.class;
            Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);

            final Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            final Object ob_phone = getSimID.invoke(_telephonyManager, obParameter);

            if (ob_phone != null)
                result = ob_phone.toString();
        }
        catch (Exception e)
        {
            throw new GeminiMethodNotFoundException(predictedMethodName);
        }

        return result;
    }

    @Nullable
    private String callMethodReturnsStringReceivesInt(String predictedMethodName, int slotID) throws GeminiMethodNotFoundException
    {
        String result = null;

        try
        {
            final Class<?> telephonyClass = /*Class.forName(*/_telephonyManager.getClass()/*.getName())*/;
            final Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);

            final Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            final Object ob_phone = getSimID.invoke(_telephonyManager, obParameter);

            if (ob_phone != null)
                result = ob_phone.toString();
        }
        catch (Exception e)
        {
            throw new GeminiMethodNotFoundException(predictedMethodName);
        }

        return result;
    }

    @Nullable
    private String getDeviceIdBySlot(String predictedMethodName, int slotID) throws GeminiMethodNotFoundException
    {
        return callMethodReturnsStringReceivesInt(predictedMethodName, slotID);
    }

    private int getSIMStateBySlot(String predictedMethodName, int slotID) throws GeminiMethodNotFoundException
    {
        boolean isReady = false;
        final String result = callMethodReturnsStringReceivesInt(predictedMethodName, slotID);
        if (result != null)
            return Integer.parseInt(result);

        return TelephonyManager.SIM_STATE_UNKNOWN;
    }

    private static class GeminiMethodNotFoundException extends Exception
    {
        private static final long serialVersionUID = -996812356902545308L;
        public GeminiMethodNotFoundException(String info) { super(info); }
    }
}
