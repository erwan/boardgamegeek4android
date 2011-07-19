package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncListColumns;
import com.boardgamegeek.util.StringUtils;

public class RemoteCollectionHandler extends XmlHandler {
	private static final String TAG = "RemoteCollectionHandler";

	// TODO: Parse Version Info
	// TODO: Parse Private Info

	private long mStartTime;
	private XmlPullParser mParser;
	private ContentResolver mResolver;

	private int mUpdateGameCount = 0;
	private int mInsertGameCount = 0;
	private int mSkipGameCount = 0;
	private int mUpdateCollectionCount = 0;
	private int mInsertCollectionCount = 0;
	private int mSkipCollectionCount = 0;

	private String[] mGameProjection = new String[] { SyncListColumns.UPDATED_LIST };
	private String[] mCollectionProjection = new String[] { SyncListColumns.UPDATED_LIST };

	public RemoteCollectionHandler(long startTime) {
		super(BggContract.CONTENT_AUTHORITY);
		mStartTime = startTime;
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;
		mResolver = resolver;

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEMS.equals(mParser.getName())) {

				int itemCount = StringUtils.parseInt(parser.getAttributeValue(null, Tags.TOTAL_ITEMS));
				Log.i(TAG, "Expecting " + itemCount + " items");

				parseItems();
			}
		}

		return false;
	}

	private void parseItems() throws XmlPullParserException, IOException {

		ContentValues gameValues = new ContentValues();
		ContentValues collectionValues = new ContentValues();

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEM.equals(mParser.getName())) {

				int gameId = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.GAME_ID));
				int collectionId = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.COLLECTION_ID));

				gameValues.clear();
				collectionValues.clear();
				parseItem(gameValues, collectionValues);

				insertOrUpdateGame(gameId, gameValues);
				insertOrUpdateCollectionItem(collectionId, gameId, collectionValues);
			}
		}
		Log.i(TAG, "Updated " + mUpdateGameCount + ", inserted " + mInsertGameCount + ", skipped " + mSkipGameCount
				+ " games");
		Log.i(TAG, "Updated " + mUpdateCollectionCount + ", inserted " + mInsertCollectionCount + ", skipped "
				+ mSkipCollectionCount + " collection items");
	}

	private void insertOrUpdateGame(int gameId, ContentValues values) {
		Cursor cursor = null;
		try {
			values.put(Games.UPDATED_LIST, System.currentTimeMillis());

			Uri uri = Games.buildGameUri(gameId);
			cursor = mResolver.query(uri, mGameProjection, null, null, null);
			if (cursor.moveToFirst()) {
				long lastUpdated = cursor.getLong(0);
				if (lastUpdated < mStartTime) {
					mResolver.update(uri, values, null, null);
					mUpdateGameCount++;
				} else {
					mSkipGameCount++;
				}
			} else {
				values.put(Games.GAME_ID, gameId);
				mResolver.insert(Games.CONTENT_URI, values);
				mInsertGameCount++;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void insertOrUpdateCollectionItem(int itemId, int gameId, ContentValues values) {
		Cursor cursor = null;
		try {
			values.put(Collection.GAME_ID, gameId);
			values.put(Collection.UPDATED_LIST, System.currentTimeMillis());

			Uri uri = Collection.buildItemUri(itemId);
			cursor = mResolver.query(uri, mCollectionProjection, null, null, null);
			if (cursor.moveToFirst()) {
				long lastUpdated = cursor.getLong(0);
				if (lastUpdated < mStartTime) {
					mResolver.update(uri, values, null, null);
					mUpdateCollectionCount++;
				} else {
					mSkipCollectionCount++;
				}
			} else {
				values.put(Collection.COLLECTION_ID, itemId);
				mResolver.insert(Collection.CONTENT_URI, values);
				mInsertCollectionCount++;
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void parseItem(ContentValues gameValues, ContentValues collectionValues) throws XmlPullParserException,
			IOException {

		String tag = null;
		int sortIndex = 1;

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {

			if (type == START_TAG) {
				tag = mParser.getName();

				if (Tags.NAME.equals(tag)) {
					sortIndex = StringUtils.parseInt(mParser.getAttributeValue(null, Tags.SORT_INDEX), 1);
				} else if (Tags.STATS.equals(tag)) {
					gameValues.put(Games.MIN_PLAYERS, mParser.getAttributeValue(null, Tags.MIN_PLAYERS));
					gameValues.put(Games.MAX_PLAYERS, mParser.getAttributeValue(null, Tags.MAX_PLAYERS));
					gameValues.put(Games.PLAYING_TIME, mParser.getAttributeValue(null, Tags.PLAYING_TIME));
					gameValues.put(Games.NUM_OWNED, mParser.getAttributeValue(null, Tags.NUM_OWNED));
				} else if (Tags.STATUS.equals(tag)) {
					collectionValues.put(Collection.STATUS_OWN, mParser.getAttributeValue(null, Tags.STATUS_OWN));
					collectionValues.put(Collection.STATUS_PREVIOUSLY_OWNED,
							mParser.getAttributeValue(null, Tags.STATUS_PREVIOUSLY_OWNED));
					collectionValues.put(Collection.STATUS_FOR_TRADE,
							mParser.getAttributeValue(null, Tags.STATUS_FOR_TRADE));
					collectionValues.put(Collection.STATUS_WANT, mParser.getAttributeValue(null, Tags.STATUS_WANT));
					collectionValues.put(Collection.STATUS_WANT_TO_PLAY,
							mParser.getAttributeValue(null, Tags.STATUS_WANT_TO_PLAY));
					collectionValues.put(Collection.STATUS_WANT_TO_BUY,
							mParser.getAttributeValue(null, Tags.STATUS_WANT_TO_BUY));
					collectionValues.put(Collection.STATUS_WISHLIST,
							mParser.getAttributeValue(null, Tags.STATUS_WISHLIST));
					collectionValues.put(Collection.STATUS_WISHLIST_PRIORITY,
							mParser.getAttributeValue(null, Tags.STATUS_WISHLIST_PRIORITY));
					collectionValues.put(Collection.STATUS_PREORDERED,
							mParser.getAttributeValue(null, Tags.STATUS_PREORDERED));
				}
			} else if (type == END_TAG) {
				tag = null;
				sortIndex = 1;
			} else if (type == TEXT) {
				String text = mParser.getText();
				if (Tags.NAME.equals(tag)) {
					String sortName = StringUtils.createSortName(text, sortIndex);
					collectionValues.put(Collection.COLLECTION_NAME, text);
					collectionValues.put(Collection.COLLECTION_SORT_NAME, sortName);
					gameValues.put(Games.GAME_NAME, text);
					gameValues.put(Games.GAME_SORT_NAME, sortName);
				} else if (Tags.ORIGINAL_NAME.equals(tag)) {
					gameValues.put(Games.GAME_NAME, text);
				} else if (Tags.YEAR_PUBLISHED.equals(tag)) {
					gameValues.put(Games.YEAR_PUBLISHED, text);
				} else if (Tags.IMAGE.equals(tag)) {
					gameValues.put(Games.IMAGE_URL, text);
				} else if (Tags.THUMBNAIL.equals(tag)) {
					gameValues.put(Games.THUMBNAIL_URL, text);
				} else if (Tags.NUM_PLAYS.equals(tag)) {
					gameValues.put(Games.NUM_PLAYS,
							StringUtils.parseInt(mParser.getAttributeValue(null, Tags.NUM_PLAYS)));
				} else if (Tags.PRIVATE_INFO.equals(tag)) {
					parsePrivateInfo(collectionValues);
					tag = null;
				} else if (Tags.COMMENT.equals(tag)) {
					collectionValues.put(Collection.COMMENT, text);
				}
			}
		}
	}

	private ContentValues parsePrivateInfo(ContentValues values) throws XmlPullParserException, IOException {
		// String tag = null;
		// final int depth = mParser.getDepth();
		// int type;
		// while (((type = mParser.next()) != END_TAG || mParser.getDepth() >
		// depth)
		// && type != END_DOCUMENT) {
		//
		// if (type == START_TAG) {
		// tag = mParser.getName();
		// } else if (type == END_TAG) {
		// tag = null;
		// } else if (type == TEXT) {
		// String text = mParser.getText();
		// if (Tags.PRIVATE_INFO_ACQUIRED_FROM.equals(tag)) {
		// values.put(Games.PRIVATE_INFO_ACQUIRED_FROM, text);
		// } else if (Tags.PRIVATE_INFO_ACQUISITION_DATE.equals(tag)) {
		// // TODO: how to handle date in YYYY-MM-DD?
		// values.put(Games.PRIVATE_INFO_ACQUISITION_DATE, text);
		// } else if (Tags.PRIVATE_INFO_COMMENT.equals(tag)) {
		// values.put(Games.PRIVATE_INFO_COMMENT, text);
		// } else if (Tags.PRIVATE_INFO_CURRENT_VALUE.equals(tag)) {
		// values.put(Games.PRIVATE_INFO_CURRENT_VALUE,
		// Utility.parseDouble(text));
		// } else if (Tags.PRIVATE_INFO_CURRENT_VALUE_CURRENCY.equals(tag)) {
		// values.put(Games.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, text);
		// } else if (Tags.PRIVATE_INFO_PRICE_PAID.equals(tag)) {
		// values.put(Games.PRIVATE_INFO_PRICE_PAID, Utility.parseDouble(text));
		// } else if (Tags.PRIVATE_INFO_PRICE_PAID_CURRENCY.equals(tag)) {
		// values.put(Games.PRIVATE_INFO_PRICE_PAID_CURRENCY, text);
		// } else if (Tags.PRIVATE_INFO_QUANTITY.equals(tag)) {
		// values.put(Games.PRIVATE_INFO_QUANTITY, Utility.parseInt(text));
		// }
		// }
		// }
		return values;
	}

	private interface Tags {
		String ITEMS = "items";
		String TOTAL_ITEMS = "totalitems";
		String ITEM = "item";
		String GAME_ID = "objectid";
		String COLLECTION_ID = "collid";
		String NAME = "name";
		String ORIGINAL_NAME = "originalname";
		String SORT_INDEX = "sortindex";
		String YEAR_PUBLISHED = "yearpublished";
		String IMAGE = "image";
		String THUMBNAIL = "thumbnail";

		String STATS = "stats";
		String MIN_PLAYERS = "minplayers";
		String MAX_PLAYERS = "maxplayers";
		String PLAYING_TIME = "playingtime";
		String NUM_OWNED = "numowned";

		String STATUS = "status";
		String STATUS_OWN = "own";
		String STATUS_PREVIOUSLY_OWNED = "prevowned";
		String STATUS_FOR_TRADE = "fortrade";
		String STATUS_WANT = "want";
		String STATUS_WANT_TO_PLAY = "wanttoplay";
		String STATUS_WANT_TO_BUY = "wanttobuy";
		String STATUS_WISHLIST = "wishlist";
		String STATUS_WISHLIST_PRIORITY = "wishlistpriority";
		String STATUS_PREORDERED = "preordered";

		String NUM_PLAYS = "numplays";

		String PRIVATE_INFO = "privateinfo";
		// String PRIVATE_INFO_PRICE_PAID_CURRENCY = "pp_currency";
		// String PRIVATE_INFO_PRICE_PAID = "pricepaid";
		// String PRIVATE_INFO_CURRENT_VALUE_CURRENCY = "cv_currency";
		// String PRIVATE_INFO_CURRENT_VALUE = "currvalue";
		// String PRIVATE_INFO_QUANTITY = "quantity";
		// String PRIVATE_INFO_ACQUISITION_DATE = "acquisitiondate";
		// String PRIVATE_INFO_ACQUIRED_FROM = "acquiredfrom";
		// String PRIVATE_INFO_COMMENT = "privatecomment";

		String COMMENT = "comment";
	}
}