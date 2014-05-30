package com.boardgamegeek.ui.widget;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.util.ColorUtils;

public class PlayerRow extends LinearLayout {
	private DecimalFormat mFormat = new DecimalFormat("0.0######");
	private boolean mEditScores;

	private View mDragHandle;
	private TextView mName;
	private TextView mUsername;
	private View mColorSwatchContainer;
	private View mColorSwatch;
	private TextView mTeamColor;
	private TextView mScore;
	private TextView mStartingPosition;
	private TextView mSeatColor;
	private TextView mSeat;
	private TextView mRating;
	private ImageView mDeleteButton;

	private Typeface mNameTypeface;
	private Typeface mUsernameTypeface;
	private Typeface mScoreTypeface;

	public PlayerRow(Context context) {
		this(context, null);
	}

	public PlayerRow(Context context, AttributeSet attrs) {
		super(context, attrs);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.row_player, this);
		initializeUi();
	}

	private void initializeUi() {
		mDragHandle = findViewById(R.id.drag_handle);
		mSeatColor = (TextView) findViewById(R.id.seat_color);
		mSeat = (TextView) findViewById(R.id.seat);
		mColorSwatchContainer = findViewById(R.id.color_swatch_container);
		mColorSwatch = findViewById(R.id.color_swatch);
		mName = (TextView) findViewById(R.id.name);
		mUsername = (TextView) findViewById(R.id.username);
		mTeamColor = (TextView) findViewById(R.id.team_color);
		mScore = (TextView) findViewById(R.id.score);
		mRating = (TextView) findViewById(R.id.rating);
		mStartingPosition = (TextView) findViewById(R.id.starting_position);

		mNameTypeface = mName.getTypeface();
		mUsernameTypeface = mUsername.getTypeface();
		mScoreTypeface = mScore.getTypeface();

		mDeleteButton = (ImageView) findViewById(R.id.log_player_delete);
	}

	public void setOnDeleteListener(OnClickListener l) {
		mDeleteButton.setVisibility(View.VISIBLE);
		mDeleteButton.setFocusable(false); // necessary to allow the row to receive click events
		mDeleteButton.setOnClickListener(l);
	}

	public void setAutoSort(boolean value) {
		mDragHandle.setVisibility(value ? View.VISIBLE : View.GONE);
	}

	public void setEditScores(boolean edit) {
		mEditScores = edit;
	}

	public void setPlayer(Player player) {
		if (player == null) {
			setText(mName, "");
			setText(mUsername, "");
			setText(mTeamColor, "");
			setText(mScore, "");
			setText(mRating, "");
			setText(mSeatColor, "");
			setText(mSeat, "");
		} else {
			int color = ColorUtils.parseColor(player.TeamColor);

			setText(mSeatColor, player.getStartingPosition());
			setText(mSeat, player.getStartingPosition());
			setText(mName, player.Name, mNameTypeface, player.New, player.Win);
			setText(mUsername, player.Username, mUsernameTypeface, player.New, false);
			setText(mTeamColor, player.TeamColor);
			setText(mScore, player.Score, mScoreTypeface, false, player.Win);
			setText(mRating, (player.Rating > 0) ? mFormat.format(player.Rating) : "");
			setText(mStartingPosition, player.getStartingPosition());

			if (color != ColorUtils.TRANSPARENT) {
				mSeat.setVisibility(View.GONE);
				if (player.getSeat() == Player.SEAT_UNKNOWN) {
					mColorSwatch.setBackgroundColor(color);
					mColorSwatchContainer.setVisibility(View.VISIBLE);
					mSeatColor.setVisibility(View.GONE);
				} else {
					mSeatColor.setTextColor(color);
					mColorSwatchContainer.setVisibility(View.GONE);
					mStartingPosition.setVisibility(View.GONE);
				}
				mTeamColor.setVisibility(View.GONE);
			} else {
				mSeatColor.setVisibility(View.GONE);
				mColorSwatchContainer.setVisibility(View.GONE);
				if (player.getSeat() == Player.SEAT_UNKNOWN) {
					mSeat.setVisibility(View.GONE);
				} else {
					mStartingPosition.setVisibility(View.GONE);
				}
			}
		}
	}

	private void setText(TextView textView, String text) {
		textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
		textView.setText(text);
	}

	private void setText(TextView textView, String text, Typeface tf, boolean italic, boolean bold) {
		setText(textView, text);
		if (!TextUtils.isEmpty(text)) {
			if (italic && bold) {
				textView.setTypeface(tf, Typeface.BOLD_ITALIC);
			} else if (italic) {
				textView.setTypeface(tf, Typeface.ITALIC);
			} else if (bold) {
				textView.setTypeface(tf, Typeface.BOLD);
			} else {
				textView.setTypeface(tf, Typeface.NORMAL);
			}
		}
	}
}
