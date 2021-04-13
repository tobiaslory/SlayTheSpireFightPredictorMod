package FightPredictor.patches.com.megacrit.cardcrawl.screens.ShopScreen;

import FightPredictor.FightPredictor;
import FightPredictor.util.HelperMethods;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.shop.ShopScreen;
import FightPredictor.CardEvaluationData;

import java.util.ArrayList;
import java.util.List;

public class ShopScreenPatches {

    @SpirePatch(clz = ShopScreen.class, method = "init")
    public static class InitCardHook {
        @SpirePostfixPatch
        public static void patch(ShopScreen __instance, ArrayList<AbstractCard> coloredCards, ArrayList<AbstractCard> colorlessCards) {
            List<AbstractCard> allCards = new ArrayList<>();
            allCards.addAll(coloredCards);
            allCards.addAll(colorlessCards);
            List<AbstractCard> upgrades = new ArrayList<>(); // To avoid infinte looping with Searing Blow
            for (AbstractCard c : allCards) { // Call up a copy of Upgraded version of card for extra statistics
                if(c.canUpgrade()) {
                    AbstractCard copy = c.makeCopy();
                    copy.upgrade();
                    upgrades.add(copy);
                }
            }
            allCards.addAll(upgrades);
            FightPredictor.cardChoicesEvaluations = CardEvaluationData.createByAdding(allCards, AbstractDungeon.actNum, Math.min(AbstractDungeon.actNum + 1, 4));
        }
    }

    @SpirePatch(clz = ShopScreen.class, method = "renderCardsAndPrices")
    public static class RenderShopCardEvaluations {
        @SpirePostfixPatch
        public static void patch(ShopScreen __instance, SpriteBatch sb) {
            for(AbstractCard c : __instance.coloredCards) {
                renderGridSelectPrediction(sb, c);
            }
            for(AbstractCard c : __instance.colorlessCards) {
                renderGridSelectPrediction(sb, c);
            }
        }

        private static void renderGridSelectPrediction(SpriteBatch sb, AbstractCard c) {
            String s = HelperMethods.getPredictionString(c, FightPredictor.cardChoicesEvaluations, false);
            sb.setColor(Color.WHITE);
            FontHelper.renderSmartText(sb,
                    FontHelper.cardDescFont_N,
                    s,
                    c.hb.cX - FontHelper.getSmartWidth(FontHelper.cardDescFont_N, s, Float.MAX_VALUE, FontHelper.cardDescFont_N.getSpaceWidth()) * 0.5f,
                    c.hb.y + (12f * Settings.scale),
                    Color.WHITE);
        }
    }
}
