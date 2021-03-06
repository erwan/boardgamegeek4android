package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.ResolverUtils;

import hugo.weaving.DebugLog;

public abstract class UpdateCollectionItemTask extends AsyncTask<Void, Void, Void> {
	protected final Context context;
	protected final int gameId;
	protected final int collectionId;

	public UpdateCollectionItemTask(Context context, int gameId, int collectionId) {
		this.gameId = gameId;
		this.collectionId = collectionId;
		this.context = context;
	}

	@DebugLog
	protected long getCollectionItemInternalId(ContentResolver resolver, int collectionId, int gameId) {
		long internalId;
		if (collectionId == BggContract.INVALID_ID) {
			internalId = ResolverUtils.queryLong(resolver,
				Collection.CONTENT_URI,
				Collection._ID,
				BggContract.INVALID_ID,
				"collection." + Collection.GAME_ID + "=? AND " + Collection.COLLECTION_ID + " IS NULL",
				new String[] { String.valueOf(gameId) });
		} else {
			internalId = ResolverUtils.queryLong(resolver,
				Collection.CONTENT_URI,
				Collection._ID,
				BggContract.INVALID_ID,
				Collection.COLLECTION_ID + "=?",
				new String[] { String.valueOf(collectionId) });
		}
		return internalId;
	}
}
