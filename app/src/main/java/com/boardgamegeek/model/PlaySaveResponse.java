package com.boardgamegeek.model;

import com.boardgamegeek.provider.BggContract;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class PlaySaveResponse extends PlayPostResponse {

	private PlaySave playSave;

	public PlaySaveResponse(OkHttpClient client, Request request) {
		super(client, request);
	}

	@Override
	protected void saveContent(String content) {
		Gson gson = new Gson();
		playSave = gson.fromJson(content, PlaySave.class);
	}

	public int getPlayCount() {
		if (hasError() || playSave == null) {
			return 0;
		} else {
			return playSave.getNumberOfPlays();
		}
	}

	public int getPlayId() {
		if (hasError() || playSave == null) {
			return BggContract.INVALID_ID;
		} else {
			return playSave.getPlayId();
		}
	}

	private static class PlaySave {
		@SuppressWarnings("unused") private int playid;
		@SuppressWarnings("unused") private int numplays;
		@SuppressWarnings("unused") private String html;

		public int getPlayId() {
			return playid;
		}

		public int getNumberOfPlays() {
			return numplays;
		}
	}
}
