package mekanism.common.util;

import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import mekanism.api.Object3D;
import mekanism.api.energy.ICableOutputter;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.EnergyNetwork;
import mekanism.common.Mekanism;
import mekanism.common.tileentity.TileEntityElectricBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.block.IConductor;
import universalelectricity.core.block.IElectrical;
import universalelectricity.core.electricity.ElectricityHelper;
import universalelectricity.core.electricity.ElectricityPack;
import universalelectricity.core.grid.IElectricityNetwork;
import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler.PowerReceiver;
import buildcraft.api.power.PowerHandler.Type;

public final class CableUtils
{
    /**
     * Gets all the connected energy acceptors, whether IC2-based or BuildCraft-based, surrounding a specific tile entity.
     * @param tileEntity - center tile entity
     * @return TileEntity[] of connected acceptors
     */
    public static TileEntity[] getConnectedEnergyAcceptors(TileEntity tileEntity)
    {
    	TileEntity[] acceptors = new TileEntity[] {null, null, null, null, null, null};
    	
    	for(ForgeDirection orientation : ForgeDirection.VALID_DIRECTIONS)
    	{
			TileEntity acceptor = Object3D.get(tileEntity).getFromSide(orientation).getTileEntity(tileEntity.worldObj);
			
			if(acceptor instanceof IStrictEnergyAcceptor || 
					acceptor instanceof IEnergySink || 
					(acceptor instanceof IPowerReceptor && !(acceptor instanceof ITransmitter) && MekanismUtils.useBuildcraft()) ||
					acceptor instanceof IElectrical)
			{
				acceptors[orientation.ordinal()] = acceptor;
			}
    	}
    	
    	return acceptors;
    }
    
    /**
     * Gets all the connected cables around a specific tile entity.
     * @param tileEntity - center tile entity
     * @return TileEntity[] of connected cables
     */
    public static TileEntity[] getConnectedCables(TileEntity tileEntity)
    {
    	TileEntity[] cables = new TileEntity[] {null, null, null, null, null, null};
    	
    	for(ForgeDirection orientation : ForgeDirection.VALID_DIRECTIONS)
    	{
			TileEntity cable = Object3D.get(tileEntity).getFromSide(orientation).getTileEntity(tileEntity.worldObj);
			
			if(TransmissionType.checkTransmissionType(cable, TransmissionType.ENERGY))
			{
				cables[orientation.ordinal()] = cable;
			}
    	}
    	
    	return cables;
    }
    
    /**
     * Gets all the adjacent connections to a TileEntity.
     * @param tileEntity - center TileEntity
     * @return boolean[] of adjacent connections
     */
    public static boolean[] getConnections(TileEntity tileEntity)
    {
		boolean[] connectable = new boolean[] {false, false, false, false, false, false};
		
		TileEntity[] connectedAcceptors = getConnectedEnergyAcceptors(tileEntity);
		TileEntity[] connectedCables = getConnectedCables(tileEntity);
		TileEntity[] connectedOutputters = getConnectedOutputters(tileEntity);
		
		for(TileEntity tile : connectedAcceptors)
		{
			int side = Arrays.asList(connectedAcceptors).indexOf(tile);
			
			if(canConnectToAcceptor(ForgeDirection.getOrientation(side), tileEntity))
			{
				connectable[side] = true;
			}
		}
		
		for(TileEntity tile : connectedOutputters)
		{
			if(tile != null)
			{
				int side = Arrays.asList(connectedOutputters).indexOf(tile);
				
				connectable[side] = true;
			}
		}
		
		for(TileEntity tile : connectedCables)
		{
			if(tile != null)
			{
				int side = Arrays.asList(connectedCables).indexOf(tile);
				
				connectable[side] = true;
			}
		}
		
		return connectable;
    }
    
