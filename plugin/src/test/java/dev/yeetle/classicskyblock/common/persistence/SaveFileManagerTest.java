package dev.yeetle.classicskyblock.common.persistence;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SaveFileManagerTest {
	private static class Content {
		private String text = "empty";
	}

	@TempDir
	Path folder;

	private Path file;
	private SaveFileManager<Content> manager;

	@BeforeEach
	void setUp() {
		file = folder.resolve("data").resolve("content.json");
		manager = new SaveFileManager<>(file, new Gson(), Content.class, Content::new);
	}

	@Test
	void loadingAMissingFileResultsInEmptyData() {
		manager.load();

		assertEquals("empty", manager.getData().text);
	}

	@Test
	void savedDataCanBeLoadedAgain() {
		manager.load();
		manager.getData().text = "saved";
		manager.save();

		SaveFileManager<Content> other = new SaveFileManager<>(file, new Gson(), Content.class, Content::new);
		other.load();

		assertEquals("saved", other.getData().text);
	}

	@Test
	void savingCreatesMissingFoldersAndLeavesNoTemporaryFile() throws IOException {
		manager.load();
		manager.save();

		assertTrue(Files.exists(file));
		assertFalse(Files.exists(file.resolveSibling("content.json.tmp")));
	}

	@Test
	void savingBeforeLoadingWritesNothing() {
		manager.getData().text = "unloaded";
		manager.save();

		assertFalse(Files.exists(file));
	}

	@Test
	void loadingAnEmptyFileResultsInEmptyData() throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, "");

		manager.load();

		assertEquals("empty", manager.getData().text);
	}

	@Test
	void aCorruptFileThrowsAndIsNeitherOverwrittenNorChanged() throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{ this is not json");

		assertThrows(IllegalStateException.class, manager::load);

		manager.save();

		assertEquals("{ this is not json", Files.readString(file));
	}
}
