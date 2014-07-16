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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.uniott.zxing.Contents;
import com.uniott.zxing.QRCodeSettings;

public final class QRCodeEncoder {

	private static final String TAG = QRCodeEncoder.class.getSimpleName();

	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;

	private String contents;
	private String displayContents;
	private QRCodeSettings qrCode;

	QRCodeEncoder(QRCodeSettings code) throws WriterException {
		this.qrCode = code;
		if (!encodeContentsFromQRCode()) {
		}
	}

	public String getContents() {
		return contents;
	}

	public String getDisplayContents() {
		return displayContents;
	}

	private boolean encodeContentsFromQRCode() {
		if (TextUtils.isEmpty(qrCode.getType())) {
			return false;
		}
		BarcodeFormat format = qrCode.getFormat();
		if (format == null) {
			qrCode.setFormat(BarcodeFormat.QR_CODE);
		}
		encodeQRCodeContents();
		return !TextUtils.isEmpty(qrCode.getData());
	}

	private void encodeQRCodeContents() {
		String data = qrCode.getData();
		switch (qrCode.getType()) {
		case Contents.Type.TEXT: {
			if (!TextUtils.isEmpty(data)) {
				contents = data;
				displayContents = data;
			}
			break;
		}
		case Contents.Type.EMAIL: {
			if (data != null) {
				contents = "mailto:" + data;
				displayContents = data;
			}
			break;
		}
		case Contents.Type.PHONE: {
			if (data != null) {
				contents = "tel:" + data;
				displayContents = PhoneNumberUtils.formatNumber(data);
			}
			break;
		}
		case Contents.Type.SMS: {
			if (data != null) {
				contents = "sms:" + data;
				displayContents = PhoneNumberUtils.formatNumber(data);
			}
			break;
		}
		case Contents.Type.CONTACT: {
			Bundle bundle = qrCode.getExtra();
			if (bundle != null) {
				String name = bundle.getString(ContactsContract.Intents.Insert.NAME);
				String organization = bundle.getString(ContactsContract.Intents.Insert.COMPANY);
				String address = bundle.getString(ContactsContract.Intents.Insert.POSTAL);
				List<String> phones = getAllBundleValues(bundle, Contents.PHONE_KEYS);
				List<String> phoneTypes = getAllBundleValues(bundle, Contents.PHONE_TYPE_KEYS);
				List<String> emails = getAllBundleValues(bundle, Contents.EMAIL_KEYS);
				String url = bundle.getString(Contents.URL_KEY);
				List<String> urls = url == null ? null : Collections.singletonList(url);
				String note = bundle.getString(Contents.NOTE_KEY);

				// ContactEncoder encoder = useVCard ? new VCardContactEncoder()
				// : new MECARDContactEncoder();
				ContactEncoder encoder = new MECARDContactEncoder();
				String[] encoded = encoder.encode(Collections.singletonList(name), organization,
						Collections.singletonList(address), phones, phoneTypes, emails, urls, note);
				// Make sure we've encoded at least one field.
				if (!TextUtils.isEmpty(encoded[1])) {
					contents = encoded[0];
					displayContents = encoded[1];
				}
			}
			break;
		}
		case Contents.Type.LOCATION: {
			Bundle bundle = qrCode.getExtra();
			if (bundle != null) {
				float latitude = bundle.getFloat("LAT", Float.MAX_VALUE);
				float longitude = bundle.getFloat("LONG", Float.MAX_VALUE);
				if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
					contents = "geo:" + latitude + ',' + longitude;
					displayContents = latitude + "," + longitude;
				}
			}
			break;
		}
		}
	}

	private static List<String> getAllBundleValues(Bundle bundle, String[] keys) {
		List<String> values = new ArrayList<>(keys.length);
		for (String key : keys) {
			Object value = bundle.get(key);
			values.add(value == null ? null : value.toString());
		}
		return values;
	}

	BitMatrix encodeAsMatrix() throws WriterException {
		String contentsToEncode = contents;
		if (contentsToEncode == null) {
			return null;
		}
		// 校正编码
		Map<EncodeHintType, Object> hints = qrCode.getHints();
		if (!hints.containsKey(EncodeHintType.CHARACTER_SET)) {
			String encoding = guessAppropriateEncoding(contentsToEncode);
			if (encoding != null) {
				hints.put(EncodeHintType.CHARACTER_SET, encoding);
			} else {
				hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			}
		}
		BitMatrix result = new MultiFormatWriter().encode(contentsToEncode, qrCode.getFormat(), qrCode.getSize(),
				qrCode.getSize(), hints);
		return result;
	}

	Bitmap encodeAsBitmap() throws WriterException {
		String contentsToEncode = contents;
		if (contentsToEncode == null) {
			return null;
		}
		// 校正编码
		Map<EncodeHintType, Object> hints = qrCode.getHints();
		if (!hints.containsKey(EncodeHintType.CHARACTER_SET)) {
			String encoding = guessAppropriateEncoding(contentsToEncode);
			if (encoding != null) {
				hints.put(EncodeHintType.CHARACTER_SET, encoding);
			} else {
				hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			}
		}

		BitMatrix result;
		try {
			result = new MultiFormatWriter().encode(contentsToEncode, qrCode.getFormat(), qrCode.getSize(),
					qrCode.getSize(), hints);
		} catch (IllegalArgumentException iae) {
			// Unsupported format
			return null;
		}
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

		// recycle resource
		pixels = null;
		return bitmap;
	}

	private static String guessAppropriateEncoding(CharSequence contents) {
		// Very crude at the moment
		for (int i = 0; i < contents.length(); i++) {
			if (contents.charAt(i) > 0xFF) {
				return "UTF-8";
			}
		}
		return null;
	}

}
