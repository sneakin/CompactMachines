package org.dave.CompactMachines.tileentity;

import java.util.List;
import java.util.HashMap;

import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import mrtjp.projectred.api.IBundledTile;
import mrtjp.projectred.api.ProjectRedAPI;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import org.dave.CompactMachines.CompactMachines;
import org.dave.CompactMachines.handler.ConfigurationHandler;
import org.dave.CompactMachines.handler.SharedStorageHandler;
import org.dave.CompactMachines.init.ModBlocks;
import org.dave.CompactMachines.integration.AbstractHoppingStorage;
import org.dave.CompactMachines.integration.HoppingMode;
import org.dave.CompactMachines.integration.AbstractSharedStorage;
import org.dave.CompactMachines.integration.appeng.AESharedStorage;
import org.dave.CompactMachines.integration.appeng.CMGridBlock;
import org.dave.CompactMachines.integration.botania.BotaniaSharedStorage;
import org.dave.CompactMachines.integration.bundledredstone.BRSharedStorage;
import org.dave.CompactMachines.integration.fluid.FluidSharedStorage;
import org.dave.CompactMachines.integration.gas.GasSharedStorage;
import org.dave.CompactMachines.integration.item.ItemSharedStorage;
import org.dave.CompactMachines.integration.opencomputers.OpenComputersSharedStorage;
import org.dave.CompactMachines.integration.redstoneflux.FluxSharedStorage;
import org.dave.CompactMachines.integration.ic2.IC2SharedStorage;
import org.dave.CompactMachines.machines.tools.ChunkLoadingTools;
import org.dave.CompactMachines.machines.tools.CubeTools;
import org.dave.CompactMachines.reference.Names;
import org.dave.CompactMachines.reference.Reference;
import org.dave.CompactMachines.utility.LogHelper;

import vazkii.botania.api.mana.IManaPool;
import appeng.api.movable.IMovableTile;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraftforge.common.MinecraftForge;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;

@Optional.InterfaceList({
		@Optional.Interface(iface = "appeng.api.networking.IGridHost", modid = "appliedenergistics2"),
		@Optional.Interface(iface = "appeng.api.movable.IMovableTile", modid = "appliedenergistics2"),
		@Optional.Interface(iface = "mrtjp.projectred.api.IBundledTile", modid = "ProjRed|Transmission"),
		@Optional.Interface(iface = "li.cil.oc.api.network.SidedEnvironment", modid = "OpenComputers"),
		@Optional.Interface(iface = "mekanism.api.gas.IGasHandler", modid = "Mekanism"),
		@Optional.Interface(iface = "mekanism.api.gas.ITubeConnection", modid = "Mekanism"),
    @Optional.Interface(iface = "vazkii.botania.api.mana.IManaPool", modid = "Botania"),
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "IC2"),
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2")
})
public class TileEntityMachine extends TileEntityCM implements ISidedInventory, IFluidHandler, IGasHandler, ITubeConnection, IEnergyHandler, IGridHost, IMovableTile, IBundledTile, SidedEnvironment, IManaPool, IEnergySource, IEnergySink {

	public int								coords			= -1;
	public int[]							_fluidid;
	public int[]							_fluidamount;
	public int[]							_gasid;
	public int[]							_gasamount;
	public int[]							_energy;
	public int								_mana = 0;
  public double[]           _eu;
  public double             _euRate = 0;
  public double             _euCapacity = 0;
  public HoppingMode[]      _hoppingmodes;
	public int								meta			= 0;

	public int								entangledInstance;

	public boolean							isUpgraded		= false;

	public HashMap<Integer, CMGridBlock>	gridBlocks;
	public HashMap<Integer, IGridNode>		gridNodes;
	private boolean							init;

	public static final int					INVENTORY_SIZE	= 6;

  private boolean _isAddedToEnergyNet;
  private boolean _didFirstAddToNet;

