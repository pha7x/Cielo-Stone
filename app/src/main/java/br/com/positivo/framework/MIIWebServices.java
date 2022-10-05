package br.com.positivo.framework;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import br.com.positivo.utils.ExceptionFormatter;

/**
 * Class that implement the access to all MII Web Services.
 * @author Leandro G. B. Becker, Carlos Simões Pelegrin
 */
@SuppressLint("SimpleDateFormat")
public class MIIWebServices
{
    private String _WebServiceURL, _KeyboxHttpFileServer;
	
	private static final String TAG = "MIIWebServices";
	public static final String NAMESPACE = "http://www.sap.com/xMII";
	public static final String SOAP_ACTION = "http://www.sap.com/xMII";
	
	public static final int HANDLER_MESSAGE_WS_SUITE_SOFTWARE = 10;
	public static final int HANDLER_MESSAGE_Ws_AutomPLC = 11; 
	public static final int HANDLER_MESSAGE_GET_IMEI_MAC = 12;
	public static final int HANDLER_MESSAGE_Ws_Fct = 13;
	public static final int HANDLER_MESSAGE_WS_NS_Atividade_Status = 14;
	public static final int HANDLER_MESSAGE_WS_Producao = 15;
	public static final int HANDLER_MESSAGE_SET_IMEI_MAC = 16;

	android.os.Handler _AsyncTaskCompletedHandler = null;
	
	/** Constructs the MII web service proxy object.
	 * @param WebServiceURL The URL for the desired web service.
	 * @param KeyboxHttpFileServer The URL of keybox HTTP file server if the keybox method will be used. Can be null otherwise.
	 */
	public MIIWebServices(String WebServiceURL, String KeyboxHttpFileServer) 
	{ 
		_WebServiceURL = WebServiceURL;
		_KeyboxHttpFileServer = KeyboxHttpFileServer;
	} 
	
	/**
	 * Construct an object to use the async versions of the calls.
	 * @param WebServiceURL The URL for the desired web service.
	 * @param KeyboxHttpFileServer The URL of keybox HTTP file server if the keybox method will be used. Can be null otherwise.
	 * @param AsyncTaskCompletedHandler An android.os.Handler object to receive a message when the asynchronous call finishes.
	 */
	public MIIWebServices(String WebServiceURL, String KeyboxHttpFileServer, android.os.Handler AsyncTaskCompletedHandler) 
	{ 
		_WebServiceURL = WebServiceURL;
		_KeyboxHttpFileServer = KeyboxHttpFileServer;
		_AsyncTaskCompletedHandler = AsyncTaskCompletedHandler;
	}
	
	public void setWebServiceURL(String url) { _WebServiceURL = url; }

    public static String stripAccents(String s)
    {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }
	
	/**
	 * Hold the result for the WS_SUITE_SOFTWARE MII Web Service.
	 * @author Leandro G. B. Becker
	 *
	 */
	public class WS_SUITE_SOFTWARE_Result
	{
		private String _CD_INTER;
		private String _CD_STATUS;
		private String _DS_MENSA;
		
		WS_SUITE_SOFTWARE_Result(SoapObject response)
		{
			_CD_INTER = response.getPropertyAsString("CD_INTER");
			_CD_STATUS = response.getPropertyAsString("CD_STATUS");
			_DS_MENSA = response.getPropertyAsString("DS_MENSA");
		}
		
		public String getCD_INTER() { return _CD_INTER; }
		public String getCD_STATUS() { return _CD_STATUS; }
		public String getDS_MENSA() { return _DS_MENSA; }
		
		@Override
		public String toString()
		{
			return "CD_INTER = " + _CD_INTER + "\r\n" + "CD_STATUS = " + _CD_STATUS + "\r\n" + "DS_MENSA = " + _DS_MENSA;
		}
	}
	
	/**
	 * Hold the result for the WS_SUITE_SOFTWARE MII Web Service.
	 * @author Leandro G. B. Becker
	 *
	 */
	public class Ws_AutomPLC_Result
	{
        final private String _sNumeroSerie;
        final private String _sTipo;
        final private String _sMensagem;
		
		Ws_AutomPLC_Result(SoapObject response)
		{
			_sNumeroSerie = response.getPropertyAsString("sNumeroSerie");
			_sTipo = response.getPropertyAsString("sTipo");
			_sMensagem = response.getPropertyAsString("sMensagem");
		}
		
		public String getsNumeroSerie() { return _sNumeroSerie; }
		public String getsTipo() { return _sTipo; }
		public String getsMensagem() { return _sMensagem; }
		
		@Override
		public String toString()
		{
			return "sNumeroSerie = " + _sNumeroSerie + "\r\n" + "sTipo = " + _sTipo + "\r\n" + "sMensagem = " + _sMensagem;
		}
	}
	
	/**
	 * Hold the result for the WS_SUITE_SOFTWARE MII Web Service.
	 * @author Leandro G. B. Becker
	 */
	public class GET_IMEI_MAC_Result
	{
        private int _CODE;
        private String _CD_IMEI_MAC;
        private String _MSG;
        final private int _ItemIndex;
        final private char _ItemType;
		
		GET_IMEI_MAC_Result(SoapObject response, int ItemIndex, char ItemType)
		{
			_CODE = Integer.parseInt(response.getPropertyAsString("CODE"));
			_CD_IMEI_MAC = response.getPropertyAsString("CD_IMEI_MAC");
			_MSG = response.getPropertyAsString("MSG");
			_ItemIndex = ItemIndex;
			_ItemType = ItemType;
		}
		
		public int getCODE() { return _CODE; }
		public String getCD_IMEI_MAC() { return _CD_IMEI_MAC; }
		public String getMSG() { return _MSG; }
		public int getItemIndex() { return _ItemIndex; }
		public char getItemType() { return _ItemType; }
		
