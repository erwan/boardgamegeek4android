package com.boardgamegeek.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteForumlistHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.UIUtils;

public class ForumlistActivity extends ListActivity {
	private final String TAG = "ForumlistActivity";

	public static final String KEY_FORUMLIST_ID = "FORUMLIST_ID";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_FORUMS = "FORUMS";

	private ForumlistAdapter mAdapter;
	private List<Forum> mForums = new ArrayList<Forum>();

	private int mForumlistId;
	private String mThumbnailUrl;
	private String mGameName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_forumlist);
		findViewById(R.id.game_thumbnail).setClickable(false);

		if (savedInstanceState == null) {
			final Intent intent = getIntent();
			mForumlistId = intent.getExtras().getInt(KEY_FORUMLIST_ID);
			mThumbnailUrl = intent.getExtras().getString(KEY_THUMBNAIL_URL);
			mGameName = intent.getExtras().getString(KEY_GAME_NAME);
		} else {
			mForumlistId = savedInstanceState.getInt(KEY_FORUMLIST_ID);
			mThumbnailUrl = savedInstanceState.getString(KEY_THUMBNAIL_URL);
			mGameName = savedInstanceState.getString(KEY_GAME_NAME);
			mForums = savedInstanceState.getParcelableArrayList(KEY_FORUMS);
		}

		UIUtils.setTitle(this);
		UIUtils u = new UIUtils(this);
		u.setGameName(mGameName);
		u.setThumbnail(mThumbnailUrl);

		if (mForums == null || mForums.size() == 0) {
			ForumlistTask task = new ForumlistTask();
			task.execute();
		} else {
			mAdapter = new ForumlistAdapter();
			setListAdapter(mAdapter);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_FORUMLIST_ID, mForumlistId);
		outState.putString(KEY_THUMBNAIL_URL, mThumbnailUrl);
		outState.putString(KEY_GAME_NAME, mGameName);
		outState.putParcelableArrayList(KEY_FORUMS, (ArrayList<? extends Parcelable>) mForums);
	}

	public void onHomeClick(View v) {
		UIUtils.goHome(this);
	}

	public void onSearchClick(View v) {
		onSearchRequested();
	}

	private class ForumlistTask extends AsyncTask<Void, Void, RemoteForumlistHandler> {
		private HttpClient mHttpClient;
		private RemoteExecutor mExecutor;
		private RemoteForumlistHandler mHandler = new RemoteForumlistHandler();

		@Override
		protected void onPreExecute() {
			mHttpClient = HttpUtils.createHttpClient(ForumlistActivity.this, true);
			mExecutor = new RemoteExecutor(mHttpClient, null);
		}

		@Override
		protected RemoteForumlistHandler doInBackground(Void... params) {
			final String url = HttpUtils.constructForumlistUrl(mForumlistId);
			Log.i(TAG, "Loading forums from " + url);
			try {
				mExecutor.executeGet(url, mHandler);
			} catch (HandlerException e) {
				Log.e(TAG, e.toString());
			}
			return mHandler;
		}

		@Override
		protected void onPostExecute(RemoteForumlistHandler result) {
			Log.i(TAG, "Forums count " + result.getCount());
			final int count = result.getCount();
			if (result.isBggDown()) {
				UIUtils.showListMessage(ForumlistActivity.this, R.string.bgg_down);
			} else if (count == 0) {
				String message = String.format(getResources().getString(R.string.forumlist_no_results), mGameName);
				UIUtils.showListMessage(ForumlistActivity.this, message);
			} else {
				mForums.addAll(result.getResults());
				mAdapter = new ForumlistAdapter();
				setListAdapter(mAdapter);
			}
		}
	}

	@Override
	protected void onListItemClick(ListView listView, View convertView, int position, long id) {
		ViewHolder holder = (ViewHolder) convertView.getTag();
		if (holder != null) {
			Intent forumsIntent = new Intent(this, ForumActivity.class);
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_ID, holder.forumId);
			forumsIntent.putExtra(ForumActivity.KEY_GAME_NAME, mGameName);
			forumsIntent.putExtra(ForumActivity.KEY_THUMBNAIL_URL, mThumbnailUrl);
			forumsIntent.putExtra(ForumActivity.KEY_FORUM_TITLE, holder.forumTitle.getText());
			forumsIntent.putExtra(ForumActivity.KEY_NUM_THREADS, holder.numThreads.getText());
			this.startActivity(forumsIntent);
		}
	}

	private class ForumlistAdapter extends ArrayAdapter<Forum> {
		private LayoutInflater mInflater;
		private Resources mResources;
		private String mLastPostText;

		public ForumlistAdapter() {
			super(ForumlistActivity.this, R.layout.row_forum, mForums);
			mInflater = getLayoutInflater();
			mResources = getResources();
			mLastPostText = mResources.getString(R.string.forum_last_post);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.row_forum, parent, false);
				holder = new ViewHolder(convertView);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			Forum forumlist;
			try {
				forumlist = mForums.get(position);
			} catch (ArrayIndexOutOfBoundsException e) {
				return convertView;
			}
			if (forumlist != null) {
				holder.forumId = forumlist.id;
				holder.forumTitle.setText(forumlist.title);
				holder.numThreads.setText(mResources.getQuantityString(R.plurals.forum_threads, forumlist.numthreads,
						forumlist.numthreads));
				if (forumlist.lastpostdate > 0) {
					holder.lastPost.setText(String.format(mLastPostText,
							DateUtils.getRelativeTimeSpanString(ForumlistActivity.this, forumlist.lastpostdate)));
					holder.lastPost.setVisibility(View.VISIBLE);
				} else {
					holder.lastPost.setVisibility(View.GONE);
				}
			}
			return convertView;
		}
	}

	static class ViewHolder {
		String forumId;
		TextView forumTitle;
		TextView numThreads;
		TextView lastPost;

		public ViewHolder(View view) {
			forumTitle = (TextView) view.findViewById(R.id.forum_title);
			numThreads = (TextView) view.findViewById(R.id.numthreads);
			lastPost = (TextView) view.findViewById(R.id.lastpost);
		}
	}
}
