package com.netviewtech.nvaudiorecordtest.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.R.integer;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {

	/**
	 * An array of sample (dummy) items.
	 */
	public static List<DummyItem> ITEMS = new ArrayList<DummyItem>();

	/**
	 * A map of sample (dummy) items, by ID.
	 */
	public static Map<String, DummyItem> ITEM_MAP = new HashMap<String, DummyItem>();

	static {
		// Add 3 sample items.
		addItem(new DummyItem("8000_1", "8K Hz, 320B", 8000, 320));
		addItem(new DummyItem("8000_2", "8K Hz, 640B", 8000, 640));
		addItem(new DummyItem("8000_3", "8K Hz, 1024B", 8000, 1024));
		addItem(new DummyItem("8000_4", "8K Hz, 1600B", 8000, 1600));
		addItem(new DummyItem("8000_5", "8K Hz, 2048B", 8000, 2048));
		addItem(new DummyItem("16000_1", "16K Hz, 640B", 16000, 640));
		addItem(new DummyItem("16000_2", "16K Hz, 1024B", 16000, 1024));
		addItem(new DummyItem("16000_3", "16K Hz, 1600B", 16000, 1600));
		addItem(new DummyItem("16000_4", "16K Hz, 2048B", 16000, 2048));
		addItem(new DummyItem("32000_1", "32K Hz, 2048B", 32000, 2048));
		addItem(new DummyItem("32000_2", "32K Hz, 4096B", 32000, 4096));
		addItem(new DummyItem("44100_1", "44.1K Hz, 2048B", 44100, 2048));
		addItem(new DummyItem("44100_2", "44.1K Hz, 4096B", 44100, 4096));
		addItem(new DummyItem("48000_1", "48K Hz, 2048B", 48000, 2048));
		addItem(new DummyItem("48000_2", "48K Hz, 4096B", 48000, 4096));
		// addItem(new DummyItem("0", "user-defined"));
	}

	private static void addItem(DummyItem item) {
		ITEMS.add(item);
		ITEM_MAP.put(item.id, item);
	}

	/**
	 * A dummy item representing a piece of content.
	 */
	public static class DummyItem {
		public String id;
		public String content;
		public int sampleRate;
		public int inputBufferSize;

		public DummyItem(String id, String content, int sampleRate,
				int inputBufferSize) {
			this.id = id;
			this.content = content;
			this.sampleRate = sampleRate;
			this.inputBufferSize = inputBufferSize;
		}

		@Override
		public String toString() {
			return content;
		}
	}
}
