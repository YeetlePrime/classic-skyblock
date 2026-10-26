package dev.yeetle.classicskyblock.common.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

/**
 * Reads and writes the data of exactly one json file.
 * The manager knows nothing about the content, it only provides reading and writing.
 *
 * @param <T> the type of the data in the file
 */
public class SaveFileManager<T> {
	private final Path file;
	private final Path tempFile;
	private final Gson gson;
	private final Class<T> dataType;
	private final Supplier<T> emptyData;

	private boolean dataLoaded = false;
	private T data;

	public SaveFileManager(Path file, Gson gson, Class<T> dataType, Supplier<T> emptyData) {
		this.file = file;
		this.tempFile = file.resolveSibling(file.getFileName() + ".tmp");
		this.gson = gson;
		this.dataType = dataType;
		this.emptyData = emptyData;
		this.data = emptyData.get();
	}

	/**
	 * Loads the data from the json file.
	 * A missing file results in empty data.
	 * A corrupt file results in an exception and leaves the file untouched.
	 */
	public void load() {
		if (!Files.exists(file)) {
			data = emptyData.get();
			dataLoaded = true;
			return;
		}

		try {
			T loaded = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), dataType);
			data = loaded != null ? loaded : emptyData.get();
			dataLoaded = true;
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read " + file, e);
		} catch (JsonParseException e) {
			throw new IllegalStateException("Could not parse " + file, e);
		}
	}

	/**
	 * Writes the data to the json file.
	 * The data is written to a temporary file first, so a failed write does not destroy the existing file.
	 * Nothing is written as long as the data was not loaded, so a failed load cannot overwrite the file.
	 */
	public void save() {
		if (!dataLoaded) {
			return;
		}

		try {
			Files.createDirectories(file.getParent());
			Files.writeString(tempFile, gson.toJson(data), StandardCharsets.UTF_8);
			Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write " + file, e);
		}
	}

	public T getData() {
		return data;
	}
}
