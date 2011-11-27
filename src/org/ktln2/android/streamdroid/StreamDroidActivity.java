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
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.util.ArrayList;


public class StreamDroidActivity extends Activity {
	private static final String TAG = "StreamDroidActivity";
	private ImageView mThumbnail;
	private ImageAdapter mAdapter;

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
	}

	// http://stackoverflow.com/questions/6995901/android-unable-to-invoke-gallery-with-video
	/**
	 * Call the gallery in order to choose a video.
	 */
	public void addVideo(View view) {
		/*
		 * If we use ACTION_GET_CONTENT will open also the file manager that
		 * return a path like /mimetype//mnt/sdcard/download/The.Big.Bang.Theory.S05E01.HDTV.XviD-ASAP.avi
		 * that is not usable (is without ID). So to avoid unavoidable crash
		 * we call only the gallery application.
		 */
		Intent videoListIntent = new Intent(Intent.ACTION_PICK);
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

			Cursor c = managedQuery(
				selectedVideoUri,
				null,
				null,
				null,
				null);
			int _idIndex =   c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
			int titleIndex = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);

			c.moveToFirst();

			android.util.Log.i(TAG, "Title: " + c.getString(titleIndex));

			String _ID = c.getString(_idIndex);
			mAdapter.add(new Long(_ID));
		}
	}

	/**
	 * Adapter for an array containing id of video in order to
	 * show their thumbnails.
	 *
	 * This adapter augments with an item used by the gallery for
	 * attach new video thumbnail.
	 *
	 * TODO: use the URI as item in the array so to use
	 * more general content provider as well.
	 */
	public class ImageAdapter extends ArrayAdapter<Long> {
		private Context mContext;

		public ImageAdapter(Context c) {
			super(c, 0, new ArrayList<Long>());

			mContext = c;
		}

		/**
		 * Retrieves the thumbnail for the video identified by the id passed
		 * as argument.
		 *
		 * The thumbnail's type is MINI_KIND.
		 */
		private Bitmap getVideoThumbnail(long id) {
			return MediaStore.Video.Thumbnails.getThumbnail(
						getContentResolver(),
						id,
						MediaStore.Video.Thumbnails.MINI_KIND,
						null
					);
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

		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageView imageView = new ImageView(mContext);

			//imageView.setImageResource(mImageIds[position]);
			imageView.setTag(position);

			imageView.setLayoutParams(new Gallery.LayoutParams(250, 200));
			imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			if (position == (getCount() - 1)) {
				imageView.setImageResource(R.drawable.ic_add);
			} else {
				imageView.setImageBitmap(getVideoThumbnail(getItem(position)));
			}

			return imageView;
		}
	}
}
