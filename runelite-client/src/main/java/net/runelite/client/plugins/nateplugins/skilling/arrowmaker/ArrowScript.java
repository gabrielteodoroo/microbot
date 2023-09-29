package net.runelite.client.plugins.nateplugins.skilling.arrowmaker;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Inventory;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.concurrent.TimeUnit;

public class ArrowScript extends Script {

    public static double version = 1.0;

    public boolean run(ArrowConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run()) return;

            try {
                if (Microbot.pauseAllScripts) return;
                if (config.ARROWBool()) {
                    checkAndUseItem(config.ARROW().getItem1(), config.ARROW().getItem2());
                }
                if (config.BOLTBool()) {
                    checkAndUseItem(config.BOLT().getItem1(), config.BOLT().getItem2());
                }
                if (config.DARTBool()) {
                    checkAndUseItem(config.DART().getItem1(), config.DART().getItem2());
                }
                if (config.TIPPINGBool()) {
                    checkAndUseItem(config.TIP().getItem1(), config.TIP().getItem2());
                }
                if (config.DRAGONTIPPINGBool()) {
                    checkAndUseItem(config.DragonTIP().getItem1(), config.DragonTIP().getItem2());
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void checkAndUseItem(String item1, String item2) {
        if (Inventory.getAmountForItem(item1) > 0 && Inventory.getAmountForItem(item2) > 0) {
            Inventory.useItemOnItem(item1, item2);
            handleSleep();
        }
    }
    private void handleSleep(){
        sleepUntilOnClientThread(() -> Rs2Widget.getWidget(17694733) != null);
        keyPress('1');
        sleep(12000,14000);
    }
}
