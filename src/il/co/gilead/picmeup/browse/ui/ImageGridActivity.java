/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package il.co.gilead.picmeup.browse.ui;

import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import il.co.gilead.picmeup.GetHTTPResponse;
import il.co.gilead.picmeup.browse.util.Utils;
import il.co.gilead.picmeup.BuildConfig;
import il.co.gilead.picmeup.R;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

/**
 * Simple FragmentActivity to hold the main {@link ImageGridFragment} and not much else.
 */
public class ImageGridActivity extends FragmentActivity {
	private static final String TAG = "ImageGridActivity";
	private String fileListResponse;
	public static String[] imageUrls, ratings;
	private Integer category;
	private String url;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		String pictureIds = "";
		String ratingsStr = "";
		
		if (BuildConfig.DEBUG) {
			Utils.enableStrictMode();
		}
		super.onCreate(savedInstanceState);
		category = getIntent().getIntExtra("category", 0);
		if (category == 0)
			url=getString(R.string.baseServerName)+"browse_pictures.php";
		else
			url=getString(R.string.baseServerName)+"browse_pictures.php?category="+category;

		try {
			fileListResponse = (String) new GetHTTPResponse(getApplicationContext())
			.execute(url).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		if (fileListResponse == null || fileListResponse == "" || 
				fileListResponse.startsWith("Exception") ||
				fileListResponse.startsWith("{\"pictures\":[]}")){
			imageUrls = "1\n".split("\n");
			ratings = "5.0000\n".split("\n");
		}else
			try {
				JSONArray jsonResponse = new JSONArray(fileListResponse);
				for (int i=0; i<jsonResponse.length(); i++){
					JSONObject picture = jsonResponse.getJSONObject(i);
					pictureIds += picture.getString("id") + ",";
					ratingsStr += picture.getString("avg_rating") + ",";
				}
				imageUrls = pictureIds.split(",");
				ratings = ratingsStr.split(",");
			} catch (JSONException e) {
				e.printStackTrace();
			}

		if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(android.R.id.content, new ImageGridFragment(), TAG);
			ft.commit();
		}
	}
}
