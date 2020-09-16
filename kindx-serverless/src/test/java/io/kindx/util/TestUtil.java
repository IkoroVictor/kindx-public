package io.kindx.util;

import com.google.maps.model.AddressComponent;
import com.google.maps.model.Geometry;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import io.kindx.client.FacebookClient;
import io.kindx.client.PlacesApiClient;
import io.kindx.dto.facebook.FacebookLocationDataDto;
import io.kindx.dto.facebook.FacebookPageDto;
import org.apache.commons.lang3.StringUtils;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.openqa.selenium.WebDriver;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtil {

    public static EnvironmentVariables loadTestEnvVariables(String envFileName) {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        List<String[]> pairs = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        ClassLoader.getSystemResourceAsStream(envFileName))))
                .lines()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .filter(s -> s.contains("="))
                .map(s -> s.split("="))
                .collect(toList());

        pairs.forEach(pair -> {
            if (StringUtils.isBlank(System.getenv(pair[0]))) {
                environmentVariables.set(pair[0], pair[1]);
            }
        });
        return environmentVariables;
    }

    public static String buildTestId(String baseId) {
        return String.format("%s_%s", baseId, EnvUtil.getEnvOrDefault("ENVIRONMENT", "TEST"));
    }

    public static WebDriver mockWebDriver() {
        WebDriver driver = mock(WebDriver.class);
        when(driver.getPageSource()).thenReturn("<html><head>\n" +
                "  <meta http-equiv=\"Content-Security-Policy\" content=\"default-src chrome:; img-src data: *; media-src *; object-src 'none'\">\n" +
                "  <meta content=\"text/html; charset=UTF-8\" http-equiv=\"content-type\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width; user-scalable=0\">\n" +
                "  <link rel=\"stylesheet\" href=\"chrome://global/skin/aboutReader.css\" type=\"text/css\">\n" +
                "  <script src=\"chrome://global/content/reader/aboutReader.js\"></script>\n" +
                "<link rel=\"stylesheet\" href=\"chrome://global/skin/narrate.css\"><title>Itaalia restoran Gianni</title></head>\n" +
                "\n" +
                "<body class=\"light sans-serif loaded\">\n" +
                "  <div class=\"container content-width3\" style=\"--font-size:20px;\">\n" +
                "    <div class=\"header reader-header reader-show-element\">\n" +
                "      <a class=\"domain reader-domain\" href=\"http://www.gianni.ee/restoran/\">gianni.ee</a>\n" +
                "      <div class=\"domain-border\"></div>\n" +
                "      <h1 class=\"reader-title\">Itaalia restoran Gianni</h1>\n" +
                "      <div class=\"credits reader-credits\"></div>\n" +
                "      <div class=\"meta-data\">\n" +
                "        <div class=\"reader-estimated-time\">5-6 minutes</div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <hr>\n" +
                "\n" +
                "    <div class=\"content\">\n" +
                "      <div class=\"moz-reader-content line-height4 reader-show-element\"><div id=\"readability-page-1\" class=\"page\"><div>\n" +
                "\t\t<div>\n" +
                "\t\t\t<div>\n" +
                "\t\t\t\t<div>\n" +
                "\t\t\t\t\t<div>\n" +
                "\t\t\t\t\t\t\n" +
                "<div>\n" +
                "\t<div><div data-vce-full-width=\"true\" id=\"el-ce3d28be\" data-vce-do-apply=\"all el-ce3d28be\"><div data-vce-element-content=\"true\"><div id=\"el-f1c91a70\" data-vce-do-apply=\"background border el-f1c91a70\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-f1c91a70\"><div><div><p><span>\n" +
                "<span>\n" +
                "\t&nbsp;<span> reede</span><span> 3:56</span> — <span>Restoran on suletud</span></span>\n" +
                "</span></p></div></div></div></div></div></div></div><div><div id=\"el-a12fbc8a\" data-vce-do-apply=\"all el-a12fbc8a\"><div data-vce-element-content=\"true\"><div id=\"el-5d6c8c43\" data-vce-do-apply=\"background border el-5d6c8c43\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-5d6c8c43\"><div><div id=\"el-9b0d0f45\" data-vce-do-apply=\"all el-9b0d0f45\"><p><span>13€</span></p><h5>VITELLO TONNATO</h5>\n" +
                "<p>Külmad vasikafileeviilud kapparite ja tuunikala kreemkastmega</p></div></div><div><div id=\"el-e206815d\" data-vce-do-apply=\"all el-e206815d\"><p><span>14€</span></p><h5>CARPACCIO DI TONNO</h5>\n" +
                "<p>Tuunikalacarpaccio šalottsibula, sidruni ja oliiviõliga</p></div></div><div><div id=\"el-070fc944\" data-vce-do-apply=\"all el-070fc944\"><p><span>14€</span></p><h5>CARPACCIO DI SALMONE</h5>\n" +
                "<p>Lõhecarpaccio lõhemarjaga</p></div></div><div><div id=\"el-83a1f3f5\" data-vce-do-apply=\"all el-83a1f3f5\"><p><span>16€</span></p><h5>CARPACCIO DI ORATA</h5><p>Kuldmerikogre carpaccio lõhemarjaga</p></div></div></div></div><div id=\"el-d57716f0\" data-vce-do-apply=\"background border el-d57716f0\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-d57716f0\"><div><div id=\"el-cb48dde0\" data-vce-do-apply=\"all el-cb48dde0\"><p><span>13€</span></p><h5>CARPACCIO DI MANZO CON TARTUFO NERO</h5>\n" +
                "<p>Loomaliha carpaccio parmesani,mustade trühvlite ja oliiviõliga</p></div></div><div><div id=\"el-5e535dcf\" data-vce-do-apply=\"all el-5e535dcf\"><p><span>12€</span></p><h5>COZZE ALLA NAPOLETANA CON POMODORO E AGLIO</h5>\n" +
                "<p>Merekarbid tomati ja küüslauguga</p></div></div><div><div id=\"el-e7a03736\" data-vce-do-apply=\"all el-e7a03736\"><p><span>12€</span></p><h5>MOZZARELLA DI BUFALA ALLA CAPRESE</h5>\n" +
                "<p>Pühvlimozzarella tomati ja basiilikuga</p></div></div><div><div id=\"el-1c31d4dd\" data-vce-do-apply=\"all el-1c31d4dd\"><p><span>12€</span></p><h5>PROSCIUTTO CRUDO CON MELONE</h5>\n" +
                "<p>Parma sink meloniga</p></div></div></div></div><div id=\"el-65fe952e\" data-vce-do-apply=\"background border el-65fe952e\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-65fe952e\"><div><div id=\"el-17bcb416\" data-vce-do-apply=\"all el-17bcb416\"><p><span>28€</span></p>\n" +
                "<h5>FEGATO GRASSO D´OCA</h5>\n" +
                "<p>Hanemaks kaneelipirni ja portveini kastmega</p></div></div><div><div id=\"el-484e2ebf\" data-vce-do-apply=\"all el-484e2ebf\"><p><span>15€</span></p><h5>INSALATA DI RUCOLA CON SCAMPI, POMODORI E AVOCADO</h5>\n" +
                "<p>Rukolasalat tiigerkrevettide, tomati ja avokaadoga</p></div></div><div><div id=\"el-85dd2927\" data-vce-do-apply=\"all el-85dd2927\"><p><span>16€</span></p><h5>FRUTTI DI MARE TIEPIDI CON VINAIGRETTE DI AGRUMI</h5>\n" +
                "<p>Soe salat mereandidest tsitrusviljade kastmega</p></div></div><div><div id=\"el-bd6985d4\" data-vce-do-apply=\"all el-bd6985d4\"><p><span>8€</span></p><h5>INSALATA MISTA</h5>\n" +
                "<p>Värske salat – lehtsalati, tomati, kurgi, porgandi, palsamaadika ja oliivõliga</p></div></div></div></div></div></div></div><div><div id=\"el-5ad31ac8\" data-vce-do-apply=\"all el-5ad31ac8\"><div data-vce-element-content=\"true\"><div id=\"el-30475c33\" data-vce-do-apply=\"background border el-30475c33\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-30475c33\"><div><div id=\"el-73f7e7e5\" data-vce-do-apply=\"all el-73f7e7e5\"><p><span>8€</span></p><h5>TORTELLINI IN BRODO DI POLLO</h5>\n" +
                "<p>Tortellinid kanapuljongis</p></div></div><div><div id=\"el-3ea7014e\" data-vce-do-apply=\"all el-3ea7014e\"><p><span>7€</span></p><h5>MINESTRONE FRESCO</h5><p>Juurviljasupp</p></div></div></div></div><div id=\"el-0b3a0cb3\" data-vce-do-apply=\"background border el-0b3a0cb3\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-0b3a0cb3\"><div><div id=\"el-a1ba2066\" data-vce-do-apply=\"all el-a1ba2066\"><p><span>7€</span></p><h5>CREMA DI BROCCOLI</h5><p>Brokkoli supp mandlilaastudega</p></div></div><div><div id=\"el-00123827\" data-vce-do-apply=\"all el-00123827\"><p><span>7€</span></p><h5>STRACCIATELLA ALLA ROMANA</h5><p>Kanapuljong muna ja parmesaniga</p></div></div></div></div><div id=\"el-12b6eff2\" data-vce-do-apply=\"background border el-12b6eff2\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-12b6eff2\"><div><div id=\"el-4a96c6ae\" data-vce-do-apply=\"all el-4a96c6ae\"><p><span>7€</span></p><h5>VELLUTA DI POMODORO</h5><p>Kreemsupp värsketest tomatitest</p></div></div><div><div id=\"el-a742625e\" data-vce-do-apply=\"all el-a742625e\"><p><span>17€</span></p><h5>ZUPPA DI PESCE</h5><p>Kalasupp</p></div></div></div></div></div></div></div><div><div id=\"el-966e484c\" data-vce-do-apply=\"all el-966e484c\"><div data-vce-element-content=\"true\"><div id=\"el-ce57313a\" data-vce-do-apply=\"background border el-ce57313a\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-ce57313a\"><div><div id=\"el-15d3dfb9\" data-vce-do-apply=\"all el-15d3dfb9\"><p><span>16€</span></p><h5>RAVIOLI DI STAGIONE</h5><p>Hooajalised ravioolid</p></div></div><div><div id=\"el-6632b2f4\" data-vce-do-apply=\"all el-6632b2f4\"><p><span>14€</span></p><h5>TAGLIOLINI AL SALMONE E CAVIALE</h5><p>Tagliolinid lõhe ja punase kalamarjaga</p></div></div><div><div id=\"el-8fce0e2e\" data-vce-do-apply=\"all el-8fce0e2e\"><p><span>15€</span></p><h5>SPAGHETTI CON VONGOLE</h5><p>Spagetid merekarpidega</p></div></div><div><div id=\"el-8c4d1a62\" data-vce-do-apply=\"all el-8c4d1a62\"><p><span>16€</span></p><h5>LINGUINE ALLO SCOGLIO</h5><p>Linguine´id mereandidega</p></div></div></div></div><div id=\"el-a6412626\" data-vce-do-apply=\"background border el-a6412626\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-a6412626\"><div><div id=\"el-a378426b\" data-vce-do-apply=\"all el-a378426b\"><p><span>17€</span></p><h5>FETTUCCINE AL NERO DI SEPPIA CON GAMBERI E ASPARAGI VERDI</h5><p>Tindikala fettuccine krevettide ja rohelise spargliga</p></div></div><div><div id=\"el-7e0d4726\" data-vce-do-apply=\"all el-7e0d4726\"><p><span>26€</span></p><h5>FETTUCCINE CON GRANCHIO REALE, POMODORINI E BASILICO</h5><p>Fettuccine Alaska kuningkrabiga</p></div></div><div><div id=\"el-d5260d88\" data-vce-do-apply=\"all el-d5260d88\"><p><span>15€</span></p><h5>TAGLIATELLE CON PORCINI E PARMIGIANO</h5><p>Tagliatelle puravike ja parmesaniga</p></div></div><div><div id=\"el-7be96fe1\" data-vce-do-apply=\"all el-7be96fe1\"><p><span>16€</span></p><h5>FETTUCCINE DELLA MAMMA</h5><p>Fettuccine puravike ja loomalihaga koorekastmes</p></div></div></div></div><div id=\"el-55b07efe\" data-vce-do-apply=\"background border el-55b07efe\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-55b07efe\"><div><div id=\"el-1c581b38\" data-vce-do-apply=\"all el-1c581b38\"><p><span>19€</span></p><h5>TAGLIOLINI SALTATI NELLA FORMA DI PARMIGIANO CON TARTUFO</h5><p>Parmesanikeras keerutatud tagliolinid musta trühvliga</p></div></div><div><div id=\"el-2d2a90a4\" data-vce-do-apply=\"all el-2d2a90a4\"><p><span>16€</span></p><h5>RISOTTO AI FUNGHI PORCINI</h5>\n" +
                "<p>Puravikurisoto</p></div></div><div><div id=\"el-f6409399\" data-vce-do-apply=\"all el-f6409399\"><p><span>18€</span></p><h5>RISOTTO AI FRUTTI DI MARE</h5><p>Risoto mereandidega</p></div></div><div><div id=\"el-d6db41d8\" data-vce-do-apply=\"all el-d6db41d8\"><p><span>21€</span></p><h5>RISOTTO CON FONDUZA DI MONTASIO E TARTUFO</h5><p>Risotto Montasio juustu fondüü ja musta trühvliga</p></div></div></div></div></div></div></div><div><div id=\"el-0d2f83ad\" data-vce-do-apply=\"all el-0d2f83ad\"><div data-vce-element-content=\"true\"><div id=\"el-3e9b71e6\" data-vce-do-apply=\"background border el-3e9b71e6\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-3e9b71e6\"><div><p id=\"el-3372d6ff\" data-vce-do-apply=\"all el-3372d6ff\"></p><h2>Pesci e Crostacei / Kala ja mereannid</h2><p></p></div></div></div></div></div></div><div><div id=\"el-8cf2f4ef\" data-vce-do-apply=\"all el-8cf2f4ef\"><div data-vce-element-content=\"true\"><div id=\"el-be379cc6\" data-vce-do-apply=\"background border el-be379cc6\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-be379cc6\"><div><div id=\"el-f65f921e\" data-vce-do-apply=\"all el-f65f921e\"><p><span>26€</span></p><h5>CODA DI ROSPO ALLA LIVORNESE</h5><p>Merikurat teravas tomatikastmes</p></div></div><div><div id=\"el-6e9863da\" data-vce-do-apply=\"all el-6e9863da\"><p><span>26€</span></p><h5>TONNO IN CROSTA DI SESAMO</h5><p>Tuunikalasteik seesami paneeringus</p></div></div><div><div id=\"el-e2365600\" data-vce-do-apply=\"all el-e2365600\"><p><span>15€</span></p><h5>CALAMARETTI FRITTI</h5><p>Beebikalmaarid friteeritult tartarkastmega</p></div></div><div><div id=\"el-52f7bb7a\" data-vce-do-apply=\"all el-52f7bb7a\"><p><span>20€</span></p><h5>GAMBERONI CON TESTA ALLA GRIGLIA 200g</h5><p>Grillitud tiigerkevetid (200g = ~ 3tk)</p></div></div><div><div id=\"el-c67304a5\" data-vce-do-apply=\"all el-c67304a5\"><p><span>20€</span></p><h5>GAMBERONI AL ROSMARINO E AGLIO 200g</h5><p>Küüslaugu ja rosmariiniga praetud tiigerkrevetid (200g = ~ 3tk)</p></div></div></div></div><div id=\"el-0469922e\" data-vce-do-apply=\"background border el-0469922e\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-0469922e\"><div><div id=\"el-da82189b\" data-vce-do-apply=\"all el-da82189b\"><p><span>24€</span></p><h5>ORATA AL SALE</h5><p>Soolakestas küpsetatud kuldmerikoger</p></div></div><div><div id=\"el-7e8db1e2\" data-vce-do-apply=\"all el-7e8db1e2\"><p><span>23€</span></p><h5>ORATA AL VINO BIANCO</h5><p>Kuldmerikoger valge veini kastmes</p></div></div><div><div id=\"el-95c7010c\" data-vce-do-apply=\"all el-95c7010c\"><p><span>26€</span></p><h5>BRANZINO AL SALE</h5><p>Soolakestas küpsetatud huntahven</p></div></div><div><div id=\"el-f01b3046\" data-vce-do-apply=\"all el-f01b3046\"><p><span>25€</span></p><h5>BRANZINO AL ROSMARINO E AGLIO</h5><p>Küüslaugu ja rosmariiniga praetud huntahven</p></div></div><div><div id=\"el-84d5633a\" data-vce-do-apply=\"all el-84d5633a\"><p><span>36€</span></p><h5>SOGLIOLA AL BURRO E LIMONE</h5><p>Või ja sidruniga praetud merikeel</p></div></div></div></div><div id=\"el-f043abc6\" data-vce-do-apply=\"background border el-f043abc6\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-f043abc6\"><div><div id=\"el-8eae41b6\" data-vce-do-apply=\"all el-8eae41b6\"><p><span>36€</span></p><h5>SOGLIOLA GRATINATA CON PARMIGIANO</h5><p>Parmesaniga küpsetatud merikeel</p></div></div><div><div id=\"el-8173e19e\" data-vce-do-apply=\"all el-8173e19e\"><p><span>7€</span></p><h5>ROMBO AL BURRO E LIMONE 100g</h5><p>Või ja sidruniga praetud kammeljas</p></div></div><div><div id=\"el-9f956256\" data-vce-do-apply=\"all el-9f956256\"><p><span>7€</span></p><h5>ROMBO AL FORNO CON ZUCCHINI 100g</h5><p>Suvikõrvitsaga ahjus küpsetatud kammeljas</p></div></div></div></div></div></div></div><div><div id=\"el-fd8c37d2\" data-vce-do-apply=\"all el-fd8c37d2\"><div data-vce-element-content=\"true\"><div id=\"el-d3433ccc\" data-vce-do-apply=\"background border el-d3433ccc\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-d3433ccc\"><div><div id=\"el-67a8ac58\" data-vce-do-apply=\"all el-67a8ac58\"><p><span>17€</span></p><h5>SCALOPPINE AI FUNGHI E CREMA</h5><p>Seafilee medaljonid seente ja koorekastmega</p></div></div><div><div id=\"el-54e199f1\" data-vce-do-apply=\"all el-54e199f1\"><p><span>18€</span></p><h5>MEDAGLIONI AL GORGONZOLA CON SPINACI</h5><p>Seafilee medaljonid spinatiga Gorgonzola juustu kastmes</p></div></div><div><div id=\"el-39ff34ea\" data-vce-do-apply=\"all el-39ff34ea\"><p><span>20€</span></p><h5>SALTIMBOCCA ALLA ROMANA</h5><p>Vasikaliha parma singi ja salveiga valge veini kastmes</p></div></div><div><div id=\"el-c908f99d\" data-vce-do-apply=\"all el-c908f99d\"><p><span>22€</span></p><h5>ENTRECOTE DI MANZO CON CIPOLLE</h5><p>Grillitud loomaliha antrekoot praetud sibulaga</p></div></div></div></div><div id=\"el-c7b38b41\" data-vce-do-apply=\"background border el-c7b38b41\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-c7b38b41\"><div><div id=\"el-e961f8ae\" data-vce-do-apply=\"all el-e961f8ae\"><p><span>25€</span></p><h5>FILETTO DI MANZO AI FUNGHI PORCINI</h5><p>Veisefilee värskete puravikkudega</p></div></div><div><div id=\"el-9eda7610\" data-vce-do-apply=\"all el-9eda7610\"><p><span>24€</span></p><h5>FILETTO DI MANZO AL PEPE VERDE E BRANDY</h5><p>Veisefilee rohelise pipra ja konjaki kastmes</p></div></div><div><div id=\"el-971ad535\" data-vce-do-apply=\"all el-971ad535\"><p><span>27€</span></p><h5>TOURNEDOS ALLA “ROSSINI” RIMODERNATO</h5><p>Veisefilee hanemaksaga portveini kastmes</p></div></div><div><div id=\"el-7ab9747c\" data-vce-do-apply=\"all el-7ab9747c\"><p><span>23€</span></p><h5>TAGLIATA DI MANZO SU RUCOLA E BALSAMICO</h5><p>Viilutatud veisefilee rukola, palsamäädika ja parmesaniga</p></div></div></div></div><div id=\"el-d135b84a\" data-vce-do-apply=\"background border el-d135b84a\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-d135b84a\"><div><div id=\"el-083440b1\" data-vce-do-apply=\"all el-083440b1\"><p><span>35€</span></p><h5>BISTECCA ALLA FIORENTINA 500g</h5><p>Grillitud T-luu steik Firenze moodi</p></div></div><div><div id=\"el-850f19ba\" data-vce-do-apply=\"all el-850f19ba\"><p><span>27€</span></p><h5>COTOLETTE D’AGNELLO SCOTTADITO</h5><p>Tallekarree rosmariini ja küüslauguga</p></div></div><div><div id=\"el-8a8c1a17\" data-vce-do-apply=\"all el-8a8c1a17\"><p><span>22€</span></p><h5>PETTO D’ANATRA ALL’ARANCIO E BALSAMICO</h5><p>Pardirind apelsini ja palsamiäädika kastmes</p></div></div></div></div></div></div></div><div><div id=\"el-d75da0d0\" data-vce-do-apply=\"all el-d75da0d0\"><div data-vce-element-content=\"true\"><div id=\"el-c7d62b92\" data-vce-do-apply=\"background border el-c7d62b92\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-c7d62b92\"><div><div id=\"el-88798392\" data-vce-do-apply=\"all el-88798392\"><p><span>12€</span></p><h5>PIZZA QUATTRO FORMAGGI</h5><p>Nelja erineva juustuga pitsa</p></div></div><div><div id=\"el-cda4a6d9\" data-vce-do-apply=\"all el-cda4a6d9\"><p><span>13€</span></p><h5>PIZZA LUCANA</h5><p>Mozzarella, tomatikaste, spinat, vürtsikas salaami, värske ricotta</p></div></div></div></div><div id=\"el-1cbc6ec7\" data-vce-do-apply=\"background border el-1cbc6ec7\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-1cbc6ec7\"><div><div id=\"el-27fbf584\" data-vce-do-apply=\"all el-27fbf584\"><p><span>17€</span></p><h5>PIZZA GAMBERI</h5><p>Mozzarella, tomatikaste, krevetid, küüslauk, suvikõrvits, basiilik</p></div></div><div><div id=\"el-04763e30\" data-vce-do-apply=\"all el-04763e30\"><p><span>14€</span></p><h5>PIZZA VESUVIANA</h5><p>Mozzarella, tomatikaste, Tyroli peekon, suitsutatud provola juust, paprika</p></div></div></div></div><div id=\"el-01caf6dc\" data-vce-do-apply=\"background border el-01caf6dc\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-01caf6dc\"><div><div id=\"el-c0bf0c41\" data-vce-do-apply=\"all el-c0bf0c41\"><p><span>20€</span></p><h5>PIZZA BIANCA CON TARTUFO</h5><p>Mozzarella, Tallegio juust, must trühvel</p></div></div></div></div></div></div></div><div><div id=\"el-7faef9a8\" data-vce-do-apply=\"all el-7faef9a8\"><div data-vce-element-content=\"true\"><div id=\"el-1752d6cc\" data-vce-do-apply=\"background border el-1752d6cc\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-1752d6cc\"><div><div id=\"el-8d78171c\" data-vce-do-apply=\"all el-8d78171c\"><p><span>7€</span></p><h5>CREME BRULEE</h5><p>Kreembrülee</p></div></div><div><div id=\"el-2dc4402c\" data-vce-do-apply=\"all el-2dc4402c\"><p><span>7€</span></p><h5>SEMIFREDDO DI STAGIONE</h5><p>Hooaja parfee</p></div></div></div></div><div id=\"el-b19e59bd\" data-vce-do-apply=\"background border el-b19e59bd\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-b19e59bd\"><div><div id=\"el-f1b45120\" data-vce-do-apply=\"all el-f1b45120\"><p><span>7€</span></p><h5>PANNA COTTA DELLA CASA</h5><p>Kodune kooretarretis</p></div></div><div><div id=\"el-bb2aa86e\" data-vce-do-apply=\"all el-bb2aa86e\"><p><span>8€</span></p><h5>SORBETTO DI STAGIONE</h5><p>Hooaja sorbee</p></div></div><div><div id=\"el-bc8a2b31\" data-vce-do-apply=\"all el-bc8a2b31\"><p><span>9€</span></p><h5>TORTINO DI CIOCCOLATO</h5><p>Šokolaadi fondant jäätisega</p></div></div></div></div><div id=\"el-407e14a0\" data-vce-do-apply=\"background border el-407e14a0\"><div data-vce-element-content=\"true\" data-vce-do-apply=\"padding margin  el-407e14a0\"><div><div id=\"el-7f6bceb9\" data-vce-do-apply=\"all el-7f6bceb9\"><p><span>11€</span></p><h5>CREPES SUZETTE</h5><p>Crepes leegitatud Grand Marnier’iga</p></div></div><div><div id=\"el-8f7fcf45\" data-vce-do-apply=\"all el-8f7fcf45\"><p><span>2€</span></p><h5>GELATO MISTO (1 ball)</h5><p>Jäätisevalik (1 pall)</p></div></div></div></div></div></div></div>\n" +
                "\t</div>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t</div>\n" +
                "\n" +
                "\t\t\t\t\n" +
                "\t\t\t</div>\n" +
                "\t\t</div>\n" +
                "\t</div></div></div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div>\n" +
                "      <div class=\"reader-message\">Loading…</div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <ul class=\"toolbar reader-toolbar\">\n" +
                "    <li><button class=\"button close-button\" title=\"Close Reader View\"></button></li>\n" +
                "    <ul class=\"dropdown style-dropdown\">\n" +
                "      <li><button class=\"dropdown-toggle button style-button\" title=\"Type controls\"></button></li>\n" +
                "      <li class=\"dropdown-popup\">\n" +
                "        <div class=\"font-type-buttons\"><button class=\"sans-serif-button selected\"><div class=\"name\">Aa</div><div class=\"description\">Sans-serif</div></button><button class=\"serif-button\"><div class=\"name\">Aa</div><div class=\"description\">Serif</div></button></div>\n" +
                "        <hr>\n" +
                "        <div class=\"font-size-buttons\">\n" +
                "          <button class=\"minus-button\" title=\"Decrease Font Size\">\n" +
                "          </button><button class=\"font-size-sample\">Aa</button><button class=\"plus-button\" title=\"Increase Font Size\">\n" +
                "        </button></div>\n" +
                "        <hr>\n" +
                "        <div class=\"content-width-buttons\">\n" +
                "          <button class=\"content-width-minus-button\" title=\"Decrease Content Width\">\n" +
                "          </button><button class=\"content-width-plus-button\" title=\"Increase Content Width\">\n" +
                "        </button></div>\n" +
                "        <hr>\n" +
                "        <div class=\"line-height-buttons\">\n" +
                "          <button class=\"line-height-minus-button\" title=\"Decrease Line Height\">\n" +
                "          </button><button class=\"line-height-plus-button\" title=\"Increase Line Height\">\n" +
                "        </button></div>\n" +
                "        <hr>\n" +
                "        <div class=\"color-scheme-buttons\"><button class=\"light-button selected\" title=\"Color Scheme Light\"><div class=\"name\">Light</div></button><button class=\"dark-button\" title=\"Color Scheme Dark\"><div class=\"name\">Dark</div></button><button class=\"sepia-button\" title=\"Color Scheme Sepia\"><div class=\"name\">Sepia</div></button></div>\n" +
                "        <div class=\"dropdown-arrow\">\n" +
                "      </div></li>\n" +
                "    </ul>\n" +
                "  <ul class=\"dropdown narrate-dropdown\"><li><button class=\"dropdown-toggle button narrate-toggle\" title=\"Narrate\" hidden=\"\"></button></li><li class=\"dropdown-popup\"><div class=\"narrate-row narrate-control\"><button class=\"narrate-skip-previous\" disabled=\"\" title=\"Back\"></button><button class=\"narrate-start-stop\" title=\"Start\"></button><button class=\"narrate-skip-next\" disabled=\"\" title=\"Forward\"></button></div><div class=\"narrate-row narrate-rate\"><input class=\"narrate-rate-input\" value=\"0\" step=\"5\" max=\"100\" min=\"-100\" type=\"range\" title=\"Speed\"></div><div class=\"narrate-row narrate-voices\"><div class=\"voiceselect voice-select\"><button class=\"select-toggle\" aria-controls=\"voice-options\">\n" +
                "      <span class=\"label\">Voice:</span> <span class=\"current-voice\"></span>\n" +
                "    </button>\n" +
                "    <div class=\"options\" id=\"voice-options\" role=\"listbox\"></div></div></div><div class=\"dropdown-arrow\"></div></li></ul><button data-buttonid=\"pocket-button\" class=\"button pocket-button\" style=\"background-image: url(&quot;chrome://pocket/content/panels/img/pocket-outline.svg&quot;); background-size: 20px 20px;\" title=\"Save to Pocket\"></button></ul>\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "</body></html>");
        return driver;
    }

    public static FacebookClient mockFbClient(String id, String username, @Nullable FacebookClient toMock) {
        FacebookClient client = toMock;
        if (client == null) {
            client = mock(FacebookClient.class) ;
        }
        FacebookPageDto pageDto = FacebookPageDto.builder()
                .id(id)
                .username(username)
                .name("Test Kitchen")
                .location(FacebookLocationDataDto.builder()
                        .latitude(9.0)
                        .longitude(6.0)
                        .zip("10101")
                        .street("Gonsiori 13")
                        .city("Tallinn").country("Nigeria")
                        .build())
                .build();
        when(client.getFacebookPage(anyString(), eq(username)))
                .thenReturn(pageDto);
        when(client.getFacebookPage(anyString(), eq(id)))
                .thenReturn(pageDto);
        return client;
    }

    public static PlacesApiClient mockPlacesClient(String... placeIds) {
        PlacesApiClient mock = mock(PlacesApiClient.class);

        for (String placeId : placeIds) {
            PlaceDetails details  =  new PlaceDetails();
            details.placeId = placeId;
            details.name = "Test Place " + placeId;
            details.formattedPhoneNumber = "234-56-789";
            details.internationalPhoneNumber = "+123456789";
            details.formattedAddress = "Test Address " + placeId;
            try {
                details.website = new URL("http://test.com/" + placeId);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Geometry g  = new Geometry();
            g.location = new LatLng(59.437515, 24.746583);

            details.geometry = g;
            details.addressComponents = new AddressComponent[0];
            when(mock.getPlaceDetails(eq(placeId))).thenReturn(details);
        }
        return mock;
    }

}
