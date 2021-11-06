package il.co.gilead.picmeup;

import il.co.gilead.picmeup.browse.ui.ImageGridActivity;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.squareup.picasso.Picasso;

public class MainActivity extends Activity implements OnClickListener,
ConnectionCallbacks, OnConnectionFailedListener {

	private LoginButton loginBtn;
	private UiLifecycleHelper uiHelper;
	private static final List<String> PERMISSIONS = Arrays.asList("email");

	private static final int RC_SIGN_IN = 0;

	// Profile pic image size in pixels
	private static final int PROFILE_PIC_SIZE = 100;

	// Google client to interact with Google API
	private GoogleApiClient mGoogleApiClient;

	/**
	 * A flag indicating that a PendingIntent is in progress and prevents us
	 * from starting further intents.
	 */
	private boolean mIntentInProgress;

	private boolean mSignInClicked;

	private ConnectionResult mConnectionResult;

	private SignInButton btnSignIn;
	private Button btnSignOut, btnRevokeAccess, btnUploadToServer, btnBrowsePictures;
	private Spinner categories_spinner;
	private Integer category;
	private ImageView imgProfilePic;
	private TextView txtName, txtEmail;
	private LinearLayout llProfileLayout;
	public static String userid, sso;
	private String personPhotoUrl;
	
	private Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		uiHelper = new UiLifecycleHelper(this, statusCallback);
		uiHelper.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		getAlbumDir();
		
		// Google Sign In
		btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
		btnSignOut = (Button) findViewById(R.id.btn_sign_out);
		btnRevokeAccess = (Button) findViewById(R.id.btn_revoke_access);
		btnUploadToServer = (Button) findViewById(R.id.btn_upload_to_server);
		btnBrowsePictures = (Button) findViewById(R.id.btn_browse_pictures);
		categories_spinner = (Spinner) findViewById(R.id.categories_spinner);
		imgProfilePic = (ImageView) findViewById(R.id.imgProfilePic);
		txtEmail = (TextView) findViewById(R.id.txtEmail);
		txtName = (TextView) findViewById(R.id.txtName);
		llProfileLayout = (LinearLayout) findViewById(R.id.llProfile);

		// Facebook Login
		loginBtn = (LoginButton) findViewById(R.id.fb_login_button);
		loginBtn.setReadPermissions(PERMISSIONS);
		loginBtn.setUserInfoChangedCallback(new UserInfoChangedCallback() {
			@Override
			public void onUserInfoFetched(GraphUser user) {
				if (user != null) {
					llProfileLayout.setVisibility(View.VISIBLE);
					txtName.setText("Hello " + user.getName());
					Object email = user.getProperty("email");
					if (email != null){
						userid = email.toString();
						txtEmail.setText(userid);
						sso = "FB";
					}
					personPhotoUrl = "https://graph.facebook.com/"+ user.getId()+ "/picture?height=100&width=100";
					Picasso.with(getApplicationContext()).load(personPhotoUrl).into(imgProfilePic);
					btnSignIn.setVisibility(View.GONE);
					btnUploadToServer.setVisibility(View.VISIBLE);
					btnBrowsePictures.setVisibility(View.VISIBLE);
					categories_spinner.setVisibility(View.VISIBLE);
				} else {
					txtName.setText("You are not logged in to Facebook");
					btnSignIn.setVisibility(View.VISIBLE);
				}
			}
		});

		// Button click listeners
		btnSignIn.setOnClickListener(this);
		btnSignOut.setOnClickListener(this);
		btnRevokeAccess.setOnClickListener(this);
		btnUploadToServer.setOnClickListener(this);
		btnBrowsePictures.setOnClickListener(this);

		mGoogleApiClient = new GoogleApiClient.Builder(this)
		.addConnectionCallbacks(this)
		.addOnConnectionFailedListener(this).addApi(Plus.API)
		.addScope(Plus.SCOPE_PLUS_LOGIN).build();
	}

	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	protected void onStop() {
		super.onStop();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	/**
	  Method to resolve any signin errors
	  */
	private void resolveSignInError() {
		if (mConnectionResult.hasResolution()) {
			try {
				mIntentInProgress = true;
				mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
			} catch (SendIntentException e) {
				mIntentInProgress = false;
				mGoogleApiClient.connect();
			}
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (!result.hasResolution()) {
			GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
					0).show();
			return;
		}

		if (!mIntentInProgress) {
			// Store the ConnectionResult for later usage
			mConnectionResult = result;

			if (mSignInClicked) {
				// The user has already clicked 'sign-in' so we attempt to
				// resolve all
				// errors until the user is signed in, or they cancel.
				resolveSignInError();
			}
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		super.onActivityResult(requestCode, responseCode, intent);
		uiHelper.onActivityResult(requestCode, responseCode, intent);

		if (requestCode == RC_SIGN_IN) {
			if (responseCode != RESULT_OK) {
				mSignInClicked = false;
			}

			mIntentInProgress = false;

			if (!mGoogleApiClient.isConnecting()) {
				mGoogleApiClient.connect();
			}
		}
	}

	@Override
	public void onConnected(Bundle arg0) {
		mSignInClicked = false;

		// Get user's information
		getProfileInformation();

		// Update the UI after signin
		updateUI(true);
	}

	/**
	 * Updating the UI, showing/hiding buttons and profile layout
	 * */
	private void updateUI(boolean isSignedIn) {
		if (isSignedIn) {
			btnSignIn.setVisibility(View.GONE);
			btnSignOut.setVisibility(View.VISIBLE);
			btnRevokeAccess.setVisibility(View.VISIBLE);
			btnUploadToServer.setVisibility(View.VISIBLE);
			btnBrowsePictures.setVisibility(View.VISIBLE);
			categories_spinner.setVisibility(View.VISIBLE);
			llProfileLayout.setVisibility(View.VISIBLE);
			loginBtn.setVisibility(View.GONE);
		} else {
			btnSignIn.setVisibility(View.VISIBLE);
			btnSignOut.setVisibility(View.GONE);
			btnRevokeAccess.setVisibility(View.GONE);
			btnUploadToServer.setVisibility(View.GONE);
			btnBrowsePictures.setVisibility(View.GONE);
			categories_spinner.setVisibility(View.GONE);
			llProfileLayout.setVisibility(View.GONE);
			loginBtn.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Fetching user's information name, email, profile pic
	 * */
	private void getProfileInformation() {
		try {
			if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
				Person currentPerson = Plus.PeopleApi
						.getCurrentPerson(mGoogleApiClient);
				String personName = currentPerson.getDisplayName();
				personPhotoUrl = currentPerson.getImage().getUrl();
				String personGooglePlusProfile = currentPerson.getUrl();
				String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
				sso = "G+";
				userid = email;

				Log.d(getString(R.string.app_name), "Name: " + personName + ", plusProfile: "
						+ personGooglePlusProfile + ", email: " + email
						+ ", Image: " + personPhotoUrl);

				txtName.setText(personName);
				txtEmail.setText(email);

				// by default the profile url gives 50x50 px image only
				// we can replace the value with whatever dimension we want by
				// replacing sz=X
				personPhotoUrl = personPhotoUrl.substring(0,
						personPhotoUrl.length() - 2)
						+ PROFILE_PIC_SIZE;
				Log.d(getString(R.string.app_name), "personPhotoUrl: "+personPhotoUrl);
				Picasso.with(getApplicationContext()).load(personPhotoUrl).into(imgProfilePic);
			} else {
				Toast.makeText(getApplicationContext(),
						"Person information is null", Toast.LENGTH_LONG).show();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
		updateUI(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Button on click listener
	 * */
	@Override
	public void onClick(View v) {
		if (categories_spinner.getSelectedItem() != null){
			category = categories_spinner.getSelectedItemPosition()+1;
		}else{
			category = 7;
		}
		switch (v.getId()) {
		case R.id.btn_sign_in:
			// Signin button clicked
			signInWithGplus();
			break;
		case R.id.btn_sign_out:
			// Signout button clicked
			signOutFromGplus();
			break;
		case R.id.btn_revoke_access:
			// Revoke access button clicked
			revokeGplusAccess();
			break;
		case R.id.btn_upload_to_server:
			// Share Pictures button clicked
			intent = new Intent(this, SharePicture.class);
			intent.putExtra("category", category);
			startActivity(intent);
			break;
		case R.id.btn_browse_pictures:
			// Browse Pictures button clicked
			intent = new Intent(this, ImageGridActivity.class);
			intent.putExtra("category", category);
			startActivity(intent);
			break;
		}
	}

	/**
	 * Sign-in into google
	 * */
	private void signInWithGplus() {
		if (!mGoogleApiClient.isConnecting()) {
			mSignInClicked = true;
			resolveSignInError();
		}
	}

	/**
	 * Sign-out from google
	 * */
	private void signOutFromGplus() {
		if (mGoogleApiClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			mGoogleApiClient.disconnect();
			mGoogleApiClient.connect();
			updateUI(false);
			userid = "";
			sso = "";
			imgProfilePic.setImageResource(android.R.color.transparent);
		}
	}

	/**
	 * Revoking access from google
	 * */
	private void revokeGplusAccess() {
		if (mGoogleApiClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
			.setResultCallback(new ResultCallback<Status>() {
				@Override
				public void onResult(Status arg0) {
					Log.d(getString(R.string.app_name), "User access revoked!");
					mGoogleApiClient.connect();
					updateUI(false);
				}

			});
		}
	}

	private Session.StatusCallback statusCallback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			if (state.isOpened()) {
				Log.d(getString(R.string.app_name), "Facebook session opened");
			} else if (state.isClosed()) {
				Log.d(getString(R.string.app_name), "Facebook session closed");
				txtName.setText("");
				llProfileLayout.setVisibility(View.GONE);
				btnUploadToServer.setVisibility(View.GONE);
				btnBrowsePictures.setVisibility(View.GONE);
				categories_spinner.setVisibility(View.GONE);
				userid = "";
				sso = "";
			}
		}
	};
	
	private File getAlbumDir() {
		File storageDir = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/Pictures/"+getString(R.string.album_name)+"/");
			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d(getString(R.string.app_name), "failed to create directory");
						return getFilesDir();
					}
				}
			}
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		return storageDir;
	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle savedState) {
		super.onSaveInstanceState(savedState);
		uiHelper.onSaveInstanceState(savedState);
	}
}