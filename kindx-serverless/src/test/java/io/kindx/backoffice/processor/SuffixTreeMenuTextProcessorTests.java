package io.kindx.backoffice.processor;

import io.kindx.backoffice.processor.menu.MenuTextProcessor;
import io.kindx.backoffice.processor.menu.SuffixTreeMenuTextProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SuffixTreeMenuTextProcessorTests {

    private SuffixTreeMenuTextProcessor textProcessor;
    private List<String> stopWords;

    @Before
    public void init() throws IOException {
        stopWords = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        ClassLoader.getSystemResourceAsStream("en_stopwords.txt"))))
                .lines()
                .collect(Collectors.toList());
    }

    @Test
    public void testProcessor() {
        textProcessor = SuffixTreeMenuTextProcessor
                .builder()
                .text("banana test cream banana egg maize akwa cream")
                .stopWords(Collections.emptyList())
                .build();
        textProcessor.score("test");
    }

    @Test
    public void testProcessor2() {
        textProcessor = SuffixTreeMenuTextProcessor
                .builder()
                .text("banana test cream banana egg egg maize akwa cream")
                .stopWords(Collections.emptyList())
                .build();
        textProcessor.score("test");
    }

    @Test
    public void shouldReturnFullScoresForSingleWord() {
        textProcessor = SuffixTreeMenuTextProcessor
                .builder()
                .text(menuText1)
                .stopWords(stopWords)
                .build();

        Assert.assertEquals(1.0f, textProcessor.score("Värskekapsasupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("varskekapsasupp"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("Kanašnitsel"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("kanasnitsel"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("Ratatouille"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Kalkuniliha"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("cabbage"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("schnitsel"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("magushapus"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("pumpkin"), 0);
    }

    @Test
    public void shouldReturnPartialScoreForSingleWordsMatchedAsSuffix() {
        textProcessor = SuffixTreeMenuTextProcessor
                .builder()
                .text(menuText2)
                .stopWords(stopWords)
                .build();

        Assert.assertEquals(0.75f, textProcessor.score("supp"), 0);
        Assert.assertEquals(0.75f, textProcessor.score("liha"), 0);
        Assert.assertEquals(0.75f, textProcessor.score("siga"), 0);
    }

    @Test
    public void shouldReturnFullScoresForMultiWord() {
        textProcessor = SuffixTreeMenuTextProcessor
                .builder()
                .text(menuText1)
                .stopWords(stopWords)
                .build();

        Assert.assertEquals(1.0f, textProcessor.score("cabbage soup"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Fresh cabbage soup"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("kalkuniliha koorekastmes"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("pork meat"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Seene risoto"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("cream soup"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Kõrvitsa kreemsupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Sealiha magushapus kastmes"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("cabbage stew"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Värskekapsa hautis"), 0);
    }

    @Test
    public void shouldReturnPartialScoreForSingleWordsMatchedViaDistance() {
        //Not implemented yet
        Assert.assertTrue(true);

    }

    @Test
    public void shouldReturnPartialScoreForMultiWordsMatchedViaDistance() {
        textProcessor = SuffixTreeMenuTextProcessor
                .builder()
                .text(menuText1)
                .stopWords(stopWords)
                .build();

        List<MenuTextProcessor.ScoreResult> results = textProcessor.scores("meat soup");
        Assert.assertEquals(0.66, results.get(0).getScore(), 0.01);
        Assert.assertEquals("Minced meat sauce", results.get(0).getLine());


        results = textProcessor.scores("kõrvitsa supp");
        Assert.assertEquals(0.61, results.get(0).getScore(), 0.01);
        Assert.assertEquals("Kõrvitsa kreemsupp 1.90€", results.get(0).getLine());
    }

    private String menuText1 =  "<Copied>\n" +
            "\n" +
            "Kallis Sõber,\n" +
            "kutsume Sind 16.08 lõunale:\n" +
            "\n" +
            "Värskekapsasupp 2.20€\n" +
            "Kõrvitsa kreemsupp 1.90€\n" +
            "-----------------------------\n" +
            "Praetud heik\n" +
            "Kalkuniliha koorekastmes\n" +
            "Sealiha magushapus kastmes\n" +
            "Hakklihakaste\n" +
            "Kanašnitsel\n" +
            "Seene risoto\n" +
            "Ratatouille\n" +
            "Värskekapsa hautis\n" +
            "\n" +
            "Lisandid: Kartul, Riis, Tatar, Pasta\n" +
            "Salativalik\n" +
            "\n" +
            "100 g = 1,28 EUR\n" +
            "\n" +
            "Magustoit 2.00 €\n" +
            "Kook 2.20 €\n" +
            "\n" +
            "Allergeenide ja toidukoostise kohta küsi teenindajalt!\n" +
            "\n" +
            "Dear Friend,\n" +
            "we are welcoming You for lunch:\n" +
            "\n" +
            "Fresh cabbage soup 2.20€\n" +
            "Pumpkin cream soup 1.90€\n" +
            "-----------------------------\n" +
            "Fried hake\n" +
            "Turkey meat in cream sauce\n" +
            "Pork meat in sweet and sour sauce\n" +
            "Minced meat sauce\n" +
            "Chicken schnitsel\n" +
            "Mushroom risoto\n" +
            "Ratatouille\n" +
            "Fresh cabbage stew\n" +
            "\n" +
            "Additives: Potatoes, Rice, Buckwheat, Pasta\n" +
            "Salad selection\n" +
            "\n" +
            "100 g = 1,28 EUR\n" +
            "\n" +
            "Dessert 2.00 €\n" +
            "Cake 2.20 €\n" +
            "\n" +
            "Ask from waiter about allergens and food ingredients!";


    private String menuText2 = "Vahel juhtub ka nii! :)\n" +
            "Lisaks kõigele vahvale, mis me siin teeme, ei  puudu ka täna meie juures maitsvad toidud, mis ootavad proovimist! :)\n" +
            "\n" +
            "Tänane menüü:\n" +
            "- Külm tomati-maasikapüreesupp fetaga\n" +
            "- Suvine salat arbuusiga (L,V)\n" +
            "- Kukeseenesalat praekartuliga\n" +
            "- Kalkuni varras ratatouillega\n" +
            "- Kukeseene-hakklihakaste mahetatraga\n" +
            ":)\n" +
            "\n" +
            "Today's menu:\n" +
            "- Cold tomato-strawberry puree soup with feta\n" +
            "- Green salad with watermelon (L,V)\n" +
            "- Chanterelle salad with fried potatoes \n" +
            "- Turkey skewer with ratatouille\n" +
            "- Chanterelle and minced meat sauce with organic buckwheat";


    private String menuText3 = "Kolmapäev: Koorene lõhesupp 2€ / Seašnitsel ahjukartulite, sinepi-koorekastme ja peedisalatiga 3,9€ / Lõunapakkumised alates 12:00 " +
            "/ Esmaspäev:\n" +
            "Kana-munanuudlisupp 2€ /\n" +
            "Lasanje 3,9€ /\n" +
            "Lõunapakkumised alates 12:00";
}
