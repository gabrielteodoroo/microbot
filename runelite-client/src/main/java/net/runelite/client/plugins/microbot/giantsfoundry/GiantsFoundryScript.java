package net.runelite.client.plugins.microbot.giantsfoundry;

import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.giantsfoundry.enums.CommissionType;
import net.runelite.client.plugins.microbot.giantsfoundry.enums.Stage;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Item;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment.getEquippedItem;

public class GiantsFoundryScript extends Script {

    static final int CRUCIBLE = 44776;
    static final int MOULD_JIG = 44777;
    static final int LAVA_POOL = 44631;
    static final int WATERFALL = 44632;

    public boolean run(GiantsFoundryConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run()) return;
            try {
                final Rs2Item weapon = getEquippedItem(EquipmentInventorySlot.WEAPON);
                if (GiantsFoundryState.getProgressAmount() == 1000) {
                    handIn();
                    sleep(600, 1200);
                    Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                } else {
                    if (weapon != null) {
                        handleTemperature();
                        craftWeapon();
                    } else {
                        getCommission();
                        selectMould();
                        fillCrucible();
                        pickupMould();
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        GiantsFoundryState.reset();
        super.shutdown();
    }

    public boolean hasCommission() {
        CommissionType type1 = CommissionType.forVarbit(Microbot.getVarbitValue(MouldHelper.SWORD_TYPE_1_VARBIT));
        CommissionType type2 = CommissionType.forVarbit(Microbot.getVarbitValue(MouldHelper.SWORD_TYPE_2_VARBIT));
        return type1 != CommissionType.NONE && type2 != CommissionType.NONE;
    }

    public void getCommission() {
        if (!hasCommission()) {
            GiantsFoundryState.reset();
            if (Rs2Npc.interact("kovac", "Commission"))
                sleepUntil(this::hasCommission, 5000);
        }
    }

    private boolean hasSelectedMould() {
        return (Microbot.getVarbitValue(GiantsFoundryState.VARBIT_BLADE_SELECTED) > 0
                && Microbot.getVarbitValue(GiantsFoundryState.VARBIT_TIP_SELECTED) > 0
                && Microbot.getVarbitValue(GiantsFoundryState.VARBIT_FORTE_SELECTED) > 0);
    }

    public void selectMould() {
        if (hasSelectedMould())
            return;

        Rs2GameObject.interact(MOULD_JIG);

        sleepUntil(() -> Rs2Widget.findWidget("Forte", null) != null, 5000);

        Widget forte = Rs2Widget.findWidget("Forte", null);
        if (forte != null) {
            Microbot.getMouse().click(forte.getBounds());
            sleep(600, 1200);
            MouldHelper.selectBest();
        }

        Widget blades = Rs2Widget.findWidget("Blades", null);
        if (blades != null) {
            Microbot.getMouse().click(blades.getBounds());
            sleep(600, 1200);
            MouldHelper.selectBest();
        }
        Widget tips = Rs2Widget.findWidget("Tips", null);
        if (tips != null) {
            Microbot.getMouse().click(tips.getBounds());
            sleep(600, 1200);
            MouldHelper.selectBest();
            Microbot.getMouse().click(forte.getBounds());
        }
        Widget setMould = Rs2Widget.getWidget(47054854);
        if (setMould != null) {
            Microbot.getMouse().click(setMould.getBounds());
        }
    }

    public boolean canPour() {
        ObjectComposition objectComposition = Rs2GameObject.findObjectComposition(CRUCIBLE);
        if (objectComposition == null) return false;
        return objectComposition.getImpostor().getName().toLowerCase().contains("(full)");
    }

    public void fillCrucible() {
        if (!hasSelectedMould())
            return;

        if (Microbot.getVarbitValue(MouldHelper.SWORD_TYPE_1_VARBIT) == 0) {
            return;
        }
        if (Microbot.getVarbitValue(GiantsFoundryState.VARBIT_GAME_STAGE) != 1) {
            return;
        }

        if (!Rs2Inventory.hasItemAmount("steel bar", 14)
                && !Rs2Inventory.hasItemAmount("mithril bar", 14) && !canPour()) {
            Rs2Bank.useBank();
            //check if inv is empty and deposit all inv items
            //needs new method in rs2bank depositAllRs2InventoryItems
            Rs2Bank.withdrawX(true, "steel bar", 14);
            Rs2Bank.withdrawX(true, "mithril bar", 14);
            Rs2Bank.closeBank();
            return;
        }
        Rs2Bank.closeBank();
        if (Rs2Inventory.hasItem("steel bar") && !canPour()) {
            Rs2GameObject.interact(CRUCIBLE, "Fill");
            sleepUntil(() -> Rs2Widget.findWidget("What metal would you like to add?", null) != null, 5000);
            Rs2Keyboard.keyPress('3');
            sleepUntil(() -> !Rs2Inventory.hasItem("steel bar"), 5000);
        }
        if (Rs2Inventory.hasItem("mithril bar") && !canPour()) {
            Rs2GameObject.interact(CRUCIBLE, "Fill");
            sleepUntil(() -> Rs2Widget.findWidget("What metal would you like to add?", null) != null, 5000);
            sleep(600, 1200);
            Rs2Keyboard.keyPress('4');
            sleepUntil(() -> !Rs2Inventory.hasItem("mithril bar"), 5000);
        }
        if (canPour()) {
            Rs2GameObject.interact(CRUCIBLE, "Pour");
            sleep(5000);
            sleepUntil(() -> !canPour(), 10000);
        }
    }

    public boolean canPickupMould() {
        if (canPour()) return false;
        ObjectComposition objectComposition = Rs2GameObject.findObjectComposition(MOULD_JIG);
        if (objectComposition == null) return false;
        return objectComposition.getImpostor().getName().toLowerCase().contains("poured metal");
    }

    public void pickupMould() {
        if (!canPickupMould()) return;
        if (Rs2Inventory.isEmpty() && GiantsFoundryState.getCurrentStage() == null) {
            Rs2GameObject.interact(MOULD_JIG, "Pick-up");
            sleepUntil(() -> !canPickupMould(), 5000);
        }
    }

    public static boolean isCoolingDown = false;
    public static boolean isHeatingUp = false;

    public void handleTemperature() {
        int actionsLeft = GiantsFoundryState.getActionsForHeatLevel();
        if (actionsLeft > 8) {
            GiantsFoundryScript.isHeatingUp = false;
            GiantsFoundryScript.isCoolingDown = false;
            return;
        }
        int heat = GiantsFoundryState.getHeatAmount();
        if (GiantsFoundryScript.isHeatingUp || GiantsFoundryScript.isCoolingDown) {
            if (heat == 1000 || heat == 0) {
                GiantsFoundryScript.isHeatingUp = false;
                GiantsFoundryScript.isCoolingDown = false;
            }
        }
        int change = GiantsFoundryState.getHeatChangeNeeded();
        if (change == 0 && !isCoolingDown) {
            Rs2GameObject.interact(WATERFALL, "Cool-preform");
            isCoolingDown = true;
            sleepUntil(() -> GiantsFoundryState.getHeatChangeNeeded() == -1 ||  GiantsFoundryState.getActionsForHeatLevel() > 8);
        } else if (change == 1 && !isHeatingUp) {
            Rs2GameObject.interact(LAVA_POOL, "Heat-preform");
            isHeatingUp = true;
            sleepUntil(() -> GiantsFoundryState.getHeatChangeNeeded() == -1||  GiantsFoundryState.getActionsForHeatLevel() > 8);
        }
    }

    public void craftWeapon() {
        if (Microbot.isGainingExp && !BonusWidget.isActive()) return;
        if (GiantsFoundryState.getHeatChangeNeeded() == -1) {
            Stage stage = GiantsFoundryState.getCurrentStage();
            if (stage == null) return;
            GameObject obj = GiantsFoundryState.getStageObject(stage);
            if (obj == null) return;
            Rs2GameObject.interact(obj);
            isCoolingDown = false;
            isHeatingUp = false;
            sleepUntil(() -> Microbot.isGainingExp || GiantsFoundryState.getHeatChangeNeeded() != -1, 5000);
        }
    }

    private void handIn() {
        Rs2Npc.interact("kovac", "Hand-in");
    }

}
