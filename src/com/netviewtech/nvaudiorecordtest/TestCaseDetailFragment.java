package com.netviewtech.nvaudiorecordtest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.R.integer;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.netviewtech.android.media.NetviewCodec;
import com.netviewtech.nvaudiorecordtest.dummy.DummyContent;

/**
 * A fragment representing a single TestCase detail screen. This fragment is
 * either contained in a {@link TestCaseListActivity} in two-pane mode (on
 * tablets) or a {@link TestCaseDetailActivity} on handsets.
 */
public class TestCaseDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	/**
	 * The dummy content this fragment is presenting.
	 */
	private DummyContent.DummyItem mItem;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public TestCaseDetailFragment() {
	}

	private static final String TAG = TestCaseDetailFragment.class
			.getSimpleName();

	private Button recordButton;
	private Button playButton;
	private EditText logEditText;
	private EditText inputBufferEditText;
	private RadioButton pcmRadioButton;
	private RadioButton aacRadioButton;
	private CheckBox inputBufferCheckBox;

	static enum TEST_STATE {
		READY, RECORDING, RECORDED, PLAYING, PLAYED
	}

	private volatile TEST_STATE state = TEST_STATE.READY;

	private String filePathPCM;
	private String filePathAAC;
	private FileOutputStream osPCM;
	private FileOutputStream osAAC;
	private FileInputStream is;

	static enum TEST_RESULT {
		UNKNOWN, GOOD, BAD
	}

	private TEST_RESULT result = TEST_RESULT.UNKNOWN;
	private int sampleRate = 8000;
	private AudioRecord audioRecord = null;
	private AudioTrack audioTrack = null;
	private MediaPlayer mediaPlayer = null;
	private int bufferSize = 8192;
	private NetviewCodec codec = new NetviewCodec();

	private static final int PCM_FRAME_BUFFER_SIZE = 2048;
	private int AAC_FRAME_BUFFER_SIZE = 2048;

	private long startTime = 0l;
	private long pcmCount = 0l;
	private long aacCount = 0l;
	private long readCount = 0l;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.
			mItem = DummyContent.ITEM_MAP.get(getArguments().getString(
					ARG_ITEM_ID));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_testcase_detail,
				container, false);

		// Show the dummy content as text in a TextView.
		if (mItem != null) {
			((TextView) rootView.findViewById(R.id.textView_title))
					.setText("TestCase: " + mItem.content);
			sampleRate = mItem.sampleRate;
			AAC_FRAME_BUFFER_SIZE = mItem.inputBufferSize;
			Log.d(TAG, "Init SampleRate:" + sampleRate);
			Log.d(TAG, "Init InputBuffer:" + AAC_FRAME_BUFFER_SIZE);
		}

		recordButton = (Button) rootView
				.findViewById(R.id.button_record_action);
		playButton = (Button) rootView.findViewById(R.id.button_play_action);
		logEditText = (EditText) rootView.findViewById(R.id.editText_log);
		inputBufferEditText = (EditText) rootView
				.findViewById(R.id.editText_input_buffer);
		pcmRadioButton = (RadioButton) rootView
				.findViewById(R.id.radioButton_pcm);
		aacRadioButton = (RadioButton) rootView
				.findViewById(R.id.radioButton_aac);
		inputBufferCheckBox = (CheckBox) rootView
				.findViewById(R.id.checkBox_buffer_fixed);

		pcmRadioButton.setChecked(true);
		updateState(TEST_STATE.READY);
		logEditText.setText("Inited:" + System.currentTimeMillis() + "\n");
		// logEditText.setEnabled(false);
		inputBufferEditText.setText(String.valueOf(AAC_FRAME_BUFFER_SIZE));
		inputBufferCheckBox.setChecked(false);
		setEvents();

		return rootView;
	}

	public void setEvents() {
		recordButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (recordButton.getText().equals(
						getString(R.string.action_start))) {

					if (!validSampleRate(sampleRate)) {
						Log.d(TAG, "Failed to validate sample rate:"
								+ sampleRate);
						result = TEST_RESULT.BAD;
						return;
					}

					final boolean fixed = inputBufferCheckBox.isChecked();
					Log.d(TAG, "Input buffer size is"
							+ (fixed ? " Fixed." : " Not fixed."));

					String path = Environment.getExternalStorageDirectory()
							.getPath() + "/netvue/debug/audiorecord/";
					File file = new File(path);
					if (!file.exists()) {
						if (!file.mkdirs()) {
							Log.d(TAG, "failed to mkdir:" + path);
							Toast.makeText(getActivity(),
									"Failed to create file..exit..", 5).show();
							return;
						}
					}
					path = path + System.currentTimeMillis() + "_" + sampleRate
							+ "_" + AAC_FRAME_BUFFER_SIZE
							+ (fixed ? "_fixed" : "_non-fix");
					filePathPCM = path + ".pcm";
					filePathAAC = path + ".aac";
					try {
						osPCM = new FileOutputStream(filePathPCM);
						osAAC = new FileOutputStream(filePathAAC);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						Log.d(TAG, "failed to open file:" + filePathPCM);
						return;
					}

					logEditText.append("Start record.\n");
					logEditText.append("BufferSize:" + AAC_FRAME_BUFFER_SIZE
							+ ",\n");
					logEditText.append("Output0:" + filePathPCM + "\n");
					logEditText.append("Output1:" + filePathAAC + "\n");

					updateState(TEST_STATE.RECORDING);

					new Thread(new Runnable() {

						@Override
						public void run() {
							if (osPCM != null && audioRecord != null) {
								byte[] buffer = new byte[AAC_FRAME_BUFFER_SIZE];
								byte[] fixedBuffer = new byte[AAC_FRAME_BUFFER_SIZE * 2];
								int offset = 0;
								codec.aacEncFinish();
								codec.aacEncInit(sampleRate, 1);

								audioRecord.startRecording();

								startTime = System.currentTimeMillis();
								pcmCount = 0l;
								aacCount = 0l;
								readCount = 0l;

								while (state == TEST_STATE.RECORDING) {
									int read = audioRecord.read(buffer, 0,
											AAC_FRAME_BUFFER_SIZE);
									Log.d(TAG, "PCM Read:" + read);
									if (read > 0) {
										readCount++;
										pcmCount += read;
										try {
											osPCM.write(buffer, 0, read);
											byte[] aac = null;
											if (!fixed
													|| (offset == 0 && read == AAC_FRAME_BUFFER_SIZE)) {
												aac = codec
														.aacEncOneFrame(buffer);
											} else if (offset + read < AAC_FRAME_BUFFER_SIZE) {
												System.arraycopy(buffer, 0,
														fixedBuffer, offset,
														read);
												offset += read;
												continue;
											} else {
												int toCopySize = AAC_FRAME_BUFFER_SIZE
														- offset;
												System.arraycopy(buffer, 0,
														fixedBuffer, offset,
														toCopySize);
												aac = codec
														.aacEncOneFrame(fixedBuffer);
												offset = read - toCopySize;
												System.arraycopy(buffer,
														toCopySize,
														fixedBuffer, 0, offset);
											}

											if (aac != null) {
												Log.d(TAG, "AAC encoded:"
														+ aac.length);
												aacCount += aac.length;
												osAAC.write(aac);
											}
										} catch (IOException e) {
											e.printStackTrace();
											break;
										}
									}
								}

								startTime = System.currentTimeMillis()
										- startTime;
								if (audioRecord != null) {
									audioRecord.stop();
									audioRecord.release();
									audioRecord = null;
								}
								codec.aacEncFinish();
								if (osPCM != null) {
									try {
										osPCM.flush();
										osPCM.close();
										osPCM = null;
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
								if (osAAC != null) {
									try {
										osAAC.flush();
										osAAC.close();
										osAAC = null;
									} catch (IOException e) {
										e.printStackTrace();
									}
								}

								getActivity().runOnUiThread(new Runnable() {

									@Override
									public void run() {

										logEditText.append(String
												.format("Time:%dms, PCM:%.2fKB/s,%.1fB/r AAC:%.3fKB/s,%.1fB/r RC:%.1f r/s\n",
														startTime, pcmCount
																* 1.0f
																/ startTime,
														pcmCount * 1.0f
																/ readCount,
														aacCount * 1.0f
																/ startTime,
														aacCount * 1.0f
																/ readCount,
														readCount * 1000.0f
																/ startTime));
									}
								});
							}
						}
					}).start();
				} else {
					logEditText.append("Stop record.\n");
					updateState(TEST_STATE.RECORDED);
				}
			}
		});

		playButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (playButton.getText().equals(
						getString(R.string.action_start))) {

					if (pcmRadioButton.isChecked()) {
						audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
								sampleRate, AudioFormat.CHANNEL_OUT_MONO,
								AudioFormat.ENCODING_PCM_16BIT, bufferSize,
								AudioTrack.MODE_STREAM);

						try {
							is = new FileInputStream(filePathPCM);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							Log.d(TAG, "failed to open input file:"
									+ filePathPCM);
							return;
						}

						logEditText.append("Start play PCM.\n");
						updateState(TEST_STATE.PLAYING);

						new Thread(new Runnable() {

							@Override
							public void run() {
								if (is != null && audioTrack != null) {
									audioTrack.play();
									byte[] buffer = new byte[PCM_FRAME_BUFFER_SIZE];
									while (state == TEST_STATE.PLAYING) {
										int read;
										try {
											read = is.read(buffer);
										} catch (IOException e) {
											e.printStackTrace();
											break;
										}
										if (read > 0) {
											audioTrack.write(buffer, 0, read);
										}
										if (read < PCM_FRAME_BUFFER_SIZE) {
											break;
										}
									}
									if (audioTrack != null) {
										audioTrack.stop();
										audioTrack.release();
										audioTrack = null;
									}
									if (is != null) {
										try {
											is.close();
										} catch (IOException e) {
											e.printStackTrace();
										}
										is = null;
									}
									if (state == TEST_STATE.PLAYING) {
										getActivity().runOnUiThread(
												new Runnable() {

													@Override
													public void run() {
														updateState(TEST_STATE.PLAYED);
														logEditText
																.append("Finish Play.\n");
													}
												});
									}
									Log.d(TAG, "Finish Play\n");
								}
							}
						}).start();

					} else {
						mediaPlayer = new MediaPlayer();
						mediaPlayer
								.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

									@Override
									public void onCompletion(MediaPlayer mp) {
										mediaPlayer.stop();
										mediaPlayer.release();
										mediaPlayer = null;
										getActivity().runOnUiThread(
												new Runnable() {

													@Override
													public void run() {
														updateState(TEST_STATE.PLAYED);
														logEditText
																.append("Finish Play.\n");
													}
												});
										Log.d(TAG, "Finish Play\n");

									}
								});
						try {
							mediaPlayer.setDataSource(filePathAAC);
							mediaPlayer.prepare();
							mediaPlayer.start();
							updateState(TEST_STATE.PLAYING);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (SecurityException e) {
							e.printStackTrace();
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

						logEditText.append("Start play AAC.\n");
					}

				} else {
					logEditText.append("Stop play.\n");
					updateState(TEST_STATE.PLAYED);
					if (mediaPlayer != null) {
						mediaPlayer.stop();
						mediaPlayer.release();
						mediaPlayer = null;
					}
				}
			}
		});

		inputBufferEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					AAC_FRAME_BUFFER_SIZE = Integer.valueOf(inputBufferEditText
							.getText().toString());
					logEditText.append("ChangeInputBufferTo:"
							+ AAC_FRAME_BUFFER_SIZE + "\n");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	public void updateState(TEST_STATE val) {
		state = val;
		switch (state) {
		case READY:
			recordButton.setText(R.string.action_start);
			recordButton.setEnabled(true);
			playButton.setText(R.string.action_start);
			playButton.setEnabled(false);
			break;
		case RECORDING:
			recordButton.setText(R.string.action_stop);
			recordButton.setEnabled(true);
			playButton.setText(R.string.action_start);
			playButton.setEnabled(false);
			break;
		case RECORDED:
			recordButton.setText(R.string.action_start);
			recordButton.setEnabled(true);
			playButton.setText(R.string.action_start);
			playButton.setEnabled(true);
			break;
		case PLAYING:
			recordButton.setText(R.string.action_start);
			recordButton.setEnabled(false);
			playButton.setText(R.string.action_stop);
			playButton.setEnabled(true);
			break;
		case PLAYED:
			recordButton.setText(R.string.action_start);
			recordButton.setEnabled(true);
			playButton.setText(R.string.action_start);
			playButton.setEnabled(true);
			break;
		default:
			break;
		}
	}

	private boolean validSampleRate(int sample_rate) {
		try {
			bufferSize = AudioRecord
					.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_IN_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					sample_rate, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		} catch (IllegalArgumentException e) {
			return false; // cannot sample at this rate
		}
		return true; // if nothing has been returned yet, then we must be able
						// to sample at this rate!
	}

	@Override
	public void onStop() {
		if (state == TEST_STATE.PLAYING) {
			updateState(TEST_STATE.PLAYED);
		} else if (state == TEST_STATE.RECORDING) {
			updateState(TEST_STATE.RECORDED);
		}
		super.onStop();
	}

}
