package br.com.positivo.framework;

import java.util.ArrayList;
import java.util.Date;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

/**
 * Class that implement the access to the Tests Statistics web service.
 * @author Leandro G. B. Becker, Carlos Simões Pelegrin
 */
@SuppressLint("SimpleDateFormat")
class TestsStatisticsWebService
{
	private String _WebServiceURL; 
	
	private static final String TAG = "StatisticsWebService";
	public static final String NAMESPACE = "http://www.positivoinformatica.com.br/Tests/TestStatistics";
	public static final String SOAP_ACTION = "http://www.positivoinformatica.com.br/Tests/TestStatistics/LogTestResults";
	
	public static final int HANDLER_MESSAGE_WS_SUITE_SOFTWARE = 10;
	public static final int HANDLER_MESSAGE_Ws_AutomPLC = 11; 
	public static final int HANDLER_MESSAGE_GET_IMEI_MAC = 12;
	public static final int HANDLER_MESSAGE_Ws_Fct = 13;

    final android.os.Handler _AsyncTaskCompletedHandler;
	
	/** Constructs the MII web service proxy object.
	 * @param WebServiceURL The URL for the desired web service.
	 */
	public TestsStatisticsWebService(final String WebServiceURL)
	{
        _AsyncTaskCompletedHandler = null;
        _WebServiceURL = WebServiceURL;
	} 
	
	/**
	 * Construct an object to use the async versions of the calls.
	 * @param WebServiceURL The URL for the desired web service.
	 * @param AsyncTaskCompletedHandler An android.os.Handler object to receive a message when the asynchronous call finishes.
	 */
	public TestsStatisticsWebService(final String WebServiceURL, final android.os.Handler AsyncTaskCompletedHandler)
	{ 
		_WebServiceURL = WebServiceURL;
		_AsyncTaskCompletedHandler = AsyncTaskCompletedHandler;
	}
	
	public void setWebServiceURL(final String url) { _WebServiceURL = url; }
	
	/**
	 * Calls the web service.
	 * @param request The SOAP request object.
	 * @param imp_types True to use implicit types in the SOAP xml.
	 * @return Return the server date.
	 */
	private String webServiceCall(final SoapObject request, final boolean imp_types) throws Exception
	{
        final SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
		
		envelope.implicitTypes = imp_types;
		envelope.dotNet = true;
		envelope.setAddAdornments(false);
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
				if (response.hasProperty("LogTestResultsResult"))
				{
                    final String date = response.getPropertyAsString("LogTestResultsResult");
					return date;
				}
				else
				{
					throw new RuntimeException("Invalid web service response. No LogTestResultsResult string was found.");
				}
			}
			catch (final Exception ex)
			{
				if (attempt > 1)
				{
					final StringBuilder strBuilder = new StringBuilder(4096);
					strBuilder.append("Erro: Exception executando TestsStatisticsWebService.webServiceCall(").
							append(_WebServiceURL).
							append(")\n").
							append("Request XML:\n").
							append(androidHttpTransport.requestDump).
							append("Response XML:\n").
							append(androidHttpTransport.responseDump);
					Log.e(TAG, strBuilder.toString(), ex);

					throw ex;
				}
			}
		}
		
		return null;
	}

	public String logTestResults(final MotherboardInfo info, final ArrayList<UnitTest> tests, final boolean useTestServer)
	{
        String response;
        try
		{
            final SoapObject motherboardInfo = new SoapObject(NAMESPACE, "motherboard");

            if (info.MAC.compareTo("02:00:00:00:00:00") != 0)
			    motherboardInfo.addProperty("MAC", info.MAC);

			motherboardInfo.addProperty("SerialNumber", info.SerialNumber);
			motherboardInfo.addProperty("UUID", info.UUID);
			motherboardInfo.addProperty("Model", info.Model);
			motherboardInfo.addProperty("TestPhase", info.TestPhase);
			motherboardInfo.addProperty("AssemblyBooth", TestsOrchestrator.getGlobalTestsConfiguration().PLC);
			motherboardInfo.addProperty("Passed", info.Passed);
			motherboardInfo.addProperty("TotalTestDurationInSecs", info.TotalTestDurationInSecs);
			motherboardInfo.addProperty("SentToRepair", info.SentToRepair);
			motherboardInfo.addProperty("BIOSVer", Build.DISPLAY);

            final SoapObject arrayOfUnitTestResults = new SoapObject(NAMESPACE, "UnitTests");
			
			for (final UnitTest test : tests)
			{
				if (!test.isTestFinished())
					continue;

                final SoapObject unitTestResult = new SoapObject (NAMESPACE, "UnitTestResult");
				unitTestResult.addProperty("TestIdGUID", test.getUUID());
				unitTestResult.addProperty("TestName", test.getName());
				unitTestResult.addProperty("Fails", test.frameworkOnlyIsTestSucceeded() ? 0 : 1);
				unitTestResult.addProperty("Repetions", test._currentAttempt);
				unitTestResult.addProperty("Passed", test.frameworkOnlyIsTestSucceeded());
				
				if (test.getTestFinishTime() == null)
					test.setTestFinishTimeToNow();
				
				unitTestResult.addProperty("TestDurationInSecs", (int)((test.getTestFinishTime().getTime() - test.getTestStartTime().getTime()) / 1000));
				arrayOfUnitTestResults.addSoapObject(unitTestResult);
			}

            final SoapObject request = new SoapObject(NAMESPACE, "LogTestResults");
			request.addSoapObject(motherboardInfo);
			request.addSoapObject(arrayOfUnitTestResults);
			request.addProperty("useTestDatabaseServer", useTestServer);

            response = webServiceCall(request, true);
		}
		catch(final Exception ex)
		{
			Log.e(TAG, "Erro: Exception tratando TestsStatisticsWebService.logTestResults.", ex);
            response = ex.getMessage();
		}
		
		return response;
	}
}
