package com.britt.lifsplit

import java.nio.ByteBuffer
import java.util

import edf.EdfSobel
import ij.ImageStack
import ij.plugin.ImagesToStack
import imageware.ImageWare
import loci.common.services.ServiceFactory
import loci.formats.{ImageReader, ImageWriter}
import loci.formats.in.LIFReader
import loci.formats.meta.MetadataRetrieve
import loci.formats.services.OMEXMLService
import ome.xml.meta.OMEXMLMetadataRoot
import ome.xml.model.{Image, Pixels}

/**
  * Created by Britt on 3/24/20.
  */
object LifSplitApp extends App {

  println("hello world")
  val x = getClass.getClassLoader.getResource("logback.xml")
  val logs = scala.io.Source.fromInputStream(x.openStream()).getLines().toArray
  println(s"I read ${logs.length} logback config lines")

  val inputFile = "/Users/Britt/lifFiles/bigOne.lif"

  val factory = new ServiceFactory
  val service = factory.getInstance(classOf[OMEXMLService])
  val omexml = service.createOMEXMLMetadata
  val reader = new LIFReader()
  reader.setMetadataStore(omexml)
  reader.setId(inputFile)

  var myStack: ImageStack = _
  for (i <- 0 until 1) {
  //for (i <- 0 until reader.getSeriesCount) {
    reader.setSeries(i)

    // Pull out the old image and make a copy
    val root = omexml.getRoot.asInstanceOf[OMEXMLMetadataRoot]
    val exportImage = new Image(root.getImage(i))
    val exportPixels = new Pixels(root.getImage(i).getPixels)
    exportImage.setPixels(exportPixels)

    // Create an ImageStack?
    myStack = new ImageStack(2048, 2048)


    // Create a new image
    val newRoot = new OMEXMLMetadataRoot()
    newRoot.addImage(exportImage)
    val omemeta2 = service.createOMEXMLMetadata()
    omemeta2.setRoot(newRoot)
    val writer = new ImageWriter()
    writer.setMetadataRetrieve(omemeta2.asInstanceOf[MetadataRetrieve])
    writer.setId("/tmp/someFile.tiff")
    writer.setSeries(0);
    writer.setWriteSequentially(true);
    for (n <- 0 until reader.getImageCount) {
      println(s"Pixel type is ${reader.getPixelType}")
      val buf = ByteBuffer.allocate(2048*2048*2)
      if (!buf.hasArray) println("Shit")
      reader.openBytes(n, buf.array())
      buf.rewind()
      //val buf = ByteBuffer.wrap(bytes)
      //println(s"Array length is ${bytes.length}, compared to ${2048*2048}")
      val myShortArray = new Array[Short](2048*2048)
      buf.asShortBuffer().get(myShortArray)
      myStack.addSlice(n.toString, myShortArray)
      println(i,n)
      writer.saveBytes(n, buf.array())
    }
    writer.close()
  }
  reader.close()

  // Down here I've built the image stack
  println("About to run edf")
  println(s"RGB: ${myStack.isRGB}")
  val iw = imageware.Builder.create(myStack)
  val edf = new EdfSobel()
  val out = edf.process(iw)
  println(s"Done, out has size ${out.length}")
}
