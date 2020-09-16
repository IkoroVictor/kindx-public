package io.kindx.backoffice.processor;

import io.kindx.backoffice.processor.menu.BasicMenuTextProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MenuMenuTextProcessorTests {

    private BasicMenuTextProcessor textProcessor;
    private List<String>  stopWords;

    @Before
    public void init() throws IOException {
        stopWords = new BufferedReader(new InputStreamReader(
                        Objects.requireNonNull(
                                ClassLoader.getSystemResourceAsStream("en_stopwords.txt"))))
                .lines()
                .collect(Collectors.toList());
    }

    @Test
    public void shouldReturnFullScoresForMenuText1() {
        textProcessor = BasicMenuTextProcessor
                .builder()
                .text(menuText1)
                .stopWords(stopWords)
                .build();

        Assert.assertEquals(1.0f, textProcessor.score("Värskekapsasupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("varskekapsasupp"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("Kanašnitsel"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("kanasnitsel"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("Värskekapsa hautis"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Varskekapsa hautis"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("Fresh cabbage stew"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Fresh cabbage soup"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Cabbage stew"), 0);

        Assert.assertEquals(1.0f, textProcessor.score("pumpkin cream-soup"), 0);
    }


    @Test
    public void shouldReturnExpectedScoresForMenuText1() {
        textProcessor = BasicMenuTextProcessor
                .builder()
                .text(menuText1)
                .stopWords(stopWords)
                .filterOutStopWordsWhenScoring(true)
                .build();
        Assert.assertEquals(1.0f, textProcessor.score("turkey"), 0);
        Assert.assertEquals(0.0f, textProcessor.score("bread"), 0);
        Assert.assertEquals(0.5f, textProcessor.score("turk"), 0);
        Assert.assertEquals(0.5f, textProcessor.score("urkey"), 0);
        Assert.assertEquals(0.5f, textProcessor.score("mushroom beans"), 0.001);
        Assert.assertEquals(0.5f, textProcessor.score("tea mushroom"), 0.001);
        Assert.assertEquals(0.5f, textProcessor.score("beef schnitsel"), 0.001);
        Assert.assertEquals(0.5f, textProcessor.score("Spat Risoto"), 0.001);
        Assert.assertEquals(0.75f, textProcessor.score("Pumpkin cream stew"), 0.001);
        Assert.assertEquals(0.875f, textProcessor.score("pork meat with sweet sauce"), 0.001);
        Assert.assertEquals(0.875f, textProcessor.score("pork meat sweet sauce"), 0.001);
        Assert.assertEquals(1f, textProcessor.score("cream sauce"), 0.001);
        Assert.assertEquals(0.75f, textProcessor.score("ream sauce"), 0.001);
        Assert.assertEquals(0.375f, textProcessor.score("ream sauc"), 0.001);
        Assert.assertEquals(0.75f, textProcessor.score("minced sauce"), 0.001);
    }

    @Test
    public void shouldReturnFullScoresForMenuText2() {
        textProcessor = BasicMenuTextProcessor
                .builder()
                .text(menuText2)
                .build();
        Assert.assertEquals(1.0f, textProcessor.score("tomati maasikapureesupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("tomati-maasikapureesupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("Fried potatoes"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("salad"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("salat"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("suvine   salat   arbuusiga"), 0);

    }

    @Test
    public void shouldReturnExpectedScoresForMenuText2() {
        textProcessor = BasicMenuTextProcessor
                .builder()
                .text(menuText2)
                .build();

        Assert.assertEquals(1.0f, textProcessor.score("turkey"), 0);
        Assert.assertEquals(0.5f, textProcessor.score("turk"), 0);
        Assert.assertEquals(0.5f, textProcessor.score("urkey"), 0);
        Assert.assertEquals(0.625f, textProcessor.score("Külm feta"), 0.001);
        Assert.assertEquals(0.75f, textProcessor.score("Külm fetaga"), 0.001);
        Assert.assertEquals(1.0f, textProcessor.score("organic buckwheat"), 0.001);
        Assert.assertEquals(0.333f, textProcessor.score("organic milk bread"), 0.001);
        Assert.assertEquals(0.666f, textProcessor.score("organic milk sauce"), 0.001);
    }

    @Test
    public void shouldFilterOutStopWordsFromScoreText()  {
        textProcessor = BasicMenuTextProcessor
                .builder()
                .text(menuText1)
                .stopWords(stopWords)
                .filterOutStopWordsWhenScoring(true)
                .build();
        Assert.assertEquals(0.875f, textProcessor.score("pork meat with sweet sauce"), 0.001);
        Assert.assertEquals(0.875f, textProcessor.score("pork meat sweet sauce"), 0.001);
    }

    @Test
    public void shouldNotFilterOutStopWordsFromScoreText()  {
        textProcessor = BasicMenuTextProcessor
                .builder()
                .text(menuText1)
                .stopWords(stopWords)
                .build();
        Assert.assertEquals(0.70f, textProcessor.score("pork meat with sweet sauce"), 0.001);
        Assert.assertEquals(0.875f, textProcessor.score("pork meat sweet sauce"), 0.001);
    }

    @Test
    public void shouldSplitBySpecifiedLineDelimiterRegex() {
        textProcessor = BasicMenuTextProcessor
                .builder()
                .text(menuText3)
                .lineDelimiterRegex("\n|/")
                .build();
        Assert.assertEquals(1.0f, textProcessor.score("munanuudlisupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("kana"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("kana munanuudlisupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("lõhesupp"), 0);
        Assert.assertEquals(0.625f, textProcessor.score("lõhesupp peedisalatiga"), 0);
    }

    @Test
    public void shouldUseExactMatch() {
        textProcessor = BasicMenuTextProcessor
                .builder()
                .text(menuText3)
                .lineDelimiterRegex("\n|/")
                .build();
        Assert.assertEquals(1.0f, textProcessor.score("munanuudlisupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("kana"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("kana munanuudlisupp"), 0);
        Assert.assertEquals(1.0f, textProcessor.score("lõhesupp"), 0);
        Assert.assertEquals(0.625f, textProcessor.score("lõhesupp peedisalatiga"), 0);
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
            "Lisaks kõigele vahvale, mis me siin teeme, ei puudu ka täna meie juures maitsvad toidud, mis ootavad proovimist! :)\n" +
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
            "- Chanterelle salad with fried potatoes\n" +
            "- Turkey skewer with ratatouille\n" +
            "- Chanterelle and minced meat sauce with organic buckwheat";


    private String menuText3 = "Kolmapäev: Koorene lõhesupp 2€ / Seašnitsel ahjukartulite, sinepi-koorekastme ja peedisalatiga 3,9€ / Lõunapakkumised alates 12:00 " +
            "/ Esmaspäev:\n" +
            "Kana-munanuudlisupp 2€ /\n" +
            "Lasanje 3,9€ /\n" +
            "Lõunapakkumised alates 12:00";
}
