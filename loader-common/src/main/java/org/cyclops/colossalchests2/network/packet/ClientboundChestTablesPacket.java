package org.cyclops.colossalchests2.network.packet;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.cyclops.colossalchests2.Reference;
import org.cyclops.colossalchests2.api.MaterialProperties;
import org.cyclops.colossalchests2.api.UpgradeProperties;
import org.cyclops.colossalchests2.config.ChestTables;
import org.cyclops.colossalchests2.config.ChestTablesLoader;
import org.cyclops.colossalchests2.storage.CompressionDiscovery;
import org.cyclops.colossalchests2.storage.CompressionFamiliesCache;
import org.cyclops.cyclopscore.network.PacketBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The server's material and upgrade tables, so clients show the same limits and material order,
 * and its compression conversions, as clients do not receive recipes.
 * @author rubensworks
 */
public class ClientboundChestTablesPacket extends PacketBase<ClientboundChestTablesPacket> {

    public static final Type<ClientboundChestTablesPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "chest_tables"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundChestTablesPacket> CODEC = getCodec(ClientboundChestTablesPacket::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, MaterialProperties>> MATERIALS = ByteBufCodecs.map(
            HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.fromCodecWithRegistries(MaterialProperties.CODEC));
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, UpgradeProperties>> UPGRADES = ByteBufCodecs.map(
            HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.fromCodecWithRegistries(UpgradeProperties.CODEC));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<CompressionDiscovery.Conversion>> CONVERSIONS = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), CompressionDiscovery.Conversion::smaller,
            ByteBufCodecs.registry(Registries.ITEM), CompressionDiscovery.Conversion::larger,
            ByteBufCodecs.VAR_INT, CompressionDiscovery.Conversion::ratio,
            CompressionDiscovery.Conversion::new).apply(ByteBufCodecs.list());

    private ChestTables tables;
    private List<CompressionDiscovery.Conversion> conversions;

    public ClientboundChestTablesPacket() {
        super(TYPE);
    }

    public ClientboundChestTablesPacket(ChestTables tables, List<CompressionDiscovery.Conversion> conversions) {
        super(TYPE);
        this.tables = tables;
        this.conversions = conversions;
    }

    public ChestTables getTables() {
        return tables;
    }

    public List<CompressionDiscovery.Conversion> getConversions() {
        return conversions;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        MATERIALS.encode(buf, tables.materials());
        UPGRADES.encode(buf, tables.upgrades());
        CONVERSIONS.encode(buf, conversions);
    }

    @Override
    public void decode(RegistryFriendlyByteBuf buf) {
        tables = new ChestTables(MATERIALS.decode(buf), UPGRADES.decode(buf));
        conversions = CONVERSIONS.decode(buf);
    }

    @Override
    public void actionClient(Level level, Player player) {
        ChestTablesLoader.set(tables);
        CompressionFamiliesCache.setClientConversions(conversions);
    }

    @Override
    public void actionServer(Level level, ServerPlayer player) {
    }

}
