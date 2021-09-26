package org.zeith.lux.luxpack.apis.js;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSetMultimap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.*;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class JSWorld
{
	public final World instance;
	public boolean captureBlockSnapshots;
	public final List<TileEntity> tickableTileEntities;
	public float thunderingStrength;
	public List<IWorldEventListener> eventListeners;
	public java.util.ArrayList<BlockSnapshot> capturedBlockSnapshots;
	public final Random rand;
	public final List<EntityPlayer> playerEntities;
	public final Profiler profiler;
	public float prevThunderingStrength;
	public float prevRainingStrength;
	public boolean restoringBlockSnapshots;
	public static double MAX_ENTITY_RADIUS = World.MAX_ENTITY_RADIUS;
	public final boolean isRemote;
	public final WorldProvider provider;
	public float rainingStrength;
	public final List<Entity> weatherEffects;
	public VillageCollection villageCollection;
	public final List<Entity> loadedEntityList;
	public final List<TileEntity> loadedTileEntityList;

	public JSWorld(World instance)
	{
		this.instance = instance;
		tickableTileEntities = instance.tickableTileEntities;
		loadedTileEntityList = instance.loadedTileEntityList;
		rainingStrength = instance.rainingStrength;
		captureBlockSnapshots = instance.captureBlockSnapshots;
		loadedEntityList = instance.loadedEntityList;
		playerEntities = instance.playerEntities;
		prevThunderingStrength = instance.prevThunderingStrength;
		rand = instance.rand;
		weatherEffects = instance.weatherEffects;
		prevRainingStrength = instance.prevRainingStrength;
		isRemote = instance.isRemote;
		profiler = instance.profiler;
		villageCollection = instance.villageCollection;
		capturedBlockSnapshots = instance.capturedBlockSnapshots;
		provider = instance.provider;
		thunderingStrength = instance.thunderingStrength;
		eventListeners = instance.eventListeners;
		restoringBlockSnapshots = instance.restoringBlockSnapshots;
	}

	public void setAllowedSpawnTypes(boolean par0, boolean par1)
	{
		instance.setAllowedSpawnTypes(par0, par1);
	}

	public long getWorldTime()
	{
		return instance.getWorldTime();
	}

	public boolean isUpdateScheduled(BlockPos par0, Block par1)
	{
		return instance.isUpdateScheduled(par0, par1);
	}

	public boolean isBlockFullCube(BlockPos par0)
	{
		return instance.isBlockFullCube(par0);
	}

	public void updateComparatorOutputLevel(BlockPos par0, Block par1)
	{
		instance.updateComparatorOutputLevel(par0, par1);
	}

	public boolean tickUpdates(boolean par0)
	{
		return instance.tickUpdates(par0);
	}

	public void removeEventListener(IWorldEventListener par0)
	{
		instance.removeEventListener(par0);
	}

	public void setSeaLevel(int par0)
	{
		instance.setSeaLevel(par0);
	}

	public void notifyNeighborsOfStateExcept(BlockPos par0, Block par1, EnumFacing par2)
	{
		instance.notifyNeighborsOfStateExcept(par0, par1, par2);
	}

	public int getStrongPower(BlockPos par0)
	{
		return instance.getStrongPower(par0);
	}

	public WorldSavedData loadData(Class<? extends WorldSavedData> par0, String par1)
	{
		return instance.loadData(par0, par1);
	}

	public boolean setBlockState(BlockPos par0, IBlockState par1)
	{
		return instance.setBlockState(par0, par1);
	}

	public boolean isAnyPlayerWithinRangeAt(double par0, double par1, double par2, double par3)
	{
		return instance.isAnyPlayerWithinRangeAt(par0, par1, par2, par3);
	}

	public boolean isBlockTickPending(BlockPos par0, Block par1)
	{
		return instance.isBlockTickPending(par0, par1);
	}

	public boolean isBlockNormalCube(BlockPos par0, boolean par1)
	{
		return instance.isBlockNormalCube(par0, par1);
	}

	public boolean isRainingAt(BlockPos par0)
	{
		return instance.isRainingAt(par0);
	}

	public double getHorizon()
	{
		return instance.getHorizon();
	}

	public void calculateInitialSkylight()
	{
		instance.calculateInitialSkylight();
	}

	public void sendBlockBreakProgress(int par0, BlockPos par1, int par2)
	{
		instance.sendBlockBreakProgress(par0, par1, par2);
	}

	public EntityPlayer getNearestAttackablePlayer(double par0, double par1, double par2, double par3, double par4, Function<EntityPlayer, Double> par5, Predicate<EntityPlayer> par6)
	{
		return instance.getNearestAttackablePlayer(par0, par1, par2, par3, par4, par5, par6);
	}

	public void sendQuittingDisconnectingPacket()
	{
		instance.sendQuittingDisconnectingPacket();
	}

	public int countEntities(Class<?> par0)
	{
		return instance.countEntities(par0);
	}

	public boolean isBlockLoaded(BlockPos par0, boolean par1)
	{
		return instance.isBlockLoaded(par0, par1);
	}

	public net.minecraft.world.DifficultyInstance getDifficultyForLocation(BlockPos par0)
	{
		return instance.getDifficultyForLocation(par0);
	}

	public Chunk getChunk(BlockPos par0)
	{
		return instance.getChunk(par0);
	}

	public int getCombinedLight(BlockPos par0, int par1)
	{
		return instance.getCombinedLight(par0, par1);
	}

	public boolean canBlockFreeze(BlockPos par0, boolean par1)
	{
		return instance.canBlockFreeze(par0, par1);
	}

	public float getSunBrightnessBody(float par0)
	{
		return instance.getSunBrightnessBody(par0);
	}

	public void removeEntityDangerously(Entity par0)
	{
		instance.removeEntityDangerously(par0);
	}

	public boolean isAreaLoaded(BlockPos par0, int par1, boolean par2)
	{
		return instance.isAreaLoaded(par0, par1, par2);
	}

	public void updateEntityWithOptionalForce(Entity par0, boolean par1)
	{
		instance.updateEntityWithOptionalForce(par0, par1);
	}

	public EntityPlayer getClosestPlayer(double par0, double par1, double par2, double par3, boolean par4)
	{
		return instance.getClosestPlayer(par0, par1, par2, par3, par4);
	}

	public void updateBlockTick(BlockPos par0, Block par1, int par2, int par3)
	{
		instance.updateBlockTick(par0, par1, par2, par3);
	}

	public boolean isRaining()
	{
		return instance.isRaining();
	}

	public void playBroadcastSound(int par0, BlockPos par1, int par2)
	{
		instance.playBroadcastSound(par0, par1, par2);
	}

	public void onEntityAdded(Entity par0)
	{
		instance.onEntityAdded(par0);
	}

	public float getLightBrightness(BlockPos par0)
	{
		return instance.getLightBrightness(par0);
	}

	public void addTileEntities(java.util.Collection<TileEntity> par0)
	{
		instance.addTileEntities(par0);
	}

	public boolean isAreaLoaded(StructureBoundingBox par0)
	{
		return instance.isAreaLoaded(par0);
	}

	public int getLight(BlockPos par0, boolean par1)
	{
		return instance.getLight(par0, par1);
	}

	public BiomeProvider getBiomeProvider()
	{
		return instance.getBiomeProvider();
	}

	public boolean isAreaLoaded(BlockPos par0, BlockPos par1, boolean par2)
	{
		return instance.isAreaLoaded(par0, par1, par2);
	}

	public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> par0, AxisAlignedBB par1, Predicate<? super T> par2)
	{
		return instance.getEntitiesWithinAABB(par0, par1, par2);
	}

	public List<Entity> getLoadedEntityList()
	{
		return instance.getLoadedEntityList();
	}

	public String getProviderName()
	{
		return instance.getProviderName();
	}

	public boolean isBlockLoaded(BlockPos par0)
	{
		return instance.isBlockLoaded(par0);
	}

	public GameRules getGameRules()
	{
		return instance.getGameRules();
	}

	public boolean checkNoEntityCollision(AxisAlignedBB par0)
	{
		return instance.checkNoEntityCollision(par0);
	}

	public void setWorldTime(long par0)
	{
		instance.setWorldTime(par0);
	}

	public EntityPlayer getPlayerEntityByUUID(java.util.UUID par0)
	{
		return instance.getPlayerEntityByUUID(par0);
	}

	public List<Entity> getEntitiesInAABBexcluding(Entity par0, AxisAlignedBB par1, Predicate<? super Entity> par2)
	{
		return instance.getEntitiesInAABBexcluding(par0, par1, par2);
	}

	public boolean canSnowAtBody(BlockPos par0, boolean par1)
	{
		return instance.canSnowAtBody(par0, par1);
	}

	public void tick()
	{
		instance.tick();
	}

	public boolean isAreaLoaded(BlockPos par0, BlockPos par1)
	{
		return instance.isAreaLoaded(par0, par1);
	}

	public void notifyLightSet(BlockPos par0)
	{
		instance.notifyLightSet(par0);
	}

	public boolean containsAnyLiquid(AxisAlignedBB par0)
	{
		return instance.containsAnyLiquid(par0);
	}

	public Vec3d getSkyColor(Entity par0, float par1)
	{
		return instance.getSkyColor(par0, par1);
	}

	public void setLastLightningBolt(int par0)
	{
		instance.setLastLightningBolt(par0);
	}

	public int getBlockLightOpacity(BlockPos par0)
	{
		return instance.getBlockLightOpacity(par0);
	}

	public void updateWeatherBody()
	{
		instance.updateWeatherBody();
	}

	public void loadEntities(java.util.Collection<Entity> par0)
	{
		instance.loadEntities(par0);
	}

	public int getChunksLowestHorizon(int par0, int par1)
	{
		return instance.getChunksLowestHorizon(par0, par1);
	}

	public boolean isSideSolid(BlockPos par0, EnumFacing par1, boolean par2)
	{
		return instance.isSideSolid(par0, par1, par2);
	}

	public void setTileEntity(BlockPos par0, TileEntity par1)
	{
		instance.setTileEntity(par0, par1);
	}

	public long getSeed()
	{
		return instance.getSeed();
	}

	public void setEntityState(Entity par0, byte par1)
	{
		instance.setEntityState(par0, par1);
	}

	public boolean isOutsideBuildHeight(BlockPos par0)
	{
		return instance.isOutsideBuildHeight(par0);
	}

	public void checkSessionLock() throws MinecraftException
	{
		instance.checkSessionLock();
	}

	public BlockPos getSpawnPoint()
	{
		return instance.getSpawnPoint();
	}

	public Vec3d getSkyColorBody(Entity par0, float par1)
	{
		return instance.getSkyColorBody(par0, par1);
	}

	public void playSound(double par0, double par1, double par2, SoundEvent par3, SoundCategory par4, float par5, float par6, boolean par7)
	{
		instance.playSound(par0, par1, par2, par3, par4, par5, par6, par7);
	}

	public float getCurrentMoonPhaseFactor()
	{
		return instance.getCurrentMoonPhaseFactor();
	}

	public int getLightFromNeighborsFor(EnumSkyBlock par0, BlockPos par1)
	{
		return instance.getLightFromNeighborsFor(par0, par1);
	}

	public void notifyNeighborsOfStateChange(BlockPos par0, Block par1, boolean par2)
	{
		instance.notifyNeighborsOfStateChange(par0, par1, par2);
	}

	public void immediateBlockTick(BlockPos par0, IBlockState par1, Random par2)
	{
		instance.immediateBlockTick(par0, par1, par2);
	}

	public boolean isAirBlock(BlockPos par0)
	{
		return instance.isAirBlock(par0);
	}

	public WorldBorder getWorldBorder()
	{
		return instance.getWorldBorder();
	}

	public int getRedstonePower(BlockPos par0, EnumFacing par1)
	{
		return instance.getRedstonePower(par0, par1);
	}

	public int calculateSkylightSubtracted(float par0)
	{
		return instance.calculateSkylightSubtracted(par0);
	}

	public boolean canBlockFreezeBody(BlockPos par0, boolean par1)
	{
		return instance.canBlockFreezeBody(par0, par1);
	}

	public boolean isSidePowered(BlockPos par0, EnumFacing par1)
	{
		return instance.isSidePowered(par0, par1);
	}

	public float getCelestialAngle(float par0)
	{
		return instance.getCelestialAngle(par0);
	}

	public BlockPos findNearestStructure(String par0, BlockPos par1, boolean par2)
	{
		return instance.findNearestStructure(par0, par1, par2);
	}

	public IChunkProvider getChunkProvider()
	{
		return instance.getChunkProvider();
	}

	public void markBlockRangeForRenderUpdate(int par0, int par1, int par2, int par3, int par4, int par5)
	{
		instance.markBlockRangeForRenderUpdate(par0, par1, par2, par3, par4, par5);
	}

	public void updateObservingBlocksAt(BlockPos par0, Block par1)
	{
		instance.updateObservingBlocksAt(par0, par1);
	}

	public float getCelestialAngleRadians(float par0)
	{
		return instance.getCelestialAngleRadians(par0);
	}

	public int getRedstonePowerFromNeighbors(BlockPos par0)
	{
		return instance.getRedstonePowerFromNeighbors(par0);
	}

	public float getThunderStrength(float par0)
	{
		return instance.getThunderStrength(par0);
	}

	public void removeEntity(Entity par0)
	{
		instance.removeEntity(par0);
	}

	public boolean canMineBlockBody(EntityPlayer par0, BlockPos par1)
	{
		return instance.canMineBlockBody(par0, par1);
	}

	public void updateEntities()
	{
		instance.updateEntities();
	}

	public boolean collidesWithAnyBlock(AxisAlignedBB par0)
	{
		return instance.collidesWithAnyBlock(par0);
	}

	public TileEntity getTileEntity(BlockPos par0)
	{
		return instance.getTileEntity(par0);
	}

	public EntityPlayer getNearestAttackablePlayer(Entity par0, double par1, double par2)
	{
		return instance.getNearestAttackablePlayer(par0, par1, par2);
	}

	public int getStrongPower(BlockPos par0, EnumFacing par1)
	{
		return instance.getStrongPower(par0, par1);
	}

	public int getSkylightSubtracted()
	{
		return instance.getSkylightSubtracted();
	}

	public BlockPos getTopSolidOrLiquidBlock(BlockPos par0)
	{
		return instance.getTopSolidOrLiquidBlock(par0);
	}

	public boolean isSideSolid(BlockPos par0, EnumFacing par1)
	{
		return instance.isSideSolid(par0, par1);
	}

	public EnumDifficulty getDifficulty()
	{
		return instance.getDifficulty();
	}

	public void scheduleUpdate(BlockPos par0, Block par1, int par2)
	{
		instance.scheduleUpdate(par0, par1, par2);
	}

	public float getStarBrightness(float par0)
	{
		return instance.getStarBrightness(par0);
	}

	public Entity getEntityByID(int par0)
	{
		return instance.getEntityByID(par0);
	}

	public int getUniqueDataId(String par0)
	{
		return instance.getUniqueDataId(par0);
	}

	public int getHeight()
	{
		return instance.getHeight();
	}

	public IBlockState getBlockState(BlockPos par0)
	{
		return instance.getBlockState(par0);
	}

	public boolean isAreaLoaded(BlockPos par0, int par1)
	{
		return instance.isAreaLoaded(par0, par1);
	}

	public List<Entity> getEntitiesWithinAABBExcludingEntity(Entity par0, AxisAlignedBB par1)
	{
		return instance.getEntitiesWithinAABBExcludingEntity(par0, par1);
	}

	public RayTraceResult rayTraceBlocks(Vec3d par0, Vec3d par1, boolean par2)
	{
		return instance.rayTraceBlocks(par0, par1, par2);
	}

	public boolean canSeeSky(BlockPos par0)
	{
		return instance.canSeeSky(par0);
	}

	public void playEvent(EntityPlayer par0, int par1, BlockPos par2, int par3)
	{
		instance.playEvent(par0, par1, par2, par3);
	}

	public String getDebugLoadedEntities()
	{
		return instance.getDebugLoadedEntities();
	}

	public boolean isMaterialInBB(AxisAlignedBB par0, Material par1)
	{
		return instance.isMaterialInBB(par0, par1);
	}

	public Explosion newExplosion(Entity par0, double par1, double par2, double par3, float par4, boolean par5, boolean par6)
	{
		return instance.newExplosion(par0, par1, par2, par3, par4, par5, par6);
	}

	public boolean canBlockFreezeNoWater(BlockPos par0)
	{
		return instance.canBlockFreezeNoWater(par0);
	}

	public int getLightFromNeighbors(BlockPos par0)
	{
		return instance.getLightFromNeighbors(par0);
	}

	public void joinEntityInSurroundings(Entity par0)
	{
		instance.joinEntityInSurroundings(par0);
	}

	public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> par0, AxisAlignedBB par1)
	{
		return instance.getEntitiesWithinAABB(par0, par1);
	}

	public MapStorage getPerWorldStorage()
	{
		return instance.getPerWorldStorage();
	}

	public void addEventListener(IWorldEventListener par0)
	{
		instance.addEventListener(par0);
	}

	public void updateAllPlayersSleepingFlag()
	{
		instance.updateAllPlayersSleepingFlag();
	}

	public void setRainStrength(float par0)
	{
		instance.setRainStrength(par0);
	}

	public boolean extinguishFire(EntityPlayer par0, BlockPos par1, EnumFacing par2)
	{
		return instance.extinguishFire(par0, par1, par2);
	}

	public boolean isBlockPowered(BlockPos par0)
	{
		return instance.isBlockPowered(par0);
	}

	public <T extends Entity> T findNearestEntityWithinAABB(Class<? extends T> par0, AxisAlignedBB par1, T par2)
	{
		return instance.findNearestEntityWithinAABB(par0, par1, par2);
	}

	public EntityPlayer getPlayerEntityByName(String par0)
	{
		return instance.getPlayerEntityByName(par0);
	}

	public boolean canSnowAt(BlockPos par0, boolean par1)
	{
		return instance.canSnowAt(par0, par1);
	}

	public void addBlockEvent(BlockPos par0, Block par1, int par2, int par3)
	{
		instance.addBlockEvent(par0, par1, par2, par3);
	}

	public boolean destroyBlock(BlockPos par0, boolean par1)
	{
		return instance.destroyBlock(par0, par1);
	}

	public boolean isSpawnChunk(int par0, int par1)
	{
		return instance.isSpawnChunk(par0, par1);
	}

	public void calculateInitialWeatherBody()
	{
		instance.calculateInitialWeatherBody();
	}

	public boolean isChunkGeneratedAt(int par0, int par1)
	{
		return instance.isChunkGeneratedAt(par0, par1);
	}

	public IBlockState getGroundAboveSeaLevel(BlockPos par0)
	{
		return instance.getGroundAboveSeaLevel(par0);
	}

	public boolean isBlockModifiable(EntityPlayer par0, BlockPos par1)
	{
		return instance.isBlockModifiable(par0, par1);
	}

	public boolean mayPlace(Block par0, BlockPos par1, boolean par2, EnumFacing par3, Entity par4)
	{
		return instance.mayPlace(par0, par1, par2, par3, par4);
	}

	public boolean spawnEntity(Entity par0)
	{
		return instance.spawnEntity(par0);
	}

	public boolean handleMaterialAcceleration(AxisAlignedBB par0, Material par1, Entity par2)
	{
		return instance.handleMaterialAcceleration(par0, par1, par2);
	}

	public EntityPlayer getClosestPlayer(double par0, double par1, double par2, double par3, Predicate<Entity> par4)
	{
		return instance.getClosestPlayer(par0, par1, par2, par3, par4);
	}

	public Biome getBiomeForCoordsBody(BlockPos par0)
	{
		return instance.getBiomeForCoordsBody(par0);
	}

	public void markBlockRangeForRenderUpdate(BlockPos par0, BlockPos par1)
	{
		instance.markBlockRangeForRenderUpdate(par0, par1);
	}

	public Biome getBiome(BlockPos par0)
	{
		return instance.getBiome(par0);
	}

	@Override
	public boolean equals(Object par0)
	{
		return instance.equals(par0);
	}

	public int getLightFor(EnumSkyBlock par0, BlockPos par1)
	{
		return instance.getLightFor(par0, par1);
	}

	public <T extends Entity> List<T> getEntities(Class<? extends T> par0, Predicate<? super T> par1)
	{
		return instance.getEntities(par0, par1);
	}

	public <T> T getCapability(Capability<T> par0, EnumFacing par1)
	{
		return instance.getCapability(par0, par1);
	}

	public void notifyNeighborsRespectDebug(BlockPos par0, Block par1, boolean par2)
	{
		instance.notifyNeighborsRespectDebug(par0, par1, par2);
	}

	public EntityPlayer getNearestAttackablePlayer(BlockPos par0, double par1, double par2)
	{
		return instance.getNearestAttackablePlayer(par0, par1, par2);
	}

	public void markChunkDirty(BlockPos par0, TileEntity par1)
	{
		instance.markChunkDirty(par0, par1);
	}

	public int getActualHeight()
	{
		return instance.getActualHeight();
	}

	public void spawnParticle(EnumParticleTypes par0, double par1, double par2, double par3, double par4, double par5, double par6, int[] par7)
	{
		instance.spawnParticle(par0, par1, par2, par3, par4, par5, par6, par7);
	}

	public List<AxisAlignedBB> getCollisionBoxes(Entity par0, AxisAlignedBB par1)
	{
		return instance.getCollisionBoxes(par0, par1);
	}

	public WorldType getWorldType()
	{
		return instance.getWorldType();
	}

	public <T extends EntityPlayer> List<T> getPlayers(Class<? extends T> par0, Predicate<? super T> par1)
	{
		return instance.getPlayers(par0, par1);
	}

	public void neighborChanged(BlockPos par0, Block par1, BlockPos par2)
	{
		instance.neighborChanged(par0, par1, par2);
	}

	public Scoreboard getScoreboard()
	{
		return instance.getScoreboard();
	}

	public Explosion createExplosion(Entity par0, double par1, double par2, double par3, float par4, boolean par5)
	{
		return instance.createExplosion(par0, par1, par2, par3, par4, par5);
	}

	public BlockPos getHeight(BlockPos par0)
	{
		return instance.getHeight(par0);
	}

	public int countEntities(EnumCreatureType par0, boolean par1)
	{
		return instance.countEntities(par0, par1);
	}

	public void onEntityRemoved(Entity par0)
	{
		instance.onEntityRemoved(par0);
	}

	public boolean checkLight(BlockPos par0)
	{
		return instance.checkLight(par0);
	}

	public void unloadEntities(java.util.Collection<Entity> par0)
	{
		instance.unloadEntities(par0);
	}

	public boolean canBlockSeeSky(BlockPos par0)
	{
		return instance.canBlockSeeSky(par0);
	}

	public void notifyBlockUpdate(BlockPos par0, IBlockState par1, IBlockState par2, int par3)
	{
		instance.notifyBlockUpdate(par0, par1, par2, par3);
	}

	public World init()
	{
		return instance.init();
	}

	public float getStarBrightnessBody(float par0)
	{
		return instance.getStarBrightnessBody(par0);
	}

	public EntityPlayer getClosestPlayerToEntity(Entity par0, double par1)
	{
		return instance.getClosestPlayerToEntity(par0, par1);
	}

	public void markTileEntityForRemoval(TileEntity par0)
	{
		instance.markTileEntityForRemoval(par0);
	}

	public int getHeight(int par0, int par1)
	{
		return instance.getHeight(par0, par1);
	}

	public VillageCollection getVillageCollection()
	{
		return instance.getVillageCollection();
	}

	public net.minecraft.crash.CrashReportCategory addWorldInfoToCrashReport(net.minecraft.crash.CrashReport par0)
	{
		return instance.addWorldInfoToCrashReport(par0);
	}

	public boolean addTileEntity(TileEntity par0)
	{
		return instance.addTileEntity(par0);
	}

	public RayTraceResult rayTraceBlocks(Vec3d par0, Vec3d par1, boolean par2, boolean par3, boolean par4)
	{
		return instance.rayTraceBlocks(par0, par1, par2, par3, par4);
	}

	public int getMoonPhase()
	{
		return instance.getMoonPhase();
	}

	public void markBlocksDirtyVertical(int par0, int par1, int par2, int par3)
	{
		instance.markBlocksDirtyVertical(par0, par1, par2, par3);
	}

	public boolean setBlockState(BlockPos par0, IBlockState par1, int par2)
	{
		return instance.setBlockState(par0, par1, par2);
	}

	public boolean isAreaLoaded(StructureBoundingBox par0, boolean par1)
	{
		return instance.isAreaLoaded(par0, par1);
	}

	public float getBlockDensity(Vec3d par0, AxisAlignedBB par1)
	{
		return instance.getBlockDensity(par0, par1);
	}

	public ISaveHandler getSaveHandler()
	{
		return instance.getSaveHandler();
	}

	public Calendar getCurrentDate()
	{
		return instance.getCurrentDate();
	}

	public RayTraceResult rayTraceBlocks(Vec3d par0, Vec3d par1)
	{
		return instance.rayTraceBlocks(par0, par1);
	}

	public boolean setBlockToAir(BlockPos par0)
	{
		return instance.setBlockToAir(par0);
	}

	public WorldInfo getWorldInfo()
	{
		return instance.getWorldInfo();
	}

	public void setSkylightSubtracted(int par0)
	{
		instance.setSkylightSubtracted(par0);
	}

	public EntityPlayer getNearestPlayerNotCreative(Entity par0, double par1)
	{
		return instance.getNearestPlayerNotCreative(par0, par1);
	}

	public String toString()
	{
		return instance.toString();
	}

	public Vec3d getFogColor(float par0)
	{
		return instance.getFogColor(par0);
	}

	public void spawnParticle(EnumParticleTypes par0, boolean par1, double par2, double par3, double par4, double par5, double par6, double par7, int[] par8)
	{
		instance.spawnParticle(par0, par1, par2, par3, par4, par5, par6, par7, par8);
	}

	public void setInitialSpawnLocation()
	{
		instance.setInitialSpawnLocation();
	}

	public List<NextTickListEntry> getPendingBlockUpdates(StructureBoundingBox par0, boolean par1)
	{
		return instance.getPendingBlockUpdates(par0, par1);
	}

	public void setLightFor(EnumSkyBlock par0, BlockPos par1, int par2)
	{
		instance.setLightFor(par0, par1, par2);
	}

	public boolean isInsideWorldBorder(Entity par0)
	{
		return instance.isInsideWorldBorder(par0);
	}

	public net.minecraft.server.MinecraftServer getMinecraftServer()
	{
		return instance.getMinecraftServer();
	}

	public float getSunBrightnessFactor(float par0)
	{
		return instance.getSunBrightnessFactor(par0);
	}

	public ImmutableSetMultimap<ChunkPos, ForgeChunkManager.Ticket> getPersistentChunks()
	{
		return instance.getPersistentChunks();
	}

	public void setTotalWorldTime(long par0)
	{
		instance.setTotalWorldTime(par0);
	}

	public int hashCode()
	{
		return instance.hashCode();
	}

	public Vec3d getCloudColorBody(float par0)
	{
		return instance.getCloudColorBody(par0);
	}

	public void removeTileEntity(BlockPos par0)
	{
		instance.removeTileEntity(par0);
	}

	public Vec3d getCloudColour(float par0)
	{
		return instance.getCloudColour(par0);
	}

	public int getLastLightningBolt()
	{
		return instance.getLastLightningBolt();
	}

	public net.minecraft.world.storage.loot.LootTableManager getLootTableManager()
	{
		return instance.getLootTableManager();
	}

	public void scheduleBlockUpdate(BlockPos par0, Block par1, int par2, int par3)
	{
		instance.scheduleBlockUpdate(par0, par1, par2, par3);
	}

	public void playSound(EntityPlayer par0, double par1, double par2, double par3, SoundEvent par4, SoundCategory par5, float par6, float par7)
	{
		instance.playSound(par0, par1, par2, par3, par4, par5, par6, par7);
	}

	public boolean hasCapability(Capability<?> par0, EnumFacing par1)
	{
		return instance.hasCapability(par0, par1);
	}

	public void markTileEntitiesInChunkForRemoval(Chunk par0)
	{
		instance.markTileEntitiesInChunkForRemoval(par0);
	}

	public long getTotalWorldTime()
	{
		return instance.getTotalWorldTime();
	}

	public boolean addWeatherEffect(Entity par0)
	{
		return instance.addWeatherEffect(par0);
	}

	public void updateEntity(Entity par0)
	{
		instance.updateEntity(par0);
	}

	public Random setRandomSeed(int par0, int par1, int par2)
	{
		return instance.setRandomSeed(par0, par1, par2);
	}

	public boolean isFlammableWithin(AxisAlignedBB par0)
	{
		return instance.isFlammableWithin(par0);
	}

	public void playEvent(int par0, BlockPos par1, int par2)
	{
		instance.playEvent(par0, par1, par2);
	}

	public void markAndNotifyBlock(BlockPos par0, Chunk par1, IBlockState par2, IBlockState par3, int par4)
	{
		instance.markAndNotifyBlock(par0, par1, par2, par3, par4);
	}

	public int getLight(BlockPos par0)
	{
		return instance.getLight(par0);
	}

	public float getRainStrength(float par0)
	{
		return instance.getRainStrength(par0);
	}

	public float getCurrentMoonPhaseFactorBody()
	{
		return instance.getCurrentMoonPhaseFactorBody();
	}

	public boolean canBlockFreezeWater(BlockPos par0)
	{
		return instance.canBlockFreezeWater(par0);
	}

	public void playRecord(BlockPos par0, SoundEvent par1)
	{
		instance.playRecord(par0, par1);
	}

	public BlockPos getPrecipitationHeight(BlockPos par0)
	{
		return instance.getPrecipitationHeight(par0);
	}

	public void setSpawnPoint(BlockPos par0)
	{
		instance.setSpawnPoint(par0);
	}

	public void initialize(WorldSettings par0)
	{
		instance.initialize(par0);
	}

	public void setThunderStrength(float par0)
	{
		instance.setThunderStrength(par0);
	}

	public boolean isThundering()
	{
		return instance.isThundering();
	}

	public Iterator<Chunk> getPersistentChunkIterable(Iterator<Chunk> par0)
	{
		return instance.getPersistentChunkIterable(par0);
	}

	public boolean isBlockinHighHumidity(BlockPos par0)
	{
		return instance.isBlockinHighHumidity(par0);
	}

	public void makeFireworks(double par0, double par1, double par2, double par3, double par4, double par5, NBTTagCompound par6)
	{
		instance.makeFireworks(par0, par1, par2, par3, par4, par5, par6);
	}

	public void observedNeighborChanged(BlockPos par0, Block par1, BlockPos par2)
	{
		instance.observedNeighborChanged(par0, par1, par2);
	}

	public void spawnAlwaysVisibleParticle(int par0, double par1, double par2, double par3, double par4, double par5, double par6, int[] par7)
	{
		instance.spawnAlwaysVisibleParticle(par0, par1, par2, par3, par4, par5, par6, par7);
	}

	public boolean checkNoEntityCollision(AxisAlignedBB par0, Entity par1)
	{
		return instance.checkNoEntityCollision(par0, par1);
	}

	public boolean isValid(BlockPos par0)
	{
		return instance.isValid(par0);
	}

	public Chunk getChunk(int par0, int par1)
	{
		return instance.getChunk(par0, par1);
	}

	public boolean checkBlockCollision(AxisAlignedBB par0)
	{
		return instance.checkBlockCollision(par0);
	}

	public boolean isDaytime()
	{
		return instance.isDaytime();
	}

	public boolean checkLightFor(EnumSkyBlock par0, BlockPos par1)
	{
		return instance.checkLightFor(par0, par1);
	}

	public MapStorage getMapStorage()
	{
		return instance.getMapStorage();
	}

	public void setData(String par0, WorldSavedData par1)
	{
		instance.setData(par0, par1);
	}

	public int getSeaLevel()
	{
		return instance.getSeaLevel();
	}

	public void sendPacketToServer(Packet<?> par0)
	{
		instance.sendPacketToServer(par0);
	}

	public void playSound(EntityPlayer par0, BlockPos par1, SoundEvent par2, SoundCategory par3, float par4, float par5)
	{
		instance.playSound(par0, par1, par2, par3, par4, par5);
	}

	public List<NextTickListEntry> getPendingBlockUpdates(Chunk par0, boolean par1)
	{
		return instance.getPendingBlockUpdates(par0, par1);
	}

	public float getSunBrightness(float par0)
	{
		return instance.getSunBrightness(par0);
	}
}