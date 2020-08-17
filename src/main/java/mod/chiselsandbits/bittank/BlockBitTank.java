package mod.chiselsandbits.bittank;

import com.google.common.base.Predicate;

import mod.chiselsandbits.core.Log;
import mod.chiselsandbits.helpers.ExceptionNoTileEntity;
import mod.chiselsandbits.helpers.ModUtil;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class BlockBitTank extends Block implements ITileEntityProvider
{

	public static final PropertyDirection FACING = PropertyDirection.create( "facing", new Predicate<Direction>() {

		@Override
		public boolean apply(
				final Direction face )
		{
			return face != Direction.DOWN && face != Direction.UP;
		}

	} );

	public BlockBitTank()
	{
		super( Material.IRON );
		setSoundType( SoundType.GLASS );
		translucent = true;
		setLightOpacity( 0 );
		setHardness( 1 );
		setHarvestLevel( "pickaxe", 0 );
	}

	@Override
	public int getLightValue(
			final BlockState state,
			final IBlockAccess world,
			final BlockPos pos )
	{
		try
		{
			return getTileEntity( world, pos ).getLightValue();
		}
		catch ( final ExceptionNoTileEntity e )
		{
			Log.noTileError( e );
		}

		return 0;
	}

	@Override
	public BlockState onBlockPlaced(
			final World worldIn,
			final BlockPos pos,
			final Direction facing,
			final float hitX,
			final float hitY,
			final float hitZ,
			final int meta,
			final EntityLivingBase placer )
	{
		return getDefaultState().withProperty( FACING, placer.getHorizontalFacing() );
	}

	@Override
	public BlockRenderLayer getBlockLayer()
	{
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	public boolean isFullBlock(
			final BlockState state )
	{
		return false;
	}

	@Override
	public boolean isOpaqueCube(
			final BlockState state )
	{
		return false;
	}

	@Override
	public float getAmbientOcclusionLightValue(
			final BlockState state )
	{
		return 1.0f;
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer( this, FACING );
	}

	@Override
	public int getMetaFromState(
			final BlockState state )
	{
		switch ( state.getValue( FACING ) )
		{
			case NORTH:
				return 0;
			case SOUTH:
				return 1;
			case EAST:
				return 2;
			case WEST:
				return 3;
			default:
				throw new RuntimeException( "Invalid State." );
		}
	}

	@Override
	public BlockState getStateFromMeta(
			final int meta )
	{
		switch ( meta )
		{
			case 0:
				return getDefaultState().withProperty( FACING, Direction.NORTH );
			case 1:
				return getDefaultState().withProperty( FACING, Direction.SOUTH );
			case 2:
				return getDefaultState().withProperty( FACING, Direction.EAST );
			case 3:
				return getDefaultState().withProperty( FACING, Direction.WEST );
			default:
				throw new RuntimeException( "Invalid State." );
		}
	}

	@Override
	public TileEntity createNewTileEntity(
			final World worldIn,
			final int meta )
	{
		return new TileEntityBitTank();
	}

	public TileEntityBitTank getTileEntity(
			final TileEntity te ) throws ExceptionNoTileEntity
	{
		if ( te instanceof TileEntityBitTank )
		{
			return (TileEntityBitTank) te;
		}
		throw new ExceptionNoTileEntity();
	}

	public TileEntityBitTank getTileEntity(
			final IBlockAccess world,
			final BlockPos pos ) throws ExceptionNoTileEntity
	{
		return getTileEntity( world.getTileEntity( pos ) );
	}

	@Override
	public boolean onBlockActivated(
			final World worldIn,
			final BlockPos pos,
			final BlockState state,
			final PlayerEntity playerIn,
			final Hand hand,
			final Direction side,
			final float hitX,
			final float hitY,
			final float hitZ )
	{
		try
		{
			final TileEntityBitTank tank = getTileEntity( worldIn, pos );
			final ItemStack current = ModUtil.nonNull( playerIn.inventory.getCurrentItem() );

			if ( !ModUtil.isEmpty( current ) )
			{
				final IFluidHandler wrappedTank = tank;
				if ( FluidUtil.interactWithFluidHandler( playerIn, hand, wrappedTank ) )
				{
					return true;
				}

				if ( tank.addHeldBits( current, playerIn ) )
				{
					return true;
				}
			}
			else
			{
				if ( tank.addAllPossibleBits( playerIn ) )
				{
					return true;
				}
			}

			if ( tank.extractBits( playerIn, hitX, hitY, hitZ, pos ) )
			{
				return true;
			}
		}
		catch ( final ExceptionNoTileEntity e )
		{
			Log.noTileError( e );
		}

		return false;
	}

}
