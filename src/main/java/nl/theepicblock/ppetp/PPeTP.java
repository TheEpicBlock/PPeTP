package nl.theepicblock.ppetp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PPeTP implements ModInitializer {
	public static final String MOD_ID = "proper-pet-tp";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final GameRules.Key<GameRules.BooleanRule> SHOULD_TP_CROSS_DIMENSIONAL =
			GameRuleRegistry.register("petTeleportCrossDimension", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(false));

	@Override
	public void onInitialize() {
	}
}