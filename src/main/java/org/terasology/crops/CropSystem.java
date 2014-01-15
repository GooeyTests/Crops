package org.terasology.crops;

import org.terasology.engine.Time;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnAddedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.In;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Vector3i;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

@RegisterSystem(RegisterMode.AUTHORITY)
public class CropSystem implements ComponentSystem, UpdateSubscriberSystem {
    private static final int CHECK_INTERVAL = 1000;

    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;
    @In
    private Time timer;
    @In
    private WorldProvider worldprovider;

    private long lastCheckTime;

    @Override
    public void initialise() {
    }

    @ReceiveEvent(components = {CropComponent.class})
    public void onSpawn(OnAddedComponent event, EntityRef entity) {
        long initTime = timer.getGameTimeInMs();
        //add 3000 to init to create  bit of a delay before first check
        CropComponent crop = entity.getComponent(CropComponent.class);
        crop.lastgrowthcheck = initTime;
        entity.saveComponent(crop);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void update(float delta) {
        // System last time check is to try to improve performance
        long gameTimeInMs = timer.getGameTimeInMs();
        if (lastCheckTime + CHECK_INTERVAL < gameTimeInMs) {
            for (EntityRef cropEntity : entityManager.getEntitiesWith(CropComponent.class, BlockComponent.class, LocationComponent.class)) {
                CropComponent crop = cropEntity.getComponent(CropComponent.class);
                if (crop.fullgrown) {
                    return;
                }
                if (crop.lastgrowthcheck == -1) {
                    crop.lastgrowthcheck = gameTimeInMs;
                    cropEntity.saveComponent(crop);
                    return;
                }
                if (gameTimeInMs - crop.lastgrowthcheck > crop.timeInGameMsToNextStage) {
                    crop.lastgrowthcheck = timer.getGameTimeInMs();
                        LocationComponent locComponent = cropEntity.getComponent(LocationComponent.class);
                        Block currentBlock = worldprovider.getBlock(locComponent.getWorldPosition());
                        String currentBlockFamilyStage = currentBlock.getURI().toString();
                        int currentstageIndex = crop.blockFamilyStages.indexOf(currentBlockFamilyStage);
                        int lastStageIndex = crop.blockFamilyStages.size() - 1;
                        if (lastStageIndex > currentstageIndex) {
                            currentstageIndex++;
                            if (currentstageIndex == lastStageIndex) {
                                crop.fullgrown = true;
                            }
                            String newBlockUri = crop.blockFamilyStages.get(currentstageIndex);
                            Block newBlock = blockManager.getBlock(newBlockUri);
                            if (newBlockUri.equals(newBlock.getURI().toString())) {
                                worldprovider.setBlock(new Vector3i(locComponent.getWorldPosition()), newBlock);
                            }
                        }
                        cropEntity.saveComponent(crop);
                }
            }
            lastCheckTime = gameTimeInMs;
        }
    }
}
