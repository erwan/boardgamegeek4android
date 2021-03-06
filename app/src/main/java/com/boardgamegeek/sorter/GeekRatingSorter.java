package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

import java.text.DecimalFormat;

public class GeekRatingSorter extends CollectionSorter {
	private static final String COLUMN = Collection.STATS_BAYES_AVERAGE;
	private static final String DEFAULT_VALUE = "?";
	private final DecimalFormat displayFormat = new DecimalFormat("0.000");

	public GeekRatingSorter(@NonNull Context context) {
		super(context);
		orderByClause = getClause(COLUMN, true);
		descriptionId = R.string.collection_sort_geek_rating;
	}

	@StringRes
	@Override
	public int getTypeResource() {
		return R.string.collection_sort_type_geek_rating;
	}

	@Override
	public String[] getColumns() {
		return new String[] { COLUMN };
	}

	@Override
	public String getHeaderText(@NonNull Cursor cursor) {
		return getInfo(cursor, null);
	}

	@Override
	public String getDisplayInfo(@NonNull Cursor cursor) {
		return getInfo(cursor, displayFormat);
	}

	private String getInfo(@NonNull Cursor cursor, DecimalFormat format) {
		return getDoubleAsString(cursor, COLUMN, DEFAULT_VALUE, true, format);
	}
}
