package org.dave.CompactMachines.handler;

import static mcp.mobius.waila.api.SpecialChars.ITALIC;
import static mcp.mobius.waila.api.SpecialChars.RESET;
import static mcp.mobius.waila.api.SpecialChars.WHITE;
import static mcp.mobius.waila.api.SpecialChars.YELLOW;

import java.util.ArrayList;
import java.util.List;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.dave.CompactMachines.init.ModBlocks;
import org.dave.CompactMachines.tileentity.TileEntityInterface;
import org.dave.CompactMachines.tileentity.TileEntityMachine;

public class WailaHandler implements IWailaDataProvider {

	public static void callbackRegister(IWailaRegistrar registrar) {
		WailaHandler instance = new WailaHandler();
		registrar.registerHeadProvider(instance, ModBlocks.machine.getClass());
		registrar.registerBodyProvider(instance, ModBlocks.machine.getClass());
		registrar.registerBodyProvider(instance, ModBlocks.interfaceblock.getClass());
	}

	@Override
	public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return null;
	}

	@Override
	public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		TileEntity te = accessor.getTileEntity();

		if (te instanceof TileEntityMachine) {
			TileEntityMachine machine = (TileEntityMachine) te;
			if (machine.coords != -1) {
				List<String> head = new ArrayList<String>();
				head.add(WHITE + StatCollector.translateToLocal("tile.compactmachines:machine.name") + RESET + YELLOW + " #" + machine.coords + RESET);
				return head;
			}
		}

		return currenttip;
	}

	@Override
	public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		TileEntity te = accessor.getTileEntity();

		if (te instanceof TileEntityMachine) {
      return addMachineBody(te, currenttip, accessor);
		} else if (te instanceof TileEntityInterface) {
      return addInterfaceBody(te, currenttip, accessor);
		}

		return currenttip;
	}

  private List<String> addInterfaceBody(TileEntity te, List<String> currenttip, IWailaDataAccessor accessor) {
    TileEntityInterface interf = (TileEntityInterface) te;
    if (interf.side != -1) {
      String direction = ForgeDirection.getOrientation(interf.side).toString();
      direction = direction.substring(0, 1) + direction.substring(1).toLowerCase();
      currenttip.add(YELLOW + "Side: " + RESET + direction);

      if(ConfigurationHandler.enableIntegrationIC2) {
        currenttip.add(YELLOW + "EU Capacity: " + RESET + interf.getEUCapacity() + " EU/t");
        currenttip.add("  IN: " + interf.getIncomingEU());
        currenttip.add(" OUT: " + interf.getOutgoingEU());
      }
    }

    return currenttip;
  }
  
  private List<String> addMachineBody(TileEntity te, List<String> currenttip, IWailaDataAccessor  accessor) {
    TileEntityMachine machine = (TileEntityMachine) te;

    if (machine.hasCustomName() && machine.coords != -1 && !machine.getCustomName().equals("Compact Machine")) {
      currenttip.add(ITALIC + machine.getCustomName() + RESET);
    }

    String langStr = "tooltip.cm:machine.size.zero";
    switch (machine.meta) {
    case 0:
      langStr = "tooltip.cm:machine.size.zero";
      break;
    case 1:
      langStr = "tooltip.cm:machine.size.one";
      break;
    case 2:
      langStr = "tooltip.cm:machine.size.two";
      break;
    case 3:
      langStr = "tooltip.cm:machine.size.three";
      break;
    case 4:
      langStr = "tooltip.cm:machine.size.four";
      break;
    case 5:
      langStr = "tooltip.cm:machine.size.five";
      break;
    default:
      break;
    }

    String direction = accessor.getSide().toString();
    direction = direction.substring(0, 1) + direction.substring(1).toLowerCase();
    currenttip.add(YELLOW + "Side: " + RESET + direction);
    currenttip.add(YELLOW + "Size: " + RESET + StatCollector.translateToLocal(langStr));

    if(ConfigurationHandler.enableIntegrationIC2) {
      currenttip.add(YELLOW + "EU Capacity: " + RESET + machine.getEUCapacity() + " EU/t");
      if(machine.getIncomingEU() != 0.0) {
        currenttip.add("  IN: " + machine.getIncomingEU());
      }
      if(machine.getOutgoingEU() != 0.0) {
        currenttip.add(" OUT: " + machine.getOutgoingEU());
      }
    }

    return currenttip;
  }

	@Override
	public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return currenttip;
	}

	@Override
	public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z) {
		return null;
	}

}
