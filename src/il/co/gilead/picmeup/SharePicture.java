package il.co.gilead.picmeup;

import il.co.gilead.picmeup.R;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.kbeanie.imagechooser.api.ChooserType;
import com.kbeanie.imagechooser.api.ChosenImage;
import com.kbeanie.imagechooser.api.ImageChooserListener;
import com.kbeanie.imagechooser.api.ImageChooserManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SharePicture extends Activity implements ImageChooserListener {
	private static final int REQUEST_TAKE_PICTURE = 1;
	private TextView messageText;
	private Button uploadButton, selectImageButton;
	int serverResponseCode = 0;
	ProgressDialog dialog = null;
	File file;
	String mCurrentPhotoPath;

	String upLoadServerUri = null;

	private ImageChooserManager imageChooserManager;
	private int chooserType;

	/**********  File Path *************/
	String uploadFilePath;
	String uploadFileName;
	public String filePath;
	private Spinner categoriesSpinner;
	Integer category;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload_to_server);

		uploadButton = (Button)findViewById(R.id.uploadButton);
		selectImageButton = (Button)findViewById(R.id.selectImageButton);
		messageText  = (TextView)findViewById(R.id.messageText);
		categoriesSpinner = (Spinner) findViewById(R.id.categories_spinner);
		
		// PHP script path
		upLoadServerUri = getString(R.string.baseServerName)+"share_picture.php";

		selectImageButton.setOnClickListener (new OnClickListener() {

			@Override
			public void onClick(View v) {
				browseForImage();
			}
		});

		uploadButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (filePath == null){
					Log.e("uploadFile", "No valid picture was selected.");
					runOnUiThread(new Runnable() {
						public void run() {
							messageText.setText("No valid picture was selected.");
						}
					});
					return;
				}
				dialog = ProgressDialog.show(SharePicture.this, "", "Sharing picture...", true);

				new Thread(new Runnable() {
					public void run() {
						runOnUiThread(new Runnable() {
							public void run() {
								messageText.setText(" Sharing started...");
							}
						});

						uploadFile(filePath);

					}
				}).start();
			}
		});
	}

	private void browseForImage() {
		chooserType = ChooserType.REQUEST_PICK_PICTURE;
		imageChooserManager = new ImageChooserManager(this, ChooserType.REQUEST_PICK_PICTURE, false);
		imageChooserManager.setImageChooserListener(this);
		try {
			filePath = imageChooserManager.choose();
			messageText.setText(filePath);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public int uploadFile(String sourceFileUri) {
		if (sourceFileUri == null){
			Log.e("uploadFile", "Source picture doesn't exist.");
			runOnUiThread(new Runnable() {
				public void run() {
					messageText.setText("Source picture doesn't exist.");
				}
			});
			return 0;
		}

		if (categoriesSpinner.getSelectedItem() != null)
			category = categoriesSpinner.getSelectedItemPosition()+1;
		else
			category = 7;

		String fileName = sourceFileUri;

		HttpURLConnection conn = null;
		DataOutputStream dos = null;  
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024; 
		File sourceFile = new File(sourceFileUri); 

		if (!sourceFile.isFile()) {

			dialog.dismiss(); 

			Log.e("uploadFile", "Source picture doesn't exist: "
					+uploadFilePath + "" + uploadFileName);

			runOnUiThread(new Runnable() {
				public void run() {
					messageText.setText("Source picture doesn't exist: "
							+uploadFilePath + "" + uploadFileName);
				}
			});

			return 0;

		}
		else
		{
			try { 

				// open a URL connection to the Servlet
				FileInputStream fileInputStream = new FileInputStream(sourceFile);
				URL url = new URL(upLoadServerUri);

				// Open a HTTP  connection to  the URL
				conn = (HttpURLConnection) url.openConnection(); 
				conn.setDoInput(true); // Allow Inputs
				conn.setDoOutput(true); // Allow Outputs
				conn.setUseCaches(false); // Don't use a Cached Copy
				conn.setRequestMethod("POST");
				conn.setRequestProperty("userid", MainActivity.userid);
				conn.setRequestProperty("sso", MainActivity.sso);
				conn.setRequestProperty("category", category.toString());
				conn.setRequestProperty("Connection", "Keep-Alive");
				conn.setRequestProperty("ENCTYPE", "multipart/form-data");
				conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
				conn.setRequestProperty("uploaded_file", fileName); 

				dos = new DataOutputStream(conn.getOutputStream());

				dos.writeBytes(twoHyphens + boundary + lineEnd); 
				dos.writeBytes("Content-Disposition: form-data; name=\"45879396-e4fe-4d60-9420-1a7f1c19c1e5\";filename=\""
						+ fileName + "\"" + lineEnd);

				dos.writeBytes(lineEnd);

				// create a buffer of  maximum size
				bytesAvailable = fileInputStream.available(); 

				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];

				// read file and write it into form...
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);  

				while (bytesRead > 0) {

					dos.write(buffer, 0, bufferSize);
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);   

				}

				// send multipart form data necessary after file data...
				dos.writeBytes(lineEnd);
				dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

				// Responses from the server (code and message)
				serverResponseCode = conn.getResponseCode();
				String serverResponseMessage = conn.getResponseMessage();

				Log.i("uploadFile", "HTTP Response is : " 
						+ serverResponseMessage + ": " + serverResponseCode);

				if(serverResponseCode == 200 && conn.getContentLength() == 0){

					runOnUiThread(new Runnable() {
						public void run() {

							messageText.setText("Picture sharing complete.");
						}
					});                
				}

				//close the streams //
				fileInputStream.close();
				dos.flush();
				dos.close();

			} catch (MalformedURLException ex) {

				dialog.dismiss();  
				ex.printStackTrace();

				runOnUiThread(new Runnable() {
					public void run() {
						messageText.setText("MalformedURLException Exception : check script url.");
						Toast.makeText(SharePicture.this, "MalformedURLException", 
								Toast.LENGTH_SHORT).show();
					}
				});

				Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
			} catch (Exception e) {

				dialog.dismiss();  
				e.printStackTrace();

				runOnUiThread(new Runnable() {
					public void run() {
						messageText.setText("Got Exception : see logcat ");
						Toast.makeText(SharePicture.this, "Got Exception : see logcat ", 
								Toast.LENGTH_SHORT).show();
					}
				});
				Log.e("Upload file to server Exception", "Exception : " 
						+ e.getMessage(), e);  
			}
			dialog.dismiss();       
			return serverResponseCode; 

		} // End else block 
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != MainActivity.RESULT_OK) return;
		switch (requestCode) {
		case ChooserType.REQUEST_PICK_PICTURE:
			if (imageChooserManager == null) {
				reinitializeImageChooser();
			}
			imageChooserManager.submit(requestCode, data);
			break;
		case REQUEST_TAKE_PICTURE:
			filePath = mCurrentPhotoPath;
			break;
		}
	}

	private File createImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
	    String imageFileName = getString(R.string.album_name)+"_" + timeStamp + "_";
	    File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
	            + "/Pictures/"+getString(R.string.album_name)+"/");
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );

	    mCurrentPhotoPath = image.getAbsolutePath();
	    return image;
	}

	public void dispatchTakePictureIntent(View v) {
	    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    // Ensure that there's a camera activity to handle the intent
	    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
	        // Create the File where the photo should go
	        File photoFile = null;
	        try {
	            photoFile = createImageFile();
	        } catch (IOException ex) {
	            // Error occurred while creating the File
	        }
	        // Continue only if the File was successfully created
	        if (photoFile != null) {
	            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
	                    Uri.fromFile(photoFile));
	            startActivityForResult(takePictureIntent, REQUEST_TAKE_PICTURE);
	        }
	    }
	}
	
	@Override
	public void onImageChosen(final ChosenImage image) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (image != null) {
					filePath = image.getFilePathOriginal();
					uploadFileName = filePath.substring(filePath.lastIndexOf("/")+1);
					uploadFilePath = filePath.substring(0, filePath.lastIndexOf("/")+1);
					messageText.setText("Picture was selected for sharing: "+uploadFileName);
				}
			}
		});

	}

	@Override
	public void onError(String reason) {
	}

	// Should be called if for some reason the ImageChooserManager is null (Due
	// to destroying of activity for low memory situations)
	private void reinitializeImageChooser() {
		imageChooserManager = new ImageChooserManager(this, chooserType, false);
		imageChooserManager.setImageChooserListener(this);
		imageChooserManager.reinitialize(filePath);
	}

}