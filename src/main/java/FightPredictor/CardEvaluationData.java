package FightPredictor;

import FightPredictor.util.BaseGameConstants;
import FightPredictor.util.StatEvaluation;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.util.*;

public class CardEvaluationData {

    // Evaluation for skip
    private final StatEvaluation skip;

    // Evaluations for each card
    private final Map<AbstractCard, StatEvaluation> evals;

    // Maps a card to its score by act. diffs.get(card).get(actNum) => score.
    private final Map<AbstractCard, Map<Integer, Float>> diffs;

    // Acts that are supported. Add more acts as needed
    private final Set<Integer> supportedActs;

    private CardEvaluationData(int startingAct, int endingAct) {
        this(startingAct, endingAct, AbstractDungeon.player.masterDeck.group);
    }

    private CardEvaluationData(int startingAct, int endingAct, List<AbstractCard> deck) {
        this.supportedActs = new HashSet<>();
        for (int act = startingAct; act <= endingAct; act++) {
            this.supportedActs.add(act);
        }

        Set<String> enemies = getAllEnemies(startingAct, endingAct);
        this.skip = new StatEvaluation(deck, AbstractDungeon.player.relics, AbstractDungeon.player.maxHealth,
                AbstractDungeon.player.currentHealth, AbstractDungeon.ascensionLevel, false, enemies);

        this.evals = new HashMap<>();
        this.diffs = new HashMap<>();

    }

    /**
     * Create a full evaluation of the given cards by evaluating a deck with each
     * card added compared to skip, against enemies in startingAct to endingAct
     * inclusive. Additional acts can be added later if needed.
     * 
     * @param cardsToAdd  Cards to evaluate adding to the deck
     * @param startingAct Act to start evaluating enemies. Inclusive. Must be 1-4
     *                    inclusive
     * @param endingAct   Act to end evaluating enmies. Inclusive. Must be 1-4
     *                    inclusive and greater than or equal to startingAct
     * @return CardEvaluationData with predictions and scores completed evaluation
     */
    public static CardEvaluationData createByAdding(List<AbstractCard> cardsToAdd, int startingAct, int endingAct) {
        DeckManipulation dm = (deck, card) -> {
            deck.add(card);
        };
        return createByFunction(cardsToAdd, dm, startingAct, endingAct);
    }

    public static CardEvaluationData createByRemoving(List<AbstractCard> cardsToRemove, int startingAct,
            int endingAct) {
        DeckManipulation dm = (deck, card) -> {
            deck.remove(card);
        };
        Set<String> enemies = getAllEnemies(startingAct, endingAct);
        CardEvaluationData ced = new CardEvaluationData(startingAct, endingAct);
        for (AbstractCard c : cardsToRemove) {
            // Modify the deck
            List<AbstractCard> newDeck = new ArrayList<>(AbstractDungeon.player.masterDeck.group);
            dm.modifyDeck(newDeck, c);

            // Run stat evaluation
            StatEvaluation se = new StatEvaluation(newDeck, AbstractDungeon.player.relics,
                    AbstractDungeon.player.maxHealth, AbstractDungeon.player.currentHealth,
                    AbstractDungeon.ascensionLevel, false, enemies);
            ced.evals.put(c, se);

            // Calculate score for each act by comparing to skip
            Map<Integer, Float> diffsByAct = new HashMap<>();
            for (int act = startingAct; act <= endingAct; act++) {
                float diff = StatEvaluation.getWeightedAvg(se, ced.skip, act);
                diffsByAct.put(act, diff);
            }
            ced.diffs.put(c, diffsByAct);

            if (c.canUpgrade()) {
                List<AbstractCard> newDeckUpgraded = new ArrayList<>(AbstractDungeon.player.masterDeck.group);
                newDeckUpgraded.remove(c);
                AbstractCard upgraded = c.makeCopy();
                upgraded.upgrade();
                newDeckUpgraded.add(upgraded);
                CardEvaluationData cedUpgraded = new CardEvaluationData(startingAct, endingAct, newDeckUpgraded);
                List<AbstractCard> newDeckUpgradedRemoved = new ArrayList<>(newDeckUpgraded);
                dm.modifyDeck(newDeckUpgradedRemoved, upgraded);

                // Run stat evaluation
                StatEvaluation seUpgraded = new StatEvaluation(newDeckUpgradedRemoved, AbstractDungeon.player.relics,
                        AbstractDungeon.player.maxHealth, AbstractDungeon.player.currentHealth,
                        AbstractDungeon.ascensionLevel, false, enemies);
                ced.evals.put(upgraded, seUpgraded);

                // Calculate score for each act by comparing to skip
                Map<Integer, Float> diffsByActUpgraded = new HashMap<>();
                for (int act = startingAct; act <= endingAct; act++) {
                    float diff = StatEvaluation.getWeightedAvg(seUpgraded, cedUpgraded.skip, act);
                    diffsByActUpgraded.put(act, diff);
                }
                ced.diffs.put(upgraded, diffsByActUpgraded);
            }
        }
        return ced;
    }

    public static CardEvaluationData createByUpgrading(List<AbstractCard> cardsToUpgrade, int startingAct,
            int endingAct) {
        DeckManipulation dm = (deck, card) -> {
            deck.remove(card);
            AbstractCard upgradedCard = card.makeCopy();
            upgradedCard.upgrade();
            deck.add(upgradedCard);
        };
        return createByFunction(cardsToUpgrade, dm, startingAct, endingAct);
    }

    private interface DeckManipulation {
        void modifyDeck(List<AbstractCard> originalDeck, AbstractCard c);
    }

    private static CardEvaluationData createByFunction(List<AbstractCard> cardsToChange, DeckManipulation dm,
            int startingAct, int endingAct) {
        CardEvaluationData ced = new CardEvaluationData(startingAct, endingAct);
        Set<String> enemies = getAllEnemies(startingAct, endingAct);

        for (AbstractCard c : cardsToChange) {
            // Modify the deck
            List<AbstractCard> newDeck = new ArrayList<>(AbstractDungeon.player.masterDeck.group);
            dm.modifyDeck(newDeck, c);

            // Run stat evaluation
            StatEvaluation se = new StatEvaluation(newDeck, AbstractDungeon.player.relics,
                    AbstractDungeon.player.maxHealth, AbstractDungeon.player.currentHealth,
                    AbstractDungeon.ascensionLevel, false, enemies);
            ced.evals.put(c, se);

            // Calculate score for each act by comparing to skip
            Map<Integer, Float> diffsByAct = new HashMap<>();
            for (int act = startingAct; act <= endingAct; act++) {
                float diff = StatEvaluation.getWeightedAvg(se, ced.skip, act);
                diffsByAct.put(act, diff);
            }
            ced.diffs.put(c, diffsByAct);
        }
        return ced;
    }

    public void addAdditionalActs(Set<Integer> actsToAdd) {

    }

    private static Set<String> getAllEnemies(int startingAct, int endingAct) {
        Set<String> enemies = new HashSet<>();
        for (int act = startingAct; act <= endingAct; act++) {
            enemies.addAll(BaseGameConstants.allFightsByAct.get(act));
        }
        return enemies;
    }

    public StatEvaluation getSkip() {
        return skip;
    }

    public Map<AbstractCard, StatEvaluation> getEvals() {
        return evals;
    }

    public Map<AbstractCard, Map<Integer, Float>> getDiffs() {
        return diffs;
    }

    public Set<Integer> getSupportedActs() {
        return supportedActs;
    }
}
