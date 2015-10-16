package org.dave.CompactMachines.integration.ic2;

import net.minecraft.nbt.NBTTagCompound;
import org.dave.CompactMachines.handler.ConfigurationHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.dave.CompactMachines.handler.SharedStorageHandler;
import org.dave.CompactMachines.integration.AbstractSharedStorage;
import cpw.mods.fml.common.Optional;

public class IC2SharedStorage extends AbstractSharedStorage {
  public double eu;
    
  public IC2SharedStorage(SharedStorageHandler storageHandler, int coord, int side) {
    super(storageHandler, coord, side);

    this.eu = 0;
  }

  @Override
  public String type() {
    return "IC2";
  }

  /*
	@Override
	public NBTTagCompound saveToTag() {
		NBTTagCompound compound = super.saveToTag();
		compound.setDouble("energyEU", eu);
		return compound;
	}

	@Override
	public void loadFromTag(NBTTagCompound tag) {
		super.loadFromTag(tag);
		eu = tag.getDouble("energyEU");
	}
  */
  

  public double injectEnergy(double amount, double voltage) {
    /*double new_amount = eu + amount;
    if(new_amount < ConfigurationHandler.capacityEU) { // TODO
      this.eu = new_amount;
      return 0.0;
    } else {
      this.eu = ConfigurationHandler.capacityEU;
      return new_amount - ConfigurationHandler.capacityEU;
      }*/
    this.eu += amount;
    return 0.0;
  }

  public double getDemandedEnergy() {
    double deu = ConfigurationHandler.capacityEU - eu;
    if(deu < 0.0) return 0.0;
    else return deu;
    //return ConfigurationHandler.capacityEU; // TODO configuration option
  }

  public double getOfferedEnergy() {
    return Math.min(eu, ConfigurationHandler.capacityEU); // TODO
  }

  public void drawEnergy(double amount) {
    this.eu -= amount;
  }

	public boolean emitsEnergyTo(TileEntity emitter, ForgeDirection direction) {
    return ConfigurationHandler.enableIntegrationIC2;
	}

}
