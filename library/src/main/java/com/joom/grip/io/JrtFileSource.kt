package com.joom.grip.io

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

class JrtFileSource(private val jrtPath: Path) : FileSource {

  override fun listFiles(callback: (name: String, type: FileSource.EntryType) -> Unit) {
    Files.walk(jrtPath)
      .filter { "class" == it.extension }
      .forEach {
        callback(jrtPath.relativize(it).toString(), FileSource.EntryType.CLASS)
      }
  }

  override fun readFile(path: String): ByteArray {
    return Files.readAllBytes(jrtPath.resolve(path))
  }

  override fun close() {
  }
}
