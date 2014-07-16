package com.uniott.zxing.encode;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.WriterException;
import com.uniott.zxing.QRCodeSettings;

public class EncodeHandler {
	private static final String TAG = EncodeHandler.class.getSimpleName();

	private QRCodeSettings code;

	public EncodeHandler(QRCodeSettings code) {
		this.code = code;
	}

	/**
	 * 获取默认的尺寸
	 * 
	 * @param context
	 * @return
	 */
	@SuppressLint("NewApi")
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
		Bitmap outBitmap = null;
		int dimension = getDefaultSize(context);
		if (this.code.getSize() <= 0) {
			this.code.setSize(dimension);
		}
		QRCodeEncoder qrCodeEncoder = null;
		try {
			qrCodeEncoder = new QRCodeEncoder(this.code);

			// // 如果没有样式，则直接返回
			if (code.getStyles() == null || code.getStyles().size() == 0) {
				return qrCodeEncoder.encodeAsBitmap();
			}
			Bitmap encodeBitmap = qrCodeEncoder.encodeAsBitmap();
			outBitmap = setQRCodeStyle(encodeBitmap);
		} catch (WriterException e) {
			e.printStackTrace();
		} finally {
			qrCodeEncoder = null;
		}
		return outBitmap;
	}

	/**
	 * 绘制二维码的样式，所有样式有绘制顺序
	 * 
	 * @param sourceBitmap
	 * @return
	 */
	private Bitmap setQRCodeStyle(Bitmap sourceBitmap) {
		HashMap<QRCodeStyle, Object> styles = this.code.getStyles();
		if (styles == null || styles.size() == 0) {
			return sourceBitmap;
		}
		int encodeW = sourceBitmap.getWidth();
		int encodeH = sourceBitmap.getHeight();
		Bitmap outBitmap = Bitmap.createBitmap(encodeW, encodeH, Bitmap.Config.ARGB_8888);
		if (styles.containsKey(QRCodeStyle.BG_SHARE)) {
		}
		// 绘制中间的图片
		if (styles.containsKey(QRCodeStyle.CENTER_IMAGE)) {
			Bitmap centerBmp = (Bitmap) styles.get(QRCodeStyle.CENTER_IMAGE);
			int imageW = centerBmp.getWidth();
			int imageH = centerBmp.getHeight();
			int maxIamgeW = encodeW / 4;
			int maxImageH = encodeH / 4;

			Log.e(TAG, "原二维码的尺寸是 " + encodeW + ":" + encodeH + "  中间图片的原尺寸是 " + imageW + ":" + imageH + "  最大尺寸是  "
					+ maxIamgeW + ":" + maxImageH);
			// 中间的图片最大不能超过二维码的1/4 防止覆盖二维码数据
			if (imageW > maxIamgeW || imageH > maxImageH) {
				imageW = maxIamgeW;
				imageH = maxImageH;
				centerBmp = Bitmap.createScaledBitmap(centerBmp, imageW, imageH, false);
			}
			int left = (encodeW - imageW) / 2;
			int top = (encodeH - imageH) / 2;

			Canvas canvas = new Canvas(outBitmap);// 绘制图片
			canvas.drawBitmap(sourceBitmap, 0, 0, null);
			canvas.drawBitmap(centerBmp, left, top, null);
			// 回收资源
			centerBmp.recycle();
		}

		sourceBitmap.recycle();
		return outBitmap;
	}
}
