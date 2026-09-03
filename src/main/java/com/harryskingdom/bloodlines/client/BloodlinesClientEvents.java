package com.harryskingdom.bloodlines.client;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.harryskingdom.bloodlines.network.BloodlinesNetwork;
import com.harryskingdom.bloodlines.network.TogglePassiveVisualsPacket;
import com.harryskingdom.bloodlines.network.UseRaceAbilityPacket;
import com.harryskingdom.bloodlines.race.ClientRaceCache;
import com.harryskingdom.bloodlines.race.Race;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fae's wing pose/animation is driven entirely by FaeWingsLayer per-frame. */
@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BloodlinesClientEvents
{
    /** Brings Shadowkin's sneak speed from vanilla's default ~30% up toward ~75% of normal walking speed. */
    private static final float SNEAK_SPEED_MULTIPLIER = 2.5f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        while (BloodlinesKeyMappings.USE_PRIMARY_ABILITY.consumeClick())
            BloodlinesNetwork.CHANNEL.sendToServer(new UseRaceAbilityPacket(false));

        while (BloodlinesKeyMappings.USE_SECONDARY_ABILITY.consumeClick())
            BloodlinesNetwork.CHANNEL.sendToServer(new UseRaceAbilityPacket(true));

        while (BloodlinesKeyMappings.TOGGLE_PASSIVE_VISUALS.consumeClick())
            BloodlinesNetwork.CHANNEL.sendToServer(new TogglePassiveVisualsPacket());
    }

    /**
     * Hides Merfolk's legs while their fish tail is showing (TailLayer handles showing the tail itself). This
     * has to happen here, in a Pre event, rather than inside TailLayer - RenderLayers only run AFTER the base
     * PlayerModel has already drawn for the frame, so a mutation made there always arrives one frame late and,
     * since something re-marks the parts visible before the next base draw, never actually took hold. Pre fires
     * before that base draw, so setting .visible here actually reaches it - same timing real MerMod gets via a
     * Mixin into PlayerModel, achieved here through Forge's own event instead of bytecode weaving.
     */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event)
    {
        Player player = event.getEntity();
        boolean showTail = ClientRaceCache.get(player.getId()) == Race.MERFOLK && player.isInWater();

        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        model.rightLeg.visible = !showTail;
        model.leftLeg.visible = !showTail;
        model.rightPants.visible = !showTail;
        model.leftPants.visible = !showTail;
    }

    /**
     * Demonkin see clearly in lava instead of vanilla's thick red fog - pushes the fog plane distances way out
     * whenever the camera's own fog type is LAVA, the same mechanism vanilla's own Night Vision uses to push
     * back water's fog (confirmed real, non-Mixin Forge API: ViewportEvent.RenderFog fires from inside vanilla's
     * FogRenderer with the current FogType already resolved). 64 blocks is an arbitrary but generous distance -
     * lava pools are rarely that large, so in practice this reads as "no fog at all" without literally disabling
     * fog rendering outright.
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event)
    {
        if (event.getType() != FogType.LAVA)
            return;

        Entity entity = event.getCamera().getEntity();
        if (!(entity instanceof Player player) || ClientRaceCache.get(player.getId()) != Race.DEMON)
            return;

        event.setNearPlaneDistance(0f);
        event.setFarPlaneDistance(64f);
    }

    /**
     * Shadowkin's "Shadow Step" - sneaking isn't the usual crawl for them. Multiplies the already-sneak-scaled
     * input impulse back up: confirmed via decompile that LocalPlayer#aiStep() runs Input#tick() - applying
     * vanilla's own 0.3 base sneaking factor, Swift Sneak's enchantment bonus included - before this event
     * fires, so MovementInputUpdateEvent's Input holds the post-sneak-scaled value here, not the raw ±1 key
     * input. Clamped to ±1 so the result can never exceed a full, non-sneaking impulse even stacked with Swift
     * Sneak - this only closes the gap toward normal walking speed, never grants more than walking already
     * allows, so there's nothing here for the server's own movement-speed validation to balk at.
     */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event)
    {
        Player player = event.getEntity();
        if (!player.isCrouching() || ClientRaceCache.get(player.getId()) != Race.GOBLIN)
            return;

        Input input = event.getInput();
        input.leftImpulse = Mth.clamp(input.leftImpulse * SNEAK_SPEED_MULTIPLIER, -1.0f, 1.0f);
        input.forwardImpulse = Mth.clamp(input.forwardImpulse * SNEAK_SPEED_MULTIPLIER, -1.0f, 1.0f);
    }
}
