package io.kindx.util.tests;

import io.kindx.util.TextUtil;
import org.junit.Assert;
import org.junit.Test;

public class TextUtilTests {

    @Test
    public void testSystemFriendlyName() {
        Assert.assertEquals("Lisandid Kartul Riis kusi Sober Varskekapsasupp",
                TextUtil.toSystemFriendlyText("Lisandid: Kartul, Riis, küsi, Sõber, Värskekapsasupp!!;##"));
    }
}
