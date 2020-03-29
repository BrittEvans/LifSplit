package com.britt.lifsplit

import ij.{IJ, ImagePlus}

/**
  * Created by Britt on 3/24/20.
  */
object LifSplitApp extends App {

  println("hello world")
  val x = getClass.getClassLoader.getResource("logback.xml")
  val logs = scala.io.Source.fromInputStream(x.openStream()).getLines().toArray
  println(s"I read ${logs.length} logback config lines")

  val inputFile = "/Users/Britt/lifFiles/bigOne.lif"
  //val inputFile = "/Volumes/Seagate Portable Drive/Britt/40X 3470 LC - UnMerged Tile - FULL DEPTH.lif"
  //val inputFile = "/Volumes/Seagate Portable Drive/Britt/40X 3470 LC - UnMerged Tile.lif"

  val reader = LifUtils.getReader(inputFile)
  val rawStack = LifUtils.extractStack(reader, 43, 0)

  IJ.saveAsTiff(new ImagePlus("Raw", rawStack), "/tmp/raw3.tiff")
  LifUtils.doEDOF(rawStack, "/tmp/edof3.tiff")
  reader.close()

}
