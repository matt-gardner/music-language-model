package org.gardner.music

import org.gardner.util.FileUtil

import scala.collection.JavaConverters._
import scala.collection.mutable

import scala.util.Random

object MusicReader {
  def other_method(x: Int, y: Int): Int = {
    x + y
  }

  val A_val = 'A'.toInt
  val G_val = 'G'.toInt
  val maxOctave = '9'.toInt
  val minOctave = '0'.toInt

  def isNote(string: String): Boolean = {
    if (string.size < 2 || string.size > 3) {
      return false
    }
    if (string(0).toInt < A_val && string(0).toInt > G_val) {
      return false
    }
    if (string.last.toInt < minOctave || string.last.toInt > maxOctave) {
      return false
    }
    return true
  }

  def getNotesFromLines(lines: Seq[String]): Seq[String] = {
    val notes = new mutable.ListBuffer[String]
    for (line <- lines) {
      val fields = line.split(" ")
      if (fields.size > 0 && isNote(fields(0))) {
        notes += fields(0)
      }
    }
    notes.toSeq
  }

  def getCountsFromNotes(notes: Seq[String]): Map[String, Map[String, Int]] = {
    val seq = "<start>" +: notes :+ "<stop>"
    val counts = new mutable.HashMap[String, mutable.Map[String, Int]]

    for (i <- 1 until seq.size) {
      val bigram = (seq(i-1), seq(i))
      val key = bigram._1
      val value = counts.getOrElseUpdate(key, new mutable.HashMap())
      val oldCount = value.getOrElse(bigram._2, 0)
      val newCount = oldCount + 1
      value(bigram._2) = newCount
    }
    counts.mapValues(_.toMap).toMap
  }

  val random = new Random

  def pickNote(counts: Map[String, Map[String, Int]], current_note: String): String = {
    val distribution = counts(current_note)
    val total = distribution.map(_._2).sum
    var current_weight = random.nextDouble * total
    for (entry <- distribution) {
      val note = entry._1
      val count = entry._2
      if (current_weight < count) {
        return note
      } else {
        current_weight -= count
      }
    }
    throw new RuntimeException("There's a bug!")
  }

  def sampleFromCounts(counts: Map[String, Map[String, Int]]): Seq[String] = {
    val notes = new mutable.ListBuffer[String]
    var current_note = "<start>"
    while (current_note != "<stop>") {
      current_note = pickNote(counts, current_note)
      notes += current_note
    }
    notes.toSeq
  }

  def main(args: Array[String]) {
    val fileUtil = new FileUtil
    val lines = fileUtil.readLinesFromFile("stage2/01/01").asScala
    val notes = getNotesFromLines(lines)
    println(notes)
    val counts = getCountsFromNotes(notes)
    println(counts("G5"))
    val generated = sampleFromCounts(counts)
    println(generated)
  }
}
