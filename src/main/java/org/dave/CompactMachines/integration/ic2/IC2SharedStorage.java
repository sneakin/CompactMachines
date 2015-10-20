package org.dave.CompactMachines.integration.ic2;

import net.minecraft.nbt.NBTTagCompound;
import org.dave.CompactMachines.handler.ConfigurationHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.dave.CompactMachines.handler.SharedStorageHandler;
import org.dave.CompactMachines.integration.AbstractHoppingStorage;
import org.dave.CompactMachines.integration.HoppingMode;
import cpw.mods.fml.common.Optional;

import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;


public class IC2SharedStorage extends AbstractHoppingStorage {
  public double eu;
    
  public IC2SharedStorage(SharedStorageHandler storageHandler, int coord, int side) {
    super(storageHandler, coord, side);

    this.eu = 0;
  }

  @Override
  public String type() {
    return "IC2";
  }

	@Override
	public NBTTagCompound saveToTag() {
		NBTTagCompound compound = super.saveToTag();
		compound.setDouble("energy", eu);
		return compound;
	}

	@Override
	public void loadFromTag(NBTTagCompound tag) {
    super.loadFromTag(tag);
		eu = tag.getDouble("energy");
	}

  @Override
	public void hopToTileEntity(TileEntity target, boolean useOppositeSide) {
    if(target instanceof IEnergySink || target instanceof IEnergySource) {
      //target.markDirty();
    }
  }

  public double injectEnergy(double amount, double voltage) {
    setDirty();

    // todo rate limit w/ rateEU
    double new_amount = Math.min(ConfigurationHandler.rateEU, amount);
    double leftover = amount - new_amount;
    
    double new_eu = eu + new_amount;
    if(new_eu <= ConfigurationHandler.capacityEU) {
      this.eu = new_eu;
      return leftover;
    } else {
      this.eu = ConfigurationHandler.capacityEU;
      return leftover + (new_eu - ConfigurationHandler.capacityEU);
    }
  }

  public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
    return ConfigurationHandler.enableIntegrationIC2;
	}

  public double getDemandedEnergy() {
    double deu = ConfigurationHandler.capacityEU - eu;
    if(deu < 0.0) return 0.0;
    else return Math.min(deu, ConfigurationHandler.rateEU);
  }

  public double getOfferedEnergy() {
    return Math.min(eu, ConfigurationHandler.rateEU);
  }

  public void drawEnergy(double amount) {
    setDirty();
    this.eu -= amount;
  }

	public boolean emitsEnergyTo(TileEntity emitter, ForgeDirection direction) {
    return ConfigurationHandler.enableIntegrationIC2;
	}
}
