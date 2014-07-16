package com.uniott.zxing.encode;

/**
 * 二维码的附加样式
 * 
 * @author tangyuchun
 * 
 */
public enum QRCodeStyle {
	/**
	 * 中间图像，传入的值应该为Bitmap，生成的中间图片的尺寸不会超过二维码尺寸的1/4
	 */
	CENTER_IMAGE,
	/**
	 * 二维码的背景图片
	 */
	BG_IMAGE,
	/**
	 * 背景色
	 */
	BG_COLOR,
	/**
	 * 背景形状
	 */
	BG_SHARE,
}
