package il.co.gilead.picmeup;

import il.co.gilead.picmeup.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BuyPicture extends Activity {
	private String confirmation = null;
	private String pictureid, provider = "";
    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
    private ProgressDialog mProgressDialog;
    private Boolean firstRun = true;
    private Boolean mProgressState = false;
    private Button btnDownload;
    private TextView tvPayConf;

	private static PayPalConfiguration config = new PayPalConfiguration()

    // Start with mock environment.  When ready, switch to sandbox (ENVIRONMENT_SANDBOX)
    // or live (ENVIRONMENT_PRODUCTION)
    .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)

//    .clientId("APP-80W284485P519543T");
    .clientId("Adu0GxDNy5r0bdJgKMaCH1LSm7g_KOYzrW0XM4ITd8Na6YA0Nln_9hG4J-vv");

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_buy_picture);
		lockScreenOrientation();
		btnDownload = (Button) findViewById(R.id.btnDownload);
		tvPayConf = (TextView) findViewById(R.id.paymentConf);
		tvPayConf.setText(R.string.click_back);
		if (savedInstanceState != null){
			firstRun = savedInstanceState.getBoolean("firstRun");
			mProgressState = savedInstanceState.getBoolean("progressState");
		}
		if (firstRun) {
			pictureid = getIntent().getStringExtra("pictureid");
			provider = getIntent().getStringExtra("provider");
		    Intent intent = new Intent(this, PayPalService.class);

		    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);

		    startService(intent);
		    onBuyPressed(getCurrentFocus());
		    firstRun = false;
		}
		if (!mProgressState && mProgressDialog != null){
			mProgressDialog.dismiss();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.buy_picture, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
	    stopService(new Intent(this, PayPalService.class));
	    super.onDestroy();
		unlockScreenOrientation();
	}

	public void onBuyPressed(View pressed) {

	    // PAYMENT_INTENT_SALE will cause the payment to complete immediately.
	    // Change PAYMENT_INTENT_SALE to 
	    //   - PAYMENT_INTENT_AUTHORIZE to only authorize payment and capture funds later.
	    //   - PAYMENT_INTENT_ORDER to create a payment for authorization and capture
	    //     later via calls from your server.

	    PayPalPayment payment = new PayPalPayment(new BigDecimal("1.00"), "USD", "PicMeUp picture",
	            PayPalPayment.PAYMENT_INTENT_SALE);

	    Intent intent = new Intent(this, PaymentActivity.class);

	    intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

	    startActivityForResult(intent, 0);
	}
	
	public void onDownloadPressed(View pressed) {
		startDownload();
	}

    private void startDownload() {
        String url = "http://pmu-naphtul.rhcloud.com/retrieve_picture.php?file="+pictureid;
        new DownloadFileAsync().execute(url);
    }

class DownloadFileAsync extends AsyncTask<String, String, String> {
   
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (!mProgressState){
			mProgressDialog = new ProgressDialog(BuyPicture.this);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMessage(getString(R.string.downloading_picture));
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
			mProgressState = true;
		}
	}

	@Override
	protected String doInBackground(String... aurl) {
		int count;

	try {

	URL url = new URL(aurl[0]);
	HttpURLConnection uc = (HttpURLConnection) url.openConnection();
	uc.setRequestProperty("userid", MainActivity.userid);
	uc.setRequestProperty("sso", MainActivity.sso);
	uc.connect();

	int lenghtOfFile = uc.getContentLength();
	Log.d(getPackageName(), "Lenght of file: " + lenghtOfFile);

	InputStream input = uc.getInputStream();
    //set the path where we want to save the file
    //in this case, going to save it on the root directory of the
    //sd card.
    String SDCardRoot = Environment.getExternalStorageDirectory() + "/Pictures/PicMeUp/";
    //create a new file, specifying the path, and the filename
    //which we want to save the file as.
    File file = new File(SDCardRoot+"PicMeUp_"+System.currentTimeMillis()+".jpg");
	OutputStream output = new FileOutputStream(file);

	byte data[] = new byte[1024];

	long total = 0;

		while ((count = input.read(data)) != -1) {
			total += count;
			publishProgress(""+(int)((total*100)/lenghtOfFile));
			output.write(data, 0, count);
		}

		output.flush();
		output.close();
		input.close();
	} catch (Exception e) {}
	return null;

	}
	protected void onProgressUpdate(String... progress) {
		 Log.d(getPackageName(), progress[0]);
		 mProgressDialog.setProgress(Integer.parseInt(progress[0]));
	}

	@Override
	protected void onPostExecute(String unused) {
		mProgressDialog.dismiss();
		mProgressState = false;
	}
}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
	    if (resultCode == Activity.RESULT_OK) {
	        PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
	        if (confirm != null) {
	            try {
	                Log.i(getPackageName(), confirm.toJSONObject().toString(4));

	                // TODO: send 'confirm' to your server for verification.
	                // TODO: Encode all the params to pass together (pictureid, confirmation, provider).
	                // see https://developer.paypal.com/webapps/developer/docs/integration/mobile/verify-mobile-payment/
	                // for more details.
	                confirmation = confirm.toJSONObject().toString(4);
	                new HttpAsyncTask().execute("http://pmu-naphtul.rhcloud.com/buy_picture.php?picture_id="+
	                		pictureid+"&provider="+provider);

	            } catch (JSONException e) {
	                Log.e(getPackageName(), "an extremely unlikely failure occurred: ", e);
	            }
	        }
	    }
	    else if (resultCode == Activity.RESULT_CANCELED) {
	        Log.i(getPackageName(), "The user canceled.");
	    }
	    else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
	        Log.i(getPackageName(), "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
	    }
	}
	
	private class HttpAsyncTask extends AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute(){
			tvPayConf.setText(R.string.waiting_for_payment);
		}
		@Override
		protected String doInBackground(String... urls) {
			return GetHTTPResponse.POST(urls[0], confirmation);
		}
		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String result) {
			if (result.equals(confirmation.replaceAll("\n", ""))){
				tvPayConf.setText(R.string.payment_confirmed);
				btnDownload.setVisibility(View.VISIBLE);
			}else{
				tvPayConf.setText(R.string.payment_not_confirmed);
				btnDownload.setEnabled(false);
				btnDownload.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void lockScreenOrientation() {
	    int currentOrientation = getResources().getConfiguration().orientation;
	    if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    } else {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	    }
	}
	 
	private void unlockScreenOrientation() {
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		savedState.putBoolean("firstRun", firstRun);
		savedState.putBoolean("progressState", mProgressState);
		savedState.putString("pictureid", pictureid);
		savedState.putString("provider", provider);
		savedState.putInt("dnldBtnState", btnDownload.getVisibility());
	}
	
	@Override
	protected void onRestoreInstanceState (Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		firstRun = savedInstanceState.getBoolean("firstRun");
		mProgressState = savedInstanceState.getBoolean("progressState");
		pictureid = savedInstanceState.getString("pictureid");
		provider = savedInstanceState.getString("provider");
		btnDownload.setVisibility(savedInstanceState.getInt("dnldBtnState"));
	}
}
