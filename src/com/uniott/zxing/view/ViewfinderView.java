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

package com.uniott.zxing.view;

import java.util.Collection;
import java.util.HashSet;

import android.R.integer;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.uniott.zxing.R;
import com.uniott.zxing.camera.CameraManager;

public class ViewfinderView extends View {

	private static final long ANIMATION_DELAY = 100L;
	private static final int OPAQUE = 0xFF;
	private static final int SPEEN_DISTANCE = 5;

	private Paint paint;
	private Bitmap resultBitmap;

	private int frameLineWidth;// 边框的线的宽度
	private int frameLineLength;// 边框线条的长度
	private int maskColor;// 摄像头周围的颜色
	private int resultColor;
	private int frameColor;
	private int resultPointColor;
	private Collection<ResultPoint> possibleResultPoints;
	private Collection<ResultPoint> lastPossibleResultPoints;
	private boolean isFirst;
	private int slideTop;

	private Bitmap qrLineBitmap;// 扫描的分割线
	private int qrLineWidth;// 扫描线的长
	private int qrLineHeight;// 扫描线的高
	private Rect qrSrc;
	private Rect qrDst;

	// This constructor is used when the class is built from an XML resource.
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint();
		// read custom styles
		// 扫描线
		TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.ViewFinderView);

		frameLineLength = styles.getInt(R.styleable.ViewFinderView_frameLineLength, 150);
		frameLineWidth = styles.getInt(R.styleable.ViewFinderView_frameLineWidth, 12);

		BitmapDrawable line = (BitmapDrawable) styles.getDrawable(R.styleable.ViewFinderView_lineDrawable);
		if (line != null) {
			qrLineBitmap = line.getBitmap();
		}
		if (qrLineBitmap == null) {
			qrLineBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.qrcode_scan_line);
		}
		qrLineHeight = qrLineBitmap.getHeight();
		qrLineWidth = qrLineBitmap.getWidth();
		qrSrc = new Rect(0, 0, qrLineWidth, qrLineHeight);

		// 颜色设置

		frameColor = styles.getColor(R.styleable.ViewFinderView_frameColor, R.color.viewfinder_default_frame_color);
		maskColor = styles.getColor(R.styleable.ViewFinderView_maskColor, R.color.viewfinder_default_mask_color);
		resultPointColor = styles.getColor(R.styleable.ViewFinderView_resultPointColor,
				R.color.viewfinder_default_result_points_color);
		resultColor = styles.getColor(R.styleable.ViewFinderView_resultColor, R.color.viewfinder_default_result_color);

		possibleResultPoints = new HashSet<ResultPoint>(10);
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = CameraManager.get().getFramingRect();
		if (frame == null) {
			return;
		}

		// 初始化中间线滑动的最上边和最下边
		if (!isFirst) {
			isFirst = true;
			slideTop = frame.top;
		}

		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// Draw the exterior (i.e. outside the framing rect) darkened
		paint.setColor(resultBitmap != null ? resultColor : maskColor);
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

		if (resultBitmap != null) {
			// Draw the opaque result bitmap over the scanning rectangle
			paint.setAlpha(OPAQUE);
			canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
		} else {

			// 绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
			slideTop += SPEEN_DISTANCE;
			if (slideTop >= frame.bottom) {
				slideTop = frame.top;
			}

			qrDst = new Rect(frame.left, slideTop, frame.right, slideTop + qrLineHeight);
			canvas.drawBitmap(qrLineBitmap, qrSrc, qrDst, null);

			paint.setColor(frameColor);
			// draw rect

			// top-left vertical-line
			canvas.drawRect(frame.left, frame.top, frameLineWidth + frame.left, frameLineLength + frameLineWidth
					+ frame.top, paint);
			// top-left horizontal-line
			canvas.drawRect(frame.left + frameLineWidth, frame.top, frameLineLength + frameLineWidth + frame.left,
					(frameLineWidth + frame.top), paint);
			// top-right vertical-line
			canvas.drawRect(frame.right - frameLineWidth, frame.top, frame.right, frame.top + frameLineWidth
					+ frameLineLength, paint);
			// top-right horizontal-line
			canvas.drawRect(frame.right - frameLineWidth - frameLineLength, frame.top, frame.right - frameLineWidth,
					frameLineWidth + frame.top, paint);

			// bottom-left vertical-line
			canvas.drawRect(frame.left, frame.bottom - frameLineLength - frameLineWidth, frameLineWidth + frame.left,
					frame.bottom, paint);
			// bottom-left horizontal-line
			canvas.drawRect(frameLineWidth + frame.left, frame.bottom - frameLineWidth, frame.left + frameLineLength
					+ frameLineWidth, frame.bottom, paint);
			// bottom-right vertical-line
			canvas.drawRect(frame.right - frameLineWidth, frame.bottom - frameLineLength - frameLineWidth, frame.right,
					frame.bottom, paint);
			canvas.drawRect(frame.right - frameLineWidth - frameLineLength, frame.bottom - frameLineWidth, frame.right
					- frameLineWidth, frame.bottom, paint);

			Collection<ResultPoint> currentPossible = possibleResultPoints;
			Collection<ResultPoint> currentLast = lastPossibleResultPoints;
			if (currentPossible.isEmpty()) {
				lastPossibleResultPoints = null;
			} else {
				possibleResultPoints = new HashSet<ResultPoint>(5);
				lastPossibleResultPoints = currentPossible;
				paint.setAlpha(OPAQUE);
				paint.setColor(resultPointColor);
				for (ResultPoint point : currentPossible) {
					canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
				}
			}
			if (currentLast != null) {
				paint.setAlpha(OPAQUE / 2);
				paint.setColor(resultPointColor);
				for (ResultPoint point : currentLast) {
					canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
				}
			}

			// Request another update at the animation interval, but only
			// repaint the laser line,
			// not the entire viewfinder mask.
			postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
		}
	}

	public void drawViewfinder() {
		resultBitmap = null;
		invalidate();
	}

	/**
	 * Draw a bitmap with the result points highlighted instead of the live
	 * scanning display.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		possibleResultPoints.add(point);
	}

}
