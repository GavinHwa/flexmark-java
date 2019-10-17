package com.vladsch.flexmark.integration;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.spec.SpecExample;
import com.vladsch.flexmark.test.*;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests that the spec examples still render the same with all extensions enabled.
 */

@RunWith(Parameterized.class)
public class SpecIntegrationTest extends RenderingTestCase {
    private static DataHolder OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(
                    AutolinkExtension.create(),
                    StrikethroughExtension.create(),
                    TablesExtension.create(),
                    YamlFrontMatterExtension.create())
            )
            .set(TestUtils.NO_FILE_EOL, false)
            .set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
            .toImmutable();

    private static final Map<String, String> OVERRIDDEN_EXAMPLES = getOverriddenExamples();

    protected final SpecExample example;

    @Override
    public @Nullable DataHolder options(String option) {
        return null;
    }

    public SpecIntegrationTest(SpecExample example) {
        this.example = example;
    }

    @NotNull
    @Override
    public SpecExample getExample() {
        return example;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestUtils.getTestData(ComboSpecTestCase.class, TestUtils.DEFAULT_SPEC_RESOURCE, TestUtils.DEFAULT_URL_PREFIX);
    }

    @Test
    public void testHtmlRendering() {
        String expectedHtml = OVERRIDDEN_EXAMPLES.get(example.getSource());
        if (expectedHtml != null) {
            assertRendering(example.getFileUrl(), example.getSource(), expectedHtml);
        } else {
            if (example.getAst() != null) {
                assertRendering(example.getFileUrl(), example.getSource(), example.getHtml(), example.getAst(), example.getOptionsSet());
            } else {
                assertRendering(example.getFileUrl(), example.getSource(), example.getHtml(), example.getOptionsSet());
            }
        }
    }

    @Override
    public @NotNull SpecExampleRenderer getSpecExampleRenderer(@NotNull SpecExample example, @Nullable DataHolder exampleOptions) {
        DataHolder combinedOptions = combineOptions(OPTIONS, exampleOptions);
        return new FlexmarkSpecExampleRenderer(example, combinedOptions, Parser.builder(combinedOptions).build(), HtmlRenderer.builder(combinedOptions).build(), true);
    }

    private static Map<String, String> getOverriddenExamples() {
        Map<String, String> m = new HashMap<>();

        // Not a spec autolink because of space, but the resulting text contains a valid URL
        m.put("<http://foo.bar/baz bim>\n", "<p>&lt;<a href=\"http://foo.bar/baz\">http://foo.bar/baz</a> bim&gt;</p>\n");

        // Not a spec autolink, but the resulting text contains a valid email
        m.put("<foo\\+@bar.example.com>\n", "<p>&lt;<a href=\"mailto:foo+@bar.example.com\">foo+@bar.example.com</a>&gt;</p>\n");

        // Not a spec autolink because of unknown scheme, but autolink extension doesn't limit schemes
        m.put("<heck://bing.bong>\n", "<p>&lt;<a href=\"heck://bing.bong%3E\">heck://bing.bong&gt;</a></p>\n");

        // Not a spec autolink because of spaces, but autolink extension doesn't limit schemes
        m.put("< http://foo.bar >\n", "<p>&lt; <a href=\"http://foo.bar\">http://foo.bar</a> &gt;</p>\n");

        // Plain autolink
        m.put("http://example.com\n", "<p><a href=\"http://example.com\">http://example.com</a></p>\n");

        // Plain autolink
        m.put("foo@bar.example.com\n", "<p><a href=\"mailto:foo@bar.example.com\">foo@bar.example.com</a></p>\n");

        // YAML front matter block
        m.put("---\nFoo\n---\nBar\n---\nBaz\n", "<h2>Bar</h2>\n<p>Baz</p>\n");
        m.put("---\n---\n", "");

        return m;
    }
}
