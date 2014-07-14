package com.uniott.zxing.encode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.WriterException;
import com.uniott.zxing.scanner.Intents;

public class EncodeHandler {
	private static final String TAG = EncodeActivity.class.getSimpleName();
	private static final String USE_VCARD_KEY = "USE_VCARD";
	private QRCodeEncoder qrCodeEncoder;

	private String type, data;
	boolean useVCard = false;

	int dimension = 0;

	public EncodeHandler(String type, String data) {
		this.type = type;
		this.data = data;
		this.useVCard = false;
	}

	public EncodeHandler(String type, String data, boolean useVCard) {
		this(type, data);
		this.useVCard = useVCard;
	}

	@SuppressLint("NewApi")
	/**
	 * 获取默认的尺寸
	 * @param context
	 * @return
	 */
	private int getDefaultSize(Context context) {
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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

		return smallerDimension;
	}

	public Bitmap encodeAsBitmap(Context context) {
		Bitmap bitmap = null;
		if (this.dimension <= 0) {
			this.dimension = getDefaultSize(context);
		}

		Intent intent = new Intent(Intents.Encode.ACTION);
		intent.putExtra(Intents.Encode.TYPE, this.type);
		intent.putExtra(Intents.Encode.DATA, this.data);

		try {
			qrCodeEncoder = new QRCodeEncoder(context, intent, dimension, useVCard);
			boolean useVCard = intent.getBooleanExtra(USE_VCARD_KEY, false);
			qrCodeEncoder = new QRCodeEncoder(context, intent, this.dimension, useVCard);
			bitmap = qrCodeEncoder.encodeAsBitmap();
		} catch (WriterException e) {
			e.printStackTrace();
		} finally {
			qrCodeEncoder = null;
		}
		return bitmap;
	}
}