		@Override
		public String toString()
		{
			return "CODE = " + _CODE + "\r\n" + "CD_IMEI_MAC = " + _CD_IMEI_MAC + "\r\n" + 
				   "_MSG = " + _MSG + "ItemIndex = " + _ItemIndex + "\r\n" + "ItemType = " + _ItemType;
		}
	}

	/**
	 * Hold the result for the SET_IMEI_MAC MII Web Service.
	 * @author Leandro G. B. Becker
	 */
	public class SET_IMEI_MAC_Result
	{
		final private int _CODE;
		final private String _MSG;
		final private int _ItemIndex;

		SET_IMEI_MAC_Result(SoapObject response, int ItemIndex)
		{
			_CODE = Integer.parseInt(response.getPropertyAsString("CODE"));
			_MSG = response.getPropertyAsString("MSG");
			_ItemIndex = ItemIndex;
		}

		public int getCODE() { return _CODE; }
		public String getMSG() { return _MSG; }
		public int getItemIndex() { return _ItemIndex; }

		@Override
		public String toString()
		{
			return "CODE = " + _CODE + "\r\n" +
					"_MSG = " + _MSG + "ItemIndex = " + _ItemIndex;
		}
	}
	
	/**
	 * Hold the result for the WS_Fct MII Web Service.
	 * @author Leandro G. B. Becker
	 *
	 */
	public class Ws_Fct_Result
	{
        final private String _sNumeroSerie;
        final private String _sTipo;
        final private String _sMensagem;
		
		Ws_Fct_Result(SoapObject response)
		{
			_sNumeroSerie = response.getPropertyAsString("sNumeroSerie");
			_sTipo = response.getPropertyAsString("sTipo");
			_sMensagem = response.getPropertyAsString("sMensagem");
		}
		
		public String getsNumeroSerie() { return _sNumeroSerie; }
		public String getsTipo() { return _sTipo; }
		public String getsMensagem() { return _sMensagem; }
		
		@Override
		public String toString()
		{
			return "sNumeroSerie = " + _sNumeroSerie + "\r\n" + "sTipo = " + _sTipo + "\r\n" + 
				   "sMensagem = " + _sMensagem;
		}
	}
	
	/**
	 * Hold the result for the WS_NS_Atividade_Status MII Web Service.
	 * @author Leandro G. B. Becker
	 *
	 */
	public class WS_NS_Atividade_Status_Result
	{
        final private String _st_status;
        private String _tx_msg;
        final private String _cd_ativi_sw_teste;
        final private String _st_ativi;
        final private String _cd_posto;
		
		WS_NS_Atividade_Status_Result(SoapObject response)
		{
			_st_status = response.getPropertyAsString("st_status");
			_tx_msg = response.getPropertyAsString("tx_msg");
			final String cd_ativi_sw_teste = response.getPropertyAsString("cd_ativi_sw_teste");
			_st_ativi = response.getPropertyAsString("st_ativi");
            final String cd_posto = response.getPropertyAsString("cd_posto");
			
			if (cd_ativi_sw_teste.equals("---"))
                _cd_ativi_sw_teste = "";
            else
                _cd_ativi_sw_teste = cd_ativi_sw_teste;

            if (cd_posto.equals("---"))
				_cd_posto = "";
            else
                _cd_posto = cd_posto;
		}
		
		public String get_st_status() { return _st_status; }
		public String get_tx_msg() { return _tx_msg; }
		public String get_cd_ativi_sw_teste() { return _cd_ativi_sw_teste; }
		public String get_st_ativi() { return _st_ativi; }
		public String get_cd_posto() { return _cd_posto; }
		
		@Override
		public String toString()
		{
			return "st_status = " + _st_status + "\r\n" + "tx_msg = " + _tx_msg + "\r\n" + 
				   "cd_ativi_sw_teste = " + _cd_ativi_sw_teste + "st_ativi = " + _st_ativi + "cd_posto = " + _cd_posto;
		}
	}
	
	/**
	 * Hold the result for the WS_Producao MII Web Service.
	 * @author Leandro G. B. Becker
	 *
	 */
	public class WS_Producao_Result
	{
        final private String _CD_STATUS;
        final private String _DS_MENSA;
		
		WS_Producao_Result(SoapObject response)
		{
			_CD_STATUS = response.getPropertyAsString("CD_STATUS");
			_DS_MENSA = response.getPropertyAsString("DS_MENSA");
		}
		
		public String getCD_STATUS() { return _CD_STATUS; }
		public String getDS_MENSA() { return _DS_MENSA; }
		
		@Override
		public String toString()
		{
			return "CD_STATUS = " + _CD_STATUS + "\r\n" + "DS_MENSA = " + _DS_MENSA;
		}
	}

	/**
	 * Hold the result for the WS_Get_Componentes_Seriados MII Web Service.
	 * @author Leandro G. B. Becker
	 *
	 */
	public class WS_Get_Componentes_Seriados_Result
	{
		final private String _MENSAGEM;
		final private String _OUT;
        final private String _STATUS;

        WS_Get_Componentes_Seriados_Result(SoapObject response)
		{
            _MENSAGEM = response.getPropertyAsString("MENSAGEM");
            _OUT = response.getPropertyAsString("OUT");
            _STATUS = response.getPropertyAsString("STATUS");
		}

        /**
         * MII message.
         * @return
         */
		public String getMENSAGEM() { return _MENSAGEM; }

        /**
         * A XML with the components list.
         * @return
         */
		public String getOUT() { return _OUT; }

        /**
         * The MII status for the call.
         * @return
         */
        public String getSTATUS() { return _STATUS; }

		@Override
		public String toString()
		{
			return "MENSAGEM = " + _MENSAGEM + "\r\n" + "STATUS = " + _STATUS + "\r\n" + "OUT = " + _OUT;
		}
	}
	
