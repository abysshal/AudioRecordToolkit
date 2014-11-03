package com.netviewtech.android.media;

public class NetviewCodec {
	public NetviewCodec() {
		System.loadLibrary("CPUneon");

		int ifSupportNeon = CPUdecision();

		if (ifSupportNeon == 1) {
			System.loadLibrary("vo-aacenc");
			System.loadLibrary("ffmpeg");
			System.loadLibrary("videokit");
			System.loadLibrary("codec");
		} else {
			System.loadLibrary("vo-aacenc_noNeon");
			System.loadLibrary("ffmpeg_noNeon");
			System.loadLibrary("videokit_noNeon");
			System.loadLibrary("codec_noNeon");
		}
	}

	public static class AACDecodeResult {
		public int samplerate;
		public int channel;
		public byte[] data;
	}

	public static class H264EncodeResult {
		public boolean keyFrame;
		public int curPacket;
		public byte[] data;
	}

	public native int CPUdecision();

	// public native void runFFMPEG(String[] args);
	//
	// public native int h264EncInit(int profile);
	// public native H264EncodeResult h264EncOneFrame(byte[] frame,
	// H264EncodeResult result);
	// public native H264EncodeResult h264EncRemaining(H264EncodeResult result);
	// public native void h264EncFinish();
	//
	// public native int h264DecInit(int width, int height);
	// public native int h264DecOneFrame(byte[] in, int nalLen, byte[] out);
	// public native void h264DecFinish();
	//
	public native int aacEncInit(int samplerate, int channel);

	public native byte[] aacEncOneFrame(byte[] frame);

	public native void aacEncFinish();

	public native int aacDecInit();

	public native int aacDecOneFrame(byte[] frame, AACDecodeResult result);

	public native void aacDecFinish();
}
