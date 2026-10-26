package dev.yeetle.classicskyblock.common.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.NamespacedKey;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Manages <code>savedata.json</code>, the single file that contains all saved data.
 */
public class SaveDataFileManager extends SaveFileManager<SaveData> {
	static final String FILE_NAME = "savedata.json";

	public SaveDataFileManager(Path dataFolder) {
		super(dataFolder.resolve(FILE_NAME), createGson(), SaveData.class, SaveData::new);
	}

	private static Gson createGson() {
		return new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(NamespacedKey.class, new NamespacedKeyAdapter().nullSafe())
			.create();
	}

	private static class NamespacedKeyAdapter extends TypeAdapter<NamespacedKey> {
		@Override
		public void write(JsonWriter out, NamespacedKey value) throws IOException {
			out.value(value.toString());
		}

		@Override
		public NamespacedKey read(JsonReader in) throws IOException {
			String value = in.nextString();
			NamespacedKey key = NamespacedKey.fromString(value);

			if (key == null) {
				throw new JsonParseException("Invalid namespaced key: " + value);
			}

			return key;
		}
	}
}
