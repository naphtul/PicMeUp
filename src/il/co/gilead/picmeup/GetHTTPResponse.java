package il.co.gilead.picmeup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class GetHTTPResponse extends AsyncTask<String, Void, String>{
	private Context context;
	public GetHTTPResponse(Context context) {
		this.context = context;
	}

	// check Internet connection.
	private void checkInternetConenction(){
		ConnectivityManager check = (ConnectivityManager) this.context.
				getSystemService(Context.CONNECTIVITY_SERVICE);
		if (check != null) 
		{
			NetworkInfo[] info = check.getAllNetworkInfo();
			if (info != null) 
				for (int i = 0; i <info.length; i++) 
					if (info[i].getState() == NetworkInfo.State.CONNECTED)
					{
					}
		}else{
			Toast.makeText(context, "not conencted to internet",Toast.LENGTH_SHORT).show();
		}
	}
	protected void onPreExecute(){
		checkInternetConenction();
	}
	protected String doInBackground(String... arg0) {
		try{
			String link = (String)arg0[0];
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.setRequestProperty("userid", MainActivity.userid);
			conn.setRequestProperty("sso", MainActivity.sso);
			conn.setDoInput(true);
			conn.connect();
			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader
					(is, "UTF-8") );
			String data = null;
			String response = "";
			while ((data = reader.readLine()) != null){
				response += data + "\n";
			}
			return response;
		}catch(Exception e){
			return new String("Exception: " + e.getMessage());
		}
	}
	protected void onPostExecute(String result){
	}
	

	public static String POST(String url, String ppResponse){
		InputStream inputStream = null;
		String result = "";
			try {
				
				// 1. create HttpClient
				HttpClient httpclient = new DefaultHttpClient();
				
				// 2. make POST request to the given URL
				HttpPost httpPost = new HttpPost(url);
				
				// 5. set json to StringEntity
				StringEntity se = new StringEntity(ppResponse);
				
				// 6. set httpPost Entity
				httpPost.setEntity(se);
				
				// 7. Set some headers to inform server about the type of the content   
				httpPost.setHeader("Accept", "application/json");
				httpPost.setHeader("Content-type", "application/json");
				httpPost.setHeader("userid", MainActivity.userid);
				httpPost.setHeader("sso", MainActivity.sso);
				
				// 8. Execute POST request to the given URL
				HttpResponse httpResponse = httpclient.execute(httpPost);
				
				// 9. receive response as inputStream
				inputStream = httpResponse.getEntity().getContent();
				
				// 10. convert inputstream to string
				if(inputStream != null)
					result = convertInputStreamToString(inputStream);
				else
					result = "Did not work!";
				
			} catch (Exception e) {
				Log.d("InputStream", e.getLocalizedMessage());
			}

			// 11. return result
			return result;
		}

	private static String convertInputStreamToString(InputStream inputStream) throws IOException {
		BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
		String line = "";
		String result = "";
		while((line = bufferedReader.readLine()) != null)
			result += line;

		inputStream.close();
		return result;
	}

}