    /**
     * Gets all the connected cables around a specific tile entity.
     * @param tileEntity - center tile entity
     * @return TileEntity[] of connected cables
     */
    public static TileEntity[] getConnectedOutputters(TileEntity tileEntity)
    {
    	TileEntity[] outputters = new TileEntity[] {null, null, null, null, null, null};
    	
    	for(ForgeDirection orientation : ForgeDirection.VALID_DIRECTIONS)
    	{
			TileEntity outputter = Object3D.get(tileEntity).getFromSide(orientation).getTileEntity(tileEntity.worldObj);
			
			if((outputter instanceof ICableOutputter && ((ICableOutputter)outputter).canOutputTo(orientation.getOpposite())) || 
					(outputter instanceof IEnergySource && ((IEnergySource)outputter).emitsEnergyTo(tileEntity, orientation.getOpposite())) ||
					(outputter instanceof IElectrical && ((IElectrical)outputter).canConnect(orientation.getOpposite())))
			{
				outputters[orientation.ordinal()] = outputter;
			}
    	}
    	
    	return outputters;
    }
    
    /**
     * Whether or not a cable can connect to a specific acceptor.
     * @param side - side to check
     * @param tileEntity - cable TileEntity
     * @return whether or not the cable can connect to the specific side
     */
    public static boolean canConnectToAcceptor(ForgeDirection side, TileEntity tile)
    {
    	TileEntity tileEntity = Object3D.get(tile).getFromSide(side).getTileEntity(tile.worldObj);
    	
    	if(tileEntity instanceof IStrictEnergyAcceptor && ((IStrictEnergyAcceptor)tileEntity).canReceiveEnergy(side.getOpposite()))
    	{
    		return true;
    	}
    	
    	if(tileEntity instanceof IEnergyAcceptor && ((IEnergyAcceptor)tileEntity).acceptsEnergyFrom(tile, side.getOpposite()))
    	{
    		return true;
    	}
    	
    	if(tileEntity instanceof ICableOutputter && ((ICableOutputter)tileEntity).canOutputTo(side.getOpposite()))
    	{
    		return true;
    	}
    	
    	if(tileEntity instanceof IElectrical && ((IElectrical)tileEntity).canConnect(side.getOpposite()))
    	{
    		return true;
    	}
    	
    	if(tileEntity instanceof IPowerReceptor && !(tileEntity instanceof ITransmitter) && MekanismUtils.useBuildcraft())
    	{
    		if(!(tileEntity instanceof IEnergyAcceptor) || ((IEnergyAcceptor)tileEntity).acceptsEnergyFrom(null, side.getOpposite()))
    		{
    			if(!(tileEntity instanceof IEnergySource) || ((IEnergySource)tileEntity).emitsEnergyTo(null, side.getOpposite()))
    			{
    				return true;
    			}
    		}
    	}
    	
    	return false;
    }
    
    /**
     * Emits a defined amount of energy to the network, distributing between IC2-based and BuildCraft-based acceptors.
     * @param amount - amount to send
     * @param sender - sending TileEntity
     * @param facing - direction the TileEntity is facing
     * @return rejected energy
     */
    public static double emitEnergyToNetwork(double amount, TileEntity sender, ForgeDirection facing)
    {
    	TileEntity pointer = Object3D.get(sender).getFromSide(facing).getTileEntity(sender.worldObj);
    	
    	if(TransmissionType.checkTransmissionType(pointer, TransmissionType.ENERGY))
    	{
    		ITransmitter<EnergyNetwork> cable = (ITransmitter<EnergyNetwork>)pointer;
    		
    		ArrayList<TileEntity> ignored = new ArrayList<TileEntity>();
    		ignored.add(sender);
    		
    		return cable.getTransmitterNetwork().emit(amount, ignored);
    	}
    	
    	return amount;
    }
    
