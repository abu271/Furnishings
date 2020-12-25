/*
 * Copyright 2020 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.furnishings.logic.trunk;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.audio.AudioManager;
import org.terasology.audio.events.PlaySoundEvent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.metadata.EntitySystemLibrary;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.JomlUtil;
import org.terasology.math.Side;
import org.terasology.registry.In;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.utilities.Assets;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockRegion;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.regions.BlockRegionComponent;

import java.util.HashMap;
import java.util.Map;

@RegisterSystem(RegisterMode.AUTHORITY)
public class TrunkSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(TrunkSystem.class);

    @In
    private WorldProvider worldProvider;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private EntityManager entityManager;
    @In
    private AudioManager audioManager;
    @In
    private EntitySystemLibrary entitySystemLibrary;

    @ReceiveEvent(components = {TrunkComponent.class, ItemComponent.class})
    public void placeTrunk(ActivateEvent event, EntityRef entity) {
        TrunkComponent trunk = entity.getComponent(TrunkComponent.class);

        BlockComponent targetBlockComp = event.getTarget().getComponent(BlockComponent.class);
        if (targetBlockComp == null) {
            event.consume();
            return;
        }

        Vector3f horizDir =
                new Vector3f(event.getDirection())
                        .setComponent(1, 0); // set y dimension to 0
        Side facingDir = Side.inDirection(horizDir);
        if (!facingDir.isHorizontal()) {
            event.consume();
            return;
        }

        Vector3f offset =
                new Vector3f(event.getHitPosition())
                        .sub(targetBlockComp.position.x, targetBlockComp.position.y, targetBlockComp.position.z);
        Side offsetDir = Side.inDirection(offset);

        Vector3i primePos =
                JomlUtil.from(targetBlockComp.position)
                        .add(offsetDir.direction());

        Block primeBlock = worldProvider.getBlock(primePos);
        if (!primeBlock.isReplacementAllowed()) {
            event.consume();
            return;
        }

        Block leftBlock;
        Block rightBlock;
        boolean isX = false;

        if (facingDir == Side.LEFT || facingDir == Side.RIGHT) {
            leftBlock = worldProvider.getBlock(primePos.x, primePos.y, primePos.z - 1);
            rightBlock = worldProvider.getBlock(primePos.x, primePos.y, primePos.z + 1);
        } else {
            isX = true;
            leftBlock = worldProvider.getBlock(primePos.x - 1, primePos.y, primePos.z);
            rightBlock = worldProvider.getBlock(primePos.x + 1, primePos.y, primePos.z);
        }

        // Determine top and bottom blocks
        Vector3i leftBlockPos = new Vector3i();
        Vector3i rightBlockPos = new Vector3i();
        if (leftBlock.isReplacementAllowed()) {
            leftBlockPos.set(isX ? primePos.x - 1 : primePos.x, primePos.y, isX ? primePos.z : primePos.z - 1);
            rightBlockPos.set(primePos);
        } else if (rightBlock.isReplacementAllowed()) {
            leftBlockPos.set(primePos);
            rightBlockPos.set(isX ? primePos.x + 1 : primePos.x, primePos.y, isX ? primePos.z : primePos.z + 1);
        } else {
            event.consume();
            return;
        }

        Block newBottomBlock;
        Block newTopBlock;

        if (facingDir == Side.FRONT || facingDir == Side.RIGHT) {
            //TODO: can this be replace by `new BlockPlacementData(leftBlockPos, Side.BOTTOM, facingDir.reverse()
            // .direction())`
            newBottomBlock = trunk.rightBlockFamily.getBlockForPlacement(JomlUtil.from(leftBlockPos), Side.BOTTOM,
                    facingDir.reverse());
            newTopBlock = trunk.leftBlockFamily.getBlockForPlacement(JomlUtil.from(rightBlockPos), Side.BOTTOM,
                    facingDir.reverse());
        } else {
            newBottomBlock = trunk.leftBlockFamily.getBlockForPlacement(JomlUtil.from(leftBlockPos), Side.BOTTOM,
                    facingDir.reverse());
            newTopBlock = trunk.rightBlockFamily.getBlockForPlacement(JomlUtil.from(rightBlockPos), Side.BOTTOM,
                    facingDir.reverse());
        }
        Map<org.joml.Vector3i, Block> blockMap = new HashMap<>();
        blockMap.put(leftBlockPos, newBottomBlock);
        blockMap.put(rightBlockPos, newTopBlock);
        PlaceBlocks blockEvent = new PlaceBlocks(blockMap, event.getInstigator());
        worldProvider.getWorldEntity().send(blockEvent);

        if (!blockEvent.isConsumed()) {
            EntityRef newTrunk = entityManager.create(trunk.trunkRegionPrefab);
            entity.removeComponent(MeshComponent.class);
            newTrunk.addComponent(new BlockRegionComponent(new BlockRegion(leftBlockPos).union(rightBlockPos)));

            newTrunk.addComponent(new LocationComponent(JomlUtil.from(new Vector3f(rightBlockPos))));

            TrunkComponent newDoorComp = newTrunk.getComponent(TrunkComponent.class);
            newTrunk.saveComponent(newDoorComp);
            newTrunk.send(new PlaySoundEvent(Assets.getSound("engine:PlaceBlock").get(), 0.5f));
            newTrunk.send(new TrunkPlacedEvent(event.getInstigator()));
        }
    }

    @ReceiveEvent(components = {TrunkComponent.class, BlockRegionComponent.class, LocationComponent.class})
    public void onOpen(ActivateEvent event, EntityRef entity) {
        //logger.info("We got activated");
    }
}