	public TileEntityMachine() {
		super();
		// XXX: Should these be initialised to -1, as in TileEntityInterface
		_fluidid = new int[6];
		_fluidamount = new int[6];
		// These need to be initialised to -1 to avoid a crash when no
		// gas-providing mod is installed
		_gasid = new int[] { -1, -1, -1, -1, -1, -1 };
		_gasamount = new int[6];
		_energy = new int[6];
		_mana = 0;
    _eu = new double[7];
    _hoppingmodes = new HoppingMode[6];
    _euRate = ConfigurationHandler.rateEU;
    _euCapacity = ConfigurationHandler.capacityEU;

		gridBlocks = new HashMap<Integer, CMGridBlock>();
		gridNodes = new HashMap<Integer, IGridNode>();

    _isAddedToEnergyNet = false;
    _didFirstAddToNet = false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		// Side directly determines the inventory
		return new int[] { side };
	}

	public ItemSharedStorage getStorage(int side) {
		return (ItemSharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, side, "item");
	}

	public FluidSharedStorage getStorageFluid(int side) {
		return (FluidSharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, side, "liquid");
	}

	public GasSharedStorage getStorageGas(int side) {
		return (GasSharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, side, "gas");
	}

	public FluxSharedStorage getStorageFlux(int side) {
		return (FluxSharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, side, "flux");
	}

	public AESharedStorage getStorageAE(int side) {
		return (AESharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, side, entangledInstance, "appeng");
	}

	public BRSharedStorage getStorageBR(int side) {
		return (BRSharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, side, "bundledRedstone");
	}

	public OpenComputersSharedStorage getStorageOC(int side) {
		return (OpenComputersSharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, side, "OpenComputers");
	}

	public BotaniaSharedStorage getStorageBotania(int side) {
		return (BotaniaSharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, 0, "botania");
	}

	public IC2SharedStorage getStorageIC2in(int side) {
		return (IC2SharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, side, "IC2");
	}

  public IC2SharedStorage getStorageIC2out() {
    return (IC2SharedStorage) SharedStorageHandler.instance(worldObj.isRemote).getStorage(this.coords, ForgeDirection.UNKNOWN.ordinal(), "IC2");
  }

	@Override
	public void readFromNBT(NBTTagCompound nbtTagCompound)
	{
		super.readFromNBT(nbtTagCompound);

		coords = nbtTagCompound.getInteger("coords");
		meta = nbtTagCompound.getInteger("meta");
		isUpgraded = nbtTagCompound.getBoolean("upgraded");
		entangledInstance = nbtTagCompound.getInteger("entangle-id");

		if (isUpgraded && worldObj != null && worldObj.isRemote) {
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTagCompound)
	{
		super.writeToNBT(nbtTagCompound);

		if (!nbtTagCompound.hasKey("meta")) {
			nbtTagCompound.setInteger("meta", meta);
		}

		nbtTagCompound.setInteger("coords", coords);
		nbtTagCompound.setBoolean("upgraded", isUpgraded);
		nbtTagCompound.setInteger("entangle-id", entangledInstance);
	}

	@Override
	@SideOnly(Side.SERVER)
	public void onChunkUnload() {
		super.onChunkUnload();

		if (worldObj.isRemote) {
			return;
		}

		if (ConfigurationHandler.chunkLoadingMode == 2) {
			ChunkLoadingTools.disableMachine(this);
		}

		deinitialize();
	}

  @Override
  public void validate() {
    super.validate();
    if(Reference.IC2_AVAILABLE && !_isAddedToEnergyNet) {
      _didFirstAddToNet = false;
    }
  }

  @Override
	public void invalidate() {
    if(Reference.IC2_AVAILABLE) {
      removeFromEnergyNet();
    }

		super.invalidate();

		if (worldObj.isRemote) {
			return;
		}

		deinitialize();
	}

	public void deinitialize() {
		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			OpenComputersSharedStorage storageOC = getStorageOC(dir.ordinal());
			if (storageOC != null) {
				Node node = storageOC.getNode();
				if (node != null) {
					node.remove();
				}
			}

			AESharedStorage storageAE = getStorageAE(dir.ordinal());
			if (storageAE != null) {
				storageAE.destroyMachineNode(entangledInstance);
				CompactMachines.instance.entangleRegistry.removeMachineTile(this);
			}
		}
	}

