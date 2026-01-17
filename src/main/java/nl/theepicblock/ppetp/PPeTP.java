package nl.theepicblock.ppetp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PPeTP implements ModInitializer {
	public static final String MOD_ID = "proper-pet-tp";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final GameRule<Boolean> SHOULD_TP_CROSS_DIMENSIONAL = GameRuleBuilder
            .forBoolean(false)
            .category(GameRuleCategory.MOBS)
            .buildAndRegister(Identifier.of(MOD_ID, "petTeleportCrossDimension"));

	@Override
	public void onInitialize() {
	}
}