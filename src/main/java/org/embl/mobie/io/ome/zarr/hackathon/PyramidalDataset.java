package org.embl.mobie.io.ome.zarr.hackathon;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.List;

/**
 * A {@code et.imagej.Dataset} that can be viewed
 * in ImageJ, backed by 5D multi-resolution image data,
 * containing additional methods presenting that
 * multi-resolution image data in convenient ways.
 *
 * @param <T> the type of the data.
 */
public class PyramidalDataset < T extends NativeType< T > & RealType< T > >  extends DefaultDataset
{
	private final Pyramidal5DImageData data;

	public PyramidalDataset( Pyramidal5DImageData data )
	{
		super( data.asDataset().context(), data.asDataset().getImgPlus() );

		this.data = data;
	}

	public SpimData asSpimData()
	{
		return data.asSpimData();
	}

	public List< SourceAndConverter< T > > asSources()
	{
		return data.asSources();
	}

	public int numChannels()
	{
		return data.numChannels();
	}

	public int numTimePoints()
	{
		return data.numTimePoints();
	}

	public int numResolutions()
	{
		return data.numResolutions();
	}

	public VoxelDimensions voxelDimensions()
	{
		return data.voxelDimensions();
	}
}
