package rsfost.loadtime;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.events.ItemContainerChanged;

import javax.inject.Inject;

@Slf4j
public class LoadTimeEstimator
{
    private static final long NANOS_PER_MILLI = 1000000;

    private final Client client;

    private long lastGameTickNanos;
    private boolean jawPreviouslyEquipped;

    private int gameTickCounter = 0;

    @Inject
    public LoadTimeEstimator(Client client)
    {
        this.client = client;
    }

    public void addTick()
    {
        ++gameTickCounter;
//        log.info("tick {}", gameTickCounter);
        this.lastGameTickNanos = System.nanoTime();
    }

    public void checkItemContainer(ItemContainerChanged event)
    {
        final long currentNanos = System.nanoTime();

//        if (event.getContainerId() != InventoryID.EQUIPMENT.getId())
//        {
//            return;
//        }
//
//        final boolean jawEquipped = event.getItemContainer().getItem(EquipmentInventorySlot.JAW.getSlotIdx()) != null;
//        if ((jawEquipped && !jawPreviouslyEquipped) || (!jawEquipped && jawPreviouslyEquipped))
//        {
//            log.info("(tick {}) jaw load: {}ms", gameTickCounter, (currentNanos - lastGameTickNanos - 600 * NANOS_PER_MILLI) / NANOS_PER_MILLI);
//        }

        if (event.getContainerId() != InventoryID.INVENTORY.getId())
        {
            return;
        }
        final boolean jawEquipped = event.getItemContainer().getItem(0) != null;
        if ((jawEquipped && !jawPreviouslyEquipped) || (!jawEquipped && jawPreviouslyEquipped))
        {
            log.info("(tick {}) jaw load: {}ms", gameTickCounter, (currentNanos - lastGameTickNanos - 600 * NANOS_PER_MILLI) / NANOS_PER_MILLI);
        }

        jawPreviouslyEquipped = jawEquipped;
    }
}
