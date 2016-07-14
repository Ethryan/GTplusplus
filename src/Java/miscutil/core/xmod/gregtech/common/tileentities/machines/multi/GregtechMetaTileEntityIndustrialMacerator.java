package miscutil.core.xmod.gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Textures;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_MultiBlockBase;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;

import java.util.ArrayList;
import java.util.Arrays;

import miscutil.core.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

public class GregtechMetaTileEntityIndustrialMacerator
        extends GT_MetaTileEntity_MultiBlockBase {
    private static boolean controller;

    public GregtechMetaTileEntityIndustrialMacerator(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GregtechMetaTileEntityIndustrialMacerator(String aName) {
        super(aName);
    }

    @Override
	public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GregtechMetaTileEntityIndustrialMacerator(this.mName);
    }

    @Override
	public String[] getDescription() {
        return new String[]{
                "Controller Block for the Industrial Maceration Stack",
                "Size(WxHxD): 3x5x3 (Hollow)", 
                "Controller (Center Bottom)",
                "1x Input Bus (Any bottom layer casing)",
                "3x Output Bus (Any casing besides bottom layer)",
                "1x Maintenance Hatch (Any casing)",
                "1x Energy Hatch (Any casing)",
                "Maceration Stack Casings for the rest (26 at least!)"};
    }

    @Override
	public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing) {
            return new ITexture[]{Textures.BlockIcons.CASING_BLOCKS[63], new GT_RenderedTexture(aActive ? Textures.BlockIcons.OVERLAY_FRONT_VACUUM_FREEZER_ACTIVE : Textures.BlockIcons.OVERLAY_FRONT_VACUUM_FREEZER)};
        }
        return new ITexture[]{Textures.BlockIcons.CASING_BLOCKS[63]};
    }

    @Override
	public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "DistillationTower.png");
    }

    @Override
	public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GT_Recipe.GT_Recipe_Map.sMaceratorRecipes;
    }

    @Override
	public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
	public boolean isFacingValid(byte aFacing) {
        return aFacing > 1;
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<ItemStack> tInputList = getStoredInputs();
        for (int i = 0; i < tInputList.size() - 1; i++) {
            for (int j = i + 1; j < tInputList.size(); j++) {
                if (GT_Utility.areStacksEqual((ItemStack) tInputList.get(i), (ItemStack) tInputList.get(j))) {
                    if (((ItemStack) tInputList.get(i)).stackSize >= ((ItemStack) tInputList.get(j)).stackSize) {
                        tInputList.remove(j--);
                    } else {
                        tInputList.remove(i--);
                        break;
                    }
                }
            }
        }
        ItemStack[] tInputs = (ItemStack[]) Arrays.copyOfRange(tInputList.toArray(new ItemStack[tInputList.size()]), 0, 2);
        if (tInputList.size() > 0) {
            GT_Recipe tRecipe = GT_Recipe.GT_Recipe_Map.sMaceratorRecipes.findRecipe(getBaseMetaTileEntity(), false, 9223372036854775807L, null, tInputs);
            if ((tRecipe != null) && (tRecipe.isRecipeInputEqual(true, null, tInputs))) {
                this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
                this.mEfficiencyIncrease = 10000;

                this.mEUt = (-tRecipe.mEUt);
                this.mMaxProgresstime = Math.max(1, tRecipe.mDuration);
                this.mOutputItems = new ItemStack[]{tRecipe.getOutput(0), tRecipe.getOutput(1)};
                sendLoopStart((byte) 20);
                updateSlots();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;
        if (!aBaseMetaTileEntity.getAirOffset(xDir, 1, zDir)) {
            return false;
        }
        int tAmount = 0;
        controller = false;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                for (int h = 0; h < 6; h++) {
                    if (!(i == 0 && j == 0 && (h > 0 && h < 5)))//((h > 0)&&(h<5)) || (((xDir + i != 0) || (zDir + j != 0)) && ((i != 0) || (j != 0)))
                    {
                        IGregTechTileEntity tTileEntity = aBaseMetaTileEntity.getIGregTechTileEntityOffset(xDir + i, h, zDir + j);
                        if ((!addMaintenanceToMachineList(tTileEntity, 63)) && (!addInputToMachineList(tTileEntity, 63)) && (!addOutputToMachineList(tTileEntity, 63)) && (!addEnergyInputToMachineList(tTileEntity, 63)) && (!ignoreController(aBaseMetaTileEntity.getBlockOffset(xDir + i, h, zDir + j)))) {
                            if (aBaseMetaTileEntity.getBlockOffset(xDir + i, h, zDir + j) != ModBlocks.blockCasingsMisc) {
                                return false;
                            }
                            if (aBaseMetaTileEntity.getMetaIDOffset(xDir + i, h, zDir + j) != 1) {
                                return false;
                            }
                            tAmount++;
                        }
                    }
                }
            }
        }
        if (this.mOutputHatches.size() != 1 || this.mInputHatches.size() != 0 || this.mOutputBusses.size() != 5) {
            return false;
        }
        int height = this.getBaseMetaTileEntity().getYCoord();
        if (this.mInputBusses.get(0).getBaseMetaTileEntity().getYCoord() != height || this.mOutputHatches.get(0).getBaseMetaTileEntity().getYCoord() != height) {
            return false;
        }
        GT_MetaTileEntity_Hatch_OutputBus[] tmpHatches = new GT_MetaTileEntity_Hatch_OutputBus[5];
        for (int i = 0; i < this.mOutputBusses.size(); i++) {
            int hatchNumber = this.mOutputBusses.get(i).getBaseMetaTileEntity().getYCoord() - 1 - height;
            if (tmpHatches[hatchNumber] == null) {
                tmpHatches[hatchNumber] = this.mOutputBusses.get(i);
            } else {
                return false;
            }
        }
        this.mOutputBusses.clear();
        for (int i = 0; i < tmpHatches.length; i++) {
            this.mOutputBusses.add(tmpHatches[i]);
        }
        return tAmount >= 26;
    }
    
    /*public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;
        if (!aBaseMetaTileEntity.getAirOffset(xDir, 1, zDir)) {
            return false;
        }
        int tAmount = 0;
        controller = false;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                for (int h = 0; h < 6; h++) {
                    if (!(i == 0 && j == 0 && (h > 0 && h < 5)))//((h > 0)&&(h<5)) || (((xDir + i != 0) || (zDir + j != 0)) && ((i != 0) || (j != 0)))
                    {
                        IGregTechTileEntity tTileEntity = aBaseMetaTileEntity.getIGregTechTileEntityOffset(xDir + i, h, zDir + j);
                        if ((!addMaintenanceToMachineList(tTileEntity, 63)) && (!addInputToMachineList(tTileEntity, 63)) && (!addOutputToMachineList(tTileEntity, 63)) && (!addEnergyInputToMachineList(tTileEntity, 63)) && (!ignoreController(aBaseMetaTileEntity.getBlockOffset(xDir + i, h, zDir + j)))) {
                            if (aBaseMetaTileEntity.getBlockOffset(xDir + i, h, zDir + j) != ModBlocks.blockCasingsMisc) {
                                return false;
                            }
                            if (aBaseMetaTileEntity.getMetaIDOffset(xDir + i, h, zDir + j) != 1) {
                                return false;
                            }
                            tAmount++;
                        }
                    }
                }
            }
        }
        if (this.mOutputBusses.size() != 5 || this.mInputBusses.size() != 1) {
            return false;
        }
        int height = this.getBaseMetaTileEntity().getYCoord();
        if (this.mInputHatches.get(0).getBaseMetaTileEntity().getYCoord() != height) {
            return false;
        }
        GT_MetaTileEntity_Hatch_OutputBus[] tmpHatches = new GT_MetaTileEntity_Hatch_OutputBus[5];
        for (int i = 0; i < this.mOutputBusses.size(); i++) {
            int hatchNumber = this.mOutputBusses.get(i).getBaseMetaTileEntity().getYCoord() - 1 - height;
            if (tmpHatches[hatchNumber] == null) {
                tmpHatches[hatchNumber] = this.mOutputBusses.get(i);
            } else {
                return false;
            }
        }
        this.mOutputBusses.clear();
        for (int i = 0; i < tmpHatches.length; i++) {
            this.mOutputBusses.add(tmpHatches[i]);
        }
        return tAmount >= 26;
    }*/

    public boolean ignoreController(Block tTileEntity) {
        if (!controller && tTileEntity == GregTech_API.sBlockMachines) {
            return true;
        }
        return false;
    }

    @Override
	public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
	public int getPollutionPerTick(ItemStack aStack) {
        return 0;
    }

    @Override
	public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
	public int getAmountOfOutputs() {
        return 1;
    }

    @Override
	public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }
}