	/**
	 * Calls any MII web service.
	 * @param request The SOAP request object.
	 * @param imp_types True to use implicit types in the SOAP xml.
	 * @return Return the MII response as a Soap Object.
	 */
	private SoapObject webServiceCall(final SoapObject request, final boolean imp_types)
	{
        final SoapSerializationEnvelope envelope =
	     /**
		 * Change the XML generation to the format that the fu$%@#n MII understands.
		 *@author Leandro G. B. Becker, Carlos Sim�es Pelegrin
		 */
		new SoapSerializationEnvelope(SoapEnvelope.VER11)
		{
			@Override
			public void write(org.xmlpull.v1.XmlSerializer writer) throws java.io.IOException {
				writer.setPrefix("", MIIWebServices.NAMESPACE);
				writer.setPrefix("soap", env);
				writer.startTag(env, "Envelope");
				writer.startTag(env, "Header");
				writeHeader(writer);
				writer.endTag(env, "Header");
				writer.startTag(env, "Body");
				writeBody(writer);
				writer.endTag(env, "Body");
				writer.endTag(env, "Envelope");
			}

			@Override
			public Object getResponse() throws SoapFault {
				return super.getResponse();
			}
		};
		
		envelope.implicitTypes = true;
		envelope.dotNet = false;
		envelope.setAddAdornments(false);
		envelope.env = SoapSerializationEnvelope.ENV;
		envelope.setOutputSoapObject(request);

        final HttpTransportSE androidHttpTransport = new HttpTransportSE(_WebServiceURL, 90000);
		for (int attempt = 0; attempt < 2; attempt++)
		{
			try
			{
				androidHttpTransport.debug = true;
				androidHttpTransport.call(SOAP_ACTION, envelope);
				androidHttpTransport.getServiceConnection().disconnect();

                final SoapObject response = (SoapObject) envelope.bodyIn;
				if (response.hasProperty("Rowset"))
				{
                    final SoapObject Rowset = (SoapObject)response.getProperty("Rowset");
                    return (SoapObject)Rowset.getProperty("Row");
				}
				else
				{
					throw new Exception("Invalid MII response. No Rowset object was found.");
				}
			}
			catch (final Exception ex)
			{
                final StringBuilder strBuilder = new StringBuilder(4096);
				strBuilder.append("Erro: Exception executando MIIWebServices.webServiceCall(").
					append(_WebServiceURL).
					append(")\n").
					append("Request XML:\n").
					append(androidHttpTransport.requestDump).
					append("Response XML:\n").
					append(androidHttpTransport.responseDump);
				Log.e(TAG, strBuilder.toString(), ex);
			}
		}
		
		return null;
	}
	
	/**
	 * Call the MII WS_SUITE_SOFTWARE web service.
	 * @param SerialNumber Serial number of the device.
	 * @param Suite Test suite. MONTAGEM, RUNIN, FINAL, etc
     * @param Pass True if all tests passed.
     * @param FailedTest The name of failed test if Pass is false (optional).
	 * @param Started Date when test suite started.
	 * @param Finished Date when test suite finished.
	 * @return Return WS_SUITE_SOFTWARE_Result with the answer from MII. 
	 */
	public WS_SUITE_SOFTWARE_Result WS_SUITE_SOFTWARE(final String SerialNumber, final String Suite, boolean Pass, String FailedTest,
													  final java.util.Date Started, final java.util.Date Finished)
	{
		WS_SUITE_SOFTWARE_Result res = null;
		
		try
		{
            final SoapObject request = new SoapObject(NAMESPACE, "XacuteRequest");
			request.addProperty("LoginName", "WSAUTOMPLC");
			request.addProperty("LoginPassword","wsplc1");

            final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            final StringBuilder requestXml = new StringBuilder(1024);

			String testName = "TESTE_UPLOAD";
			if (!Pass)
			{
				if (FailedTest != null)
				{
					if (FailedTest.length() > 20)
						testName = FailedTest.substring(0, 19);
					else
						testName = FailedTest;
				}
			}

			requestXml.append("<?xml version=\"1.0\"?>\r\n").
				append("<WS_SUITE_SOFTWARE>\r\n").
				append("   <SUITE_REQUEST>\r\n").
				append("     <CD_NUMER_SERIE>").append(SerialNumber).append("</CD_NUMER_SERIE>\r\n").
				append("     <CD_SUITE>").append(Suite).append("</CD_SUITE>\r\n").
				append("     <CD_RESUL>").append(Pass ? "S" : "F").append("</CD_RESUL>\r\n").
				append("     <DT_INICI>").append(dateFormatter.format(Started)).append("</DT_INICI>\r\n").
				append("     <DT_FINAL>").append(dateFormatter.format(Finished)).append("</DT_FINAL>\r\n").
			    append("     <SUITE_SOFTWARE>\r\n").
				append("          <item>\r\n").
				append("             <CD_SOFTW>").append(stripAccents(testName)).append("</CD_SOFTW>\r\n").
				append("             <DT_INICI>").append(dateFormatter.format(Started)).append("</DT_INICI>\r\n").
				append("             <DT_FINAL>").append(dateFormatter.format(Finished)).append("</DT_FINAL>\r\n").
				append("             <CD_RESUL>").append(Pass ? "S" : "F").append("</CD_RESUL>\r\n").
				append("             <CD_RETOR>").append(Pass ? "0" : "-1").append("</CD_RETOR>\r\n").
				append("           </item>\r\n").
				append("     </SUITE_SOFTWARE>\r\n").
				append("   </SUITE_REQUEST>\r\n").
				append("</WS_SUITE_SOFTWARE>\r\n");

            final SoapObject inputParams = new SoapObject(NAMESPACE, "InputParams");
			inputParams.addProperty("REQUEST", stripAccents(requestXml.toString()));
			request.addSoapObject(inputParams);

            final SoapObject response;
			response = webServiceCall(request, true);
			if (response != null)
				res = new WS_SUITE_SOFTWARE_Result(response);
		}
		catch(Exception ex)
		{
			Log.e(TAG, "Erro: Exception tratando MIIWebServices.WS_SUITE_SOFTWARE.", ex);
		}
		return res;
	}
	
