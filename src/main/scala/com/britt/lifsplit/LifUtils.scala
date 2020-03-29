package com.britt.lifsplit

import java.nio.{ByteBuffer, ByteOrder}

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

  // Optionally transform
  def extractStack(reader: LIFReader, series: Int, channel: Int,
                   scale: Option[(Int,Int)]): ImageStack = {
    reader.setSeries(series)
    val nX = reader.getSizeX
    val nY = reader.getSizeY
    val buf = ByteBuffer.allocate(nX * nY * 2) //2 for 16-bit pixels
    buf.order(ByteOrder.LITTLE_ENDIAN)
    val stack = new ImageStack(nX,nY)

    for (i <- 0 until reader.getImageCount if i / reader.getSizeZ == channel) {
      buf.clear()
      reader.openBytes(i, buf.array())
      buf.rewind()
      scale match {
        case Some((min, max)) =>
          val pixels = Array.fill(nX * nY)(transform(buf.getShort(), min, max))
          stack.addSlice(i.toString, pixels)
        case None             =>
          val myShortArray = new Array[Short](nX * nY)
          buf.asShortBuffer().get(myShortArray)
          stack.addSlice(i.toString, myShortArray)
      }
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
    //out.show("EDOF")
    IJ.saveAsTiff(new ImagePlus("Output", out.buildImageStack()), outputFileName)
    //println("Done EDOF, printing output info")
    //out.printInfo()
  }

  def transform(original: Short, min: Int, max: Int): Byte = {
    val unsigned = original & 0xFFFF
    if (unsigned <= min)
      0.toByte
    else if (unsigned >= max)
      255.toByte
    else
      (((unsigned - min)/max.toFloat) * 255).floor.toByte
  }
  /*
  * Ch00 Color Blue: 100-2000
  * Ch01 Color Green: 100-1000
  * Ch03 Color Red: 100-2000
   */
  def rangeForChannel(channel: Int): (Int, Int) =
    channel match {
      case 0 => (100, 2000)
      case 1 => (100, 1000)
      case 3 => (100, 2000)
      case _ => throw new IllegalArgumentException(s"Invalid channel $channel")
    }

  /*
   * Naming construct needed the ID code which will be a combination of 4 numbers
 and examples LC, MLAD2 (exclude the 2), RI, DRC, PROX RCA (make PROXRCA), etc.
 then _s## (the 0-76) then _zEDOF then _ch##
   * e.g. 3470LC_s05_zEDOF_ch03
   */
  def getFileName(series: Int, channel: Int): String =
    f"/Users/Britt/lifFiles/output/3470LC_s$series%2d_zEDOF_ch$channel%2d.tiff"
}
