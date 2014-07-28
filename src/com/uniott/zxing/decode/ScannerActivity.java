package com.uniott.zxing.decode;

import java.io.IOException;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.uniott.zxing.R;
import com.uniott.zxing.camera.CameraManager;
import com.uniott.zxing.view.ViewfinderResultPointCallback;
import com.uniott.zxing.view.ViewfinderView;

public class ScannerActivity extends Activity implements Callback {
	private static final String TAG = ScannerActivity.class.getSimpleName();

	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 1.0f;
	private boolean vibrate;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_scan);

		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);

		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	public void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	/**
	 * @param result
	 * @param barcode
	 */
	public void handleDecode(Result result, Bitmap barcode) {
		inactivityTimer.onActivity();
		playBeepSoundAndVibrate();

		final String resultString = result.getText();
		Intent resultIntent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putString("result", resultString);
		resultIntent.putExtras(bundle);
		this.setResult(RESULT_OK, resultIntent);

		Toast.makeText(this, resultString, Toast.LENGTH_SHORT).show();
		Log.e("QRCode", resultString);
		// finish();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(decodeFormats, characterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();

	}

	/**
	 * 扫描正确后的震动声音,如果感觉apk大了,可以删除
	 */
	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.qrcode_completed);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};
	private State state;

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	class CaptureActivityHandler extends Handler {
		private final DecodeThread decodeThread;

		public CaptureActivityHandler(Vector<BarcodeFormat> decodeFormats, String characterSet) {
			decodeThread = new DecodeThread(this, decodeFormats, characterSet, new ViewfinderResultPointCallback(
					getViewfinderView()));
			decodeThread.start();
			state = State.SUCCESS;
			// Start ourselves capturing previews and decoding.
			CameraManager.get().startPreview();
			restartPreviewAndDecode();
		}

		@Override
		public void handleMessage(Message message) {
			if (message.what == R.id.auto_focus && state == State.PREVIEW) {
				CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
			} else if (message.what == R.id.restart_preview) {
				restartPreviewAndDecode();
			} else if (message.what == R.id.decode_succeeded) {
				Log.d(TAG, "Got decode succeeded message");
				state = State.SUCCESS;
				Bundle bundle = message.getData();

				/***********************************************************************/
				Bitmap barcode = bundle == null ? null : (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);// ���ñ����߳�

				handleDecode((Result) message.obj, barcode);// ���ؽ��
			} else if (message.what == R.id.decode_failed) {
				// We're decoding as fast as possible, so when one decode fails,
				// start another.
				state = State.PREVIEW;
				CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
			} else if (message.what == R.id.return_scan_result) {
				Log.d(TAG, "Got return scan result message");
				setResult(Activity.RESULT_OK, (Intent) message.obj);
				finish();
			} else if (message.what == R.id.launch_product_query) {
				Log.d(TAG, "Got product query message");
				String url = (String) message.obj;
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(intent);
			}
			// switch (message.what) {
			// case R.id.auto_focus:
			// // Log.d(TAG, "Got auto-focus message");
			// // When one auto focus pass finishes, start another. This is the
			// // closest thing to
			// // continuous AF. It does seem to hunt a bit, but I'm not sure
			// // what else to do.
			// if (state == State.PREVIEW) {
			// CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
			// }
			// break;
			// case R.id.restart_preview:
			// Log.d(TAG, "Got restart preview message");
			// restartPreviewAndDecode();
			// break;
			// case R.id.decode_succeeded:
			// Log.d(TAG, "Got decode succeeded message");
			// state = State.SUCCESS;
			// Bundle bundle = message.getData();
			//
			// /***********************************************************************/
			// Bitmap barcode = bundle == null ? null :
			// (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);//
			// ���ñ����߳�
			//
			// activity.handleDecode((Result) message.obj, barcode);// ���ؽ��
			// /***********************************************************************/
			// break;
			// case R.id.decode_failed:
			// // We're decoding as fast as possible, so when one decode fails,
			// // start another.
			// state = State.PREVIEW;
			// CameraManager.get().requestPreviewFrame(decodeThread.getHandler(),
			// R.id.decode);
			// break;
			// case R.id.return_scan_result:
			// Log.d(TAG, "Got return scan result message");
			// activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
			// activity.finish();
			// break;
			// case R.id.launch_product_query:
			// Log.d(TAG, "Got product query message");
			// String url = (String) message.obj;
			// Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			// activity.startActivity(intent);
			// break;
			// }
		}

		public void quitSynchronously() {
			state = State.DONE;
			CameraManager.get().stopPreview();
			Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
			quit.sendToTarget();
			try {
				decodeThread.join();
			} catch (InterruptedException e) {
				// continue
			}

			// Be absolutely sure we don't send any queued up messages
			removeMessages(R.id.decode_succeeded);
			removeMessages(R.id.decode_failed);
		}

		private void restartPreviewAndDecode() {
			if (state == State.SUCCESS) {
				state = State.PREVIEW;
				CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
				CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
				drawViewfinder();
			}
		}

	}
}
