package com.britt.lifsplit

import java.nio.ByteBuffer

import edf.EdfComplexWavelets
import edfgui.Parameters
import ij.{IJ, ImagePlus, ImageStack}
import loci.common.services.ServiceFactory
import loci.formats.in.LIFReader
import loci.formats.services.OMEXMLService

object LifUtils {

  def getReader(filename: String): LIFReader = {
    val factory = new ServiceFactory
    val service = factory.getInstance(classOf[OMEXMLService])
    val omexml = service.createOMEXMLMetadata
    val reader = new LIFReader()
    reader.setMetadataStore(omexml)
    reader.setId(filename)
    reader
  }

  def extractStack(reader: LIFReader, series: Int, channel: Int): ImageStack = {
    reader.setSeries(series)
    val nX = reader.getSizeX
    val nY = reader.getSizeY
    val buf = ByteBuffer.allocate(nX * nY * 2) //2 for 16-bit pixels
    val stack = new ImageStack(nX,nY)

    for (i <- 0 until reader.getImageCount if i / reader.getSizeZ == channel) {
      buf.clear()
      reader.openBytes(i, buf.array())
      buf.rewind()
      val myShortArray = new Array[Short](nX * nY)
      buf.asShortBuffer().get(myShortArray)
      stack.addSlice(i.toString, myShortArray)
    }
    stack
  }

  def doEDOF(input: ImageStack, outputFileName: String): Unit = {
    println("Running EDOF")
    val iw = imageware.Builder.create(input)
    val params = new Parameters()
    params.setQualitySettings(Parameters.QUALITY_HIGH)
    val edf = new EdfComplexWavelets(params.daubechielength, params.nScales, params.subBandCC, params.majCC)
    val out = edf.process(iw)(0)
    out.show("EDOF")
    IJ.saveAsTiff(new ImagePlus("Output", out.buildImageStack()), outputFileName)
    println("Done EDOF, printing output info")
    out.printInfo()
  }

  def transform(original: Short, min: Short, max: Short): Byte = {
    if (original >= 0 && original < min)
      0.toByte
    else if (original < 0 || original > max)
      255.toByte
    else
      (((original - min)/max.toFloat) * 255).floor.toByte
  }
}
