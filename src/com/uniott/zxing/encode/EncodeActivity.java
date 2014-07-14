/*
 * Copyright (C) 2008 ZXing authors
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

package com.uniott.zxing.encode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;
import com.uniott.uni.zxing.R;
import com.uniott.zxing.scanner.FinishListener;
import com.uniott.zxing.scanner.Intents;

/**
 * This class encodes data from an Intent into a QR code, and then displays it
 * full screen so that another person can scan it with their device.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class EncodeActivity extends Activity {

	private static final String TAG = EncodeActivity.class.getSimpleName();

	private static final String USE_VCARD_KEY = "USE_VCARD";

	private QRCodeEncoder qrCodeEncoder;

	private String type, data;

	boolean useVCard = false;

//	public static final void startEncode(Context context, String type, String text) {
//		Intent intent = new Intent(context, EncodeActivity.class);
//		intent.addCategory(Intent.CATEGORY_DEFAULT);
//		intent.putExtra(Intents.Encode.TYPE, type);
//		intent.putExtra(Intents.Encode.DATA, text);
//		intent.setAction(Intents.Encode.ACTION);
//		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//
//		context.startActivity(intent);
//	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Intent intent = getIntent();
		if (intent == null || TextUtils.isEmpty(type = intent.getStringExtra(Intents.Encode.TYPE))
				|| TextUtils.isEmpty(data = intent.getStringExtra(Intents.Encode.DATA))) {
			finish();
			return;
		}

		useVCard = intent.getBooleanExtra(USE_VCARD_KEY, false);
		String action = intent.getAction();

		if (Intents.Encode.ACTION.equals(action) || Intent.ACTION_SEND.equals(action)) {
		} else {
			finish();
		}
		setContentView(R.layout.encode);
	}

	@SuppressLint("NewApi")
	private void handleEncode() {
		WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		Point displaySize = new Point();
		try {
			display.getSize(displaySize);
		} catch (java.lang.NoSuchMethodError ignore) { // Older device
			displaySize.x = display.getWidth();
			displaySize.y = display.getHeight();
		}

		int width = displaySize.x;
		int height = displaySize.y;
		int smallerDimension = width < height ? width : height;
		smallerDimension = smallerDimension * 7 / 8;

		Intent intent = getIntent();
		try {
			boolean useVCard = intent.getBooleanExtra(USE_VCARD_KEY, false);
			qrCodeEncoder = new QRCodeEncoder(this, intent, smallerDimension, useVCard);

			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			if (bitmap == null) {
				Log.w(TAG, "Could not encode barcode");
				showErrorMessage(R.string.msg_encode_contents_failed);
				qrCodeEncoder = null;
				return;
			}

			ImageView view = (ImageView) findViewById(R.id.image_view);
			view.setImageBitmap(bitmap);

			TextView contents = (TextView) findViewById(R.id.contents_text_view);
			if (intent.getBooleanExtra(Intents.Encode.SHOW_CONTENTS, true)) {
				contents.setText(qrCodeEncoder.getDisplayContents());
				setTitle("Encoder");
			} else {
				contents.setText("");
				setTitle("");
			}
		} catch (WriterException e) {
			Log.w(TAG, "Could not encode barcode", e);
			showErrorMessage(R.string.msg_encode_contents_failed);
			qrCodeEncoder = null;
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		handleEncode();
	}

	private void showErrorMessage(int message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}
}
