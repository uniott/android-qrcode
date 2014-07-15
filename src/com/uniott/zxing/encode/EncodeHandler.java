package com.uniott.zxing.encode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.WriterException;
import com.uniott.zxing.QRCodeSettings;

public class EncodeHandler {
	private QRCodeSettings code;

	public EncodeHandler(QRCodeSettings code) {
		this.code = code;
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
		int dimension = getDefaultSize(context);
		if (this.code.getSize() <= 0) {
			this.code.setSize(dimension);
		}
		QRCodeEncoder qrCodeEncoder = null;
		try {
			qrCodeEncoder = new QRCodeEncoder(this.code);
			bitmap = qrCodeEncoder.encodeAsBitmap();
		} catch (WriterException e) {
			e.printStackTrace();
		} finally {
			qrCodeEncoder = null;
		}
		return bitmap;
	}
}