	/**
	 * Call the WS_SUITE_SOFTWARE method asynchronously. You must pass a android.os.Handler object in the
	 * object constructor to receive a message when the async call finishes.
	 * The android.os.Message.what will be set to HANDLER_MESSAGE_WS_SUITE_SOFTWARE and obj will be set to an instance of WS_SUITE_SOFTWARE_Result.
	 * MUST be called on an UI thread!
	 * @param SerialNumber Serial number of the device.
	 * @param Suite Test suite. MONTAGEM, RUNIN, FINAL, etc
     * @param Pass True if all tests passed.
     * @param FailedTest The name of failed test if Pass is false (optional).
	 * @param Started Date when test suite started.
	 * @param Finished Date when test suite finished.
	 */
	public void WS_SUITE_SOFTWARE_Async(final String SerialNumber, final String Suite, boolean Pass, String FailedTest,
                                        final java.util.Date Started, final java.util.Date Finished)
	{
		new AsyncTask<Object, Void, WS_SUITE_SOFTWARE_Result> ()
        {
            @SuppressLint("NewApi")
			protected WS_SUITE_SOFTWARE_Result doInBackground(Object ... input)
            {
               	return WS_SUITE_SOFTWARE((String)input[0], (String)input[1], (boolean)input[2], (String)input[3], (java.util.Date)input[4], (java.util.Date)input[5]);
            }

            protected void onPostExecute(final WS_SUITE_SOFTWARE_Result result)
            {
            	if (_AsyncTaskCompletedHandler != null)
            		Message.obtain(_AsyncTaskCompletedHandler, HANDLER_MESSAGE_WS_SUITE_SOFTWARE, result).sendToTarget();
            }
            
        }.execute(SerialNumber, Suite, Pass, FailedTest, Started, Finished);
	}
	
	/**
	 * Call the MII Ws_AutomPLC web service.
	 * @param SerialNumber Serial number of the device.
	 * @param AssemblyBooth Assembly PLC code.
	 * @param TestPhase FCT, etc.
	 * @return Return Ws_AutomPLC_Result with the answer from MII. 
	 */
	public Ws_AutomPLC_Result Ws_AutomPLC(final String SerialNumber, final String AssemblyBooth, final String TestPhase)
	{
		Ws_AutomPLC_Result res = null;
		
		try
		{
            final SoapObject request = new SoapObject(NAMESPACE, "XacuteRequest");
			request.addProperty("LoginName", "WSAUTOMPLC");
			request.addProperty("LoginPassword","wsplc1");

            final SoapObject inputParams = new SoapObject(NAMESPACE, "InputParams");
			inputParams.addProperty("CDPosto", AssemblyBooth);
			inputParams.addProperty("Conteudo", "Test Status:  Pass");
			inputParams.addProperty("NumeroSerie", SerialNumber);
			inputParams.addProperty("Tipo", TestPhase);
			request.addSoapObject(inputParams);

            final SoapObject response;
			response = webServiceCall(request, true);
			if (response != null)
				res = new Ws_AutomPLC_Result(response);
		}
		catch(final Exception ex)
		{
			Log.e(TAG, "Erro: Exception tratando MIIWebServices.Ws_AutomPLC.", ex);
		}
		
		return res;
	}
	
	/**
	 * Call the Ws_AutomPLC method asynchronously. You must pass a android.os.Handler object in the
	 * object constructor to receive a message when the async call finishes.
	 * The android.os.Message.what will be set to HANDLER_MESSAGE_Ws_AutomPLC and obj will be set to an instance of Ws_AutomPLC_Result.
	 * MUST be called on an UI thread!
	 * @param SerialNumber Serial number of the device.
	 * @param AssemblyBooth Assembly PLC code.
	 * @param TestPhase FCT, etc. 
	 */
	public void Ws_AutomPLC_Async(final String SerialNumber, final String AssemblyBooth, final String TestPhase)
	{
		new AsyncTask<String, Void, Ws_AutomPLC_Result> ()
        {
            @SuppressLint("NewApi")
			protected Ws_AutomPLC_Result doInBackground(String ... input)
            {
               	return Ws_AutomPLC(input[0], input[1], input[2]);
            }

            protected void onPostExecute(final Ws_AutomPLC_Result result)
            {
            	if (_AsyncTaskCompletedHandler != null)
            		Message.obtain(_AsyncTaskCompletedHandler, HANDLER_MESSAGE_Ws_AutomPLC, result).sendToTarget();
            }
            
        }.execute(SerialNumber, AssemblyBooth, TestPhase);
	}
	
