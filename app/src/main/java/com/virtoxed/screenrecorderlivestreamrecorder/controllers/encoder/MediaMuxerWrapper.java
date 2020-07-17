package com.virtoxed.screenrecorderlivestreamrecorder.controllers.encoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;

import com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils.DEBUG;

public class MediaMuxerWrapper {
	private static final String TAG = MediaMuxerWrapper.class.getSimpleName();

	private String mOutputPath = "";
	private final MediaMuxer mMediaMuxer;	// API >= 18
	private int mEncoderCount, mStatredCount;
	private boolean mIsStarted;
	private volatile boolean mIsPaused;
	private MediaEncoder mVideoEncoder, mAudioEncoder;

	/**
	 * Constructor
	 * @param _ext extension of output file
	 * @throws IOException
	 */
	public MediaMuxerWrapper(final Context context, final String _ext) throws IOException {
		String ext = _ext;
		if (TextUtils.isEmpty(ext)) ext = ".mp4";
		try {
			File outputFile = new File(MyUtils.getBaseStorageDirectory(), MyUtils.createFileName(_ext));

			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}
			mOutputPath = outputFile.getAbsolutePath();
		}catch (final NullPointerException e) {
			throw new RuntimeException("This app has no permission of writing external storage");
		}

		mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		mEncoderCount = mStatredCount = 0;
		mIsStarted = false;
	}


	public String getOutputPath() {
		return mOutputPath;
	}

	public synchronized void prepare() throws IOException {
		if (mVideoEncoder != null)
			mVideoEncoder.prepare();
		if (mAudioEncoder != null)
			mAudioEncoder.prepare();
	}

	public synchronized void startRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.startRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.startRecording();
	}

	public synchronized void stopRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.stopRecording();
		mVideoEncoder = null;
		if (mAudioEncoder != null)
			mAudioEncoder.stopRecording();
		mAudioEncoder = null;
	}

	public synchronized boolean isStarted() {
		return mIsStarted;
	}

	public synchronized void pauseRecording() {
		mIsPaused = true;
		if (mVideoEncoder != null)
			mVideoEncoder.pauseRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.pauseRecording();
	}

	public synchronized void resumeRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.resumeRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.resumeRecording();
		mIsPaused = false;
	}

	public synchronized boolean isPaused() {
		return mIsPaused;
	}

//**********************************************************************
//**********************************************************************
	/**
	 * assign encoder to this calss. this is called from encoder.
	 * @param encoder instance of MediaVideoEncoderBase
	 */
	/*package*/ void addEncoder(final MediaEncoder encoder) {
		if (encoder instanceof MediaVideoEncoderBase) {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mVideoEncoder = encoder;
		} else if (encoder instanceof MediaAudioEncoder) {
			if (mAudioEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mAudioEncoder = encoder;
		} else
			throw new IllegalArgumentException("unsupported encoder");
		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}

	/**
	 * request start recording from encoder
	 * @return true when muxer is ready to write
	 */
	/*package*/ synchronized boolean start() {
		if (DEBUG) Log.i(TAG,  "start:");
		mStatredCount++;
		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
			mMediaMuxer.start();
			mIsStarted = true;
			notifyAll();
			if (DEBUG) Log.i(TAG,  "MediaMuxer started:");
		}
		return mIsStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	*/
	/*package*/ synchronized void stop() {
		if (DEBUG) Log.i(TAG,  "stop:mStatredCount=" + mStatredCount);
		mStatredCount--;
		if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
			mMediaMuxer.stop();
			mMediaMuxer.release();
			mIsStarted = false;
			if (DEBUG) Log.i(TAG,  "MediaMuxer stopped:");
		}
	}

	/**
	 * assign encoder to muxer
	 * @param format
	 * @return minus value indicate error
	 */
	/*package*/ synchronized int addTrack(final MediaFormat format) {
		if (mIsStarted)
			throw new IllegalStateException("muxer already started");
		final int trackIx = mMediaMuxer.addTrack(format);
		if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
		return trackIx;
	}

	/**
	 * write encoded data to muxer
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/*package*/
	public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if (mStatredCount > 0)
			mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
	}


}
