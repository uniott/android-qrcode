package com.uniott.zxing;

import java.util.HashMap;

import android.os.Bundle;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.uniott.zxing.encode.QRCodeStyle;

public class QRCodeSettings {
	private String type;// 类型
	private String data;// url数据
	private BarcodeFormat format;// 二维码类型
	private int size;// 二维码尺寸
	private HashMap<EncodeHintType, Object> hints;// 二维码参数设置
	private Bundle extra;// 额外的数据
	private HashMap<QRCodeStyle, Object> styles;// 样式

	public QRCodeSettings(String type, String data) {
		this.type = type;
		this.data = data;
	}

	public HashMap<EncodeHintType, Object> getHints() {
		if (hints == null) {
			hints = new HashMap<>();
		}
		return hints;
	}

	public void setHints(HashMap<EncodeHintType, Object> hints) {
		this.hints = hints;
	}

	public void addHint(EncodeHintType type, Object value) {
		if (hints == null) {
			hints = new HashMap<>();
		}
		hints.put(type, value);
	}

	public HashMap<QRCodeStyle, Object> getStyles() {
		if (this.styles == null) {
			styles = new HashMap<>();
		}
		return styles;
	}

	public void addStyle(QRCodeStyle style, Object value) {
		if (this.styles == null) {
			this.styles = new HashMap<>();
		}
		this.styles.put(style, value);
	}

	public void setStyles(HashMap<QRCodeStyle, Object> styles) {
		this.styles = styles;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public BarcodeFormat getFormat() {
		return format;
	}

	public void setFormat(BarcodeFormat format) {
		this.format = format;
	}

	public Bundle getExtra() {
		return extra;
	}

	public void setExtra(Bundle extra) {
		this.extra = extra;
	}
}