	/**
	 * Call the MII GET_IMEI_MAC web service. If getting a keybox, the GET_IMEI_MAC_Result will hold
	 * a base64 string with the keybox file contents.
	 * @param SerialNumber Serial number of the device.
	 * @param ItemIndex The item index. Ex.: For two MACs, call passing 0 and make a second call passing 1.
	 * @param ItemType The item type. I -> IMEI, M -> MAC WIFI, B -> MAC Bluetooth, K -> Keybox filename.
	 * @return Return GET_IMEI_MAC_Result with the answer from MII. 
	 */
	public GET_IMEI_MAC_Result GET_IMEI_MAC(final String SerialNumber, final int ItemIndex, final char ItemType)
	{
		GET_IMEI_MAC_Result res = null;
		
		try
		{
            final SoapObject request = new SoapObject(NAMESPACE, "XacuteRequest");
			request.addProperty("LoginName", "WSAUTOMPLC");
			request.addProperty("LoginPassword","wsplc1");

			String stringfied = "";
			stringfied += ItemType;
            final SoapObject inputParams = new SoapObject(NAMESPACE, "InputParams");
			inputParams.addProperty("CD_NUMER_SERIE", SerialNumber);
			inputParams.addProperty("CD_SEQ", ((Integer)ItemIndex).toString());
			inputParams.addProperty("CD_TIPO", stringfied);
			request.addSoapObject(inputParams);

            final SoapObject response;
			response = webServiceCall(request, true);
			if (response != null)
			{
				res = new GET_IMEI_MAC_Result(response, ItemIndex, ItemType);				
				if (ItemType == 'K' && (res.getCODE() == 998 || res.getCODE() == 999))
				{
					// MII returns only the keybox filename. To get the file contents, we must
					// call a HTTP web server.
					
					java.net.URL url = new java.net.URL(_KeyboxHttpFileServer + res._CD_IMEI_MAC);
					java.net.HttpURLConnection urlConnection = null;
					try
					{
                        final int read;
                        final byte[] buffer = new byte[10 * 1024];
						urlConnection = (java.net.HttpURLConnection) url.openConnection();

                        final String userPassword = "WSAUTOMPLC:wsplc1";
                        final byte[] userPasswordBytes = userPassword.getBytes("ascii");
                        final String encoded = android.util.Base64.encodeToString(userPasswordBytes, 0, userPasswordBytes.length, android.util.Base64.DEFAULT);
						urlConnection.setRequestProperty("Authorization", "Basic " + encoded);
						
						java.io.InputStream in = urlConnection.getInputStream();
						read = in.read(buffer);
						if (read == buffer.length)
							throw new Exception("Keybox size cannot be greater than 10 KB");
						
						res._CD_IMEI_MAC = android.util.Base64.encodeToString(buffer, 0, read, android.util.Base64.DEFAULT);
					}
					catch(Exception ex)
					{
						res._MSG = ExceptionFormatter.format("Erro: Exception obtendo keybox do webserver "  + _KeyboxHttpFileServer,
								ex, false);

						Log.e(TAG, res._MSG, ex);
						res._CODE = 100;
						res._CD_IMEI_MAC = "";
					}
				    finally
				    {
				    	if (urlConnection != null) urlConnection.disconnect();
				    }
				}
			}
		}
		catch(Exception ex)
		{
			Log.e(TAG, "Erro: Exception tratando MIIWebServices.GET_IMEI_MAC.", ex);
		}
		
		return res;
	}
	
	/**
	 * Call the SET_IMEI_MAC method asynchronously. You must pass a android.os.Handler object in the
	 * object constructor to receive a message when the async call finishes. 
	 * If getting a keybox, the SET_IMEI_MAC_Result will hold
	 * a base64 string with the keybox file contents.
	 * The android.os.Message.what will be set to HANDLER_MESSAGE_SET_IMEI_MAC and obj will be set to an instance of SET_IMEI_MAC_Result.
	 * MUST be called on an UI thread!
	 * @param SerialNumber Serial number of the device.
	 * @param IMEIs The array of IMEIs to send to MII.
	 * @param ItemIndex The item index. Ex.: For two MACs, call passing 0 and make a second call passing 1.
	 * @param ItemType The item type. I -> IMEI, M -> MAC WIFI, B -> MAC Bluetooth.
	 */
	/*public void SET_IMEI_MAC_Async(final String SerialNumber, final String[] IMEIs)
	{
		new AsyncTask<String, Void, SET_IMEI_MAC_Result> ()
        {
            @SuppressLint("NewApi")
			protected SET_IMEI_MAC_Result doInBackground(String ... input)
            {
				String[] IMEIs = new String[Integer.decode(input[3])];
				for (int i = 0; i < IMEIs.length; i++)
					IMEIs[i] = input[i + 4];

               	return SET_IMEI_MAC(input[0], IMEIs);
            }

            protected void onPostExecute(final SET_IMEI_MAC_Result result)
            {
            	if (_AsyncTaskCompletedHandler != null)
            		Message.obtain(_AsyncTaskCompletedHandler, HANDLER_MESSAGE_SET_IMEI_MAC, result).sendToTarget();
            }
            
        }.execute(SerialNumber, Integer.toString(IMEIs.length), IMEIs));
	}*/

	/**
	 * Call the MII SET_IMEI_MAC web service.
	 * @param SerialNumber Serial number of the device.
	 * @param IMEIs The array of IMEIs to send to MII.
	 * @return Return SET_IMEI_MAC_Result with the answer from MII.
	 */
	public SET_IMEI_MAC_Result[] SET_IMEI_MAC(final String SerialNumber, final String[] IMEIs)
	{
		SET_IMEI_MAC_Result[] res = new SET_IMEI_MAC_Result[IMEIs.length];

		for (int i = 0; i < IMEIs.length; i++)
		{
			try
			{
				final SoapObject request = new SoapObject(NAMESPACE, "XacuteRequest");
				request.addProperty("LoginName", "WSAUTOMPLC");
				request.addProperty("LoginPassword", "wsplc1");

				final SoapObject inputParams = new SoapObject(NAMESPACE, "InputParams");
				inputParams.addProperty("CD_IMEI_MAC", IMEIs[i]);
				inputParams.addProperty("CD_NUMER_SERIE", SerialNumber);
				inputParams.addProperty("CD_SEQ", ((Integer)i).toString());
				inputParams.addProperty("CD_TIPO", "I");
				request.addSoapObject(inputParams);

				final SoapObject response;
				response = webServiceCall(request, true);
				if (response != null)
				{
					res[i] = new SET_IMEI_MAC_Result(response, i);
				}
			}
			catch (Exception ex)
			{
				Log.e(TAG, "Erro: Exception tratando MIIWebServices.SET_IMEI_MAC.", ex);
			}
		}
		return res;
	}

