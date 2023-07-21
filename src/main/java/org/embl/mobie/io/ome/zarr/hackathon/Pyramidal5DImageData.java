package org.embl.mobie.io.ome.zarr.hackathon;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.List;


/**
 * 5D multi-resolution array data
 * represented as various 5D images objects
 * that can be visualised in different ImageJ
 * viewers.
 *
 * @param <T>
 */
public interface Pyramidal5DImageData< T extends NativeType< T > & RealType< T > >
{
	/**
	 * @return an IJ2 {@code net.imagej.Dataset}
	 *   with additional methods for retrieving the
	 *   underlying multi-resolution data.
	 */
	PyramidalDataset asPyramidalDataset();

	/**
	 * @return a IJ2 {@code net.imagej.Dataset} wrapping the full resolution
	 *   5D (XYZCT) image; this will indirectly also serve the ImagePlus.
	 *
	 */
	Dataset asDataset();

	/**
	 * @return a list of BigDataViewer sources, representing a 5D (XYZCT) multi-resolution image, one source for each channel of the dataset.
	 * 	 The sources provide nested volatile versions. The sources are
	 * 	 multi-resolution, reflecting the resolution pyramid of the OME-Zarr.
	 */
	List< SourceAndConverter< T > > asSources();

	/**
	 * @return a SpimData representation of a 5D (XYZCT) multi-resolution image.
	 */
	SpimData asSpimData();

	int numChannels();

	int numTimePoints();

	int numResolutions();

	T getType();

	VoxelDimensions voxelDimensions();

	String getName();
}
