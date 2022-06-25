// Copyright 2022 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.furnishings.logic.WordlyTooltipDisplay;

import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.DisplayNameComponent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.furnishings.logic.door.DoorComponent;
import org.terasology.furnishings.logic.trunk.TrunkComponent;
import org.terasology.gestalt.entitysystem.entity.EntityRef;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.inventory.ui.GetItemTooltip;
import org.terasology.nui.widgets.TooltipLine;
import org.terasology.worldlyTooltipAPI.events.GetTooltipIconEvent;

@RegisterSystem(value = RegisterMode.CLIENT)
public class FurnishingsTooltipSystem extends BaseComponentSystem {
    /*
     * Adds the Name inside WorldlyTooltip to show the DisplayName for Door
     */
    @ReceiveEvent
    public void addDisplayNameToTooltipForDoor(GetItemTooltip event, EntityRef entity, DisplayNameComponent displayNameComponent, DoorComponent doorComponent) {
        event.getTooltipLines().add(new TooltipLine("Name: " + displayNameComponent.name));
    }

    /*
     * Adds the Icon to the WorldlyTooltip to show Door icon
     */
    @ReceiveEvent
    public void addIconToWorldlyTooltipForDoor(GetTooltipIconEvent event, EntityRef entity, ItemComponent itemComponent, DoorComponent doorComponent) {
        event.setIcon(itemComponent.icon);
    }

    /*
     * Adds the Name inside WorldlyTooltip to show the DisplayName for Trunk
     */
    @ReceiveEvent
    public void addDisplayNameToTooltipForTrunk(GetItemTooltip event, EntityRef entity, DisplayNameComponent displayNameComponent, TrunkComponent trunkComponent) {
        event.getTooltipLines().add(new TooltipLine("Name: " + displayNameComponent.name));
    }

    /*
     * Adds the Icon to the WorldlyTooltip to show Trunk icon
     */
    @ReceiveEvent
    public void addIconToWorldlyTooltipForTrunk(GetTooltipIconEvent event, EntityRef entity, ItemComponent itemComponent, TrunkComponent trunkComponent) {
        event.setIcon(itemComponent.icon);
    }
}