	/**
	 * Call the GET_IMEI_MAC method asynchronously. You must pass a android.os.Handler object in the
	 * object constructor to receive a message when the async call finishes.
	 * If getting a keybox, the GET_IMEI_MAC_Result will hold
	 * a base64 string with the keybox file contents.
	 * The android.os.Message.what will be set to HANDLER_MESSAGE_GET_IMEI_MAC and obj will be set to an instance of GET_IMEI_MAC_Result.
	 * MUST be called on an UI thread!
	 * @param SerialNumber Serial number of the device.
	 * @param ItemIndex The item index. Ex.: For two MACs, call passing 0 and make a second call passing 1.
	 * @param ItemType The item type. I -> IMEI, M -> MAC WIFI, B -> MAC Bluetooth, K -> Keybox filename.
	 */
	public void GET_IMEI_MAC_Async(final String SerialNumber, final int ItemIndex, final char ItemType)
	{
		new AsyncTask<String, Void, GET_IMEI_MAC_Result> ()
		{
			@SuppressLint("NewApi")
			protected GET_IMEI_MAC_Result doInBackground(String ... input)
			{
				return GET_IMEI_MAC(input[0], Integer.decode(input[1]), input[2].charAt(0));
			}

			protected void onPostExecute(final GET_IMEI_MAC_Result result)
			{
				if (_AsyncTaskCompletedHandler != null)
					Message.obtain(_AsyncTaskCompletedHandler, HANDLER_MESSAGE_GET_IMEI_MAC, result).sendToTarget();
			}

		}.execute(SerialNumber, Integer.toString(ItemIndex), Character.toString(ItemType));
	}
	
	/**
	 * Call the MII Ws_Fct web service.
	 * @param MAC The device MAC address.
	 * @param SerialNumber Serial number of the device.
	 * @param AssemblyBooth Assembly PLC code.
	 * @return Return Ws_Fct_Result with the answer from MII. 
	 */
	public Ws_Fct_Result Ws_Fct(final String MAC, final String SerialNumber, final String AssemblyBooth)
	{
		Ws_Fct_Result res = null;
		
		try
		{
            final SoapObject request = new SoapObject(NAMESPACE, "XacuteRequest");
			request.addProperty("LoginName", "WSAUTOMPLC");
			request.addProperty("LoginPassword","wsplc1");

            final SoapObject inputParams = new SoapObject(NAMESPACE, "InputParams");
			inputParams.addProperty("CD_MAC", MAC);
			inputParams.addProperty("CD_NS", SerialNumber);
			inputParams.addProperty("CD_POSTO", AssemblyBooth);
			inputParams.addProperty("CD_SENHA", "");
			inputParams.addProperty("CD_USUAR", "");
			request.addSoapObject(inputParams);

            final SoapObject response;
			response = webServiceCall(request, true);
			if (response != null)
				res = new Ws_Fct_Result(response);
		}
		catch(Exception ex)
		{
			Log.e(TAG, "Erro: Exception tratando MIIWebServices.Ws_AutomPLC.", ex);
		}
		
		return res;
	}
	
	/**
	 * Call the WS_Fct method asynchronously. You must pass a android.os.Handler object in the
	 * object constructor to receive a message when the async call finishes.
	 * The android.os.Message.what will be set to HANDLER_MESSAGE_Ws_Fct and obj will be set to an instance of Ws_Fct_Result.
	 * MUST be called on an UI thread!
	 * @param MAC The device MAC address.
	 * @param SerialNumber Serial number of the device.
	 * @param AssemblyBooth Assembly PLC code.
	 */
	public void Ws_Fct_Async(final String MAC, final String SerialNumber, final String AssemblyBooth)
	{
		new AsyncTask<String, Void, Ws_Fct_Result> ()
        {
            @SuppressLint("NewApi")
			protected Ws_Fct_Result doInBackground(String ... input)
            {
               	return Ws_Fct(input[0], input[1], input[2]);
            }

            protected void onPostExecute(final Ws_Fct_Result result)
            {
            	if (_AsyncTaskCompletedHandler != null)
            		Message.obtain(_AsyncTaskCompletedHandler, HANDLER_MESSAGE_Ws_Fct, result).sendToTarget();
            }
            
        }.execute(MAC, SerialNumber, AssemblyBooth);
	}

	/**
	 * Call the MII WS_NS_Atividade_Status web service.
	 * @param SerialNumber Serial number of the device.
	 * @return Return WS_NS_Atividade_Status_Result with the answer from MII. 
	 */
	public WS_NS_Atividade_Status_Result WS_NS_Atividade_Status(final String SerialNumber)
	{
		WS_NS_Atividade_Status_Result res = null;
		
		try
		{
            final SoapObject request = new SoapObject(NAMESPACE, "XacuteRequest");
			request.addProperty("LoginName", "WSAUTOMPLC");
			request.addProperty("LoginPassword","wsplc1");

            final SoapObject inputParams = new SoapObject(NAMESPACE, "InputParams");
			inputParams.addProperty("CD_NUMER_SERIE", SerialNumber);
			request.addSoapObject(inputParams);

            final SoapObject response;
			response = webServiceCall(request, true);
			if (response != null)
				res = new WS_NS_Atividade_Status_Result(response);
		}
		catch(final Exception ex)
		{
			Log.e(TAG, "Erro: Exception tratando MIIWebServices.WS_NS_Atividade_Status.", ex);
		}
		
		return res;
	}
	
