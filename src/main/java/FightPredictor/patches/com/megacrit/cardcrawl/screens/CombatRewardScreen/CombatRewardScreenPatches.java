package FightPredictor.patches.com.megacrit.cardcrawl.screens.CombatRewardScreen;

import FightPredictor.FightPredictor;
import FightPredictor.CardEvaluationData;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.screens.CombatRewardScreen;

import java.util.*;
import java.util.stream.Collectors;


public class CombatRewardScreenPatches {

    @SpirePatch(clz = CombatRewardScreen.class, method = "setupItemReward")
    public static class EvaluateCardRewards {
        public static void Postfix(CombatRewardScreen __instance) {
            List<AbstractCard> cards = __instance.rewards.stream()
                    .filter(r -> r.type == RewardItem.RewardType.CARD)
                    .flatMap(r -> r.cards.stream()) // Flatten out all card rewards for prayer wheel
                    .collect(Collectors.toList());
            List<AbstractCard> upgrades = new ArrayList<>(); // To avoid infinte looping with Searing Blow
            for (AbstractCard c : cards) { // Call up a copy of Upgraded version of card for extra statistics
                if(c.canUpgrade()) {
                    AbstractCard copy = c.makeCopy();
                    copy.upgrade();
                    upgrades.add(copy);
                }
            }
            cards.addAll(upgrades);
            FightPredictor.cardChoicesEvaluations = CardEvaluationData.createByAdding(cards, AbstractDungeon.actNum, Math.min(AbstractDungeon.actNum + 1, 4));

        }
    }
}
