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

import il.co.gilead.picmeup.BuyPicture;
import il.co.gilead.picmeup.GetHTTPResponse;
import il.co.gilead.picmeup.browse.util.ImageFetcher;
import il.co.gilead.picmeup.browse.util.ImageWorker;
import il.co.gilead.picmeup.browse.util.Utils;
import il.co.gilead.picmeup.R;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This fragment will populate the children of the ViewPager from {@link ImageDetailActivity}.
 */
public class ImageDetailFragment extends Fragment {
    private static final String IMAGE_DATA_EXTRA = "extra_image_data";
    private String mImageUrl, mRating;
    private String baseUrl = "http://pmu-naphtul.rhcloud.com/retrieve_picture.php?dimensions=1024&file=";
    private ImageView mImageView;
    private ImageFetcher mImageFetcher;
    private Context context;
    private TextView tv;

    /**
     * Factory method to generate a new instance of the fragment given an image number.
     *
     * @param imageUrl The image url to load
     * @param ratings 
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static ImageDetailFragment newInstance(String imageUrl, String rating) {
        final ImageDetailFragment f = new ImageDetailFragment();

        final Bundle args = new Bundle();
        args.putString(IMAGE_DATA_EXTRA, imageUrl);
        args.putString("IMAGE_RATING", rating);
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageDetailFragment() {}

    /**
     * Populate image using a url from extras, use the convenience factory method
     * {@link ImageDetailFragment#newInstance(String)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments() != null ? getArguments().getString(IMAGE_DATA_EXTRA) : null;
        mRating = getArguments() != null ? getArguments().getString("IMAGE_RATING") : null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        hasOptionsMenu();
        // Inflate and locate the main ImageView
        final View v = inflater.inflate(R.layout.image_detail_fragment, container, false);
        context = v.getContext();
        tv = (TextView) v.findViewById(R.id.pictureDetails);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        registerForContextMenu(mImageView);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the ImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (ImageDetailActivity.class.isInstance(getActivity())) {
            mImageFetcher = ((ImageDetailActivity) getActivity()).getImageFetcher();
            mImageFetcher.loadImage(baseUrl+mImageUrl, mImageView);
            tv.setText("Average Rating: "+mRating);
        }

        // Pass clicks on the ImageView to the parent activity to handle
        if (OnClickListener.class.isInstance(getActivity()) && Utils.hasHoneycomb()) {
            mImageView.setOnClickListener((OnClickListener) getActivity());
        }
    }

	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(R.string.rate);
		menu.add(1, Menu.NONE, 1, "*");
		menu.add(1, Menu.NONE, 2, "**");
		menu.add(1, Menu.NONE, 3, "***");
		menu.add(1, Menu.NONE, 4, "****");
		menu.add(1, Menu.NONE, 5, "*****");
		menu.add(1, Menu.NONE, 6, "Buy");
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		if (item.getGroupId() == 1){		
			switch (item.getOrder()) {
			case 1:
				rate_picture(1);
				return true;
			case 2:
				rate_picture(2);
				return true;
			case 3:
				rate_picture(3);
				return true;
			case 4:
				rate_picture(4);
				return true;
			case 5:
				rate_picture(5);
				return true;
            case 6:
            	Intent intent = new Intent(this.getActivity(), BuyPicture.class);
            	intent.putExtra("pictureid", mImageUrl);
            	intent.putExtra("provider", "PP");
            	startActivity(intent);
            	return true;
			}
		    return super.onContextItemSelected(item);
		}
		return false;
	}

    private String rate_picture(int i) {
		String url = getString(R.string.baseServerName)+"rate_picture.php";
		url += "?picture_id="+mImageUrl;
		url += "&rating="+i;
		String response = null;

		try {
			response = (String) new GetHTTPResponse(context)
			.execute(url).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		return response;
		// TODO Display the user with the response to the rating action (successful or not).
		
	}

	@Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageView != null) {
            // Cancel any pending image work
            ImageWorker.cancelWork(mImageView);
            mImageView.setImageDrawable(null);
        }
    }
	
}