    /**
     * Emits energy from all sides of a TileEntity.
     * @param amount - amount to send
     * @param pointer - sending TileEntity
     * @param ignored - ignored acceptors
     * @return rejected energy
     */
    public static double emitEnergyFromAllSides(double amount, TileEntity pointer, ArrayList<TileEntity> ignored)
    {
    	if(pointer != null)
    	{
    		Set<EnergyNetwork> networks = new HashSet<EnergyNetwork>();
    		double totalRemaining = 0;
    		
    		ignored.add(pointer);
    		
    		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
    		{
    			TileEntity sideTile = Object3D.get(pointer).getFromSide(side).getTileEntity(pointer.worldObj);
    			
    			if(TransmissionType.checkTransmissionType(sideTile, TransmissionType.ENERGY) && !ignored.contains(sideTile))
    			{
    				networks.add(((ITransmitter<EnergyNetwork>)sideTile).getTransmitterNetwork());
    			}
    		}
    		
    		if(networks.size() == 0)
    		{
    			return amount;
    		}
    		
    		double remaining = amount%networks.size();
    		double splitEnergy = (amount-remaining)/networks.size();
    		
    		for(EnergyNetwork network : networks)
    		{
    			totalRemaining += network.emit(splitEnergy+remaining, ignored);
    			remaining = 0;
    		}
    		
    		return totalRemaining;
    	}
    	
    	return amount;
    }
    
    public static void emit(TileEntityElectricBlock emitter)
    {
		if(!emitter.worldObj.isRemote && MekanismUtils.canFunction(emitter))
		{
			TileEntity tileEntity = Object3D.get(emitter).getFromSide(emitter.getOutputtingSide()).getTileEntity(emitter.worldObj);
			
			if(emitter.getEnergy() > 0)
			{
				if(TransmissionType.checkTransmissionType(tileEntity, TransmissionType.ENERGY))
				{
					double energyToSend = Math.min(emitter.getEnergy(), emitter.getMaxOutput());
					emitter.setEnergy(emitter.getEnergy() - (energyToSend - emitEnergyToNetwork(energyToSend, emitter, emitter.getOutputtingSide())));
					return;
				}
				else if(tileEntity instanceof IStrictEnergyAcceptor)
				{
					IStrictEnergyAcceptor acceptor = (IStrictEnergyAcceptor)tileEntity;
					double toSend = Math.min(emitter.getEnergy(), emitter.getMaxOutput());
					emitter.setEnergy(emitter.getEnergy() - (toSend - acceptor.transferEnergyToAcceptor(toSend)));
				}
				else if(tileEntity instanceof IPowerReceptor && MekanismUtils.useBuildcraft())
				{
					PowerReceiver receiver = ((IPowerReceptor)tileEntity).getPowerReceiver(emitter.getOutputtingSide().getOpposite());
					
					if(receiver != null)
					{
		            	double transferEnergy = Math.min(emitter.getEnergy(), Math.min(receiver.powerRequest()*Mekanism.FROM_BC, emitter.getMaxOutput()));
		            	float sent = receiver.receiveEnergy(Type.STORAGE, (float)(transferEnergy*Mekanism.TO_BC), emitter.getOutputtingSide().getOpposite());
		            	emitter.setEnergy(emitter.getEnergy() - sent);
					}
				}
			}
			
			if(tileEntity instanceof IConductor)
			{
				ForgeDirection outputDirection = emitter.getOutputtingSide();
				float provide = emitter.getProvide(outputDirection);
	
				if(provide > 0)
				{
					IElectricityNetwork outputNetwork = ElectricityHelper.getNetworkFromTileEntity(tileEntity, outputDirection);
		
					if(outputNetwork != null)
					{
						ElectricityPack request = outputNetwork.getRequest(emitter);
						
						if(request.getWatts() > 0)
						{
							ElectricityPack sendPack = ElectricityPack.min(ElectricityPack.getFromWatts(emitter.getEnergyStored(), emitter.getVoltage()), ElectricityPack.getFromWatts(provide, emitter.getVoltage()));
							float rejectedPower = outputNetwork.produce(sendPack, emitter);
							emitter.setEnergyStored(emitter.getEnergyStored() - (sendPack.getWatts() - rejectedPower));
						}
					}
				}
			}
		}
    }
}
