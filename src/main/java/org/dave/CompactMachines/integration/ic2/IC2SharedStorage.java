package org.dave.CompactMachines.integration.ic2;

import net.minecraft.nbt.NBTTagCompound;
import org.dave.CompactMachines.handler.ConfigurationHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.dave.CompactMachines.handler.SharedStorageHandler;
import org.dave.CompactMachines.integration.AbstractBufferedStorage;
import cpw.mods.fml.common.Optional;

public class IC2SharedStorage extends AbstractBufferedStorage {
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
		NBTTagCompound compound = new NBTTagCompound();
		compound.setDouble("energy", eu);
		return compound;
	}

	@Override
	public void loadFromTag(NBTTagCompound tag) {
		eu = tag.getDouble("energy");
	}

  public double injectEnergy(double amount, double voltage) {
    setDirty();
    
    double new_amount = eu + amount;
    if(new_amount <= ConfigurationHandler.capacityEU) {
      this.eu = new_amount;
      return 0.0;
    } else {
      this.eu = ConfigurationHandler.capacityEU;
      return new_amount - ConfigurationHandler.capacityEU;
    }
  }

  public double getDemandedEnergy() {
    double deu = ConfigurationHandler.capacityEU - eu;
    if(deu < 0.0) return 0.0;
    else return deu;
  }

  public double getOfferedEnergy() {
    return Math.min(eu, ConfigurationHandler.capacityEU);
  }

  public void drawEnergy(double amount) {
    setDirty();
    this.eu -= amount;
  }

	public boolean emitsEnergyTo(TileEntity emitter, ForgeDirection direction) {
    return ConfigurationHandler.enableIntegrationIC2;
	}

}