	public void initialize() {
		if (worldObj.isRemote) {
			return;
		}

		entangledInstance = CompactMachines.instance.entangleRegistry.registerMachineTile(this);
		this.markDirty();

		if (ConfigurationHandler.chunkLoadingMode != 0 && !ChunkLoadingTools.isCoordChunkLoaded(this)) {
			ChunkLoadingTools.forceChunkLoad(this.coords);
		}

		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			OpenComputersSharedStorage storageOC = getStorageOC(dir.ordinal());
			if (storageOC != null) {
				Node node = storageOC.getNode();
				if (node != null && node.network() == null) {
					li.cil.oc.api.Network.joinOrCreateNetwork(this);
				}
			}

			AESharedStorage storageAE = getStorageAE(dir.ordinal());
			if (storageAE != null) {
				getGridNode(dir);
			}
		}
	}

	@Override
	public void updateEntity() {
		super.updateEntity();

		if (!init && !isInvalid() && coords != -1) {
			initialize();
			init = true;
		}

		if (Reference.PR_AVAILABLE) {
			updateIncomingSignals();
		}

		if (!worldObj.isRemote) {
      if (Reference.IC2_AVAILABLE) {
        addToEnergyNet();
        updateIC2HoppingMode();
      }

			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
				TileEntity outside = worldObj.getTileEntity(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
				for (AbstractSharedStorage storage : SharedStorageHandler.instance(false).getAllStorages(coords, dir.ordinal())) {
					if (!(storage instanceof AbstractHoppingStorage)) {
						continue;
					}

					hopStorage(storage, outside);
				}
			}
		}
	}

  @Optional.Method(modid = "IC2")
  private void updateIC2HoppingMode() {
    boolean needs_update = false;
    for(ForgeDirection dir: ForgeDirection.VALID_DIRECTIONS) {
      HoppingMode mode = getStorageIC2in(dir.ordinal()).getHoppingMode();
      if(mode != _hoppingmodes[dir.ordinal()]) {
        _hoppingmodes[dir.ordinal()] = mode;
        //LogHelper.info(this + " mode changed " + dir + " " + getStorageIC2in(dir.ordinal()).getHoppingMode());
        //getStorageIC2in(dir.ordinal()).clearMachineModeChanged(CompactMachines.instance.entangleRegistry.numberOfMachines(entangledInstance));
        needs_update = true;
      }
    }

    if(needs_update) {
      readdToEnergyNet();
    }
  }
  
  @Optional.Method(modid = "IC2")
  private void removeFromEnergyNet() {
		if (_isAddedToEnergyNet) {
      
			if (!worldObj.isRemote) {
				MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			}
			_isAddedToEnergyNet = false;
      //LogHelper.info(this + " removeFromEnergyNet");
		}
  }
  
  @Optional.Method(modid = "IC2")
  private void addToEnergyNet() {
    if(!_didFirstAddToNet && !worldObj.isRemote) {
      MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
      _didFirstAddToNet = true;
      _isAddedToEnergyNet = true;
      //LogHelper.info(this + " addToEnergyNet");
    }
  }

  @Optional.Method(modid = "IC2")
  private void readdToEnergyNet() {
    removeFromEnergyNet();
    _didFirstAddToNet = false;
  }

  public ChunkCoordinates getChunkCoordinates() {
		int x = coords * ConfigurationHandler.cubeDistance;
		int y = 39;
		int z = 0;

		int size = (Reference.getBoxSize(meta) / 2);

		return new ChunkCoordinates(x + size, y + size, z + size);

	}

	private void hopStorage(AbstractSharedStorage storage, TileEntity outside) {
		if (storage == null || !(storage instanceof AbstractHoppingStorage)) {
			return;
		}

		AbstractHoppingStorage hoppingStorage = (AbstractHoppingStorage) storage;

		if (hoppingStorage.getHoppingMode() == HoppingMode.Export || (hoppingStorage.getHoppingMode() == HoppingMode.Auto && hoppingStorage.isAutoHoppingToInside() == false)) {
			hoppingStorage.hoppingTick(outside, true);
		}
	}

	@Override
	public int getSizeInventory() {
		return INVENTORY_SIZE;
	}

	@Override
	public ItemStack getStackInSlot(int slotIndex) {
		if (coords == -1) {
			return null;
		}
		return getStorage(slotIndex).getStackInSlot(0);
	}

	@Override
	public ItemStack decrStackSize(int slotIndex, int decreaseAmount) {
		if (coords == -1) {
			return null;
		}
		return getStorage(slotIndex).decrStackSize(0, decreaseAmount);
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slotIndex) {
		if (coords == -1) {
			return null;
		}
		return getStorage(slotIndex).getStackInSlotOnClosing(0);
	}

	@Override
	public void setInventorySlotContents(int slotIndex, ItemStack itemStack) {
		if (coords == -1) {
			return;
		}

		ItemSharedStorage storage = getStorage(slotIndex);
		storage.setAutoHoppingToInside(true);
		storage.setDirty();
		storage.setInventorySlotContents(0, itemStack);
	}

	@Override
	public String getInventoryName() {
		return this.hasCustomName() ? this.getCustomName() : Names.Containers.MACHINE;
	}

	@Override
	public boolean hasCustomInventoryName() {
		return this.hasCustomName();
	}

	@Override
	public int getInventoryStackLimit() {
		if (coords == -1) {
			return 0;
		}
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer p_70300_1_) {
		return false;
	}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}

	@Override
	public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_) {
		if (coords == -1) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canInsertItem(int slotIndex, ItemStack itemStack, int side) {
		if (coords == -1) {
			return false;
		}
		return true;
	}

	@Override
	public boolean canExtractItem(int slotIndex, ItemStack itemStack, int side) {
		if (coords == -1) {
			return false;
		}
		return true;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		if (coords == -1) {
			return 0;
		}
		FluidSharedStorage fss = getStorageFluid(from.ordinal());
		if (doFill && resource.amount > 0) {
			fss.setAutoHoppingToInside(true);
			fss.setDirty();
		}
		return fss.fill(from, resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		return getStorageFluid(from.ordinal()).drain(from, maxDrain, doDrain);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		return getStorageFluid(from.ordinal()).drain(from, resource, doDrain);
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		if (coords == -1) {
			return false;
		}
		return getStorageFluid(from.ordinal()).canDrain(from, fluid);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		if (coords == -1) {
			return false;
		}
		return getStorageFluid(from.ordinal()).canFill(from, fluid);
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return getStorageFluid(from.ordinal()).getTankInfo(from);
	}

	public FluidStack getFluid(int side) {
		return getStorageFluid(side).getFluid();
	}

	public FluidStack getFluid(ForgeDirection from) {
		return getStorageFluid(from.ordinal()).getFluid();
	}

	@Optional.Method(modid = "Mekanism")
	@Override
	public int receiveGas(ForgeDirection from, GasStack stack) {
		if (coords == -1) {
			return 0;
		}

		if(!ConfigurationHandler.enableIntegrationMekanism) {
			return 0;
		}

		GasSharedStorage gss = getStorageGas(from.ordinal());

		// XXX: Should we test with canReceiveGas first? Or do we rely on
		// suppliers to do this?
		gss.setAutoHoppingToInside(true);
		gss.setDirty();

		return gss.receiveGas(from, stack);
	}

	@Optional.Method(modid = "Mekanism")
	@Override
	public GasStack drawGas(ForgeDirection from, int amount) {
		return getStorageGas(from.ordinal()).drawGas(from, amount);
	}

	@Optional.Method(modid = "Mekanism")
	@Override
	public int receiveGas(ForgeDirection side, GasStack stack, boolean doTransfer) {
		return this.receiveGas(side, stack);
	}

	@Optional.Method(modid = "Mekanism")
	@Override
	public GasStack drawGas(ForgeDirection side, int amount, boolean doTransfer) {
		return this.drawGas(side, amount);
	}

	@Optional.Method(modid = "Mekanism")
	@Override
	public boolean canReceiveGas(ForgeDirection from, Gas type) {
		return getStorageGas(from.ordinal()).canReceiveGas(from, type);
	}

	@Optional.Method(modid = "Mekanism")
	@Override
	public boolean canDrawGas(ForgeDirection from, Gas type) {
		return getStorageGas(from.ordinal()).canDrawGas(from, type);
	}

	@Optional.Method(modid = "Mekanism")
	@Override
	public boolean canTubeConnect(ForgeDirection side) {
		if(!ConfigurationHandler.enableIntegrationMekanism) {
			return false;
		}
		return true;
	}

	@Optional.Method(modid = "Mekanism")
	public GasStack getGasContents(ForgeDirection from) {
		return getStorageGas(from.ordinal()).getGasContents();
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		return true;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
		if (coords == -1) {
			return 0;
		}
		FluxSharedStorage fss = getStorageFlux(from.ordinal());
		if (!simulate && maxReceive > 0) {
			fss.setAutoHoppingToInside(true);
			fss.setDirty();
		}
		return fss.receiveEnergy(maxReceive, simulate);
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
		if (coords == -1) {
			return 0;
		}
		return getStorageFlux(from.ordinal()).extractEnergy(maxExtract, simulate);
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		if (coords == -1) {
			return 0;
		}
		return getStorageFlux(from.ordinal()).getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		if (coords == -1) {
			return 0;
		}
		return getStorageFlux(from.ordinal()).getMaxEnergyStored();
	}

	public CMGridBlock getGridBlock(ForgeDirection dir) {
		CMGridBlock gridBlock = gridBlocks.get(dir.ordinal());
		if (gridBlock == null) {
			gridBlock = new CMGridBlock(this);
			gridBlocks.put(dir.ordinal(), gridBlock);
		}

		return gridBlock;
	}

	@Optional.Method(modid = "appliedenergistics2")
	@Override
	public IGridNode getGridNode(ForgeDirection dir) {
		if (coords == -1) {
			return null;
		}

		if (worldObj.isRemote) {
			return null;
		}

		if (!ConfigurationHandler.enableIntegrationAE2) {
			return null;
		}

		IGridNode gridNode = gridNodes.get(dir.ordinal());

		if (gridNode == null) {
			gridNode = getStorageAE(dir.ordinal()).getMachineNode(getGridBlock(dir), entangledInstance);
			gridNodes.put(dir.ordinal(), gridNode);
		}

		return gridNode;
	}

	@Optional.Method(modid = "appliedenergistics2")
	@Override
	public AECableType getCableConnectionType(ForgeDirection dir) {
		return AECableType.DENSE;
	}

	@Optional.Method(modid = "appliedenergistics2")
	@Override
	public void securityBreak() {}

	public ItemStack getItemDrop() {
		ItemStack stack = new ItemStack(ModBlocks.machine, 1, meta);

		if (isUpgraded) {
			if (stack.stackTagCompound == null) {
				stack.stackTagCompound = new NBTTagCompound();
			}
			//LogHelper.info("Dropping item stack with coords: " + coords);
			stack.stackTagCompound.setInteger("coords", coords);
		}

		if (hasCustomName()) {
			stack.setStackDisplayName(getCustomName());
		}

		return stack;
	}

	public void dropAsItem() {
		ItemStack stack = getItemDrop();

		EntityItem entityitem = new EntityItem(this.getWorldObj(), this.xCoord, this.yCoord + 0.5F, this.zCoord, stack);

		entityitem.lifespan = 1200;
		entityitem.delayBeforeCanPickup = 10;

		float f3 = 0.05F;
		entityitem.motionX = (float) worldObj.rand.nextGaussian() * f3;
		entityitem.motionY = (float) worldObj.rand.nextGaussian() * f3 + 0.2F;
		entityitem.motionZ = (float) worldObj.rand.nextGaussian() * f3;
		this.getWorldObj().spawnEntityInWorld(entityitem);
	}

	@Override
	@Optional.Method(modid = "appliedenergistics2")
	public boolean prepareToMove() {
		if (isUpgraded) {
			return true;
		}
		return false;
	}

	@Override
	@Optional.Method(modid = "appliedenergistics2")
	public void doneMoving() {}

	@Optional.Method(modid = "ProjRed|Transmission")
	private void updateIncomingSignals() {
		if(!ConfigurationHandler.enableIntegrationProjectRed) {
			return;
		}

		boolean needsNotify = false;
		boolean haveChanges = false;
		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			BRSharedStorage br = getStorageBR(dir.ordinal());
			if (br.machineNeedsNotify) {
				//LogHelper.info("Signal into one of the interfaces changed: " + dir);
				needsNotify = true;
				br.machineNeedsNotify = false;
			}
			byte[] previous = br.machineBundledSignal;
			byte[] current = ProjectRedAPI.transmissionAPI.getBundledInput(worldObj, xCoord, yCoord, zCoord, dir.ordinal());

			if (current != null) {
				for (int i = 0; i < current.length; i++) {
					if (previous[i] != current[i]) {
						haveChanges = true;
						previous[i] = current[i];
					}
				}
			}
			br.setDirty();
		}

		if (haveChanges) {
			WorldServer machineWorld = MinecraftServer.getServer().worldServerForDimension(ConfigurationHandler.dimensionId);
			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
				Vec3 pos = CubeTools.getInterfacePosition(this.coords, this.meta, dir);

				machineWorld.notifyBlockChange((int) pos.xCoord, (int) pos.yCoord, (int) pos.zCoord, ModBlocks.interfaceblock);

			}
		}

		if (needsNotify || haveChanges) {
			worldObj.notifyBlockChange(xCoord, yCoord, zCoord, blockType);
		}
	}

	@Override
	@Optional.Method(modid = "ProjRed|Transmission")
	public byte[] getBundledSignal(int dir) {
		if(!ConfigurationHandler.enableIntegrationProjectRed) {
			return null;
		}

		BRSharedStorage storage = getStorageBR(dir);
		byte[] current = storage.interfaceBundledSignal;

		if (current == null) {
			return null;
		}

		byte[] result = new byte[current.length];
		for (int i = 0; i < current.length; i++) {
			//Machine-Output = Interface-Input unless Interface-Output is made by us
			int a = current[i] & 255;
			int b = storage.interfaceOutputtedSignal[i] & 255;
			int c = a;
			if (b > 0) {
				continue;
			}

			result[i] = (byte) c;
		}

		storage.machineOutputtedSignal = result;
		storage.setDirty();

		//LogHelper.info("Machine outputting to " + ForgeDirection.getOrientation(dir) + ": " + getByteString(result));

		return result;
	}

	@Override
	@Optional.Method(modid = "ProjRed|Transmission")
	public boolean canConnectBundled(int side) {
		if(!ConfigurationHandler.enableIntegrationProjectRed) {
			return false;
		}

		return true;
	}

	@Override
	@Optional.Method(modid = "OpenComputers")
	public Node sidedNode(ForgeDirection side) {
		if (worldObj.isRemote) {
			return null;
		}

		if(!ConfigurationHandler.enableIntegrationOpenComputers) {
			return null;
		}

		return getStorageOC(side.ordinal()).getNode();
	}

	@Override
	@Optional.Method(modid = "OpenComputers")
	public boolean canConnect(ForgeDirection side) {
		if(!ConfigurationHandler.enableIntegrationOpenComputers) {
			return false;
		}

		return true;
	}

	@Override
	@Optional.Method(modid = "Botania")
	public boolean isFull() {
		if (coords == -1) {
			return true;
		}

		return getStorageBotania(0).isFull();
	}

	@Override
	@Optional.Method(modid = "Botania")
	public void recieveMana(int mana) {
		if (coords == -1) {
			return;
		}
		getStorageBotania(0).recieveMana(mana);
	}

	@Override
	@Optional.Method(modid = "Botania")
	public boolean canRecieveManaFromBursts() {
		if (coords == -1) {
			return false;
		}
		return getStorageBotania(0).canRecieveManaFromBursts();
	}

	@Override
	@Optional.Method(modid = "Botania")
	public int getCurrentMana() {
		if (coords == -1) {
			return 0;
		}
		return getStorageBotania(0).getCurrentMana();
	}

	@Override
	@Optional.Method(modid = "Botania")
	public boolean isOutputtingPower() {
		if (coords == -1) {
			return false;
		}
		return getStorageBotania(0).isOutputtingPower();
	}

  @Override
  @Optional.Method(modid = "IC2")
  public int getSourceTier() {
    return Integer.MAX_VALUE;
  }

  @Override
  @Optional.Method(modid = "IC2")
  public int getSinkTier() {
    return Integer.MAX_VALUE;
  }

  @Override
  @Optional.Method(modid = "IC2")
  public double injectEnergy(ForgeDirection direction, double amount, double voltage)
  {
    IC2SharedStorage storage = getStorageIC2in(direction.ordinal());
    //LogHelper.info(this + " injectEnergy " + amount + " " + direction + " " + storage.getHoppingMode());
    if(storage.getHoppingMode() == HoppingMode.Export ||
       storage.getHoppingMode() == HoppingMode.Disabled) {
      return amount;
    }
    
    return storage.injectEnergy(amount, voltage);
  }
    
  @Override
  @Optional.Method(modid = "IC2")
  public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
    //LogHelper.info(this + " acceptsEnergyFrom " + direction + " " + getStorageIC2in(direction.ordinal()).getHoppingMode());


    return getStorageIC2in(direction.ordinal()).acceptsEnergyFrom(emitter, direction) &&
      getStorageIC2in(direction.ordinal()).getHoppingMode() != HoppingMode.Disabled &&
      getStorageIC2in(direction.ordinal()).getHoppingMode() != HoppingMode.Export;
  }

  @Override
  @Optional.Method(modid = "IC2")
  public double getDemandedEnergy() {
    double max = 0.0;
    double eu = 0.0;
    for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
      HoppingMode mode = getStorageIC2in(dir.ordinal()).getHoppingMode();
      if(mode == HoppingMode.Export || mode == HoppingMode.Disabled) continue;
      double e = getStorageIC2in(dir.ordinal()).getDemandedEnergy();
      eu += getStorageIC2in(dir.ordinal()).eu;
      if(max < e) max = e;
    }

    //LogHelper.info(this + " getDemandedEnergy " + eu + " " + max);
    return Math.min(max, ConfigurationHandler.capacityEU);
    // should be IC2in, but we have no side so we try
    //return getStorageIC2in(demanding_side).getDemandedEnergy();
  }
    
  @Override
  @Optional.Method(modid = "IC2")
  public void drawEnergy(double amount) {
    //LogHelper.info(this + " drawEnergy " + amount);
    getStorageIC2out().drawEnergy(amount);
  }

  @Override
  @Optional.Method(modid = "IC2")
  public double getOfferedEnergy() {
    //LogHelper.info(this + " getOfferedEnergy " + getStorageIC2out().getOfferedEnergy());
    return getStorageIC2out().getOfferedEnergy();
  }
    
  @Override
  @Optional.Method(modid = "IC2")
  public boolean emitsEnergyTo(TileEntity emitter, ForgeDirection direction) {
    //LogHelper.info(this + " emitsEnergyTo " + direction + " " + getStorageIC2in(direction.ordinal()).getHoppingMode());

    return getStorageIC2out().emitsEnergyTo(emitter, direction) &&
      getStorageIC2in(direction.ordinal()).getHoppingMode() != HoppingMode.Disabled &&
      getStorageIC2in(direction.ordinal()).getHoppingMode() != HoppingMode.Import;
  }

  @Optional.Method(modid = "IC2")
  public double getEUCapacity() { return ConfigurationHandler.capacityEU * (ForgeDirection.VALID_DIRECTIONS.length + 1); }

  @Optional.Method(modid = "IC2")
  public double getEUrate() { return ConfigurationHandler.rateEU; }

  @Optional.Method(modid = "IC2")
  public double getIncomingEU(int side) { return getStorageIC2in(side).eu; }

  @Optional.Method(modid = "IC2")
  public double getIncomingEU() {
    double sum = 0.0;
    for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
      sum += getStorageIC2in(dir.ordinal()).eu;
    }
    return sum;
  }

  @Optional.Method(modid = "IC2")
  public double getOutgoingEU() { return getStorageIC2out().eu; }

  public HoppingMode getHoppingModeIn(ForgeDirection dir) {
    return getStorageIC2in(dir.ordinal()).getHoppingMode();
  }

  public HoppingMode getHoppingModeOut() {
    return getStorageIC2out().getHoppingMode();
  }
}
