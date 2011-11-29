package org.ktln2.android.streamdroid;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
//import android.content.ClipData;
//import android.content.ClipDescription;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Gallery;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.provider.MediaStore;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.ArrayList;
import java.util.List;


public class StreamDroidActivity extends Activity {
	private static final String TAG = "StreamDroidActivity";
	private ImageView mThumbnail;
	private ImageAdapter mAdapter;

	public native String concatenate();
	public native String concatenateBis(String myString);
	public native int printArray(String[] myString);

	static {
		System.loadLibrary("concatenate");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Gallery gallery = (Gallery) findViewById(R.id.gallery);
		mAdapter = new ImageAdapter(this);
		gallery.setAdapter(mAdapter);

		/*
		 * If you click on the last item you call the gallery application to
		 * add a video to the gallery.
		 */
		gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position, long id) {
				int count = parent.getAdapter().getCount();
				if (position == (count - 1)) {
					addVideo(null);
				}
			}
		});

		android.util.Log.i(TAG, "from jni: " + concatenate());
		android.util.Log.i(TAG, "from jni: " + concatenateBis("miao"));
		android.util.Log.i(TAG, "from jni: " + printArray(new String[] {"miao", "bau"}));
	}

	// http://stackoverflow.com/questions/6995901/android-unable-to-invoke-gallery-with-video
	/**
	 * Call the intents able to choose a video.
	 */
	public void addVideo(View view) {
		Intent videoListIntent = new Intent(Intent.ACTION_GET_CONTENT);
		videoListIntent.setType("video/*");
		startActivityForResult(videoListIntent, 1);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// this returns a path like "/external/video/media/12"
		//
		// see this
		//  http://stackoverflow.com/questions/7856959/android-file-chooser
		// for
		//  /mimetype//mnt/sdcard/download/The.Big.Bang.Theory.S05E01.HDTV.XviD-ASAP.avi
		if ((requestCode == 1) && (resultCode == RESULT_OK) && (data != null)) {
			android.util.Log.i(TAG, "---------------------" + data.getData().getEncodedPath());
			// http://stackoverflow.com/questions/3874275/how-to-receive-multiple-gallery-results-in-onactivityresult
			Uri selectedVideoUri = data.getData();
			android.util.Log.i(TAG, "uri: " + data.getData());

			mAdapter.add(selectedVideoUri);
		}
	}

	/**
	 * Adapter for an array containing uri of video in order to
	 * show their thumbnails.
	 *
	 * This adapter augments with an item used by the gallery for
	 * attach new video thumbnail.
	 *
	 * For implementational problems we create various List which
	 * maintain the properties for each items (like thumbnails)
	 * and augment it with the add() method.
	 */
	public class ImageAdapter extends ArrayAdapter<Uri> {
		private Context mContext;
		private List<Bitmap> mThumbnails = new ArrayList();

		public ImageAdapter(Context c) {
			super(c, 0, new ArrayList<Uri>());

			mContext = c;
		}

		/**
		 * Retrieves the thumbnail for the video identified by the Uri passed
		 * as argument.
		 *
		 * I don't know which properties can have a item returned from an intent
		 * that handles videos, with content://org.openintents.cmfilemanager/ exists
		 * the "DATA" column with the path of the resource with which build the thumbnail.
		 *
		 * The thumbnail's type is MINI_KIND.
		 */
		private Bitmap getVideoThumbnail(Cursor c, Uri uri) {
			Bitmap thumbnail = null;


			try {
				int titleIndex = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
				android.util.Log.i(TAG, "Title: " + c.getString(titleIndex));

				int _idIndex =   c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
				String _ID = c.getString(_idIndex);

				thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
							getContentResolver(),
							new Long(_ID),
							MediaStore.Video.Thumbnails.MINI_KIND,
							null
						);
			} catch (java.lang.IllegalArgumentException e) {
				int dataIndex = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
				String data = c.getString(dataIndex);

				android.util.Log.i(TAG, "Data: " + c.getString(dataIndex));

				// May return null if the video is corrupt or the format is not supported.
				thumbnail = ThumbnailUtils.createVideoThumbnail(data, MediaStore.Video.Thumbnails.MINI_KIND);

				if (thumbnail == null) {
					android.util.Log.i(TAG, "thumbnail not created");
					// TODO: create a default image
				}

			}

			return thumbnail;
		}

		/**
		 * Since we want to have ever a "+" item augment the original
		 * size by one.
		 */
		@Override
		public int getCount() {
			int n = super.getCount();

			return n + 1;
		}

		@Override
		public void add(Uri uri) {
			// so to avoid + 1
			int n = super.getCount();

			Cursor c = managedQuery(
				uri,
				null,
				null,
				null,
				null);

			if (!( c != null && c.moveToFirst())){
				return;
			}

			mThumbnails.add(n, getVideoThumbnail(c, uri));

			super.add(uri);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageView imageView = new ImageView(mContext);

			imageView.setTag(position);

			imageView.setLayoutParams(new Gallery.LayoutParams(250, 200));
			imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			if (position == (getCount() - 1)) {
				imageView.setImageResource(R.drawable.ic_add);
			} else {
				imageView.setImageBitmap(mThumbnails.get(position));
			}

			return imageView;
		}
	}
}