	/**
	 * Call the MII WS_NS_Atividade_Status web service asynchronously. You must pass a android.os.Handler object in the
	 * object constructor to receive a message when the async call finishes.
	 * The android.os.Message.what will be set to HANDLER_MESSAGE_WS_NS_Atividade_Status and obj will be set to an instance of WS_NS_Atividade_Status_Result.
	 * MUST be called on an UI thread!
	 * @param SerialNumber Serial number of the device.
	 *
	 **/
	public void WS_NS_Atividade_Status_Async(final String SerialNumber)
	{
		new AsyncTask<String, Void, WS_NS_Atividade_Status_Result> ()
        {
            @SuppressLint("NewApi")
			protected WS_NS_Atividade_Status_Result doInBackground(String ... input)
            {
               	return WS_NS_Atividade_Status(input[0]);
            }

            protected void onPostExecute(final WS_NS_Atividade_Status_Result result)
            {
            	if (_AsyncTaskCompletedHandler != null)
            		Message.obtain(_AsyncTaskCompletedHandler, HANDLER_MESSAGE_WS_NS_Atividade_Status, result).sendToTarget();
            }
            
        }.execute(SerialNumber);
	}
	
	static public enum WS_ActivityEvent
	{
		OPEN(2), CLOSE(3), SEND_REPAIR(7);
		private final int id;

		WS_ActivityEvent(int id) { this.id = id; }
	    public int getValue() { return id; }
	}
	
	/**
	 * Call the MII WS_Producao web service.
	 * @param SerialNumber Serial number of the device.
	 * @param WorkstationCode The workstation (station) code.
	 * @param Activity The desired MII activity (MONTAGEM, RUNIN, FINAL, DOWN_FINAL, ANTENA, CALIBRACAO, CFT, CORRENTE, GRAVA_IMEI, CHECK_IMEI, PSN, SWDL, CIT2)
	 * @param Event The desired event (OPEN, CLOSE, SEND_REPAIR).
	 * @return Return WS_NS_Atividade_Status_Result with the answer from MII. 
	 */
	public WS_Producao_Result WS_Producao(final String SerialNumber, final String WorkstationCode, final String Activity, final WS_ActivityEvent Event)
	{
		WS_Producao_Result res = null;
		
		try
		{
            final SoapObject request = new SoapObject(NAMESPACE, "XacuteRequest");
			request.addProperty("LoginName", "WSAUTOMPLC");
			request.addProperty("LoginPassword","wsplc1");

            final SoapObject inputParams = new SoapObject(NAMESPACE, "InputParams");
			inputParams.addProperty("CD_NUMER_SERIE", SerialNumber);
			inputParams.addProperty("CD_POSTO", WorkstationCode);
			inputParams.addProperty("LOGIN", "WSAUTOMPLC");
			inputParams.addProperty("SENHA", "wsplc1");
			inputParams.addProperty("EVENTO", Event.getValue());
			request.addSoapObject(inputParams);

            final SoapObject response;
			response = webServiceCall(request, true);
			if (response != null)
				res = new WS_Producao_Result(response);
		}
		catch(final Exception ex)
		{
			Log.e(TAG, "Erro: Exception tratando MIIWebServices.WS_Producao.", ex);
		}
		
		return res;
	}
	
	/**
	 * Call the MII WS_Producao web service asynchronously. You must pass a android.os.Handler object in the
	 * object constructor to receive a message when the async call finishes.
	 * The android.os.Message.what will be set to HANDLER_MESSAGE_WS_Producao and obj will be set to an instance of WS_Producao_Result.
	 * MUST be called on an UI thread!
	 * @param SerialNumber Serial number of the device.
	 * @param WorkstationCode The workstation (station) code.
	 * @param Activity The desired MII activity (MONTAGEM, RUNIN, FINAL, DOWN_FINAL, ANTENA, CALIBRACAO, CFT, CORRENTE, GRAVA_IMEI, CHECK_IMEI, PSN, SWDL, CIT2)
	 * @param Event The desired event (OPEN, CLOSE, SEND_REPAIR).
	 *
	 **/
	public void WS_Producao_Async(final String SerialNumber, final String WorkstationCode, final String Activity, final WS_ActivityEvent Event)
	{
		new AsyncTask<String, Void, WS_Producao_Result> ()
        {
            @SuppressLint("NewApi")
			protected WS_Producao_Result doInBackground(String ... input)
            {
               	return WS_Producao(input[0], input[1], input[2], WS_ActivityEvent.valueOf(input[3]));
            }

            protected void onPostExecute(final WS_Producao_Result result)
            {
            	if (_AsyncTaskCompletedHandler != null)
            		Message.obtain(_AsyncTaskCompletedHandler, HANDLER_MESSAGE_WS_Producao, result).sendToTarget();
            }
            
        }.execute(SerialNumber, WorkstationCode, Activity, Event.toString());
	}

	/**
	 * Open or close an activity at MII. Uses the WS_NS_Atividade_Status and Ws_Producao web services
	 * to perform the operation, so you do not need to use WS_NS_Atividade_Status and Ws_Producao by yourself.
	 * @param SerialNumber The product serial number.
	 * @param WorkstationCode The station code from which the activity belongs.
	 * @param Activity The activity name (normally for the test, the activity name is CIT2).
	 * @param Event The event to process. Open, Close or send to Repair.
	 * @return The object representing the result of the operation.
	 * @throws Exception
	 */
	public WS_NS_Atividade_Status_Result openCloseActivity(final String SerialNumber, final String WorkstationCode, final String Activity, final WS_ActivityEvent Event) throws Exception
	{
		final String bkp_Url = _WebServiceURL;
		_WebServiceURL = _WebServiceURL.replace("Ws_Producao", "WS_NS_Atividade_Status");
        final WS_NS_Atividade_Status_Result result = WS_NS_Atividade_Status(SerialNumber);
		_WebServiceURL = bkp_Url;

		if (result == null)
			throw new Exception("MII WS_NS_Atividade_Status retornou uma resposta vazia");
		
		if (!result.get_st_status().equals("S"))
			throw new Exception(result.get_tx_msg());
		
		if (Event == WS_ActivityEvent.OPEN)
		{
			// The activity is already started?
		    if (result.get_st_ativi().equals("I"))
		    {
		    	// No action is required when the equipment is at REPAIR.
                if (result.get_cd_ativi_sw_teste().equals("REPARO"))
                	return result;
                // Check if the started activity is the same as the one we need
                else if (!result.get_cd_ativi_sw_teste().equals(Activity))
                    throw new Exception ("Atividade (" + result.get_cd_ativi_sw_teste() + ") que já está aberta no MII é diferente da atividade desse posto ( " + Activity + ").\r\nMensagem extra do MII: " + result.get_tx_msg());
                // Activity is the same, but the station that opened is different from this one
                else if (!result.get_cd_posto().equals(WorkstationCode))
                    throw new Exception ("Atividade (" + result.get_cd_ativi_sw_teste() + ") que já está aberta foi iniciada no posto " + result.get_cd_posto() + "), e n�o nesse posto.\r\nnMensagem extra do MII: " + result.get_tx_msg());

                // The activity is already started and is the same as the one we want to open or is in REPARO
                return result;
            }
            else if (!result.get_st_ativi().equals("F"))
                throw new Exception ("MII retornou um estado de atividade inválido (" + result.get_st_ativi() + ").\r\nMensagem extra do MII: " + result.get_tx_msg());

            // If we reached this line, the activity must be opened
		}
		else if (Event == WS_ActivityEvent.CLOSE)
		{
			// No action is required when the equipment is at REPAIR.
            if (result.get_cd_ativi_sw_teste().equals("REPARO"))
            	return result;
            else if (result.get_st_ativi().equals("F"))
            {
                // The activity is already closed so it must be the same as the one we want to close if one activity was returned (not empty)
            	
            	// Check if the closed activity is the same as the one we want to close
                if (result.get_cd_ativi_sw_teste().length() > 0 && !result.get_cd_ativi_sw_teste().equals(Activity))
                    throw new Exception ("Atividade (" + result.get_cd_ativi_sw_teste() + ") já está finalizada no MII, porém é diferente da atividade que está sendo finalizada ("+ Activity + ") nesse posto.\r\nMensagem extra do MII: " + result.get_tx_msg());
            	// Check if the closed activity station is the same as the one we want to close
                else if (result.get_cd_posto().length() > 0 && !result.get_cd_posto().equals(WorkstationCode))
                  	throw new Exception ("Atividade (" + result.get_cd_ativi_sw_teste() + ") já está finalizada no MII no posto (" + result.get_cd_posto() + "), que é diferente desse posto.\r\nMensagem extra do MII: " + result.get_tx_msg());

            	// The activity is already closed and is the same as the one we want to close or is in REPARO
            	return result;
            }
            else if (!result.get_st_ativi().equals("I"))
                throw new Exception ("MII retornou um estado de atividade inválido (" + result.get_st_ativi() + ").\r\nMensagem extra do MII: " + result.get_tx_msg());
			// Check if the started activity is the same as the one we want to close
            else if (!result.get_cd_ativi_sw_teste().equals(Activity))
                throw new Exception ("Atividade (" + result.get_cd_ativi_sw_teste() + ") que está iniciada no MII é diferente da atividade ( " + Activity + ") que está sendo finalizada nesse posto.\r\nMensagem extra do MII: " + result.get_tx_msg());
			// Check if the started activity was started in the same station as ours
            else if (!result.get_cd_posto().equals(WorkstationCode))
				throw new Exception ("Atividade (" + result.get_cd_ativi_sw_teste() + ") que está iniciada no MII foi aberta no posto ( " + result.get_cd_posto() + ") que é diferente da atividade desse posto.\r\nMensagem extra do MII: " + result.get_tx_msg());

			// If reached this line, the activity must be closed
		}

        final WS_Producao_Result operationResult = WS_Producao(SerialNumber, WorkstationCode, Activity, Event);

		if (operationResult == null)
			throw new Exception("MII WS_Producao retornou uma resposta vazia");
		
		if (!operationResult.getCD_STATUS().equals("S"))
			throw new Exception(operationResult.getDS_MENSA());
		
		result._tx_msg = operationResult.getDS_MENSA();
		return result;
	}

    /**
     * Call the MII WS_Get_Componentes_Seriados web service.
     * @param SerialNumber Serial number of the device.
     * @return Return WS_Get_Componentes_Seriados_Result with the answer from MII.
     */
    public WS_Get_Componentes_Seriados_Result WS_Get_Componentes_Seriados(final String SerialNumber)
    {
        WS_Get_Componentes_Seriados_Result res = null;

        try
        {
            final SoapObject request = new SoapObject(NAMESPACE, "XacuteRequest");
            request.addProperty("LoginName", "WSAUTOMPLC");
            request.addProperty("LoginPassword","wsplc1");

            final SoapObject inputParams = new SoapObject(NAMESPACE, "InputParams");
            inputParams.addProperty("CD_NUMER_SERIE", SerialNumber);
            request.addSoapObject(inputParams);

            final SoapObject response;
            response = webServiceCall(request, true);
            if (response != null)
                res = new WS_Get_Componentes_Seriados_Result(response);
        }
        catch(final Exception ex)
        {
            Log.e(TAG, "Erro: Exception tratando MIIWebServices.WS_Get_Componentes_Seriados.", ex);
        }

        return res;
    }
}
